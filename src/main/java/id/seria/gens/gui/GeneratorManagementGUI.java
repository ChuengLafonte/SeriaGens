package id.seria.gens.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import id.seria.gens.SeriaGens;
import id.seria.gens.managers.FuelManager;
import id.seria.gens.managers.RequirementsChecker.RequirementResult;
import id.seria.gens.managers.RequirementsChecker.RequirementType;
import id.seria.gens.models.Generator;
import id.seria.gens.models.PlayerGlobalGrid;

public class GeneratorManagementGUI extends BaseGUI {
    
    private final SeriaGens plugin;
    private final PlayerGlobalGrid globalGrid;
    
    private int currentPage = 0;
    private List<Generator> allPlayerGens;
    private final List<Integer> genSlots;
    
    private int refreshTaskId = -1;
    
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
    
    @Override
    public void open() {
        super.open();
        startRefreshTask();
    }
    
    private void startRefreshTask() {
        if (refreshTaskId != -1) return;
        refreshTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (player.getOpenInventory().getTopInventory().equals(inventory)) {
                this.allPlayerGens = plugin.getGeneratorManager().getAllGenerators().stream()
                        .filter(g -> g.getOwner().equals(player.getUniqueId()))
                        .collect(Collectors.toList());

                globalGrid.calculateGridStatus(plugin);
                FileConfiguration guiCfg = plugin.getConfigManager().getGuiConfig();
                
                renderGeneratorPage(guiCfg);
                renderGlobalGarduSlots(guiCfg);
                renderStatistics(guiCfg);
            } else {
                stopRefreshTask();
            }
        }, 20L, 20L);
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
        actions.clear();
        FileConfiguration guiCfg = plugin.getConfigManager().getGuiConfig();
        
        renderPriorityConfigItems(guiCfg);
        renderGeneratorPage(guiCfg);
        renderGlobalGarduSlots(guiCfg);
        renderStatistics(guiCfg);
    }

    @Override
    public void onInventoryClose(InventoryCloseEvent event) {
        super.onInventoryClose(event);
        stopRefreshTask();
        
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

    @SuppressWarnings("deprecation")
    private void renderPriorityConfigItems(FileConfiguration guiCfg) {
        ConfigurationSection itemsSec = guiCfg.getConfigurationSection("management.items");
        if (itemsSec == null) return;

        List<ConfigItem> cItems = new ArrayList<>();
        
        for (String key : itemsSec.getKeys(false)) {
            if (key.startsWith("stat-")) continue; 
            
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
                else if (ci.key.equalsIgnoreCase("next-btn") || ci.key.equalsIgnoreCase("next-page")) {
                    if (currentPage < totalPages - 1) {
                        ItemStack displayItem = ci.itemStack.clone();
                        ItemMeta meta = displayItem.getItemMeta();
                        meta.setDisplayName(meta.getDisplayName().replace("{page}", String.valueOf(currentPage + 2)));
                        if (meta.hasLore()) {
                            List<String> newLore = new ArrayList<>();
                            for (String l : meta.getLore()) newLore.add(l.replace("{page}", String.valueOf(currentPage + 2)));
                            meta.setLore(newLore);
                        }
                        displayItem.setItemMeta(meta);
                        inventory.setItem(slot, displayItem);
                        setAction(slot, (p, e) -> { e.setCancelled(true); currentPage++; showManagementMenu(); });
                    } else {
                        inventory.setItem(slot, getFallbackFiller(guiCfg));
                    }
                } 
                else if (ci.key.equalsIgnoreCase("back-btn") || ci.key.equalsIgnoreCase("prev-page")) {
                    if (currentPage > 0) {
                        ItemStack displayItem = ci.itemStack.clone();
                        ItemMeta meta = displayItem.getItemMeta();
                        meta.setDisplayName(meta.getDisplayName().replace("{page}", String.valueOf(currentPage)));
                        if (meta.hasLore()) {
                            List<String> newLore = new ArrayList<>();
                            for (String l : meta.getLore()) newLore.add(l.replace("{page}", String.valueOf(currentPage)));
                            meta.setLore(newLore);
                        }
                        displayItem.setItemMeta(meta);
                        inventory.setItem(slot, displayItem);
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
        
        while (slotIndex < genSlots.size()) {
            int conceptualSlot = genSlots.get(slotIndex);
            if (conceptualSlot < inventory.getSize()) {
                inventory.setItem(conceptualSlot, new ItemStack(Material.AIR));
                actions.remove(conceptualSlot);
            }
            slotIndex++;
        }
    }

    @SuppressWarnings("deprecation")
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
                    inventory.setItem(conceptualSlot, createItem(Material.YELLOW_STAINED_GLASS_PANE, "&e&lSLOT FUEL " + (fuelIndex + 1), "&7Drag & Drop Fuel Kesini!"));
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
                            ItemStack newFuel = cursor.clone();
                            if (isFiller) {
                                p.setItemOnCursor(null);
                            } else {
                                p.setItemOnCursor(rawFuelItem != null ? rawFuelItem.clone() : null); 
                            }
                            globalGrid.setFuel(fuelIndex, newFuel);
                            globalGrid.calculateGridStatus(plugin);
                            renderGlobalGarduSlots(guiCfg);
                            renderStatistics(guiCfg);
                        } else {
                            p.sendMessage(plugin.getConfigManager().colorize("&cItu bukan bahan bakar yang valid!"));
                        }
                    } else if (!isFiller && rawFuelItem != null && rawFuelItem.getType() != Material.AIR) {
                        p.setItemOnCursor(rawFuelItem.clone());
                        globalGrid.setFuel(fuelIndex, null);
                        globalGrid.calculateGridStatus(plugin);
                        renderGlobalGarduSlots(guiCfg);
                        renderStatistics(guiCfg);
                    }
                });
            }
        }
    }
    
    @SuppressWarnings("deprecation")
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
        int currentJoules = globalGrid.getTotalDisplayedJoules(plugin); 
        int gensCount = allPlayerGens.size();
        
        long corruptedCount = allPlayerGens.stream().filter(Generator::isCorrupted).count();
        double repairCost = 0.0;
        for (Generator gen : allPlayerGens) {
            if (gen.isCorrupted()) {
                repairCost += plugin.getConfigManager().getGeneratorsConfig().getDouble(gen.getType() + ".corrupted.cost", 50.0);
            }
        }
        int extraSlots = plugin.getConfigManager().getPlayersConfig().getInt(player.getUniqueId().toString() + ".extra-slots", 0);

        String gridStateKey;
        if (globalGrid.isOverCapacity()) {
            gridStateKey = "stat-grid-overload";
        } else if (currentJoules <= 0) {
            gridStateKey = "stat-grid-offline";
        } else {
            gridStateKey = "stat-grid-online";
        }

        int statusSlot = guiCfg.getInt("management.items." + gridStateKey + ".slot", 47);
        if (statusSlot < inventory.getSize()) {
            String name = guiCfg.getString("management.items." + gridStateKey + ".name", "&6STATUS GARDU");
            Material gridMat = Material.matchMaterial(guiCfg.getString("management.items." + gridStateKey + ".material", "BEDROCK"));
            if (gridMat == null) gridMat = Material.BEDROCK;
            
            List<String> lore = new ArrayList<>();
            for (String line : guiCfg.getStringList("management.items." + gridStateKey + ".lore")) {
                lore.add(line.replace("{current}", String.valueOf(currentJoules))
                             .replace("{max}", String.valueOf(maxJoule))
                             .replace("{required}", String.valueOf(totalRequired)));
            }
            inventory.setItem(statusSlot, createItem(gridMat, name, lore.toArray(new String[0])));
        }

        int assetSlot = guiCfg.getInt("management.items.stat-asset.slot", 48);
        if (assetSlot < inventory.getSize()) {
            String name = guiCfg.getString("management.items.stat-asset.name", "&6TOTAL ASSET MESIN");
            Material m = Material.matchMaterial(guiCfg.getString("management.items.stat-asset.material", "COMPARATOR"));
            List<String> lore = new ArrayList<>();
            for (String line : guiCfg.getStringList("management.items.stat-asset.lore")) {
                lore.add(line.replace("{count}", String.valueOf(gensCount))
                             .replace("{max_gens}", String.valueOf(plugin.getGeneratorManager().getMaxGenerators(player)))
                             .replace("{corrupted_count}", String.valueOf(corruptedCount))
                             .replace("{repair_cost}", String.valueOf(repairCost)));
            }
            inventory.setItem(assetSlot, createItem(m != null ? m : Material.COMPARATOR, name, lore.toArray(new String[0])));
        }
        
        // PERBAIKAN: Lore Event yang Dinamis Tanpa Hardcode {multiplier}
        int eventSlot = guiCfg.getInt("management.items.stat-event.slot", 50);
        if (eventSlot < inventory.getSize()) {
            String name = guiCfg.getString("management.items.stat-event.name", "&6EVENT MULTIPLIER");
            Material m = Material.matchMaterial(guiCfg.getString("management.items.stat-event.material", "BEACON"));
            
            String activeEventTitle = plugin.getEventManager().hasActiveEvent() 
                ? plugin.getEventManager().getActiveEvent().getDisplayName() 
                : "&7Tidak Ada Event Aktif";
            
            List<String> lore = new ArrayList<>();
            for (String line : guiCfg.getStringList("management.items.stat-event.lore")) {
                if (line.contains("{multiplier}")) {
                    if (!plugin.getEventManager().hasActiveEvent()) {
                        lore.add(plugin.getConfigManager().colorize("&7Tidak ada bonus aktif."));
                    } else {
                        String eType = plugin.getEventManager().getActiveEvent().getType();
                        if (eType.equals("drop_multiplier")) {
                            lore.add(plugin.getConfigManager().colorize("&7Bonus Drop: &ex" + plugin.getGeneratorManager().getDropMultiplier() + " Item"));
                        } else if (eType.equals("generator_speed")) {
                            lore.add(plugin.getConfigManager().colorize("&7Bonus Speed: &e+" + plugin.getGeneratorManager().getSpeedReduction() + "% Lebih Cepat"));
                        } else if (eType.equals("sell_multiplier")) {
                            lore.add(plugin.getConfigManager().colorize("&7Bonus Jual: &ex" + plugin.getSellManager().getEventMultiplier() + " Harga"));
                        } else if (eType.equals("generator_upgrade")) {
                            lore.add(plugin.getConfigManager().colorize("&7Bonus Tier: &e+" + plugin.getGeneratorManager().getTierBoost() + " Level Mesin"));
                        } else if (eType.equals("mixed_up")) {
                            lore.add(plugin.getConfigManager().colorize("&7Bonus Item: &eAcak dari semua jenis!"));
                        }
                    }
                } else {
                    lore.add(plugin.getConfigManager().colorize(line.replace("{active_event}", activeEventTitle)));
                }
            }
            inventory.setItem(eventSlot, createItem(m != null ? m : Material.BEACON, name, lore.toArray(new String[0])));
        }
        
        int limitSlot = guiCfg.getInt("management.items.stat-limit.slot", 51);
        if (limitSlot < inventory.getSize()) {
            String name = guiCfg.getString("management.items.stat-limit.name", "&6LIMIT KAPASITAS GARDU");
            Material m = Material.matchMaterial(guiCfg.getString("management.items.stat-limit.material", "EXPERIENCE_BOTTLE"));
            List<String> lore = new ArrayList<>();
            for (String line : guiCfg.getStringList("management.items.stat-limit.lore")) {
                lore.add(line.replace("{max}", String.valueOf(maxJoule))
                             .replace("{extra_slots}", String.valueOf(extraSlots)));
            }
            inventory.setItem(limitSlot, createItem(m != null ? m : Material.EXPERIENCE_BOTTLE, name, lore.toArray(new String[0])));
        }
    }
    
    @SuppressWarnings("deprecation")
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