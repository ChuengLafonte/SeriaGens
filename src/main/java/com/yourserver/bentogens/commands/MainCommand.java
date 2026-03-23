package com.yourserver.bentogens.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.yourserver.bentogens.BentoGens;
import com.yourserver.bentogens.models.SellwandData;

/**
 * Main /bentogens command with FULL sellwand support
 */
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
        
        String subCmd = args[0].toLowerCase();
        
        switch (subCmd) {
            case "help":
                sendHelp(sender);
                return true;
                
            case "reload":
                return handleReload(sender);
                
            case "give":
                return handleGive(sender, args);
                
            case "sellwand":
            case "wand":
                return handleSellwand(sender, args);
                
            case "restore":
                return handleRestore(sender);
                
            case "info":
                return handleInfo(sender);
                
            default:
                sender.sendMessage(plugin.getConfigManager().colorize("&cUnknown command! Use /bentogens help"));
                return true;
        }
    }
    
    /**
     * Handle /bentogens give <player> <type> [amount]
     */
    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bentogens.admin")) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cNo permission!"));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cUsage: /bentogens give <player> <generator> [amount]"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cPlayer not found!"));
            return true;
        }
        
        String type = args[2].toLowerCase();
        int amount = 1;
        
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getConfigManager().colorize("&cInvalid amount!"));
                return true;
            }
        }
        
        // Check if generator type exists
        if (!plugin.getConfigManager().getGeneratorsConfig().contains(type)) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cInvalid generator type!"));
            return true;
        }
        
        // Give generator
        for (int i = 0; i < amount; i++) {
            ItemStack item = plugin.getGeneratorManager().getGeneratorItem(type);
            target.getInventory().addItem(item);
        }
        
        String displayName = plugin.getConfigManager().getGeneratorsConfig().getString(type + ".display-name", type);
        
        sender.sendMessage(plugin.getConfigManager().colorize(
            "&#84fab0&l✦ &fGave &e" + amount + "x " + displayName + " &fto &b" + target.getName()
        ));
        
        target.sendMessage(plugin.getConfigManager().colorize(
            "&#84fab0&l✦ &fYou received &e" + amount + "x " + displayName
        ));
        
        return true;
    }
    
    /**
     * Handle /bentogens sellwand <player> <multiplier> <uses>
     */
    private boolean handleSellwand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bentogens.admin")) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cNo permission!"));
            return true;
        }
        
        if (args.length < 4) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cUsage: /bentogens sellwand <player> <multiplier> <uses>"));
            sender.sendMessage(plugin.getConfigManager().colorize("&7Example: /bentogens sellwand Notch 2.0 100"));
            sender.sendMessage(plugin.getConfigManager().colorize("&7Unlimited uses: /bentogens sellwand Notch 1.5 -1"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cPlayer not found!"));
            return true;
        }
        
        double multiplier;
        int uses;
        
        try {
            multiplier = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cInvalid multiplier! Use numbers like 1.0, 1.5, 2.0"));
            return true;
        }
        
        try {
            uses = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cInvalid uses! Use -1 for unlimited or any positive number"));
            return true;
        }
        
        // Validate input
        if (multiplier <= 0) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cMultiplier must be greater than 0!"));
            return true;
        }
        
        if (uses < -1 || uses == 0) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cUses must be -1 (unlimited) or positive number!"));
            return true;
        }
        
        // Create sellwand
        ItemStack wand = SellwandData.createSellwand(plugin, multiplier, uses);
        target.getInventory().addItem(wand);
        
        String usesDisplay = uses == -1 ? "Unlimited" : uses + " uses";
        
        sender.sendMessage(plugin.getConfigManager().colorize(
            "&#84fab0&l✦ &fGave sellwand &7(" + multiplier + "x, " + usesDisplay + ") &fto &b" + target.getName()
        ));
        
        target.sendMessage(plugin.getConfigManager().colorize(
            "&#84fab0&l✦ &fYou received sellwand &7(" + multiplier + "x, " + usesDisplay + ")"
        ));
        
        return true;
    }
    
    /**
     * Handle /bentogens reload
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("bentogens.admin")) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cNo permission!"));
            return true;
        }
        
        plugin.reloadConfig();
        plugin.getConfigManager().loadConfigs();
        
        if (plugin.getSellManager() != null) {
            plugin.getSellManager().reload();
        }
        
        sender.sendMessage(plugin.getConfigManager().colorize("&#84fab0&l✦ &fBentoGens reloaded!"));
        return true;
    }
    
    /**
     * Handle /bentogens restore
     */
    private boolean handleRestore(CommandSender sender) {
        if (!sender.hasPermission("bentogens.admin")) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cNo permission!"));
            return true;
        }
        
        int restored = plugin.getGeneratorManager().restoreAllBlocks();
        
        sender.sendMessage(plugin.getConfigManager().colorize(
            "&#84fab0&l✦ &fRestored &e" + restored + " &fgenerator blocks"
        ));
        
        return true;
    }
    
    /**
     * Handle /bentogens info
     */
    private boolean handleInfo(CommandSender sender) {
        int total = plugin.getGeneratorManager().getAllGenerators().size();
        
        sender.sendMessage("");
        sender.sendMessage(plugin.getConfigManager().colorize("&#84fab0&l⚡ BENTOGENS INFO"));
        sender.sendMessage(plugin.getConfigManager().colorize("  &7Version: &f" + plugin.getDescription().getVersion()));
        sender.sendMessage(plugin.getConfigManager().colorize("  &7Generators: &f" + total));
        sender.sendMessage(plugin.getConfigManager().colorize("  &7Economy: &f" + 
            (plugin.getEconomy() != null ? "&aEnabled" : "&cDisabled")));
        sender.sendMessage(plugin.getConfigManager().colorize("  &7BentoBox: &f" + 
            (plugin.getBentoBox() != null ? "&aEnabled" : "&cDisabled")));
        sender.sendMessage("");
        
        return true;
    }
    
    /**
     * Send help message
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(plugin.getConfigManager().colorize("&#84fab0&l⚡ BENTOGENS COMMANDS"));
        sender.sendMessage("");
        
        if (sender.hasPermission("bentogens.admin")) {
            sender.sendMessage(plugin.getConfigManager().colorize("  &7/bentogens give <player> <type> [amount]"));
            sender.sendMessage(plugin.getConfigManager().colorize("  &7/bentogens sellwand <player> <multiplier> <uses>"));
            sender.sendMessage(plugin.getConfigManager().colorize("  &7/bentogens reload"));
            sender.sendMessage(plugin.getConfigManager().colorize("  &7/bentogens restore"));
        }
        
        sender.sendMessage(plugin.getConfigManager().colorize("  &7/bentogens info"));
        sender.sendMessage(plugin.getConfigManager().colorize("  &7/genshop"));
        sender.sendMessage(plugin.getConfigManager().colorize("  &7/genset"));
        sender.sendMessage(plugin.getConfigManager().colorize("  &7/gensell &8- Sell generator items"));
        sender.sendMessage("");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("help", "info", "reload", "give", "sellwand", "restore"));
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("sellwand"))) {
            // Online players
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            // Generator types
            completions.addAll(plugin.getConfigManager().getAllGeneratorTypes());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("sellwand")) {
            // Multiplier suggestions
            completions.addAll(Arrays.asList("1.0", "1.5", "2.0", "2.5", "3.0"));
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("give")) {
                // Amount suggestions
                completions.addAll(Arrays.asList("1", "5", "10", "64"));
            } else if (args[0].equalsIgnoreCase("sellwand")) {
                // Uses suggestions
                completions.addAll(Arrays.asList("-1", "10", "50", "100", "500"));
            }
        }
        
        return completions;
    }
}