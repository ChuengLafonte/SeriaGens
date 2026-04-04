package id.seria.gens.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import id.seria.gens.SeriaGens;
import id.seria.gens.managers.RequirementsChecker.RequirementResult;
import id.seria.gens.managers.RequirementsChecker.RequirementType;
import id.seria.gens.models.Generator;

public class UpgradeGUI extends BaseGUI {
    
    private final SeriaGens plugin;
    private final Generator generator;
    
    public UpgradeGUI(SeriaGens plugin, Player player, Generator generator) {
        super(player, plugin.getConfigManager().colorize(plugin.getConfigManager().getGuiConfig().getString("upgrade.title", "&6Pengaturan")), 27);
        this.plugin = plugin;
        this.generator = generator;
    }
    
    @Override
    public void init() {
        if (generator.isCorrupted()) showRepairMenu();
        else showUpgradeMenu();
    }
    
    private void showUpgradeMenu() {
        fillBorders(Material.BLACK_STAINED_GLASS_PANE);
        FileConfiguration guiCfg = plugin.getConfigManager().getGuiConfig();
        ConfigurationSection genConfig = plugin.getConfigManager().getGeneratorsConfig().getConfigurationSection(generator.getType());
        if (genConfig == null) return;
        
        inventory.setItem(11, buildDisplayItem(generator.getType(), false));
        String nextTier = genConfig.getString("upgrade.next-tier", "none");
        
        if (nextTier.equalsIgnoreCase("none") || nextTier.equals("[]")) {
            List<String> lore = guiCfg.getStringList("upgrade.max-tier-lore");
            ItemStack maxLevel = createItem(Material.BARRIER, guiCfg.getString("upgrade.max-tier-title", "&c&lMAX TIER"), lore.toArray(new String[0]));
            inventory.setItem(15, maxLevel);
        } else {
            inventory.setItem(15, buildDisplayItem(nextTier, true));
            double cost = genConfig.getDouble("upgrade.cost", 0.0);
            List<String> loreConfig = guiCfg.getStringList("upgrade.upgrade-btn-lore");
            List<String> lore = new ArrayList<>();
            for(String l : loreConfig) { lore.add(l.replace("{cost}", String.valueOf(cost))); }
            
            ItemStack upgradeBtn = createItem(Material.EMERALD_BLOCK, guiCfg.getString("upgrade.upgrade-btn-title", "&a&lUPGRADE"), lore.toArray(new String[0]));
            inventory.setItem(13, upgradeBtn);
            
            setAction(13, (p, event) -> {
                RequirementResult result = plugin.getRequirementsChecker().checkRequirements(p, generator.getType(), RequirementType.UPGRADE);
                if (!result.hasPassed()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    result.sendMessages(p);
                    p.closeInventory();
                    return;
                }
                if (plugin.getEconomy() != null && plugin.getEconomy().withdrawPlayer(p, cost).transactionSuccess()) {
                    generator.setType(nextTier);
                    plugin.getDatabaseManager().saveGenerator(generator);
                    plugin.getGeneratorManager().restoreGeneratorBlock(generator);
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    p.sendMessage(plugin.getConfigManager().colorize("&aBerhasil meng-upgrade generator!"));
                    p.closeInventory();
                } else {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    p.sendMessage(plugin.getConfigManager().colorize("&cDana tidak mencukupi untuk upgrade!"));
                }
            });
        }
        
        int backSlot = guiCfg.getInt("upgrade.back-btn-slot", 26);
        ItemStack back = createItem(Material.DARK_OAK_DOOR, guiCfg.getString("upgrade.back-btn", "&c⬅ Kembali ke Gardu Induk"));
        if(backSlot < inventory.getSize()) {
            inventory.setItem(backSlot, back);
            setAction(backSlot, (p, event) -> new GeneratorManagementGUI(plugin, p).open());
        }
    }
    
    private void showRepairMenu() {
        fillBorders(Material.RED_STAINED_GLASS_PANE);
        FileConfiguration guiCfg = plugin.getConfigManager().getGuiConfig();
        ConfigurationSection genConfig = plugin.getConfigManager().getGeneratorsConfig().getConfigurationSection(generator.getType());
        double repairCost = genConfig != null ? genConfig.getDouble("corrupted.cost", 50.0) : 50.0;
        
        List<String> loreConfig = guiCfg.getStringList("upgrade.repair-btn-lore");
        List<String> lore = new ArrayList<>();
        for(String l : loreConfig) { lore.add(l.replace("{cost}", String.valueOf(repairCost))); }
        
        ItemStack warningItem = createItem(Material.REDSTONE_BLOCK, guiCfg.getString("upgrade.repair-btn-title", "&c&lRUSAK!"), lore.toArray(new String[0]));
        inventory.setItem(13, warningItem);
        setAction(13, (p, event) -> {
            if (plugin.getCorruptionManager().repairGenerator(generator, p)) {
                p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
                new UpgradeGUI(plugin, p, generator).open();
            } else {
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        });
        
        int backSlot = guiCfg.getInt("upgrade.back-btn-slot", 26);
        ItemStack back = createItem(Material.DARK_OAK_DOOR, guiCfg.getString("upgrade.back-btn", "&c⬅ Kembali ke Gardu Induk"));
        if(backSlot < inventory.getSize()) {
            inventory.setItem(backSlot, back);
            setAction(backSlot, (p, event) -> new GeneratorManagementGUI(plugin, p).open());
        }
    }

    @SuppressWarnings("deprecation")
    private ItemStack buildDisplayItem(String type, boolean next) {
        FileConfiguration guiCfg = plugin.getConfigManager().getGuiConfig();
        ConfigurationSection genConfig = plugin.getConfigManager().getGeneratorsConfig().getConfigurationSection(type);
        if (genConfig == null) return new ItemStack(Material.STONE);
        
        String materialName = genConfig.getString("item.material", "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.STONE;
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String displayName = genConfig.getString("display-name", type);
        String format = next ? guiCfg.getString("upgrade.display-next", "&a&lNEXT: {name}") : 
                               guiCfg.getString("upgrade.display-current", "&e&lCURRENT: {name}");
        meta.setDisplayName(plugin.getConfigManager().colorize(format.replace("{name}", displayName)));
        
        // PERBAIKAN: Menerjemahkan placeholder {fuel_cost}
        int fuelCost = genConfig.getInt("joule-cost-per-drop", 1);
        
        List<String> lore = new ArrayList<>();
        for(String l : guiCfg.getStringList("upgrade.display-lore")) {
            lore.add(plugin.getConfigManager().colorize(l
                .replace("{type}", type)
                .replace("{interval}", String.valueOf(genConfig.getInt("interval", 20)))
                .replace("{fuel_cost}", String.valueOf(fuelCost))
            ));
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    @SuppressWarnings("deprecation")
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getConfigManager().colorize(name));
        List<String> loreList = new ArrayList<>();
        for (String line : lore) loreList.add(plugin.getConfigManager().colorize(line));
        meta.setLore(loreList);
        item.setItemMeta(meta);
        return item;
    }
    
    @SuppressWarnings("deprecation")
    private void fillBorders(Material material) {
        ItemStack glass = new ItemStack(material);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (i < 10 || i > 16 || i % 9 == 0 || (i + 1) % 9 == 0) { 
                inventory.setItem(i, glass); 
            }
        }
    }
}