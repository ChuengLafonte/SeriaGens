package id.seria.gens.models;

import id.seria.gens.SeriaGens;
import id.seria.gens.managers.FuelManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PlayerGlobalGrid {
    
    private final UUID playerUUID;
    private int currentJoules;
    private ItemStack[] fuelSlots; // Akan selalu berisi 5 slot
    private boolean overCapacity = false;
    
    public PlayerGlobalGrid(UUID playerUUID, int joules, ItemStack[] fuels) {
        this.playerUUID = playerUUID;
        this.currentJoules = joules;
        this.fuelSlots = (fuels == null || fuels.length != 5) ? new ItemStack[5] : fuels;
        // Jangan kalkulasi joules di sini, panggil calculateGridStatus(plugin) secara eksplisit
    }
    
    // Hitung status grid terpusat
    public void calculateGridStatus(SeriaGens plugin) {
        int totalFromSlots = 0;
        for (ItemStack item : fuelSlots) {
            if (item != null && item.getType() != Material.AIR) {
                if (plugin.getFuelManager().isFuel(item.getType())) {
                    FuelManager.FuelData data = plugin.getFuelManager().getFuelData(item.getType());
                    totalFromSlots += (data.joulesPerItem * item.getAmount());
                }
            }
        }

        int limit = getMaxJoulesLimitByRank(plugin);
        
        // JIKA TOTAL MELEBIHI LIMIT RANK GLOBAL
        if (totalFromSlots > limit) {
            this.currentJoules = 0; // Matikan daya GARDU
            this.overCapacity = true;
        } else {
            this.currentJoules = totalFromSlots;
            this.overCapacity = false;
        }
    }

    // Pindahkan logika rank limit di level GLOBAL GRID
    public int getMaxJoulesLimitByRank(SeriaGens plugin) {
        Player player = Bukkit.getPlayer(playerUUID);
        int baseMax = plugin.getConfigManager().getGuiConfig().getInt("global-fuel.default-base-limit", 1000);
        
        if (player == null) return baseMax;

        // Rank Berbayar
        int[] paidLimits = {10000, 15000, 20000, 25000, 30000, 35000, 40000, 45000};
        for (int i = 8; i >= 1; i--) {
            if (player.hasPermission("seriagens.fuel.paid." + i)) return paidLimits[i-1];
        }

        // Rank Gratis
        for (int i = 8; i >= 1; i--) {
            if (player.hasPermission("seriagens.fuel.free." + i)) return i * 1000;
        }

        return baseMax;
    }

    // Mengecek apakah daya GARDU cukup untuk konsumsi satu mesin
    public boolean hasPower(int requiredJoules) {
        if (overCapacity) return false; // Mati jika overload
        return this.currentJoules >= requiredJoules;
    }

    // Mengurangi daya GARDU secara statis
    public void consumeGlobally(SeriaGens plugin, int amount) {
        this.currentJoules -= amount;
        if (this.currentJoules < 0) this.currentJoules = 0;
        
        // Update item di slot (mengurangi stack item fuel)
        updateFuelItems(plugin, amount);
    }

    private void updateFuelItems(SeriaGens plugin, int cost) {
        int remainingToDeduct = cost;
        for (int i = 0; i < fuelSlots.length; i++) {
            ItemStack item = fuelSlots[i];
            if (item == null || !plugin.getFuelManager().isFuel(item.getType())) continue;
            
            int fuelVal = plugin.getFuelManager().getFuelData(item.getType()).joulesPerItem;
            int itemsNeeded = (int) Math.ceil((double) remainingToDeduct / fuelVal);
            
            if (item.getAmount() > itemsNeeded) {
                item.setAmount(item.getAmount() - itemsNeeded);
                break;
            } else {
                remainingToDeduct -= (item.getAmount() * fuelVal);
                fuelSlots[i] = null;
                if (remainingToDeduct <= 0) break;
            }
        }
    }

    public int getCurrentJoules() { return currentJoules; }
    public boolean isOverCapacity() { return overCapacity; }
    public ItemStack getFuel(int index) {
        if (index >= 0 && index < fuelSlots.length) return fuelSlots[index];
        return null;
    }
    public void setFuel(int index, ItemStack item) {
        if (index >= 0 && index < fuelSlots.length) this.fuelSlots[index] = item;
    }
    public ItemStack[] getAllFuels() { return fuelSlots; }
    public UUID getPlayerUUID() { return playerUUID; }
}