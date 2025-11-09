package io.github.railgun19457.astrbotadapter.manager;

import io.github.railgun19457.astrbotadapter.AstrbotAdapter;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("deprecation") // Some deprecated methods are used for compatibility
public class StatusManager {
    
    private final AstrbotAdapter plugin;
    private final ScheduledExecutorService scheduler;
    private final boolean enableTps;
    private final boolean enableMemory;
    
    private double lastTps = 20.0;
    
    public StatusManager(AstrbotAdapter plugin) {
        this.plugin = plugin;
        this.enableTps = plugin.getConfig().getBoolean("status.enable-tps", true);
        this.enableMemory = plugin.getConfig().getBoolean("status.enable-memory", true);
        
        // Create scheduler for periodic updates
        this.scheduler = Executors.newScheduledThreadPool(1);
        
        // Schedule periodic TPS monitoring
        int updateInterval = plugin.getConfig().getInt("status.update-interval", 30);
        scheduler.scheduleAtFixedRate(this::updateTps, 0, updateInterval, TimeUnit.SECONDS);
    }
    
    private void updateTps() {
        try {
            // Try to get TPS from Paper API
            lastTps = Bukkit.getTPS()[0];
        } catch (NoSuchMethodError e) {
            // TPS API not available, keep last value or default
            lastTps = 20.0;
        }
    }
    
    /**
     * Get complete server status
     * This method is thread-safe and can be called from any thread
     */
    public JsonObject getStatus() {
        // If we're already on the main thread, get status directly
        if (Bukkit.isPrimaryThread()) {
            return getStatusSync();
        }
        
        // Otherwise, schedule on main thread and wait for result
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    JsonObject status = getStatusSync();
                    future.complete(status);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        }.runTask(plugin);
        
        try {
            // Wait up to 5 seconds for the result
            return future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            plugin.getLogger().warning("Failed to get status: " + e.getMessage());
            // Return basic status on error
            JsonObject errorStatus = new JsonObject();
            errorStatus.addProperty("online", true);
            errorStatus.addProperty("error", "Failed to get complete status: " + e.getMessage());
            return errorStatus;
        }
    }
    
    /**
     * Get server status synchronously (must be called from main thread)
     */
    private JsonObject getStatusSync() {
        JsonObject status = new JsonObject();
        
        // Basic info
        status.addProperty("online", true);
        status.addProperty("version", Bukkit.getVersion());
        status.addProperty("minecraft_version", Bukkit.getBukkitVersion());
        status.addProperty("max_players", Bukkit.getMaxPlayers());
        status.addProperty("online_players", Bukkit.getOnlinePlayers().size());
        
        // TPS info
        if (enableTps) {
            try {
                double[] tps = Bukkit.getTPS();
                JsonArray tpsArray = new JsonArray();
                for (double t : tps) {
                    tpsArray.add(Math.min(20.0, t));
                }
                status.add("tps", tpsArray);
            } catch (NoSuchMethodError e) {
                status.addProperty("tps", lastTps);
            }
        }
        
        // Memory info
        if (enableMemory) {
            status.add("memory", getMemoryInfo());
        }
        
        // World info
        status.add("worlds", getWorldsInfo());
        
        // Players list
        status.add("players", getPlayersArray());
        
        return status;
    }
    
    /**
     * Get detailed players information
     * This method is thread-safe and can be called from any thread
     */
    public JsonObject getPlayersInfo() {
        // If we're already on the main thread, get info directly
        if (Bukkit.isPrimaryThread()) {
            return getPlayersInfoSync();
        }
        
        // Otherwise, schedule on main thread and wait for result
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    JsonObject info = getPlayersInfoSync();
                    future.complete(info);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        }.runTask(plugin);
        
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            plugin.getLogger().warning("Failed to get players info: " + e.getMessage());
            JsonObject errorInfo = new JsonObject();
            errorInfo.addProperty("error", "Failed to get players info: " + e.getMessage());
            return errorInfo;
        }
    }
    
    /**
     * Get players information synchronously (must be called from main thread)
     */
    private JsonObject getPlayersInfoSync() {
        JsonObject info = new JsonObject();
        
        info.addProperty("online", Bukkit.getOnlinePlayers().size());
        info.addProperty("max", Bukkit.getMaxPlayers());
        info.add("list", getPlayersDetailedArray());
        
        return info;
    }
    
    private JsonArray getPlayersArray() {
        JsonArray players = new JsonArray();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            players.add(player.getName());
        }
        
        return players;
    }
    
    private JsonArray getPlayersDetailedArray() {
        JsonArray players = new JsonArray();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            JsonObject playerInfo = new JsonObject();
            playerInfo.addProperty("name", player.getName());
            playerInfo.addProperty("uuid", player.getUniqueId().toString());
            playerInfo.addProperty("health", player.getHealth());
            playerInfo.addProperty("max_health", player.getMaxHealth());
            playerInfo.addProperty("level", player.getLevel());
            playerInfo.addProperty("exp", player.getExp());
            playerInfo.addProperty("gamemode", player.getGameMode().name());
            playerInfo.addProperty("world", player.getWorld().getName());
            
            JsonObject location = new JsonObject();
            location.addProperty("x", player.getLocation().getX());
            location.addProperty("y", player.getLocation().getY());
            location.addProperty("z", player.getLocation().getZ());
            playerInfo.add("location", location);
            
            playerInfo.addProperty("ping", getPing(player));
            playerInfo.addProperty("is_op", player.isOp());
            
            players.add(playerInfo);
        }
        
        return players;
    }
    
    private int getPing(Player player) {
        try {
            return player.getPing();
        } catch (NoSuchMethodError e) {
            return -1;
        }
    }
    
    private JsonObject getMemoryInfo() {
        JsonObject memory = new JsonObject();
        
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        
        long used = heapUsage.getUsed() / (1024 * 1024); // Convert to MB
        long max = heapUsage.getMax() / (1024 * 1024); // Convert to MB
        long committed = heapUsage.getCommitted() / (1024 * 1024); // Convert to MB
        
        memory.addProperty("used_mb", used);
        memory.addProperty("max_mb", max);
        memory.addProperty("committed_mb", committed);
        memory.addProperty("free_mb", max - used);
        memory.addProperty("usage_percent", (double) used / max * 100);
        
        return memory;
    }
    
    private JsonArray getWorldsInfo() {
        JsonArray worlds = new JsonArray();
        
        for (World world : Bukkit.getWorlds()) {
            JsonObject worldInfo = new JsonObject();
            worldInfo.addProperty("name", world.getName());
            worldInfo.addProperty("players", world.getPlayers().size());
            worldInfo.addProperty("time", world.getTime());
            worldInfo.addProperty("difficulty", world.getDifficulty().name());
            worldInfo.addProperty("loaded_chunks", world.getLoadedChunks().length);
            
            try {
                worldInfo.addProperty("entities", world.getEntityCount());
            } catch (NoSuchMethodError e) {
                worldInfo.addProperty("entities", world.getEntities().size());
            }
            
            worlds.add(worldInfo);
        }
        
        return worlds;
    }
    
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
    }
}
