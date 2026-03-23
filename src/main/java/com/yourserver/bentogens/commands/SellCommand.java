package com.yourserver.bentogens.commands;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.yourserver.bentogens.BentoGens;
import com.yourserver.bentogens.managers.SellManager;

/**
 * Handles /gensell command ONLY
 * No /sell command to avoid conflict with ShopGUIPlus!
 */
public class SellCommand implements CommandExecutor, TabCompleter {
    
    private final BentoGens plugin;
    private final DecimalFormat formatter = new DecimalFormat("#,###.##");
    
    public SellCommand(BentoGens plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cOnly players can use this command!"));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("bentogens.sell")) {
            player.sendMessage(plugin.getConfigManager().colorize("&cNo permission!"));
            return true;
        }
        
        SellManager sellManager = plugin.getSellManager();
        
        if (sellManager == null) {
            player.sendMessage(plugin.getConfigManager().colorize("&cSell system not available!"));
            return true;
        }
        
        // Sell generator items from inventory
        SellResult result = sellInventoryItems(player, sellManager);
        
        // Send result message
        if (result.error != null) {
            player.sendMessage(plugin.getConfigManager().colorize("&c" + result.error));
            return true;
        }
        
        if (!result.isSuccess()) {
            player.sendMessage(plugin.getConfigManager().colorize("&cNo generator items to sell!"));
            return true;
        }
        
        // Success message
        sendSellMessage(player, result);
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Return list kosong supaya tidak error, atau isi dengan saran command
        return new ArrayList<>();
    }    
    /**
     * Sell all generator items from player inventory
     */
    private SellResult sellInventoryItems(Player player, SellManager sellManager) {
        int totalItems = 0;
        double totalValue = 0.0;
        Map<String, Integer> itemCounts = new HashMap<>();
        
        ItemStack[] contents = player.getInventory().getContents();
        
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            
            // Check if sellable (generator item)
            if (!sellManager.isSellable(item)) {
                continue;
            }
            
            int amount = item.getAmount();
            double value = sellManager.getSellValue(item) * amount;
            
            if (value <= 0) {
                continue;
            }
            
            totalItems += amount;
            totalValue += value;
            
            // Track for message
            String displayName = getItemDisplayName(item);
            itemCounts.put(displayName, itemCounts.getOrDefault(displayName, 0) + amount);
            
            // Remove item
            player.getInventory().setItem(i, null);
        }
        
        if (totalValue > 0 && plugin.getEconomy() != null) {
            plugin.getEconomy().depositPlayer(player, totalValue);
        }
        
        return new SellResult(totalItems, totalValue, itemCounts);
    }
    
    /**
     * Get item display name
     */
    private String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name();
    }
    
    /**
     * Send formatted sell message
     */
    private void sendSellMessage(Player player, SellResult result) {
        String prefix = plugin.getConfigManager().colorize("&#84fab0&l⚡ SELL");
        
        // Header
        player.sendMessage("");
        player.sendMessage(plugin.getConfigManager().colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(prefix);
        player.sendMessage("");
        
        // Items sold
        if (result.itemCounts.size() <= 3) {
            // Show individual items
            for (Map.Entry<String, Integer> entry : result.itemCounts.entrySet()) {
                player.sendMessage(plugin.getConfigManager().colorize(
                    "  &7• " + entry.getKey() + " &fx" + entry.getValue()
                ));
            }
        } else {
            // Show summary
            player.sendMessage(plugin.getConfigManager().colorize(
                "  &7• &f" + result.itemCounts.size() + " different items"
            ));
        }
        
        player.sendMessage("");
        
        // Total
        player.sendMessage(plugin.getConfigManager().colorize(
            "  &#D8A073&l✦ TOTAL: &a●" + formatter.format(result.totalValue)
        ));
        
        // Multiplier info
        double multiplier = plugin.getSellManager().getEventMultiplier();
        if (multiplier > 1.0) {
            player.sendMessage(plugin.getConfigManager().colorize(
                "  &#f69220&l⚡ EVENT BONUS: &f" + multiplier + "x"
            ));
        }
        
        player.sendMessage("");
        player.sendMessage(plugin.getConfigManager().colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage("");
    }
    
    /**
     * Sell result class
     */
    private static class SellResult {
        final int itemsSold;
        final double totalValue;
        final Map<String, Integer> itemCounts;
        final String error;
        
        SellResult(int items, double value, Map<String, Integer> counts) {
            this.itemsSold = items;
            this.totalValue = value;
            this.itemCounts = counts;
            this.error = null;
        }
        
        SellResult(int items, double value, String error) {
            this.itemsSold = items;
            this.totalValue = value;
            this.itemCounts = new HashMap<>();
            this.error = error;
        }
        
        boolean isSuccess() {
            return error == null && totalValue > 0;
        }
    }
}