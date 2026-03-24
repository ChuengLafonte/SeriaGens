package id.seria.gens.integration;

import id.seria.gens.SeriaGens;
import id.seria.gens.models.Generator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.island.IslandDeleteEvent;
import world.bentobox.bentobox.api.events.island.IslandResetEvent;
import world.bentobox.bentobox.database.objects.Island;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BentoBoxIntegration implements Listener {
    
    private final SeriaGens plugin;
    
    // Kita mempertahankan parameter 'dummyObject' agar tidak error dengan SeriaGens.java lamamu
    public BentoBoxIntegration(SeriaGens plugin, Object dummyObject) {
        this.plugin = plugin;
        
        if (Bukkit.getPluginManager().getPlugin("BentoBox") != null) {
            // Mendaftarkan class ini sebagai pendengar Event (Sensor) ke server
            Bukkit.getPluginManager().registerEvents(this, plugin);
            plugin.getLogger().info("✅ BentoBox integration enabled! (Native API & Auto-Pickup Active)");
        }
    }
    
    // Fitur Pengecekan Kepemilikan Pulau
    public static boolean isOnOwnIsland(Player player, Location location, Object dummyObject) {
        if (Bukkit.getPluginManager().getPlugin("BentoBox") == null) return true;
        
        Island island = BentoBox.getInstance().getIslands().getIslandAt(location).orElse(null);
        if (island == null) return false;
        
        return island.getMemberSet().contains(player.getUniqueId());
    }
    
    // ==========================================
    // SENSOR EVENT: SAAT PEMAIN RESET PULAU/CAVE
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR)
    public void onIslandReset(IslandResetEvent event) {
        handleIslandClear(event.getPlayerUUID());
    }
    
    // ==========================================
    // SENSOR EVENT: SAAT PULAU/CAVE DIHAPUS
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR)
    public void onIslandDelete(IslandDeleteEvent event) {
        handleIslandClear(event.getPlayerUUID());
    }
    
    // Logika Auto-Pickup Penyelamatan Aset
    private void handleIslandClear(UUID ownerUUID) {
        Player player = Bukkit.getPlayer(ownerUUID);
        
        // 1. Cari semua generator milik pemain yang melakukan reset
        List<Generator> toRemove = new ArrayList<>();
        for (Generator gen : plugin.getGeneratorManager().getAllGenerators()) {
            if (gen.getOwner().equals(ownerUUID)) {
                toRemove.add(gen);
            }
        }
        
        if (toRemove.isEmpty()) return; // Jika tidak punya generator, lewati
        
        // 2. Cabut semua generator tersebut dan kembalikan itemnya
        int returnedCount = 0;
        for (Generator gen : toRemove) {
            if (player != null && player.isOnline()) {
                ItemStack item = plugin.getGeneratorManager().getGeneratorItem(gen.getType());
                
                // Cek apakah inventory penuh
                if (player.getInventory().firstEmpty() != -1) {
                    player.getInventory().addItem(item);
                } else {
                    // Jika inventory penuh, jatuhkan di kaki pemain
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }
            
            // Hapus data generator dari memory dan database (PENTING agar tidak jadi hantu)
            plugin.getGeneratorManager().removeGenerator(gen.getLocation(), null);
            returnedCount++;
        }
        
        // 3. Kirim notifikasi sukses ke pemain
        if (player != null && player.isOnline()) {
            player.sendMessage(plugin.getConfigManager().colorize("&a&l[!] &aSistem mendeteksi Reset Cave!"));
            player.sendMessage(plugin.getConfigManager().colorize("&e" + returnedCount + " &agenerator telah otomatis diselamatkan ke inventory-mu."));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
        
        plugin.getLogger().info("✅ BentoBox Reset: Berhasil menyelamatkan " + returnedCount + " generator milik UUID: " + ownerUUID);
    }
}