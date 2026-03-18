package com.yourserver.bentogens.managers;

import com.yourserver.bentogens.BentoGens;
import com.yourserver.bentogens.models.Generator;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GeneratorManager {
    
    private final BentoGens plugin;
    private final Map<Location, Generator> generators;
    private final Map<UUID, List<Generator>> playerGenerators;
    
    // Track pending restorations by world name AND full generator data
    private final Map<String, List<PendingGenerator>> pendingRestorations;
    
    public GeneratorManager(BentoGens plugin) {
        this.plugin = plugin;
        this.generators = new ConcurrentHashMap<>();
        this.playerGenerators = new ConcurrentHashMap<>();
        this.pendingRestorations = new ConcurrentHashMap<>();
    }
    
    /**
     * Pending generator data (for unloaded worlds)
     */
    private static class PendingGenerator {
        String worldName;
        int x, y, z;
        Generator generator;
        
        PendingGenerator(String worldName, int x, int y, int z, Generator gen) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.generator = gen;
        }
    }
    
    /**
     * Load all generators from database
     */
    public void loadGenerators() {
        plugin.getLogger().info("Loading generators from database...");
        
        List<Generator> loadedGens = plugin.getDatabaseManager().loadAllGenerators();
        
        for (Generator gen : loadedGens) {
            // Try to get world from location
            String worldName = null;
            int x = 0, y = 0, z = 0;
            
            // Parse location even if world not loaded
            try {
                String locStr = gen.getLocationString();
                String[] parts = locStr.split(",");
                if (parts.length >= 4) {
                    worldName = parts[0];
                    x = Integer.parseInt(parts[1]);
                    y = Integer.parseInt(parts[2]);
                    z = Integer.parseInt(parts[3]);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse generator location: " + e.getMessage());
                continue;
            }
            
            if (worldName == null) {
                continue;
            }
            
            // Check if world loaded
            World world = Bukkit.getWorld(worldName);
            
            if (world == null) {
                // World not loaded - add to pending
                PendingGenerator pending = new PendingGenerator(worldName, x, y, z, gen);
                pendingRestorations.computeIfAbsent(worldName, k -> new ArrayList<>()).add(pending);
                plugin.getLogger().warning("World not found: " + worldName);
                continue;
            }
            
            // World loaded - create proper location and add to active
            Location location = new Location(world, x, y, z);
            gen.setLocation(location);
            
            generators.put(location, gen);
            playerGenerators.computeIfAbsent(gen.getOwner(), k -> new ArrayList<>()).add(gen);
        }
        
        plugin.getLogger().info("Loaded " + generators.size() + " generators");
        
        if (!pendingRestorations.isEmpty()) {
            int totalPending = pendingRestorations.values().stream().mapToInt(List::size).sum();
            plugin.getLogger().warning("Pending restoration for " + totalPending + 
                " generators in unloaded worlds");
        }
    }
    
    /**
     * Handle world load - restore generators in that world
     */
    public void onWorldLoad(World world) {
        String worldName = world.getName();
        
        plugin.getLogger().info("World '" + worldName + "' loaded - checking for pending generators...");
        
        List<PendingGenerator> pending = pendingRestorations.remove(worldName);
        
        if (pending != null && !pending.isEmpty()) {
            plugin.getLogger().info("Restoring " + pending.size() + " generators in world '" + worldName + "'");
            
            int restored = 0;
            for (PendingGenerator pg : pending) {
                try {
                    // Create proper location with loaded world
                    Location location = new Location(world, pg.x, pg.y, pg.z);
                    pg.generator.setLocation(location);
                    
                    // Add to active generators
                    generators.put(location, pg.generator);
                    playerGenerators.computeIfAbsent(pg.generator.getOwner(), k -> new ArrayList<>()).add(pg.generator);
                    
                    // Restore block IMMEDIATELY
                    if (restoreGeneratorBlock(pg.generator)) {
                        restored++;
                        plugin.getLogger().info("✅ Restored generator at " + pg.x + "," + pg.y + "," + pg.z);
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to restore generator: " + e.getMessage());
                }
            }
            
            plugin.getLogger().info("Successfully restored " + restored + "/" + pending.size() + 
                " generators in world '" + worldName + "'");
        } else {
            plugin.getLogger().info("No pending generators for world '" + worldName + "'");
        }
    }
    
    /**
     * CRITICAL: Restore all generator blocks
     */
    public int restoreAllBlocks() {
        if (!plugin.getConfig().getBoolean("generators.force-restore-blocks", true)) {
            return 0;
        }
        
        int restored = 0;
        int failed = 0;
        
        for (Generator gen : new ArrayList<>(generators.values())) {
            try {
                if (restoreGeneratorBlock(gen)) {
                    restored++;
                }
            } catch (Exception e) {
                failed++;
                plugin.getLogger().warning("Failed to restore generator " + gen.getId() + ": " + e.getMessage());
            }
        }
        
        if (restored > 0) {
            plugin.getLogger().info("✅ Block restoration: " + restored + " blocks restored");
        }
        
        if (failed > 0) {
            plugin.getLogger().warning("⚠ Failed to restore " + failed + " blocks");
        }
        
        return restored;
    }
    
    /**
     * Restore a single generator block
     */
    public boolean restoreGeneratorBlock(Generator gen) {
        Location loc = gen.getLocation();
        World world = loc.getWorld();
        
        if (world == null) {
            return false;
        }
        
        // Force load chunk if needed
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            world.loadChunk(chunkX, chunkZ);
        }
        
        Block block = loc.getBlock();
        String materialName = plugin.getConfigManager().getGeneratorMaterial(gen.getType());
        Material correctMaterial = Material.matchMaterial(materialName);
        
        if (correctMaterial == null) {
            plugin.getLogger().severe("Invalid material for generator: " + gen.getType() + " -> " + materialName);
            return false;
        }
        
        // Check if block needs restoration
        if (block.getType() != correctMaterial) {
            plugin.getLogger().info("🔧 Restoring: " + 
                world.getName() + " @ " + 
                loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + 
                " | " + block.getType() + " → " + correctMaterial);
            
            // FORCE SET BLOCK TYPE
            block.setType(correctMaterial, true);
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Restore generators in a specific chunk
     */
    public int restoreChunkGenerators(Chunk chunk) {
        if (!plugin.getConfig().getBoolean("generators.restore-on-chunk-load", true)) {
            return 0;
        }
        
        int restored = 0;
        
        for (Generator gen : new ArrayList<>(generators.values())) {
            Location loc = gen.getLocation();
            
            if (loc.getWorld() != null && 
                loc.getWorld().equals(chunk.getWorld()) &&
                (loc.getBlockX() >> 4) == chunk.getX() &&
                (loc.getBlockZ() >> 4) == chunk.getZ()) {
                
                if (restoreGeneratorBlock(gen)) {
                    restored++;
                }
            }
        }
        
        return restored;
    }
    
    /**
     * Place a new generator
     */
    public boolean placeGenerator(Player player, Location location, String type) {
        if (generators.containsKey(location)) {
            return false;
        }
        
        int current = getPlayerGeneratorCount(player.getUniqueId());
        int max = getMaxGenerators(player);
        
        if (current >= max) {
            String msg = plugin.getConfigManager().getMessage("max-generators")
                .replace("{max}", String.valueOf(max));
            player.sendMessage(msg);
            return false;
        }
        
        String id = UUID.randomUUID().toString();
        Generator gen = new Generator(id, player.getUniqueId(), location, type);
        
        String materialName = plugin.getConfigManager().getGeneratorMaterial(type);
        Material material = Material.matchMaterial(materialName);
        
        if (material != null) {
            location.getBlock().setType(material, true);
        }
        
        generators.put(location, gen);
        playerGenerators.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(gen);
        
        plugin.getDatabaseManager().saveGenerator(gen);
        
        if (plugin.getConfig().getBoolean("sounds.generator-place.enabled", true)) {
            String soundName = plugin.getConfig().getString("sounds.generator-place.sound", "ENTITY_PLAYER_LEVELUP");
            float volume = (float) plugin.getConfig().getDouble("sounds.generator-place.volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble("sounds.generator-place.pitch", 2.0);
            
            try {
                Sound sound = Sound.valueOf(soundName);
                player.playSound(location, sound, volume, pitch);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound: " + soundName);
            }
        }
        
        String msg = plugin.getConfigManager().getMessage("generator-placed")
            .replace("{current}", String.valueOf(current + 1))
            .replace("{max}", String.valueOf(max));
        player.sendMessage(msg);
        
        plugin.getLogger().info("Generator placed: " + type + " by " + player.getName() + 
            " at " + location.getWorld().getName() + " " + 
            location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
        
        return true;
    }
    
    /**
     * Remove generator
     */
    public boolean removeGenerator(Location location, Player player) {
        Generator gen = generators.get(location);
        if (gen == null) {
            return false;
        }
        
        generators.remove(location);
        
        List<Generator> playerGens = playerGenerators.get(gen.getOwner());
        if (playerGens != null) {
            playerGens.remove(gen);
        }
        
        plugin.getDatabaseManager().deleteGenerator(gen.getId());
        
        location.getBlock().setType(Material.AIR);
        
        if (player != null && plugin.getConfig().getBoolean("sounds.generator-break.enabled", true)) {
            String soundName = plugin.getConfig().getString("sounds.generator-break.sound", "ENTITY_ITEM_PICKUP");
            float volume = (float) plugin.getConfig().getDouble("sounds.generator-break.volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble("sounds.generator-break.pitch", 1.0);
            
            try {
                Sound sound = Sound.valueOf(soundName);
                player.playSound(location, sound, volume, pitch);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound: " + soundName);
            }
        }
        
        if (player != null) {
            String msg = plugin.getConfigManager().getMessage("generator-removed");
            player.sendMessage(msg);
        }
        
        return true;
    }
    
    /**
     * Tick all generators
     */
    public void tickGenerators() {
        for (Generator gen : new ArrayList<>(generators.values())) {
            World world = gen.getLocation().getWorld();
            
            if (world == null) {
                continue;
            }
            
            if (!world.isChunkLoaded(
                    gen.getLocation().getBlockX() >> 4,
                    gen.getLocation().getBlockZ() >> 4)) {
                continue;
            }
            
            boolean onlineOnly = plugin.getConfig().getBoolean("generators.online-only", true);
            if (onlineOnly) {
                Player owner = Bukkit.getPlayer(gen.getOwner());
                if (owner == null || !owner.isOnline()) {
                    continue;
                }
            }
            
            String type = gen.getType();
            int interval = plugin.getConfigManager().getGeneratorInterval(type);
            
            if (gen.canDrop(interval)) {
                dropItems(gen);
                gen.markDropped();
            }
        }
    }
    
    /**
     * Drop items from generator
     */
    private void dropItems(Generator gen) {
        ConfigurationSection drops = plugin.getConfigManager()
            .getGeneratorsConfig()
            .getConfigurationSection(gen.getType() + ".drops");
        
        if (drops == null) return;
        
        int totalChance = 0;
        Map<String, Integer> chances = new HashMap<>();
        
        for (String dropId : drops.getKeys(false)) {
            int chance = drops.getInt(dropId + ".chance", 0);
            chances.put(dropId, chance);
            totalChance += chance;
        }
        
        if (totalChance == 0) return;
        
        Random random = new Random();
        int roll = random.nextInt(totalChance);
        
        int current = 0;
        String selectedDrop = null;
        
        for (Map.Entry<String, Integer> entry : chances.entrySet()) {
            current += entry.getValue();
            if (roll < current) {
                selectedDrop = entry.getKey();
                break;
            }
        }
        
        if (selectedDrop == null) return;
        
        ConfigurationSection dropConfig = drops.getConfigurationSection(selectedDrop + ".item");
        if (dropConfig == null) return;
        
        String materialName = dropConfig.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName);
        
        if (material == null) return;
        
        int amount = dropConfig.getInt("amount", 1);
        ItemStack item = new ItemStack(material, amount);
        
        if (dropConfig.contains("display-name") || dropConfig.contains("lore")) {
            ItemMeta meta = item.getItemMeta();
            
            if (dropConfig.contains("display-name")) {
                String displayName = plugin.getConfigManager()
                    .colorize(dropConfig.getString("display-name"));
                meta.setDisplayName(displayName);
            }
            
            if (dropConfig.contains("lore")) {
                List<String> lore = new ArrayList<>();
                for (String line : dropConfig.getStringList("lore")) {
                    lore.add(plugin.getConfigManager().colorize(line));
                }
                meta.setLore(lore);
            }
            
            item.setItemMeta(meta);
        }
        
        Location spawnLocation = gen.getLocation().clone().add(0.5, 1.0, 0.5);
        World world = gen.getLocation().getWorld();
        
        if (world != null) {
            Item droppedItem = world.dropItemNaturally(spawnLocation, item);
            droppedItem.setVelocity(new Vector(0, 0.2, 0));
            
            if (plugin.getConfig().getBoolean("sounds.generator-drop.enabled", false)) {
                String soundName = plugin.getConfig().getString("sounds.generator-drop.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
                float volume = (float) plugin.getConfig().getDouble("sounds.generator-drop.volume", 0.5);
                float pitch = (float) plugin.getConfig().getDouble("sounds.generator-drop.pitch", 1.5);
                
                try {
                    Sound sound = Sound.valueOf(soundName);
                    world.playSound(spawnLocation, sound, volume, pitch);
                } catch (IllegalArgumentException e) {
                    // Invalid sound
                }
            }
        }
    }
    
    public Generator getGenerator(Location location) {
        return generators.get(location);
    }
    
    public Collection<Generator> getAllGenerators() {
        return generators.values();
    }
    
    public int getPlayerGeneratorCount(UUID player) {
        List<Generator> gens = playerGenerators.get(player);
        return gens != null ? gens.size() : 0;
    }
    
    public int getMaxGenerators(Player player) {
        if (player.hasPermission("bentogens.max.unlimited")) {
            return Integer.MAX_VALUE;
        }
        
        for (int i = 100; i >= 10; i -= 10) {
            if (player.hasPermission("bentogens.max." + i)) {
                return i;
            }
        }
        
        return plugin.getConfig().getInt("generators.default-max", 10);
    }
    
    public ItemStack getGeneratorItem(String type) {
        ConfigurationSection itemConfig = plugin.getConfigManager()
            .getGeneratorsConfig()
            .getConfigurationSection(type + ".item");
        
        if (itemConfig == null) {
            return new ItemStack(Material.STONE);
        }
        
        String materialName = itemConfig.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName);
        
        if (material == null) {
            material = Material.STONE;
        }
        
        int amount = itemConfig.getInt("amount", 1);
        ItemStack item = new ItemStack(material, amount);
        
        ItemMeta meta = item.getItemMeta();
        
        if (itemConfig.contains("display-name")) {
            String displayName = plugin.getConfigManager()
                .colorize(itemConfig.getString("display-name"));
            meta.setDisplayName(displayName);
        }
        
        if (itemConfig.contains("lore")) {
            List<String> lore = new ArrayList<>();
            for (String line : itemConfig.getStringList("lore")) {
                lore.add(plugin.getConfigManager().colorize(line));
            }
            meta.setLore(lore);
        }
        
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Save all (ASYNC)
     */
    public void saveAll() {
        for (Generator gen : generators.values()) {
            plugin.getDatabaseManager().saveGenerator(gen);
        }
    }
    
    /**
     * Save all SYNCHRONOUSLY (for shutdown)
     */
    public void saveAllSync() {
        plugin.getLogger().info("Saving " + generators.size() + " generators synchronously...");
        
        int saved = 0;
        int failed = 0;
        
        for (Generator gen : generators.values()) {
            try {
                plugin.getDatabaseManager().saveGeneratorSync(gen);
                saved++;
            } catch (Exception e) {
                failed++;
                plugin.getLogger().severe("Failed to save generator " + gen.getId() + ": " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Saved " + saved + " generators" + 
            (failed > 0 ? " (" + failed + " failed)" : ""));
    }
}