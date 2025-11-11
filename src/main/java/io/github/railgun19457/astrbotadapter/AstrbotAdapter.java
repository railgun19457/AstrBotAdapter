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
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Level;

public class AstrbotAdapter extends JavaPlugin {
    
    private static AstrbotAdapter instance;
    private WebSocketServer webSocketServer;
    private RestApiServer restApiServer;
    private MessageManager messageManager;
    private StatusManager statusManager;
    private ChatListener chatListener;
    private PlayerListener playerListener;
    
    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        generateTokensIfNeeded();
        
        logSeparator();
        getLogger().info("  AstrBot Adapter is starting...");
        logSeparator();
        
        // Initialize managers
        messageManager = new MessageManager(this);
        statusManager = new StatusManager(this);
        
        // Register listeners and commands
        registerListeners();
        registerCommands();
        
        // Start servers
        startServers();
        
        logSeparator();
        getLogger().info("  AstrBot Adapter enabled successfully!");
        getLogger().info("  WebSocket: " + getConfig().getString("websocket.host") + ":" + getConfig().getInt("websocket.port"));
        getLogger().info("  REST API: http://" + getConfig().getString("rest-api.host") + ":" + getConfig().getInt("rest-api.port"));
        logSeparator();
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Stopping AstrBot Adapter...");
        stopServers();
        if (statusManager != null) {
            statusManager.shutdown();
        }
        getLogger().info("AstrBot Adapter disabled.");
    }
    
    private void registerListeners() {
        chatListener = new ChatListener(this);
        playerListener = new PlayerListener(this);
        getServer().getPluginManager().registerEvents(chatListener, this);
        getServer().getPluginManager().registerEvents(playerListener, this);
        getLogger().info("Listeners registered.");
    }
    
    private void registerCommands() {
        getCommand("astrbot").setExecutor(new AstrbotCommand(this));
        getCommand("astrbot").setTabCompleter(new io.github.railgun19457.astrbotadapter.command.AstrbotTabCompleter());
        getLogger().info("Commands registered.");
    }
    
    private void startServers() {
        startServer("WebSocket", getConfig().getBoolean("websocket.enabled", true), () -> {
            String host = getConfig().getString("websocket.host", "0.0.0.0");
            int port = getConfig().getInt("websocket.port", 8765);
            String token = getConfig().getString("websocket.token", "");
            webSocketServer = new WebSocketServer(this, host, port, token);
            webSocketServer.start();
            return host + ":" + port;
        });
        
        startServer("REST API", getConfig().getBoolean("rest-api.enabled", true), () -> {
            String host = getConfig().getString("rest-api.host", "0.0.0.0");
            int port = getConfig().getInt("rest-api.port", 8766);
            String token = getConfig().getString("rest-api.token", "");
            restApiServer = new RestApiServer(this, host, port, token);
            restApiServer.start();
            return host + ":" + port;
        });
    }
    
    private void startServer(String name, boolean enabled, ServerStarter starter) {
        if (enabled) {
            try {
                String address = starter.start();
                getLogger().info(name + " server started on " + address);
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to start " + name + " server", e);
            }
        }
    }
    
    @FunctionalInterface
    private interface ServerStarter {
        String start() throws Exception;
    }
    
    private void stopServers() {
        stopServer("WebSocket", webSocketServer, webSocketServer::stop);
        stopServer("REST API", restApiServer, restApiServer::stop);
    }
    
    private void stopServer(String name, Object server, Runnable stopAction) {
        if (server != null) {
            try {
                stopAction.run();
                getLogger().info(name + " server stopped.");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error stopping " + name + " server", e);
            }
        }
    }
    
    public void reload() {
        getLogger().info("Reloading AstrBot Adapter...");
        
        stopServers();
        reloadConfig();
        generateTokensIfNeeded();
        unregisterListeners();
        
        // Restart managers
        if (statusManager != null) {
            statusManager.shutdown();
        }
        messageManager = new MessageManager(this);
        statusManager = new StatusManager(this);
        
        registerListeners();
        startServers();
        
        getLogger().info("AstrBot Adapter reloaded successfully!");
    }
    
    private void unregisterListeners() {
        if (chatListener != null) {
            HandlerList.unregisterAll(chatListener);
            chatListener = null;
        }
        if (playerListener != null) {
            HandlerList.unregisterAll(playerListener);
            playerListener = null;
        }
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
    
    private void generateTokensIfNeeded() {
        boolean configChanged = false;
        
        configChanged |= generateTokenIfNeeded("websocket.token", "WebSocket");
        configChanged |= generateTokenIfNeeded("rest-api.token", "REST API");
        
        if (configChanged) {
            saveConfig();
            getLogger().info("Configuration saved with new tokens. Please save these tokens securely!");
        }
    }
    
    private boolean generateTokenIfNeeded(String configKey, String serviceName) {
        String token = getConfig().getString(configKey, "");
        if (token.isEmpty() || token.equals("your_secure_token_here")) {
            String newToken = generateSecureToken();
            getConfig().set(configKey, newToken);
            logSeparator();
            getLogger().warning(serviceName + " token not configured!");
            getLogger().warning("Generated new token: " + newToken);
            logSeparator();
            return true;
        }
        return false;
    }
    
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    private void logSeparator() {
        getLogger().info("========================================");
    }
}
