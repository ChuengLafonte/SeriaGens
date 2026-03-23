package com.yourserver.bentogens.integration;

import com.yourserver.bentogens.BentoGens;
import com.yourserver.bentogens.models.Generator;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

/**
 * FancyHolograms integration using reflection (NO dependency!)
 * Like BentoBox integration - uses server JAR via reflection
 */
public class FancyHologramsIntegration {
    
    private final BentoGens plugin;
    private Object hologramManager;
    private boolean enabled = false;
    
    // Track holograms by generator location
    private final Map<Location, String> activeHolograms = new HashMap<>();
    
    // Reflection classes/methods
    private Class<?> textHologramDataClass;
    private Class<?> builderClass;
    private Method createMethod;
    private Method deleteMethod;
    private Method getHologramMethod;
    
    public FancyHologramsIntegration(BentoGens plugin) {
        this.plugin = plugin;
        tryEnable();
    }
    
    /**
     * Try to enable FancyHolograms integration via REFLECTION
     */
    private void tryEnable() {
        Plugin fancyHolograms = plugin.getServer().getPluginManager().getPlugin("FancyHolograms");
        
        if (fancyHolograms == null) {
            plugin.getLogger().info("FancyHolograms not found - hologram notifications disabled");
            return;
        }
        
        try {
            // Get HologramManager from plugin (it IS the manager)
            hologramManager = fancyHolograms;
            
            // Load classes via reflection
            textHologramDataClass = Class.forName("de.oliver.fancyholograms.api.data.TextHologramData");
            builderClass = Class.forName("de.oliver.fancyholograms.api.data.TextHologramData$Builder");
            
            // Get methods from HologramManager
            Class<?> hologramManagerClass = hologramManager.getClass();
            createMethod = hologramManagerClass.getMethod("create", 
                Class.forName("de.oliver.fancyholograms.api.data.HologramData"));
            deleteMethod = hologramManagerClass.getMethod("delete", String.class);
            getHologramMethod = hologramManagerClass.getMethod("getHologram", String.class);
            
            enabled = true;
            plugin.getLogger().info("✅ FancyHolograms integration enabled! (reflection)");
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to enable FancyHolograms integration: " + e.getMessage());
            enabled = false;
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Show corruption hologram above generator using REFLECTION
     */
    public void showCorruptionHologram(Generator generator) {
        if (!enabled || !plugin.getConfig().getBoolean("hologram.corruption.enabled", true)) {
            return;
        }
        
        Location location = generator.getLocation();
        
        if (activeHolograms.containsKey(location)) {
            return;
        }
        
        try {
            double height = plugin.getConfig().getDouble("hologram.corruption.height", 2.0);
            Location hologramLoc = location.clone().add(0.5, height, 0.5);
            
            String hologramId = "bentogens_corrupted_" + UUID.randomUUID().toString().substring(0, 8);
            
            // Get hologram lines
            List<String> lines = plugin.getConfig().getStringList("hologram.corruption.lines");
            if (lines.isEmpty()) {
                int cost = plugin.getConfigManager().getGeneratorsConfig()
                    .getInt(generator.getType() + ".corrupted.cost", 100);
                lines = Arrays.asList(
                    "&c&l⚠ GENERATOR RUSAK!",
                    "&7Repair dengan &e/genset",
                    "&7Cost: &c●" + cost
                );
            }
            
            // Colorize and replace placeholders
            List<String> coloredLines = new ArrayList<>();
            for (String line : lines) {
                int cost = plugin.getConfigManager().getGeneratorsConfig()
                    .getInt(generator.getType() + ".corrupted.cost", 100);
                String colored = line.replace("{cost}", String.valueOf(cost));
                coloredLines.add(plugin.getConfigManager().colorize(colored));
            }
            
            // Create TextHologramData using reflection
            // TextHologramData.builder()
            Method builderMethod = textHologramDataClass.getMethod("builder");
            Object builder = builderMethod.invoke(null);
            
            // .name(hologramId)
            Method nameMethod = builderClass.getMethod("name", String.class);
            builder = nameMethod.invoke(builder, hologramId);
            
            // .location(hologramLoc)
            Method locationMethod = builderClass.getMethod("location", Location.class);
            builder = locationMethod.invoke(builder, hologramLoc);
            
            // .text(coloredLines)
            Method textMethod = builderClass.getMethod("text", List.class);
            builder = textMethod.invoke(builder, coloredLines);
            
            // .build()
            Method buildMethod = builderClass.getMethod("build");
            Object hologramData = buildMethod.invoke(builder);
            
            // hologramManager.create(hologramData)
            createMethod.invoke(hologramManager, hologramData);
            
            activeHolograms.put(location, hologramId);
            
            plugin.getLogger().info("Created corruption hologram at " + 
                location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create corruption hologram: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Remove corruption hologram using REFLECTION
     */
    public void removeCorruptionHologram(Generator generator) {
        if (!enabled) {
            return;
        }
        
        Location location = generator.getLocation();
        String hologramId = activeHolograms.get(location);
        
        if (hologramId == null) {
            return;
        }
        
        try {
            // Check if hologram exists
            Object hologram = getHologramMethod.invoke(hologramManager, hologramId);
            
            if (hologram != null) {
                // Delete hologram
                deleteMethod.invoke(hologramManager, hologramId);
                
                plugin.getLogger().info("Removed corruption hologram at " + 
                    location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
            }
            
            activeHolograms.remove(location);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to remove corruption hologram: " + e.getMessage());
        }
    }
    
    /**
     * Remove all corruption holograms (for shutdown)
     */
    public void removeAllHolograms() {
        if (!enabled) {
            return;
        }
        
        plugin.getLogger().info("Removing " + activeHolograms.size() + " corruption holograms...");
        
        for (String hologramId : new ArrayList<>(activeHolograms.values())) {
            try {
                deleteMethod.invoke(hologramManager, hologramId);
            } catch (Exception e) {
                // Ignore errors during shutdown
            }
        }
        
        activeHolograms.clear();
    }
    
    /**
     * Check if location has active hologram
     */
    public boolean hasHologram(Location location) {
        return activeHolograms.containsKey(location);
    }
    
    /**
     * Reload integration
     */
    public void reload() {
        removeAllHolograms();
        enabled = false;
        tryEnable();
    }
}
