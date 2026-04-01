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
        
        // Cek apakah menu yang sedang dibuka adalah Custom GUI buatan kita
        if (event.getInventory().getHolder() instanceof BaseGUI) {
            BaseGUI gui = (BaseGUI) event.getInventory().getHolder();
            
            // 1. CEK JIKA KLIK INVENTORY MILIK PLAYER SENDIRI (BOTTOM INVENTORY)
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getBottomInventory())) {
                if (event.isShiftClick()) {
                    event.setCancelled(true); // Cegah Shift-Click agar item tidak "melompat" sembarangan merusak GUI
                    return;
                }
                event.setCancelled(false); // IZINKAN pemain berinteraksi dengan tas mereka sendiri!
                return; // Berhenti di sini, tidak perlu meneruskan ke logika menu atas
            }
            
            // 2. JIKA KLIK MENU ATAS, TERUSKAN LOGIKA KE BaseGUI
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