package id.seria.gens.listeners;

import id.seria.gens.SeriaGens;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class ChunkListener implements Listener {
    
    private final SeriaGens plugin;
    
    public ChunkListener(SeriaGens plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        
        int restored = plugin.getGeneratorManager().restoreChunkGenerators(chunk);
        
        if (restored > 0) {
            plugin.getLogger().info("✅ Chunk " + chunk.getX() + "," + chunk.getZ() + 
                " in " + chunk.getWorld().getName() + ": Restored " + restored + " generator blocks");
        }
    }
}