package io.github.railgun19457.astrbotadapter.server;

import io.github.railgun19457.astrbotadapter.AstrbotAdapter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class RestApiServer {
    
    private final AstrbotAdapter plugin;
    private final String host;
    private final int port;
    private final String token;
    private final Gson gson;
    private HttpServer server;
    private volatile boolean running = false;
    
    public RestApiServer(AstrbotAdapter plugin, String host, int port, String token) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.token = token;
        this.gson = new Gson();
    }
    
    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(host, port), 0);
            server.setExecutor(Executors.newFixedThreadPool(4));
            
            // Register endpoints
            server.createContext("/api/status", new StatusHandler());
            server.createContext("/api/players", new PlayersHandler());
            server.createContext("/api/command", new CommandHandler());
            server.createContext("/api/message", new MessageHandler());
            
            server.start();
            running = true;
            plugin.getLogger().info("REST API server started successfully!");
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to start REST API server", e);
        }
    }
    
    public void stop() {
        if (server != null) {
            server.stop(0);
            running = false;
        }
    }
    
    public boolean isRunning() {
        return running;
    }
    
    private boolean authenticate(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        
        if (authHeader == null) {
            return false;
        }
        
        // Support both "Bearer TOKEN" and "TOKEN" formats
        String receivedToken = authHeader.startsWith("Bearer ") ? 
            authHeader.substring(7) : authHeader;
        
        return token.isEmpty() || token.equals(receivedToken);
    }
    
    /**
     * Build a base response envelope with common metadata.
     */
    private JsonObject baseResponse(HttpExchange exchange, int statusCode, boolean success) {
        JsonObject base = new JsonObject();
        base.addProperty("success", success);
        base.addProperty("status", statusCode);
        base.addProperty("timestamp", System.currentTimeMillis());
        base.addProperty("path", exchange.getRequestURI().getPath());
        return base;
    }

    /**
     * Low-level send of already serialized JSON.
     */
    private void writeJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * High-level send that wraps data object in standard envelope.
     */
    private void sendJson(HttpExchange exchange, int statusCode, JsonObject data) {
        try {
            JsonObject base = baseResponse(exchange, statusCode, statusCode < 400);
            if (data != null) {
                base.add("data", data);
            }
            writeJson(exchange, statusCode, gson.toJson(base));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to send JSON response", e);
        }
    }

    private void sendOk(HttpExchange exchange, JsonObject data) {
        sendJson(exchange, 200, data);
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) {
        try {
            JsonObject base = baseResponse(exchange, statusCode, false);
            base.addProperty("error", message);
            writeJson(exchange, statusCode, gson.toJson(base));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Error sending error response", e);
        }
    }
    
    // Status endpoint handler
    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!authenticate(exchange)) {
                sendError(exchange, 401, "Unauthorized");
                return;
            }
            
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            
            try {
                JsonObject status = plugin.getStatusManager().getStatus();
                sendOk(exchange, status);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error getting status", e);
                sendError(exchange, 500, "Internal server error");
            }
        }
    }
    
    // Players endpoint handler
    private class PlayersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!authenticate(exchange)) {
                sendError(exchange, 401, "Unauthorized");
                return;
            }
            
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            
            try {
                JsonObject players = plugin.getStatusManager().getPlayersInfo();
                sendOk(exchange, players);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error getting players info", e);
                sendError(exchange, 500, "Internal server error");
            }
        }
    }
    
    // Command endpoint handler
    private class CommandHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!authenticate(exchange)) {
                sendError(exchange, 401, "Unauthorized");
                return;
            }
            
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            
            try {
                // Read request body
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                JsonObject json;
                try {
                    json = gson.fromJson(body, JsonObject.class);
                } catch (JsonSyntaxException ex) {
                    sendError(exchange, 400, "Malformed JSON");
                    return;
                }
                
                if (!json.has("command")) {
                    sendError(exchange, 400, "Command is required");
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
                        
                        JsonObject data = new JsonObject();
                        data.addProperty("command", command);
                        data.addProperty("executed", success);
                        sendOk(exchange, data);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Error executing command", e);
                        sendError(exchange, 500, "Error executing command");
                    }
                });
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error processing command request", e);
                sendError(exchange, 400, "Invalid request");
            }
        }
    }
    
    // Message endpoint handler
    private class MessageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!authenticate(exchange)) {
                sendError(exchange, 401, "Unauthorized");
                return;
            }
            
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            
            try {
                // Read request body
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                JsonObject json;
                try {
                    json = gson.fromJson(body, JsonObject.class);
                } catch (JsonSyntaxException ex) {
                    sendError(exchange, 400, "Malformed JSON");
                    return;
                }
                
                // Make message optional; default to empty string when absent
                String message = (json.has("message") && !json.get("message").isJsonNull())
                    ? json.get("message").getAsString()
                    : "";
                String sender = json.has("sender") ? json.get("sender").getAsString() : null;
                
                plugin.getMessageManager().sendToMinecraft(message, sender);
                
                JsonObject data = new JsonObject();
                data.addProperty("message_length", message.length());
                if (sender != null) {
                    data.addProperty("sender", sender);
                }
                sendOk(exchange, data);
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error processing message request", e);
                sendError(exchange, 400, "Invalid request");
            }
        }
    }
}
