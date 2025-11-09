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
    
    public ChatListener(AstrbotAdapter plugin) {
        this.plugin = plugin;
    }
    
    // For Paper servers (1.19+)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        try {
            String player = event.getPlayer().getName();
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());
            
            plugin.getMessageManager().sendToExternal(player, message);
        } catch (NoClassDefFoundError e) {
            // Event not available, ignore
        }
    }
    
    // For Spigot/older Paper servers
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        String player = event.getPlayer().getName();
        String message = event.getMessage();
        
        plugin.getMessageManager().sendToExternal(player, message);
    }
}
