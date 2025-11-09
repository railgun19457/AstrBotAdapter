package io.github.railgun19457.astrbotadapter.listener;

import io.github.railgun19457.astrbotadapter.AstrbotAdapter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    
    private final AstrbotAdapter plugin;
    
    public PlayerListener(AstrbotAdapter plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        String player = event.getPlayer().getName();
        plugin.getMessageManager().notifyPlayerJoin(player);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        String player = event.getPlayer().getName();
        plugin.getMessageManager().notifyPlayerLeave(player);
    }
}
