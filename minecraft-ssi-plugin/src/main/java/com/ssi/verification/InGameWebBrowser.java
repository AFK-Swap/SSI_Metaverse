package com.ssi.verification;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.net.URL;

public class InGameWebBrowser {
    
    private final JavaPlugin plugin;
    private final Player player;
    private final String verificationSessionId;
    private JFrame browserFrame;
    private WebView webView;
    private WebEngine webEngine;
    
    public InGameWebBrowser(JavaPlugin plugin, Player player, String verificationSessionId) {
        this.plugin = plugin;
        this.player = player;
        this.verificationSessionId = verificationSessionId;
    }
    
    public void openWebWallet() {
        // Initialize JavaFX if not already done
        try {
            Platform.startup(() -> {
                plugin.getLogger().info("JavaFX Platform initialized");
            });
        } catch (IllegalStateException e) {
            // Platform already initialized
            plugin.getLogger().info("JavaFX Platform already running");
        }
        
        SwingUtilities.invokeLater(() -> {
            try {
                createBrowserWindow();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create web browser window: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    private void createBrowserWindow() {
        // Create phone-shaped window
        browserFrame = new JFrame("Web Wallet - " + player.getName());
        browserFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // Phone dimensions (9:16 aspect ratio)
        int width = 375;  // iPhone-like width
        int height = 667; // iPhone-like height
        browserFrame.setSize(width, height);
        browserFrame.setResizable(false);
        
        // Center the window on screen
        browserFrame.setLocationRelativeTo(null);
        
        // Create rounded phone-like appearance
        browserFrame.setUndecorated(true);
        browserFrame.setShape(new RoundRectangle2D.Float(0, 0, width, height, 25, 25));
        
        // Create JavaFX panel for web content
        JFXPanel jfxPanel = new JFXPanel();
        
        // Add phone-like styling with border
        JPanel phoneContainer = new JPanel(new BorderLayout());
        phoneContainer.setBackground(Color.BLACK);
        phoneContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        phoneContainer.add(jfxPanel, BorderLayout.CENTER);
        
        // Add title bar with close button
        JPanel titleBar = createTitleBar();
        phoneContainer.add(titleBar, BorderLayout.NORTH);
        
        browserFrame.add(phoneContainer);
        
        // Initialize JavaFX WebView
        Platform.runLater(() -> {
            createWebView(jfxPanel);
        });
        
        // Handle window closing
        browserFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeBrowser();
            }
        });
        
        // Show the browser window
        browserFrame.setVisible(true);
        browserFrame.setAlwaysOnTop(true); // Keep on top for better visibility
        
        plugin.getLogger().info("Web browser window opened for player: " + player.getName());
    }
    
    private JPanel createTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(40, 40, 40));
        titleBar.setPreferredSize(new Dimension(0, 30));
        
        // Title label
        JLabel titleLabel = new JLabel("📱 Web Wallet", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        
        // Close button
        JButton closeButton = new JButton("✕");
        closeButton.setBackground(new Color(255, 95, 87));
        closeButton.setForeground(Color.WHITE);
        closeButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        closeButton.setFocusPainted(false);
        closeButton.addActionListener(e -> closeBrowser());
        
        titleBar.add(titleLabel, BorderLayout.CENTER);
        titleBar.add(closeButton, BorderLayout.EAST);
        
        return titleBar;
    }
    
    private void createWebView(JFXPanel jfxPanel) {
        webView = new WebView();
        webEngine = webView.getEngine();
        
        // Enable JavaScript
        webEngine.setJavaScriptEnabled(true);
        
        // Set user agent to include auto-close information
        webEngine.setUserAgent("Mozilla/5.0 (Minecraft WebWallet) autoClose=true verificationId=" + verificationSessionId);
        
        // Add console message handler for debugging
        webEngine.setOnError(event -> {
            plugin.getLogger().warning("WebEngine error: " + event.getMessage());
        });
        
        // Set up security permissions for localhost
        System.setProperty("com.sun.webkit.useHTTP2Loader", "false");
        System.setProperty("com.sun.webkit.inspector.enabled", "true");
        System.setProperty("com.sun.webkit.disableHTTP2", "true");
        
        // Configure WebEngine for localhost access
        webEngine.setCreatePopupHandler(null);
        webEngine.setPromptHandler(null);
        
        // Handle page load events
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            plugin.getLogger().info("WebEngine state changed: " + oldState + " -> " + newState);
            
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                plugin.getLogger().info("Web page loaded successfully");
                
                // Inject auto-close JavaScript
                injectAutoCloseScript();
            } else if (newState == javafx.concurrent.Worker.State.FAILED) {
                plugin.getLogger().warning("Failed to load web page. Exception: " + 
                    webEngine.getLoadWorker().getException());
                
                // Try to load a simple HTML page instead
                loadFallbackContent();
            } else if (newState == javafx.concurrent.Worker.State.CANCELLED) {
                plugin.getLogger().warning("Web page loading was cancelled");
            }
        });
        
        // Handle JavaScript alerts and console messages
        webEngine.setOnAlert(event -> {
            plugin.getLogger().info("Web alert: " + event.getData());
        });
        
        // Create scene and set it
        Scene scene = new Scene(webView);
        jfxPanel.setScene(scene);
        
        // Load the web wallet URL
        String webWalletUrl = "http://localhost:3001?autoClose=true&verificationId=" + verificationSessionId;
        webEngine.load(webWalletUrl);
        
        plugin.getLogger().info("Loading web wallet URL: " + webWalletUrl);
    }
    
    private void injectAutoCloseScript() {
        // Inject JavaScript to handle auto-close functionality
        String autoCloseScript = """
            // Auto-close functionality for in-game browser
            window.minecraftAutoClose = true;
            window.minecraftVerificationId = '%s';
            
            // Monitor for credential sharing completion
            const originalFetch = window.fetch;
            window.fetch = function(...args) {
                return originalFetch.apply(this, args).then(response => {
                    // Check if this was a notification action (accept/decline)
                    if (args[0] && args[0].includes('/api/notifications/') && 
                        (args[1]?.method === 'PATCH' || args[1]?.method === 'POST')) {
                        
                        console.log('Credential action detected, scheduling auto-close...');
                        
                        // Auto-close after 3 seconds to show result
                        setTimeout(() => {
                            console.log('Auto-closing Minecraft web browser...');
                            window.close();
                        }, 3000);
                    }
                    return response;
                });
            };
            
            // Also monitor for button clicks that indicate completion
            document.addEventListener('click', function(event) {
                const target = event.target;
                
                // Check for credential sharing buttons
                if (target.textContent && (
                    target.textContent.includes('Share') || 
                    target.textContent.includes('Accept') || 
                    target.textContent.includes('Decline')
                )) {
                    console.log('Credential action button clicked');
                    
                    // Auto-close after action
                    setTimeout(() => {
                        console.log('Auto-closing after credential action...');
                        window.close();
                    }, 2000);
                }
            });
            
            console.log('Minecraft auto-close script injected');
        """.formatted(verificationSessionId);
        
        webEngine.executeScript(autoCloseScript);
    }
    
    public void closeBrowser() {
        plugin.getLogger().info("Closing web browser for player: " + player.getName());
        
        // Close JavaFX components first
        Platform.runLater(() -> {
            if (webEngine != null) {
                webEngine.load("about:blank");
                webEngine = null;
            }
            if (webView != null) {
                webView = null;
            }
        });
        
        // Close Swing window
        SwingUtilities.invokeLater(() -> {
            if (browserFrame != null) {
                browserFrame.dispose();
                browserFrame = null;
            }
        });
    }
    
    private void loadFallbackContent() {
        String fallbackHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Web Wallet</title>
            <meta charset="UTF-8">
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    margin: 0;
                    padding: 20px;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white;
                    text-align: center;
                }
                .container {
                    max-width: 350px;
                    margin: 0 auto;
                    background: rgba(255,255,255,0.1);
                    border-radius: 20px;
                    padding: 30px;
                    backdrop-filter: blur(10px);
                }
                h1 { margin-top: 0; }
                .status { background: rgba(255,255,255,0.2); padding: 15px; border-radius: 10px; margin: 20px 0; }
                .button {
                    background: #4CAF50;
                    color: white;
                    border: none;
                    padding: 12px 24px;
                    border-radius: 8px;
                    cursor: pointer;
                    font-size: 16px;
                    margin: 10px;
                }
                .button:hover { background: #45a049; }
                .error { background: #f44336; }
                .error:hover { background: #da190b; }
            </style>
        </head>
        <body>
            <div class="container">
                <h1>📱 Web Wallet</h1>
                <div class="status">
                    <p>⚠️ Could not connect to localhost:3001</p>
                    <p>Make sure your web wallet is running</p>
                </div>
                <button class="button" onclick="window.location.reload()">🔄 Retry</button>
                <button class="button error" onclick="window.close()">❌ Close</button>
                
                <div style="margin-top: 30px; font-size: 14px; opacity: 0.8;">
                    <p>Verification ID: %VERIFICATION_ID%</p>
                    <p>Player: %PLAYER_NAME%</p>
                </div>
            </div>
            
            <script>
                // Auto-close functionality
                window.minecraftAutoClose = true;
                window.minecraftVerificationId = '%VERIFICATION_ID%';
                
                setTimeout(() => {
                    document.querySelector('.status').innerHTML = 
                        '<p>🔄 Retrying connection...</p>';
                    window.location.href = 'http://localhost:3001?autoClose=true&verificationId=%VERIFICATION_ID%';
                }, 5000);
            </script>
        </body>
        </html>
        """;
        
        // Replace placeholders manually to avoid formatting errors
        final String finalHtml = fallbackHtml.replace("%VERIFICATION_ID%", verificationSessionId)
                                            .replace("%PLAYER_NAME%", player.getName());
        
        Platform.runLater(() -> {
            webEngine.loadContent(finalHtml);
        });
    }
    
    public boolean isOpen() {
        return browserFrame != null && browserFrame.isVisible();
    }
    
    public void refresh() {
        if (webEngine != null) {
            Platform.runLater(() -> {
                webEngine.reload();
            });
        }
    }
    
    public void navigateToNotifications() {
        if (webEngine != null) {
            Platform.runLater(() -> {
                String notificationsUrl = "http://localhost:3001?autoClose=true&verificationId=" + verificationSessionId;
                webEngine.load(notificationsUrl);
            });
        }
    }
}