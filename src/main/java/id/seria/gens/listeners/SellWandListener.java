package id.seria.gens.listeners;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import id.seria.gens.SeriaGens;
import id.seria.gens.managers.SellManager;
import id.seria.gens.models.SellwandData;

public class SellWandListener implements Listener {
    
    private final SeriaGens plugin;
    private final DecimalFormat formatter = new DecimalFormat("#,###.##");
    
    public SellWandListener(SeriaGens plugin) { this.plugin = plugin; }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onWandUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Block block = event.getClickedBlock();
        
        if (item == null || block == null) return;
        if (!SellwandData.isSellwand(plugin, item)) return;
        event.setCancelled(true);
        
        if (!player.hasPermission("seriagens.wand")) {
            player.sendMessage(plugin.getConfigManager().getMessage("sellwand-no-permission"));
            return;
        }
        if (!(block.getState() instanceof Container)) {
            player.sendMessage(plugin.getConfigManager().getMessage("sellwand-not-container"));
            return;
        }
        
        SellwandData wandData = SellwandData.fromItem(plugin, item);
        if (wandData == null || wandData.hasBroken()) {
            player.sendMessage(plugin.getConfigManager().getMessage("sellwand-broken"));
            return;
        }
        
        Container container = (Container) block.getState();
        Inventory inventory = container.getInventory();
        SellResult result = processSell(player, inventory, wandData.getMultiplier());
        
        if (!result.isSuccess()) {
            player.sendMessage(plugin.getConfigManager().getMessage("sellwand-no-items"));
            return;
        }
        
        wandData.decreaseUses();
        wandData.updateItem(plugin);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        
        String msg = plugin.getConfigManager().getMessage("sellwand-sell-success")
            .replace("{items}", String.valueOf(result.itemsSold))
            .replace("{value}", formatter.format(result.totalValue))
            .replace("{multiplier}", String.valueOf(wandData.getMultiplier()));
        player.sendMessage(msg);
        
        if (wandData.hasBroken()) {
            player.getInventory().setItemInMainHand(null);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            player.sendMessage(plugin.getConfigManager().getMessage("sellwand-broke-notify"));
        }
    }
    
    private SellResult processSell(Player player, Inventory inventory, double multiplier) {
        int totalItems = 0; double totalValue = 0.0;
        SellManager sellManager = plugin.getSellManager();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack content = inventory.getItem(i);
            if (content == null || content.getType() == Material.AIR) continue;
            if (sellManager.isSellable(player, content)) {
                int amount = content.getAmount();
                double value = (sellManager.getSellValue(player, content) * amount) * multiplier;
                totalItems += amount; totalValue += value;
                inventory.setItem(i, null);
            }
        }
        if (totalValue > 0 && plugin.getEconomy() != null) plugin.getEconomy().depositPlayer(player, totalValue);
        return new SellResult(totalItems, totalValue, new HashMap<>());
    }
    
    private static class SellResult {
        final int itemsSold; final double totalValue;
        SellResult(int itemsSold, double totalValue, Map<String, Integer> itemCounts) {
            this.itemsSold = itemsSold; this.totalValue = totalValue;
        }
        boolean isSuccess() { return totalValue > 0; }
    }
}