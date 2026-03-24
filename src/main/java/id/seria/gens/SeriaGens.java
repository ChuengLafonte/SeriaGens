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

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        getLogger().info("========================================");
        getLogger().info("  SeriaGens v" + getDescription().getVersion());
        getLogger().info("========================================");
        
        // Setup economy (Vault)
        if (!setupEconomy()) {
            getLogger().severe("Vault not found! Economy features will be disabled.");
        } else {
            getLogger().info("Hooked into Vault Economy");
        }

        // Hook ke PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new id.seria.gens.integration.SeriaGensExpansion(this).register();
            getLogger().info("✅ PlaceholderAPI integration enabled!");
        } else {
            getLogger().info("PlaceholderAPI not found - placeholders will not work");
        }
        
        // Check if BentoBox is available
        if (getServer().getPluginManager().getPlugin("BentoBox") != null) {
            try {
                bentoBox = getServer().getPluginManager().getPlugin("BentoBox");
                getLogger().info("Hooked into BentoBox v" + 
                    ((JavaPlugin) bentoBox).getDescription().getVersion());
            } catch (Exception e) {
                getLogger().warning("BentoBox found but failed to hook: " + e.getMessage());
                bentoBox = null;
            }
        } else {
            getLogger().warning("BentoBox not found! Island features will be disabled.");
        }
        
        getLogger().info("Initializing managers...");
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();
        
        generatorManager = new GeneratorManager(this);
        corruptionManager = new CorruptionManager(this);
        
        getLogger().info("Registering event listeners...");
        registerListeners();
        
        getLogger().info("Registering commands...");
        registerCommands();
        
        // Cek pemuatan world
        World caveworld = Bukkit.getWorld("caveblock-world");
        
        if (caveworld != null) {
            getLogger().info("Worlds already loaded - loading generators immediately");
            generatorManager.loadGenerators();
        } else {
            getLogger().info("Waiting for BentoBox to create worlds...");
            scheduleDelayedLoad(20L);
            scheduleDelayedLoad(60L);
            scheduleDelayedLoad(100L);
            scheduleDelayedLoad(200L);
        }
        
        getLogger().info("Starting background tasks...");
        startTasks();
        
        corruptionManager.startCorruptionTask();

        sellManager = new SellManager(this);
        requirementsChecker = new RequirementsChecker(this);
        eventManager = new EventManager(this);
        hologramIntegration = new FancyHologramsIntegration(this);

        getLogger().info("========================================");
        getLogger().info("  SeriaGens enabled successfully!");
        getLogger().info("========================================");
    }
    
    private void scheduleDelayedLoad(long delay) {
        getServer().getScheduler().runTaskLater(this, () -> {
            if (generatorManager.getAllGenerators().size() > 0) return;
            
            World world = Bukkit.getWorld("caveblock-world");
            if (world != null) {
                getLogger().info("World 'caveblock-world' detected - loading generators now!");
                generatorManager.loadGenerators();
            }
        }, delay);
    }
    
    @Override
    public void onDisable() {
        if (hologramIntegration != null) {
            hologramIntegration.removeAllHolograms();
        }
        
        if (generatorManager != null) {
            getLogger().info("Saving all generators...");
            generatorManager.saveAllSync();
        }
        
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        getLogger().info("SeriaGens disabled!");
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GeneratorListener(this), this);
        getServer().getPluginManager().registerEvents(new ChunkListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldLoadListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(new SellWandListener(this), this);

        if (bentoBox != null && getConfig().getBoolean("bentobox.enabled", true)) {
            try {
                bentoBoxIntegration = new BentoBoxIntegration(this, bentoBox);
            } catch (Exception e) {
                getLogger().warning("Failed to enable BentoBox integration: " + e.getMessage());
            }
        }
    }
    
    private void registerCommands() {
        getCommand("seriagens").setExecutor(new MainCommand(this));
        getCommand("seriagens").setTabCompleter(new MainCommand(this));
        getCommand("genshop").setExecutor(new ShopCommand(this));
        getCommand("genset").setExecutor(new GensetCommand(this));
        getCommand("genset").setTabCompleter(new GensetCommand(this));
        getCommand("gensell").setExecutor(new SellCommand(this));
        getCommand("gensell").setTabCompleter(new SellCommand(this));
        
        getCommand("event").setExecutor(new EventCommand(this));
        getCommand("event").setTabCompleter(new EventCommand(this));
    }
    
    private void startTasks() {
        getServer().getScheduler().runTaskTimer(this, () -> {
            generatorManager.tickGenerators();
        }, 20L, 20L);
        
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            generatorManager.saveAll();
        }, 6000L, 6000L);
        
        getServer().getScheduler().runTaskLater(this, () -> generatorManager.restoreAllBlocks(), 100L);
        getServer().getScheduler().runTaskLater(this, () -> generatorManager.restoreAllBlocks(), 600L);
        getServer().getScheduler().runTaskLater(this, () -> generatorManager.restoreAllBlocks(), 1200L);
        
        getServer().getScheduler().runTaskTimer(this, () -> {
            generatorManager.restoreAllBlocks();
        }, 6000L, 6000L);
    }
    
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
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
}