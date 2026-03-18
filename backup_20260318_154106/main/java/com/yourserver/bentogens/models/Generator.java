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
    
    public Generator(String id, UUID owner, Location location, String type) {
        this.id = id;
        this.owner = owner;
        this.location = location;
        this.type = type;
        this.lastDrop = System.currentTimeMillis();
        this.placedAt = System.currentTimeMillis();
    }
    
    /**
     * Check if generator can drop now based on interval
     */
    public boolean canDrop(int intervalSeconds) {
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
    
    @Override
    public String toString() {
        return "Generator{" +
                "id='" + id + '\'' +
                ", owner=" + owner +
                ", location=" + getLocationString() +
                ", type='" + type + '\'' +
                '}';
    }
}