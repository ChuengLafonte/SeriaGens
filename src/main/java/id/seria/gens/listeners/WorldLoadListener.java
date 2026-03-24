package id.seria.gens.listeners;

import id.seria.gens.SeriaGens;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public class WorldLoadListener implements Listener {
    
    private final SeriaGens plugin;
    
    public WorldLoadListener(SeriaGens plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        
        plugin.getLogger().info("🌍 World loaded: " + world.getName());
        
        plugin.getGeneratorManager().onWorldLoad(world);
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            int restored = plugin.getGeneratorManager().restoreAllBlocks();
            if (restored > 0) {
                plugin.getLogger().info("✅ Restored " + restored + " generator blocks in world: " + world.getName());
            }
        }, 100L);
    }
}