package com.yourserver.bentogens.gui;

import com.yourserver.bentogens.BentoGens;
import com.yourserver.bentogens.models.Generator;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for upgrading generators
 * Opens when player right-clicks a generator
 */
public class UpgradeGUI extends BaseGUI {
    
    private final BentoGens plugin;
    private final Generator generator;
    
    public UpgradeGUI(BentoGens plugin, Player player, Generator generator) {
        super(player, plugin.getConfigManager().colorize("&6Generator Upgrade"), 27);
        this.plugin = plugin;
        this.generator = generator;
    }
    
    @Override
    public void init() {
        // Check if generator is corrupted
        if (generator.isCorrupted()) {
            showRepairMenu();
        } else {
            showUpgradeMenu();
        }
    }
    
    /**
     * Show upgrade menu
     */
    private void showUpgradeMenu() {
        ConfigurationSection genConfig = plugin.getConfigManager()
            .getGeneratorsConfig()
            .getConfigurationSection(generator.getType());
        
        if (genConfig == null) {
            close();
            return;
        }
        
        // Get upgrade info
        String nextTier = genConfig.getString("upgrade.next-tier", "none");
        boolean canUpgrade = genConfig.getBoolean("upgrade.enabled", false);
        
        if (!canUpgrade || "none".equals(nextTier)) {
            showMaxTierMenu();
            return;
        }
        
        double upgradeCost = genConfig.getDouble("upgrade.cost", 0);
        
        // Fill borders with glass
        fillBorders(Material.GRAY_STAINED_GLASS_PANE);
        
        // Show current generator info (slot 13)
        ItemStack currentGen = createGeneratorInfoItem(generator.getType(), false);
        inventory.setItem(13, currentGen);
        
        // Check if player has enough money
        double balance = plugin.getEconomy() != null ? 
            plugin.getEconomy().getBalance(player) : 0.0;
        
        boolean canAfford = balance >= upgradeCost;
        
        // Confirm buttons (green glass)
        if (canAfford) {
            ItemStack confirm = createItem(
                Material.LIME_STAINED_GLASS_PANE,
                "&a&lCONFIRM UPGRADE",
                "&7Cost: &a$" + String.format("%.2f", upgradeCost),
                "&7Balance: &a$" + String.format("%.2f", balance),
                "",
                "&aClick to upgrade!"
            );
            
            for (int slot : new int[]{4, 5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26}) {
                inventory.setItem(slot, confirm);
                setAction(slot, (p, e) -> upgradeGenerator(nextTier, upgradeCost));
            }
        } else {
            ItemStack cannotAfford = createItem(
                Material.RED_STAINED_GLASS_PANE,
                "&c&lCANNOT AFFORD",
                "&7Cost: &c$" + String.format("%.2f", upgradeCost),
                "&7Balance: &c$" + String.format("%.2f", balance),
                "",
                "&cYou need $" + String.format("%.2f", (upgradeCost - balance)) + " more!"
            );
            
            for (int slot : new int[]{4, 5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26}) {
                inventory.setItem(slot, cannotAfford);
            }
        }
        
        // Cancel buttons (red glass)
        ItemStack cancel = createItem(
            Material.RED_STAINED_GLASS_PANE,
            "&c&lCANCEL",
            "&7Click to close"
        );
        
        for (int slot : new int[]{0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 22}) {
            inventory.setItem(slot, cancel);
            setAction(slot, (p, e) -> close());
        }
    }
    
    /**
     * Show repair menu for corrupted generators
     */
    private void showRepairMenu() {
        ConfigurationSection genConfig = plugin.getConfigManager()
            .getGeneratorsConfig()
            .getConfigurationSection(generator.getType());
        
        if (genConfig == null) {
            close();
            return;
        }
        
        double repairCost = genConfig.getDouble("corruption.cost", 50.0);
        
        // Fill borders
        fillBorders(Material.RED_STAINED_GLASS_PANE);
        
        // Show broken generator (slot 13)
        ItemStack brokenGen = createItem(
            Material.BARRIER,
            "&c&lBROKEN GENERATOR",
            "&7Type: &f" + generator.getType(),
            "&7Status: &cCORRUPTED",
            "",
            "&7Repair Cost: &a$" + String.format("%.2f", repairCost),
            "",
            "&eClick to repair!"
        );
        
        inventory.setItem(13, brokenGen);
        
        // Check balance
        double balance = plugin.getEconomy() != null ? 
            plugin.getEconomy().getBalance(player) : 0.0;
        
        boolean canAfford = balance >= repairCost;
        
        // Repair button
        if (canAfford) {
            ItemStack repair = createItem(
                Material.LIME_STAINED_GLASS_PANE,
                "&a&lREPAIR GENERATOR",
                "&7Cost: &a$" + String.format("%.2f", repairCost),
                "",
                "&aClick to repair!"
            );
            
            for (int slot : new int[]{4, 5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26}) {
                inventory.setItem(slot, repair);
                setAction(slot, (p, e) -> repairGenerator(repairCost));
            }
        } else {
            ItemStack cannotRepair = createItem(
                Material.RED_STAINED_GLASS_PANE,
                "&c&lCANNOT AFFORD",
                "&7Cost: &c$" + String.format("%.2f", repairCost),
                "&7Balance: &c$" + String.format("%.2f", balance),
                "",
                "&cYou need $" + String.format("%.2f", (repairCost - balance)) + " more!"
            );
            
            for (int slot : new int[]{4, 5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26}) {
                inventory.setItem(slot, cannotRepair);
            }
        }
        
        // Cancel button
        ItemStack cancel = createItem(Material.BARRIER, "&c&lCANCEL", "&7Click to close");
        for (int slot : new int[]{0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 22}) {
            inventory.setItem(slot, cancel);
            setAction(slot, (p, e) -> close());
        }
    }
    
    /**
     * Show max tier menu (cannot upgrade further)
     */
    private void showMaxTierMenu() {
        fillBorders(Material.YELLOW_STAINED_GLASS_PANE);
        
        ItemStack maxTier = createItem(
            Material.DIAMOND,
            "&e&lMAX TIER REACHED",
            "&7This generator is already at maximum tier!",
            "",
            "&7Type: &f" + generator.getType(),
            "",
            "&aNo further upgrades available"
        );
        
        inventory.setItem(13, maxTier);
        
        // Close button
        ItemStack close = createItem(Material.BARRIER, "&c&lCLOSE", "&7Click to close");
        for (int slot = 0; slot < 27; slot++) {
            if (slot != 13) {
                inventory.setItem(slot, close);
                setAction(slot, (p, e) -> close());
            }
        }
    }
    
    /**
     * Upgrade generator to next tier
     */
    private void upgradeGenerator(String nextTier, double cost) {
        // Check economy
        if (plugin.getEconomy() == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("economy-disabled"));
            close();
            return;
        }
        
        // Withdraw money
        if (plugin.getEconomy().withdrawPlayer(player, cost).transactionSuccess()) {
            // Change generator type
            generator.setType(nextTier);
            
            // Update block material
            String materialName = plugin.getConfigManager().getGeneratorMaterial(nextTier);
            Material material = Material.matchMaterial(materialName);
            if (material != null) {
                generator.getLocation().getBlock().setType(material);
            }
            
            // Save to database
            plugin.getDatabaseManager().saveGenerator(generator);
            
            // Play sound
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            
            // Send message
            String msg = plugin.getConfigManager().getMessage("generator-upgraded")
                .replace("{type}", nextTier)
                .replace("{cost}", String.format("%.2f", cost));
            player.sendMessage(msg);
            
            close();
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("insufficient-funds"));
            close();
        }
    }
    
    /**
     * Repair corrupted generator
     */
    private void repairGenerator(double cost) {
        // Check economy
        if (plugin.getEconomy() == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("economy-disabled"));
            close();
            return;
        }
        
        // Withdraw money
        if (plugin.getEconomy().withdrawPlayer(player, cost).transactionSuccess()) {
            // Fix corruption
            generator.setCorrupted(false);
            
            // Save to database
            plugin.getDatabaseManager().saveGenerator(generator);
            
            // Play sound
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
            
            // Send message
            String msg = plugin.getConfigManager().getMessage("generator-repaired")
                .replace("{cost}", String.format("%.2f", cost));
            player.sendMessage(msg);
            
            close();
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("insufficient-funds"));
            close();
        }
    }
    
    /**
     * Create generator info item
     */
    private ItemStack createGeneratorInfoItem(String type, boolean next) {
        ConfigurationSection genConfig = plugin.getConfigManager()
            .getGeneratorsConfig()
            .getConfigurationSection(type);
        
        if (genConfig == null) {
            return new ItemStack(Material.STONE);
        }
        
        String materialName = genConfig.getString("item.material", "STONE");
        Material material = Material.matchMaterial(materialName);
        
        if (material == null) material = Material.STONE;
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String displayName = genConfig.getString("display-name", type);
        meta.setDisplayName(plugin.getConfigManager().colorize(
            (next ? "&a&lNEXT: " : "&e&lCURRENT: ") + displayName
        ));
        
        List<String> lore = new ArrayList<>();
        lore.add(plugin.getConfigManager().colorize("&7Type: &f" + type));
        lore.add(plugin.getConfigManager().colorize("&7Interval: &f" + genConfig.getInt("interval", 20) + "s"));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Create custom item
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(plugin.getConfigManager().colorize(name));
        
        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(plugin.getConfigManager().colorize(line));
        }
        meta.setLore(loreList);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Fill borders with glass pane
     */
    private void fillBorders(Material material) {
        ItemStack glass = new ItemStack(material);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);
        
        // Don't fill slot 13 (center)
        for (int i = 0; i < 27; i++) {
            if (i != 13) {
                inventory.setItem(i, glass);
            }
        }
    }
}