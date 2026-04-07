package id.seria.gens.models;


import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import java.util.stream.Collectors;

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
        if (!isUnlimited() && uses > 0) uses--;
    }
    
    public void updateItem(SeriaGens plugin) {
        if (item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        String unlimText = plugin.getConfig().getString("sellwand-item.unlimited-text", "&aUnlimited");
        String usesDisplay = isUnlimited() ? unlimText : "&e" + uses;
        
        String nameFormat = plugin.getConfig().getString("sellwand-item.name", "Sell Wand");
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize(plugin.getConfigManager().colorize(nameFormat.replace("{multiplier}", String.valueOf(multiplier)))));
        
        List<String> loreFormat = plugin.getConfig().getStringList("sellwand-item.lore");
        meta.lore(loreFormat.stream()
            .map(line -> LegacyComponentSerializer.legacySection().deserialize(plugin.getConfigManager().colorize(line
                .replace("{multiplier}", String.valueOf(multiplier))
                .replace("{uses}", usesDisplay))))
            .collect(Collectors.toList()));
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey(plugin, SELLWAND_KEY), PersistentDataType.BYTE, (byte) 1);
        pdc.set(new NamespacedKey(plugin, MULTIPLIER_KEY), PersistentDataType.DOUBLE, multiplier);
        pdc.set(new NamespacedKey(plugin, USES_KEY), PersistentDataType.INTEGER, uses);
        item.setItemMeta(meta);
    }
    
    public static ItemStack createSellwand(SeriaGens plugin, double multiplier, int uses) {
        String matName = plugin.getConfig().getString("sellwand-item.material", "BLAZE_ROD");
        Material mat = Material.matchMaterial(matName);
        ItemStack item = new ItemStack(mat != null ? mat : Material.BLAZE_ROD);
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
        return item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, SELLWAND_KEY), PersistentDataType.BYTE);
    }
    
    public static SellwandData fromItem(SeriaGens plugin, ItemStack item) {
        if (!isSellwand(plugin, item)) return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        double multiplier = pdc.getOrDefault(new NamespacedKey(plugin, MULTIPLIER_KEY), PersistentDataType.DOUBLE, 1.0);
        int uses = pdc.getOrDefault(new NamespacedKey(plugin, USES_KEY), PersistentDataType.INTEGER, -1);
        return new SellwandData(item, multiplier, uses);
    }
}