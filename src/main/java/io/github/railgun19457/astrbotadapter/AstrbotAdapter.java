package io.github.railgun19457.astrbotadapter;

import io.github.railgun19457.astrbotadapter.command.AstrbotCommand;
import io.github.railgun19457.astrbotadapter.listener.ChatListener;
import io.github.railgun19457.astrbotadapter.listener.PlayerListener;
import io.github.railgun19457.astrbotadapter.server.RestApiServer;
import io.github.railgun19457.astrbotadapter.server.WebSocketServer;
import io.github.railgun19457.astrbotadapter.manager.MessageManager;
import io.github.railgun19457.astrbotadapter.manager.StatusManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.HandlerList;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Level;

public class AstrbotAdapter extends JavaPlugin {
    
    private static AstrbotAdapter instance;
    private WebSocketServer webSocketServer;
    private RestApiServer restApiServer;
    private MessageManager messageManager;
    private StatusManager statusManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config and generate tokens if needed
        saveDefaultConfig();
        generateTokensIfNeeded();
        
        getLogger().info("========================================");
        getLogger().info("  AstrBot Adapter is starting...");
        getLogger().info("========================================");
        
        // Initialize managers
        messageManager = new MessageManager(this);
        statusManager = new StatusManager(this);
        
        // Register listeners
        registerListeners();
        
        // Register commands
        registerCommands();
        
        // Start servers
        startServers();
        
        getLogger().info("========================================");
        getLogger().info("  AstrBot Adapter enabled successfully!");
        getLogger().info("  WebSocket Server: " + getConfig().getString("websocket.host") + ":" + getConfig().getInt("websocket.port"));
        getLogger().info("  REST API Server: http://" + getConfig().getString("rest-api.host") + ":" + getConfig().getInt("rest-api.port"));
        getLogger().info("========================================");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Stopping AstrBot Adapter...");
        
        // Stop servers
        stopServers();
        
        // Stop managers
        if (statusManager != null) {
            statusManager.shutdown();
        }
        
        getLogger().info("AstrBot Adaptor disabled.");
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getLogger().info("Listeners registered.");
    }
    
    private void registerCommands() {
        getCommand("astrbot").setExecutor(new AstrbotCommand(this));
        getCommand("astrbot").setTabCompleter(new io.github.railgun19457.astrbotadapter.command.AstrbotTabCompleter());
        getLogger().info("Commands registered.");
    }
    
    private void startServers() {
        // Start WebSocket Server
        if (getConfig().getBoolean("websocket.enabled", true)) {
            try {
                String wsHost = getConfig().getString("websocket.host", "0.0.0.0");
                int wsPort = getConfig().getInt("websocket.port", 8765);
                String wsToken = getConfig().getString("websocket.token", "");
                
                webSocketServer = new WebSocketServer(this, wsHost, wsPort, wsToken);
                webSocketServer.start();
                getLogger().info("WebSocket server started on " + wsHost + ":" + wsPort);
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to start WebSocket server", e);
            }
        }
        
        // Start REST API Server
        if (getConfig().getBoolean("rest-api.enabled", true)) {
            try {
                String apiHost = getConfig().getString("rest-api.host", "0.0.0.0");
                int apiPort = getConfig().getInt("rest-api.port", 8766);
                String apiToken = getConfig().getString("rest-api.token", "");
                
                restApiServer = new RestApiServer(this, apiHost, apiPort, apiToken);
                restApiServer.start();
                getLogger().info("REST API server started on " + apiHost + ":" + apiPort);
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to start REST API server", e);
            }
        }
    }
    
    private void stopServers() {
        if (webSocketServer != null) {
            try {
                webSocketServer.stop();
                getLogger().info("WebSocket server stopped.");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error stopping WebSocket server", e);
            }
        }
        
        if (restApiServer != null) {
            try {
                restApiServer.stop();
                getLogger().info("REST API server stopped.");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error stopping REST API server", e);
            }
        }
    }
    
    public void reload() {
        getLogger().info("Reloading AstrBot Adaptor...");
        
        // Stop servers
        stopServers();
        
        // Reload config
        reloadConfig();
        // Ensure required tokens exist if still placeholders/empty
        generateTokensIfNeeded();

        // Re-register listeners to refresh any cached config values (e.g., forward prefix)
        try {
            HandlerList.unregisterAll(this);
        } catch (Exception ignored) {
            // Safe guard: continue even if no handlers were registered yet
        }
        
        // Restart managers
        if (statusManager != null) {
            statusManager.shutdown();
        }
        messageManager = new MessageManager(this);
        statusManager = new StatusManager(this);
        // Re-register listeners (ChatListener caches config in constructor)
        registerListeners();
        
        // Restart servers
        startServers();
        
        getLogger().info("AstrBot Adapter reloaded successfully!");
    }
    
    public static AstrbotAdapter getInstance() {
        return instance;
    }
    
    public WebSocketServer getWebSocketServer() {
        return webSocketServer;
    }
    
    public RestApiServer getRestApiServer() {
        return restApiServer;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public StatusManager getStatusManager() {
        return statusManager;
    }
    
    public boolean isDebugEnabled() {
        return getConfig().getBoolean("debug", false);
    }
    
    public void debug(String message) {
        if (isDebugEnabled()) {
            getLogger().info("[DEBUG] " + message);
        }
    }
    
    /**
     * Generate random tokens if default tokens are still in use
     */
    private void generateTokensIfNeeded() {
        boolean configChanged = false;
        
        // Check WebSocket token
        String wsToken = getConfig().getString("websocket.token", "");
        if (wsToken.isEmpty() || wsToken.equals("your_secure_token_here")) {
            String newToken = generateSecureToken();
            getConfig().set("websocket.token", newToken);
            configChanged = true;
            getLogger().warning("========================================");
            getLogger().warning("WebSocket token not configured!");
            getLogger().warning("Generated new token: " + newToken);
            getLogger().warning("========================================");
        }
        
        // Check REST API token
        String apiToken = getConfig().getString("rest-api.token", "");
        if (apiToken.isEmpty() || apiToken.equals("your_secure_token_here")) {
            String newToken = generateSecureToken();
            getConfig().set("rest-api.token", newToken);
            configChanged = true;
            getLogger().warning("========================================");
            getLogger().warning("REST API token not configured!");
            getLogger().warning("Generated new token: " + newToken);
            getLogger().warning("========================================");
        }
        
        // Save config if changed
        if (configChanged) {
            saveConfig();
            getLogger().info("Configuration saved with new tokens.");
            getLogger().info("Please save these tokens securely!");
        }
    }
    
    /**
     * Generate a cryptographically secure random token
     * @return A Base64-encoded random token
     */
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32]; // 256 bits
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
