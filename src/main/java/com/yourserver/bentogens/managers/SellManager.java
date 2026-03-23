package com.yourserver.bentogens.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.yourserver.bentogens.BentoGens;
import com.yourserver.bentogens.integration.ShopGUIPlusIntegration;

import net.milkbowl.vault.economy.Economy;

/**
 * Enhanced SellManager with ShopGUIPlus integration
 */
public class SellManager {
    
    private final BentoGens plugin;
    private final List<CustomItemData> customItems;
    private final ShopGUIPlusIntegration shopGUIPlus;
    private double eventMultiplier = 1.0;
    
    public SellManager(BentoGens plugin) {
        this.plugin = plugin;
        this.customItems = new ArrayList<>();
        this.shopGUIPlus = new ShopGUIPlusIntegration(plugin);
        loadItemValues();
    }
    
    /**
     * Custom item data for matching
     */
    private static class CustomItemData {
        Material material;
        String displayName;
        List<String> lore;
        double sellValue;
        String generatorType;
        
        CustomItemData(Material material, String displayName, List<String> lore, double sellValue, String genType) {
            this.material = material;
            this.displayName = displayName;
            this.lore = lore != null ? lore : new ArrayList<>();
            this.sellValue = sellValue;
            this.generatorType = genType;
        }
        
        boolean matches(ItemStack item) {
            if (item == null || item.getType() != material) {
                return false;
            }
            
            if (!item.hasItemMeta()) {
                return false;
            }
            
            ItemMeta meta = item.getItemMeta();
            
            // Check display name
            if (displayName != null && !displayName.isEmpty()) {
                if (!meta.hasDisplayName()) {
                    return false;
                }
                
                if (!meta.getDisplayName().equals(displayName)) {
                    return false;
                }
            }
            
            // Check lore
            if (!lore.isEmpty()) {
                if (!meta.hasLore()) {
                    return false;
                }
                
                List<String> itemLore = meta.getLore();
                for (String loreLine : lore) {
                    if (!itemLore.contains(loreLine)) {
                        return false;
                    }
                }
            }
            
            return true;
        }
    }
    
    /**
     * Load sell values from generators.yml
     */
    private void loadItemValues() {
        customItems.clear();
        
        ConfigurationSection gens = plugin.getConfigManager().getGeneratorsConfig().getRoot();
        
        for (String genType : gens.getKeys(false)) {
            ConfigurationSection drops = gens.getConfigurationSection(genType + ".drops");
            
            if (drops == null) continue;
            
            for (String dropId : drops.getKeys(false)) {
                ConfigurationSection drop = drops.getConfigurationSection(dropId);
                
                if (drop == null) continue;
                
                String materialName = drop.getString("item.material", "STONE");
                double sellValue = drop.getDouble("sell-value", 0.0);
                
                if (sellValue <= 0) continue;
                
                Material material = Material.matchMaterial(materialName);
                if (material == null) {
                    plugin.getLogger().warning("Invalid material: " + materialName);
                    continue;
                }
                
                String displayName = drop.getString("item.display-name", null);
                if (displayName != null) {
                    displayName = plugin.getConfigManager().colorize(displayName);
                }
                
                List<String> lore = new ArrayList<>();
                if (drop.contains("item.lore")) {
                    for (String line : drop.getStringList("item.lore")) {
                        lore.add(plugin.getConfigManager().colorize(line));
                    }
                }
                
                CustomItemData itemData = new CustomItemData(material, displayName, lore, sellValue, genType);
                customItems.add(itemData);
            }
        }
        
        plugin.getLogger().info("Loaded " + customItems.size() + " sellable generator items");
    }
    
    /**
     * Get sell value for an item
     */
    public double getSellValue(ItemStack item) {
        return getSellValue(null, item);
    }
    
    /**
     * Get sell value for an item (with ShopGUIPlus support)
     */
    public double getSellValue(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return 0.0;
        }
        
        // Check generator items first
        for (CustomItemData data : customItems) {
            if (data.matches(item)) {
                return data.sellValue * eventMultiplier;
            }
        }
        
        // Check ShopGUIPlus if enabled
        if (player != null && shopGUIPlus.isEnabled()) {
            double shopPrice = shopGUIPlus.getSellPrice(player, item);
            if (shopPrice > 0) {
                return shopPrice * eventMultiplier;
            }
        }
        
        return 0.0;
    }
    
    /**
     * Check if item is sellable
     */
    public boolean isSellable(ItemStack item) {
        return isSellable(null, item);
    }
    
    /**
     * Check if item is sellable (with ShopGUIPlus support)
     */
    public boolean isSellable(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        // Check generator items
        for (CustomItemData data : customItems) {
            if (data.matches(item)) {
                return true;
            }
        }
        
        // Check ShopGUIPlus
        if (player != null && shopGUIPlus.isEnabled()) {
            return shopGUIPlus.isSellable(player, item);
        }
        
        return false;
    }
    
    /**
     * Sell all sellable items in player inventory
     */
    public SellResult sellGeneratorItems(Player player) {
        Economy economy = plugin.getEconomy();
        
        if (economy == null) {
            return new SellResult(0, 0.0, "Economy not available!");
        }
        
        int totalItems = 0;
        double totalValue = 0.0;
        Map<String, Integer> itemCounts = new HashMap<>();
        
        ItemStack[] contents = player.getInventory().getContents();
        
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            
            // Check if sellable (generator or ShopGUIPlus)
            if (!isSellable(player, item)) {
                continue;
            }
            
            int amount = item.getAmount();
            double value = getSellValue(player, item) * amount;
            
            totalItems += amount;
            totalValue += value;
            
            String displayName = getItemDisplayName(item);
            itemCounts.put(displayName, itemCounts.getOrDefault(displayName, 0) + amount);
            
            player.getInventory().setItem(i, null);
        }
        
        if (totalValue > 0) {
            economy.depositPlayer(player, totalValue);
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
     * Set event multiplier
     */
    public void setEventMultiplier(double multiplier) {
        this.eventMultiplier = multiplier;
    }
    
    /**
     * Get current event multiplier
     */
    public double getEventMultiplier() {
        return eventMultiplier;
    }
    
    /**
     * Reset event multiplier
     */
    public void resetEventMultiplier() {
        this.eventMultiplier = 1.0;
    }
    
    /**
     * Get ShopGUIPlus integration
     */
    public ShopGUIPlusIntegration getShopGUIPlus() {
        return shopGUIPlus;
    }
    
    /**
     * Reload sell values
     */
    public void reload() {
        loadItemValues();
        shopGUIPlus.reload();
    }
    
    /**
     * Sell result class
     */
    public static class SellResult {
        public final int itemsSold;
        public final double totalValue;
        public final Map<String, Integer> itemCounts;
        public final String error;
        
        public SellResult(int items, double value, Map<String, Integer> counts) {
            this.itemsSold = items;
            this.totalValue = value;
            this.itemCounts = counts;
            this.error = null;
        }
        
        public SellResult(int items, double value, String error) {
            this.itemsSold = items;
            this.totalValue = value;
            this.itemCounts = new HashMap<>();
            this.error = error;
        }
        
        public boolean isSuccess() {
            return error == null && totalValue > 0;
        }
    }
}