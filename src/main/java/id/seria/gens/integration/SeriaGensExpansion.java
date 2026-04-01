package id.seria.gens.integration;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import id.seria.gens.SeriaGens;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class SeriaGensExpansion extends PlaceholderExpansion {
    
    private final SeriaGens plugin;
    
    public SeriaGensExpansion(SeriaGens plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean persist() { return true; }
    
    @Override
    public boolean canRegister() { return true; }
    
    @Override
    public String getAuthor() { return "SeriaTeam"; }
    
    @Override
    public String getIdentifier() { return "seriagens"; }
    
    @Override
    public String getVersion() { return plugin.getDescription().getVersion(); }
    
    @Override
    public String onRequest(OfflinePlayer player, String params) {
        // Placeholder Global (Event)
        if (params.equalsIgnoreCase("event_active")) {
            return plugin.getEventManager().hasActiveEvent() ? "Aktif" : "Tidak Ada";
        }
        
        if (params.equalsIgnoreCase("event_name")) {
            return plugin.getEventManager().hasActiveEvent() ? 
                plugin.getEventManager().getActiveEvent().getDisplayName() : "Tidak Ada Event";
        }
        
        // Placeholder Spesifik Pemain
        if (player != null && player.isOnline()) {
            Player p = player.getPlayer();
            
            if (params.equalsIgnoreCase("count")) {
                return String.valueOf(plugin.getGeneratorManager().getPlayerGeneratorCount(p.getUniqueId()));
            }
            
            if (params.equalsIgnoreCase("max")) {
                return String.valueOf(plugin.getGeneratorManager().getMaxGenerators(p));
            }
            
            if (params.equalsIgnoreCase("corrupted")) {
                return String.valueOf(plugin.getCorruptionManager().getCorruptedCount(p));
            }

            if (params.startsWith("joules_")) {
                String genType = params.substring("joules_".length());
                return String.valueOf(plugin.getGeneratorManager().getJoules(p.getUniqueId(), genType));
            }

            if (params.equalsIgnoreCase("total_joules")) {
                // Tampilkan TENAGA JOULE Garden Induk Global Owner
                int globalJoules = plugin.getFuelManager().getGlobalGrid(player.getUniqueId()).getCurrentJoules();
                return String.valueOf(globalJoules);
            }
        }
        
        return null; // Mengembalikan null jika placeholder tidak dikenali
    }
}