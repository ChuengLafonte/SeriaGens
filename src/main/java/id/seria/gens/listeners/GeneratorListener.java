package id.seria.gens.listeners;

import id.seria.gens.SeriaGens;
import id.seria.gens.gui.UpgradeGUI;
import id.seria.gens.models.Generator;
import id.seria.gens.managers.RequirementsChecker.RequirementResult;
import id.seria.gens.managers.RequirementsChecker.RequirementType;
import id.seria.gens.integration.BentoBoxIntegration;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.UUID;

public class GeneratorListener implements Listener {
    
    private final SeriaGens plugin;
    
    public GeneratorListener(SeriaGens plugin) {
        this.plugin = plugin;
    }
    
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
            if (genItem.hasItemMeta() && 
                genItem.getItemMeta().getDisplayName().equals(meta.getDisplayName())) {
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
        
        // BentoBox check
        if (plugin.getBentoBox() != null) {
            if (!BentoBoxIntegration.isOnOwnIsland(player, block.getLocation(), plugin.getBentoBox())) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().colorize("&cYou can only place generators on your own island!"));
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
        if (!placed) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGeneratorBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Generator gen = plugin.getGeneratorManager().getGenerator(block.getLocation());
        
        if (gen == null) return;
        
        Player player = event.getPlayer();
        
        if (!player.hasPermission("seriagens.break") && !player.hasPermission("seriagens.admin")) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        if (!gen.getOwner().equals(player.getUniqueId()) && !player.hasPermission("seriagens.admin")) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().colorize("&cThis is not your generator!"));
            return;
        }
        
        event.setDropItems(false);
        event.setExpToDrop(0);
        
        plugin.getGeneratorManager().removeGenerator(block.getLocation(), player);
    }
    
    @EventHandler
    public void onGeneratorInteract(PlayerInteractEvent event) {
        // Hanya proses Klik Kanan (Upgrade/Repair) dan Klik Kiri (Pickup)
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        
        Block block = event.getClickedBlock();
        if (block == null) return;
        
        Generator gen = plugin.getGeneratorManager().getGenerator(block.getLocation());
        if (gen == null) return;
        
        event.setCancelled(true); // Membatalkan animasi hancur blok default Vanilla
        Player player = event.getPlayer();
        
        if (!gen.getOwner().equals(player.getUniqueId()) && !player.hasPermission("seriagens.admin")) {
            player.sendMessage(plugin.getConfigManager().colorize("&cIni bukan generator milikmu!"));
            return;
        }
        
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // KLIK KANAN: Buka Menu Upgrade/Repair
            new UpgradeGUI(plugin, player, gen).open();
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // KLIK KIRI: 1-Tap Pickup Instan
            plugin.getGeneratorManager().removeGenerator(block.getLocation(), player);
        }
    }
}