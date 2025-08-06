package com.ssi.verification;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class WebWalletGUI implements Listener {
    
    private final JavaPlugin plugin;
    private final OkHttpClient httpClient;
    private final String verificationSessionId;
    private final Player player;
    private Inventory gui;
    private JsonArray notifications;
    private JsonArray credentials;
    private String selectedNotificationId;
    private String selectedCredentialId;
    
    public WebWalletGUI(JavaPlugin plugin, OkHttpClient httpClient, String verificationSessionId, Player player) {
        this.plugin = plugin;
        this.httpClient = httpClient;
        this.verificationSessionId = verificationSessionId;
        this.player = player;
        
        // Register this as an event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    public void openWebWallet() {
        player.sendMessage(Component.text("📱 Opening Web Wallet Interface...", NamedTextColor.YELLOW));
        
        CompletableFuture.runAsync(() -> {
            try {
                // Fetch notifications and credentials from web wallet API
                fetchWebWalletData().thenRun(() -> {
                    Bukkit.getScheduler().runTask(plugin, this::createGUI);
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to fetch web wallet data: " + e.getMessage());
                e.printStackTrace();
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(Component.text("❌ Failed to load web wallet", NamedTextColor.RED));
                });
            }
        });
    }
    
    private CompletableFuture<Void> fetchWebWalletData() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Fetch notifications
                Request notificationsRequest = new Request.Builder()
                    .url("http://localhost:3001/api/notifications")
                    .build();
                
                try (Response response = httpClient.newCall(notificationsRequest).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
                        notifications = responseJson.getAsJsonArray("notifications");
                        plugin.getLogger().info("Fetched " + notifications.size() + " notifications");
                    } else {
                        notifications = new JsonArray();
                    }
                }
                
                // Fetch credentials
                Request credentialsRequest = new Request.Builder()
                    .url("http://localhost:3001/api/credentials")
                    .build();
                
                try (Response response = httpClient.newCall(credentialsRequest).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
                        credentials = responseJson.getAsJsonArray("credentials");
                        plugin.getLogger().info("Fetched " + credentials.size() + " credentials");
                    } else {
                        credentials = new JsonArray();
                    }
                }
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error fetching web wallet data: " + e.getMessage());
                e.printStackTrace();
                notifications = new JsonArray();
                credentials = new JsonArray();
            }
        });
    }
    
    private void createGUI() {
        // Create phone-screen shaped inventory (9x6 = 54 slots, phone-like aspect ratio)
        gui = Bukkit.createInventory(null, 54, Component.text("📱 Web Wallet - Notifications", NamedTextColor.BLUE, TextDecoration.BOLD));
        
        // Fill borders to create phone screen effect
        createPhoneBorder();
        
        // Add notifications
        displayNotifications();
        
        // Add navigation buttons
        addNavigationButtons();
        
        // Open GUI for player
        player.openInventory(gui);
    }
    
    private void createPhoneBorder() {
        // Create black glass panes for phone border
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.text(" ", NamedTextColor.BLACK));
        border.setItemMeta(borderMeta);
        
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, border); // Top row
            gui.setItem(45 + i, border); // Bottom row
        }
        
        // Left and right columns
        for (int row = 1; row < 5; row++) {
            gui.setItem(row * 9, border); // Left column
            gui.setItem(row * 9 + 8, border); // Right column
        }
    }
    
    private void displayNotifications() {
        if (notifications == null || notifications.size() == 0) {
            // No notifications - show empty state
            ItemStack emptyState = new ItemStack(Material.PAPER);
            ItemMeta emptyMeta = emptyState.getItemMeta();
            emptyMeta.displayName(Component.text("📭 No Notifications", NamedTextColor.GRAY, TextDecoration.BOLD));
            emptyMeta.lore(Arrays.asList(
                Component.text("No proof requests or credentials", NamedTextColor.DARK_GRAY),
                Component.text("waiting for your attention.", NamedTextColor.DARK_GRAY)
            ));
            emptyState.setItemMeta(emptyMeta);
            gui.setItem(22, emptyState); // Center position
            return;
        }
        
        // Display notifications (limit to visible area)
        int startSlot = 10; // First inner slot
        int maxNotifications = 7; // Max notifications to show at once
        
        for (int i = 0; i < Math.min(notifications.size(), maxNotifications); i++) {
            JsonObject notification = notifications.get(i).getAsJsonObject();
            String type = notification.get("type").getAsString();
            String id = notification.get("id").getAsString();
            
            ItemStack notificationItem;
            List<Component> lore = new ArrayList<>();
            
            if ("proof-request".equals(type)) {
                // Proof request notification
                notificationItem = new ItemStack(Material.WRITTEN_BOOK);
                ItemMeta meta = notificationItem.getItemMeta();
                meta.displayName(Component.text("🔐 Proof Request", NamedTextColor.GOLD, TextDecoration.BOLD));
                
                // Add request details to lore
                lore.add(Component.text("Minecraft Verification Request", NamedTextColor.YELLOW));
                lore.add(Component.text(""));
                
                if (notification.has("proofRequestData")) {
                    JsonObject proofData = notification.getAsJsonObject("proofRequestData");
                    if (proofData.has("minecraftPlayer")) {
                        JsonObject playerInfo = proofData.getAsJsonObject("minecraftPlayer");
                        lore.add(Component.text("Player: " + playerInfo.get("playerName").getAsString(), NamedTextColor.WHITE));
                    }
                }
                
                lore.add(Component.text(""));
                lore.add(Component.text("📝 Required Attributes:", NamedTextColor.AQUA));
                lore.add(Component.text("• Name", NamedTextColor.GRAY));
                lore.add(Component.text("• Email", NamedTextColor.GRAY));
                lore.add(Component.text("• Department", NamedTextColor.GRAY));
                lore.add(Component.text("• Issuer DID", NamedTextColor.GRAY));
                lore.add(Component.text("• Age", NamedTextColor.GRAY));
                lore.add(Component.text(""));
                lore.add(Component.text("🖱️ Click to view options", NamedTextColor.GREEN));
                
                meta.lore(lore);
                notificationItem.setItemMeta(meta);
                
            } else if ("credential-offer".equals(type)) {
                // Credential offer notification
                notificationItem = new ItemStack(Material.ENCHANTED_BOOK);
                ItemMeta meta = notificationItem.getItemMeta();
                meta.displayName(Component.text("🎓 Credential Offer", NamedTextColor.BLUE, TextDecoration.BOLD));
                
                lore.add(Component.text("New credential available", NamedTextColor.AQUA));
                lore.add(Component.text(""));
                lore.add(Component.text("🖱️ Click to accept", NamedTextColor.GREEN));
                
                meta.lore(lore);
                notificationItem.setItemMeta(meta);
                
            } else {
                // Generic notification
                notificationItem = new ItemStack(Material.PAPER);
                ItemMeta meta = notificationItem.getItemMeta();
                meta.displayName(Component.text("📄 Notification", NamedTextColor.WHITE));
                meta.lore(Arrays.asList(Component.text("Type: " + type, NamedTextColor.GRAY)));
                notificationItem.setItemMeta(meta);
            }
            
            // Store notification ID in the item's custom data (using display name trick)
            ItemMeta meta = notificationItem.getItemMeta();
            List<Component> currentLore = meta.lore();
            if (currentLore == null) currentLore = new ArrayList<>();
            currentLore.add(Component.text("ID:" + id, NamedTextColor.BLACK).decoration(TextDecoration.ITALIC, false));
            meta.lore(currentLore);
            notificationItem.setItemMeta(meta);
            
            gui.setItem(startSlot + i, notificationItem);
        }
    }
    
    private void addNavigationButtons() {
        // Close button (red wool)
        ItemStack closeButton = new ItemStack(Material.RED_WOOL);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.displayName(Component.text("❌ Close Wallet", NamedTextColor.RED, TextDecoration.BOLD));
        closeMeta.lore(Arrays.asList(Component.text("Close the web wallet interface", NamedTextColor.DARK_RED)));
        closeButton.setItemMeta(closeMeta);
        gui.setItem(53, closeButton); // Bottom right corner
        
        // Refresh button (green wool)
        ItemStack refreshButton = new ItemStack(Material.LIME_WOOL);
        ItemMeta refreshMeta = refreshButton.getItemMeta();
        refreshMeta.displayName(Component.text("🔄 Refresh", NamedTextColor.GREEN, TextDecoration.BOLD));
        refreshMeta.lore(Arrays.asList(Component.text("Reload notifications and credentials", NamedTextColor.DARK_GREEN)));
        refreshButton.setItemMeta(refreshMeta);
        gui.setItem(46, refreshButton); // Bottom left corner
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != gui) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getWhoClicked().equals(player)) return;
        
        event.setCancelled(true); // Prevent item movement
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        // Check if this is the credential selection GUI
        String title = event.getView().getTitle();
        if (title.contains("Select Credential")) {
            handleCredentialSelectionClick(event);
            return;
        }
        
        // Handle main GUI button clicks
        if (clickedItem.getType() == Material.RED_WOOL) {
            // Close button
            player.closeInventory();
            player.sendMessage(Component.text("📱 Web wallet closed", NamedTextColor.GRAY));
            return;
        }
        
        if (clickedItem.getType() == Material.LIME_WOOL) {
            // Refresh button
            player.sendMessage(Component.text("🔄 Refreshing web wallet...", NamedTextColor.YELLOW));
            fetchWebWalletData().thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    displayNotifications();
                    player.sendMessage(Component.text("✅ Web wallet refreshed", NamedTextColor.GREEN));
                });
            });
            return;
        }
        
        // Handle notification clicks
        if (clickedItem.getType() == Material.WRITTEN_BOOK || clickedItem.getType() == Material.ENCHANTED_BOOK) {
            handleNotificationClick(clickedItem);
        }
    }
    
    private void handleNotificationClick(ItemStack item) {
        // Extract notification ID from lore
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null) return;
        
        String notificationId = null;
        for (Component line : lore) {
            String text = ((net.kyori.adventure.text.TextComponent) line).content();
            if (text.startsWith("ID:")) {
                notificationId = text.substring(3);
                break;
            }
        }
        
        if (notificationId == null) return;
        
        selectedNotificationId = notificationId;
        
        // Find the notification
        JsonObject selectedNotification = null;
        for (JsonElement element : notifications) {
            JsonObject notification = element.getAsJsonObject();
            if (notification.get("id").getAsString().equals(notificationId)) {
                selectedNotification = notification;
                break;
            }
        }
        
        if (selectedNotification == null) return;
        
        String type = selectedNotification.get("type").getAsString();
        
        if ("proof-request".equals(type)) {
            showCredentialSelectionGUI();
        } else if ("credential-offer".equals(type)) {
            acceptCredentialOffer(notificationId);
        }
    }
    
    private void showCredentialSelectionGUI() {
        // Create credential selection GUI
        Inventory credentialGUI = Bukkit.createInventory(null, 54, Component.text("📱 Select Credential to Share", NamedTextColor.BLUE, TextDecoration.BOLD));
        
        // Create phone border
        createPhoneBorderFor(credentialGUI);
        
        // Display available credentials
        if (credentials != null && credentials.size() > 0) {
            int startSlot = 10;
            int maxCredentials = 7;
            
            for (int i = 0; i < Math.min(credentials.size(), maxCredentials); i++) {
                JsonObject credential = credentials.get(i).getAsJsonObject();
                String id = credential.get("id").getAsString();
                
                ItemStack credentialItem = new ItemStack(Material.EMERALD);
                ItemMeta meta = credentialItem.getItemMeta();
                meta.displayName(Component.text("💳 Credential", NamedTextColor.GREEN, TextDecoration.BOLD));
                
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("ID: " + id.substring(0, Math.min(8, id.length())), NamedTextColor.GRAY));
                
                // Add credential attributes
                if (credential.has("attributes")) {
                    JsonArray attributes = credential.getAsJsonArray("attributes");
                    lore.add(Component.text(""));
                    lore.add(Component.text("📝 Attributes:", NamedTextColor.AQUA));
                    
                    for (JsonElement attrElement : attributes) {
                        JsonObject attr = attrElement.getAsJsonObject();
                        String name = attr.get("name").getAsString();
                        String value = attr.get("value").getAsString();
                        lore.add(Component.text("• " + name + ": " + value, NamedTextColor.WHITE));
                    }
                }
                
                lore.add(Component.text(""));
                lore.add(Component.text("🖱️ Click to share this credential", NamedTextColor.GREEN));
                lore.add(Component.text("ID:" + id, NamedTextColor.BLACK).decoration(TextDecoration.ITALIC, false));
                
                meta.lore(lore);
                credentialItem.setItemMeta(meta);
                
                credentialGUI.setItem(startSlot + i, credentialItem);
            }
        } else {
            // No credentials available
            ItemStack noCredentials = new ItemStack(Material.BARRIER);
            ItemMeta meta = noCredentials.getItemMeta();
            meta.displayName(Component.text("❌ No Credentials", NamedTextColor.RED, TextDecoration.BOLD));
            meta.lore(Arrays.asList(
                Component.text("You don't have any credentials", NamedTextColor.DARK_RED),
                Component.text("that match this request.", NamedTextColor.DARK_RED)
            ));
            noCredentials.setItemMeta(meta);
            credentialGUI.setItem(22, noCredentials);
        }
        
        // Add back and decline buttons
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.displayName(Component.text("← Back", NamedTextColor.YELLOW, TextDecoration.BOLD));
        backMeta.lore(Arrays.asList(Component.text("Return to notifications", NamedTextColor.GOLD)));
        backButton.setItemMeta(backMeta);
        credentialGUI.setItem(46, backButton);
        
        ItemStack declineButton = new ItemStack(Material.RED_WOOL);
        ItemMeta declineMeta = declineButton.getItemMeta();
        declineMeta.displayName(Component.text("❌ Decline Request", NamedTextColor.RED, TextDecoration.BOLD));
        declineMeta.lore(Arrays.asList(Component.text("Decline this proof request", NamedTextColor.DARK_RED)));
        declineButton.setItemMeta(declineMeta);
        credentialGUI.setItem(53, declineButton);
        
        // Update GUI reference and open
        gui = credentialGUI;
        player.openInventory(credentialGUI);
    }
    
    private void createPhoneBorderFor(Inventory inventory) {
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.text(" ", NamedTextColor.BLACK));
        border.setItemMeta(borderMeta);
        
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(45 + i, border);
        }
        
        // Left and right columns
        for (int row = 1; row < 5; row++) {
            inventory.setItem(row * 9, border);
            inventory.setItem(row * 9 + 8, border);
        }
    }
    
    private void acceptCredentialOffer(String notificationId) {
        player.sendMessage(Component.text("✅ Accepting credential offer...", NamedTextColor.GREEN));
        
        CompletableFuture.runAsync(() -> {
            try {
                // Send accept request to web wallet API
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("action", "accept");
                
                RequestBody body = RequestBody.create(requestBody.toString(), MediaType.get("application/json"));
                Request request = new Request.Builder()
                    .url("http://localhost:3001/api/notifications/" + notificationId)
                    .patch(body)
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (response.isSuccessful()) {
                            player.sendMessage(Component.text("🎉 Credential accepted and stored!", NamedTextColor.GREEN));
                            player.closeInventory();
                        } else {
                            player.sendMessage(Component.text("❌ Failed to accept credential", NamedTextColor.RED));
                        }
                    });
                }
                
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to accept credential: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(Component.text("❌ Error accepting credential", NamedTextColor.RED));
                });
            }
        });
    }
    
    private void shareCredential(String credentialId) {
        player.sendMessage(Component.text("📤 Sharing credential...", NamedTextColor.YELLOW));
        
        CompletableFuture.runAsync(() -> {
            try {
                // Send share request to web wallet API
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("action", "accept");
                requestBody.addProperty("credentialId", credentialId);
                
                RequestBody body = RequestBody.create(requestBody.toString(), MediaType.get("application/json"));
                Request request = new Request.Builder()
                    .url("http://localhost:3001/api/notifications/" + selectedNotificationId)
                    .patch(body)
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (response.isSuccessful()) {
                            player.sendMessage(Component.text("🎉 Credential shared successfully!", NamedTextColor.GREEN));
                            player.sendMessage(Component.text("🔐 Verification in progress...", NamedTextColor.YELLOW));
                            player.closeInventory();
                        } else {
                            player.sendMessage(Component.text("❌ Failed to share credential", NamedTextColor.RED));
                        }
                    });
                }
                
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to share credential: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(Component.text("❌ Error sharing credential", NamedTextColor.RED));
                });
            }
        });
    }
    
    private void declineProofRequest() {
        player.sendMessage(Component.text("❌ Declining proof request...", NamedTextColor.RED));
        
        CompletableFuture.runAsync(() -> {
            try {
                // Send decline request to web wallet API
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("action", "decline");
                
                RequestBody body = RequestBody.create(requestBody.toString(), MediaType.get("application/json"));
                Request request = new Request.Builder()
                    .url("http://localhost:3001/api/notifications/" + selectedNotificationId)
                    .patch(body)
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (response.isSuccessful()) {
                            player.sendMessage(Component.text("❌ Proof request declined", NamedTextColor.GRAY));
                            player.closeInventory();
                        } else {
                            player.sendMessage(Component.text("❌ Failed to decline request", NamedTextColor.RED));
                        }
                    });
                }
                
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to decline proof request: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(Component.text("❌ Error declining request", NamedTextColor.RED));
                });
            }
        });
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() == gui && event.getPlayer().equals(player)) {
            // Unregister this listener when GUI is closed
            InventoryClickEvent.getHandlerList().unregister(this);
            InventoryCloseEvent.getHandlerList().unregister(this);
        }
    }
    
    // Handle credential selection GUI clicks
    public void handleCredentialSelectionClick(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;
        
        if (clickedItem.getType() == Material.ARROW) {
            // Back button - return to notifications
            createGUI();
            return;
        }
        
        if (clickedItem.getType() == Material.RED_WOOL) {
            // Decline button
            declineProofRequest();
            return;
        }
        
        if (clickedItem.getType() == Material.EMERALD) {
            // Credential selected - extract ID and share
            ItemMeta meta = clickedItem.getItemMeta();
            List<Component> lore = meta.lore();
            if (lore == null) return;
            
            String credentialId = null;
            for (Component line : lore) {
                String text = ((net.kyori.adventure.text.TextComponent) line).content();
                if (text.startsWith("ID:")) {
                    credentialId = text.substring(3);
                    break;
                }
            }
            
            if (credentialId != null) {
                shareCredential(credentialId);
            }
        }
    }
}