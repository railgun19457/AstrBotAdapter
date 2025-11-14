package io.github.railgun19457.astrbotadapter.manager;

import io.github.railgun19457.astrbotadapter.AstrbotAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

@SuppressWarnings("deprecation") // ChatColor and legacy methods are used for Spigot compatibility
public class MessageManager {
    
    private final AstrbotAdapter plugin;
    private final String incomingFormat;
    
    public MessageManager(AstrbotAdapter plugin) {
        this.plugin = plugin;
        this.incomingFormat = plugin.getConfig().getString(
            "message.incoming-format",
            "§7[§bAstrBot§7] §7<§e{sender}§7> §f{message}"
        );
    }
    
    /**
     * Send a message from external source to Minecraft server
     */
    public void sendToMinecraft(String message) {
        sendToMinecraft(message, null);
    }
    
    /**
     * Send a message from external source to Minecraft server with sender info
     * @param message The message content
     * @param sender The sender name (optional)
     */
    public void sendToMinecraft(String message, String sender) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            String formatted = incomingFormat
                .replace("{sender}", sender == null ? "" : sender)
                .replace("{message}", message);
            formatted = ChatColor.translateAlternateColorCodes('&', formatted);
            
            // Try to use Paper's Adventure API if available
            try {
                Component component = LegacyComponentSerializer.legacySection().deserialize(formatted);
                Bukkit.broadcast(component);
            } catch (NoClassDefFoundError e) {
                // Fallback to legacy Bukkit API
                Bukkit.broadcastMessage(formatted);
            }
            
            plugin.debug("Sent message to Minecraft: " + (sender != null ? "[" + sender + "] " : "") + message);
        });
    }

    // Simplified: single template with {sender} and {message} only.
    
    /**
     * Send a chat message from Minecraft to external service
     */
    public void sendToExternal(String player, String message) {
        if (plugin.getWebSocketServer() == null) {
            notifyPlayer(player, ChatColor.RED + "[ERR] " + ChatColor.GRAY + "转发失败：外部服务未启用。");
            return;
        }
        
        try {
            int clients = plugin.getWebSocketServer().getConnectedClients();
            plugin.getWebSocketServer().broadcastChatMessage(player, message);
            plugin.debug("Sent chat message to external: [" + player + "] " + message);
            
            String notification = clients > 0
                //? ChatColor.GREEN + "[OK] " + ChatColor.GRAY + "消息已成功转发到外部服务。"
                ? ChatColor.GRAY + "消息已成功转发到外部服务。"                
                : ChatColor.RED + "[ERR] " + ChatColor.GRAY + "转发失败：没有可用的外部客户端连接。";
            notifyPlayer(player, notification);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error sending chat to external: " + e.getMessage());
            plugin.debug("Error sending chat to external: " + e.toString());
            notifyPlayer(player, ChatColor.RED + "[ERR] " + ChatColor.GRAY + "转发失败：" + e.getMessage());
        }
    }
    
    /**
     * Send an AI chat message from Minecraft to external service
     */
    public void sendAiChatToExternal(String player, String message) {
        if (plugin.getWebSocketServer() == null) {
            notifyPlayer(player, ChatColor.RED + "[AI] " + ChatColor.GRAY + "AI 服务未启用。");
            return;
        }
        
        try {
            int clients = plugin.getWebSocketServer().getConnectedClients();
            plugin.getWebSocketServer().broadcastAiChat(player, message);
            plugin.debug("Sent AI chat message to external: [" + player + "] " + message);
            
            String notification = clients > 0
                //? ChatColor.GREEN + "[AI] " + ChatColor.GRAY + "AI 对话已发送..."
                ? ChatColor.GRAY + "AI 对话已发送..."
                : ChatColor.RED + "[AI] " + ChatColor.GRAY + "发送失败：没有可用的 AI 服务连接。";
            notifyPlayer(player, notification);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error sending AI chat to external: " + e.getMessage());
            plugin.debug("Error sending AI chat to external: " + e.toString());
            notifyPlayer(player, ChatColor.RED + "[AI] " + ChatColor.GRAY + "发送失败：" + e.getMessage());
        }
    }
    
    private void notifyPlayer(String playerName, String message) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            org.bukkit.entity.Player p = plugin.getServer().getPlayerExact(playerName);
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        });
    }
    
    /**
     * Send player join notification to external service
     */
    public void notifyPlayerJoin(String player) {
        notifyPlayerEvent(player, "notify-join", ws -> ws.broadcastPlayerJoin(player), "join");
    }
    
    public void notifyPlayerLeave(String player) {
        notifyPlayerEvent(player, "notify-leave", ws -> ws.broadcastPlayerLeave(player), "leave");
    }
    
    private void notifyPlayerEvent(String player, String configKey, java.util.function.Consumer<io.github.railgun19457.astrbotadapter.server.WebSocketServer> broadcaster, String eventType) {
        if (plugin.getConfig().getBoolean("message." + configKey, true) && plugin.getWebSocketServer() != null) {
            broadcaster.accept(plugin.getWebSocketServer());
            plugin.debug("Notified player " + eventType + ": " + player);
        }
    }
}
