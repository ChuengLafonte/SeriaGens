package com.yourserver.bentogens.commands;

import com.yourserver.bentogens.BentoGens;
import com.yourserver.bentogens.gui.GeneratorManagementGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * /genset command - Opens Generator Management GUI
 */
public class GensetCommand implements CommandExecutor, TabCompleter {
    
    private final BentoGens plugin;
    
    public GensetCommand(BentoGens plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Must be a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().colorize(
                "&6[BentoGens] &cThis command can only be used by players!"
            ));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("bentogens.genset")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        
        // Open GUI
        new GeneratorManagementGUI(plugin, player).open();
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>(); // No tab completion needed
    }
}