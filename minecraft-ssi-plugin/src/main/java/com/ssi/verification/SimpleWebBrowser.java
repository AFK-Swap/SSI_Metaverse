package com.ssi.verification;

import com.google.gson.JsonObject;
import okhttp3.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class SimpleWebBrowser {
    
    private final JavaPlugin plugin;
    private final Player player;
    private final String verificationSessionId;
    private final OkHttpClient httpClient;
    private JFrame browserFrame;
    private JTextPane contentPane;
    private Timer refreshTimer;
    
    public SimpleWebBrowser(JavaPlugin plugin, Player player, String verificationSessionId, OkHttpClient httpClient) {
        this.plugin = plugin;
        this.player = player;
        this.verificationSessionId = verificationSessionId;
        this.httpClient = httpClient;
    }
    
    public void openWebWallet() {
        SwingUtilities.invokeLater(() -> {
            try {
                createBrowserWindow();
                loadContent();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create web browser window: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    private void createBrowserWindow() {
        // Create phone-shaped window
        browserFrame = new JFrame("📱 Web Wallet - " + player.getName());
        browserFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        // Phone dimensions (9:16 aspect ratio)
        int width = 375;
        int height = 667;
        browserFrame.setSize(width, height);
        browserFrame.setResizable(false);
        
        // Center the window on screen
        browserFrame.setLocationRelativeTo(null);
        
        // Create rounded phone-like appearance
        browserFrame.setUndecorated(true);
        browserFrame.setShape(new RoundRectangle2D.Float(0, 0, width, height, 25, 25));
        
        // Create main container
        JPanel phoneContainer = new JPanel(new BorderLayout());
        phoneContainer.setBackground(Color.BLACK);
        phoneContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Add title bar
        JPanel titleBar = createTitleBar();
        phoneContainer.add(titleBar, BorderLayout.NORTH);
        
        // Create content area
        contentPane = new JTextPane();
        contentPane.setContentType("text/html");
        contentPane.setEditable(false);
        contentPane.setBackground(Color.WHITE);
        
        JScrollPane scrollPane = new JScrollPane(contentPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        phoneContainer.add(scrollPane, BorderLayout.CENTER);
        
        // Add action buttons
        JPanel buttonPanel = createButtonPanel();
        phoneContainer.add(buttonPanel, BorderLayout.SOUTH);
        
        browserFrame.add(phoneContainer);
        
        // Handle window closing
        browserFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeBrowser();
            }
        });
        
        // Show the browser window
        browserFrame.setVisible(true);
        browserFrame.setAlwaysOnTop(true);
        
        plugin.getLogger().info("Simple web browser window opened for player: " + player.getName());
    }
    
    private JPanel createTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(40, 40, 40));
        titleBar.setPreferredSize(new Dimension(0, 35));
        
        JLabel titleLabel = new JLabel("📱 Web Wallet", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        JButton closeButton = new JButton("✕");
        closeButton.setBackground(new Color(255, 95, 87));
        closeButton.setForeground(Color.WHITE);
        closeButton.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        closeButton.setFocusPainted(false);
        closeButton.addActionListener(e -> closeBrowser());
        
        titleBar.add(titleLabel, BorderLayout.CENTER);
        titleBar.add(closeButton, BorderLayout.EAST);
        
        return titleBar;
    }
    
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(new Color(50, 50, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        
        JButton refreshButton = new JButton("🔄 Refresh");
        refreshButton.setBackground(new Color(0, 122, 255));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> loadContent());
        
        JButton shareButton = new JButton("📤 Share Credential");
        shareButton.setBackground(new Color(52, 199, 89));
        shareButton.setForeground(Color.WHITE);
        shareButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        shareButton.setFocusPainted(false);
        shareButton.addActionListener(e -> shareCredential());
        
        JButton declineButton = new JButton("❌ Decline");
        declineButton.setBackground(new Color(255, 59, 48));
        declineButton.setForeground(Color.WHITE);
        declineButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        declineButton.setFocusPainted(false);
        declineButton.addActionListener(e -> declineRequest());
        
        buttonPanel.add(refreshButton);
        buttonPanel.add(shareButton);
        buttonPanel.add(declineButton);
        
        return buttonPanel;
    }
    
    private void loadContent() {
        CompletableFuture.runAsync(() -> {
            try {
                // Fetch notifications from web wallet
                Request request = new Request.Builder()
                    .url("http://localhost:3001/api/notifications")
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        
                        SwingUtilities.invokeLater(() -> {
                            displayNotifications(responseBody);
                        });
                        
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            displayError("Failed to connect to web wallet");
                        });
                    }
                }
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load web wallet content: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    displayError("Connection error: " + e.getMessage());
                });
            }
        });
    }
    
    private void displayNotifications(String jsonResponse) {
        String html = "<html><head><style>" +
            "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; }" +
            ".container { background: rgba(255,255,255,0.1); border-radius: 15px; padding: 20px; backdrop-filter: blur(10px); }" +
            ".notification { background: rgba(255,255,255,0.2); border-radius: 10px; padding: 15px; margin: 10px 0; }" +
            ".proof-request { border-left: 4px solid #52c41a; }" +
            ".credential-offer { border-left: 4px solid #1890ff; }" +
            "h1 { margin-top: 0; text-align: center; }" +
            "h3 { margin: 10px 0 5px 0; color: #ffd700; }" +
            ".attribute { background: rgba(0,0,0,0.2); padding: 5px 10px; border-radius: 5px; margin: 2px 0; display: inline-block; }" +
            ".status { text-align: center; padding: 20px; font-size: 16px; }" +
            ".player-info { text-align: center; margin-top: 20px; font-size: 12px; opacity: 0.8; }" +
            "</style></head><body><div class=\"container\">" +
            "<h1>📱 Web Wallet</h1>" +
            parseNotifications(jsonResponse) +
            "<div class=\"player-info\">" +
            "<p>Player: " + player.getName() + "</p>" +
            "<p>Session: " + verificationSessionId + "</p>" +
            "</div></div></body></html>";
        
        contentPane.setText(html);
    }
    
    private String parseNotifications(String jsonResponse) {
        try {
            // Simple JSON parsing - look for proof requests
            if (jsonResponse.contains("proof-request")) {
                return "<div class=\"notification proof-request\">" +
                    "<h3>🔐 Proof Request</h3>" +
                    "<p><strong>Minecraft Verification Request</strong></p>" +
                    "<p>The server is requesting proof of your credentials.</p>" +
                    "<h4>Required Attributes:</h4>" +
                    "<div class=\"attribute\">👤 Name</div>" +
                    "<div class=\"attribute\">📧 Email</div>" +
                    "<div class=\"attribute\">🏢 Department</div>" +
                    "<div class=\"attribute\">🆔 Issuer DID</div>" +
                    "<div class=\"attribute\">🎂 Age</div>" +
                    "<p style=\"margin-top: 15px;\"><strong>Use the buttons below to respond to this request.</strong></p>" +
                    "</div>";
            } else if (jsonResponse.contains("credential-offer")) {
                return "<div class=\"notification credential-offer\">" +
                    "<h3>🎓 Credential Offer</h3>" +
                    "<p><strong>New Credential Available</strong></p>" +
                    "<p>A new credential has been offered to you.</p>" +
                    "<p>Click \"Share Credential\" to accept it.</p>" +
                    "</div>";
            } else {
                return "<div class=\"status\">" +
                    "<p>📭 No pending notifications</p>" +
                    "<p>All requests have been processed.</p>" +
                    "</div>";
            }
        } catch (Exception e) {
            return "<div class=\"status\">" +
                "<p>⚠️ Error parsing notifications</p>" +
                "<p>Raw data received from server.</p>" +
                "</div>";
        }
    }
    
    private void displayError(String error) {
        String html = "<html><head><style>" +
            "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 20px; background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%); color: white; text-align: center; }" +
            ".container { background: rgba(255,255,255,0.1); border-radius: 15px; padding: 30px; backdrop-filter: blur(10px); }" +
            "h1 { margin-top: 0; }" +
            ".error-details { background: rgba(0,0,0,0.2); border-radius: 10px; padding: 15px; margin: 20px 0; }" +
            "</style></head><body><div class=\"container\">" +
            "<h1>⚠️ Connection Error</h1>" +
            "<div class=\"error-details\">" +
            "<p>" + error + "</p>" +
            "<p>Make sure localhost:3001 is running</p>" +
            "</div>" +
            "<p>Use the Refresh button to try again</p>" +
            "</div></body></html>";
        
        contentPane.setText(html);
    }
    
    private void shareCredential() {
        CompletableFuture.runAsync(() -> {
            try {
                plugin.getLogger().info("[SimpleWebBrowser] Sharing credential for verification session: " + verificationSessionId);
                
                // First, find the notification ID for this verification session
                String notificationId = findNotificationId();
                if (notificationId == null) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(browserFrame, "No notification found for this verification session");
                    });
                    return;
                }
                
                // Send accept request to the notification endpoint
                JsonObject acceptRequest = new JsonObject();
                acceptRequest.addProperty("action", "accept");
                
                RequestBody body = RequestBody.create(acceptRequest.toString(), MediaType.get("application/json"));
                Request httpRequest = new Request.Builder()
                    .url("http://localhost:3001/api/notifications/" + notificationId)
                    .patch(body)
                    .build();
                
                try (Response response = httpClient.newCall(httpRequest).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "No response";
                    plugin.getLogger().info("[SimpleWebBrowser] Share credential response: " + response.code() + " - " + responseBody);
                    
                    if (response.isSuccessful()) {
                        SwingUtilities.invokeLater(() -> {
                            contentPane.setText("<html><body style='text-align:center; padding:50px; font-family:Arial'>" +
                                "<h2>✅ Credential Shared Successfully!</h2>" +
                                "<p>Your verification is being processed...</p>" +
                                "<p>This window will close automatically.</p>" +
                                "</body></html>");
                        });
                        
                        // Auto-close after showing success message
                        Timer timer = new Timer(3000, e -> closeBrowser());
                        timer.setRepeats(false);
                        timer.start();
                        
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(browserFrame, "Failed to share credential: " + responseBody);
                        });
                    }
                }
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to share credential: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(browserFrame, "Error sharing credential: " + e.getMessage());
                });
            }
        });
    }
    
    private String findNotificationId() {
        try {
            Request request = new Request.Builder()
                .url("http://localhost:3001/api/notifications")
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    // Simple parsing to find notification with our verification session ID
                    if (responseBody.contains("\"id\":\"" + verificationSessionId + "\"")) {
                        return verificationSessionId;
                    }
                    // Also check if the verification session ID appears as notification ID format
                    if (responseBody.contains("\"id\":\"notification-" + verificationSessionId.replace("verification-", "") + "\"")) {
                        return "notification-" + verificationSessionId.replace("verification-", "");
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to find notification ID: " + e.getMessage());
        }
        return null;
    }
    
    private void declineRequest() {
        CompletableFuture.runAsync(() -> {
            try {
                plugin.getLogger().info("[SimpleWebBrowser] Declining verification session: " + verificationSessionId);
                
                // First, find the notification ID for this verification session
                String notificationId = findNotificationId();
                if (notificationId == null) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(browserFrame, "No notification found for this verification session");
                    });
                    return;
                }
                
                // Send decline request to the notification endpoint
                JsonObject declineRequest = new JsonObject();
                declineRequest.addProperty("action", "decline");
                
                RequestBody body = RequestBody.create(declineRequest.toString(), MediaType.get("application/json"));
                Request httpRequest = new Request.Builder()
                    .url("http://localhost:3001/api/notifications/" + notificationId)
                    .patch(body)
                    .build();
                
                try (Response response = httpClient.newCall(httpRequest).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "No response";
                    plugin.getLogger().info("[SimpleWebBrowser] Decline request response: " + response.code() + " - " + responseBody);
                    
                    if (response.isSuccessful()) {
                        SwingUtilities.invokeLater(() -> {
                            contentPane.setText("<html><body style='text-align:center; padding:50px; font-family:Arial'>" +
                                "<h2>❌ Verification Declined</h2>" +
                                "<p>The verification request has been declined.</p>" +
                                "<p>This window will close automatically.</p>" +
                                "</body></html>");
                        });
                        
                        // Auto-close after showing decline message
                        Timer timer = new Timer(2000, e -> closeBrowser());
                        timer.setRepeats(false);
                        timer.start();
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(browserFrame, "Failed to decline request: " + responseBody);
                        });
                    }
                }
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to decline request: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(browserFrame, "Error declining request: " + e.getMessage());
                });
            }
        });
    }
    
    private String extractNotificationId(String jsonResponse) {
        // Simple ID extraction from JSON
        try {
            int idStart = jsonResponse.indexOf("\"id\":\"");
            if (idStart != -1) {
                idStart += 6; // Length of "\"id\":\""
                int idEnd = jsonResponse.indexOf("\"", idStart);
                if (idEnd != -1) {
                    return jsonResponse.substring(idStart, idEnd);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to extract notification ID: " + e.getMessage());
        }
        return null;
    }
    
    private void acceptNotification(String notificationId) throws IOException {
        String requestBody = "{\"action\":\"accept\"}";
        
        RequestBody body = RequestBody.create(requestBody, MediaType.get("application/json"));
        Request request = new Request.Builder()
            .url("http://localhost:3001/api/notifications/" + notificationId)
            .patch(body)
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            SwingUtilities.invokeLater(() -> {
                if (response.isSuccessful()) {
                    JOptionPane.showMessageDialog(browserFrame, "✅ Credential shared successfully!");
                    
                    // Auto-close after 2 seconds
                    Timer timer = new Timer(2000, e -> closeBrowser());
                    timer.setRepeats(false);
                    timer.start();
                } else {
                    JOptionPane.showMessageDialog(browserFrame, "❌ Failed to share credential");
                }
            });
        }
    }
    
    private void declineNotification(String notificationId) throws IOException {
        String requestBody = "{\"action\":\"decline\"}";
        
        RequestBody body = RequestBody.create(requestBody, MediaType.get("application/json"));
        Request request = new Request.Builder()
            .url("http://localhost:3001/api/notifications/" + notificationId)
            .patch(body)
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            SwingUtilities.invokeLater(() -> {
                if (response.isSuccessful()) {
                    JOptionPane.showMessageDialog(browserFrame, "❌ Request declined");
                    
                    // Auto-close after 1 second
                    Timer timer = new Timer(1000, e -> closeBrowser());
                    timer.setRepeats(false);
                    timer.start();
                } else {
                    JOptionPane.showMessageDialog(browserFrame, "Failed to decline request");
                }
            });
        }
    }
    
    public void closeBrowser() {
        plugin.getLogger().info("Closing simple web browser for player: " + player.getName());
        
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        
        SwingUtilities.invokeLater(() -> {
            if (browserFrame != null) {
                browserFrame.dispose();
                browserFrame = null;
            }
        });
    }
    
    public boolean isOpen() {
        return browserFrame != null && browserFrame.isVisible();
    }
}