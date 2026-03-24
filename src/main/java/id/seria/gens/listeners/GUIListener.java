package id.seria.gens.listeners;

import id.seria.gens.gui.BaseGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class GUIListener implements Listener {
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        if (event.getInventory().getHolder() instanceof BaseGUI) {
            BaseGUI gui = (BaseGUI) event.getInventory().getHolder();
            gui.onInventoryClick(event);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        if (event.getInventory().getHolder() instanceof BaseGUI) {
            BaseGUI gui = (BaseGUI) event.getInventory().getHolder();
            gui.onInventoryClose(event);
        }
    }
}