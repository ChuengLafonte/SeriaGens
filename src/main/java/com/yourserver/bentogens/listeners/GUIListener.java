package com.yourserver.bentogens.listeners;

import com.yourserver.bentogens.gui.BaseGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * Global listener for all GUI events
 * Delegates to appropriate BaseGUI instances
 */
public class GUIListener implements Listener {
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if this inventory belongs to a BaseGUI
        if (event.getInventory().getHolder() instanceof BaseGUI) {
            BaseGUI gui = (BaseGUI) event.getInventory().getHolder();
            gui.onInventoryClick(event);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        // Check if this inventory belongs to a BaseGUI
        if (event.getInventory().getHolder() instanceof BaseGUI) {
            BaseGUI gui = (BaseGUI) event.getInventory().getHolder();
            gui.onInventoryClose(event);
        }
    }
}