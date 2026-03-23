package com.yourserver.bentogens.commands;

import com.yourserver.bentogens.BentoGens;
import com.yourserver.bentogens.managers.EventManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Event command for managing generator events
 * /event start <eventid>
 * /event stop
 * /event list
 */
public class EventCommand implements CommandExecutor, TabCompleter {
    
    private final BentoGens plugin;
    
    public EventCommand(BentoGens plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        if (!sender.hasPermission("bentogens.admin")) {
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
    
    /**
     * Handle /event start <eventid>
     */
    private boolean handleStart(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cUsage: /event start <eventid>"));
            sender.sendMessage(plugin.getConfigManager().colorize("&7Use /event list to see available events"));
            return true;
        }
        
        String eventId = args[1];
        EventManager eventManager = plugin.getEventManager();
        
        if (eventManager.hasActiveEvent()) {
            sender.sendMessage(plugin.getConfigManager().colorize("&c&l⚠ &fAn event is already active!"));
            sender.sendMessage(plugin.getConfigManager().colorize("  &7Current: &f" + eventManager.getActiveEvent().getDisplayName()));
            sender.sendMessage(plugin.getConfigManager().colorize("  &7Use &c/event stop &7first"));
            return true;
        }
        
        boolean started = eventManager.startEvent(eventId);
        
        if (started) {
            sender.sendMessage(plugin.getConfigManager().colorize("&#84fab0&l✦ &fStarted event: &b" + eventId));
        } else {
            sender.sendMessage(plugin.getConfigManager().colorize("&c&l⚠ &fEvent not found: &c" + eventId));
            sender.sendMessage(plugin.getConfigManager().colorize("  &7Use &e/event list &7to see available events"));
        }
        
        return true;
    }
    
    /**
     * Handle /event stop
     */
    private boolean handleStop(CommandSender sender) {
        EventManager eventManager = plugin.getEventManager();
        
        if (!eventManager.hasActiveEvent()) {
            sender.sendMessage(plugin.getConfigManager().colorize("&c&l⚠ &fNo event is currently active!"));
            return true;
        }
        
        String eventName = eventManager.getActiveEvent().getDisplayName();
        eventManager.stopEvent();
        
        sender.sendMessage(plugin.getConfigManager().colorize("&#84fab0&l✦ &fStopped event: " + eventName));
        
        return true;
    }
    
    /**
     * Handle /event list
     */
    private boolean handleList(CommandSender sender) {
        EventManager eventManager = plugin.getEventManager();
        
        sender.sendMessage("");
        sender.sendMessage(plugin.getConfigManager().colorize("&#84fab0&l⚡ AVAILABLE EVENTS"));
        sender.sendMessage("");
        
        for (EventManager.GeneratorEvent event : eventManager.getEvents().values()) {
            String status = eventManager.getActiveEvent() == event ? " &a(ACTIVE)" : "";
            
            sender.sendMessage(plugin.getConfigManager().colorize(
                "  &7• &f" + event.getId() + status
            ));
            sender.sendMessage(plugin.getConfigManager().colorize(
                "    " + event.getDisplayName()
            ));
            sender.sendMessage(plugin.getConfigManager().colorize(
                "    &7Type: &b" + event.getType() + " &7| Duration: &b" + event.getDuration() + "s"
            ));
            
            // Show event-specific info
            if (event instanceof EventManager.SellMultiplierEvent) {
                EventManager.SellMultiplierEvent sellEvent = (EventManager.SellMultiplierEvent) event;
                sender.sendMessage(plugin.getConfigManager().colorize(
                    "    &7Multiplier: &e" + sellEvent.getMultiplier() + "x"
                ));
            } else if (event instanceof EventManager.DropMultiplierEvent) {
                EventManager.DropMultiplierEvent dropEvent = (EventManager.DropMultiplierEvent) event;
                sender.sendMessage(plugin.getConfigManager().colorize(
                    "    &7Multiplier: &e" + dropEvent.getMultiplier() + "x"
                ));
            } else if (event instanceof EventManager.GeneratorSpeedEvent) {
                EventManager.GeneratorSpeedEvent speedEvent = (EventManager.GeneratorSpeedEvent) event;
                sender.sendMessage(plugin.getConfigManager().colorize(
                    "    &7Speed: &e" + speedEvent.getSpeedReduction() + "% faster"
                ));
            } else if (event instanceof EventManager.GeneratorUpgradeEvent) {
                EventManager.GeneratorUpgradeEvent upgradeEvent = (EventManager.GeneratorUpgradeEvent) event;
                sender.sendMessage(plugin.getConfigManager().colorize(
                    "    &7Tier Boost: &e+" + upgradeEvent.getTierBoost()
                ));
            }
            
            sender.sendMessage("");
        }
        
        sender.sendMessage(plugin.getConfigManager().colorize("&7Total: &f" + eventManager.getEvents().size() + " events"));
        sender.sendMessage("");
        
        return true;
    }
    
    /**
     * Handle /event info
     */
    private boolean handleInfo(CommandSender sender) {
        EventManager eventManager = plugin.getEventManager();
        
        sender.sendMessage("");
        sender.sendMessage(plugin.getConfigManager().colorize("&#84fab0&l⚡ EVENT SYSTEM INFO"));
        sender.sendMessage("");
        
        sender.sendMessage(plugin.getConfigManager().colorize(
            "  &7Status: " + (eventManager.hasActiveEvent() ? "&aACTIVE" : "&7Inactive")
        ));
        
        if (eventManager.hasActiveEvent()) {
            EventManager.GeneratorEvent event = eventManager.getActiveEvent();
            sender.sendMessage(plugin.getConfigManager().colorize(
                "  &7Current Event: " + event.getDisplayName()
            ));
            sender.sendMessage(plugin.getConfigManager().colorize(
                "  &7Type: &b" + event.getType()
            ));
        }
        
        sender.sendMessage(plugin.getConfigManager().colorize(
            "  &7Total Events: &f" + eventManager.getEvents().size()
        ));
        
        sender.sendMessage("");
        
        return true;
    }
    
    /**
     * Send help message
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(plugin.getConfigManager().colorize("&#84fab0&l⚡ EVENT COMMANDS"));
        sender.sendMessage("");
        sender.sendMessage(plugin.getConfigManager().colorize("  &7/event start <eventid> &f- Start an event"));
        sender.sendMessage(plugin.getConfigManager().colorize("  &7/event stop &f- Stop active event"));
        sender.sendMessage(plugin.getConfigManager().colorize("  &7/event list &f- List all events"));
        sender.sendMessage(plugin.getConfigManager().colorize("  &7/event info &f- Show current event"));
        sender.sendMessage("");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("start", "stop", "list", "info"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            // Event IDs
            completions.addAll(plugin.getEventManager().getEvents().keySet());
        }
        
        return completions;
    }
}