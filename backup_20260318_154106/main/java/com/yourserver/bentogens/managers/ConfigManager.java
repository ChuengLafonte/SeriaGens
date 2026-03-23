package com.yourserver.bentogens.managers;

import com.yourserver.bentogens.BentoGens;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    
    private final BentoGens plugin;
    private final Map<String, FileConfiguration> configs;
    
    public ConfigManager(BentoGens plugin) {
        this.plugin = plugin;
        this.configs = new HashMap<>();
    }
    
    /**
     * Load all configuration files
     */
    public void loadConfigs() {
        // Save default configs
        plugin.saveDefaultConfig();
        saveResource("generators.yml");
        
        // Load configs
        configs.put("config", plugin.getConfig());
        configs.put("generators", loadConfig("generators.yml"));
        
        plugin.getLogger().info("Loaded " + configs.size() + " configuration files");
    }
    
    /**
     * Reload all configurations
     */
    public void reloadConfigs() {
        plugin.reloadConfig();
        configs.clear();
        loadConfigs();
    }
    
    /**
     * Get a configuration file
     */
    public FileConfiguration getConfig(String name) {
        return configs.getOrDefault(name, plugin.getConfig());
    }
    
    /**
     * Get generators configuration
     */
    public FileConfiguration getGeneratorsConfig() {
        return getConfig("generators");
    }
    
    /**
     * Get message from config with color codes
     */
    public String getMessage(String path) {
        String message = plugin.getConfig().getString("messages." + path);
        if (message == null) {
            return "§cMessage not found: " + path;
        }
        
        // Replace prefix placeholder
        String prefix = plugin.getConfig().getString("messages.prefix", "&6[BentoGens]");
        message = message.replace("{prefix}", prefix);
        
        // Translate color codes
        return colorize(message);
    }
    
    /**
     * Get generator interval in seconds
     */
    public int getGeneratorInterval(String type) {
        return getGeneratorsConfig().getInt(type + ".interval", 20);
    }
    
    /**
     * Get generator material name
     */
    public String getGeneratorMaterial(String type) {
        return getGeneratorsConfig().getString(type + ".material", "HAY_BLOCK");
    }
    
    /**
     * Check if generator type exists
     */
    public boolean generatorExists(String type) {
        return getGeneratorsConfig().contains(type);
    }
    
    /**
     * Get all generator types
     */
    public java.util.Set<String> getAllGeneratorTypes() {
        ConfigurationSection section = getGeneratorsConfig().getRoot();
        return section != null ? section.getKeys(false) : new java.util.HashSet<>();
    }
    
    /**
     * Save resource from JAR to plugin folder
     */
    private void saveResource(String resourcePath) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
        }
    }
    
    /**
     * Load configuration file
     */
    private FileConfiguration loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }
    
    /**
     * Translate color codes (&-style to §)
     */
    public String colorize(String message) {
        if (message == null) return "";
        return message.replace('&', '§');
    }
}