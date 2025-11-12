package io.github.railgun19457.astrbotadapter.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Utility class for updating configuration files with missing fields from default config
 */
public class ConfigUpdater {
    
    private final Plugin plugin;
    private final File configFile;
    
    public ConfigUpdater(Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }
    
    /**
     * Update configuration with missing fields from default config
     */
    public void updateConfig() {
        if (!configFile.exists()) {
            plugin.getLogger().warning("Config file does not exist, skipping update");
            return;
        }
        
        plugin.getLogger().info("Checking for missing configuration fields...");
        
        try {
            List<String> currentLines = Files.readAllLines(configFile.toPath(), StandardCharsets.UTF_8);
            List<String> defaultLines = readDefaultConfig();
            
            if (defaultLines.isEmpty()) {
                plugin.getLogger().warning("Could not read default config");
                return;
            }
            
            List<String> missingKeys = findMissingKeys(currentLines, defaultLines);
            
            if (missingKeys.isEmpty()) {
                plugin.getLogger().info("All configuration fields are present");
                return;
            }
            
            plugin.getLogger().info("Found " + missingKeys.size() + " missing field(s): " + 
                                   String.join(", ", missingKeys));
            
            List<String> updatedLines = new ArrayList<>(currentLines);
            boolean modified = false;
            
            for (String key : missingKeys) {
                if (addMissingField(updatedLines, defaultLines, key)) {
                    modified = true;
                }
            }
            
            if (modified) {
                Files.write(configFile.toPath(), updatedLines, StandardCharsets.UTF_8,
                           StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                plugin.reloadConfig();
                plugin.getLogger().info("Configuration updated successfully!");
            }
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update config", e);
        }
    }
    
    private List<String> readDefaultConfig() throws IOException {
        List<String> lines = new ArrayList<>();
        try (var stream = plugin.getResource("config.yml");
             var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
    
    private List<String> findMissingKeys(List<String> currentLines, List<String> defaultLines) {
        List<String> missingKeys = new ArrayList<>();
        
        for (String defaultLine : defaultLines) {
            String trimmed = defaultLine.trim();
            
            // Skip comments, empty lines, and section headers
            if (trimmed.isEmpty() || trimmed.startsWith("#") ||
                (trimmed.endsWith(":") && !trimmed.contains(": "))) {
                continue;
            }
            
            int colonIndex = trimmed.indexOf(':');
            if (colonIndex <= 0) continue;
            
            String key = trimmed.substring(0, colonIndex).trim();
            
            // Check if key exists in current config
            boolean exists = currentLines.stream()
                .map(String::trim)
                .anyMatch(line -> line.startsWith(key + ":"));
            
            if (!exists) {
                missingKeys.add(key);
            }
        }
        
        return missingKeys;
    }
    
    private boolean addMissingField(List<String> currentLines, List<String> defaultLines, String key) {
        // Find field in default config
        int fieldIndex = -1;
        for (int i = 0; i < defaultLines.size(); i++) {
            if (defaultLines.get(i).trim().startsWith(key + ":")) {
                fieldIndex = i;
                break;
            }
        }
        
        if (fieldIndex == -1) return false;
        
        // Collect field and preceding comments
        List<String> linesToInsert = new ArrayList<>();
        int startIndex = fieldIndex;
        
        for (int i = fieldIndex - 1; i >= 0; i--) {
            String line = defaultLines.get(i);
            String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                startIndex = i;
            } else if (!trimmed.isEmpty()) {
                break;
            }
        }
        
        for (int i = startIndex; i <= fieldIndex; i++) {
            linesToInsert.add(defaultLines.get(i));
        }
        
        // Find insertion point
        String insertAfterKey = findPreviousSiblingKey(defaultLines, fieldIndex);
        int insertIndex = findInsertionPoint(currentLines, insertAfterKey);
        
        if (insertIndex == -1) {
            plugin.getLogger().warning("Could not find insertion point for: " + key);
            return false;
        }
        
        currentLines.addAll(insertIndex, linesToInsert);
        plugin.getLogger().info("Added missing field: " + key);
        return true;
    }
    
    private String findPreviousSiblingKey(List<String> defaultLines, int targetIndex) {
        String targetLine = defaultLines.get(targetIndex);
        int targetIndent = getIndentation(targetLine);
        
        for (int i = targetIndex - 1; i >= 0; i--) {
            String line = defaultLines.get(i);
            String trimmed = line.trim();
            
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            
            if (getIndentation(line) == targetIndent && trimmed.contains(":")) {
                int colonIndex = trimmed.indexOf(':');
                return trimmed.substring(0, colonIndex).trim();
            }
        }
        
        return null;
    }
    
    private int findInsertionPoint(List<String> lines, String afterKey) {
        if (afterKey == null) return -1;
        
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().startsWith(afterKey + ":")) {
                return i + 1;
            }
        }
        
        return -1;
    }
    
    private int getIndentation(String line) {
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) != ' ') {
                return i;
            }
        }
        return 0;
    }
}
