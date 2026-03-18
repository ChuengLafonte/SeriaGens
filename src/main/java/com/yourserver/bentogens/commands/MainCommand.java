package com.yourserver.bentogens.commands;

import com.yourserver.bentogens.BentoGens;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainCommand implements CommandExecutor, TabCompleter {
    
    private final BentoGens plugin;
    
    public MainCommand(BentoGens plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "help":
                sendHelp(sender);
                break;
            
            case "info":
                sendInfo(sender);
                break;
            
            case "reload":
                if (!sender.hasPermission("bentogens.admin")) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                
                plugin.getConfigManager().loadConfigs();
                plugin.reloadConfig();
                sender.sendMessage(plugin.getConfigManager().getMessage("reload-success"));
                break;
            
            case "give":
                if (!sender.hasPermission("bentogens.admin")) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                
                if (args.length < 3) {
                    sender.sendMessage(plugin.getConfigManager().colorize(
                        "&6[BentoGens] &cUsage: /bentogens give <player> <type> [amount]"
                    ));
                    return true;
                }
                
                giveGenerator(sender, args);
                break;
            
            case "restore":
                if (!sender.hasPermission("bentogens.admin")) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                
                restoreBlocks(sender);
                break;
            
            default:
                sendHelp(sender);
                break;
        }
        
        return true;
    }
    
    /**
     * Send help message
     */
    private void sendHelp(CommandSender sender) {
        if (sender.hasPermission("bentogens.admin")) {
            List<String> helpLines = plugin.getConfig().getStringList("messages.help-admin");
            for (String line : helpLines) {
                sender.sendMessage(plugin.getConfigManager().colorize(line));
            }
        } else {
            List<String> helpLines = plugin.getConfig().getStringList("messages.help-normal");
            for (String line : helpLines) {
                sender.sendMessage(plugin.getConfigManager().colorize(line));
            }
        }
    }
    
    /**
     * Send plugin info
     */
    private void sendInfo(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().colorize("&6&m-----------------&r &6&lBentoGens&r &6&m-----------------"));
        sender.sendMessage(plugin.getConfigManager().colorize("&eVersion: &f" + plugin.getDescription().getVersion()));
        sender.sendMessage(plugin.getConfigManager().colorize("&eAuthor: &f" + plugin.getDescription().getAuthors().get(0)));
        sender.sendMessage(plugin.getConfigManager().colorize("&eTotal Generators: &f" + 
            plugin.getGeneratorManager().getAllGenerators().size()));
        sender.sendMessage(plugin.getConfigManager().colorize("&eEconomy: &f" + 
            (plugin.getEconomy() != null ? "Enabled" : "Disabled")));
        sender.sendMessage(plugin.getConfigManager().colorize("&eBentoBox: &f" + 
            (plugin.getBentoBox() != null ? "Enabled" : "Disabled")));
        sender.sendMessage(plugin.getConfigManager().colorize("&eCorruption: &f" + 
            (plugin.getConfig().getBoolean("corruption.enabled") ? "Enabled" : "Disabled")));
        sender.sendMessage(plugin.getConfigManager().colorize("&6&m----------------------------------------"));
    }
    
    /**
     * Give generator to player
     */
    private void giveGenerator(CommandSender sender, String[] args) {
        String playerName = args[1];
        String type = args[2];
        int amount = args.length >= 4 ? Integer.parseInt(args[3]) : 1;
        
        Player target = Bukkit.getPlayer(playerName);
        
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().colorize(
                "&6[BentoGens] &cPlayer not found: " + playerName
            ));
            return;
        }
        
        // Check if generator type exists
        if (!plugin.getConfigManager().generatorExists(type)) {
            sender.sendMessage(plugin.getConfigManager().colorize(
                "&6[BentoGens] &cInvalid generator type: " + type
            ));
            sender.sendMessage(plugin.getConfigManager().colorize(
                "&7Available types: " + String.join(", ", plugin.getConfigManager().getAllGeneratorTypes())
            ));
            return;
        }
        
        // Give generator item
        ItemStack item = plugin.getGeneratorManager().getGeneratorItem(type);
        item.setAmount(amount);
        
        target.getInventory().addItem(item);
        
        // Messages
        String giveMsg = plugin.getConfigManager().getMessage("give-gen")
            .replace("{amount}", String.valueOf(amount))
            .replace("{type}", type)
            .replace("{player}", target.getName());
        sender.sendMessage(giveMsg);
        
        String receiveMsg = plugin.getConfigManager().getMessage("receive-gen")
            .replace("{amount}", String.valueOf(amount))
            .replace("{type}", type);
        target.sendMessage(receiveMsg);
    }
    
    /**
     * Restore all generator blocks
     */
    private void restoreBlocks(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().colorize(
            "&6[BentoGens] &eRestoring generator blocks..."
        ));
        
        int restored = plugin.getGeneratorManager().restoreAllBlocks();
        
        sender.sendMessage(plugin.getConfigManager().colorize(
            "&6[BentoGens] &aRestored &e" + restored + " &agenerator blocks!"
        ));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Main commands
            completions.addAll(Arrays.asList("help", "info", "reload", "give", "restore"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            // Player names
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            // Generator types
            completions.addAll(plugin.getConfigManager().getAllGeneratorTypes());
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            // Amount suggestions
            completions.addAll(Arrays.asList("1", "5", "10", "64"));
        }
        
        return completions;
    }
}