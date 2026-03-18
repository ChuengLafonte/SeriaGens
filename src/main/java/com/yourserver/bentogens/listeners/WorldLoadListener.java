package com.yourserver.bentogens.listeners;

import com.yourserver.bentogens.BentoGens;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

/**
 * Listen for world load events
 * CRITICAL: Restore generators when worlds load (fixes BentoBox delayed world loading!)
 */
public class WorldLoadListener implements Listener {
    
    private final BentoGens plugin;
    
    public WorldLoadListener(BentoGens plugin) {
        this.plugin = plugin;
    }
    
    /**
     * When world loads, restore all generators in that world
     * This fixes the issue where BentoBox worlds load AFTER plugin enable
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        
        plugin.getLogger().info("🌍 World loaded: " + world.getName());
        
        // Restore generators in this world
        plugin.getGeneratorManager().onWorldLoad(world);
        
        // Also run immediate restoration check
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            int restored = plugin.getGeneratorManager().restoreAllBlocks();
            if (restored > 0) {
                plugin.getLogger().info("✅ Restored " + restored + " generator blocks in world: " + world.getName());
            }
        }, 20L); // 1 second after world load
    }
}