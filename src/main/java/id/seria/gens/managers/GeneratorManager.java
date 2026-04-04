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
    private final Map<String, List<PendingGenerator>> pendingRestorations;
    
    // EVENT VARIABLES
    private int dropMultiplier = 1;
    private double speedReductionPercent = 0.0; // PERBAIKAN: Menggunakan persentase pengurangan
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
    
    // GETTERS UNTUK GUI
    public double getSpeedReduction() { return speedReductionPercent; }
    public int getDropMultiplier() { return dropMultiplier; }
    public int getTierBoost() { return tierBoost; }
    
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
                continue;
            }
            
            if (worldName == null) continue;
            
            World world = Bukkit.getWorld(worldName);
            
            if (world == null) {
                PendingGenerator pending = new PendingGenerator(worldName, x, y, z, gen);
                pendingRestorations.computeIfAbsent(worldName, k -> new ArrayList<>()).add(pending);
                continue;
            }
            
            Location location = new Location(world, x, y, z);
            gen.setLocation(location);
            
            generators.put(location, gen);
            playerGenerators.computeIfAbsent(gen.getOwner(), k -> new ArrayList<>()).add(gen);
        }
        plugin.getLogger().info("Loaded " + generators.size() + " generators");
    }
    
    public void onWorldLoad(World world) {
        String worldName = world.getName();
        List<PendingGenerator> pending = pendingRestorations.remove(worldName);
        
        if (pending != null && !pending.isEmpty()) {
            for (PendingGenerator pg : pending) {
                try {
                    Location location = new Location(world, pg.x, pg.y, pg.z);
                    pg.generator.setLocation(location);
                    generators.put(location, pg.generator);
                    playerGenerators.computeIfAbsent(pg.generator.getOwner(), k -> new ArrayList<>()).add(pg.generator);
                    restoreGeneratorBlock(pg.generator);
                } catch (Exception ignored) {}
            }
        }
    }
    
    public int restoreAllBlocks() {
        if (!plugin.getConfig().getBoolean("generators.force-restore-blocks", true)) return 0;
        int restored = 0;
        for (Generator gen : new ArrayList<>(generators.values())) {
            try { if (restoreGeneratorBlock(gen)) restored++; } catch (Exception ignored) {}
        }
        return restored;
    }
    
    public boolean restoreGeneratorBlock(Generator gen) {
        Location loc = gen.getLocation();
        World world = loc.getWorld();
        if (world == null) return false;
        
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) world.loadChunk(chunkX, chunkZ);
        
        Block block = loc.getBlock();
        String materialName = plugin.getConfigManager().getGeneratorMaterial(gen.getType());
        Material correctMaterial = Material.matchMaterial(materialName);
        
        if (correctMaterial == null) return false;
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
            if (loc.getWorld() != null && loc.getWorld().equals(chunk.getWorld()) &&
                (loc.getBlockX() >> 4) == chunk.getX() && (loc.getBlockZ() >> 4) == chunk.getZ()) {
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
            player.sendMessage(plugin.getConfigManager().getMessage("max-generators").replace("{max}", String.valueOf(max)));
            return false;
        }
        
        String id = UUID.randomUUID().toString();
        Generator gen = new Generator(id, player.getUniqueId(), location, type);
        
        String materialName = plugin.getConfigManager().getGeneratorMaterial(type);
        Material material = Material.matchMaterial(materialName);
        if (material != null) location.getBlock().setType(material, true);
        
        generators.put(location, gen);
        playerGenerators.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(gen);
        plugin.getDatabaseManager().saveGenerator(gen);
        
        if (plugin.getConfig().getBoolean("sounds.generator-place.enabled", true)) {
            try { player.playSound(location, Sound.valueOf(plugin.getConfig().getString("sounds.generator-place.sound", "ENTITY_PLAYER_LEVELUP")), 1.0f, 2.0f); } catch (Exception ignored) {}
        }
        
        player.sendMessage(plugin.getConfigManager().getMessage("generator-placed").replace("{current}", String.valueOf(current + 1)).replace("{max}", String.valueOf(max)));
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
                for (ItemStack item : leftover.values()) player.getWorld().dropItemNaturally(location, item);
                player.sendMessage(plugin.getConfigManager().colorize("&e&l⚠ &fInventory full! Generator dropped."));
            }
        }
        
        if (player != null && plugin.getConfig().getBoolean("sounds.generator-break.enabled", true)) {
            try { player.playSound(location, Sound.valueOf(plugin.getConfig().getString("sounds.generator-break.sound", "ENTITY_ITEM_PICKUP")), 1.0f, 1.0f); } catch (Exception ignored) {}
        }
        if (player != null) player.sendMessage(plugin.getConfigManager().getMessage("generator-removed"));
        return true;
    }
    
    public void tickGenerators() {
        for (Generator gen : new ArrayList<>(generators.values())) {
            org.bukkit.World world = gen.getLocation().getWorld();
            if (world == null) continue;
            if (!world.isChunkLoaded(gen.getLocation().getBlockX() >> 4, gen.getLocation().getBlockZ() >> 4)) continue;
            
            if (gen.isCorrupted()) continue; 

            UUID ownerUUID = gen.getOwner();
            PlayerGlobalGrid grid = plugin.getFuelManager().getGlobalGrid(ownerUUID);
            if (grid == null || grid.isOverCapacity()) continue;

            String type = gen.getType();
            int interval = plugin.getConfigManager().getGeneratorInterval(type);
            int jouleCost = plugin.getConfigManager().getGeneratorsConfig().getInt(type + ".joule-cost-per-drop", 1);

            if (!grid.hasPower(plugin, jouleCost)) continue;

            boolean onlineOnly = plugin.getConfig().getBoolean("generators.online-only", true);
            if (onlineOnly) {
                org.bukkit.entity.Player owner = org.bukkit.Bukkit.getPlayer(ownerUUID);
                if (owner == null || !owner.isOnline()) continue;
            }
            
            // PERBAIKAN MATEMATIKA SPEED EVENT: (Misal interval 50, speedReduction 50.0%) -> 50 * (1 - 0.5) = 25
            int adjustedInterval = interval;
            if (speedReductionPercent > 0) {
                adjustedInterval = (int) (interval * (1.0 - (speedReductionPercent / 100.0)));
            }
            if (adjustedInterval < 1) adjustedInterval = 1;
            
            if (gen.canDrop(adjustedInterval)) {
                dropItems(gen);
                grid.consumeGlobally(plugin, jouleCost); 
                gen.markDropped();
                plugin.getDatabaseManager().saveGenerator(gen);
            }
        }
    }
 
    private void dropItems(Generator gen) {
        String effectiveType = gen.getType();
        
        // PERBAIKAN: Mode MIXED UP EVENT (Acak Total)
        if (mixedUpMode) {
            List<String> allTypes = new ArrayList<>(plugin.getConfigManager().getAllGeneratorTypes());
            if (!allTypes.isEmpty()) {
                effectiveType = allTypes.get(new Random().nextInt(allTypes.size()));
            }
        } else if (tierBoost > 0) {
            for (int i = 0; i < tierBoost; i++) {
                String nextTier = plugin.getConfigManager().getGeneratorsConfig().getString(effectiveType + ".upgrade.next-tier", "none");
                if (!nextTier.equals("none") && !nextTier.equals("[]")) {
                    effectiveType = nextTier;
                }
            }
        }

        ConfigurationSection drops = plugin.getConfigManager().getGeneratorsConfig().getConfigurationSection(effectiveType + ".drops");
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
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (dropConfig.contains("display-name")) meta.setDisplayName(plugin.getConfigManager().colorize(dropConfig.getString("display-name")));
            if (dropConfig.contains("lore")) {
                List<String> lore = new ArrayList<>();
                for (String line : dropConfig.getStringList("lore")) lore.add(plugin.getConfigManager().colorize(line));
                meta.setLore(lore);
            }
            
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
                try { world.playSound(spawnLocation, Sound.valueOf(plugin.getConfig().getString("sounds.generator-drop.sound", "ENTITY_EXPERIENCE_ORB_PICKUP")), 0.5f, 1.5f); } catch (Exception ignored) {}
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
        if (player.hasPermission("seriagens.max.unlimited")) return Integer.MAX_VALUE;
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
        int extraSlots = plugin.getConfigManager().getPlayersConfig().getInt(player.getUniqueId().toString() + ".extra-slots", 0);
        return max + extraSlots;
    }
    
    public ItemStack getGeneratorItem(String type) {
        ConfigurationSection itemConfig = plugin.getConfigManager().getGeneratorsConfig().getConfigurationSection(type + ".item");
        if (itemConfig == null) return new ItemStack(Material.STONE);
        
        Material material = Material.matchMaterial(itemConfig.getString("material", "STONE"));
        ItemStack item = new ItemStack(material != null ? material : Material.STONE, itemConfig.getInt("amount", 1));
        ItemMeta meta = item.getItemMeta();
        
        if (itemConfig.contains("display-name")) meta.setDisplayName(plugin.getConfigManager().colorize(itemConfig.getString("display-name")));
        if (itemConfig.contains("lore")) {
            List<String> lore = new ArrayList<>();
            for (String line : itemConfig.getStringList("lore")) lore.add(plugin.getConfigManager().colorize(line));
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }
    
    public void saveAll() {
        for (Generator gen : generators.values()) plugin.getDatabaseManager().saveGenerator(gen);
    }
    public void saveAllSync() {
        for (Generator gen : generators.values()) plugin.getDatabaseManager().saveGeneratorSync(gen);
    }

    public void setDropMultiplier(int multiplier) { this.dropMultiplier = multiplier; }
    public void resetDropMultiplier() { this.dropMultiplier = 1; }
    // PERBAIKAN: Ubah Setter/Resetter agar sesuai dengan tipe data double
    public void setSpeedMultiplier(double reductionPercent) { this.speedReductionPercent = reductionPercent; }
    public void resetSpeedMultiplier() { this.speedReductionPercent = 0.0; }
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