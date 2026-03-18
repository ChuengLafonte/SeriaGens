package com.yourserver.bentogens.models;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public class Generator {
    
    private final String id;
    private final UUID owner;
    private Location location;
    private String type;
    private long lastDrop;
    private final long placedAt;
    private boolean corrupted;  // NEW: Corruption status
    private long lastCorruptionCheck;  // NEW: Last time checked for corruption
    
    public Generator(String id, UUID owner, Location location, String type) {
        this.id = id;
        this.owner = owner;
        this.location = location;
        this.type = type;
        this.lastDrop = System.currentTimeMillis();
        this.placedAt = System.currentTimeMillis();
        this.corrupted = false;
        this.lastCorruptionCheck = System.currentTimeMillis();
    }
    
    /**
     * Full constructor with all fields (for database loading)
     */
    public Generator(String id, UUID owner, Location location, String type, 
                    long lastDrop, long placedAt, boolean corrupted, long lastCorruptionCheck) {
        this.id = id;
        this.owner = owner;
        this.location = location;
        this.type = type;
        this.lastDrop = lastDrop;
        this.placedAt = placedAt;
        this.corrupted = corrupted;
        this.lastCorruptionCheck = lastCorruptionCheck;
    }
    
    /**
     * Check if generator can drop now based on interval
     * Returns false if corrupted!
     */
    public boolean canDrop(int intervalSeconds) {
        if (corrupted) {
            return false; // Corrupted generators don't drop!
        }
        
        long elapsed = System.currentTimeMillis() - lastDrop;
        return elapsed >= (intervalSeconds * 1000L);
    }
    
    /**
     * Mark that generator just dropped
     */
    public void markDropped() {
        this.lastDrop = System.currentTimeMillis();
    }
    
    /**
     * Check if generator should be tested for corruption
     */
    public boolean shouldCheckCorruption(long intervalMinutes) {
        long elapsed = System.currentTimeMillis() - lastCorruptionCheck;
        return elapsed >= (intervalMinutes * 60 * 1000L);
    }
    
    /**
     * Mark corruption check time
     */
    public void markCorruptionCheck() {
        this.lastCorruptionCheck = System.currentTimeMillis();
    }
    
    /**
     * Get location string for database storage
     */
    public String getLocationString() {
        World world = location.getWorld();
        if (world == null) return "";
        
        return world.getName() + "," + 
               location.getBlockX() + "," + 
               location.getBlockY() + "," + 
               location.getBlockZ();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public UUID getOwner() {
        return owner;
    }
    
    public Location getLocation() {
        return location;
    }
    
    public void setLocation(Location location) {
        this.location = location;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public long getLastDrop() {
        return lastDrop;
    }
    
    public long getPlacedAt() {
        return placedAt;
    }
    
    public boolean isCorrupted() {
        return corrupted;
    }
    
    public void setCorrupted(boolean corrupted) {
        this.corrupted = corrupted;
    }
    
    public long getLastCorruptionCheck() {
        return lastCorruptionCheck;
    }
    
    public void setLastCorruptionCheck(long lastCorruptionCheck) {
        this.lastCorruptionCheck = lastCorruptionCheck;
    }
    
    @Override
    public String toString() {
        return "Generator{" +
                "id='" + id + '\'' +
                ", owner=" + owner +
                ", location=" + getLocationString() +
                ", type='" + type + '\'' +
                ", corrupted=" + corrupted +
                '}';
    }
}