package id.seria.gens.models;

import java.util.UUID;

import org.bukkit.Location;

public class Generator {
    
    private final String id;
    private final UUID owner;
    private Location location;
    private String type;
    private long lastDrop;
    private final long placedAt;
    private boolean corrupted;
    private long lastCorruptionCheck;
    
    // Constructor saat pertama dipasang
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
    
    // Constructor saat load dari DB (Tanpa Kolom Fuel)
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
    
    // --- METHOD GETTER/SETTER FUNDAMENTAL ---
    public boolean canDrop(long intervalSeconds) {
        return System.currentTimeMillis() - lastDrop >= (intervalSeconds * 1000);
    }
    
    public void markDropped() {
        this.lastDrop = System.currentTimeMillis();
    }
    
    public boolean shouldCheckCorruption(long intervalMinutes) {
        return System.currentTimeMillis() - lastCorruptionCheck >= (intervalMinutes * 60 * 1000);
    }
    
    public void markCorruptionCheck() {
        this.lastCorruptionCheck = System.currentTimeMillis();
    }
    
    public String getLocationString() {
        if (location == null || location.getWorld() == null) return "unknown";
        return location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }
    
    public String getId() { return id; }
    public UUID getOwner() { return owner; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public long getLastDrop() { return lastDrop; }
    public long getPlacedAt() { return placedAt; }
    public boolean isCorrupted() { return corrupted; }
    public void setCorrupted(boolean corrupted) { this.corrupted = corrupted; }
    public long getLastCorruptionCheck() { return lastCorruptionCheck; }
    public void setLastCorruptionCheck(long lastCorruptionCheck) { this.lastCorruptionCheck = lastCorruptionCheck; }
}