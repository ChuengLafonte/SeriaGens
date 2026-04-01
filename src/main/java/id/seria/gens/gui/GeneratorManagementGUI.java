package id.seria.gens.gui;

import id.seria.gens.SeriaGens;
import id.seria.gens.managers.FuelManager;
import id.seria.gens.managers.RequirementsChecker.RequirementResult;
import id.seria.gens.managers.RequirementsChecker.RequirementType;
import id.seria.gens.models.Generator;
import id.seria.gens.models.PlayerGlobalGrid;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class GeneratorManagementGUI extends BaseGUI {
    
    private final SeriaGens plugin;
    private final PlayerGlobalGrid globalGrid;
    
    private int currentPage = 0;
    private List<Generator> allPlayerGens;
    private final List<Integer> genSlots;
    
    private int refreshTaskId = -1; // Task ID untuk Live Update
    
    public GeneratorManagementGUI(SeriaGens plugin, Player player) {
        super(player, 
              plugin.getConfigManager().colorize(plugin.getConfigManager().getGuiConfig().getString("management.title", "&6Generator Saya")), 
              plugin.getConfigManager().getGuiConfig().getInt("management.size", 54));
              
        this.plugin = plugin;
        this.globalGrid = plugin.getFuelManager().getGlobalGrid(player.getUniqueId());
        
        this.allPlayerGens = plugin.getGeneratorManager().getAllGenerators().stream()
                .filter(g -> g.getOwner().equals(player.getUniqueId()))
                .collect(Collectors.toList());
                
        this.genSlots = plugin.getConfigManager().getGuiConfig().getIntegerList("management.generator-slots");
        if (this.genSlots.isEmpty()) {
            for (int i = 10; i < 17; i++) genSlots.add(i); 
        }
        
        this.globalGrid.calculateGridStatus(plugin);
    }
    
    @Override
    public void init() {
        showManagementMenu();
    }
    
    // Override metode open untuk menjalankan Auto-Refresh saat GUI terbuka
    @Override
    public void open() {
        super.open();
        startRefreshTask();
    }
    
    // Fitur LIVE UPDATE setiap 1 detik
    private void startRefreshTask() {
        if (refreshTaskId != -1) return;
        refreshTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            // Pastikan player masih membuka GUI ini
            if (player.getOpenInventory().getTopInventory().equals(inventory)) {
                
                // Ambil ulang data generator terbaru (jika ada yang diletakkan/dihancurkan/rusak di background)
                this.allPlayerGens = plugin.getGeneratorManager().getAllGenerators().stream()
                        .filter(g -> g.getOwner().equals(player.getUniqueId()))
                        .collect(Collectors.toList());

                // Kalkulasi ulang Daya Gardu (bahan bakar yang berkurang karena drop item)
                globalGrid.calculateGridStatus(plugin);
                
                FileConfiguration guiCfg = plugin.getConfigManager().getGuiConfig();
                
                // Update elemen-elemen dinamis secara halus (tanpa menghapus/clear seluruh isi GUI)
                renderGeneratorPage(guiCfg);
                renderGlobalGarduSlots(guiCfg);
                renderStatistics(guiCfg);
            } else {
                stopRefreshTask();
            }
        }, 20L, 20L); // 20 Ticks = 1 Detik
    }
    
    private void stopRefreshTask() {
        if (refreshTaskId != -1) {
            Bukkit.getScheduler().cancelTask(refreshTaskId);
            refreshTaskId = -1;
        }
    }
    
    private int getTotalRequiredJoules() {
        int total = 0;
        for (Generator gen : allPlayerGens) {
            total += plugin.getConfigManager().getGeneratorsConfig().getInt(gen.getType() + ".joule-cost-per-drop", 1);
        }
        return total;
    }
    
    private void showManagementMenu() {
        inventory.clear(); 
        FileConfiguration guiCfg = plugin.getConfigManager().getGuiConfig();
        
        // 1. Render Sistem Priority (Background, Tombol, Filler)
        renderPriorityConfigItems(guiCfg);
        
        // 2. Render Core System
        renderGeneratorPage(guiCfg);
        renderGlobalGarduSlots(guiCfg);
        renderStatistics(guiCfg);
    }

    @Override
    public void onInventoryClose(InventoryCloseEvent event) {
        super.onInventoryClose(event);
        stopRefreshTask(); // Matikan task saat GUI ditutup
        
        if (globalGrid.isOverCapacity()) {
            Player p = (Player) event.getPlayer();
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            p.sendMessage(plugin.getConfigManager().colorize("&c&l⚠ ALARM: &fGardu Induk dibiarkan OVER CAPACITY! Generator dinonaktifkan."));
        }
    }
    
    private static class ConfigItem {
        String key; int priority; List<Integer> slots; ItemStack itemStack;
        ConfigItem(String key, int priority, List<Integer> slots, ItemStack itemStack) {
            this.key = key; this.priority = priority; this.slots = slots; this.itemStack = itemStack;
        }
    }

    private void renderPriorityConfigItems(FileConfiguration guiCfg) {
        ConfigurationSection itemsSec = guiCfg.getConfigurationSection("management.items");
        if (itemsSec == null) return;

        List<ConfigItem> cItems = new ArrayList<>();
        
        for (String key : itemsSec.getKeys(false)) {
            ConfigurationSection itemCfg = itemsSec.getConfigurationSection(key);
            if (itemCfg == null) continue;
            
            int priority = itemCfg.getInt("priority", 1);
            List<Integer> slots = new ArrayList<>();
            if (itemCfg.contains("slot")) slots.add(itemCfg.getInt("slot"));
            if (itemCfg.contains("slots")) slots.addAll(itemCfg.getIntegerList("slots"));
            
            String matStr = itemCfg.getString("material", "STONE");
            Material mat = Material.matchMaterial(matStr);
            if (mat == null) mat = Material.STONE;
            
            String name = itemCfg.getString("name", " ");
            List<String> lore = itemCfg.getStringList("lore");
            
            ItemStack item = createItem(mat, name, lore.toArray(new String[0]));
            cItems.add(new ConfigItem(key, priority, slots, item));
        }

        cItems.sort(Comparator.comparingInt(c -> c.priority));
        int totalPages = (int) Math.ceil((double) allPlayerGens.size() / genSlots.size());

        for (ConfigItem ci : cItems) {
            for (int slot : ci.slots) {
                if (slot < 0 || slot >= inventory.getSize()) continue;
                
                inventory.setItem(slot, ci.itemStack);
                setAction(slot, (p, e) -> e.setCancelled(true));
                
                if (ci.key.equalsIgnoreCase("close-btn")) {
                    setAction(slot, (p, e) -> { e.setCancelled(true); p.closeInventory(); });
                } 
                else if (ci.key.equalsIgnoreCase("next-btn")) {
                    if (currentPage < totalPages - 1) {
                        ItemMeta meta = ci.itemStack.getItemMeta();
                        meta.setDisplayName(meta.getDisplayName().replace("{page}", String.valueOf(currentPage + 2)));
                        ci.itemStack.setItemMeta(meta);
                        inventory.setItem(slot, ci.itemStack);
                        setAction(slot, (p, e) -> { e.setCancelled(true); currentPage++; showManagementMenu(); });
                    } else {
                        inventory.setItem(slot, getFallbackFiller(guiCfg));
                    }
                } 
                else if (ci.key.equalsIgnoreCase("back-btn")) {
                    if (currentPage > 0) {
                        ItemMeta meta = ci.itemStack.getItemMeta();
                        meta.setDisplayName(meta.getDisplayName().replace("{page}", String.valueOf(currentPage)));
                        ci.itemStack.setItemMeta(meta);
                        inventory.setItem(slot, ci.itemStack);
                        setAction(slot, (p, e) -> { e.setCancelled(true); currentPage--; showManagementMenu(); });
                    } else {
                        inventory.setItem(slot, getFallbackFiller(guiCfg));
                    }
                }
            }
        }
    }
    
    private ItemStack getFallbackFiller(FileConfiguration guiCfg) {
        String matStr = guiCfg.getString("management.items.filler.material", "BLACK_STAINED_GLASS_PANE");
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.BLACK_STAINED_GLASS_PANE;
        return createItem(mat, guiCfg.getString("management.items.filler.name", " "));
    }

    private void renderGeneratorPage(FileConfiguration guiCfg) {
        int itemsPerPage = genSlots.size();
        int totalGens = allPlayerGens.size();
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalGens);
        
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Generator gen = allPlayerGens.get(i);
            ItemStack genItem = buildGeneratorDisplayItem(gen, guiCfg);
            
            int conceptualSlot = genSlots.get(slotIndex);
            if (conceptualSlot < inventory.getSize()) {
                inventory.setItem(conceptualSlot, genItem);
                
                setAction(conceptualSlot, (p, event) -> {
                    event.setCancelled(true);
                    if (event.getClick().isRightClick()) {
                        new UpgradeGUI(plugin, p, gen).open();
                    } else if (event.getClick().isLeftClick()) {
                        if (gen.isCorrupted()) {
                            p.sendMessage(plugin.getConfigManager().colorize("&c&l⚠ &cGenerator rusak tidak bisa dicabut! Perbaiki di menu upgrade."));
                        } else {
                            p.closeInventory();
                            plugin.getGeneratorManager().removeGenerator(gen.getLocation(), p);
                        }
                    }
                });
            }
            slotIndex++;
        }
        
        // Membersihkan slot kosong (Jika ada generator yang dihancurkan saat menu terbuka)
        while (slotIndex < genSlots.size()) {
            int conceptualSlot = genSlots.get(slotIndex);
            if (conceptualSlot < inventory.getSize()) {
                inventory.setItem(conceptualSlot, new ItemStack(Material.AIR));
                actions.remove(conceptualSlot);
            }
            slotIndex++;
        }
    }

    private void renderGlobalGarduSlots(FileConfiguration guiCfg) {
        List<Integer> fuelSlots = guiCfg.getIntegerList("management.fuel-slots");
        if (fuelSlots.isEmpty() || fuelSlots.size() != 5) {
            fuelSlots = java.util.Arrays.asList(38, 39, 40, 41, 42);
        }
        
        for (int i = 0; i < 5; i++) {
            int conceptualSlot = fuelSlots.get(i);
            int fuelIndex = i;
            
            if (conceptualSlot >= inventory.getSize()) continue;
            
            RequirementResult result = plugin.getRequirementsChecker().checkRequirements(player, "global_fuel_slot_" + (fuelIndex + 1), RequirementType.FUEL_UNLOCK);
            
            if (!result.hasPassed()) {
                inventory.setItem(conceptualSlot, createItem(Material.RED_STAINED_GLASS_PANE, "&c&lSLOT TERKUNCI"));
                setAction(conceptualSlot, (p, e) -> {
                    e.setCancelled(true);
                    result.sendMessages(p);
                });
            } else {
                ItemStack rawFuelItem = globalGrid.getFuel(fuelIndex);
                if (rawFuelItem == null || rawFuelItem.getType() == Material.AIR) {
                    inventory.setItem(conceptualSlot, createItem(Material.YELLOW_STAINED_GLASS_PANE, "&e&lSLOT FUEL GLOBAL " + (fuelIndex + 1), "&7Drag & Drop Fuel Kesini!"));
                } else {
                    ItemStack displayFuel = rawFuelItem.clone();
                    ItemMeta meta = displayFuel.getItemMeta();
                    List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                    
                    lore.add(plugin.getConfigManager().colorize("&8&m------------------"));
                    lore.add(plugin.getConfigManager().colorize("&7Fuel aktif di Gardu Induk"));
                    
                    FuelManager.FuelData fData = plugin.getFuelManager().getFuelData(displayFuel.getType());
                    if (fData != null) {
                        lore.add(plugin.getConfigManager().colorize("&8» &7Tenaga: &e" + fData.joulesPerItem + " J/Item"));
                        lore.add(plugin.getConfigManager().colorize("&8» &7Total: &6" + (fData.joulesPerItem * displayFuel.getAmount()) + " J"));
                    }
                    
                    if (globalGrid.isOverCapacity()) {
                        lore.add("");
                        lore.add(plugin.getConfigManager().colorize("&c&l⚠ GARDU INDUK OVERLOAD"));
                        lore.add(plugin.getConfigManager().colorize("&cKapasitas Rank-mu tidak muat."));
                    }
                    
                    meta.setLore(lore);
                    displayFuel.setItemMeta(meta);
                    inventory.setItem(conceptualSlot, displayFuel);
                }
                
                setAction(conceptualSlot, (p, e) -> {
                    e.setCancelled(true);
                    ItemStack cursor = e.getCursor();
                    ItemStack clicked = e.getCurrentItem();
                    boolean isFiller = clicked != null && (clicked.getType() == Material.YELLOW_STAINED_GLASS_PANE || clicked.getType() == Material.RED_STAINED_GLASS_PANE);
                    
                    if (cursor != null && cursor.getType() != Material.AIR) {
                        if (plugin.getFuelManager().isFuel(cursor.getType())) {
                            if (isFiller) {
                                inventory.setItem(conceptualSlot, cursor.clone());
                                p.setItemOnCursor(null);
                            } else {
                                inventory.setItem(conceptualSlot, cursor.clone());
                                p.setItemOnCursor(rawFuelItem.clone()); 
                            }
                            // Muluskan drag n drop tanpa clear() layar sepenuhnya
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                globalGrid.setFuel(fuelIndex, inventory.getItem(conceptualSlot));
                                globalGrid.calculateGridStatus(plugin);
                                renderGlobalGarduSlots(guiCfg);
                                renderStatistics(guiCfg);
                            }, 1L);
                        } else {
                            p.sendMessage(plugin.getConfigManager().colorize("&cItu bukan bahan bakar Gardu Induk yang valid!"));
                        }
                    } else if (!isFiller && rawFuelItem != null && rawFuelItem.getType() != Material.AIR) {
                        p.setItemOnCursor(rawFuelItem.clone());
                        inventory.setItem(conceptualSlot, createItem(Material.YELLOW_STAINED_GLASS_PANE, "&e&lSLOT FUEL GLOBAL " + (fuelIndex + 1), "&7Drag & Drop Fuel Kesini!"));
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            globalGrid.setFuel(fuelIndex, null);
                            globalGrid.calculateGridStatus(plugin);
                            renderGlobalGarduSlots(guiCfg);
                            renderStatistics(guiCfg);
                        }, 1L);
                    }
                });
            }
        }
    }
    
    private ItemStack buildGeneratorDisplayItem(Generator gen, FileConfiguration guiCfg) {
        ConfigurationSection cfg = plugin.getConfigManager().getGeneratorsConfig().getConfigurationSection(gen.getType());
        Material mat = cfg != null ? Material.matchMaterial(cfg.getString("item.material", "STONE")) : Material.STONE;
        if (mat == null) mat = Material.STONE;
        
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getConfigManager().colorize(cfg != null ? cfg.getString("display-name", gen.getType()) : gen.getType()));
        
        List<String> loreTemplate = guiCfg.getStringList("management.generator-display-lore");
        List<String> finalLore = new ArrayList<>();
        
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(gen.getOwner());
        String ownerName = offlinePlayer.getName();
        if (ownerName == null || ownerName.isEmpty()) ownerName = "Unknown";
        
        String statusStr = gen.isCorrupted() ? 
            guiCfg.getString("management.status-corrupted", "&c&l⚠ STATUS: RUSAK") : 
            guiCfg.getString("management.status-normal", "&a&l✔ STATUS: OPERASIONAL");
        
        for (String line : loreTemplate) {
            line = line.replace("{owner}", ownerName)
                       .replace("{x}", String.valueOf(gen.getLocation().getBlockX()))
                       .replace("{y}", String.valueOf(gen.getLocation().getBlockY()))
                       .replace("{z}", String.valueOf(gen.getLocation().getBlockZ()))
                       .replace("{status}", statusStr);
            finalLore.add(plugin.getConfigManager().colorize(line));
        }
        
        meta.setLore(finalLore);
        item.setItemMeta(meta);
        return item;
    }

    private void renderStatistics(FileConfiguration guiCfg) {
        int maxJoule = globalGrid.getMaxJoulesLimitByRank(plugin);
        int totalRequired = getTotalRequiredJoules();
        int currentJoules = globalGrid.getCurrentJoules(); // Ini akan otomatis 0 jika over-capacity
        int gensCount = allPlayerGens.size();
        double speedMulti = plugin.getConfig().getDouble("events.speed-multiplier", 1.0);
        
        String gridStatus;
        Material gridMat;

        // PERBAIKAN: Deteksi status yang lebih realistis
        if (globalGrid.isOverCapacity()) {
            gridStatus = "&c&lOVERLOAD! &c(Mesin Mati)";
            gridMat = Material.REDSTONE_BLOCK;
        } else if (currentJoules <= 0) {
            gridStatus = "&c&lOFFLINE! &7(Bahan Bakar Kosong)";
            gridMat = Material.REDSTONE_BLOCK;
        } else {
            gridStatus = "&a&lONLINE! &7(Beroperasi Normal)";
            gridMat = Material.EMERALD_BLOCK;
        }

        int statusSlot = guiCfg.getInt("management.items.stat-status.slot", 47);
        if (statusSlot < inventory.getSize()) {
            String name = guiCfg.getString("management.items.stat-status.name", "&6STATUS GARDU INDUK");
            List<String> lore = new ArrayList<>();
            for (String line : guiCfg.getStringList("management.items.stat-status.lore")) {
                lore.add(line.replace("{grid_status}", gridStatus)
                             .replace("{current}", String.valueOf(currentJoules))
                             .replace("{max}", String.valueOf(maxJoule))
                             .replace("{required}", String.valueOf(totalRequired)));
            }
            inventory.setItem(statusSlot, createItem(gridMat, name, lore.toArray(new String[0])));
        }

        int assetSlot = guiCfg.getInt("management.items.stat-asset.slot", 48);
        if (assetSlot < inventory.getSize()) {
            String name = guiCfg.getString("management.items.stat-asset.name", "&6TOTAL ASSET MESIN");
            List<String> lore = new ArrayList<>();
            for (String line : guiCfg.getStringList("management.items.stat-asset.lore")) {
                lore.add(line.replace("{count}", String.valueOf(gensCount)));
            }
            inventory.setItem(assetSlot, createItem(Material.REDSTONE_LAMP, name, lore.toArray(new String[0])));
        }
        
        int eventSlot = guiCfg.getInt("management.items.stat-event.slot", 50);
        if (eventSlot < inventory.getSize()) {
            String name = guiCfg.getString("management.items.stat-event.name", "&6EVENT MULTIPLIER");
            List<String> lore = new ArrayList<>();
            for (String line : guiCfg.getStringList("management.items.stat-event.lore")) {
                lore.add(line.replace("{multiplier}", String.valueOf(speedMulti)));
            }
            inventory.setItem(eventSlot, createItem(Material.HOPPER, name, lore.toArray(new String[0])));
        }
        
        int limitSlot = guiCfg.getInt("management.items.stat-limit.slot", 51);
        if (limitSlot < inventory.getSize()) {
            String name = guiCfg.getString("management.items.stat-limit.name", "&6LIMIT KAPASITAS GARDU");
            List<String> lore = new ArrayList<>();
            for (String line : guiCfg.getStringList("management.items.stat-limit.lore")) {
                lore.add(line.replace("{max}", String.valueOf(maxJoule)));
            }
            inventory.setItem(limitSlot, createItem(Material.GOLD_BLOCK, name, lore.toArray(new String[0])));
        }
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
        if (material == null) material = Material.STONE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getConfigManager().colorize(name));
        List<String> loreList = new ArrayList<>();
        for (String line : lore) loreList.add(plugin.getConfigManager().colorize(line));
        meta.setLore(loreList);
        item.setItemMeta(meta);
        return item;
    }
}