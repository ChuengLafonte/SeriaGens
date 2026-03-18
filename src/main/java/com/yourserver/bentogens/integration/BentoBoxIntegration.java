package com.yourserver.bentogens.integration;

import com.yourserver.bentogens.BentoGens;
import com.yourserver.bentogens.models.Generator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * BentoBox integration using reflection
 * COMPLETELY REWRITTEN - No generic Event usage!
 */
public class BentoBoxIntegration implements Listener {
    
    private final BentoGens plugin;
    private final Object bentoBox;
    
    public BentoBoxIntegration(BentoGens plugin, Object bentoBox) throws Exception {
        this.plugin = plugin;
        this.bentoBox = bentoBox;
        
        plugin.getLogger().info("BentoBox integration initialized (reflection mode)");
    }
    
    /**
     * NOTE: We CANNOT listen to BentoBox events directly via @EventHandler
     * because they require class loading at compile time.
     * 
     * Instead, we provide static utility methods for checking island ownership
     * that are called from other listeners (like GeneratorListener).
     */
    
    /**
     * Check if player is on their own island
     * Static method - can be called from anywhere
     */
    public static boolean isOnOwnIsland(Player player, Location location, Object bentoBox) {
        if (bentoBox == null) {
            return true; // No BentoBox, allow everywhere
        }
        
        try {
            // Get islands manager
            Method getIslandsMethod = bentoBox.getClass().getMethod("getIslands");
            Object islands = getIslandsMethod.invoke(bentoBox);
            
            // Get island at location
            Method getIslandAtMethod = islands.getClass().getMethod("getIslandAt", Location.class);
            Object optional = getIslandAtMethod.invoke(islands, location);
            
            // Check if island exists
            Method isPresentMethod = java.util.Optional.class.getMethod("isPresent");
            boolean isPresent = (boolean) isPresentMethod.invoke(optional);
            
            if (!isPresent) {
                return false; // No island at location
            }
            
            // Get island object
            Method getMethod = java.util.Optional.class.getMethod("get");
            Object island = getMethod.invoke(optional);
            
            // Get island owner
            Method getOwnerMethod = island.getClass().getMethod("getOwner");
            UUID owner = (UUID) getOwnerMethod.invoke(island);
            
            // Check if player is owner
            if (owner != null && owner.equals(player.getUniqueId())) {
                return true;
            }
            
            // Check if player is member
            Method getMemberSetMethod = island.getClass().getMethod("getMemberSet");
            Object memberSet = getMemberSetMethod.invoke(island);
            
            Method containsMethod = memberSet.getClass().getMethod("contains", Object.class);
            return (boolean) containsMethod.invoke(memberSet, player.getUniqueId());
            
        } catch (Exception e) {
            return true; // Allow if error
        }
    }
    
    /**
     * Handle island deletion via API polling
     * Called periodically from main plugin
     */
    public void handleIslandDeletion(UUID islandOwner) {
        try {
            // Get all generators owned by this player
            List<Generator> toRemove = new ArrayList<>();
            
            for (Generator gen : plugin.getGeneratorManager().getAllGenerators()) {
                if (gen.getOwner().equals(islandOwner)) {
                    toRemove.add(gen);
                }
            }
            
            // Remove all generators
            for (Generator gen : toRemove) {
                plugin.getGeneratorManager().removeGenerator(gen.getLocation(), null);
            }
            
            if (!toRemove.isEmpty()) {
                plugin.getLogger().info("Removed " + toRemove.size() + 
                    " generators from deleted island (owner: " + islandOwner + ")");
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling island deletion: " + e.getMessage());
        }
    }
    
    /**
     * Handle player leaving island
     * Return generators to player
     */
    public void handlePlayerLeave(UUID playerUUID) {
        try {
            // Get player's generators
            List<Generator> playerGens = new ArrayList<>();
            
            for (Generator gen : plugin.getGeneratorManager().getAllGenerators()) {
                if (gen.getOwner().equals(playerUUID)) {
                    playerGens.add(gen);
                }
            }
            
            if (playerGens.isEmpty()) {
                return;
            }
            
            // Return generators to player
            Player player = Bukkit.getPlayer(playerUUID);
            
            if (player != null && player.isOnline()) {
                int returned = 0;
                
                for (Generator gen : playerGens) {
                    // Give generator item
                    ItemStack item = plugin.getGeneratorManager().getGeneratorItem(gen.getType());
                    
                    // Add to inventory or drop
                    if (player.getInventory().firstEmpty() != -1) {
                        player.getInventory().addItem(item);
                    } else {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                    
                    // Remove from world
                    plugin.getGeneratorManager().removeGenerator(gen.getLocation(), null);
                    returned++;
                }
                
                // Notify player
                String msg = plugin.getConfigManager().getMessage("generators-returned")
                    .replace("{amount}", String.valueOf(returned));
                player.sendMessage(msg);
                
                plugin.getLogger().info("Returned " + returned + " generators to " + 
                    player.getName() + " (left island)");
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling player leave: " + e.getMessage());
        }
    }
}