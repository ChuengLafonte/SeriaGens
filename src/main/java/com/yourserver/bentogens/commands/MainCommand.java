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
import java.util.stream.Collectors;

public class MainCommand implements CommandExecutor, TabCompleter {
    
    private final BentoGens plugin;
    
    public MainCommand(BentoGens plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        // /bentogens (no args) - show help
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
                sendHelp(sender);
                return true;
                
            case "reload":
                return handleReload(sender);
                
            case "give":
                return handleGive(sender, args);
                
            case "info":
                return handleInfo(sender);
                
            case "restore":
                return handleRestore(sender);
                
            default:
                sender.sendMessage(plugin.getConfigManager().colorize("&cUnknown command! Use /bentogens help"));
                return true;
        }
    }
    
    /**
     * Send help message
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().colorize("&6&m                                          "));
        sender.sendMessage(plugin.getConfigManager().colorize("&e&lBentoGens Commands"));
        sender.sendMessage(plugin.getConfigManager().colorize("&6&m                                          "));
        sender.sendMessage(plugin.getConfigManager().colorize("&e/bentogens help &7- Show this help"));
        sender.sendMessage(plugin.getConfigManager().colorize("&e/bentogens info &7- Show plugin info"));
        
        if (sender.hasPermission("bentogens.admin")) {
            sender.sendMessage(plugin.getConfigManager().colorize("&e/bentogens reload &7- Reload configuration"));
            sender.sendMessage(plugin.getConfigManager().colorize("&e/bentogens give <player> <type> [amount] &7- Give generator"));
            sender.sendMessage(plugin.getConfigManager().colorize("&e/bentogens restore &7- Restore all generator blocks"));
        }
        
        sender.sendMessage(plugin.getConfigManager().colorize("&6&m                                          "));
        return;
    }
    
    /**
     * Handle reload command
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("bentogens.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        
        sender.sendMessage(plugin.getConfigManager().colorize("&6[BentoGens] &eReloading configuration..."));
        
        // Reload configs
        plugin.getConfigManager().reloadConfigs();
        
        sender.sendMessage(plugin.getConfigManager().getMessage("reload-success"));
        return true;
    }
    
    /**
     * Handle give command
     * /bentogens give <player> <type> [amount]
     */
    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bentogens.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        
        // Check args
        if (args.length < 3) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cUsage: /bentogens give <player> <type> [amount]"));
            return true;
        }
        
        // Get player
        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);
        
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cPlayer not found: " + playerName));
            return true;
        }
        
        // Get generator type
        String type = args[2];
        
        if (!plugin.getConfigManager().generatorExists(type)) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cInvalid generator type: " + type));
            sender.sendMessage(plugin.getConfigManager().colorize("&cAvailable types: " + 
                String.join(", ", plugin.getConfigManager().getAllGeneratorTypes())));
            return true;
        }
        
        // Get amount
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount <= 0) {
                    amount = 1;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getConfigManager().colorize("&cInvalid amount: " + args[3]));
                return true;
            }
        }
        
        // Give generator item
        ItemStack item = plugin.getGeneratorManager().getGeneratorItem(type);
        item.setAmount(amount);
        
        target.getInventory().addItem(item);
        
        // Messages
        sender.sendMessage(plugin.getConfigManager().colorize(
            "&6[BentoGens] &aGave &e" + amount + "x " + type + " &ato &e" + target.getName()));
        
        target.sendMessage(plugin.getConfigManager().colorize(
            "&6[BentoGens] &aYou received &e" + amount + "x " + type + " &afrom &e" + sender.getName()));
        
        return true;
    }
    
    /**
     * Handle info command
     */
    private boolean handleInfo(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().colorize("&6&m                                          "));
        sender.sendMessage(plugin.getConfigManager().colorize("&e&lBentoGens Info"));
        sender.sendMessage(plugin.getConfigManager().colorize("&6&m                                          "));
        sender.sendMessage(plugin.getConfigManager().colorize("&eVersion: &f" + plugin.getDescription().getVersion()));
        sender.sendMessage(plugin.getConfigManager().colorize("&eAuthor: &f" + plugin.getDescription().getAuthors().get(0)));
        sender.sendMessage(plugin.getConfigManager().colorize("&eTotal Generators: &f" + 
            plugin.getGeneratorManager().getAllGenerators().size()));
        
        // Database info
        String dbType = plugin.getConfig().getString("database.type", "SQLITE");
        sender.sendMessage(plugin.getConfigManager().colorize("&eDatabase: &f" + dbType));
        
        // BentoBox integration
        boolean bentoBoxEnabled = plugin.getBentoBox() != null;
        sender.sendMessage(plugin.getConfigManager().colorize("&eBentoBox Integration: &f" + 
            (bentoBoxEnabled ? "&aEnabled" : "&cDisabled")));
        
        if (sender instanceof Player) {
            Player player = (Player) sender;
            int playerGens = plugin.getGeneratorManager().getPlayerGeneratorCount(player.getUniqueId());
            int maxGens = plugin.getGeneratorManager().getMaxGenerators(player);
            
            sender.sendMessage(plugin.getConfigManager().colorize("&eYour Generators: &f" + 
                playerGens + "/" + (maxGens == Integer.MAX_VALUE ? "Unlimited" : maxGens)));
        }
        
        sender.sendMessage(plugin.getConfigManager().colorize("&6&m                                          "));
        return true;
    }
    
    /**
     * Handle restore command - manually force restore all blocks
     */
    private boolean handleRestore(CommandSender sender) {
        if (!sender.hasPermission("bentogens.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        
        sender.sendMessage(plugin.getConfigManager().colorize("&6[BentoGens] &eRestoring all generator blocks..."));
        
        int restored = plugin.getGeneratorManager().restoreAllBlocks();
        
        sender.sendMessage(plugin.getConfigManager().colorize(
            "&6[BentoGens] &aRestored &e" + restored + " &agenerator blocks!"));
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            completions.add("help");
            completions.add("info");
            
            if (sender.hasPermission("bentogens.admin")) {
                completions.add("reload");
                completions.add("give");
                completions.add("restore");
            }
            
            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            // Second argument for give - player names
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            // Third argument for give - generator types
            return plugin.getConfigManager().getAllGeneratorTypes().stream()
                .filter(type -> type.toLowerCase().startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            // Fourth argument for give - amount
            return Arrays.asList("1", "5", "10", "32", "64");
        }
        
        return completions;
    }
}