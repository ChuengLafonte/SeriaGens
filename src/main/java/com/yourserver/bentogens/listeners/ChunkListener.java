package com.yourserver.bentogens.listeners;

import com.yourserver.bentogens.BentoGens;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * Listen for chunk load/unload events
 * CRITICAL: Restore generator blocks when chunks load!
 */
public class ChunkListener implements Listener {
    
    private final BentoGens plugin;
    
    public ChunkListener(BentoGens plugin) {
        this.plugin = plugin;
    }
    
    /**
     * When chunk loads, restore any generator blocks in it
     * This is the FINAL safety net - blocks MUST be correct when chunk loads!
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        
        // Restore generators in this chunk
        int restored = plugin.getGeneratorManager().restoreChunkGenerators(chunk);
        
        if (restored > 0) {
            plugin.getLogger().info("✅ Chunk " + chunk.getX() + "," + chunk.getZ() + 
                " in " + chunk.getWorld().getName() + ": Restored " + restored + " generator blocks");
        }
    }
    
    /**
     * Optional: Save generators when chunk unloads
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        // Could save generators here if needed
        // For now, auto-save task handles this
    }
}