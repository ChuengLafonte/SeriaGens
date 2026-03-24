package id.seria.gens.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import id.seria.gens.SeriaGens;
import id.seria.gens.models.Generator;

public class GeneratorManagementGUI extends BaseGUI {
    
    private final SeriaGens plugin;
    private int page;
    private List<Generator> playerGenerators;
    
    public GeneratorManagementGUI(SeriaGens plugin, Player player) { this(plugin, player, 0); }
    
    public GeneratorManagementGUI(SeriaGens plugin, Player player, int page) {
        super(player, plugin.getConfigManager().colorize(plugin.getConfigManager().getGuiConfig().getString("management.title", "&6&lManajemen Aset")), 54);
        this.plugin = plugin;
        this.page = page;
    }
    
    @Override
    public void init() {
        playerGenerators = plugin.getGeneratorManager().getAllGenerators().stream()
            .filter(g -> g.getOwner().equals(player.getUniqueId())).collect(Collectors.toList());
        fillBorders();
        populateGenerators();
    }
    
    private void populateGenerators() {
        FileConfiguration guiCfg = plugin.getConfigManager().getGuiConfig();
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        int startIndex = page * slots.length;
        
        for (int i = 0; i < slots.length; i++) {
            int genIndex = startIndex + i;
            if (genIndex < playerGenerators.size()) {
                Generator gen = playerGenerators.get(genIndex);
                ItemStack item = plugin.getGeneratorManager().getGeneratorItem(gen.getType());
                ItemMeta meta = item.getItemMeta();
                
                List<String> loreConfig = gen.isCorrupted() ? 
                    guiCfg.getStringList("management.generator-lore-corrupted") :
                    guiCfg.getStringList("management.generator-lore-normal");
                    
                List<String> lore = new ArrayList<>();
                for(String line : loreConfig) {
                    lore.add(plugin.getConfigManager().colorize(line
                        .replace("{x}", String.valueOf(gen.getLocation().getBlockX()))
                        .replace("{y}", String.valueOf(gen.getLocation().getBlockY()))
                        .replace("{z}", String.valueOf(gen.getLocation().getBlockZ()))
                    ));
                }
                
                meta.setLore(lore);
                item.setItemMeta(meta);
                inventory.setItem(slots[i], item);
                setAction(slots[i], (p, event) -> {
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new UpgradeGUI(plugin, p, gen).open();
                });
            } else {
                inventory.setItem(slots[i], new ItemStack(Material.AIR));
                actions.remove(slots[i]);
            }
        }
        
        if (page > 0) {
            ItemStack prev = createItem(Material.ARROW, guiCfg.getString("management.prev-page", "&e⬅ Sebelumnya"));
            inventory.setItem(48, prev);
            setAction(48, (p, event) -> new GeneratorManagementGUI(plugin, p, page - 1).open());
        }
        
        if (startIndex + slots.length < playerGenerators.size()) {
            ItemStack next = createItem(Material.ARROW, guiCfg.getString("management.next-page", "&eSelanjutnya ➡"));
            inventory.setItem(50, next);
            setAction(50, (p, event) -> new GeneratorManagementGUI(plugin, p, page + 1).open());
        }
        
        double totalRepairCost = calculateTotalRepairCost();
        List<String> summaryLoreConfig = guiCfg.getStringList("management.summary-lore");
        List<String> summaryLore = new ArrayList<>();
        for(String line : summaryLoreConfig) {
            summaryLore.add(line.replace("{count}", String.valueOf(playerGenerators.size())).replace("{repair_cost}", String.valueOf(totalRepairCost)));
        }
        
        ItemStack summary = createItem(Material.PAPER, guiCfg.getString("management.summary-title", "&b&lStatistik"), summaryLore.toArray(new String[0]));
        inventory.setItem(49, summary);
    }
    
    private double calculateTotalRepairCost() {
        double total = 0.0;
        for (Generator gen : playerGenerators) {
            if (gen.isCorrupted()) {
                ConfigurationSection genConfig = plugin.getConfigManager().getGeneratorsConfig().getConfigurationSection(gen.getType());
                if (genConfig != null) { total += genConfig.getDouble("corrupted.cost", 50.0); }
            }
        }
        return total;
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getConfigManager().colorize(name));
        if (lore.length > 0) {
            List<String> loreList = new ArrayList<>();
            for (String line : lore) { loreList.add(plugin.getConfigManager().colorize(line)); }
            meta.setLore(loreList);
        }
        item.setItemMeta(meta);
        return item;
    }
    
    private void fillBorders() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);
        for (int i = 0; i < 9; i++) { inventory.setItem(i, glass); inventory.setItem(45 + i, glass); }
        for (int i = 9; i < 45; i += 9) { inventory.setItem(i, glass); inventory.setItem(i + 8, glass); }
    }
}