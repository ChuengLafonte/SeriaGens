package com.yourserver.bentogens.managers;

import com.yourserver.bentogens.BentoGens;
import com.yourserver.bentogens.models.Generator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GeneratorManager {
    
    private final BentoGens plugin;
    private final Map<Location, Generator> generators;
    private final Map<UUID, List<Generator>> playerGenerators;
    
    public GeneratorManager(BentoGens plugin) {
        this.plugin = plugin;
        this.generators = new ConcurrentHashMap<>();
        this.playerGenerators = new ConcurrentHashMap<>();
    }
    
    /**
     * Load all generators from database
     */
    public void loadGenerators() {
        plugin.getLogger().info("Loading generators from database...");
        
        List<Generator> loadedGens = plugin.getDatabaseManager().loadAllGenerators();
        
        for (Generator gen : loadedGens) {
            generators.put(gen.getLocation(), gen);
            playerGenerators.computeIfAbsent(gen.getOwner(), k -> new ArrayList<>()).add(gen);
        }
        
        plugin.getLogger().info("Loaded " + generators.size() + " generators");
    }
    
    /**
     * CRITICAL: Restore all generator blocks after server restart
     * This fixes the bug where blocks become vanilla!
     */
    public int restoreAllBlocks() {
        if (!plugin.getConfig().getBoolean("generators.force-restore-blocks", true)) {
            return 0;
        }
        
        int restored = 0;
        
        for (Generator gen : generators.values()) {
            if (restoreGeneratorBlock(gen)) {
                restored++;
            }
        }
        
        if (restored > 0) {
            plugin.getLogger().info("Block restoration complete: " + restored + " blocks restored");
        }
        
        return restored;
    }
    
    /**
     * Restore a single generator block
     */
    private boolean restoreGeneratorBlock(Generator gen) {
        Location loc = gen.getLocation();
        World world = loc.getWorld();
        
        if (world == null) {
            plugin.getLogger().warning("Cannot restore generator - world not loaded: " + gen.getId());
            return false;
        }
        
        // Ensure chunk is loaded
        if (!world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            world.loadChunk(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        }
        
        Block block = loc.getBlock();
        String materialName = plugin.getConfigManager().getGeneratorMaterial(gen.getType());
        Material correctMaterial = Material.matchMaterial(materialName);
        
        if (correctMaterial == null) {
            plugin.getLogger().severe("Invalid material for generator type: " + gen.getType() + " -> " + materialName);
            return false;
        }
        
        // Check if block needs restoration
        if (block.getType() != correctMaterial) {
            plugin.getLogger().info(
                "Restoring generator at " + 
                world.getName() + " " + loc.getBlockX() + "," + 
                loc.getBlockY() + "," + loc.getBlockZ() + 
                " from " + block.getType() + " to " + correctMaterial
            );
            
            // Restore block type
            block.setType(correctMaterial);
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Place a new generator
     */
    public boolean placeGenerator(Player player, Location location, String type) {
        // Check if generator already exists at location
        if (generators.containsKey(location)) {
            return false;
        }
        
        // Check max generators
        int current = getPlayerGeneratorCount(player.getUniqueId());
        int max = getMaxGenerators(player);
        
        if (current >= max) {
            String msg = plugin.getConfigManager().getMessage("max-generators")
                .replace("{max}", String.valueOf(max));
            player.sendMessage(msg);
            return false;
        }
        
        // Create generator
        String id = UUID.randomUUID().toString();
        Generator gen = new Generator(id, player.getUniqueId(), location, type);
        
        // Set block
        String materialName = plugin.getConfigManager().getGeneratorMaterial(type);
        Material material = Material.matchMaterial(materialName);
        
        if (material != null) {
            location.getBlock().setType(material);
        }
        
        // Add to cache
        generators.put(location, gen);
        playerGenerators.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(gen);
        
        // Save to database
        plugin.getDatabaseManager().saveGenerator(gen);
        
        // Send message
        String msg = plugin.getConfigManager().getMessage("generator-placed")
            .replace("{current}", String.valueOf(current + 1))
            .replace("{max}", String.valueOf(max));
        player.sendMessage(msg);
        
        return true;
    }
    
    /**
     * Remove generator at location
     */
    public boolean removeGenerator(Location location, Player player) {
        Generator gen = generators.get(location);
        if (gen == null) {
            return false;
        }
        
        // Remove from cache
        generators.remove(location);
        
        List<Generator> playerGens = playerGenerators.get(gen.getOwner());
        if (playerGens != null) {
            playerGens.remove(gen);
        }
        
        // Delete from database
        plugin.getDatabaseManager().deleteGenerator(gen.getId());
        
        // Set block to air
        location.getBlock().setType(Material.AIR);
        
        // Send message if player provided
        if (player != null) {
            String msg = plugin.getConfigManager().getMessage("generator-removed");
            player.sendMessage(msg);
        }
        
        return true;
    }
    
    /**
     * Tick all generators (called every second)
     */
    public void tickGenerators() {
        for (Generator gen : generators.values()) {
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
        
        // Get random drop
        int totalChance = 0;
        Map<String, Integer> chances = new HashMap<>();
        
        for (String dropId : drops.getKeys(false)) {
            int chance = drops.getInt(dropId + ".chance", 0);
            chances.put(dropId, chance);
            totalChance += chance;
        }
        
        if (totalChance == 0) return;
        
        // Roll random
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
        
        // Create item
        ConfigurationSection dropConfig = drops.getConfigurationSection(selectedDrop + ".item");
        if (dropConfig == null) return;
        
        String materialName = dropConfig.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName);
        
        if (material == null) return;
        
        int amount = dropConfig.getInt("amount", 1);
        ItemStack item = new ItemStack(material, amount);
        
        // Set display name and lore
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
        
        // Drop item to player or world
        Player owner = Bukkit.getPlayer(gen.getOwner());
        
        boolean dropToInventory = plugin.getConfig().getBoolean("generators.drop-to-inventory", true);
        
        if (owner != null && owner.isOnline() && dropToInventory) {
            // Add to inventory
            owner.getInventory().addItem(item);
        } else {
            // Drop in world
            gen.getLocation().getWorld().dropItemNaturally(gen.getLocation(), item);
        }
    }
    
    /**
     * Get generator at location
     */
    public Generator getGenerator(Location location) {
        return generators.get(location);
    }
    
    /**
     * Get all generators
     */
    public Collection<Generator> getAllGenerators() {
        return generators.values();
    }
    
    /**
     * Get player's generator count
     */
    public int getPlayerGeneratorCount(UUID player) {
        List<Generator> gens = playerGenerators.get(player);
        return gens != null ? gens.size() : 0;
    }
    
    /**
     * Get max generators for player
     */
    public int getMaxGenerators(Player player) {
        // Check permissions
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
    
    /**
     * Get generator item
     */
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
     * Save all generators to database
     */
    public void saveAll() {
        for (Generator gen : generators.values()) {
            plugin.getDatabaseManager().saveGenerator(gen);
        }
    }
}