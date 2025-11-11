package io.github.railgun19457.astrbotadapter.server;

import io.github.railgun19457.astrbotadapter.AstrbotAdapter;

public class WebSocketServer {
    
    private final AstrbotAdapter plugin;
    private final String host;
    private final int port;
    private final String token;
    private AstrbotWebSocketServer server;
    
    public WebSocketServer(AstrbotAdapter plugin, String host, int port, String token) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.token = token;
    }
    
    public void start() {
        // Quick retry strategy to mitigate fast-reload bind races (e.g., pluginmanX)
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                server = new AstrbotWebSocketServer(plugin, host, port, token);
                server.start();
                // Wait a short time for onStart() to flip running=true; if not, treat as failure and retry
                long deadline = System.currentTimeMillis() + 1500L;
                while (System.currentTimeMillis() < deadline) {
                    if (server.isRunning()) {
                        return;
                    }
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                }
                // Not running after grace period; retry once more
                plugin.getLogger().warning(String.format(
                    "WebSocket server didn't reach running state, retrying in 1s (%d/2)...", attempt
                ));
            } catch (Exception e) {
                plugin.getLogger().warning(String.format(
                    "WebSocket start attempt failed (%d/2): %s", attempt, e.getMessage()
                ));
            }
            // Cleanup between attempts
            try {
                if (server != null) {
                    server.stop(500);
                }
            } catch (Exception ignored) { }
            server = null;
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }
        plugin.getLogger().severe("Failed to start WebSocket server after retries.");
    }
    
    public void stop() {
        if (server != null) {
            try {
                server.stop(1000);
                server = null;
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
    
    public java.util.List<String> getConnectionDetails() {
        return server != null ? server.getConnectionDetails() : java.util.Collections.emptyList();
    }
    
    public boolean isRunning() {
        return server != null && server.isRunning();
    }
}
