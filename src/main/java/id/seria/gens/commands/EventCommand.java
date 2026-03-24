package id.seria.gens.commands;

import id.seria.gens.SeriaGens;
import id.seria.gens.managers.EventManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventCommand implements CommandExecutor, TabCompleter {
    
    private final SeriaGens plugin;
    
    public EventCommand(SeriaGens plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("seriagens.admin")) {
            sender.sendMessage(plugin.getConfigManager().colorize("&c&l⚠ &fNo permission!"));
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCmd = args[0].toLowerCase();
        
        switch (subCmd) {
            case "start":
                return handleStart(sender, args);
            case "stop":
                return handleStop(sender);
            case "list":
                return handleList(sender);
            case "info":
                return handleInfo(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private boolean handleStart(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cUsage: /event start <eventid>"));
            return true;
        }
        
        String eventId = args[1];
        if (plugin.getEventManager().startEvent(eventId)) {
            sender.sendMessage(plugin.getConfigManager().colorize("&aStarted event: " + eventId));
        } else {
            sender.sendMessage(plugin.getConfigManager().colorize("&cFailed to start event. Either it doesn't exist or another event is active."));
        }
        return true;
    }
    
    private boolean handleStop(CommandSender sender) {
        if (!plugin.getEventManager().hasActiveEvent()) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cNo active event to stop!"));
            return true;
        }
        
        plugin.getEventManager().stopEvent();
        sender.sendMessage(plugin.getConfigManager().colorize("&aEvent stopped manually."));
        return true;
    }
    
    private boolean handleInfo(CommandSender sender) {
        EventManager eventManager = plugin.getEventManager();
        if (!eventManager.hasActiveEvent()) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cNo active event right now."));
            return true;
        }
        
        EventManager.GeneratorEvent active = eventManager.getActiveEvent();
        sender.sendMessage(plugin.getConfigManager().colorize("&aActive Event: &f" + active.getDisplayName()));
        sender.sendMessage(plugin.getConfigManager().colorize("&aType: &f" + active.getType()));
        return true;
    }
    
    private boolean handleList(CommandSender sender) {
        EventManager eventManager = plugin.getEventManager();
        sender.sendMessage(plugin.getConfigManager().colorize("&aAvailable Events:"));
        for (String id : eventManager.getEvents().keySet()) {
            sender.sendMessage(plugin.getConfigManager().colorize("&7- &f" + id));
        }
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().colorize("&a/event start <eventid> &7- Start an event"));
        sender.sendMessage(plugin.getConfigManager().colorize("&a/event stop &7- Stop active event"));
        sender.sendMessage(plugin.getConfigManager().colorize("&a/event list &7- List all events"));
        sender.sendMessage(plugin.getConfigManager().colorize("&a/event info &7- Show current event info"));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList("start", "stop", "list", "info"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            completions.addAll(plugin.getEventManager().getEvents().keySet());
        }
        return completions;
    }
}