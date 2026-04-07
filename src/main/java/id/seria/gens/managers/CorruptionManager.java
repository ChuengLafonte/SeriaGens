package id.seria.gens.managers;

import id.seria.gens.SeriaGens;
import id.seria.gens.models.Generator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CorruptionManager {
    
    private final SeriaGens plugin;
    private final Random random;
    
    public CorruptionManager(SeriaGens plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }
    
    public void startCorruptionTask() {
        if (!plugin.getConfig().getBoolean("corruption.enabled", false)) {
            plugin.getLogger().info("Corruption system disabled in config");
            return;
        }
        
        long intervalMinutes = plugin.getConfig().getLong("corruption.interval", 180);
        long intervalTicks = intervalMinutes * 60 * 20;
        
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            runCorruptionWave();
        }, intervalTicks, intervalTicks);
        
        plugin.getLogger().info("Corruption system started (interval: " + intervalMinutes + " minutes)");
    }
    
    private void runCorruptionWave() {
        if (!plugin.getConfig().getBoolean("corruption.enabled", false)) {
            return;
        }
        
        List<Generator> allGenerators = new ArrayList<>(plugin.getGeneratorManager().getAllGenerators());
        if (allGenerators.isEmpty()) return;
        
        List<Generator> eligibleGenerators = new ArrayList<>();
        for (Generator gen : allGenerators) {
            Player owner = Bukkit.getPlayer(gen.getOwner());
            if (owner != null && owner.isOnline()) {
                if (!gen.isCorrupted()) {
                    if (gen.shouldCheckCorruption(plugin.getConfig().getLong("corruption.interval", 180))) {
                        eligibleGenerators.add(gen);
                    }
                }
            }
        }
        
        if (eligibleGenerators.isEmpty()) return;
        
        int percentage = plugin.getConfig().getInt("corruption.percentage", 10);
        int toTest = Math.max(1, (eligibleGenerators.size() * percentage) / 100);
        
        List<Generator> toTestGenerators = new ArrayList<>();
        List<Generator> tempList = new ArrayList<>(eligibleGenerators);
        
        for (int i = 0; i < toTest && !tempList.isEmpty(); i++) {
            int index = random.nextInt(tempList.size());
            toTestGenerators.add(tempList.remove(index));
        }
        
        int corruptedCount = 0;
        for (Generator gen : toTestGenerators) {
            gen.markCorruptionCheck();
            double corruptionChance = getCorruptionChance(gen.getType());
            
            if (random.nextDouble() * 100 < corruptionChance) {
                gen.setCorrupted(true);
                plugin.getDatabaseManager().saveGenerator(gen);
                corruptedCount++;
                
                plugin.getHologramIntegration().showCorruptionHologram(gen);
                
                Player owner = Bukkit.getPlayer(gen.getOwner());
                if (owner != null && owner.isOnline()) {
                    notifyCorruption(owner, gen);
                }
            }
        }
        
        if (corruptedCount > 0) {
            broadcastCorruptionWave(corruptedCount);
        }
    }
    
    private double getCorruptionChance(String type) {
        return plugin.getConfigManager()
            .getGeneratorsConfig()
            .getDouble(type + ".corruption.chance", 5.0);
    }
    
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
    
    private void broadcastCorruptionWave(int amount) {
        List<String> broadcast = plugin.getConfig().getStringList("corruption.broadcast");
        if (broadcast.isEmpty()) return;
        for (String msg : broadcast) {
            String formatted = plugin.getConfigManager()
                .colorize(msg.replace("{amount}", String.valueOf(amount)));
            Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(formatted));
        }
    }
    
    public void corruptGenerator(Generator gen) {
        gen.setCorrupted(true);
        plugin.getDatabaseManager().saveGenerator(gen);
        Player owner = Bukkit.getPlayer(gen.getOwner());
        if (owner != null && owner.isOnline()) {
            notifyCorruption(owner, gen);
        }
    }
    
    public boolean repairGenerator(Generator gen, Player player) {
        if (!gen.isCorrupted()) return false;
        
        double cost = plugin.getConfigManager()
            .getGeneratorsConfig()
            .getDouble(gen.getType() + ".corruption.cost", 50.0);
        
        if (plugin.getEconomy() == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("economy-disabled"));
            return false;
        }
        
        if (!plugin.getEconomy().withdrawPlayer(player, cost).transactionSuccess()) {
            String msg = plugin.getConfigManager().colorize(
                "&6[SeriaGens] &cInsufficient funds! Need: &e$" + 
                String.format("%.2f", cost)
            );
            player.sendMessage(msg);
            return false;
        }
        
        gen.setCorrupted(false);
        plugin.getDatabaseManager().saveGenerator(gen);
        plugin.getHologramIntegration().removeCorruptionHologram(gen);
        
        String msg = plugin.getConfigManager().getMessage("generator-repaired")
            .replace("{cost}", String.format("%.2f", cost));
        player.sendMessage(msg);
        return true;
    }
    
    public long getCorruptedCount(Player player) {
        return plugin.getGeneratorManager()
            .getAllGenerators()
            .stream()
            .filter(gen -> gen.getOwner().equals(player.getUniqueId()))
            .filter(Generator::isCorrupted)
            .count();
    }
}