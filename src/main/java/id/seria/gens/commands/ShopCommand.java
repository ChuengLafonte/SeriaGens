package id.seria.gens.commands;

import id.seria.gens.SeriaGens;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {
    
    private final SeriaGens plugin;
    
    public ShopCommand(SeriaGens plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cThis command can only be used by players!"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("seriagens.shop")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        
        player.sendMessage(plugin.getConfigManager().colorize("&6&m                                          "));
        player.sendMessage(plugin.getConfigManager().colorize("&e&lSeriaGens Shop"));
        player.sendMessage(plugin.getConfigManager().colorize("&6&m                                          "));
        player.sendMessage(plugin.getConfigManager().colorize("&cMenu Shop Interaktif akan segera hadir!"));
        player.sendMessage(plugin.getConfigManager().colorize("&eGenerator yang tersedia di server:"));
        
        for (String type : plugin.getConfigManager().getAllGeneratorTypes()) {
            String displayName = plugin.getConfigManager()
                .getGeneratorsConfig()
                .getString(type + ".display-name", type);
            
            player.sendMessage(plugin.getConfigManager().colorize("&7- " + displayName));
        }
        
        player.sendMessage(plugin.getConfigManager().colorize("&6&m                                          "));
        
        return true;
    }
}