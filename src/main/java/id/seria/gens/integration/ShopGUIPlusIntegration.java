package id.seria.gens.integration;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import id.seria.gens.SeriaGens;

public class ShopGUIPlusIntegration {
    
    private final SeriaGens plugin;
    private boolean enabled = false;
    private Object shopGuiPlusAPI = null;
    
    public ShopGUIPlusIntegration(SeriaGens plugin) {
        this.plugin = plugin;
        tryEnable();
    }
    
    private void tryEnable() {
        Plugin shopGUIPlus = plugin.getServer().getPluginManager().getPlugin("ShopGUIPlus");
        
        if (shopGUIPlus == null) {
            plugin.getLogger().info("ShopGUIPlus not found - integration disabled");
            return;
        }
        
        try {
            Class<?> apiClass = Class.forName("net.brcdev.shopgui.ShopGuiPlusApi");
            shopGuiPlusAPI = apiClass.getDeclaredMethod("getPlugin").invoke(null);
            
            enabled = true;
            plugin.getLogger().info("ShopGUIPlus integration enabled!");
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to hook into ShopGUIPlus: " + e.getMessage());
            enabled = false;
        }
    }
    
    public double getSellPrice(Player player, ItemStack item) {
        if (!enabled || item == null) return 0.0;
        
        try {
            Class<?> apiClass = Class.forName("net.brcdev.shopgui.ShopGuiPlusApi");
            Object price = apiClass.getDeclaredMethod("getItemStackPriceSell", Player.class, ItemStack.class)
                                   .invoke(null, player, item);
            
            if (price != null && (double) price > 0) {
                // ShopGUIPlus returns the price for the entire ItemStack (including quantity).
                // We divide by item amount to get the price per single item.
                return (double) price / item.getAmount();
            }
            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    public boolean isSellable(Player player, ItemStack item) {
        if (!enabled) return false;
        double price = getSellPrice(player, item);
        return price > 0;
    }
    
    public boolean sellItem(Player player, ItemStack item) {
        if (!enabled) return false;
        
        try {
            double price = getSellPrice(player, item);
            if (price <= 0) return false;
            
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
    
    public void reload() {
        enabled = false;
        shopGuiPlusAPI = null;
        tryEnable();
    }

    public Object getShopGuiPlusAPI() {
        return shopGuiPlusAPI;
    }
}