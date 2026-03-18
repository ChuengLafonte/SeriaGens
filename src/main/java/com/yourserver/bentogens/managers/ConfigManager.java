package com.yourserver.bentogens.managers;

import com.yourserver.bentogens.BentoGens;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {
    
    private final BentoGens plugin;
    private FileConfiguration generatorsConfig;
    
    // HEX color pattern
    private static final Pattern HEX_PATTERN = Pattern.compile("(?:&#|#)([A-Fa-f0-9]{6})");
    
    public ConfigManager(BentoGens plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load all config files
     */
    public void loadConfigs() {
        // Reload main config
        plugin.reloadConfig();
        
        // Load generators.yml
        loadGeneratorsConfig();
        
        plugin.getLogger().info("Configuration loaded!");
    }
    
    /**
     * Load generators.yml
     */
    private void loadGeneratorsConfig() {
        File file = new File(plugin.getDataFolder(), "generators.yml");
        
        if (!file.exists()) {
            plugin.saveResource("generators.yml", false);
        }
        
        generatorsConfig = YamlConfiguration.loadConfiguration(file);
    }
    
    /**
     * Get generators config
     */
    public FileConfiguration getGeneratorsConfig() {
        return generatorsConfig;
    }
    
    /**
     * Get all generator types
     */
    public Set<String> getAllGeneratorTypes() {
        return generatorsConfig.getKeys(false);
    }
    
    /**
     * Get generator material
     */
    public String getGeneratorMaterial(String type) {
        return generatorsConfig.getString(type + ".material", "STONE");
    }
    
    /**
     * Get generator interval
     */
    public int getGeneratorInterval(String type) {
        return generatorsConfig.getInt(type + ".interval", 20);
    }
    
    /**
     * Get generator display name
     */
    public String getGeneratorDisplayName(String type) {
        return colorize(generatorsConfig.getString(type + ".display-name", type));
    }
    
    /**
     * Check if generator type exists
     */
    public boolean generatorExists(String type) {
        return generatorsConfig.contains(type);
    }
    
    /**
     * Get message from config
     */
    public String getMessage(String key) {
        String msg = plugin.getConfig().getString("messages." + key);
        
        if (msg == null) {
            return colorize("&c[BentoGens] Message not found: " + key);
        }
        
        return colorize(msg.replace("{prefix}", 
            plugin.getConfig().getString("messages.prefix", "&6[BentoGens]")));
    }
    
    /**
     * Colorize string with both legacy and HEX colors
     */
    public String colorize(String text) {
        if (text == null) {
            return "";
        }
        
        // Process HEX colors first
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + hex).toString());
        }
        matcher.appendTail(buffer);
        
        // Then process legacy colors
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
    
    /**
     * Colorize list
     */
    public List<String> colorize(List<String> list) {
        List<String> colored = new ArrayList<>();
        for (String line : list) {
            colored.add(colorize(line));
        }
        return colored;
    }
}