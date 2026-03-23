package com.yourserver.bentogens.integration;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.yourserver.bentogens.BentoGens;

/**
 * ShopGUIPlus integration
 * Allows selling non-generator items using ShopGUIPlus prices
 */
public class ShopGUIPlusIntegration {
    
    private final BentoGens plugin;
    private boolean enabled = false;
    private Object shopGuiPlusAPI = null;
    
    public ShopGUIPlusIntegration(BentoGens plugin) {
        this.plugin = plugin;
        tryEnable();
    }
    
    /**
     * Try to enable ShopGUIPlus integration
     */
    private void tryEnable() {
        Plugin shopGUIPlus = plugin.getServer().getPluginManager().getPlugin("ShopGUIPlus");
        
        if (shopGUIPlus == null) {
            plugin.getLogger().info("ShopGUIPlus not found - integration disabled");
            return;
        }
        
        try {
            // Try to get ShopGUIPlus API
            Class<?> apiClass = Class.forName("net.brcdev.shopgui.ShopGuiPlusApi");
            shopGuiPlusAPI = apiClass.getDeclaredMethod("getPlugin").invoke(null);
            
            enabled = true;
            plugin.getLogger().info("ShopGUIPlus integration enabled!");
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to enable ShopGUIPlus integration: " + e.getMessage());
            enabled = false;
        }
    }
    
    /**
     * Check if integration is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Get sell price for item from ShopGUIPlus
     */
    public double getSellPrice(Player player, ItemStack item) {
        if (!enabled) {
            return 0.0;
        }
        
        try {
            // Use ShopGUIPlus API to get sell price
            // This is a simplified example - actual implementation depends on ShopGUIPlus API version
            
            // ShopGUIPlus API example:
            // ShopGuiPlusApi api = ShopGuiPlusApi.getPlugin();
            // double price = api.getItemStackPriceSell(player, item);
            
            // For now, return 0 as placeholder
            // TODO: Implement actual ShopGUIPlus API calls when needed
            
            return 0.0;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting ShopGUIPlus price: " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Check if item is sellable in ShopGUIPlus
     */
    public boolean isSellable(Player player, ItemStack item) {
        if (!enabled) {
            return false;
        }
        
        double price = getSellPrice(player, item);
        return price > 0;
    }
    
    /**
     * Sell item using ShopGUIPlus
     */
    public boolean sellItem(Player player, ItemStack item) {
        if (!enabled) {
            return false;
        }
        
        try {
            double price = getSellPrice(player, item);
            
            if (price <= 0) {
                return false;
            }
            
            // Give money to player
            if (plugin.getEconomy() != null) {
                plugin.getEconomy().depositPlayer(player, price);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error selling item with ShopGUIPlus: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Reload integration
     */
    public void reload() {
        enabled = false;
        shopGuiPlusAPI = null;
        tryEnable();
    }

    public Object getShopGuiPlusAPI() {
        return shopGuiPlusAPI;
    }

    public void setShopGuiPlusAPI(Object shopGuiPlusAPI) {
        this.shopGuiPlusAPI = shopGuiPlusAPI;
    }
}