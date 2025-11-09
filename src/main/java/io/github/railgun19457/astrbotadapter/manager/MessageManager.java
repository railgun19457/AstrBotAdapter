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
    private final String incomingFormatWithSender;
    
    public MessageManager(AstrbotAdapter plugin) {
        this.plugin = plugin;
        this.incomingFormat = plugin.getConfig().getString("message.incoming-format", "§7[§bAstrBot§7] §f{message}");
        this.incomingFormatWithSender = plugin.getConfig().getString("message.incoming-format-with-sender", "§7[§bAstrBot§7] §7<§e{sender}§7> §f{message}");
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
            String formatted;
            if (sender != null && !sender.isEmpty()) {
                formatted = incomingFormatWithSender
                    .replace("{sender}", sender)
                    .replace("{message}", message);
            } else {
                formatted = incomingFormat.replace("{message}", message);
            }
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
    
    /**
     * Send a chat message from Minecraft to external service
     */
    public void sendToExternal(String player, String message) {
        if (plugin.getWebSocketServer() != null) {
            plugin.getWebSocketServer().broadcastChatMessage(player, message);
            plugin.debug("Sent chat message to external: [" + player + "] " + message);
        }
    }
    
    /**
     * Send player join notification to external service
     */
    public void notifyPlayerJoin(String player) {
        if (plugin.getConfig().getBoolean("message.notify-join", true)) {
            if (plugin.getWebSocketServer() != null) {
                plugin.getWebSocketServer().broadcastPlayerJoin(player);
                plugin.debug("Notified player join: " + player);
            }
        }
    }
    
    /**
     * Send player leave notification to external service
     */
    public void notifyPlayerLeave(String player) {
        if (plugin.getConfig().getBoolean("message.notify-leave", true)) {
            if (plugin.getWebSocketServer() != null) {
                plugin.getWebSocketServer().broadcastPlayerLeave(player);
                plugin.debug("Notified player leave: " + player);
            }
        }
    }
}
