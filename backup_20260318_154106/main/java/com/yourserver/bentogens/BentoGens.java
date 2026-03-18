package com.yourserver.bentogens;

import com.yourserver.bentogens.commands.MainCommand;
import com.yourserver.bentogens.commands.ShopCommand;
import com.yourserver.bentogens.integration.BentoBoxIntegration;  // ← HARUS INI!
import com.yourserver.bentogens.listeners.ChunkListener;
import com.yourserver.bentogens.listeners.GeneratorListener;
import com.yourserver.bentogens.managers.ConfigManager;
import com.yourserver.bentogens.managers.DatabaseManager;
import com.yourserver.bentogens.managers.GeneratorManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class BentoGens extends JavaPlugin {
    
    private static BentoGens instance;
    
    // BentoBox integration (optional)
    private Object bentoBox;
    
    // Managers
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private GeneratorManager generatorManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        getLogger().info("========================================");
        getLogger().info("  BentoGens v" + getDescription().getVersion());
        getLogger().info("========================================");
        
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
        
        // Load generators from database
        generatorManager.loadGenerators();
        
        // Register listeners
        getLogger().info("Registering event listeners...");
        registerListeners();
        
        // Register commands
        getLogger().info("Registering commands...");
        registerCommands();
        
        // Start background tasks
        getLogger().info("Starting background tasks...");
        startTasks();
        
        getLogger().info("========================================");
        getLogger().info("  Plugin enabled successfully!");
        getLogger().info("  Generators loaded: " + generatorManager.getAllGenerators().size());
        getLogger().info("========================================");
    }
    
    @Override
    public void onDisable() {
        // Save all data
        if (generatorManager != null) {
            getLogger().info("Saving all generators...");
            generatorManager.saveAll();
        }
        
        // Close database
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
        
        // Register BentoBox integration if available
        if (bentoBox != null) {
            try {
                BentoBoxIntegration integration = new BentoBoxIntegration(this, bentoBox);
                getServer().getPluginManager().registerEvents(integration, this);
                getLogger().info("BentoBox integration enabled via reflection!");
            } catch (Exception e) {
                getLogger().warning("Failed to initialize BentoBox integration: " + e.getMessage());
            }
        } else {
            getLogger().info("BentoBox not found - island features disabled");
        }
    }
    
    /**
     * Register commands
     */
    private void registerCommands() {
        getCommand("bentogens").setExecutor(new MainCommand(this));
        getCommand("bentogens").setTabCompleter(new MainCommand(this));
        getCommand("genshop").setExecutor(new ShopCommand(this));
        
        getLogger().info("Commands registered!");
    }
    
    /**
     * Start background tasks
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
        
        // Delayed restoration task (5 seconds after enable)
        getServer().getScheduler().runTaskLater(this, () -> {
            int restored = generatorManager.restoreAllBlocks();
            if (restored > 0) {
                getLogger().info("Restored " + restored + " generator blocks after startup!");
            }
        }, 100L);
    }
    
    // Getters
    public static BentoGens getInstance() {
        return instance;
    }
    
    public Object getBentoBox() {
        return bentoBox;
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
}