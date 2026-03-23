package com.yourserver.bentogens.managers;

import com.yourserver.bentogens.BentoGens;
import com.yourserver.bentogens.models.Generator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Manages generator corruption system
 * Generators only corrupt when owner is ONLINE
 */
public class CorruptionManager {
    
    private final BentoGens plugin;
    private final Random random;
    
    public CorruptionManager(BentoGens plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }
    
    /**
     * Start corruption task
     * Runs periodically to corrupt generators
     */
    public void startCorruptionTask() {
        if (!plugin.getConfig().getBoolean("corruption.enabled", false)) {
            plugin.getLogger().info("Corruption system disabled in config");
            return;
        }
        
        long intervalMinutes = plugin.getConfig().getLong("corruption.interval", 180);
        long intervalTicks = intervalMinutes * 60 * 20; // Convert to ticks
        
        // Run corruption check every X minutes
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            runCorruptionWave();
        }, intervalTicks, intervalTicks);
        
        plugin.getLogger().info("Corruption system started (interval: " + intervalMinutes + " minutes)");
    }
    
    /**
     * Run corruption wave
     * Only affects generators whose owners are ONLINE
     */
    private void runCorruptionWave() {
        if (!plugin.getConfig().getBoolean("corruption.enabled", false)) {
            return;
        }
        
        // Get all generators
        List<Generator> allGenerators = new ArrayList<>(plugin.getGeneratorManager().getAllGenerators());
        
        if (allGenerators.isEmpty()) {
            return;
        }
        
        // Filter: Only generators with ONLINE owners
        List<Generator> eligibleGenerators = new ArrayList<>();
        
        for (Generator gen : allGenerators) {
            Player owner = Bukkit.getPlayer(gen.getOwner());
            
            // Only include if owner is online
            if (owner != null && owner.isOnline()) {
                // Skip already corrupted
                if (!gen.isCorrupted()) {
                    // Check if enough time has passed since last check
                    if (gen.shouldCheckCorruption(plugin.getConfig().getLong("corruption.interval", 180))) {
                        eligibleGenerators.add(gen);
                    }
                }
            }
        }
        
        if (eligibleGenerators.isEmpty()) {
            return;
        }
        
        // Calculate how many to test (percentage of eligible)
        int percentage = plugin.getConfig().getInt("corruption.percentage", 10);
        int toTest = Math.max(1, (eligibleGenerators.size() * percentage) / 100);
        
        // Randomly select generators to test
        List<Generator> toTestGenerators = new ArrayList<>();
        List<Generator> tempList = new ArrayList<>(eligibleGenerators);
        
        for (int i = 0; i < toTest && !tempList.isEmpty(); i++) {
            int index = random.nextInt(tempList.size());
            toTestGenerators.add(tempList.remove(index));
        }
        
        // Test each selected generator for corruption
        int corruptedCount = 0;
        
        for (Generator gen : toTestGenerators) {
            // Mark corruption check
            gen.markCorruptionCheck();
            
            // Get corruption chance for this generator type
            double corruptionChance = getCorruptionChance(gen.getType());
            
            // Roll for corruption
            if (random.nextDouble() * 100 < corruptionChance) {
                gen.setCorrupted(true);
                plugin.getDatabaseManager().saveGenerator(gen);
                corruptedCount++;
                
                // Show hologram ✅
                plugin.getHologramIntegration().showCorruptionHologram(gen);
                
                // Notify owner
                Player owner = Bukkit.getPlayer(gen.getOwner());
                if (owner != null && owner.isOnline()) {
                    notifyCorruption(owner, gen);
                }
            }
        }
        
        // Broadcast if any corrupted
        if (corruptedCount > 0) {
            broadcastCorruptionWave(corruptedCount);
        }
    }
    
    /**
     * Get corruption chance for generator type
     */
    private double getCorruptionChance(String type) {
        return plugin.getConfigManager()
            .getGeneratorsConfig()
            .getDouble(type + ".corruption.chance", 5.0);
    }
    
    /**
     * Notify player about corrupted generator
     */
    private void notifyCorruption(Player player, Generator gen) {
        List<String> messages = plugin.getConfig().getStringList("corruption.notify.messages");
        
        if (messages.isEmpty()) {
            messages = List.of(
                "&c&l[!] One of your generators has been corrupted!",
                "&7Location: &e" + gen.getLocation().getBlockX() + ", " + 
                    gen.getLocation().getBlockY() + ", " + 
                    gen.getLocation().getBlockZ(),
                "&7Type: &e" + gen.getType(),
                "&7Repair it with &e/genset &7or by right-clicking it"
            );
        }
        
        for (String msg : messages) {
            player.sendMessage(plugin.getConfigManager().colorize(msg));
        }
    }
    
    /**
     * Broadcast corruption wave
     */
    private void broadcastCorruptionWave(int amount) {
        List<String> broadcast = plugin.getConfig().getStringList("corruption.broadcast");
        
        if (broadcast.isEmpty()) {
            return;
        }
        
        for (String msg : broadcast) {
            String formatted = plugin.getConfigManager()
                .colorize(msg.replace("{amount}", String.valueOf(amount)));
            Bukkit.broadcastMessage(formatted);
        }
    }
    
    /**
     * Manually corrupt a generator (for testing)
     */
    public void corruptGenerator(Generator gen) {
        gen.setCorrupted(true);
        plugin.getDatabaseManager().saveGenerator(gen);
        
        Player owner = Bukkit.getPlayer(gen.getOwner());
        if (owner != null && owner.isOnline()) {
            notifyCorruption(owner, gen);
        }
    }
    
    /**
     * Repair generator
     */
    public boolean repairGenerator(Generator gen, Player player) {
        if (!gen.isCorrupted()) {
            return false;
        }
        
        // Get repair cost
        double cost = plugin.getConfigManager()
            .getGeneratorsConfig()
            .getDouble(gen.getType() + ".corruption.cost", 50.0);
        
        // Check economy
        if (plugin.getEconomy() == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("economy-disabled"));
            return false;
        }
        
        // Withdraw money
        if (!plugin.getEconomy().withdrawPlayer(player, cost).transactionSuccess()) {
            String msg = plugin.getConfigManager().colorize(
                "&6[BentoGens] &cInsufficient funds! Need: &e$" + 
                String.format("%.2f", cost)
            );
            player.sendMessage(msg);
            return false;
        }
        
        // Repair
        gen.setCorrupted(false);
        plugin.getDatabaseManager().saveGenerator(gen);
        
        // Remove hologram ✅
        plugin.getHologramIntegration().removeCorruptionHologram(gen);
        
        // Success message
        String msg = plugin.getConfigManager().getMessage("generator-repaired")
            .replace("{cost}", String.format("%.2f", cost));
        player.sendMessage(msg);
        
        return true;
    }
    
    /**
     * Get total corrupted generators for player
     */
    public long getCorruptedCount(Player player) {
        return plugin.getGeneratorManager()
            .getAllGenerators()
            .stream()
            .filter(gen -> gen.getOwner().equals(player.getUniqueId()))
            .filter(Generator::isCorrupted)
            .count();
    }
}