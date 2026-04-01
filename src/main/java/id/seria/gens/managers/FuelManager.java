package id.seria.gens.managers;

import id.seria.gens.SeriaGens;
import id.seria.gens.models.PlayerGlobalGrid;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FuelManager {
    
    private final SeriaGens plugin;
    private FileConfiguration fuelConfig;
    private final Map<Material, FuelData> validFuels;
    private final Map<UUID, PlayerGlobalGrid> activeGrids; // Cache Gardu Induk Pemain
    
    public FuelManager(SeriaGens plugin) {
        this.plugin = plugin;
        this.validFuels = new HashMap<>();
        this.activeGrids = new HashMap<>();
        loadConfig();
    }
    
    public void loadConfig() {
        File file = new File(plugin.getDataFolder(), "fuel.yml");
        if (!file.exists()) {
            plugin.saveResource("fuel.yml", false);
        }
        fuelConfig = YamlConfiguration.loadConfiguration(file);
        validFuels.clear();
        
        ConfigurationSection fuelsSection = fuelConfig.getConfigurationSection("fuels");
        if (fuelsSection != null) {
            for (String key : fuelsSection.getKeys(false)) {
                Material mat = Material.matchMaterial(key);
                if (mat != null) {
                    int joules = fuelsSection.getInt(key + ".joules-per-item", 1);
                    int burnTime = fuelsSection.getInt(key + ".burn-time", 10);
                    validFuels.put(mat, new FuelData(joules, burnTime));
                }
            }
        }
    }
    
    public boolean isFuel(Material material) {
        return validFuels.containsKey(material);
    }
    
    public FuelData getFuelData(Material material) {
        return validFuels.get(material);
    }

    // --- LOGIKA GARDU INDUK GLOBAL ---
    public PlayerGlobalGrid getGlobalGrid(UUID playerUUID) {
        if (!activeGrids.containsKey(playerUUID)) {
            // Load dari database jika belum ada di cache
            DatabaseManager.GlobalFuelData data = plugin.getDatabaseManager().loadGlobalFuelSync(playerUUID);
            PlayerGlobalGrid grid = new PlayerGlobalGrid(playerUUID, data.joules, data.fuelItems);
            activeGrids.put(playerUUID, grid);
        }
        return activeGrids.get(playerUUID);
    }

    public void saveAllGrids() {
        for (PlayerGlobalGrid grid : activeGrids.values()) {
            plugin.getDatabaseManager().saveGlobalFuelSync(grid.getPlayerUUID(), grid.getCurrentJoules(), grid.getAllFuels());
        }
    }
    
    public static class FuelData {
        public final int joulesPerItem;
        public final int burnTime;
        public FuelData(int joulesPerItem, int burnTime) {
            this.joulesPerItem = joulesPerItem;
            this.burnTime = burnTime;
        }
    }
}