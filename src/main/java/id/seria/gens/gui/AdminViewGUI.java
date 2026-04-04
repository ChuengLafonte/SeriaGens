package id.seria.gens.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import id.seria.gens.SeriaGens;
import id.seria.gens.models.Generator;

public class AdminViewGUI extends BaseGUI {

    private final SeriaGens plugin;
    private final OfflinePlayer target;
    private int page;
    private List<Generator> targetGenerators;

    public AdminViewGUI(SeriaGens plugin, Player admin, OfflinePlayer target) {
        this(plugin, admin, target, 0);
    }

    public AdminViewGUI(SeriaGens plugin, Player admin, OfflinePlayer target, int page) {
        super(admin, plugin.getConfigManager().colorize(
            plugin.getConfigManager().getGuiConfig().getString("admin-view.title", "&c&l[CCTV] &8Aset: &n{player}")
            .replace("{player}", target.getName() != null ? target.getName() : "Unknown")
        ), plugin.getConfigManager().getGuiConfig().getInt("admin-view.size", 54));
        this.plugin = plugin;
        this.target = target;
        this.page = page;
    }

    @Override
    public void init() {
        targetGenerators = plugin.getGeneratorManager().getAllGenerators().stream()
            .filter(g -> g.getOwner().equals(target.getUniqueId())).collect(Collectors.toList());
        fillBorders();
        populateGenerators();
    }

    @SuppressWarnings("deprecation")
    private void populateGenerators() {
        FileConfiguration guiCfg = plugin.getConfigManager().getGuiConfig();
        List<Integer> slots = guiCfg.getIntegerList("admin-view.generator-slots");
        int startIndex = page * slots.size();

        for (int i = 0; i < slots.size(); i++) {
            int genIndex = startIndex + i;
            int slot = slots.get(i);
            
            if (genIndex < targetGenerators.size()) {
                Generator gen = targetGenerators.get(genIndex);
                ItemStack item = plugin.getGeneratorManager().getGeneratorItem(gen.getType());
                ItemMeta meta = item.getItemMeta();
                
                String statusNormal = guiCfg.getString("admin-view.status-normal", "&a&lAKTIF");
                String statusCorrupted = guiCfg.getString("admin-view.status-corrupted", "&c&lRUSAK");
                String status = gen.isCorrupted() ? statusCorrupted : statusNormal;
                
                List<String> lore = new ArrayList<>();
                for(String line : guiCfg.getStringList("admin-view.generator-lore")) {
                    lore.add(plugin.getConfigManager().colorize(line
                        .replace("{status}", status)
                        .replace("{x}", String.valueOf(gen.getLocation().getBlockX()))
                        .replace("{y}", String.valueOf(gen.getLocation().getBlockY()))
                        .replace("{z}", String.valueOf(gen.getLocation().getBlockZ()))
                    ));
                }
                
                meta.setLore(lore);
                item.setItemMeta(meta);
                inventory.setItem(slot, item);
                
                setAction(slot, (p, event) -> {
                    if (event.getClick() == ClickType.LEFT) {
                        p.teleport(gen.getLocation().clone().add(0.5, 1, 0.5));
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    } else if (event.getClick() == ClickType.RIGHT) {
                        Player onlineTarget = target.isOnline() ? target.getPlayer() : null;
                        plugin.getGeneratorManager().removeGenerator(gen.getLocation(), onlineTarget);
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1.0f, 1.0f);
                        new AdminViewGUI(plugin, p, target, page).open(); 
                    }
                });
            }
        }

        if (page > 0) {
            ItemStack prev = createItem(Material.ARROW, "&e⬅ Prev Page");
            inventory.setItem(48, prev);
            setAction(48, (p, event) -> new AdminViewGUI(plugin, p, target, page - 1).open());
        }
        if (startIndex + slots.size() < targetGenerators.size()) {
            ItemStack next = createItem(Material.ARROW, "&eNext Page ➡");
            inventory.setItem(50, next);
            setAction(50, (p, event) -> new AdminViewGUI(plugin, p, target, page + 1).open());
        }
        
        int infoSlot = guiCfg.getInt("admin-view.items.info.slot", 49);
        String infoName = guiCfg.getString("admin-view.items.info.name", "&b&lInfo Pemain");
        Material infoMat = Material.matchMaterial(guiCfg.getString("admin-view.items.info.material", "PAPER"));
        List<String> infoLore = new ArrayList<>();
        int extraSlots = plugin.getConfigManager().getPlayersConfig().getInt(target.getUniqueId().toString() + ".extra-slots", 0);
        for(String line : guiCfg.getStringList("admin-view.items.info.lore")) {
            infoLore.add(plugin.getConfigManager().colorize(line
                .replace("{count}", String.valueOf(targetGenerators.size()))
                .replace("{extra_slots}", String.valueOf(extraSlots))
            ));
        }
        inventory.setItem(infoSlot, createItem(infoMat != null ? infoMat : Material.PAPER, infoName, infoLore.toArray(new String[0])));
    }

    @SuppressWarnings("deprecation")
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getConfigManager().colorize(name));
        List<String> loreList = new ArrayList<>();
        for (String l : lore) loreList.add(plugin.getConfigManager().colorize(l));
        meta.setLore(loreList);
        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings("deprecation")
    private void fillBorders() {
        String matStr = plugin.getConfigManager().getGuiConfig().getString("admin-view.items.filler.material", "RED_STAINED_GLASS_PANE");
        Material mat = Material.matchMaterial(matStr);
        ItemStack glass = new ItemStack(mat != null ? mat : Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(plugin.getConfigManager().colorize(plugin.getConfigManager().getGuiConfig().getString("admin-view.items.filler.name", " ")));
        glass.setItemMeta(meta);
        
        for (int i = 0; i < 9; i++) { inventory.setItem(i, glass); inventory.setItem(45 + i, glass); }
        for (int i = 9; i < 45; i += 9) { inventory.setItem(i, glass); inventory.setItem(i + 8, glass); }
    }
}