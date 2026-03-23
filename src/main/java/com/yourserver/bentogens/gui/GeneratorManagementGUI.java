package com.yourserver.bentogens.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.yourserver.bentogens.BentoGens;
import com.yourserver.bentogens.models.Generator;

/**
 * Main generator management GUI
 * Opens with /genset command
 * Shows all player's generators with upgrade/repair options
 */
public class GeneratorManagementGUI extends BaseGUI {
    
    private final BentoGens plugin;
    private int page;
    private List<Generator> playerGenerators;
    
    public GeneratorManagementGUI(BentoGens plugin, Player player) {
        this(plugin, player, 0);
    }
    
    public GeneratorManagementGUI(BentoGens plugin, Player player, int page) {
        super(player, plugin.getConfigManager().colorize("&6&lGenerator Management"), 54);
        this.plugin = plugin;
        this.page = page;
    }
    
    @Override
    public void init() {
        // Get player's generators
        playerGenerators = plugin.getGeneratorManager()
            .getAllGenerators()
            .stream()
            .filter(gen -> gen.getOwner().equals(player.getUniqueId()))
            .collect(Collectors.toList());
        
        if (playerGenerators.isEmpty()) {
            showEmptyMenu();
            return;
        }
        
        // Calculate pagination
        int itemsPerPage = 28;
        int totalPages = (int) Math.ceil((double) playerGenerators.size() / itemsPerPage);
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, playerGenerators.size());
        
        // Display generators
        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            Generator gen = playerGenerators.get(i);
            
            ItemStack item = createGeneratorItem(gen);
            inventory.setItem(slot, item);
            
            // Set click action
            final Generator clickedGen = gen;
            setAction(slot, (p, e) -> {
                if (e.isLeftClick()) {
                    // Teleport to generator
                    teleportToGenerator(clickedGen);
                } else if (e.isRightClick()) {
                    // Open upgrade/repair menu
                    openGeneratorMenu(clickedGen);
                }
            });
            
            // Move to next slot (skip border slots)
            slot++;
            if (slot == 17) slot = 19;
            if (slot == 26) slot = 28;
            if (slot == 35) slot = 37;
        }
        
        // Navigation buttons
        if (page > 0) {
            // Previous page button
            ItemStack prevPage = createItem(
                Material.ARROW,
                "&e&l← Previous Page",
                "&7Page " + page + " of " + totalPages
            );
            inventory.setItem(48, prevPage);
            setAction(48, (p, e) -> {
                new GeneratorManagementGUI(plugin, player, page - 1).open();
            });
        }
        
        if (page < totalPages - 1) {
            // Next page button
            ItemStack nextPage = createItem(
                Material.ARROW,
                "&e&lNext Page →",
                "&7Page " + (page + 2) + " of " + totalPages
            );
            inventory.setItem(50, nextPage);
            setAction(50, (p, e) -> {
                new GeneratorManagementGUI(plugin, player, page + 1).open();
            });
        }
        
        // Info button
        ItemStack info = createItem(
            Material.BOOK,
            "&6&lGenerator Info",
            "&7Total Generators: &e" + playerGenerators.size(),
            "&7Working: &a" + countWorking(),
            "&7Corrupted: &c" + countCorrupted(),
            "",
            "&e&lControls:",
            "&7Left Click: &eTeleport",
            "&7Right Click: &eUpgrade/Repair"
        );
        inventory.setItem(49, info);
        
        // Repair all button
        long corruptedCount = countCorrupted();
        if (corruptedCount > 0) {
            double totalRepairCost = calculateTotalRepairCost();
            
            ItemStack repairAll = createItem(
                Material.ANVIL,
                "&a&lRepair All Generators",
                "&7Corrupted: &c" + corruptedCount,
                "&7Total Cost: &a$" + String.format("%.2f", totalRepairCost),
                "",
                "&eClick to repair all!"
            );
            inventory.setItem(53, repairAll);
            setAction(53, (p, e) -> repairAllGenerators());
        }
        
        // Close button
        ItemStack close = createItem(Material.BARRIER, "&c&lClose", "&7Click to close");
        inventory.setItem(45, close);
        setAction(45, (p, e) -> close());
        
        // Fill borders
        fillBorders();
    }
    
    /**
     * Show empty menu when player has no generators
     */
    private void showEmptyMenu() {
        ItemStack noGens = createItem(
            Material.BARRIER,
            "&c&lNo Generators",
            "&7You don't have any generators yet!",
            "",
            "&7Get generators from &e/genshop",
            "&7or ask an admin with &e/bentogens give"
        );
        
        inventory.setItem(22, noGens);
        
        // Close button
        ItemStack close = createItem(Material.BARRIER, "&c&lClose");
        inventory.setItem(49, close);
        setAction(49, (p, e) -> close());
        
        fillBorders();
    }
    
    /**
     * Create generator item for display
     */
    private ItemStack createGeneratorItem(Generator gen) {
        ConfigurationSection genConfig = plugin.getConfigManager()
            .getGeneratorsConfig()
            .getConfigurationSection(gen.getType());
        
        Material material;
        String displayName;
        
        if (gen.isCorrupted()) {
            material = Material.REDSTONE_BLOCK;
            displayName = "&c&l⚠ CORRUPTED - " + gen.getType();
        } else {
            String materialName = genConfig != null ? 
                genConfig.getString("material", "STONE") : "STONE";
            material = Material.matchMaterial(materialName);
            if (material == null) material = Material.STONE;
            
            displayName = genConfig != null ?
                genConfig.getString("display-name", gen.getType()) : gen.getType();
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(plugin.getConfigManager().colorize(displayName));
        
        List<String> lore = new ArrayList<>();
        lore.add(plugin.getConfigManager().colorize("&7Type: &f" + gen.getType()));
        lore.add(plugin.getConfigManager().colorize("&7Status: " + 
            (gen.isCorrupted() ? "&cCORRUPTED" : "&aWORKING")));
        lore.add(plugin.getConfigManager().colorize("&7Location: &f" + 
            gen.getLocation().getBlockX() + ", " + 
            gen.getLocation().getBlockY() + ", " + 
            gen.getLocation().getBlockZ()));
        lore.add("");
        
        if (gen.isCorrupted()) {
            double repairCost = genConfig != null ?
                genConfig.getDouble("corruption.cost", 50.0) : 50.0;
            lore.add(plugin.getConfigManager().colorize("&c&l⚠ NEEDS REPAIR"));
            lore.add(plugin.getConfigManager().colorize("&7Repair Cost: &a$" + 
                String.format("%.2f", repairCost)));
            lore.add("");
        }
        
        lore.add(plugin.getConfigManager().colorize("&e&lActions:"));
        lore.add(plugin.getConfigManager().colorize("&7Left Click: &eTeleport"));
        lore.add(plugin.getConfigManager().colorize("&7Right Click: &eUpgrade/Repair"));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Teleport player to generator
     */
    private void teleportToGenerator(Generator gen) {
        player.teleport(gen.getLocation().clone().add(0.5, 1, 0.5));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        
        String msg = plugin.getConfigManager().colorize(
            "&6[BentoGens] &aTeleported to generator at &e" +
            gen.getLocation().getBlockX() + ", " +
            gen.getLocation().getBlockY() + ", " +
            gen.getLocation().getBlockZ()
        );
        player.sendMessage(msg);
        
        close();
    }
    
    /**
     * Open upgrade/repair menu for generator
     */
    private void openGeneratorMenu(Generator gen) {
        close();
        new UpgradeGUI(plugin, player, gen).open();
    }
    
    /**
     * Repair all corrupted generators
     */
    private void repairAllGenerators() {
        double totalCost = calculateTotalRepairCost();
        
        // Check economy
        if (plugin.getEconomy() == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("economy-disabled"));
            close();
            return;
        }
        
        // Check balance
        double balance = plugin.getEconomy().getBalance(player);
        if (balance < totalCost) {
            String msg = plugin.getConfigManager().colorize(
                "&6[BentoGens] &cInsufficient funds! Need: &e$" + 
                String.format("%.2f", totalCost) + " &cYou have: &e$" + 
                String.format("%.2f", balance)
            );
            player.sendMessage(msg);
            return;
        }
        
        // Withdraw money
        if (!plugin.getEconomy().withdrawPlayer(player, totalCost).transactionSuccess()) {
            player.sendMessage(plugin.getConfigManager().getMessage("transaction-failed"));
            return;
        }
        
        // Repair all corrupted generators
        int repaired = 0;
        for (Generator gen : playerGenerators) {
            if (gen.isCorrupted()) {
                gen.setCorrupted(false);
                plugin.getDatabaseManager().saveGenerator(gen);
                repaired++;
            }
        }
        
        // Play sound
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        
        // Send message
        String msg = plugin.getConfigManager().colorize(
            "&6[BentoGens] &aRepaired &e" + repaired + " &agenerators for &e$" + 
            String.format("%.2f", totalCost)
        );
        player.sendMessage(msg);
        
        // Refresh GUI
        init();
    }
    
    /**
     * Count working generators
     */
    private long countWorking() {
        return playerGenerators.stream().filter(gen -> !gen.isCorrupted()).count();
    }
    
    /**
     * Count corrupted generators
     */
    private long countCorrupted() {
        return playerGenerators.stream().filter(Generator::isCorrupted).count();
    }
    
    /**
     * Calculate total repair cost
     */
    private double calculateTotalRepairCost() {
        double total = 0.0;
        
        for (Generator gen : playerGenerators) {
            if (gen.isCorrupted()) {
                ConfigurationSection genConfig = plugin.getConfigManager()
                    .getGeneratorsConfig()
                    .getConfigurationSection(gen.getType());
                
                if (genConfig != null) {
                    total += genConfig.getDouble("corruption.cost", 50.0);
                }
            }
        }
        
        return total;
    }
    
    /**
     * Create item helper
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(plugin.getConfigManager().colorize(name));
        
        if (lore.length > 0) {
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(plugin.getConfigManager().colorize(line));
            }
            meta.setLore(loreList);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Fill borders with glass
     */
    private void fillBorders() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);
        
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, glass);
            inventory.setItem(45 + i, glass);
        }
        
        // Side columns
        for (int i = 1; i < 5; i++) {
            inventory.setItem(i * 9, glass);
            inventory.setItem(i * 9 + 8, glass);
        }
    }
}