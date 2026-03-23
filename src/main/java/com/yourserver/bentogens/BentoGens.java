package com.yourserver.bentogens;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.yourserver.bentogens.commands.EventCommand;
import com.yourserver.bentogens.commands.GensetCommand;
import com.yourserver.bentogens.commands.MainCommand;
import com.yourserver.bentogens.commands.SellCommand;
import com.yourserver.bentogens.commands.ShopCommand;
import com.yourserver.bentogens.integration.BentoBoxIntegration;
import com.yourserver.bentogens.integration.FancyHologramsIntegration;
import com.yourserver.bentogens.listeners.ChunkListener;
import com.yourserver.bentogens.listeners.GUIListener;
import com.yourserver.bentogens.listeners.GeneratorListener;
import com.yourserver.bentogens.listeners.SellWandListener;
import com.yourserver.bentogens.listeners.WorldLoadListener;
import com.yourserver.bentogens.managers.ConfigManager;
import com.yourserver.bentogens.managers.CorruptionManager;
import com.yourserver.bentogens.managers.DatabaseManager;
import com.yourserver.bentogens.managers.EventManager;
import com.yourserver.bentogens.managers.GeneratorManager;
import com.yourserver.bentogens.managers.RequirementsChecker;
import com.yourserver.bentogens.managers.SellManager;

import net.milkbowl.vault.economy.Economy;

public final class BentoGens extends JavaPlugin {
    
    private static BentoGens instance;
    
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
    private FancyHologramsIntegration hologramIntegration;  // ← NEW!

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        getLogger().info("========================================");
        getLogger().info("  BentoGens v" + getDescription().getVersion());
        getLogger().info("========================================");
        
        // Setup economy (Vault)
        if (!setupEconomy()) {
            getLogger().severe("Vault not found! Economy features will be disabled.");
            getLogger().severe("Download Vault from: https://www.spigotmc.org/resources/vault.34315/");
        } else {
            getLogger().info("Hooked into Vault Economy");
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
        
        // Initialize managers
        getLogger().info("Initializing managers...");
        
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();
        
        generatorManager = new GeneratorManager(this);
        corruptionManager = new CorruptionManager(this);
        
        // Register listeners FIRST (to catch world loads)
        getLogger().info("Registering event listeners...");
        registerListeners();
        
        // Register commands
        getLogger().info("Registering commands...");
        registerCommands();
        
        // ========================================
        // CRITICAL: Delayed generator loading
        // Wait for BentoBox to finish loading worlds
        // ========================================
        
        // Check if worlds already loaded
        World caveworld = Bukkit.getWorld("caveblock-world");
        
        if (caveworld != null) {
            // Worlds already loaded - load immediately
            getLogger().info("Worlds already loaded - loading generators immediately");
            generatorManager.loadGenerators();
        } else {
            // Worlds not loaded yet - wait
            getLogger().info("Waiting for BentoBox to create worlds...");
            
            // Try multiple times with increasing delays
            scheduleDelayedLoad(20L);   // 1 second
            scheduleDelayedLoad(60L);   // 3 seconds
            scheduleDelayedLoad(100L);  // 5 seconds
            scheduleDelayedLoad(200L);  // 10 seconds
        }
        
        // Start background tasks
        getLogger().info("Starting background tasks...");
        startTasks();
        
        // Start corruption system
        corruptionManager.startCorruptionTask();
        
        getLogger().info("========================================");
        getLogger().info("  Plugin enabled successfully!");
        getLogger().info("  Economy: " + (economy != null ? "Enabled" : "Disabled"));
        getLogger().info("  BentoBox: " + (bentoBox != null ? "Enabled" : "Disabled"));
        getLogger().info("  Corruption: " + 
            (getConfig().getBoolean("corruption.enabled") ? "Enabled" : "Disabled"));
        getLogger().info("========================================");

        sellManager = new SellManager(this);
        requirementsChecker = new RequirementsChecker(this);
        
        // Initialize event system - NEW! ✅
        eventManager = new EventManager(this);
        
        // Initialize hologram integration - NEW! ✅
        hologramIntegration = new FancyHologramsIntegration(this);
    }
    
    /**
     * Schedule delayed generator loading
     */
    private void scheduleDelayedLoad(long delay) {
        getServer().getScheduler().runTaskLater(this, () -> {
            // Check if already loaded
            if (generatorManager.getAllGenerators().size() > 0) {
                return; // Already loaded
            }
            
            // Check if world exists now
            World world = Bukkit.getWorld("caveblock-world");
            if (world != null) {
                getLogger().info("World 'caveblock-world' detected - loading generators now!");
                generatorManager.loadGenerators();
                
                int loaded = generatorManager.getAllGenerators().size();
                getLogger().info("Successfully loaded " + loaded + " generators");
            }
        }, delay);
    }
    
    @Override
    public void onDisable() {
        // Remove all holograms
        if (hologramIntegration != null) {
            getLogger().info("Removing holograms...");
            hologramIntegration.removeAllHolograms();
        }
        
        // CRITICAL: Save SYNCHRONOUSLY before closing database!
        if (generatorManager != null) {
            getLogger().info("Saving all generators...");
            generatorManager.saveAllSync();  // ← SYNC save!
            getLogger().info("All generators saved!");
        }
        
        // NOW it's safe to close database
        if (databaseManager != null) {
            getLogger().info("Closing database...");
            databaseManager.close();
        }
        
        getLogger().info("BentoGens disabled!");
    }
    
    /**
     * Register event listeners
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GeneratorListener(this), this);
        getServer().getPluginManager().registerEvents(new ChunkListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldLoadListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(new SellWandListener(this), this);

        // Initialize BentoBox integration (but DON'T register as event listener!)
        if (bentoBox != null && getConfig().getBoolean("bentobox.enabled", true)) {
            try {
                bentoBoxIntegration = new BentoBoxIntegration(this, bentoBox);
                getLogger().info("BentoBox integration enabled (static mode)!");
            } catch (Exception e) {
                getLogger().warning("Failed to enable BentoBox integration: " + e.getMessage());
                getLogger().info("Island features will still work via static methods.");
            }
        } else if (bentoBox == null) {
            getLogger().info("BentoBox not found - island features disabled");
        } else {
            getLogger().info("BentoBox integration disabled in config");
        }

    }
    
    /**
     * Register commands
     */
    private void registerCommands() {
        getCommand("bentogens").setExecutor(new MainCommand(this));
        getCommand("bentogens").setTabCompleter(new MainCommand(this));
        getCommand("genshop").setExecutor(new ShopCommand(this));
        getCommand("genset").setExecutor(new GensetCommand(this));
        getCommand("genset").setTabCompleter(new GensetCommand(this));
        getCommand("gensell").setExecutor(new SellCommand(this));
        getCommand("gensell").setTabCompleter(new SellCommand(this));
        
        // Event command - NEW! ✅
        getCommand("event").setExecutor(new EventCommand(this));
        getCommand("event").setTabCompleter(new EventCommand(this));

        getLogger().info("Commands registered!");
    }
    
    /**
     * Start background tasks with AGGRESSIVE block restoration
     */
    private void startTasks() {
        // Generator tick task (every second)
        getServer().getScheduler().runTaskTimer(this, () -> {
            generatorManager.tickGenerators();
        }, 20L, 20L);
        
        // Auto-save task (every 5 minutes)
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            generatorManager.saveAll();
        }, 6000L, 6000L);
        
        // STAGE 1: Initial restoration (5 seconds after enable)
        getServer().getScheduler().runTaskLater(this, () -> {
            int restored = generatorManager.restoreAllBlocks();
            if (restored > 0) {
                getLogger().info("✅ Stage 1: Restored " + restored + " generator blocks (5s after enable)");
            }
        }, 100L);
        
        // STAGE 2: Secondary restoration (30 seconds after enable - for late-loading worlds)
        getServer().getScheduler().runTaskLater(this, () -> {
            int restored = generatorManager.restoreAllBlocks();
            if (restored > 0) {
                getLogger().info("✅ Stage 2: Restored " + restored + " generator blocks (30s after enable)");
            }
        }, 600L);
        
        // STAGE 3: Tertiary restoration (60 seconds after enable - final safety net)
        getServer().getScheduler().runTaskLater(this, () -> {
            int restored = generatorManager.restoreAllBlocks();
            if (restored > 0) {
                getLogger().info("✅ Stage 3: Restored " + restored + " generator blocks (60s after enable)");
            }
        }, 1200L);
        
        // STAGE 4: Periodic restoration check (every 5 minutes)
        getServer().getScheduler().runTaskTimer(this, () -> {
            int restored = generatorManager.restoreAllBlocks();
            if (restored > 0) {
                getLogger().warning("⚠ Periodic check: Restored " + restored + " generator blocks!");
            }
        }, 6000L, 6000L); // Every 5 minutes
    }
    
    /**
     * Setup Vault Economy
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = getServer()
            .getServicesManager()
            .getRegistration(Economy.class);
        
        if (rsp == null) {
            return false;
        }
        
        economy = rsp.getProvider();
        return economy != null;
    }

    // Getters
    
    public static BentoGens getInstance() {
        return instance;
    }
    
    public Object getBentoBox() {
        return bentoBox;
    }
    
    public BentoBoxIntegration getBentoBoxIntegration() {
        return bentoBoxIntegration;
    }
    
    public Economy getEconomy() {
        return economy;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public GeneratorManager getGeneratorManager() {
        return generatorManager;
    }
    
    public CorruptionManager getCorruptionManager() {
        return corruptionManager;
    }

    public SellManager getSellManager() {
        return sellManager;
    }

    public RequirementsChecker getRequirementsChecker() {
        return requirementsChecker;
    }

    public EventManager getEventManager() {
        return this.eventManager;
    }
    
    public FancyHologramsIntegration getHologramIntegration() {
        return hologramIntegration;
    }
}