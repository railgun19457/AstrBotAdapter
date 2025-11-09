package io.github.railgun19457.astrbotadaptor.server;

import io.github.railgun19457.astrbotadaptor.AstrbotAdaptor;

public class WebSocketServer {
    
    private final AstrbotAdaptor plugin;
    private final String host;
    private final int port;
    private final String token;
    private AstrbotWebSocketServer server;
    
    public WebSocketServer(AstrbotAdaptor plugin, String host, int port, String token) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.token = token;
    }
    
    public void start() {
        server = new AstrbotWebSocketServer(plugin, host, port, token);
        server.start();
    }
    
    public void stop() {
        if (server != null) {
            try {
                server.stop(1000);
            } catch (InterruptedException e) {
                plugin.getLogger().warning("Error stopping WebSocket server: " + e.getMessage());
            }
        }
    }
    
    public void broadcastChatMessage(String player, String message) {
        if (server != null) {
            server.broadcastChatMessage(player, message);
        }
    }
    
    public void broadcastPlayerJoin(String player) {
        if (server != null) {
            server.broadcastPlayerJoin(player);
        }
    }
    
    public void broadcastPlayerLeave(String player) {
        if (server != null) {
            server.broadcastPlayerLeave(player);
        }
    }
    
    public int getConnectedClients() {
        return server != null ? server.getConnectedClients() : 0;
    }
    
    public boolean isRunning() {
        return server != null && server.isRunning();
    }
}
