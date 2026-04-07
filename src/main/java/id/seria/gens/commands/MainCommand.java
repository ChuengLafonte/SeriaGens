package id.seria.gens.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import id.seria.gens.SeriaGens;
import id.seria.gens.models.Generator;
import id.seria.gens.models.SellwandData;

public class MainCommand implements CommandExecutor, TabCompleter {
    
    private final SeriaGens plugin;
    
    public MainCommand(SeriaGens plugin) {
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
                return handleSellwand(sender, args);
                
            case "restore":
                return handleRestore(sender);
                
            case "corrupt":
                return handleCorrupt(sender);
            
            case "pickup":
                return handlePickup(sender, args);

            case "view":
                return handleView(sender, args);

            case "addslot":
                return handleAddSlot(sender, args);
                
            default:
                sendHelp(sender);
                return true;
            
        }
    }
    
    // ============================================
    // FITUR BARU: ALAT DEBUG UNTUK MERUSAK GENERATOR
    // ============================================
    private boolean handleCorrupt(CommandSender sender) {
        if (!sender.hasPermission("seriagens.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cPerintah ini hanya bisa digunakan oleh pemain di dalam game!"));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Mendapatkan blok yang sedang dilihat pemain (maksimal jarak 5 blok)
        Block targetBlock = player.getTargetBlockExact(5);
        
        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            player.sendMessage(plugin.getConfigManager().colorize("&c&l⚠ &cKamu harus melihat tepat ke arah blok generator! (Jarak maks 5 blok)"));
            return true;
        }
        
        // Mengecek apakah blok tersebut terdaftar sebagai generator di database
        Generator gen = plugin.getGeneratorManager().getGenerator(targetBlock.getLocation());
        
        if (gen == null) {
            player.sendMessage(plugin.getConfigManager().colorize("&c&l⚠ &cBlok yang kamu lihat bukan generator milik SeriaGens!"));
            return true;
        }
        
        if (gen.isCorrupted()) {
            player.sendMessage(plugin.getConfigManager().colorize("&e&lℹ &eGenerator ini sudah dalam keadaan rusak!"));
            return true;
        }
        
        // Eksekusi perusakan paksa
        plugin.getCorruptionManager().corruptGenerator(gen);
        
        // Memaksa FancyHolograms untuk memunculkan teks peringatan
        plugin.getHologramIntegration().showCorruptionHologram(gen);
        
        player.sendMessage(plugin.getConfigManager().colorize("&a&l[!] &aGenerator berhasil dirusak secara paksa untuk keperluan testing!"));
        
        // Memberikan suara efek ledakan kecil/anvil agar terasa nyata saat dites
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 0.5f);
        
        return true;
    }
    
    private boolean handlePickup(CommandSender sender, String[] args) {
        if (!sender.hasPermission("seriagens.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cPenggunaan: /sgens pickup <pemain>"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
            return true;
        }
        
        // Cari semua generator milik pemain tersebut
        List<Generator> toRemove = new ArrayList<>();
        for (Generator gen : plugin.getGeneratorManager().getAllGenerators()) {
            if (gen.getOwner().equals(target.getUniqueId())) {
                toRemove.add(gen);
            }
        }
        
        if (toRemove.isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().colorize("&c" + target.getName() + " tidak memiliki generator di dunia ini."));
            return true;
        }
        
        int count = 0;
        for (Generator gen : toRemove) {
            plugin.getGeneratorManager().removeGenerator(gen.getLocation(), target);
            count++;
        }
        
        sender.sendMessage(plugin.getConfigManager().colorize("&aBerhasil mengambil (pickup) &e" + count + " &agenerator milik &e" + target.getName() + "&a. Generator telah masuk ke inventory mereka."));
        return true;
    }

    private boolean handleView(CommandSender sender, String[] args) {
        if (!sender.hasPermission("seriagens.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cPerintah ini butuh GUI, harus digunakan di dalam game!"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cPenggunaan: /sgens view <pemain>"));
            return true;
        }
        
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || !target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
            return true;
        }
        
        new id.seria.gens.gui.AdminViewGUI(plugin, (Player) sender, target).open();
        return true;
    }

    private boolean handleAddSlot(CommandSender sender, String[] args) {
        if (!sender.hasPermission("seriagens.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cPenggunaan: /sgens addslot <pemain> <jumlah>"));
            return true;
        }
        
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || !target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
            return true;
        }
        
        try {
            int amount = Integer.parseInt(args[2]);
            plugin.getGeneratorManager().addExtraSlots(target.getUniqueId(), amount);
            sender.sendMessage(plugin.getConfigManager().colorize("&aBerhasil menambahkan &e" + amount + " &aextra slot untuk &e" + target.getName() + "&a."));
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cJumlah harus berupa angka!"));
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("seriagens.reload")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        
        plugin.getConfigManager().loadConfigs();
        plugin.getSellManager().reload();
        plugin.getEventManager().reload();
        plugin.getHologramIntegration().reload();
        
        sender.sendMessage(plugin.getConfigManager().getMessage("plugin-reloaded"));
        return true;
    }
    
    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("seriagens.give")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cUsage: /sgens give <player> <type> [amount]"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
            return true;
        }
        
        String type = args[2];
        if (!plugin.getConfigManager().generatorExists(type)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("invalid-generator"));
            return true;
        }
        
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getConfigManager().colorize("&cAmount must be a number!"));
                return true;
            }
        }
        
        ItemStack item = plugin.getGeneratorManager().getGeneratorItem(type);
        item.setAmount(amount);
        
        target.getInventory().addItem(item);
        
        String msg = plugin.getConfigManager().getMessage("generator-given")
            .replace("{amount}", String.valueOf(amount))
            .replace("{player}", target.getName());
        sender.sendMessage(msg);
        
        return true;
    }
    
    private boolean handleSellwand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("seriagens.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cUsage: /sgens sellwand <player> [multiplier] [uses]"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
            return true;
        }
        
        double multiplier = 1.0;
        if (args.length >= 3) {
            try { multiplier = Double.parseDouble(args[2]); } 
            catch (NumberFormatException ignored) {}
        }
        
        int uses = -1;
        if (args.length >= 4) {
            try { uses = Integer.parseInt(args[3]); } 
            catch (NumberFormatException ignored) {}
        }
        
        ItemStack wand = SellwandData.createSellwand(plugin, multiplier, uses);
        target.getInventory().addItem(wand);
        
        sender.sendMessage(plugin.getConfigManager().colorize(
            "&aGave sell wand to " + target.getName() + " (" + multiplier + "x, " + 
            (uses == -1 ? "Unlimited" : uses) + " uses)"
        ));
        
        return true;
    }
    
    private boolean handleRestore(CommandSender sender) {
        if (!sender.hasPermission("seriagens.restore")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        
        sender.sendMessage(plugin.getConfigManager().colorize("&eStarting block restoration..."));
        int restored = plugin.getGeneratorManager().restoreAllBlocks();
        sender.sendMessage(plugin.getConfigManager().colorize("&aRestored " + restored + " generator blocks!"));
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        // Mengambil pesan bantuan dari command.yml
        List<String> helpList = sender.hasPermission("seriagens.admin") ? 
            plugin.getConfigManager().getCommandConfig().getStringList("messages.help-admin") : 
            plugin.getConfigManager().getCommandConfig().getStringList("messages.help-normal");
            
        for (String line : helpList) {
            sender.sendMessage(plugin.getConfigManager().colorize(line));
        }
        
        if (sender.hasPermission("seriagens.admin")) {
            sender.sendMessage(plugin.getConfigManager().colorize("&6/sgens corrupt &7- Merusak gen yang sedang dilihat (Debug)"));
            sender.sendMessage(plugin.getConfigManager().colorize("&6/sgens pickup <nama> &7- Ambil semua gen pemain secara paksa"));
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList("help", "reload", "give", "sellwand", "restore", "corrupt", "pickup","view", "addslot"));
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("sellwand") || args[0].equalsIgnoreCase("pickup") || args[0].equalsIgnoreCase("view") || args[0].equalsIgnoreCase("addslot"))) {
            for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            completions.addAll(plugin.getConfigManager().getAllGeneratorTypes());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("sellwand")) {
            completions.addAll(Arrays.asList("1.0", "1.5", "2.0", "2.5", "3.0"));
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("give")) {
                completions.addAll(Arrays.asList("1", "5", "10", "64"));
            } else if (args[0].equalsIgnoreCase("sellwand")) {
                completions.addAll(Arrays.asList("-1", "100", "500", "1000"));
            }
        }
        return completions;
    }
}