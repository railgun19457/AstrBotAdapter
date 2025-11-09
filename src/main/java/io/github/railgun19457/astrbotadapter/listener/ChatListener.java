package io.github.railgun19457.astrbotadapter.listener;

import io.github.railgun19457.astrbotadapter.AstrbotAdapter;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

@SuppressWarnings("deprecation") // AsyncPlayerChatEvent is used for Spigot compatibility
public class ChatListener implements Listener {
    
    private final AstrbotAdapter plugin;
    private final String forwardPrefix;
    
    public ChatListener(AstrbotAdapter plugin) {
        this.plugin = plugin;
        this.forwardPrefix = plugin.getConfig().getString("message.forward-prefix", "");
    }
    
    // For Paper servers (1.19+)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        try {
            String player = event.getPlayer().getName();
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());
            
            // Check if message should be forwarded
            if (shouldForward(message)) {
                // Remove prefix if it exists
                String forwardMessage = removePrefix(message);
                plugin.getMessageManager().sendToExternal(player, forwardMessage);
            }
        } catch (NoClassDefFoundError e) {
            // Event not available, ignore
        }
    }
    
    // For Spigot/older Paper servers
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        String player = event.getPlayer().getName();
        String message = event.getMessage();
        
        // Check if message should be forwarded
        if (shouldForward(message)) {
            // Remove prefix if it exists
            String forwardMessage = removePrefix(message);
            plugin.getMessageManager().sendToExternal(player, forwardMessage);
        }
    }
    
    /**
     * Check if message should be forwarded based on prefix
     */
    private boolean shouldForward(String message) {
        // If prefix is empty, forward all messages
        if (forwardPrefix == null || forwardPrefix.isEmpty()) {
            return true;
        }
        // Check if message starts with prefix
        return message.startsWith(forwardPrefix);
    }
    
    /**
     * Remove prefix from message if it exists
     */
    private String removePrefix(String message) {
        if (forwardPrefix != null && !forwardPrefix.isEmpty() && message.startsWith(forwardPrefix)) {
            return message.substring(forwardPrefix.length());
        }
        return message;
    }
}
