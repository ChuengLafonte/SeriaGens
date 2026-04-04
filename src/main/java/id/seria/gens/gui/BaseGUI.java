package id.seria.gens.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Base GUI class for all inventory-based GUIs
 */
public abstract class BaseGUI implements InventoryHolder, Listener {
    
    protected final Player player;
    protected final Inventory inventory;
    protected final Map<Integer, GUIAction> actions;
    
    // Track open GUIs
    private static final Map<UUID, BaseGUI> openGUIs = new HashMap<>();
    
    @SuppressWarnings("deprecation")
    public BaseGUI(Player player, String title, int size) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, size, title);
        this.actions = new HashMap<>();
    }
    
    /**
     * Initialize GUI contents
     */
    public abstract void init();
    
    /**
     * Open GUI for player
     */
    public void open() {
        init();
        player.openInventory(inventory);
        openGUIs.put(player.getUniqueId(), this);
    }
    
    /**
     * Set click action for a slot
     */
    protected void setAction(int slot, GUIAction action) {
        actions.put(slot, action);
    }
    
    /**
     * Get GUI for player
     */
    public static BaseGUI getGUI(Player player) {
        return openGUIs.get(player.getUniqueId());
    }
    
    /**
     * Handle inventory click
     */
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getInventory().getHolder() != this) return;
        
        event.setCancelled(true); // Cancel by default to prevent stealing items
        
        int slot = event.getRawSlot();
        GUIAction action = actions.get(slot);
        
        if (action != null) {
            action.execute((Player) event.getWhoClicked(), event);
        }
    }
    
    /**
     * Handle inventory close
     */
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() != this) return;
        openGUIs.remove(event.getPlayer().getUniqueId());
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Functional interface for GUI actions
     */
    @FunctionalInterface
    public interface GUIAction {
        void execute(Player player, InventoryClickEvent event);
    }
}