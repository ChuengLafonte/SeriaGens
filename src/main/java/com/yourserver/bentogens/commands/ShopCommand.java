package com.yourserver.bentogens.commands;

import com.yourserver.bentogens.BentoGens;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {
    
    private final BentoGens plugin;
    
    public ShopCommand(BentoGens plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        // Must be a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cThis command can only be used by players!"));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("bentogens.shop")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        
        // TODO: Open shop GUI
        // For now, just show available generators
        player.sendMessage(plugin.getConfigManager().colorize("&6&m                                          "));
        player.sendMessage(plugin.getConfigManager().colorize("&e&lGenerator Shop"));
        player.sendMessage(plugin.getConfigManager().colorize("&6&m                                          "));
        player.sendMessage(plugin.getConfigManager().colorize("&cShop GUI coming soon!"));
        player.sendMessage(plugin.getConfigManager().colorize("&eAvailable generators:"));
        
        for (String type : plugin.getConfigManager().getAllGeneratorTypes()) {
            String displayName = plugin.getConfigManager()
                .getGeneratorsConfig()
                .getString(type + ".display-name", type);
            
            player.sendMessage(plugin.getConfigManager().colorize("&7- " + displayName));
        }
        
        player.sendMessage(plugin.getConfigManager().colorize("&eUse &6/bentogens give <player> <type> &eto get generators for now!"));
        player.sendMessage(plugin.getConfigManager().colorize("&6&m                                          "));
        
        return true;
    }
}