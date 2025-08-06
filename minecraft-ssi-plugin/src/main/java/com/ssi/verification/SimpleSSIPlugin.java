package com.ssi.verification;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SimpleSSIPlugin extends JavaPlugin {
    
    private OkHttpClient httpClient;
    private Gson gson;
    private String acapyAdminUrl;
    private String credentialDefinitionId;
    private final ConcurrentHashMap<String, Boolean> verifiedPlayers = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        acapyAdminUrl = getConfig().getString("acapy.admin-url", "http://localhost:8021");
        credentialDefinitionId = getConfig().getString("acapy.credential-definition-id", "");
        
        httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
        
        gson = new Gson();
        
        // Note: Credential definition ID is no longer required for flexible verification
        // The plugin now accepts credentials from any issuer with required attributes
        
        getLogger().info("Simple SSI Plugin enabled! Using flexible attribute-only verification.");
    }
    
    // Credential definition discovery is no longer needed for flexible verification
    // The plugin now accepts any credential containing required attributes (department, age)

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        
        if ("verify".equals(command.getName())) {
            if (args.length > 0 && "web".equalsIgnoreCase(args[0])) {
                handleWebVerify(player);
            } else {
                handleVerify(player); // Default mobile verification
            }
            return true;
        } else if ("ssiverify".equals(command.getName())) {
            String targetPlayer = args.length > 0 ? args[0] : player.getName();
            handleSSIVerify(player, targetPlayer);
            return true;
        } else if ("reset".equals(command.getName())) {
            handleReset(player);
            return true;
        }
        return false;
    }
    
    private void handleVerify(Player player) {
        if (verifiedPlayers.getOrDefault(player.getName(), false)) {
            player.sendMessage(Component.text("✓ Already verified!", NamedTextColor.GREEN));
            return;
        }
        
        player.sendMessage(Component.text("Creating QR code...", NamedTextColor.YELLOW));
        CompletableFuture.runAsync(() -> createVerification(player));
    }
    
    private void handleWebVerify(Player player) {
        if (verifiedPlayers.getOrDefault(player.getName(), false)) {
            player.sendMessage(Component.text("✓ Already verified!", NamedTextColor.GREEN));
            return;
        }
        
        player.sendMessage(Component.text("🌐 Opening web wallet in browser...", NamedTextColor.YELLOW));
        
        CompletableFuture.runAsync(() -> createWebVerification(player));
    }
    
    private void createVerification(Player player) {
        try {
            getLogger().info("Creating verification for player: " + player.getName());
            
            // Use ssi-tutorial verifier API (simple approach)
            JsonObject request = new JsonObject();
            request.addProperty("label", "Minecraft-Server-" + player.getName());
            request.addProperty("alias", "minecraft-player-" + player.getName());
            
            RequestBody body = RequestBody.create(request.toString(), MediaType.get("application/json"));
            Request httpRequest = new Request.Builder()
                .url("http://localhost:4002/v2/create-invitation")
                .post(body)
                .build();
            
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                getLogger().info("ACA-Py response: " + response.code() + " - " + responseBody);
                
                if (!response.isSuccessful()) {
                    sendMessage(player, Component.text("Failed to create invitation: " + responseBody, NamedTextColor.RED));
                    return;
                }
                
                JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
                
                if (!responseJson.has("invitation_url")) {
                    sendMessage(player, Component.text("Invalid response from verification service", NamedTextColor.RED));
                    return;
                }
                
                String invitationUrl = responseJson.get("invitation_url").getAsString();
                String connectionId = responseJson.get("connection_id").getAsString();
                getLogger().info("Generated invitation URL: " + invitationUrl);
                getLogger().info("Connection ID: " + connectionId);
                
                // Give QR map
                Bukkit.getScheduler().runTask(this, () -> {
                    giveQRMap(player, invitationUrl);
                    player.sendMessage(Component.text("✓ QR Code created! Scan with your SSI wallet.", NamedTextColor.GREEN));
                });
                
                // Start monitoring this specific connection
                monitorConnection(connectionId, player);
                
            }
        } catch (Exception e) {
            getLogger().severe("Verification failed for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            sendMessage(player, Component.text("System error: " + e.getMessage(), NamedTextColor.RED));
        }
    }
    
    private void giveQRMap(Player player, String qrData) {
        try {
            QRCodeWriter qrWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrWriter.encode(qrData, BarcodeFormat.QR_CODE, 128, 128);
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            
            ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
            MapMeta mapMeta = (MapMeta) mapItem.getItemMeta();
            
            MapView mapView = Bukkit.createMap(player.getWorld());
            mapView.getRenderers().clear();
            
            mapView.addRenderer(new MapRenderer() {
                @Override
                public void render(MapView map, MapCanvas canvas, Player player) {
                    for (int x = 0; x < 128; x++) {
                        for (int y = 0; y < 128; y++) {
                            if (x < qrImage.getWidth() && y < qrImage.getHeight()) {
                                int rgb = qrImage.getRGB(x, y);
                                byte color = (rgb == -1) ? (byte) 0 : (byte) 119;
                                canvas.setPixel(x, y, color);
                            }
                        }
                    }
                }
            });
            
            mapMeta.setMapView(mapView);
            mapMeta.setDisplayName("SSI Verification QR Code");
            mapItem.setItemMeta(mapMeta);
            
            player.getInventory().addItem(mapItem);
            
        } catch (Exception e) {
            getLogger().warning("Failed to create QR map: " + e.getMessage());
        }
    }
    
    private void monitorConnection(String connectionId, Player player) {
        final int[] taskId = new int[1];
        
        taskId[0] = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            private int attempts = 0;
            private boolean proofSent = false;
            
            @Override
            public void run() {
                attempts++;
                
                if (attempts > 40) { // 2 minutes timeout
                    sendMessage(player, Component.text("Verification timeout", NamedTextColor.RED));
                    Bukkit.getScheduler().cancelTask(taskId[0]);
                    return;
                }
                
                try {
                    // Check specific connection status
                    Request request = new Request.Builder()
                        .url("http://localhost:4002/v2/connections?connectionId=" + connectionId)
                        .build();
                    
                    try (Response response = httpClient.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseBody = response.body().string();
                            getLogger().info("Connection status check #" + attempts);
                            
                            JsonObject connectionData = JsonParser.parseString(responseBody).getAsJsonObject();
                            String state = connectionData.get("state").getAsString();
                            
                            if ("active".equals(state) && !proofSent) {
                                proofSent = true;
                                getLogger().info("Connection is active! Sending proof request...");
                                
                                sendMessage(player, Component.text("✓ Wallet connected! Sending proof request...", NamedTextColor.GREEN));
                                
                                // Remove QR map
                                Bukkit.getScheduler().runTask(SimpleSSIPlugin.this, () -> removeQRMaps(player));
                                
                                // Send proof request
                                sendProofRequest(connectionId, player);
                                
                                // Stop this monitoring task
                                Bukkit.getScheduler().cancelTask(taskId[0]);
                                return;
                            }
                        }
                    }
                } catch (Exception e) {
                    getLogger().warning("Connection monitoring error: " + e.getMessage());
                }
            }
        }, 60L, 60L).getTaskId(); // Check every 3 seconds
    }
    
    private void sendProofRequest(String connectionId, Player player) {
        try {
            getLogger().info("Sending proof request for connection: " + connectionId);
            
            // Use ssi-tutorial verifier API approach (like in proof.controller.ts)
            JsonObject proofRequest = new JsonObject();
            proofRequest.addProperty("proofRequestlabel", "Minecraft Server Verification");
            proofRequest.addProperty("connectionId", connectionId);
            proofRequest.addProperty("version", "1.0");
            
            RequestBody body = RequestBody.create(proofRequest.toString(), MediaType.get("application/json"));
            Request httpRequest = new Request.Builder()
                .url("http://localhost:4002/v2/send-proof-request")
                .post(body)
                .build();
            
            getLogger().info("Proof request payload: " + proofRequest.toString());
            
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "No response";
                getLogger().info("Proof request response: " + response.code() + " - " + responseBody);
                
                if (response.isSuccessful()) {
                    sendMessage(player, Component.text("Proof request sent! Please approve in your wallet.", NamedTextColor.YELLOW));
                    
                    JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
                    if (responseJson.has("pres_ex_id")) {
                        String proofExchangeId = responseJson.get("pres_ex_id").getAsString();
                        monitorProofStatus(proofExchangeId, player);
                    } else {
                        // Fallback: monitor all proof records for this connection
                        monitorProofStatusByConnection(connectionId, player);
                    }
                    
                } else {
                    getLogger().warning("Proof request failed: " + response.code() + " - " + responseBody);
                    sendMessage(player, Component.text("Failed to send proof request", NamedTextColor.RED));
                }
            }
        } catch (Exception e) {
            getLogger().severe("Proof request failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private JsonArray buildFlexibleRestrictions() {
        JsonArray restrictions = new JsonArray();
        
        try {
            // Query all available credential definitions
            Request request = new Request.Builder()
                .url(acapyAdminUrl + "/credential-definitions/created")
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject responseJson = JsonParser.parseString(response.body().string()).getAsJsonObject();
                    JsonArray credDefIds = responseJson.getAsJsonArray("credential_definition_ids");
                    
                    // Add each credential definition as a valid restriction
                    for (int i = 0; i < credDefIds.size(); i++) {
                        JsonObject restriction = new JsonObject();
                        restriction.addProperty("cred_def_id", credDefIds.get(i).getAsString());
                        restrictions.add(restriction);
                    }
                    
                    getLogger().info("Built flexible restrictions with " + restrictions.size() + " credential definitions");
                }
            }
        } catch (Exception e) {
            getLogger().warning("Failed to build flexible restrictions: " + e.getMessage());
        }
        
        // If no credential definitions found, add a fallback broad restriction
        if (restrictions.size() == 0) {
            JsonObject fallback = new JsonObject();
            fallback.addProperty("schema_name", "Identity_Schema");
            restrictions.add(fallback);
            getLogger().info("Using fallback restriction: Identity_Schema");
        }
        
        return restrictions;
    }
    
    private void monitorProofStatus(String proofExchangeId, Player player) {
        final int[] taskId = new int[1];
        
        taskId[0] = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            private int attempts = 0;
            
            @Override
            public void run() {
                attempts++;
                
                if (attempts > 60) { // 3 minutes timeout  
                    sendMessage(player, Component.text("Proof verification timeout", NamedTextColor.RED));
                    Bukkit.getScheduler().cancelTask(taskId[0]);
                    return;
                }
                
                try {
                    // Check proof status using ACA-Py API
                    Request request = new Request.Builder()
                        .url(acapyAdminUrl + "/present-proof-2.0/records/" + proofExchangeId)
                        .build();
                    
                    try (Response response = httpClient.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseBody = response.body().string();
                            getLogger().info("Proof status check #" + attempts + ": " + responseBody);
                            
                            JsonObject proofData = JsonParser.parseString(responseBody).getAsJsonObject();
                            String state = proofData.get("state").getAsString();
                            
                            if ("presentation-received".equals(state) || "done".equals(state)) {
                                // Proof was received - now validate DID trust
                                String proofExchangeId = proofData.get("pres_ex_id").getAsString();
                                validateProofWithDIDCheck(proofExchangeId, player, taskId[0]);
                                return;
                                
                            } else if ("abandoned".equals(state) || "request-rejected".equals(state)) {
                                sendMessage(player, Component.text("Verification was rejected or abandoned", NamedTextColor.RED));
                                Bukkit.getScheduler().cancelTask(taskId[0]);
                                return;
                            }
                            
                            // Still waiting - continue monitoring
                            if (attempts == 1) {
                                sendMessage(player, Component.text("Please check your wallet and approve the proof request!", NamedTextColor.GOLD));
                            }
                            
                        }
                    }
                } catch (Exception e) {
                    getLogger().warning("Proof status monitoring error: " + e.getMessage());
                }
            }
        }, 60L, 60L).getTaskId(); // Check every 3 seconds
    }
    
    private void monitorProofStatusByConnection(String connectionId, Player player) {
        final int[] taskId = new int[1];
        
        taskId[0] = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            private int attempts = 0;
            
            @Override
            public void run() {
                attempts++;
                
                if (attempts > 60) { // 3 minutes timeout  
                    sendMessage(player, Component.text("Proof verification timeout", NamedTextColor.RED));
                    Bukkit.getScheduler().cancelTask(taskId[0]);
                    return;
                }
                
                try {
                    // Check all proof records to find one for this connection
                    Request request = new Request.Builder()
                        .url(acapyAdminUrl + "/present-proof-2.0/records")
                        .build();
                    
                    try (Response response = httpClient.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseBody = response.body().string();
                            getLogger().info("Proof records check #" + attempts);
                            
                            JsonObject recordsData = JsonParser.parseString(responseBody).getAsJsonObject();
                            JsonArray records = recordsData.getAsJsonArray("results");
                            
                            for (int i = 0; i < records.size(); i++) {
                                JsonObject record = records.get(i).getAsJsonObject();
                                if (record.has("connection_id") && 
                                    connectionId.equals(record.get("connection_id").getAsString())) {
                                    
                                    String state = record.get("state").getAsString();
                                    
                                    if ("presentation-received".equals(state) || "done".equals(state)) {
                                        // Proof was received - now validate DID trust
                                        String proofExchangeId = record.get("pres_ex_id").getAsString();
                                        validateProofWithDIDCheck(proofExchangeId, player, taskId[0]);
                                        return;
                                        
                                    } else if ("abandoned".equals(state) || "request-rejected".equals(state)) {
                                        sendMessage(player, Component.text("Verification was rejected or abandoned", NamedTextColor.RED));
                                        Bukkit.getScheduler().cancelTask(taskId[0]);
                                        return;
                                    }
                                }
                            }
                            
                            // Still waiting - continue monitoring
                            if (attempts == 1) {
                                sendMessage(player, Component.text("Please check your wallet and approve the proof request!", NamedTextColor.GOLD));
                            }
                        }
                    }
                } catch (Exception e) {
                    getLogger().warning("Proof status monitoring error: " + e.getMessage());
                }
            }
        }, 60L, 60L).getTaskId(); // Check every 3 seconds
    }
    
    private void validateProofWithDIDCheck(String proofExchangeId, Player player, int taskId) {
        try {
            getLogger().info("Validating proof with DID check for exchange: " + proofExchangeId);
            
            JsonObject validationRequest = new JsonObject();
            validationRequest.addProperty("proofRecordId", proofExchangeId);
            
            RequestBody body = RequestBody.create(validationRequest.toString(), MediaType.get("application/json"));
            Request httpRequest = new Request.Builder()
                .url("http://localhost:4002/v2/validate-proof")
                .post(body)
                .build();
            
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "No response";
                getLogger().info("DID validation response: " + response.code() + " - " + responseBody);
                
                if (response.isSuccessful()) {
                    JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
                    boolean success = responseJson.get("success").getAsBoolean();
                    
                    if (success) {
                        // DID is trusted - verification successful
                        String message = responseJson.has("message") ? responseJson.get("message").getAsString() : "Verification completed";
                        String issuerDID = responseJson.has("issuerDID") ? responseJson.get("issuerDID").getAsString() : "unknown";
                        
                        verifiedPlayers.put(player.getName(), true);
                        sendMessage(player, Component.text("✓ " + message, NamedTextColor.GREEN));
                        getLogger().info("Player " + player.getName() + " verified successfully by issuer: " + issuerDID);
                        
                        // Give glowing effect
                        Bukkit.getScheduler().runTask(SimpleSSIPlugin.this, () -> {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                                "effect give " + player.getName() + " minecraft:glowing 999999 0 true");
                        });
                        
                    } else {
                        // DID validation failed
                        String errorMessage = responseJson.has("error") ? responseJson.get("error").getAsString() : "Verification failed";
                        String issuerDID = responseJson.has("issuerDID") ? responseJson.get("issuerDID").getAsString() : "unknown";
                        
                        sendMessage(player, Component.text("✗ " + errorMessage, NamedTextColor.RED));
                        getLogger().warning("DID validation failed for player " + player.getName() + ": " + errorMessage + " (DID: " + issuerDID + ")");
                    }
                    
                } else {
                    // HTTP error
                    JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
                    String errorMessage = responseJson.has("error") ? responseJson.get("error").getAsString() : "Validation service error";
                    
                    sendMessage(player, Component.text("✗ " + errorMessage, NamedTextColor.RED));
                    getLogger().warning("DID validation service error for player " + player.getName() + ": " + errorMessage);
                }
                
                Bukkit.getScheduler().cancelTask(taskId);
                
            }
        } catch (Exception e) {
            getLogger().severe("DID validation failed: " + e.getMessage());
            sendMessage(player, Component.text("✗ Verification system error", NamedTextColor.RED));
            Bukkit.getScheduler().cancelTask(taskId);
            e.printStackTrace();
        }
    }
    
    private void createWebVerification(Player player) {
        try {
            getLogger().info("Creating web verification for player: " + player.getName() + " (Browser popup mode)");
            
            String playerName = player.getName();
            String playerUUID = player.getUniqueId().toString();
            
            // Launch browser popup and send proof request
            launchBrowserPopup(playerName, playerUUID, player);
            
        } catch (Exception e) {
            getLogger().severe("Web verification failed for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            sendMessage(player, Component.text("Web verification system error", NamedTextColor.RED));
        }
    }
    
    private void launchBrowserPopup(String playerName, String playerUUID, Player player) {
        try {
            getLogger().info("Opening in-game web browser for player: " + playerName);
            
            String initialSessionId = "web_verify_" + System.currentTimeMillis();
            
            // Send proof request to web wallet first and get the actual verification ID
            String actualVerificationId = sendDirectProofRequestToWebWallet(playerName, playerUUID, initialSessionId, player);
            
            if (actualVerificationId == null) {
                sendMessage(player, Component.text("❌ Failed to create verification session", NamedTextColor.RED));
                return;
            }
            
            // Then open in-game web browser
            sendMessage(player, Component.text("📱 Opening Web Browser...", NamedTextColor.GREEN));
            sendMessage(player, Component.text("→ Phone-shaped browser window will appear", NamedTextColor.GRAY));
            sendMessage(player, Component.text("→ Real HTML/CSS web interface", NamedTextColor.GRAY));
            sendMessage(player, Component.text("→ Window will auto-close after verification", NamedTextColor.GRAY));
            
            // Open the simple web browser that works reliably
            SimpleWebBrowser webBrowser = new SimpleWebBrowser(this, player, actualVerificationId, httpClient);
            webBrowser.openWebWallet();
            
            // Monitor verification session using the actual verification ID from web wallet
            getLogger().info("[SSIVerification] Starting monitoring for verification ID: " + actualVerificationId);
            monitorWebVerificationSession(actualVerificationId, player);
            
        } catch (Exception e) {
            getLogger().severe("Failed to open in-game web browser: " + e.getMessage());
            e.printStackTrace();
            sendMessage(player, Component.text("❌ Failed to open web browser", NamedTextColor.RED));
            sendMessage(player, Component.text("→ Make sure JavaFX is available", NamedTextColor.GRAY));
        }
    }
    
    private String sendDirectProofRequestToWebWallet(String playerName, String playerUUID, String verificationSessionId, Player player) {
        try {
            getLogger().info("Sending direct proof request to web wallet for player: " + playerName);
            
            // Send proof request notification to web wallet (Bifold-compatible format)
            JsonObject webProofRequest = new JsonObject();
            webProofRequest.addProperty("type", "web_proof_request");
            webProofRequest.addProperty("verificationSessionId", verificationSessionId);
            webProofRequest.addProperty("title", "Minecraft Web Verification");
            webProofRequest.addProperty("message", playerName + " requests verification via web wallet (Bifold-compatible)");
            webProofRequest.addProperty("timestamp", java.time.Instant.now().toString());
            
            // Add requester object as expected by web wallet API
            JsonObject requester = new JsonObject();
            requester.addProperty("playerName", playerName);
            requester.addProperty("playerUUID", playerUUID);
            webProofRequest.add("requester", requester);
            
            // Add requestedAttributes array as expected by web wallet API
            JsonArray requestedAttributesArray = new JsonArray();
            requestedAttributesArray.add("name"); 
            requestedAttributesArray.add("email");
            requestedAttributesArray.add("department");
            requestedAttributesArray.add("issuer_did");
            requestedAttributesArray.add("age");
            webProofRequest.add("requestedAttributes", requestedAttributesArray);
            
            // Include proof request details (like what Bifold receives)
            JsonObject proofRequestData = new JsonObject();
            proofRequestData.addProperty("name", "Minecraft Web Verification");
            proofRequestData.addProperty("version", "1.0");
            
            JsonObject requestedAttributes = new JsonObject();
            JsonObject nameAttr = new JsonObject();
            nameAttr.addProperty("name", "name");
            requestedAttributes.add("attr_name", nameAttr);
            
            JsonObject emailAttr = new JsonObject();
            emailAttr.addProperty("name", "email");
            requestedAttributes.add("attr_email", emailAttr);
            
            JsonObject deptAttr = new JsonObject();
            deptAttr.addProperty("name", "department");
            requestedAttributes.add("attr_department", deptAttr);
            
            JsonObject issuerAttr = new JsonObject();
            issuerAttr.addProperty("name", "issuer_did");
            requestedAttributes.add("attr_issuer_did", issuerAttr);
            
            JsonObject ageAttr = new JsonObject();
            ageAttr.addProperty("name", "age");
            requestedAttributes.add("attr_age", ageAttr);
            
            proofRequestData.add("requested_attributes", requestedAttributes);
            proofRequestData.add("requested_predicates", new JsonObject());
            
            webProofRequest.add("proofRequestData", proofRequestData);
            
            RequestBody body = RequestBody.create(webProofRequest.toString(), MediaType.get("application/json"));
            Request httpRequest = new Request.Builder()
                .url("http://localhost:3001/api/minecraft/verify")
                .post(body)
                .build();
            
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "No response";
                getLogger().info("Web wallet proof request response: " + response.code() + " - " + responseBody);
                
                if (response.isSuccessful()) {
                    getLogger().info("Proof request sent successfully to web wallet");
                    
                    // Extract verificationId from response
                    try {
                        JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
                        if (responseJson.has("verificationId")) {
                            String actualVerificationId = responseJson.get("verificationId").getAsString();
                            getLogger().info("[SSIVerification] Extracted verification ID: " + actualVerificationId);
                            return actualVerificationId;
                        }
                    } catch (Exception parseEx) {
                        getLogger().warning("Failed to parse verification ID from response: " + parseEx.getMessage());
                    }
                } else {
                    getLogger().warning("Failed to send proof request to web wallet: " + responseBody);
                }
            }
        } catch (Exception e) {
            getLogger().severe("Failed to send proof request to web wallet: " + e.getMessage());
            e.printStackTrace();
            sendMessage(player, Component.text("Web proof request system error", NamedTextColor.RED));
        }
        
        return null; // Return null if failed to get verification ID
    }
    
    private void sendInvitationToWebWallet(String invitationUrl, String connectionId, String playerName, String playerUUID, Player player) {
        try {
            getLogger().info("Sending invitation to web wallet for connection: " + connectionId);
            
            // Send invitation to web wallet for automatic acceptance
            JsonObject webWalletRequest = new JsonObject();
            webWalletRequest.addProperty("type", "web_invitation");
            webWalletRequest.addProperty("invitationUrl", invitationUrl);
            webWalletRequest.addProperty("connectionId", connectionId);
            webWalletRequest.addProperty("playerName", playerName);
            webWalletRequest.addProperty("playerUUID", playerUUID);
            
            RequestBody body = RequestBody.create(webWalletRequest.toString(), MediaType.get("application/json"));
            Request httpRequest = new Request.Builder()
                .url("http://localhost:3001/api/acapy/accept-invitation")
                .post(body)
                .build();
            
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "No response";
                getLogger().info("Web wallet invitation response: " + response.code() + " - " + responseBody);
                
                if (response.isSuccessful()) {
                    sendMessage(player, Component.text("✓ Invitation sent to web wallet!", NamedTextColor.GREEN));
                    sendMessage(player, Component.text("→ Check your browser at localhost:3001", NamedTextColor.GRAY));
                    sendMessage(player, Component.text("→ Connection will be established automatically", NamedTextColor.GRAY));
                } else {
                    getLogger().warning("Failed to send invitation to web wallet: " + responseBody);
                    sendMessage(player, Component.text("Failed to send invitation to web wallet", NamedTextColor.RED));
                }
            }
        } catch (Exception e) {
            getLogger().severe("Failed to send invitation to web wallet: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void monitorWebConnection(String connectionId, Player player) {
        final int[] taskId = new int[1];
        
        taskId[0] = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            private int attempts = 0;
            private boolean proofSent = false;
            
            @Override
            public void run() {
                attempts++;
                
                if (attempts > 40) { // 2 minutes timeout
                    sendMessage(player, Component.text("Web wallet connection timeout", NamedTextColor.RED));
                    Bukkit.getScheduler().cancelTask(taskId[0]);
                    return;
                }
                
                try {
                    // Check connection status (same as regular /verify)
                    Request request = new Request.Builder()
                        .url("http://localhost:4002/v2/connections?connectionId=" + connectionId)
                        .build();
                    
                    try (Response response = httpClient.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseBody = response.body().string();
                            getLogger().info("Web connection status check #" + attempts);
                            
                            JsonObject connectionData = JsonParser.parseString(responseBody).getAsJsonObject();
                            String state = connectionData.get("state").getAsString();
                            
                            if ("active".equals(state) && !proofSent) {
                                proofSent = true;
                                getLogger().info("Web connection is active! Sending proof request...");
                                
                                sendMessage(player, Component.text("✓ Web wallet connected! Sending proof request...", NamedTextColor.GREEN));
                                
                                // Send proof request (same as regular /verify)
                                sendProofRequest(connectionId, player);
                                
                                // Stop this monitoring task
                                Bukkit.getScheduler().cancelTask(taskId[0]);
                                return;
                            }
                        }
                    }
                } catch (Exception e) {
                    getLogger().warning("Web connection monitoring error: " + e.getMessage());
                }
            }
        }, 60L, 60L).getTaskId(); // Check every 3 seconds
    }
    
    private void monitorWebVerificationSession(String verificationSessionId, Player player) {
        getLogger().info("[SSIVerification] Starting verification monitoring for session: " + verificationSessionId);
        final int[] taskId = new int[1];
        
        taskId[0] = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            private int attempts = 0;
            private final int maxAttempts = 100; // 5 minutes
            
            @Override
            public void run() {
                attempts++;
                getLogger().info("[SSIVerification] Monitoring attempt " + attempts + " for session: " + verificationSessionId);
                
                try {
                    String monitorUrl = "http://localhost:3001/api/minecraft/verify/" + verificationSessionId;
                    getLogger().info("[SSIVerification] Checking status at: " + monitorUrl);
                    
                    Request request = new Request.Builder()
                        .url(monitorUrl)
                        .build();
                    
                    try (Response response = httpClient.newCall(request).execute()) {
                        getLogger().info("[SSIVerification] Monitoring response code: " + response.code());
                        
                        if (response.isSuccessful() && response.body() != null) {
                            String responseBody = response.body().string();
                            getLogger().info("[SSIVerification] Monitoring response body: " + responseBody);
                            
                            if (responseBody.contains("\"status\":\"verified\"") || responseBody.contains("\"verified\":true")) {
                                // Verification successful with trust validation!
                                verifiedPlayers.put(player.getName(), true);
                                
                                sendMessage(player, Component.text("🎉 Web wallet verification completed!", NamedTextColor.GREEN));
                                sendMessage(player, Component.text("📜 Your DID has been validated as trusted!", NamedTextColor.YELLOW));
                                sendMessage(player, Component.text("🔗 Bifold-compatible verification successful!", NamedTextColor.GRAY));
                                
                                // Give glowing effect
                                Bukkit.getScheduler().runTask(SimpleSSIPlugin.this, () -> {
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                                        "effect give " + player.getName() + " minecraft:glowing 999999 0 true");
                                });
                                
                                Bukkit.getScheduler().cancelTask(taskId[0]);
                                return;
                                
                            } else if (responseBody.contains("\"status\":\"failed\"")) {
                                // Extract failure reason
                                String failureReason = "Verification failed - DID not trusted";
                                try {
                                    if (responseBody.contains("\"message\"")) {
                                        String messageStart = "\"message\":\"";
                                        int startIndex = responseBody.indexOf(messageStart);
                                        if (startIndex != -1) {
                                            startIndex += messageStart.length();
                                            int endIndex = responseBody.indexOf("\"", startIndex);
                                            if (endIndex != -1) {
                                                failureReason = responseBody.substring(startIndex, endIndex);
                                                failureReason = failureReason.replace("\\\"", "\"").replace("\\n", " ");
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    getLogger().warning("Failed to parse failure reason: " + e.getMessage());
                                }
                                
                                sendMessage(player, Component.text("❌ " + failureReason, NamedTextColor.RED));
                                sendMessage(player, Component.text("→ Your DID may not be in the trusted list", NamedTextColor.GRAY));
                                
                                Bukkit.getScheduler().cancelTask(taskId[0]);
                                return;
                                
                            } else if (responseBody.contains("\"status\":\"declined\"")) {
                                // User declined - stop silently
                                getLogger().info("[SSIVerification] User declined verification");
                                Bukkit.getScheduler().cancelTask(taskId[0]);
                                return;
                            } else {
                                getLogger().info("[SSIVerification] Status still pending, continuing to monitor...");
                            }
                        } else {
                            getLogger().warning("[SSIVerification] Monitoring response not successful or no body. Code: " + response.code());
                        }
                    }
                } catch (Exception e) {
                    getLogger().warning("[SSIVerification] Failed to check web verification status: " + e.getMessage());
                    e.printStackTrace();
                }
                
                // Timeout
                if (attempts >= maxAttempts) {
                    sendMessage(player, Component.text("⏰ Web wallet verification timeout (5 minutes)", NamedTextColor.RED));
                    sendMessage(player, Component.text("→ Try /verify web again", NamedTextColor.GRAY));
                    Bukkit.getScheduler().cancelTask(taskId[0]);
                }
            }
        }, 60L, 60L).getTaskId();
    }
    
    private void startVerificationMonitoring(String verificationId, Player player) {
        final int[] taskId = new int[1];
        
        taskId[0] = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            private int attempts = 0;
            private final int maxAttempts = 100; // 5 minutes
            
            @Override
            public void run() {
                attempts++;
                
                try {
                    Request request = new Request.Builder()
                        .url("http://localhost:3001/api/minecraft/verify/" + verificationId)
                        .build();
                    
                    try (Response response = httpClient.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseBody = response.body().string();
                            
                            if (responseBody.contains("\"status\":\"verified\"") || responseBody.contains("\"verified\":true")) {
                                // Verification successful with ACA-Py trust validation!
                                verifiedPlayers.put(player.getName(), true);
                                
                                sendMessage(player, Component.text("🎉 Web wallet verification completed with ACA-Py trust validation!", NamedTextColor.GREEN));
                                sendMessage(player, Component.text("📜 Your DID has been verified as trusted!", NamedTextColor.YELLOW));
                                sendMessage(player, Component.text("→ You now have verified player benefits", NamedTextColor.GRAY));
                                
                                // Give glowing effect
                                Bukkit.getScheduler().runTask(SimpleSSIPlugin.this, () -> {
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                                        "effect give " + player.getName() + " minecraft:glowing 999999 0 true");
                                });
                                
                                Bukkit.getScheduler().cancelTask(taskId[0]);
                                return;
                                
                            } else if (responseBody.contains("\"status\":\"failed\"")) {
                                // Extract failure reason
                                String failureReason = "Verification failed - DID not trusted by ACA-Py";
                                try {
                                    if (responseBody.contains("\"message\"")) {
                                        String messageStart = "\"message\":\"";
                                        int startIndex = responseBody.indexOf(messageStart);
                                        if (startIndex != -1) {
                                            startIndex += messageStart.length();
                                            int endIndex = responseBody.indexOf("\"", startIndex);
                                            if (endIndex != -1) {
                                                failureReason = responseBody.substring(startIndex, endIndex);
                                                failureReason = failureReason.replace("\\\"", "\"").replace("\\n", " ");
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    getLogger().warning("Failed to parse failure reason: " + e.getMessage());
                                }
                                
                                sendMessage(player, Component.text("❌ " + failureReason, NamedTextColor.RED));
                                sendMessage(player, Component.text("→ Your DID may not be in the trusted list", NamedTextColor.GRAY));
                                sendMessage(player, Component.text("→ Check admin interface at localhost:3000/admin", NamedTextColor.GRAY));
                                
                                Bukkit.getScheduler().cancelTask(taskId[0]);
                                return;
                                
                            } else if (responseBody.contains("\"status\":\"declined\"")) {
                                // User declined - stop silently
                                Bukkit.getScheduler().cancelTask(taskId[0]);
                                return;
                            }
                        }
                    }
                } catch (Exception e) {
                    getLogger().warning("Failed to check web verification status: " + e.getMessage());
                }
                
                // Timeout
                if (attempts >= maxAttempts) {
                    sendMessage(player, Component.text("⏰ Web wallet verification timeout (5 minutes)", NamedTextColor.RED));
                    sendMessage(player, Component.text("→ Try /verify web again", NamedTextColor.GRAY));
                    sendMessage(player, Component.text("→ Or use /verify for mobile wallet", NamedTextColor.GRAY));
                    Bukkit.getScheduler().cancelTask(taskId[0]);
                }
            }
        }, 60L, 60L).getTaskId();
    }
    
    private void removeQRMaps(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.FILLED_MAP) {
                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                    item.getItemMeta().getDisplayName().contains("SSI Verification")) {
                    player.getInventory().setItem(i, null);
                    break;
                }
            }
        }
    }
    
    private void sendMessage(Player player, Component message) {
        Bukkit.getScheduler().runTask(this, () -> player.sendMessage(message));
    }
    
    private void handleSSIVerify(Player sender, String targetPlayerName) {
        sender.sendMessage(Component.text("=== Verification Status ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Player: " + targetPlayerName, NamedTextColor.WHITE));
        
        if (verifiedPlayers.getOrDefault(targetPlayerName, false)) {
            sender.sendMessage(Component.text("Status: ✓ VERIFIED", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Status: ✗ NOT VERIFIED", NamedTextColor.RED));
        }
    }
    
    private void handleReset(Player player) {
        String playerName = player.getName();
        
        // Check if player is currently verified
        if (!verifiedPlayers.getOrDefault(playerName, false)) {
            player.sendMessage(Component.text("⚠ You are not currently verified!", NamedTextColor.YELLOW));
            return;
        }
        
        // Remove verification status
        verifiedPlayers.remove(playerName);
        
        // Remove glowing effect if present
        player.removePotionEffect(PotionEffectType.GLOWING);
        
        // Send confirmation message
        player.sendMessage(Component.text("🔄 Verification status reset!", NamedTextColor.GOLD));
        player.sendMessage(Component.text("You can now use /verify to complete the trust triangle again.", NamedTextColor.GRAY));
        
        getLogger().info("Player " + playerName + " reset their verification status");
    }
}