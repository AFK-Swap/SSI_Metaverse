package com.ssi.verification;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SSIVerificationPlugin extends JavaPlugin implements Listener {
    
    private static final String WEB_WALLET_URL = "http://localhost:3001";
    private Map<String, VerificationSession> verificationSessions = new HashMap<>();
    private Map<String, Boolean> verifiedPlayers = new HashMap<>();
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("SSI Verification Plugin enabled!");
        getLogger().info("Web wallet should be running on: " + WEB_WALLET_URL);
        
        // Check if integration server is running
        checkIntegrationServer();
    }
    
    @Override
    public void onDisable() {
        getLogger().info("SSI Verification Plugin disabled!");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        switch (command.getName().toLowerCase()) {
            case "verify":
                if (args.length == 0) {
                    handleVerifyCommand(player, "mobile"); // Default mobile QR mode
                } else if (args.length == 1 && args[0].equalsIgnoreCase("web")) {
                    handleVerifyCommand(player, "web"); // Web wallet API mode
                } else {
                    player.sendMessage(ChatColor.RED + "Usage: /verify or /verify web");
                }
                return true;
                
            case "ssiverify":
                if (args.length == 0) {
                    handleSSIVerifyCommand(player, player.getName());
                } else {
                    handleSSIVerifyCommand(player, args[0]);
                }
                return true;
        }
        
        return false;
    }
    
    private void handleVerifyCommand(Player player, String mode) {
        String playerName = player.getName();
        
        
        // Check if already verified
        if (verifiedPlayers.getOrDefault(playerName, false)) {
            player.sendMessage(ChatColor.GREEN + "✓ You are already verified!");
            return;
        }
        
        // Check if verification in progress
        VerificationSession currentSession = verificationSessions.get(playerName);
        if (currentSession != null && !currentSession.isExpired()) {
            player.sendMessage(ChatColor.YELLOW + "Verification already in progress...");
            return;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== SSI Identity Verification ===");
        
        if (mode.equals("web")) {
            player.sendMessage(ChatColor.YELLOW + "🌐 Sending request to web wallet...");
            player.sendMessage(ChatColor.GRAY + "→ Check your browser at localhost:3001");
            player.sendMessage(ChatColor.GRAY + "→ Look for the proof request notification");
        } else {
            player.sendMessage(ChatColor.YELLOW + "📱 Generating QR code for mobile wallet...");
            player.sendMessage(ChatColor.GRAY + "→ Scan with your mobile SSI wallet app");
        }
        
        // Start verification asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                startVerificationProcess(player, mode);
            } catch (Exception e) {
                getLogger().severe("Verification failed for " + playerName + ": " + e.getMessage());
                player.sendMessage(ChatColor.RED + "Verification system unavailable. Try again later.");
            }
        });
    }
    
    private void handleSSIVerifyCommand(Player sender, String targetPlayerName) {
        boolean isVerified = verifiedPlayers.getOrDefault(targetPlayerName, false);
        sender.sendMessage(ChatColor.GOLD + "=== Verification Status ===");
        sender.sendMessage(ChatColor.WHITE + "Player: " + ChatColor.YELLOW + targetPlayerName);
        sender.sendMessage(ChatColor.WHITE + "Status: " + 
            (isVerified ? ChatColor.GREEN + "✓ VERIFIED" : ChatColor.RED + "✗ NOT VERIFIED"));
    }
    
    private void startVerificationProcess(Player player, String mode) throws Exception {
        String playerName = player.getName();
        String playerUUID = player.getUniqueId().toString();
        
        if (mode.equals("web")) {
            // Use new web wallet API
            String requestBody = String.format("""
                {
                  "type": "verification",
                  "requester": {
                    "playerUUID": "%s",
                    "playerName": "%s"
                  },
                  "requestedAttributes": ["name", "email", "department", "issuer_did", "age"],
                  "timestamp": "%s"
                }
                """, playerUUID, playerName, java.time.Instant.now().toString());
            
            // Make request to web wallet
            String response = makeHttpRequest("POST", WEB_WALLET_URL + "/api/minecraft/verify", requestBody);
            
            // Parse response for web wallet
            if (response.contains("\"success\":true")) {
                String verificationId = extractJsonValue(response, "verificationId");
                
                if (verificationId != null) {
                    // Create session for web wallet
                    VerificationSession session = new VerificationSession();
                    session.playerName = playerName;
                    session.sessionId = verificationId;
                    session.startTime = System.currentTimeMillis();
                    session.walletMode = "web";
                    
                    verificationSessions.put(playerName, session);
                    
                    // Send success message
                    Bukkit.getScheduler().runTask(this, () -> {
                        player.sendMessage(ChatColor.GREEN + "✅ Verification request sent to web wallet!");
                        player.sendMessage(ChatColor.GRAY + "→ Check your browser at localhost:3001");
                        player.sendMessage(ChatColor.GRAY + "→ Look for the proof request notification");
                        player.sendMessage(ChatColor.GRAY + "→ Click 'Share Info' to verify your identity");
                    });
                    
                    // Start monitoring verification status
                    getLogger().info("MONITOR: Starting monitoring for session " + verificationId + " for player " + playerName);
                    startWebVerificationMonitoring(player, session);
                } else {
                    Bukkit.getScheduler().runTask(this, () -> {
                        player.sendMessage(ChatColor.RED + "Failed to create verification session");
                    });
                }
            } else {
                final String errorMessage = "Web wallet verification failed to start";
                Bukkit.getScheduler().runTask(this, () -> {
                    player.sendMessage(ChatColor.RED + "✗ " + errorMessage);
                    player.sendMessage(ChatColor.GRAY + "→ Make sure web wallet is running at localhost:3001");
                });
            }
            return; // Exit after web mode processing
            
        } else {
            // Mobile mode - use old QR code system (keep existing implementation)
            String requestBody = String.format("{\"playerName\":\"%s\"}", playerName);
            String response = makeHttpRequest("POST", WEB_WALLET_URL + "/api/verify-player", requestBody);
            
            if (response.contains("\"success\":true")) {
                String qrUrl = extractJsonValue(response, "qrUrl");
                String sessionId = extractJsonValue(response, "sessionId");
                
                if (qrUrl != null && sessionId != null) {
                    VerificationSession session = new VerificationSession();
                    session.playerName = playerName;
                    session.sessionId = sessionId;
                    session.qrUrl = qrUrl;
                    session.startTime = System.currentTimeMillis();
                    session.walletMode = "mobile";
                    
                    verificationSessions.put(playerName, session);
                    
                    Bukkit.getScheduler().runTask(this, () -> {
                        player.sendMessage(ChatColor.GREEN + "✓ QR code generated!");
                        player.sendMessage(ChatColor.AQUA + "Scan this QR with your mobile SSI wallet:");
                        player.sendMessage(ChatColor.BLUE + "" + ChatColor.BOLD + qrUrl);
                    });
                    
                    startMobileVerificationMonitoring(player, session);
                } else {
                    Bukkit.getScheduler().runTask(this, () -> {
                        player.sendMessage(ChatColor.RED + "Failed to generate QR code for mobile mode");
                    });
                }
            } else {
                Bukkit.getScheduler().runTask(this, () -> {
                    player.sendMessage(ChatColor.RED + "Mobile wallet verification not available");
                });
            }
        }
    }
    
    private void startWebVerificationMonitoring(Player player, VerificationSession session) {
        // Check verification status every 3 seconds
        final int[] taskId = new int[1];
        
        taskId[0] = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            private int attempts = 0;
            private final int maxAttempts = 100; // 5 minutes
            
            @Override
            public void run() {
                attempts++;
                
                try {
                    // Check web wallet verification status
                    String response = makeHttpRequest("GET", WEB_WALLET_URL + "/api/minecraft/verify/" + session.sessionId, null);
                    getLogger().info("MONITOR: Checking session " + session.sessionId + " - attempt " + attempts + "/" + maxAttempts);
                    getLogger().info("MONITOR: Response: " + (response.length() > 200 ? response.substring(0, 200) + "..." : response));
                    
                    if (response.contains("\"status\":\"verified\"") || response.contains("\"verified\":true")) {
                        // Verification successful!
                        verifiedPlayers.put(session.playerName, true);
                        verificationSessions.remove(session.playerName);
                        
                        Bukkit.getScheduler().runTask(SSIVerificationPlugin.this, () -> {
                            player.sendMessage(ChatColor.GREEN + "🎉 Web wallet verification completed successfully!");
                            player.sendMessage(ChatColor.YELLOW + "📜 Your credentials have been verified!");
                            player.sendMessage(ChatColor.GRAY + "→ You now have verified player benefits");
                            applyVerifiedBenefits(player);
                            Bukkit.broadcastMessage(ChatColor.GOLD + "🌐 " + player.getName() + 
                                ChatColor.GREEN + " has been verified with web wallet SSI credentials!");
                        });
                        
                        // Cancel the monitoring task
                        Bukkit.getScheduler().cancelTask(taskId[0]);
                        return; // Stop monitoring
                    } else if (response.contains("\"status\":\"failed\"")) {
                        // Verification failed (not declined) - show specific reason
                        verificationSessions.remove(session.playerName);
                        
                        // Extract failure reason from response
                        String failureReason = "Verification failed";
                        try {
                            // Try to extract the detailed message
                            if (response.contains("\"message\"")) {
                                String messageStart = "\"message\":\"";
                                int startIndex = response.indexOf(messageStart);
                                if (startIndex != -1) {
                                    startIndex += messageStart.length();
                                    int endIndex = response.indexOf("\"", startIndex);
                                    if (endIndex != -1) {
                                        failureReason = response.substring(startIndex, endIndex);
                                        // Clean up JSON escape characters
                                        failureReason = failureReason.replace("\\\"", "\"").replace("\\n", " ");
                                    }
                                }
                            }
                        } catch (Exception e) {
                            getLogger().warning("Failed to parse failure reason: " + e.getMessage());
                        }
                        
                        final String finalReason = failureReason;
                        Bukkit.getScheduler().runTask(SSIVerificationPlugin.this, () -> {
                            player.sendMessage(ChatColor.RED + "❌ " + finalReason);
                            player.sendMessage(ChatColor.GRAY + "→ You can try /verify web again");
                            player.sendMessage(ChatColor.GRAY + "→ Or use /verify for mobile wallet");
                        });
                        
                        // Cancel the monitoring task
                        Bukkit.getScheduler().cancelTask(taskId[0]);
                        return; // Stop monitoring
                    } else if (response.contains("\"status\":\"declined\"")) {
                        // User declined - just stop monitoring silently (no notification needed)
                        verificationSessions.remove(session.playerName);
                        // Cancel the monitoring task
                        Bukkit.getScheduler().cancelTask(taskId[0]);
                        return; // Stop monitoring silently
                    }
                    
                } catch (Exception e) {
                    getLogger().warning("Failed to check web verification status: " + e.getMessage());
                }
                
                // Stop monitoring after max attempts
                if (attempts >= maxAttempts) {
                    verificationSessions.remove(session.playerName);
                    Bukkit.getScheduler().runTask(SSIVerificationPlugin.this, () -> {
                        player.sendMessage(ChatColor.RED + "⏰ Web wallet verification timeout (5 minutes)");
                        player.sendMessage(ChatColor.GRAY + "→ Try /verify web again");
                        player.sendMessage(ChatColor.GRAY + "→ Or use /verify for mobile wallet");
                    });
                    // Cancel the monitoring task
                    Bukkit.getScheduler().cancelTask(taskId[0]);
                }
            }
        }, 60L, 60L).getTaskId(); // Start after 3 seconds, repeat every 3 seconds
    }
    
    private void startMobileVerificationMonitoring(Player player, VerificationSession session) {
        // Similar to web but for mobile QR code verification
        final int[] taskId = new int[1];
        
        taskId[0] = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            private int attempts = 0;
            private final int maxAttempts = 100;
            
            @Override
            public void run() {
                attempts++;
                
                try {
                    String response = makeHttpRequest("GET", WEB_WALLET_URL + "/api/verify-player?playerName=" + session.playerName, null);
                    
                    if (response.contains("\"status\":\"verified\"") || response.contains("\"verified\":true")) {
                        verifiedPlayers.put(session.playerName, true);
                        verificationSessions.remove(session.playerName);
                        
                        Bukkit.getScheduler().runTask(SSIVerificationPlugin.this, () -> {
                            player.sendMessage(ChatColor.GREEN + "🎉 Mobile wallet verification completed successfully!");
                            player.sendMessage(ChatColor.YELLOW + "📱 Your mobile credentials have been verified!");
                            player.sendMessage(ChatColor.GRAY + "→ You now have verified player benefits");
                            applyVerifiedBenefits(player);
                            Bukkit.broadcastMessage(ChatColor.GOLD + "📱 " + player.getName() + 
                                ChatColor.GREEN + " has been verified with mobile SSI wallet!");
                        });
                        
                        // Cancel the monitoring task
                        Bukkit.getScheduler().cancelTask(taskId[0]);
                        return;
                    }
                    
                } catch (Exception e) {
                    getLogger().warning("Failed to check mobile verification status: " + e.getMessage());
                }
                
                if (attempts >= maxAttempts) {
                    verificationSessions.remove(session.playerName);
                    Bukkit.getScheduler().runTask(SSIVerificationPlugin.this, () -> {
                        player.sendMessage(ChatColor.RED + "⏰ Mobile wallet verification timeout (5 minutes)");
                        player.sendMessage(ChatColor.GRAY + "→ Try /verify again for mobile");
                        player.sendMessage(ChatColor.GRAY + "→ Or use /verify web for web wallet");
                    });
                    // Cancel the monitoring task
                    Bukkit.getScheduler().cancelTask(taskId[0]);
                }
            }
        }, 60L, 60L).getTaskId();
    }
    
    
    private void applyVerifiedBenefits(Player player) {
        // Give glowing effect
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
            "effect give " + player.getName() + " minecraft:glowing 999999 0 true");
        
        player.sendMessage(ChatColor.GREEN + "✓ You now have verified player benefits!");
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is verified
        if (verifiedPlayers.getOrDefault(player.getName(), false)) {
            player.sendMessage(ChatColor.GREEN + "Welcome back! You are verified.");
            applyVerifiedBenefits(player);
        } else {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.GOLD + "/verify " + 
                    ChatColor.YELLOW + "to verify your identity with SSI credentials!");
            }, 60L); // 3 seconds delay
        }
    }
    
    private void checkIntegrationServer() {
        CompletableFuture.runAsync(() -> {
            try {
                makeHttpRequest("GET", WEB_WALLET_URL + "/api/minecraft/verify", null);
                getLogger().info("✓ Web wallet is running at " + WEB_WALLET_URL);
            } catch (Exception e) {
                getLogger().warning("⚠ Web wallet not accessible: " + e.getMessage());
                getLogger().warning("  Please run: cd vr-web-wallet && npm run dev");
            }
        });
    }
    
    private String makeHttpRequest(String method, String urlString, String requestBody) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        
        if (requestBody != null) {
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBody.getBytes());
            }
        }
        
        int responseCode = connection.getResponseCode();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                responseCode >= 200 && responseCode < 300 ? 
                connection.getInputStream() : connection.getErrorStream()))) {
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            
            if (responseCode >= 200 && responseCode < 300) {
                return response.toString();
            } else {
                throw new Exception("HTTP " + responseCode + ": " + response.toString());
            }
        }
    }
    
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) return null;
        
        startIndex += searchKey.length();
        int endIndex = json.indexOf("\"", startIndex);
        if (endIndex == -1) return null;
        
        return json.substring(startIndex, endIndex);
    }
    
    private static class VerificationSession {
        String playerName;
        String sessionId;
        String qrUrl;
        String walletMode; // "web" or "mobile"
        long startTime;
        
        boolean isExpired() {
            return System.currentTimeMillis() - startTime > 600000; // 10 minutes
        }
    }
}