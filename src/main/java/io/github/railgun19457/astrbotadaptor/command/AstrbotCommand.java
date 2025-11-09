package io.github.railgun19457.astrbotadaptor.command;

import io.github.railgun19457.astrbotadaptor.AstrbotAdaptor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

@SuppressWarnings("deprecation") // ChatColor is used for Spigot compatibility
public class AstrbotCommand implements CommandExecutor {
    
    private final AstrbotAdaptor plugin;
    
    public AstrbotCommand(AstrbotAdaptor plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("astrbot.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                sender.sendMessage(ChatColor.YELLOW + "Reloading AstrBot Adaptor...");
                plugin.reload();
                sender.sendMessage(ChatColor.GREEN + "AstrBot Adaptor reloaded successfully!");
                break;
                
            case "status":
                sendStatus(sender);
                break;
                
            case "help":
                sendHelp(sender);
                break;
                
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /astrbot help for help.");
                break;
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== AstrBot Adaptor ==========");
        sender.sendMessage(ChatColor.YELLOW + "/astrbot reload" + ChatColor.WHITE + " - Reload the plugin");
        sender.sendMessage(ChatColor.YELLOW + "/astrbot status" + ChatColor.WHITE + " - Show server status");
        sender.sendMessage(ChatColor.YELLOW + "/astrbot help" + ChatColor.WHITE + " - Show this help message");
        sender.sendMessage(ChatColor.GOLD + "=====================================");
    }
    
    private void sendStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== Server Status ==========");
        
        // WebSocket status
        boolean wsEnabled = plugin.getConfig().getBoolean("websocket.enabled", true);
        boolean wsRunning = plugin.getWebSocketServer() != null && plugin.getWebSocketServer().isRunning();
        int wsPort = plugin.getConfig().getInt("websocket.port", 8765);
        sender.sendMessage(ChatColor.YELLOW + "WebSocket: " + 
            (wsEnabled ? (wsRunning ? ChatColor.GREEN + "Running on port " + wsPort : ChatColor.RED + "Error") : ChatColor.GRAY + "Disabled"));
        
        // REST API status
        boolean apiEnabled = plugin.getConfig().getBoolean("rest-api.enabled", true);
        boolean apiRunning = plugin.getRestApiServer() != null && plugin.getRestApiServer().isRunning();
        int apiPort = plugin.getConfig().getInt("rest-api.port", 8766);
        sender.sendMessage(ChatColor.YELLOW + "REST API: " + 
            (apiEnabled ? (apiRunning ? ChatColor.GREEN + "Running on port " + apiPort : ChatColor.RED + "Error") : ChatColor.GRAY + "Disabled"));
        
        // Connected clients
        int connectedClients = plugin.getWebSocketServer() != null ? plugin.getWebSocketServer().getConnectedClients() : 0;
        sender.sendMessage(ChatColor.YELLOW + "Connected Clients: " + ChatColor.WHITE + connectedClients);
        
        sender.sendMessage(ChatColor.GOLD + "===================================");
    }
}
