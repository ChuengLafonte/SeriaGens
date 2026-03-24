package id.seria.gens.commands;

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

import id.seria.gens.SeriaGens;
import id.seria.gens.managers.SellManager;

public class SellCommand implements CommandExecutor, TabCompleter {
    
    private final SeriaGens plugin;
    private final DecimalFormat formatter = new DecimalFormat("#,###.##");
    
    public SellCommand(SeriaGens plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cOnly players can use this command!"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("seriagens.sell")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        
        if (plugin.getEconomy() == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("economy-disabled"));
            return true;
        }
        
        SellResult result = processSell(player);
        
        if (!result.isSuccess()) {
            player.sendMessage(plugin.getConfigManager().colorize("&cYou don't have any generator items to sell."));
            return true;
        }
        
        plugin.getEconomy().depositPlayer(player, result.totalValue);
        
        player.sendMessage("");
        player.sendMessage(plugin.getConfigManager().colorize("&#84fab0&l💰 PENJUALAN SUKSES!"));
        player.sendMessage(plugin.getConfigManager().colorize("  &7Item Terjual: &f" + result.itemsSold));
        player.sendMessage(plugin.getConfigManager().colorize("  &#D8A073&l✦ TOTAL: &a●" + formatter.format(result.totalValue)));
        
        double multiplier = plugin.getSellManager().getEventMultiplier();
        if (multiplier > 1.0) {
            player.sendMessage(plugin.getConfigManager().colorize("  &#f69220&l⚡ EVENT BONUS: &f" + multiplier + "x"));
        }
        player.sendMessage("");
        
        return true;
    }
    
    private SellResult processSell(Player player) {
        int totalItems = 0;
        double totalValue = 0.0;
        Map<String, Integer> itemCounts = new HashMap<>();
        SellManager sellManager = plugin.getSellManager();
        
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) continue;
            
            // Menyertakan player untuk ShopGUI+ check
            if (sellManager.isSellable(player, item)) {
                int amount = item.getAmount();
                double value = (sellManager.getSellValue(player, item) * amount);
                
                totalItems += amount;
                totalValue += value;
                player.getInventory().setItem(i, null);
            }
        }
        return new SellResult(totalItems, totalValue, itemCounts);
    }
    
    private static class SellResult {
        final int itemsSold;
        final double totalValue;
        
        SellResult(int items, double value, Map<String, Integer> counts) {
            this.itemsSold = items;
            this.totalValue = value;
        }
        boolean isSuccess() { return totalValue > 0; }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return new ArrayList<>();
    }
}