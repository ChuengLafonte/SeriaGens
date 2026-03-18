package com.yourserver.bentogens.listeners;

import com.yourserver.bentogens.BentoGens;
import com.yourserver.bentogens.gui.UpgradeGUI;
import com.yourserver.bentogens.models.Generator;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.UUID;

public class GeneratorListener implements Listener {
    
    private final BentoGens plugin;
    
    public GeneratorListener(BentoGens plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle generator placement
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGeneratorPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack item = event.getItemInHand();
        
        // Check if item is a generator
        if (!isGeneratorItem(item)) {
            return;
        }
        
        // Get generator type from item
        String type = getGeneratorType(item);
        if (type == null) {
            return;
        }
        
        Location location = block.getLocation();
        
        // Check if BentoBox integration is enabled
        if (plugin.getConfig().getBoolean("bentobox.enabled", true)) {
            // Check if player is on their island
            if (!isOnOwnIsland(player, location)) {
                event.setCancelled(true);
                String msg = plugin.getConfigManager().getMessage("not-your-island");
                player.sendMessage(msg);
                return;
            }
        }
        
        // Place generator
        boolean success = plugin.getGeneratorManager().placeGenerator(player, location, type);
        
        if (!success) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle generator break
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGeneratorBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();
        
        // Check if block is a generator
        Generator gen = plugin.getGeneratorManager().getGenerator(location);
        if (gen == null) {
            return;
        }
        
        // Check ownership or admin permission
        if (!gen.getOwner().equals(player.getUniqueId()) 
                && !player.hasPermission("bentogens.admin")) {
            event.setCancelled(true);
            String msg = plugin.getConfigManager().getMessage("not-owner");
            if (msg != null) {
                player.sendMessage(msg);
            }
            return;
        }
        
        // Remove generator
        plugin.getGeneratorManager().removeGenerator(location, player);
        
        // Drop generator item (not the block)
        event.setDropItems(false);
        
        // Give generator item back to player (only if not in creative)
        if (player.getGameMode() != GameMode.CREATIVE) {
            ItemStack generatorItem = plugin.getGeneratorManager().getGeneratorItem(gen.getType());
            
            // Try to add to inventory
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(generatorItem);
            } else {
                // Drop if inventory full
                player.getWorld().dropItemNaturally(player.getLocation(), generatorItem);
            }
        }
    }
    
    /**
     * Handle right-click on generator to open GUI
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onGeneratorRightClick(PlayerInteractEvent event) {
        // Only handle right-click on block
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        
        Location location = block.getLocation();
        Generator gen = plugin.getGeneratorManager().getGenerator(location);
        
        // Not a generator
        if (gen == null) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check ownership or admin permission
        if (!gen.getOwner().equals(player.getUniqueId()) 
                && !player.hasPermission("bentogens.admin")) {
            return;
        }
        
        // Cancel event to prevent block interaction
        event.setCancelled(true);
        
        // Open upgrade/repair GUI
        new UpgradeGUI(plugin, player, gen).open();
    }
    
    /**
     * Check if item is a generator
     */
    private boolean isGeneratorItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName() || !meta.hasLore()) {
            return false;
        }
        
        String displayName = meta.getDisplayName();
        
        // Check if display name matches any generator
        for (String type : plugin.getConfigManager().getAllGeneratorTypes()) {
            String genName = plugin.getConfigManager()
                .getGeneratorsConfig()
                .getString(type + ".item.display-name");
            
            if (genName != null && displayName.equals(plugin.getConfigManager().colorize(genName))) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get generator type from item
     */
    private String getGeneratorType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        String displayName = meta.getDisplayName();
        
        // Match display name to generator type
        for (String type : plugin.getConfigManager().getAllGeneratorTypes()) {
            String genName = plugin.getConfigManager()
                .getGeneratorsConfig()
                .getString(type + ".item.display-name");
            
            if (genName != null && displayName.equals(plugin.getConfigManager().colorize(genName))) {
                return type;
            }
        }
        
        return null;
    }
    
    /**
     * Check if player is on their own island (BentoBox integration)
     */
    private boolean isOnOwnIsland(Player player, Location location) {
        // If BentoBox integration is disabled, allow placement
        if (!plugin.getConfig().getBoolean("bentobox.enabled", true)) {
            return true;
        }
        
        // If BentoBox is not available, allow placement
        if (plugin.getBentoBox() == null) {
            return true;
        }
        
        try {
            // Use reflection to check island
            Object bentoBox = plugin.getBentoBox();
            
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
                return false;
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
            plugin.getLogger().warning("Error checking island ownership: " + e.getMessage());
            return true; // Allow if error
        }
    }
}