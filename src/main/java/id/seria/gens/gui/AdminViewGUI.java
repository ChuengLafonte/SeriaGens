package id.seria.gens.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
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
        super(admin, plugin.getConfigManager().colorize("&c&l[Admin] &8Aset: &n" + target.getName()), 54);
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

    private void populateGenerators() {
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        int startIndex = page * slots.length;

        for (int i = 0; i < slots.length; i++) {
            int genIndex = startIndex + i;
            if (genIndex < targetGenerators.size()) {
                Generator gen = targetGenerators.get(genIndex);
                ItemStack item = plugin.getGeneratorManager().getGeneratorItem(gen.getType());
                ItemMeta meta = item.getItemMeta();
                
                String status = gen.isCorrupted() ? "&c&lRUSAK" : "&a&lAKTIF";
                
                List<String> lore = new ArrayList<>();
                lore.add(plugin.getConfigManager().colorize("&8&m━━━━━━━━━━━━━━━━━━━━"));
                lore.add(plugin.getConfigManager().colorize("&7Status: " + status));
                lore.add(plugin.getConfigManager().colorize("&7Lokasi: &e" + gen.getLocation().getBlockX() + ", " + gen.getLocation().getBlockY() + ", " + gen.getLocation().getBlockZ()));
                lore.add("");
                lore.add(plugin.getConfigManager().colorize("&b[Klik Kiri] &7Teleport ke Mesin"));
                lore.add(plugin.getConfigManager().colorize("&c[Klik Kanan] &7Cabut/Pickup Paksa"));
                
                meta.setLore(lore);
                item.setItemMeta(meta);

                inventory.setItem(slots[i], item);
                
                setAction(slots[i], (p, event) -> {
                    if (event.getClick() == ClickType.LEFT) {
                        // Fitur Teleport
                        p.teleport(gen.getLocation().clone().add(0.5, 1, 0.5));
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                        p.sendMessage(plugin.getConfigManager().colorize("&aTeleportasi ke aset milik &e" + target.getName() + "&a."));
                    } else if (event.getClick() == ClickType.RIGHT) {
                        // Fitur Hapus/Pickup Paksa
                        Player onlineTarget = target.isOnline() ? target.getPlayer() : null;
                        plugin.getGeneratorManager().removeGenerator(gen.getLocation(), onlineTarget);
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1.0f, 1.0f);
                        p.sendMessage(plugin.getConfigManager().colorize("&cGenerator berhasil dicabut paksa!"));
                        new AdminViewGUI(plugin, p, target, page).open(); // Refresh GUI
                    }
                });
            }
        }

        // Navigasi Halaman
        if (page > 0) {
            ItemStack prev = createItem(Material.ARROW, "&e⬅ Halaman Sebelumnya");
            inventory.setItem(48, prev);
            setAction(48, (p, event) -> new AdminViewGUI(plugin, p, target, page - 1).open());
        }
        if (startIndex + slots.length < targetGenerators.size()) {
            ItemStack next = createItem(Material.ARROW, "&eHalaman Selanjutnya ➡");
            inventory.setItem(50, next);
            setAction(50, (p, event) -> new AdminViewGUI(plugin, p, target, page + 1).open());
        }
        
        ItemStack info = createItem(Material.PAPER, "&b&lInfo Pemain", 
            "&7Total Mesin: &e" + targetGenerators.size(),
            "&7Extra Slot: &a+" + plugin.getConfigManager().getPlayersConfig().getInt(target.getUniqueId().toString() + ".extra-slots", 0));
        inventory.setItem(49, info);
    }

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

    private void fillBorders() {
        ItemStack glass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);
        for (int i = 0; i < 9; i++) { inventory.setItem(i, glass); inventory.setItem(45 + i, glass); }
        for (int i = 9; i < 45; i += 9) { inventory.setItem(i, glass); inventory.setItem(i + 8, glass); }
    }
}