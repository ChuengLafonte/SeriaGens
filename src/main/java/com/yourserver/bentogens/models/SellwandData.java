package com.yourserver.bentogens.models;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.yourserver.bentogens.BentoGens;

/**
 * Sellwand data model
 * Stores multiplier and uses in NBT data
 */
public class SellwandData {
    
    private static final String SELLWAND_KEY = "bentogens_sellwand";
    private static final String MULTIPLIER_KEY = "sellwand_multiplier";
    private static final String USES_KEY = "sellwand_uses";
    
    private final ItemStack item;
    private double multiplier;
    private int uses; // -1 = unlimited
    
    public SellwandData(ItemStack item, double multiplier, int uses) {
        this.item = item;
        this.multiplier = multiplier;
        this.uses = uses;
    }
    
    public ItemStack getItem() {
        return item;
    }
    
    public double getMultiplier() {
        return multiplier;
    }
    
    public int getUses() {
        return uses;
    }
    
    public boolean isUnlimited() {
        return uses == -1;
    }
    
    public void decreaseUses() {
        if (!isUnlimited()) {
            uses--;
        }
    }
    
    public boolean hasBroken() {
        return !isUnlimited() && uses <= 0;
    }
    
    /**
     * Create a new sellwand item
     */
    public static ItemStack createSellwand(BentoGens plugin, double multiplier, int uses) {
        ItemStack wand = new ItemStack(Material.STICK);
        ItemMeta meta = wand.getItemMeta();
        
        // Set name
        String usesDisplay = uses == -1 ? "Unlimited" : String.valueOf(uses);
        String displayName = plugin.getConfigManager().colorize(
            "&#84fab0&l⚡ &fSell Wand &7(" + usesDisplay + ")"
        );
        meta.setDisplayName(displayName);
        
        // Set lore
        List<String> lore = new ArrayList<>();
        lore.add(plugin.getConfigManager().colorize("&7"));
        lore.add(plugin.getConfigManager().colorize("&b・&7Right Click &f→ &cSell Container"));
        lore.add(plugin.getConfigManager().colorize("&7"));
        lore.add(plugin.getConfigManager().colorize("&c&nUsable on:"));
        lore.add(plugin.getConfigManager().colorize("&b・&fAll Chests"));
        lore.add(plugin.getConfigManager().colorize("&b・&fBarrels"));
        lore.add(plugin.getConfigManager().colorize("&b・&fHoppers"));
        lore.add(plugin.getConfigManager().colorize("&b・&fShulker Boxes"));
        lore.add(plugin.getConfigManager().colorize("&7"));
        lore.add(plugin.getConfigManager().colorize("&c&nInformation:"));
        lore.add(plugin.getConfigManager().colorize("&b・&cMultiplier: &f" + multiplier + "x"));
        lore.add(plugin.getConfigManager().colorize("&b・&cUses: &f" + usesDisplay));
        lore.add(plugin.getConfigManager().colorize("&7"));
        lore.add(plugin.getConfigManager().colorize("&#f69220&l✦ &fQuick Sell Tool"));
        
        meta.setLore(lore);
        
        // Add enchantment glint
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        // Store data in NBT
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey wandKey = new NamespacedKey(plugin, SELLWAND_KEY);
        NamespacedKey multKey = new NamespacedKey(plugin, MULTIPLIER_KEY);
        NamespacedKey usesKey = new NamespacedKey(plugin, USES_KEY);
        
        pdc.set(wandKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(multKey, PersistentDataType.DOUBLE, multiplier);
        pdc.set(usesKey, PersistentDataType.INTEGER, uses);
        
        wand.setItemMeta(meta);
        
        return wand;
    }
    
    /**
     * Check if item is a sellwand
     */
    public static boolean isSellwand(BentoGens plugin, ItemStack item) {
        if (item == null || item.getType() != Material.STICK) {
            return false;
        }
        
        if (!item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey wandKey = new NamespacedKey(plugin, SELLWAND_KEY);
        
        return pdc.has(wandKey, PersistentDataType.BYTE);
    }
    
    /**
     * Get sellwand data from item
     */
    public static SellwandData fromItem(BentoGens plugin, ItemStack item) {
        if (!isSellwand(plugin, item)) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        NamespacedKey multKey = new NamespacedKey(plugin, MULTIPLIER_KEY);
        NamespacedKey usesKey = new NamespacedKey(plugin, USES_KEY);
        
        double multiplier = pdc.getOrDefault(multKey, PersistentDataType.DOUBLE, 1.0);
        int uses = pdc.getOrDefault(usesKey, PersistentDataType.INTEGER, -1);
        
        return new SellwandData(item, multiplier, uses);
    }
    
    /**
     * Update item with current data
     */
    public void updateItem(BentoGens plugin) {
        if (!item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // Update display name with uses
        String usesDisplay = uses == -1 ? "Unlimited" : String.valueOf(uses);
        String displayName = plugin.getConfigManager().colorize(
            "&#84fab0&l⚡ &fSell Wand &7(" + usesDisplay + ")"
        );
        meta.setDisplayName(displayName);
        
        // Update lore
        List<String> lore = new ArrayList<>();
        lore.add(plugin.getConfigManager().colorize("&7"));
        lore.add(plugin.getConfigManager().colorize("&b・&7Right Click &f→ &cSell Container"));
        lore.add(plugin.getConfigManager().colorize("&7"));
        lore.add(plugin.getConfigManager().colorize("&c&nUsable on:"));
        lore.add(plugin.getConfigManager().colorize("&b・&fAll Chests"));
        lore.add(plugin.getConfigManager().colorize("&b・&fBarrels"));
        lore.add(plugin.getConfigManager().colorize("&b・&fHoppers"));
        lore.add(plugin.getConfigManager().colorize("&b・&fShulker Boxes"));
        lore.add(plugin.getConfigManager().colorize("&7"));
        lore.add(plugin.getConfigManager().colorize("&c&nInformation:"));
        lore.add(plugin.getConfigManager().colorize("&b・&cMultiplier: &f" + multiplier + "x"));
        lore.add(plugin.getConfigManager().colorize("&b・&cUses: &f" + usesDisplay));
        lore.add(plugin.getConfigManager().colorize("&7"));
        lore.add(plugin.getConfigManager().colorize("&#f69220&l✦ &fQuick Sell Tool"));
        
        meta.setLore(lore);
        
        // Update NBT
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey usesKey = new NamespacedKey(plugin, USES_KEY);
        pdc.set(usesKey, PersistentDataType.INTEGER, uses);
        
        item.setItemMeta(meta);
    }
}