package com.ssi.wallet;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class SSIWalletPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private static final String WALLET_BASE_URL = "http://localhost:3001";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public void onEnable() {
        // Register events and commands
        getServer().getPluginManager().registerEvents(this, this);
        
        // Register commands
        this.getCommand("wallet").setExecutor(this);
        this.getCommand("verify").setExecutor(this);
        
        getLogger().info("SSI Wallet Plugin enabled!");
        getLogger().info("Web wallet should be running at " + WALLET_BASE_URL);
    }

    @Override
    public void onDisable() {
        getLogger().info("SSI Wallet Plugin disabled!");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();

        // Handle /verify command via chat
        if (message.equals("/verify")) {
            event.setCancelled(true);
            sendVerificationRequest(player);
        }
        // Handle /wallet commands via chat
        else if (message.startsWith("/wallet ")) {
            event.setCancelled(true);
            String[] args = message.split(" ");
            if (args.length >= 2) {
                handleWalletCommand(player, args[1]);
            } else {
                showWalletHelp(player);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        switch (command.getName().toLowerCase()) {
            case "verify":
                sendVerificationRequest(player);
                break;
                
            case "wallet":
                if (args.length == 0) {
                    showWalletHelp(player);
                } else {
                    handleWalletCommand(player, args[0]);
                }
                break;
                
            default:
                return false;
        }
        
        return true;
    }

    private void handleWalletCommand(Player player, String subCommand) {
        switch (subCommand.toLowerCase()) {
            case "web":
                player.sendMessage(ChatColor.GREEN + "🌐 Switched to Web Mode");
                player.sendMessage(ChatColor.GRAY + "→ Open your browser at localhost:3001");
                player.sendMessage(ChatColor.GRAY + "→ Use /verify to send proof requests to browser");
                setWalletMode(player, "web");
                break;
                
            case "mobile":
                player.sendMessage(ChatColor.GREEN + "📱 Switched to Mobile Mode");
                player.sendMessage(ChatColor.GRAY + "→ /verify will show QR codes to scan");
                player.sendMessage(ChatColor.GRAY + "→ Use your existing mobile SSI wallet app");
                setWalletMode(player, "mobile");
                break;
                
            case "status":
                checkWalletStatus(player);
                break;
                
            default:
                player.sendMessage(ChatColor.RED + "❌ Unknown wallet command: " + subCommand);
                showWalletHelp(player);
        }
    }

    private void showWalletHelp(Player player) {
        player.sendMessage(ChatColor.YELLOW + "📋 SSI Wallet Commands:");
        player.sendMessage(ChatColor.GRAY + "  /wallet web - Switch to web browser mode");
        player.sendMessage(ChatColor.GRAY + "  /wallet mobile - Switch to mobile QR mode");
        player.sendMessage(ChatColor.GRAY + "  /wallet status - Show wallet status");
        player.sendMessage(ChatColor.GRAY + "  /verify - Request identity verification");
    }

    private void sendVerificationRequest(Player player) {
        player.sendMessage(ChatColor.YELLOW + "🔍 Sending verification request...");
        
        // Create JSON payload - request all available attributes
        String jsonPayload = String.format("""
            {
              "type": "verification",
              "requester": {
                "playerUUID": "%s",
                "playerName": "%s"
              },
              "requestedAttributes": ["name", "degree", "university", "certification", "institution", "level", "age", "country"],
              "timestamp": "%s"
            }
            """, 
            player.getUniqueId().toString(),
            player.getName(),
            java.time.Instant.now().toString()
        );

        // Send HTTP request to web wallet
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WALLET_BASE_URL + "/api/minecraft/verify"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        CompletableFuture<HttpResponse<String>> futureResponse = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        
        futureResponse.thenAccept(response -> {
            if (response.statusCode() == 200) {
                player.sendMessage(ChatColor.GREEN + "✅ Verification request sent successfully!");
                player.sendMessage(ChatColor.GRAY + "→ Check your browser at localhost:3001");
                player.sendMessage(ChatColor.GRAY + "→ Look for the proof request notification");
                player.sendMessage(ChatColor.GRAY + "→ Click 'Share Info' to verify your identity");
            } else {
                player.sendMessage(ChatColor.RED + "❌ Failed to send verification request");
                player.sendMessage(ChatColor.GRAY + "→ Make sure web wallet is running at localhost:3001");
                player.sendMessage(ChatColor.GRAY + "→ Try /wallet status to check connection");
            }
        }).exceptionally(throwable -> {
            player.sendMessage(ChatColor.RED + "❌ Connection error to web wallet");
            player.sendMessage(ChatColor.GRAY + "→ Is localhost:3001 running?");
            player.sendMessage(ChatColor.GRAY + "→ Error: " + throwable.getMessage());
            return null;
        });
    }

    private void setWalletMode(Player player, String mode) {
        String jsonPayload = String.format("""
            {
              "mode": "%s",
              "playerUUID": "%s"
            }
            """, mode, player.getUniqueId().toString());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WALLET_BASE_URL + "/api/wallet/mode"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        CompletableFuture<HttpResponse<String>> futureResponse = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        
        futureResponse.thenAccept(response -> {
            if (response.statusCode() != 200) {
                player.sendMessage(ChatColor.RED + "⚠️ Could not sync mode with web wallet");
            }
        });
    }

    private void checkWalletStatus(Player player) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WALLET_BASE_URL + "/api/wallet/mode"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        CompletableFuture<HttpResponse<String>> futureResponse = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        
        futureResponse.thenAccept(response -> {
            if (response.statusCode() == 200) {
                player.sendMessage(ChatColor.GREEN + "📊 Wallet Status: ✅ CONNECTED");
                player.sendMessage(ChatColor.GRAY + "→ Web wallet is running at localhost:3001");
                player.sendMessage(ChatColor.GRAY + "→ Ready to process /verify commands");
            } else {
                player.sendMessage(ChatColor.RED + "📊 Wallet Status: ❌ DISCONNECTED");
                player.sendMessage(ChatColor.GRAY + "→ Web wallet is not responding");
                player.sendMessage(ChatColor.GRAY + "→ Start wallet with: npm run dev");
            }
        }).exceptionally(throwable -> {
            player.sendMessage(ChatColor.RED + "📊 Wallet Status: ❌ CONNECTION ERROR");
            player.sendMessage(ChatColor.GRAY + "→ Cannot reach localhost:3001");
            player.sendMessage(ChatColor.GRAY + "→ Make sure web wallet is running");
            return null;
        });
    }
}