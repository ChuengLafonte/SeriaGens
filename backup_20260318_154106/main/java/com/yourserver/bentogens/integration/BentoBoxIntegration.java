package com.yourserver.bentogens.integration;

import com.yourserver.bentogens.BentoGens;
import com.yourserver.bentogens.models.Generator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * BentoBox integration using reflection
 * Works without BentoBox JAR during compilation!
 */
public class BentoBoxIntegration implements Listener {
    
    private final BentoGens plugin;
    private final Object bentoBox;
    private final Class<?> islandEventClass;
    private final Class<?> islandDeleteEventClass;
    private final Class<?> teamLeaveEventClass;
    private final Class<?> islandClass;
    
    public BentoBoxIntegration(BentoGens plugin, Object bentoBox) {
        this.plugin = plugin;
        this.bentoBox = bentoBox;
        
        try {
            // Load BentoBox classes dynamically
            this.islandEventClass = Class.forName("world.bentobox.bentobox.api.events.island.IslandEvent");
            this.islandDeleteEventClass = Class.forName("world.bentobox.bentobox.api.events.island.IslandDeleteEvent");
            this.teamLeaveEventClass = Class.forName("world.bentobox.bentobox.api.events.team.TeamLeaveEvent");
            this.islandClass = Class.forName("world.bentobox.bentobox.database.objects.Island");
            
            plugin.getLogger().info("BentoBox integration initialized via reflection!");
            
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to initialize BentoBox integration", e);
        }
    }
    
    /**
     * Check if player can place generator at location
     */
    public boolean canPlaceAt(Player player, Location location) {
        try {
            // Get island at location
            Object island = getIslandAt(location);
            
            if (island == null) {
                return false;
            }
            
            // Check if player is owner or member
            UUID owner = getIslandOwner(island);
            
            if (owner != null && owner.equals(player.getUniqueId())) {
                return true;
            }
            
            // Check if player is member
            return isIslandMember(island, player.getUniqueId());
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking BentoBox permission: " + e.getMessage());
            return true; // Allow on error
        }
    }
    
    /**
     * Handle island events using reflection
     */
    @EventHandler
    public void onIslandEvent(org.bukkit.event.Event event) {
        try {
            // Check if this is an island delete event
            if (islandDeleteEventClass.isInstance(event)) {
                handleIslandDelete(event);
            }
            // Check if this is an island reset event
            else if (islandEventClass.isInstance(event)) {
                handleIslandReset(event);
            }
            // Check if this is a team leave event
            else if (teamLeaveEventClass.isInstance(event)) {
                handleTeamLeave(event);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling BentoBox event: " + e.getMessage());
        }
    }
    
    private void handleIslandDelete(Object event) throws Exception {
        if (!plugin.getConfig().getBoolean("bentobox.remove-on-delete", true)) {
            return;
        }
        
        Method getIslandMethod = event.getClass().getMethod("getIsland");
        Object island = getIslandMethod.invoke(event);
        
        if (island == null) return;
        
        // Remove all generators on island
        List<Generator> generators = getGeneratorsOnIsland(island);
        
        for (Generator gen : generators) {
            plugin.getGeneratorManager().removeGenerator(gen.getLocation(), null);
        }
        
        if (!generators.isEmpty()) {
            plugin.getLogger().info("Removed " + generators.size() + " generators from deleted island");
        }
    }
    
    private void handleIslandReset(Object event) throws Exception {
        Method getReasonMethod = event.getClass().getMethod("getReason");
        Object reason = getReasonMethod.invoke(event);
        
        if (reason == null || !reason.toString().equals("RESETTED")) {
            return;
        }
        
        if (!plugin.getConfig().getBoolean("bentobox.clear-on-reset", true)) {
            return;
        }
        
        Method getIslandMethod = event.getClass().getMethod("getIsland");
        Object island = getIslandMethod.invoke(event);
        
        UUID owner = getIslandOwner(island);
        if (owner == null) return;
        
        // Get generators and return items
        List<Generator> generators = getGeneratorsOnIsland(island);
        
        if (generators.isEmpty()) return;
        
        boolean returnItems = plugin.getConfig().getBoolean("bentobox.return-on-reset", true);
        
        if (returnItems) {
            Player player = Bukkit.getPlayer(owner);
            if (player != null && player.isOnline()) {
                for (Generator gen : generators) {
                    ItemStack item = plugin.getGeneratorManager().getGeneratorItem(gen.getType());
                    player.getInventory().addItem(item);
                }
                
                String msg = plugin.getConfigManager().getMessage("generators-returned")
                    .replace("{amount}", String.valueOf(generators.size()));
                player.sendMessage(msg);
            }
        }
        
        // Remove generators
        for (Generator gen : generators) {
            plugin.getGeneratorManager().removeGenerator(gen.getLocation(), null);
        }
        
        plugin.getLogger().info("Handled " + generators.size() + " generators on island reset");
    }
    
    private void handleTeamLeave(Object event) throws Exception {
        if (!plugin.getConfig().getBoolean("bentobox.return-on-leave", true)) {
            return;
        }
        
        Method getPlayerUUIDMethod = event.getClass().getMethod("getPlayerUUID");
        UUID playerUUID = (UUID) getPlayerUUIDMethod.invoke(event);
        
        Method getIslandMethod = event.getClass().getMethod("getIsland");
        Object island = getIslandMethod.invoke(event);
        
        if (island == null) return;
        
        // Get player's generators on island
        List<Generator> playerGens = new ArrayList<>();
        
        for (Generator gen : plugin.getGeneratorManager().getAllGenerators()) {
            if (gen.getOwner().equals(playerUUID) && isOnIsland(gen.getLocation(), island)) {
                playerGens.add(gen);
            }
        }
        
        if (playerGens.isEmpty()) return;
        
        // Return items
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            for (Generator gen : playerGens) {
                ItemStack item = plugin.getGeneratorManager().getGeneratorItem(gen.getType());
                player.getInventory().addItem(item);
            }
            
            String msg = plugin.getConfigManager().getMessage("generators-returned")
                .replace("{amount}", String.valueOf(playerGens.size()));
            player.sendMessage(msg);
        }
        
        // Remove generators
        for (Generator gen : playerGens) {
            plugin.getGeneratorManager().removeGenerator(gen.getLocation(), null);
        }
    }
    
    private Object getIslandAt(Location location) throws Exception {
        Method getIslandsMethod = bentoBox.getClass().getMethod("getIslands");
        Object islands = getIslandsMethod.invoke(bentoBox);
        
        Method getIslandAtMethod = islands.getClass().getMethod("getIslandAt", Location.class);
        Object optional = getIslandAtMethod.invoke(islands, location);
        
        Method isPresentMethod = Optional.class.getMethod("isPresent");
        boolean isPresent = (boolean) isPresentMethod.invoke(optional);
        
        if (!isPresent) {
            return null;
        }
        
        Method getMethod = Optional.class.getMethod("get");
        return getMethod.invoke(optional);
    }
    
    private UUID getIslandOwner(Object island) throws Exception {
        Method getOwnerMethod = island.getClass().getMethod("getOwner");
        return (UUID) getOwnerMethod.invoke(island);
    }
    
    private boolean isIslandMember(Object island, UUID player) throws Exception {
        Method getMemberSetMethod = island.getClass().getMethod("getMemberSet");
        Object memberSet = getMemberSetMethod.invoke(island);
        
        Method containsMethod = memberSet.getClass().getMethod("contains", Object.class);
        return (boolean) containsMethod.invoke(memberSet, player);
    }
    
    private boolean isOnIsland(Location location, Object island) throws Exception {
        Method onIslandMethod = island.getClass().getMethod("onIsland", Location.class);
        return (boolean) onIslandMethod.invoke(island, location);
    }
    
    private List<Generator> getGeneratorsOnIsland(Object island) throws Exception {
        List<Generator> result = new ArrayList<>();
        
        for (Generator gen : plugin.getGeneratorManager().getAllGenerators()) {
            if (isOnIsland(gen.getLocation(), island)) {
                result.add(gen);
            }
        }
        
        return result;
    }
}