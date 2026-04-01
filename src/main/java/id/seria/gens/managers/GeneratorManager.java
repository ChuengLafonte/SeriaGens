package id.seria.gens.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

import id.seria.gens.SeriaGens;
import id.seria.gens.models.Generator;
import id.seria.gens.models.PlayerGlobalGrid;

public class GeneratorManager {
    
    private final SeriaGens plugin;
    private final Map<Location, Generator> generators;
    private final Map<UUID, List<Generator>> playerGenerators;
    
    // Track pending restorations by world name AND full generator data
    private final Map<String, List<PendingGenerator>> pendingRestorations;
    private int dropMultiplier = 1;
    private double speedMultiplier = 1.0;
    private int tierBoost = 0;
    private boolean mixedUpMode = false;    

    public GeneratorManager(SeriaGens plugin) {
        this.plugin = plugin;
        this.generators = new ConcurrentHashMap<>();
        this.playerGenerators = new ConcurrentHashMap<>();
        this.pendingRestorations = new ConcurrentHashMap<>();
    }

    public Object getJoules(UUID uniqueId, String genType) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public double getSpeedMultiplier() {
        return plugin.getConfig().getDouble("events.speed-multiplier", 1.0);
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
            String worldName = null;
            int x = 0, y = 0, z = 0;
            
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
            
            if (worldName == null) continue;
            
            World world = Bukkit.getWorld(worldName);
            
            if (world == null) {
                PendingGenerator pending = new PendingGenerator(worldName, x, y, z, gen);
                pendingRestorations.computeIfAbsent(worldName, k -> new ArrayList<>()).add(pending);
                plugin.getLogger().warning("World not found: " + worldName);
                continue;
            }
            
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
    
    public void onWorldLoad(World world) {
        String worldName = world.getName();
        plugin.getLogger().info("World '" + worldName + "' loaded - checking for pending generators...");
        
        List<PendingGenerator> pending = pendingRestorations.remove(worldName);
        
        if (pending != null && !pending.isEmpty()) {
            plugin.getLogger().info("Restoring " + pending.size() + " generators in world '" + worldName + "'");
            
            int restored = 0;
            for (PendingGenerator pg : pending) {
                try {
                    Location location = new Location(world, pg.x, pg.y, pg.z);
                    pg.generator.setLocation(location);
                    
                    generators.put(location, pg.generator);
                    playerGenerators.computeIfAbsent(pg.generator.getOwner(), k -> new ArrayList<>()).add(pg.generator);
                    
                    if (restoreGeneratorBlock(pg.generator)) {
                        restored++;
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to restore generator: " + e.getMessage());
                }
            }
            
            plugin.getLogger().info("Successfully restored " + restored + "/" + pending.size() + 
                " generators in world '" + worldName + "'");
        }
    }
    
    public int restoreAllBlocks() {
        if (!plugin.getConfig().getBoolean("generators.force-restore-blocks", true)) return 0;
        
        int restored = 0;
        int failed = 0;
        
        for (Generator gen : new ArrayList<>(generators.values())) {
            try {
                if (restoreGeneratorBlock(gen)) restored++;
            } catch (Exception e) {
                failed++;
                plugin.getLogger().warning("Failed to restore generator " + gen.getId() + ": " + e.getMessage());
            }
        }
        
        if (restored > 0) plugin.getLogger().info("✅ Block restoration: " + restored + " blocks restored");
        if (failed > 0) plugin.getLogger().warning("⚠ Failed to restore " + failed + " blocks");
        
        return restored;
    }
    
    public boolean restoreGeneratorBlock(Generator gen) {
        Location loc = gen.getLocation();
        World world = loc.getWorld();
        
        if (world == null) return false;
        
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
        
        if (block.getType() != correctMaterial) {
            block.setType(correctMaterial, true);
            return true;
        }
        return false;
    }
    
    public int restoreChunkGenerators(Chunk chunk) {
        if (!plugin.getConfig().getBoolean("generators.restore-on-chunk-load", true)) return 0;
        
        int restored = 0;
        for (Generator gen : new ArrayList<>(generators.values())) {
            Location loc = gen.getLocation();
            if (loc.getWorld() != null && 
                loc.getWorld().equals(chunk.getWorld()) &&
                (loc.getBlockX() >> 4) == chunk.getX() &&
                (loc.getBlockZ() >> 4) == chunk.getZ()) {
                
                if (restoreGeneratorBlock(gen)) restored++;
            }
        }
        return restored;
    }
    
    public boolean placeGenerator(Player player, Location location, String type) {
        if (generators.containsKey(location)) return false;
        
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
            } catch (IllegalArgumentException ignored) {}
        }
        
        String msg = plugin.getConfigManager().getMessage("generator-placed")
            .replace("{current}", String.valueOf(current + 1))
            .replace("{max}", String.valueOf(max));
        player.sendMessage(msg);
        
        return true;
    }
    
    public boolean removeGenerator(Location location, Player player) {
        Generator gen = generators.get(location);
        if (gen == null) return false;
        
        generators.remove(location);
        List<Generator> playerGens = playerGenerators.get(gen.getOwner());
        if (playerGens != null) playerGens.remove(gen);
        
        plugin.getDatabaseManager().deleteGenerator(gen.getId());
        plugin.getHologramIntegration().removeCorruptionHologram(gen);
        location.getBlock().setType(Material.AIR);
        
        if (player != null && player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            ItemStack generatorItem = getGeneratorItem(gen.getType());
            java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(generatorItem);
            if (!leftover.isEmpty()) {
                for (ItemStack item : leftover.values()) {
                    player.getWorld().dropItemNaturally(location, item);
                }
                player.sendMessage(plugin.getConfigManager().colorize("&e&l⚠ &fInventory full! Generator dropped."));
            }
        }
        
        if (player != null && plugin.getConfig().getBoolean("sounds.generator-break.enabled", true)) {
            String soundName = plugin.getConfig().getString("sounds.generator-break.sound", "ENTITY_ITEM_PICKUP");
            float volume = (float) plugin.getConfig().getDouble("sounds.generator-break.volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble("sounds.generator-break.pitch", 1.0);
            try {
                Sound sound = Sound.valueOf(soundName);
                player.playSound(location, sound, volume, pitch);
            } catch (IllegalArgumentException ignored) {}
        }
        
        if (player != null) {
            player.sendMessage(plugin.getConfigManager().getMessage("generator-removed"));
        }
        
        return true;
    }
    
    public void tickGenerators() {
    for (Generator gen : new ArrayList<>(generators.values())) {
        org.bukkit.World world = gen.getLocation().getWorld();
        if (world == null) continue;
        
        if (!world.isChunkLoaded(gen.getLocation().getBlockX() >> 4, gen.getLocation().getBlockZ() >> 4)) continue;
        
        // 1. SKIP JIKA RUSAK
        if (gen.isCorrupted()) continue; 

        UUID ownerUUID = gen.getOwner();
        
        // 2. CEK GARDU INDUK GLOBAL OWNER
        PlayerGlobalGrid grid = plugin.getFuelManager().getGlobalGrid(ownerUUID);
        if (grid == null) continue; // Owner belum login/load (tergantung logika caching)

        // JIKA GARDU OVER CAPACITY: Mesin mati
        if (grid.isOverCapacity()) continue;

        String type = gen.getType();
        int interval = plugin.getConfigManager().getGeneratorInterval(type);
        int jouleCost = plugin.getConfigManager().getGeneratorsConfig().getInt(type + ".joule-cost-per-drop", 1);

        // 3. CEK DAYA GARDU: Jika GARDU habis daya, mesin mati
        if (grid.getCurrentJoules() < jouleCost) continue;

        boolean onlineOnly = plugin.getConfig().getBoolean("generators.online-only", true);
        if (onlineOnly) {
            org.bukkit.entity.Player owner = org.bukkit.Bukkit.getPlayer(ownerUUID);
            if (owner == null || !owner.isOnline()) continue;
        }
        
        // Apply speed multiplier from events
        int adjustedInterval = (int) (interval * (1.0 - (1.0 - speedMultiplier)));
        if (adjustedInterval < 1) adjustedInterval = 1;
        
        if (gen.canDrop(adjustedInterval)) {
            dropItems(gen);
            
            // KONSUMSI DAYA DARI GARDU INDUK GLOBAL
            grid.consumeGlobally(plugin, jouleCost); 
            
            gen.markDropped();
            
            // Simpan Generator (Wajib untuk update last_drop), 
            // Save Grid akan dilakukan secara Asynchronous/ketika GUI tutup.
            plugin.getDatabaseManager().saveGenerator(gen);
            }
        }
    }
 
    private void dropItems(Generator gen) {
        String effectiveType = gen.getType();
        if (tierBoost > 0) {
            for (int i = 0; i < tierBoost; i++) {
                String nextTier = plugin.getConfigManager().getGeneratorsConfig()
                    .getString(effectiveType + ".upgrade.next-tier", "none");
                if (!nextTier.equals("none") && !nextTier.equals("[]")) {
                    effectiveType = nextTier;
                }
            }
        }

        ConfigurationSection drops = plugin.getConfigManager()
            .getGeneratorsConfig()
            .getConfigurationSection(effectiveType + ".drops");
        
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
        
        int amount = dropConfig.getInt("amount", 1) * dropMultiplier;
        ItemStack item = new ItemStack(material, amount);
        
        // --- NBT TAG INJECTION SYSTEM ---
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (dropConfig.contains("display-name")) {
                meta.setDisplayName(plugin.getConfigManager().colorize(dropConfig.getString("display-name")));
            }
            if (dropConfig.contains("lore")) {
                List<String> lore = new ArrayList<>();
                for (String line : dropConfig.getStringList("lore")) {
                    lore.add(plugin.getConfigManager().colorize(line));
                }
                meta.setLore(lore);
            }
            
            // Menyuntikkan NBT Data Harga Jual (Akurasi 100%)
            double sellValue = drops.getDouble(selectedDrop + ".sell-value", 0.0);
            if (sellValue > 0) {
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "seriagens_value");
                meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.DOUBLE, sellValue);
            }
            
            item.setItemMeta(meta);
        }
        
        Location spawnLocation = gen.getLocation().clone().add(0.5, 1.0, 0.5);
        World world = gen.getLocation().getWorld();
        
        if (world != null) {
            Item droppedItem = world.dropItem(spawnLocation, item);
            droppedItem.setVelocity(new Vector(0, 0, 0));
            
            if (plugin.getConfig().getBoolean("sounds.generator-drop.enabled", false)) {
                String soundName = plugin.getConfig().getString("sounds.generator-drop.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
                float volume = (float) plugin.getConfig().getDouble("sounds.generator-drop.volume", 0.5);
                float pitch = (float) plugin.getConfig().getDouble("sounds.generator-drop.pitch", 1.5);
                try {
                    Sound sound = Sound.valueOf(soundName);
                    world.playSound(spawnLocation, sound, volume, pitch);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }
    
    public Generator getGenerator(Location location) { return generators.get(location); }
    public Collection<Generator> getAllGenerators() { return generators.values(); }
    public int getPlayerGeneratorCount(UUID player) {
        List<Generator> gens = playerGenerators.get(player);
        return gens != null ? gens.size() : 0;
    }
    
    public int getMaxGenerators(Player player) {
        if (player.hasPermission("seriagens.max.unlimited")) {
            return Integer.MAX_VALUE;
        }
        
        int max = plugin.getConfig().getInt("generators.default-max", 10);
        
        ConfigurationSection limits = plugin.getConfig().getConfigurationSection("generators.limits");
        if (limits != null) {
            for (String key : limits.getKeys(false)) {
                if (player.hasPermission("seriagens.max." + key)) {
                    int limitValue = limits.getInt(key);
                    if (limitValue > max) max = limitValue;
                }
            }
        }
        
        // Tambahkan Extra Slot dari Database Pemain
        int extraSlots = plugin.getConfigManager().getPlayersConfig().getInt(player.getUniqueId().toString() + ".extra-slots", 0);
        return max + extraSlots;
    }
    
    public ItemStack getGeneratorItem(String type) {
        ConfigurationSection itemConfig = plugin.getConfigManager()
            .getGeneratorsConfig()
            .getConfigurationSection(type + ".item");
        
        if (itemConfig == null) return new ItemStack(Material.STONE);
        
        String materialName = itemConfig.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.STONE;
        
        int amount = itemConfig.getInt("amount", 1);
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        
        if (itemConfig.contains("display-name")) {
            meta.setDisplayName(plugin.getConfigManager().colorize(itemConfig.getString("display-name")));
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
    
    public void saveAll() {
        for (Generator gen : generators.values()) {
            plugin.getDatabaseManager().saveGenerator(gen);
        }
    }
    
    public void saveAllSync() {
        plugin.getLogger().info("Saving " + generators.size() + " generators synchronously...");
        int saved = 0, failed = 0;
        for (Generator gen : generators.values()) {
            try {
                plugin.getDatabaseManager().saveGeneratorSync(gen);
                saved++;
            } catch (Exception e) {
                failed++;
                plugin.getLogger().severe("Failed to save generator " + gen.getId() + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Saved " + saved + " generators" + (failed > 0 ? " (" + failed + " failed)" : ""));
    }

    public void setDropMultiplier(int multiplier) { this.dropMultiplier = multiplier; }
    public void resetDropMultiplier() { this.dropMultiplier = 1; }
    public void setSpeedMultiplier(double multiplier) { this.speedMultiplier = multiplier; }
    public void resetSpeedMultiplier() { this.speedMultiplier = 1.0; }
    public void setTierBoost(int boost) { this.tierBoost = boost; }
    public void resetTierBoost() { this.tierBoost = 0; }
    public void setMixedUpMode(boolean active) { this.mixedUpMode = active; }

    public void addExtraSlots(java.util.UUID uuid, int amount) {
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfigManager().getPlayersConfig();
        String path = uuid.toString() + ".extra-slots";
        int current = cfg.getInt(path, 0);
        cfg.set(path, current + amount);
        plugin.getConfigManager().savePlayersConfig();
    }
}