package id.seria.gens.models;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import id.seria.gens.SeriaGens;
import id.seria.gens.managers.FuelManager;

public class PlayerGlobalGrid {
    
    private final UUID playerUUID;
    private int currentJoules; // BATERAI INTERNAL (Sisa Joule dari item yang dibakar)
    private ItemStack[] fuelSlots; // 5 Slot GUI
    private boolean overCapacity = false;
    
    public PlayerGlobalGrid(UUID playerUUID, int joules, ItemStack[] fuels) {
        this.playerUUID = playerUUID;
        this.currentJoules = joules;
        this.fuelSlots = (fuels == null || fuels.length != 5) ? new ItemStack[5] : fuels;
    }
    
    // Menghitung potensi Joule dari item fisik di slot
    public int getPotentialJoules(SeriaGens plugin) {
        int total = 0;
        for (ItemStack item : fuelSlots) {
            if (item != null && item.getType() != Material.AIR) {
                if (plugin.getFuelManager().isFuel(item.getType())) {
                    FuelManager.FuelData data = plugin.getFuelManager().getFuelData(item.getType());
                    total += (data.joulesPerItem * item.getAmount());
                }
            }
        }
        return total;
    }

    // Total daya yang ditampilkan di GUI (Baterai Internal + Item di Slot)
    public int getTotalDisplayedJoules(SeriaGens plugin) {
        return currentJoules + getPotentialJoules(plugin);
    }
    
    public void calculateGridStatus(SeriaGens plugin) {
        int limit = getMaxJoulesLimitByRank(plugin);
        this.overCapacity = (getTotalDisplayedJoules(plugin) > limit);
    }

    public int getMaxJoulesLimitByRank(SeriaGens plugin) {
        Player player = Bukkit.getPlayer(playerUUID);
        int baseMax = plugin.getConfigManager().getGuiConfig().getInt("global-fuel.default-base-limit", 1000);
        
        if (player == null) return baseMax;

        int[] paidLimits = {10000, 15000, 20000, 25000, 30000, 35000, 40000, 45000};
        for (int i = 8; i >= 1; i--) {
            if (player.hasPermission("seriagens.fuel.paid." + i)) return paidLimits[i-1];
        }

        for (int i = 8; i >= 1; i--) {
            if (player.hasPermission("seriagens.fuel.free." + i)) return i * 1000;
        }

        return baseMax;
    }

    // PENGECEKAN AMAN: Apakah daya gabungan (baterai + slot) mencukupi?
    public boolean hasPower(SeriaGens plugin, int requiredJoules) {
        if (overCapacity) return false;
        return getTotalDisplayedJoules(plugin) >= requiredJoules;
    }

    // LOGIKA ATOMIC (Zero Waste): Bakar item hanya saat baterai habis
    public void consumeGlobally(SeriaGens plugin, int amount) {
        if (overCapacity) return;

        if (currentJoules >= amount) {
            currentJoules -= amount;
            return;
        }

        while (currentJoules < amount) {
            int fuelIndex = -1;
            for (int i = 0; i < fuelSlots.length; i++) {
                if (fuelSlots[i] != null && fuelSlots[i].getAmount() > 0) {
                    fuelIndex = i;
                    break;
                }
            }
            
            if (fuelIndex == -1) {
                currentJoules = 0; 
                return;
            }

            ItemStack fuelItem = fuelSlots[fuelIndex];
            int valuePerItem = plugin.getFuelManager().getFuelData(fuelItem.getType()).joulesPerItem;
            
            fuelItem.setAmount(fuelItem.getAmount() - 1);
            if (fuelItem.getAmount() <= 0) {
                fuelSlots[fuelIndex] = null; // Item habis, hapus dari slot
            }
            
            currentJoules += valuePerItem;
        }

        currentJoules -= amount;
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
    
    public ItemStack[] getAllFuels() { 
        ItemStack[] cloned = new ItemStack[fuelSlots.length];
        for (int i = 0; i < fuelSlots.length; i++) {
            cloned[i] = fuelSlots[i] != null ? fuelSlots[i].clone() : null;
        }
        return cloned; 
    }
    
    public UUID getPlayerUUID() { return playerUUID; }
}