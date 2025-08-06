package com.ssi.wallet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Mod("ssi_wallet")
@EventBusSubscriber(modid = "ssi_wallet", bus = EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SSIWalletMod {

    private static final String WALLET_BASE_URL = "http://localhost:3001";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
        String message = event.getMessage();
        LocalPlayer player = Minecraft.getInstance().player;
        
        if (player == null) return;

        // Handle /verify command
        if (message.equals("/verify")) {
            event.setCanceled(true); // Prevent the message from being sent as chat
            
            String playerUUID = player.getStringUUID();
            
            // Send general verification request to web wallet
            sendVerificationRequest(player, "general", playerUUID);
        }
        
        // Handle /wallet command for mode switching
        else if (message.startsWith("/wallet ")) {
            event.setCanceled(true);
            
            String[] args = message.split(" ");
            if (args.length >= 2) {
                String subCommand = args[1].toLowerCase();
                
                switch (subCommand) {
                    case "web":
                        player.sendSystemMessage(Component.literal("§a🌐 Switched to Web Mode"));
                        player.sendSystemMessage(Component.literal("§7→ Open your browser at localhost:3001"));
                        player.sendSystemMessage(Component.literal("§7→ Use /verify to send proof requests to browser"));
                        setWalletMode(player, "web");
                        break;
                        
                    case "mobile":
                        player.sendSystemMessage(Component.literal("§a📱 Switched to Mobile Mode"));
                        player.sendSystemMessage(Component.literal("§7→ /verify will show QR codes to scan"));
                        player.sendSystemMessage(Component.literal("§7→ Use your existing mobile SSI wallet app"));
                        setWalletMode(player, "mobile");
                        break;
                        
                    case "status":
                        checkWalletStatus(player);
                        break;
                        
                    default:
                        player.sendSystemMessage(Component.literal("§c❌ Unknown wallet command: " + subCommand));
                        player.sendSystemMessage(Component.literal("§7Available commands:"));
                        player.sendSystemMessage(Component.literal("§7  /wallet web - Switch to web browser mode"));
                        player.sendSystemMessage(Component.literal("§7  /wallet mobile - Switch to mobile QR mode"));
                        player.sendSystemMessage(Component.literal("§7  /wallet status - Show wallet status"));
                }
            } else {
                // No subcommand provided
                player.sendSystemMessage(Component.literal("§e📋 SSI Wallet Commands:"));
                player.sendSystemMessage(Component.literal("§7  /wallet web - Switch to web browser mode"));
                player.sendSystemMessage(Component.literal("§7  /wallet mobile - Switch to mobile QR mode"));
                player.sendSystemMessage(Component.literal("§7  /wallet status - Show wallet status"));
                player.sendSystemMessage(Component.literal("§7  /verify - Request identity verification"));
            }
        }
    }

    private static void sendVerificationRequest(LocalPlayer player, String verificationType, String playerUUID) {
        player.sendSystemMessage(Component.literal("§e🔍 Sending verification request..."));
        
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
            playerUUID,
            player.getName().getString(),
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
                player.sendSystemMessage(Component.literal("§a✅ Verification request sent successfully!"));
                player.sendSystemMessage(Component.literal("§7→ Check your browser at localhost:3001"));
                player.sendSystemMessage(Component.literal("§7→ Look for the proof request notification"));
                player.sendSystemMessage(Component.literal("§7→ Click 'Share Info' to verify your identity"));
            } else {
                player.sendSystemMessage(Component.literal("§c❌ Failed to send verification request"));
                player.sendSystemMessage(Component.literal("§7→ Make sure web wallet is running at localhost:3001"));
                player.sendSystemMessage(Component.literal("§7→ Try /wallet status to check connection"));
            }
        }).exceptionally(throwable -> {
            player.sendSystemMessage(Component.literal("§c❌ Connection error to web wallet"));
            player.sendSystemMessage(Component.literal("§7→ Is localhost:3001 running?"));
            player.sendSystemMessage(Component.literal("§7→ Error: " + throwable.getMessage()));
            return null;
        });
    }


    private static void setWalletMode(LocalPlayer player, String mode) {
        String jsonPayload = String.format("""
            {
              "mode": "%s",
              "playerUUID": "%s"
            }
            """, mode, player.getStringUUID());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WALLET_BASE_URL + "/api/wallet/mode"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        CompletableFuture<HttpResponse<String>> futureResponse = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        
        futureResponse.thenAccept(response -> {
            if (response.statusCode() != 200) {
                player.sendSystemMessage(Component.literal("§c⚠️ Could not sync mode with web wallet"));
            }
        });
    }

    private static void checkWalletStatus(LocalPlayer player) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WALLET_BASE_URL + "/api/wallet/mode"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        CompletableFuture<HttpResponse<String>> futureResponse = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        
        futureResponse.thenAccept(response -> {
            if (response.statusCode() == 200) {
                player.sendSystemMessage(Component.literal("§a📊 Wallet Status: ✅ CONNECTED"));
                player.sendSystemMessage(Component.literal("§7→ Web wallet is running at localhost:3001"));
                player.sendSystemMessage(Component.literal("§7→ Ready to process /verify commands"));
            } else {
                player.sendSystemMessage(Component.literal("§c📊 Wallet Status: ❌ DISCONNECTED"));
                player.sendSystemMessage(Component.literal("§7→ Web wallet is not responding"));
                player.sendSystemMessage(Component.literal("§7→ Start wallet with: npm run dev"));
            }
        }).exceptionally(throwable -> {
            player.sendSystemMessage(Component.literal("§c📊 Wallet Status: ❌ CONNECTION ERROR"));
            player.sendSystemMessage(Component.literal("§7→ Cannot reach localhost:3001"));
            player.sendSystemMessage(Component.literal("§7→ Make sure web wallet is running"));
            return null;
        });
    }
}