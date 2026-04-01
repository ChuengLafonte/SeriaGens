package id.seria.gens;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import id.seria.gens.commands.EventCommand;
import id.seria.gens.commands.GensetCommand;
import id.seria.gens.commands.MainCommand;
import id.seria.gens.commands.SellCommand;
import id.seria.gens.commands.ShopCommand;
import id.seria.gens.integration.BentoBoxIntegration;
import id.seria.gens.integration.FancyHologramsIntegration;
import id.seria.gens.listeners.ChunkListener;
import id.seria.gens.listeners.GUIListener;
import id.seria.gens.listeners.GeneratorListener;
import id.seria.gens.listeners.SellWandListener;
import id.seria.gens.listeners.WorldLoadListener;
import id.seria.gens.managers.ConfigManager;
import id.seria.gens.managers.CorruptionManager;
import id.seria.gens.managers.DatabaseManager;
import id.seria.gens.managers.EventManager;
import id.seria.gens.managers.FuelManager;
import id.seria.gens.managers.GeneratorManager;
import id.seria.gens.managers.RequirementsChecker;
import id.seria.gens.managers.SellManager;
import net.milkbowl.vault.economy.Economy;

public final class SeriaGens extends JavaPlugin {
    
    private static SeriaGens instance;
    private Object bentoBox;
    private BentoBoxIntegration bentoBoxIntegration;
    private Economy economy;
    
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private GeneratorManager generatorManager;
    private CorruptionManager corruptionManager;
    private SellManager sellManager;
    private RequirementsChecker requirementsChecker;
    private EventManager eventManager;
    private FancyHologramsIntegration hologramIntegration;
    private FuelManager fuelManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        
        getLogger().info("========================================");
        getLogger().info("  SeriaGens v" + getDescription().getVersion());
        getLogger().info("========================================");
        
        if (!setupEconomy()) {
            getLogger().severe("Vault not found! Economy features will be disabled.");
        }
        
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new id.seria.gens.integration.SeriaGensExpansion(this).register();
        }
        
        if (getServer().getPluginManager().getPlugin("BentoBox") != null) {
            try { bentoBox = getServer().getPluginManager().getPlugin("BentoBox"); } 
            catch (Exception e) { bentoBox = null; }
        }
        
        getLogger().info("Initializing managers...");
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        
        // HARUS DIINISIALISASI LEBIH DULU
        fuelManager = new FuelManager(this);
        
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();
        
        generatorManager = new GeneratorManager(this);
        corruptionManager = new CorruptionManager(this);
        sellManager = new SellManager(this);
        requirementsChecker = new RequirementsChecker(this);
        eventManager = new EventManager(this);
        hologramIntegration = new FancyHologramsIntegration(this);

        getLogger().info("Registering event listeners...");
        getServer().getPluginManager().registerEvents(new GeneratorListener(this), this);
        getServer().getPluginManager().registerEvents(new ChunkListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldLoadListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(new SellWandListener(this), this);

        if (bentoBox != null && getConfig().getBoolean("bentobox.enabled", true)) {
            try { bentoBoxIntegration = new BentoBoxIntegration(this, bentoBox); } 
            catch (Exception ignored) {}
        }

        getLogger().info("Registering commands...");
        getCommand("seriagens").setExecutor(new MainCommand(this));
        getCommand("seriagens").setTabCompleter(new MainCommand(this));
        getCommand("genshop").setExecutor(new ShopCommand(this));
        getCommand("genset").setExecutor(new GensetCommand(this));
        getCommand("genset").setTabCompleter(new GensetCommand(this));
        getCommand("gensell").setExecutor(new SellCommand(this));
        getCommand("gensell").setTabCompleter(new SellCommand(this));
        getCommand("event").setExecutor(new EventCommand(this));
        getCommand("event").setTabCompleter(new EventCommand(this));
        
        World caveworld = Bukkit.getWorld("caveblock-world");
        if (caveworld != null) generatorManager.loadGenerators();
        
        startTasks();
        corruptionManager.startCorruptionTask();
        
        getLogger().info("========================================");
        getLogger().info("  SeriaGens enabled successfully!");
        getLogger().info("========================================");
    }
    
    @Override
    public void onDisable() {
        if (hologramIntegration != null) hologramIntegration.removeAllHolograms();
        
        // PENTING: Simpan semua data sebelum mati!
        if (fuelManager != null) fuelManager.saveAllGrids(); 
        if (generatorManager != null) generatorManager.saveAllSync(); 
        
        if (databaseManager != null) databaseManager.close();
    }
    
    private void startTasks() {
        getServer().getScheduler().runTaskTimer(this, () -> generatorManager.tickGenerators(), 20L, 20L);
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> generatorManager.saveAll(), 6000L, 6000L);
        getServer().getScheduler().runTaskTimer(this, () -> generatorManager.restoreAllBlocks(), 6000L, 6000L);
    }
    
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public static SeriaGens getInstance() { return instance; }
    public Object getBentoBox() { return bentoBox; }
    public BentoBoxIntegration getBentoBoxIntegration() { return bentoBoxIntegration; }
    public Economy getEconomy() { return economy; }
    public ConfigManager getConfigManager() { return configManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public GeneratorManager getGeneratorManager() { return generatorManager; }
    public CorruptionManager getCorruptionManager() { return corruptionManager; }
    public SellManager getSellManager() { return sellManager; }
    public RequirementsChecker getRequirementsChecker() { return requirementsChecker; }
    public EventManager getEventManager() { return eventManager; }
    public FancyHologramsIntegration getHologramIntegration() { return hologramIntegration; }
    public FuelManager getFuelManager() { return fuelManager; }
}