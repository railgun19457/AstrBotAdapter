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
    private final String aiChatPrefix;
    private final boolean usePaperEvent;
    
    public ChatListener(AstrbotAdapter plugin) {
        this.plugin = plugin;
        this.forwardPrefix = plugin.getConfig().getString("message.forward-prefix", "");
        this.aiChatPrefix = plugin.getConfig().getString("message.ai-chat-prefix", "");
        
        // Detect if Paper's AsyncChatEvent is available
        boolean paperEventAvailable = false;
        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            paperEventAvailable = true;
            plugin.getLogger().info("Using Paper AsyncChatEvent");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("Using Spigot AsyncPlayerChatEvent");
        }
        this.usePaperEvent = paperEventAvailable;
    }
    
    // For Paper servers (1.19+)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        // Only handle this event if Paper event is available
        if (!usePaperEvent) {
            return;
        }
        
        try {
            String player = event.getPlayer().getName();
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());
            
            plugin.debug("Paper AsyncChatEvent triggered for: " + player + " - " + message);
            
            // Check if this is an AI chat message
            if (isAiChat(message)) {
                String aiMessage = removeAiPrefix(message);
                plugin.getMessageManager().sendAiChatToExternal(player, aiMessage);
                return;
            }
            
            // Check if message should be forwarded as normal chat
            if (shouldForward(message)) {
                // Remove prefix if it exists
                String forwardMessage = removePrefix(message);
                plugin.getMessageManager().sendToExternal(player, forwardMessage);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling AsyncChatEvent: " + e.getMessage());
        }
    }
    
    // For Spigot/older Paper servers
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        // Only handle this event if Paper event is NOT available
        if (usePaperEvent) {
            return;
        }
        
        String player = event.getPlayer().getName();
        String message = event.getMessage();
        
        plugin.debug("Spigot AsyncPlayerChatEvent triggered for: " + player + " - " + message);
        
        // Check if this is an AI chat message
        if (isAiChat(message)) {
            String aiMessage = removeAiPrefix(message);
            plugin.getMessageManager().sendAiChatToExternal(player, aiMessage);
            return;
        }
        
        // Check if message should be forwarded as normal chat
        if (shouldForward(message)) {
            // Remove prefix if it exists
            String forwardMessage = removePrefix(message);
            plugin.getMessageManager().sendToExternal(player, forwardMessage);
        }
    }
    
    /**
     * Check if message is an AI chat message
     */
    private boolean isAiChat(String message) {
        return aiChatPrefix != null && !aiChatPrefix.isEmpty() && message.startsWith(aiChatPrefix);
    }
    
    /**
     * Remove AI chat prefix from message
     */
    private String removeAiPrefix(String message) {
        if (isAiChat(message)) {
            return message.substring(aiChatPrefix.length()).trim();
        }
        return message;
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
