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
    
    public JsonObject getStatus() {
        return executeOnMainThread(this::getStatusSync, "Failed to get status");
    }
    
    public JsonObject getPlayersInfo() {
        return executeOnMainThread(this::getPlayersInfoSync, "Failed to get players info");
    }
    
    private JsonObject executeOnMainThread(java.util.function.Supplier<JsonObject> supplier, String errorMessage) {
        if (Bukkit.isPrimaryThread()) {
            return supplier.get();
        }
        
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    future.complete(supplier.get());
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        }.runTask(plugin);
        
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            plugin.getLogger().warning(errorMessage + ": " + e.getMessage());
            JsonObject error = new JsonObject();
            error.addProperty("error", errorMessage + ": " + e.getMessage());
            return error;
        }
    }
    
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
    long max = heapUsage.getMax() / (1024 * 1024); // Convert to MB (can be -1)
    long committed = heapUsage.getCommitted() / (1024 * 1024); // Convert to MB
        
        memory.addProperty("used_mb", used);
        memory.addProperty("max_mb", max);
        memory.addProperty("committed_mb", committed);
        long freeMb = (max > 0) ? Math.max(0, max - used) : Math.max(0, committed - used);
        memory.addProperty("free_mb", freeMb);
        double percent = (max > 0) ? (double) used / max * 100.0 : -1.0; // -1 denotes unavailable
        if (percent >= 0) {
            // clamp to [0,100]
            percent = Math.max(0.0, Math.min(100.0, percent));
        }
        memory.addProperty("usage_percent", percent);
        
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
