package com.yourserver.bentogens.listeners;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.yourserver.bentogens.BentoGens;
import com.yourserver.bentogens.managers.SellManager;
import com.yourserver.bentogens.models.SellwandData;

/**
 * Enhanced sellwand listener
 * - Multiplier support
 * - Uses system
 * - All container types
 * - ShopGUIPlus integration
 */
public class SellWandListener implements Listener {
    
    private final BentoGens plugin;
    private final DecimalFormat formatter = new DecimalFormat("#,###.##");
    
    public SellWandListener(BentoGens plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle sellwand usage
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onSellwandUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        // Check if right-click block
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        // Check if holding sellwand
        if (!SellwandData.isSellwand(plugin, item)) {
            return;
        }
        
        // Get sellwand data
        SellwandData wandData = SellwandData.fromItem(plugin, item);
        if (wandData == null) {
            return;
        }
        
        // Check if clicked block is a container
        Block block = event.getClickedBlock();
        if (block == null || !isContainer(block.getType())) {
            return;
        }
        
        // Cancel event
        event.setCancelled(true);
        
        // Check permission
        if (!player.hasPermission("bentogens.wand")) {
            player.sendMessage(plugin.getConfigManager().colorize(
                "&c&l⚠ &fYou don't have permission to use sell wand!"
            ));
            return;
        }
        
        // Check if wand is broken
        if (wandData.hasBroken()) {
            player.sendMessage(plugin.getConfigManager().colorize(
                "&c&l⚠ &fYour sellwand has broken!"
            ));
            player.getInventory().setItemInMainHand(null);
            return;
        }
        
        // Get container inventory
        Container container = (Container) block.getState();
        Inventory containerInv = container.getInventory();
        
        // Sell all items in container
        SellManager sellManager = plugin.getSellManager();
        
        if (sellManager == null) {
            player.sendMessage(plugin.getConfigManager().colorize("&cSell system not available!"));
            return;
        }
        
        // Save original multiplier
        double originalMultiplier = sellManager.getEventMultiplier();
        
        // Set wand multiplier
        sellManager.setEventMultiplier(wandData.getMultiplier());
        
        // Sell items
        SellResult result = sellItemsFromInventory(player, containerInv, sellManager);
        
        // Restore original multiplier
        sellManager.setEventMultiplier(originalMultiplier);
        
        if (result.error != null) {
            player.sendMessage(plugin.getConfigManager().colorize("&c" + result.error));
            return;
        }
        
        if (!result.isSuccess()) {
            player.sendMessage(plugin.getConfigManager().colorize("&cNo sellable items in container!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Decrease uses
        wandData.decreaseUses();
        
        // Check if wand broke
        if (wandData.hasBroken()) {
            player.sendMessage(plugin.getConfigManager().colorize(
                "&#84fab0&l⚡ SOLD &fx" + result.itemsSold + " items &ffor &a●" + 
                formatter.format(result.totalValue) + " &7(" + wandData.getMultiplier() + "x)"
            ));
            player.sendMessage(plugin.getConfigManager().colorize(
                "&c&l⚠ &fYour sellwand has broken!"
            ));
            player.getInventory().setItemInMainHand(null);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        } else {
            // Update wand
            wandData.updateItem(plugin);
            
            // Send success message
            player.sendMessage(plugin.getConfigManager().colorize(
                "&#84fab0&l⚡ SOLD &fx" + result.itemsSold + " items &ffor &a●" + 
                formatter.format(result.totalValue) + " &7(" + wandData.getMultiplier() + "x)"
            ));
            
            if (!wandData.isUnlimited()) {
                player.sendMessage(plugin.getConfigManager().colorize(
                    "  &7Uses remaining: &f" + wandData.getUses()
                ));
            }
        }
        
        // Play success sound
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
    }
    
    /**
     * Check if material is a container
     */
    private boolean isContainer(Material material) {
        switch (material) {
            case CHEST:
            case TRAPPED_CHEST:
            case BARREL:
            case HOPPER:
            case SHULKER_BOX:
            case WHITE_SHULKER_BOX:
            case ORANGE_SHULKER_BOX:
            case MAGENTA_SHULKER_BOX:
            case LIGHT_BLUE_SHULKER_BOX:
            case YELLOW_SHULKER_BOX:
            case LIME_SHULKER_BOX:
            case PINK_SHULKER_BOX:
            case GRAY_SHULKER_BOX:
            case LIGHT_GRAY_SHULKER_BOX:
            case CYAN_SHULKER_BOX:
            case PURPLE_SHULKER_BOX:
            case BLUE_SHULKER_BOX:
            case BROWN_SHULKER_BOX:
            case GREEN_SHULKER_BOX:
            case RED_SHULKER_BOX:
            case BLACK_SHULKER_BOX:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Sell all sellable items from inventory
     */
    private SellResult sellItemsFromInventory(Player player, Inventory inventory, SellManager sellManager) {
        int totalItems = 0;
        double totalValue = 0.0;
        Map<String, Integer> itemCounts = new HashMap<>();
        
        ItemStack[] contents = inventory.getContents();
        
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            
            // Check if sellable (generator item or ShopGUIPlus item)
            if (!sellManager.isSellable(item)) {
                // TODO: Check ShopGUIPlus price here
                continue;
            }
            
            int amount = item.getAmount();
            double value = sellManager.getSellValue(item) * amount;
            
            totalItems += amount;
            totalValue += value;
            
            // Track for message
            String displayName = getItemDisplayName(item);
            itemCounts.put(displayName, itemCounts.getOrDefault(displayName, 0) + amount);
            
            // Remove item from container
            inventory.setItem(i, null);
        }
        
        if (totalValue > 0 && plugin.getEconomy() != null) {
            plugin.getEconomy().depositPlayer(player, totalValue);
        }
        
        return new SellResult(totalItems, totalValue, itemCounts);
    }
    
    /**
     * Get item display name
     */
    private String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name();
    }
    
    /**
     * Sell result class
     */
    private static class SellResult {
        final int itemsSold;
        final double totalValue;
        final Map<String, Integer> itemCounts;
        final String error;
        
        SellResult(int items, double value, Map<String, Integer> counts) {
            this.itemsSold = items;
            this.totalValue = value;
            this.itemCounts = counts;
            this.error = null;
        }
        
        SellResult(int items, double value, String error) {
            this.itemsSold = items;
            this.totalValue = value;
            this.itemCounts = new HashMap<>();
            this.error = error;
        }
        
        boolean isSuccess() {
            return error == null && totalValue > 0;
        }
    }
}