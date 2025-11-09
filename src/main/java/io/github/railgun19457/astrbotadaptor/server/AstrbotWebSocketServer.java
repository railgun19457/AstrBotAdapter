package io.github.railgun19457.astrbotadaptor.server;

import io.github.railgun19457.astrbotadaptor.AstrbotAdaptor;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class AstrbotWebSocketServer extends WebSocketServer {
    
    private final AstrbotAdaptor plugin;
    private final String token;
    private final Gson gson;
    private final Set<WebSocket> authenticatedClients;
    private volatile boolean running = false;
    
    public AstrbotWebSocketServer(AstrbotAdaptor plugin, String host, int port, String token) {
        super(new InetSocketAddress(host, port));
        this.plugin = plugin;
        this.token = token;
        this.gson = new Gson();
        this.authenticatedClients = Collections.synchronizedSet(new HashSet<>());
        
        // Set connection timeout
        setConnectionLostTimeout(30);
    }
    
    @Override
    public void onStart() {
        running = true;
        plugin.getLogger().info("WebSocket server started successfully!");
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        plugin.debug("New WebSocket connection from: " + conn.getRemoteSocketAddress());
        
        // Send authentication request
        JsonObject authRequest = new JsonObject();
        authRequest.addProperty("type", "auth_required");
        authRequest.addProperty("message", "Please authenticate with your token");
        conn.send(gson.toJson(authRequest));
    }
    
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        authenticatedClients.remove(conn);
        plugin.debug("WebSocket connection closed: " + reason);
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            String type = json.has("type") ? json.get("type").getAsString() : "";
            
            // Handle authentication
            if (!authenticatedClients.contains(conn)) {
                if ("auth".equals(type)) {
                    handleAuthentication(conn, json);
                } else {
                    sendError(conn, "Not authenticated");
                }
                return;
            }
            
            // Handle authenticated messages
            switch (type) {
                case "chat":
                    handleChatMessage(conn, json);
                    break;
                    
                case "command":
                    handleCommand(conn, json);
                    break;
                    
                case "status_request":
                    handleStatusRequest(conn);
                    break;
                    
                case "ping":
                    handlePing(conn);
                    break;
                    
                default:
                    sendError(conn, "Unknown message type: " + type);
                    break;
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error processing WebSocket message", e);
            sendError(conn, "Error processing message: " + e.getMessage());
        }
    }
    
    @Override
    public void onError(WebSocket conn, Exception ex) {
        plugin.getLogger().log(Level.SEVERE, "WebSocket error", ex);
    }
    
    private void handleAuthentication(WebSocket conn, JsonObject json) {
        if (!json.has("token")) {
            sendError(conn, "Token is required");
            return;
        }
        
        String receivedToken = json.get("token").getAsString();
        
        if (token.isEmpty() || token.equals(receivedToken)) {
            authenticatedClients.add(conn);
            
            JsonObject response = new JsonObject();
            response.addProperty("type", "auth_success");
            response.addProperty("message", "Authentication successful");
            conn.send(gson.toJson(response));
            
            plugin.getLogger().info("Client authenticated: " + conn.getRemoteSocketAddress());
        } else {
            JsonObject response = new JsonObject();
            response.addProperty("type", "auth_failed");
            response.addProperty("message", "Invalid token");
            conn.send(gson.toJson(response));
            
            plugin.getLogger().warning("Authentication failed from: " + conn.getRemoteSocketAddress());
            conn.close(1008, "Invalid token");
        }
    }
    
    private void handleChatMessage(WebSocket conn, JsonObject json) {
        if (!json.has("message")) {
            sendError(conn, "Message is required");
            return;
        }
        
        String message = json.get("message").getAsString();
        String sender = json.has("sender") ? json.get("sender").getAsString() : null;
        
        plugin.getMessageManager().sendToMinecraft(message, sender);
        
        // Send confirmation
        JsonObject response = new JsonObject();
        response.addProperty("type", "chat_sent");
        response.addProperty("status", "success");
        conn.send(gson.toJson(response));
    }
    
    private void handleCommand(WebSocket conn, JsonObject json) {
        if (!json.has("command")) {
            sendError(conn, "Command is required");
            return;
        }
        
        String command = json.get("command").getAsString();
        
        // Execute command on main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                boolean success = plugin.getServer().dispatchCommand(
                    plugin.getServer().getConsoleSender(), 
                    command
                );
                
                JsonObject response = new JsonObject();
                response.addProperty("type", "command_result");
                response.addProperty("command", command);
                response.addProperty("success", success);
                conn.send(gson.toJson(response));
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error executing command: " + command, e);
                sendError(conn, "Error executing command: " + e.getMessage());
            }
        });
    }
    
    private void handleStatusRequest(WebSocket conn) {
        JsonObject status = plugin.getStatusManager().getStatus();
        status.addProperty("type", "status_response");
        conn.send(gson.toJson(status));
    }
    
    private void handlePing(WebSocket conn) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "pong");
        response.addProperty("timestamp", System.currentTimeMillis());
        conn.send(gson.toJson(response));
    }
    
    private void sendError(WebSocket conn, String error) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "error");
        response.addProperty("message", error);
        conn.send(gson.toJson(response));
    }
    
    public void broadcast(String message) {
        JsonObject json = gson.fromJson(message, JsonObject.class);
        String jsonString = gson.toJson(json);
        
        synchronized (authenticatedClients) {
            for (WebSocket client : authenticatedClients) {
                if (client.isOpen()) {
                    client.send(jsonString);
                }
            }
        }
    }
    
    public void broadcastChatMessage(String player, String message) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "chat");
        json.addProperty("player", player);
        json.addProperty("message", message);
        json.addProperty("timestamp", System.currentTimeMillis());
        
        broadcast(gson.toJson(json));
    }
    
    public void broadcastPlayerJoin(String player) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "player_join");
        json.addProperty("player", player);
        json.addProperty("timestamp", System.currentTimeMillis());
        
        broadcast(gson.toJson(json));
    }
    
    public void broadcastPlayerLeave(String player) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "player_leave");
        json.addProperty("player", player);
        json.addProperty("timestamp", System.currentTimeMillis());
        
        broadcast(gson.toJson(json));
    }
    
    public int getConnectedClients() {
        return authenticatedClients.size();
    }
    
    public boolean isRunning() {
        return running;
    }
    
    @Override
    public void stop() throws InterruptedException {
        running = false;
        super.stop();
    }
}
