package id.seria.gens.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import java.util.Objects;
import java.util.stream.Collectors;

import id.seria.gens.SeriaGens;
import id.seria.gens.integration.ShopGUIPlusIntegration;

public class SellManager {
    
    private final SeriaGens plugin;
    private final List<CustomItemData> customItems;
    private final ShopGUIPlusIntegration shopGUIPlus;
    private double eventMultiplier = 1.0;
    
    public SellManager(SeriaGens plugin) {
        this.plugin = plugin;
        this.customItems = new ArrayList<>();
        this.shopGUIPlus = new ShopGUIPlusIntegration(plugin);
        loadItemValues();
    }
    
    private static class CustomItemData {
        Material material;
        String displayName;
        List<String> lore;
        double sellValue;
        
        CustomItemData(Material material, String displayName, List<String> lore, double sellValue) {
            this.material = material;
            this.displayName = displayName;
            this.lore = lore;
            this.sellValue = sellValue;
        }
        
        boolean matches(ItemStack item) {
            if (item.getType() != material) return false;
            
            if (displayName != null || lore != null) {
                if (!item.hasItemMeta()) return false;
                ItemMeta meta = item.getItemMeta();
                
                if (displayName != null) {
                    if (!meta.hasDisplayName() || !Objects.equals(meta.displayName(), LegacyComponentSerializer.legacySection().deserialize(displayName))) return false;
                }
                
                if (lore != null && !lore.isEmpty()) {
                    if (!meta.hasLore()) return false;
                    List<String> itemLore = meta.lore().stream()
                        .map(comp -> LegacyComponentSerializer.legacySection().serialize(comp))
                        .collect(Collectors.toList());
                    for (String line : lore) {
                        if (!itemLore.contains(line)) return false;
                    }
                }
            }
            return true;
        }
    }
    
    public void loadItemValues() {
        customItems.clear();
        ConfigurationSection generators = plugin.getConfigManager().getGeneratorsConfig();
        
        for (String genType : generators.getKeys(false)) {
            ConfigurationSection drops = generators.getConfigurationSection(genType + ".drops");
            if (drops == null) continue;
            
            for (String dropId : drops.getKeys(false)) {
                ConfigurationSection itemConfig = drops.getConfigurationSection(dropId + ".item");
                double sellValue = drops.getDouble(dropId + ".sell-value", 0.0);
                
                if (itemConfig == null || sellValue <= 0) continue;
                
                String materialName = itemConfig.getString("material", "STONE");
                Material material = Material.matchMaterial(materialName);
                if (material == null) continue;
                
                String displayName = null;
                if (itemConfig.contains("display-name")) {
                    displayName = plugin.getConfigManager().colorize(itemConfig.getString("display-name"));
                }
                
                List<String> lore = null;
                if (itemConfig.contains("lore")) {
                    lore = new ArrayList<>();
                    for (String line : itemConfig.getStringList("lore")) {
                        lore.add(plugin.getConfigManager().colorize(line));
                    }
                }
                
                customItems.add(new CustomItemData(material, displayName, lore, sellValue));
            }
        }
        plugin.getLogger().info("Loaded " + customItems.size() + " custom item sell values");
    }
    
    // PERUBAHAN: Menambahkan parameter Player untuk kompatibilitas ShopGUI+
    public double getSellValue(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 0.0;
        
        // RESTRICTION: Only sell generator results
        if (!isGeneratorItem(item)) return 0.0;
        
        // 1. Cek NBT Tag (Metode Baru, Kecepatan & Akurasi 100% untuk Generator)
        if (item.hasItemMeta()) {
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "seriagens_value");
            org.bukkit.persistence.PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            if (pdc.has(key, org.bukkit.persistence.PersistentDataType.DOUBLE)) {
                return pdc.get(key, org.bukkit.persistence.PersistentDataType.DOUBLE) * eventMultiplier;
            }
        }
        
        // 2. Metode Lama (Fallback untuk item generator jadul)
        for (CustomItemData data : customItems) {
            if (data.matches(item)) {
                return data.sellValue * eventMultiplier;
            }
        }
        
        // 3. Integrasi ShopGUI+ (Fallback untuk item generator yang tidak punya harga di config)
        if (shopGUIPlus != null && shopGUIPlus.isSellable(player, item)) {
            return shopGUIPlus.getSellPrice(player, item) * eventMultiplier;
        }
        
        return 0.0;
    }
    
    /**
     * Memeriksa apakah suatu item adalah produk dari generator SeriaGens.
     */
    public boolean isGeneratorItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        // Cek NBT Tag khusus
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "seriagens_value");
        if (item.getItemMeta().getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.DOUBLE)) {
            return true;
        }
        
        // Cek kecocokan dengan konfigurasi (untuk item tanpa NBT)
        for (CustomItemData data : customItems) {
            if (data.matches(item)) return true;
        }
        
        return false;
    }
    
    // Sesuaikan isSellable dengan parameter baru
    public boolean isSellable(Player player, ItemStack item) {
        return getSellValue(player, item) > 0;
    }
    
    public void setEventMultiplier(double multiplier) { this.eventMultiplier = multiplier; }
    public double getEventMultiplier() { return eventMultiplier; }
    public void resetEventMultiplier() { this.eventMultiplier = 1.0; }
    public ShopGUIPlusIntegration getShopGUIPlus() { return shopGUIPlus; }
    
    public void reload() {
        loadItemValues();
        shopGUIPlus.reload();
    }
    
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