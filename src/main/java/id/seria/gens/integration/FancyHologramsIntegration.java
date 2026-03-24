package id.seria.gens.integration;

import id.seria.gens.SeriaGens;
import id.seria.gens.models.Generator;
import org.bukkit.Location;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;

import java.util.*;

public class FancyHologramsIntegration {
    
    private final SeriaGens plugin;
    private boolean enabled = false;
    private HologramManager hologramManager;
    
    private final Map<Location, String> activeHolograms = new HashMap<>();
    
    public FancyHologramsIntegration(SeriaGens plugin) {
        this.plugin = plugin;
        tryEnable();
    }
    
    private void tryEnable() {
        if (plugin.getServer().getPluginManager().getPlugin("FancyHolograms") != null) {
            this.hologramManager = FancyHologramsPlugin.get().getHologramManager();
            this.enabled = true;
            plugin.getLogger().info("✅ FancyHolograms integration enabled! (Native API)");
        } else {
            plugin.getLogger().info("FancyHolograms not found - hologram notifications disabled");
        }
    }
    
    public boolean isEnabled() { return enabled; }
    
    public void showCorruptionHologram(Generator generator) {
        if (!enabled || !plugin.getConfig().getBoolean("hologram.corruption.enabled", true)) return;
        
        Location location = generator.getLocation();
        if (activeHolograms.containsKey(location)) return;
        
        try {
            double height = plugin.getConfig().getDouble("hologram.corruption.height", 2.0);
            Location hologramLoc = location.clone().add(0.5, height, 0.5);
            
            // ID Unik untuk Project Seria
            String hologramId = "seriagens_corrupted_" + UUID.randomUUID().toString().substring(0, 8);
            
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
            
            List<String> coloredLines = new ArrayList<>();
            for (String line : lines) {
                int cost = plugin.getConfigManager().getGeneratorsConfig()
                    .getInt(generator.getType() + ".corrupted.cost", 100);
                String colored = line.replace("{cost}", String.valueOf(cost));
                coloredLines.add(plugin.getConfigManager().colorize(colored));
            }
            
            // Native API Constructor untuk v2.9.1
            TextHologramData textData = new TextHologramData(hologramId, hologramLoc);
            textData.setText(coloredLines);
            
            Hologram hologram = hologramManager.create(textData);
            hologramManager.addHologram(hologram);
            
            activeHolograms.put(location, hologramId);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Gagal membuat hologram: " + e.getMessage());
        }
    }
    
    public void removeCorruptionHologram(Generator generator) {
        if (!enabled) return;
        
        Location location = generator.getLocation();
        String hologramId = activeHolograms.get(location);
        if (hologramId == null) return;
        
        try {
            Optional<Hologram> optionalHologram = hologramManager.getHologram(hologramId);
            if (optionalHologram.isPresent()) {
                hologramManager.removeHologram(optionalHologram.get());
            }
            activeHolograms.remove(location);
        } catch (Exception e) {
            plugin.getLogger().warning("Gagal menghapus hologram: " + e.getMessage());
        }
    }
    
    public void removeAllHolograms() {
        if (!enabled) return;
        for (String hologramId : new ArrayList<>(activeHolograms.values())) {
            try {
                Optional<Hologram> optionalHologram = hologramManager.getHologram(hologramId);
                if (optionalHologram.isPresent()) {
                    hologramManager.removeHologram(optionalHologram.get());
                }
            } catch (Exception ignored) {}
        }
        activeHolograms.clear();
    }
    
    public boolean hasHologram(Location location) {
        return activeHolograms.containsKey(location);
    }
    
    public void reload() {
        removeAllHolograms();
        enabled = false;
        tryEnable();
    }
}