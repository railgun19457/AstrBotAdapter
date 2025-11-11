package io.github.railgun19457.astrbotadapter.command;

import io.github.railgun19457.astrbotadapter.AstrbotAdapter;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

@SuppressWarnings("deprecation") // ChatColor is used for Spigot compatibility
public class AstrbotCommand implements CommandExecutor {
    
    private final AstrbotAdapter plugin;
    
    public AstrbotCommand(AstrbotAdapter plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("astrbot.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令。");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                sender.sendMessage(ChatColor.YELLOW + "正在重载 AstrBot 适配器...");
                plugin.reload();
                sender.sendMessage(ChatColor.GREEN + "AstrBot 适配器重载成功！");
                break;
                
            case "status":
                sendStatus(sender);
                break;
                
            case "help":
                sendHelp(sender);
                break;
                
            default:
                sender.sendMessage(ChatColor.RED + "未知的子命令。使用 /astrbot help 查看帮助。");
                break;
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== AstrBot 适配器 ==========");
        sender.sendMessage(ChatColor.YELLOW + "/astrbot reload" + ChatColor.WHITE + " - 重载插件");
        sender.sendMessage(ChatColor.YELLOW + "/astrbot status" + ChatColor.WHITE + " - 显示服务器状态");
        sender.sendMessage(ChatColor.YELLOW + "/astrbot help" + ChatColor.WHITE + " - 显示此帮助信息");
        sender.sendMessage(ChatColor.GOLD + "=================================");
    }
    
    private void sendStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== 服务器状态 ==========");
        
        sendServerStatus(sender, "WebSocket", "websocket", plugin.getWebSocketServer(), plugin.getWebSocketServer());
        sendServerStatus(sender, "REST API", "rest-api", plugin.getRestApiServer(), plugin.getRestApiServer());
        
        // WebSocket 连接详情
        if (plugin.getWebSocketServer() != null && plugin.getWebSocketServer().isRunning()) {
            int connectedClients = plugin.getWebSocketServer().getConnectedClients();
            sender.sendMessage(ChatColor.YELLOW + "已连接客户端: " + ChatColor.WHITE + connectedClients);
            
            if (connectedClients > 0) {
                java.util.List<String> connections = plugin.getWebSocketServer().getConnectionDetails();
                sender.sendMessage(ChatColor.GRAY + "连接详情:");
                for (int i = 0; i < connections.size(); i++) {
                    sender.sendMessage(ChatColor.GRAY + "  " + (i + 1) + ". " + ChatColor.WHITE + connections.get(i));
                }
            }
        } else {
            sender.sendMessage(ChatColor.YELLOW + "已连接客户端: " + ChatColor.GRAY + "0 (服务未运行)");
        }
        
        sender.sendMessage(ChatColor.GOLD + "=============================");
    }
    
    private void sendServerStatus(CommandSender sender, String name, String configKey, Object server, Object runningChecker) {
        boolean enabled = plugin.getConfig().getBoolean(configKey + ".enabled", true);
        boolean running = server != null && (runningChecker instanceof io.github.railgun19457.astrbotadapter.server.WebSocketServer 
            ? ((io.github.railgun19457.astrbotadapter.server.WebSocketServer) runningChecker).isRunning()
            : ((io.github.railgun19457.astrbotadapter.server.RestApiServer) runningChecker).isRunning());
        int port = plugin.getConfig().getInt(configKey + ".port");
        
        String status = enabled 
            ? (running ? ChatColor.GREEN + "运行中，端口 " + port : ChatColor.RED + "错误")
            : ChatColor.GRAY + "已禁用";
        sender.sendMessage(ChatColor.YELLOW + name + ": " + status);
    }
}
