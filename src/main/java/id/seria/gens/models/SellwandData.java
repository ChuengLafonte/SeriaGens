package id.seria.gens.models;

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

import id.seria.gens.SeriaGens;

public class SellwandData {
    
    private static final String SELLWAND_KEY = "seriagens_sellwand";
    private static final String MULTIPLIER_KEY = "sellwand_multiplier";
    private static final String USES_KEY = "sellwand_uses";
    
    private final ItemStack item;
    private double multiplier;
    private int uses;
    
    public SellwandData(ItemStack item, double multiplier, int uses) {
        this.item = item;
        this.multiplier = multiplier;
        this.uses = uses;
    }
    
    public ItemStack getItem() { return item; }
    public double getMultiplier() { return multiplier; }
    public int getUses() { return uses; }
    public boolean isUnlimited() { return uses == -1; }
    public boolean hasBroken() { return uses == 0; }
    
    public void decreaseUses() {
        if (!isUnlimited() && uses > 0) {
            uses--;
        }
    }
    
    public void updateItem(SeriaGens plugin) {
        if (item == null || item.getType() == Material.AIR) return;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        String usesDisplay = isUnlimited() ? "&aUnlimited" : "&e" + uses;
        String displayName = plugin.getConfigManager().colorize("&#f69220&l⚡ SELL WAND &7(" + multiplier + "x)");
        meta.setDisplayName(displayName);
        
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
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey wandKey = new NamespacedKey(plugin, SELLWAND_KEY);
        NamespacedKey multKey = new NamespacedKey(plugin, MULTIPLIER_KEY);
        NamespacedKey usesKey = new NamespacedKey(plugin, USES_KEY);
        
        pdc.set(wandKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(multKey, PersistentDataType.DOUBLE, multiplier);
        pdc.set(usesKey, PersistentDataType.INTEGER, uses);
        
        item.setItemMeta(meta);
    }
    
    public static ItemStack createSellwand(SeriaGens plugin, double multiplier, int uses) {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        
        SellwandData data = new SellwandData(item, multiplier, uses);
        data.updateItem(plugin);
        
        return item;
    }
    
    public static boolean isSellwand(SeriaGens plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, SELLWAND_KEY);
        
        return pdc.has(key, PersistentDataType.BYTE);
    }
    
    public static SellwandData fromItem(SeriaGens plugin, ItemStack item) {
        if (!isSellwand(plugin, item)) return null;
        
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey multKey = new NamespacedKey(plugin, MULTIPLIER_KEY);
        NamespacedKey usesKey = new NamespacedKey(plugin, USES_KEY);
        
        double multiplier = pdc.getOrDefault(multKey, PersistentDataType.DOUBLE, 1.0);
        int uses = pdc.getOrDefault(usesKey, PersistentDataType.INTEGER, -1);
        
        return new SellwandData(item, multiplier, uses);
    }
}