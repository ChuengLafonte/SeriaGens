package id.seria.gens.listeners;

import java.util.Objects;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import id.seria.gens.SeriaGens;
import id.seria.gens.integration.BentoBoxIntegration;
import id.seria.gens.managers.RequirementsChecker.RequirementResult;
import id.seria.gens.managers.RequirementsChecker.RequirementType;

public class GeneratorListener implements Listener {
    
    private final SeriaGens plugin;
    public GeneratorListener(SeriaGens plugin) { this.plugin = plugin; }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGeneratorPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack item = event.getItemInHand();
        
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        
        String generatorType = null;
        for (String type : plugin.getConfigManager().getAllGeneratorTypes()) {
            ItemStack genItem = plugin.getGeneratorManager().getGeneratorItem(type);
            if (genItem.hasItemMeta() && Objects.equals(genItem.getItemMeta().displayName(), meta.displayName())) {
                generatorType = type;
                break;
            }
        }
        
        if (generatorType == null) return;
        if (!player.hasPermission("seriagens.place")) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        if (plugin.getBentoBox() != null) {
            if (!BentoBoxIntegration.isOnOwnIsland(player, block.getLocation(), plugin.getBentoBox())) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getMessage("generator-island-only"));
                return;
            }
        }
        
        RequirementResult reqResult = plugin.getRequirementsChecker().checkRequirements(player, generatorType, RequirementType.PLACE);
        if (!reqResult.hasPassed()) {
            event.setCancelled(true);
            reqResult.sendMessages(player);
            return;
        }
        
        boolean placed = plugin.getGeneratorManager().placeGenerator(player, block.getLocation(), generatorType);
        if (!placed) event.setCancelled(true);
    }
    
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGeneratorBreak(org.bukkit.event.block.BlockBreakEvent event) {
        Block block = event.getBlock();
        id.seria.gens.models.Generator gen = plugin.getGeneratorManager().getGenerator(block.getLocation());
        if (gen == null) return;
        
        if (gen.isCorrupted()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getConfigManager().getMessage("generator-corrupted-break"));
            return;
        }
        
        Player player = event.getPlayer();
        if (!player.hasPermission("seriagens.break") && !player.hasPermission("seriagens.admin")) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        if (!gen.getOwner().equals(player.getUniqueId()) && !player.hasPermission("seriagens.admin")) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("generator-not-yours"));
            return;
        }
        
        event.setDropItems(false);
        event.setExpToDrop(0);
        plugin.getGeneratorManager().removeGenerator(block.getLocation(), player);
    }

    @EventHandler
    public void onGeneratorInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK && event.getAction() != org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        
        id.seria.gens.models.Generator gen = plugin.getGeneratorManager().getGenerator(block.getLocation());
        if (gen == null) return;
        
        event.setCancelled(true); 
        Player player = event.getPlayer();
        
        if (!gen.getOwner().equals(player.getUniqueId()) && !player.hasPermission("seriagens.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("generator-not-yours"));
            return;
        }
        
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            new id.seria.gens.gui.UpgradeGUI(plugin, player, gen).open();
        } else if (event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) {
            if (!player.isSneaking()) return;
            
            if (gen.isCorrupted()) {
                player.sendMessage(plugin.getConfigManager().getMessage("generator-corrupted-interact"));
                return;
            }
            plugin.getGeneratorManager().removeGenerator(block.getLocation(), player);
        }
    }
}