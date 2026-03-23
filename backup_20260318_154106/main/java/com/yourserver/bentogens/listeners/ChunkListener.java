package com.yourserver.bentogens.listeners;

import com.yourserver.bentogens.BentoGens;
import com.yourserver.bentogens.models.Generator;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class ChunkListener implements Listener {
    
    private final BentoGens plugin;
    
    public ChunkListener(BentoGens plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Restore generator blocks when chunk loads
     * This helps fix blocks that became vanilla
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!plugin.getConfig().getBoolean("generators.restore-on-chunk-load", true)) {
            return;
        }
        
        Chunk chunk = event.getChunk();
        
        // Check all generators in this chunk
        for (Generator gen : plugin.getGeneratorManager().getAllGenerators()) {
            Location loc = gen.getLocation();
            
            // Check if generator is in this chunk
            if (loc.getWorld().equals(chunk.getWorld()) &&
                loc.getBlockX() >> 4 == chunk.getX() &&
                loc.getBlockZ() >> 4 == chunk.getZ()) {
                
                // Restore block if wrong type
                restoreBlock(gen);
            }
        }
    }
    
    /**
     * Restore generator block to correct type
     */
    private void restoreBlock(Generator gen) {
        Location loc = gen.getLocation();
        Block block = loc.getBlock();
        
        String materialName = plugin.getConfigManager().getGeneratorMaterial(gen.getType());
        Material correctMaterial = Material.matchMaterial(materialName);
        
        if (correctMaterial == null) {
            return;
        }
        
        // Only restore if block is wrong type
        if (block.getType() != correctMaterial) {
            block.setType(correctMaterial);
            
            if (plugin.getConfig().getBoolean("debug.log-block-restore", false)) {
                plugin.getLogger().info("Restored generator block at " + 
                    loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
            }
        }
    }
}