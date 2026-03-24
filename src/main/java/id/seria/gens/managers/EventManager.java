package id.seria.gens.managers;

import id.seria.gens.SeriaGens;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages generator events
 * - Sell multiplier
 * - Drop multiplier
 * - Generator speed
 * - Generator upgrade
 * - Mixed up (random drops)
 */
public class EventManager {
    
    private final SeriaGens plugin;
    private final Map<String, GeneratorEvent> events;
    private GeneratorEvent activeEvent;
    private BukkitTask eventTask;
    private BukkitTask randomEventTask;
    
    public EventManager(SeriaGens plugin) {
        this.plugin = plugin;
        this.events = new HashMap<>();
        loadEvents();
        
        // Start random event scheduler if enabled
        if (plugin.getConfig().getBoolean("events.settings.random", false)) {
            startRandomEvents();
        }
    }
    
    /**
     * Load events from events.yml
     */
    private void loadEvents() {
        events.clear();
        
        ConfigurationSection eventsSection = plugin.getConfigManager().getEventsConfig().getConfigurationSection("events");
        
        if (eventsSection == null) {
            plugin.getLogger().warning("No events found in events.yml!");
            return;
        }
        
        for (String eventId : eventsSection.getKeys(false)) {
            ConfigurationSection eventConfig = eventsSection.getConfigurationSection(eventId);
            
            if (eventConfig == null) continue;
            
            String type = eventConfig.getString("type");
            String displayName = eventConfig.getString("display-name");
            int duration = eventConfig.getInt("duration");
            List<String> startMessages = eventConfig.getStringList("messages.start");
            List<String> endMessages = eventConfig.getStringList("messages.end");
            
            GeneratorEvent event;
            
            switch (type.toLowerCase()) {
                case "sell_multiplier":
                    double multiplier = eventConfig.getDouble("multiplier");
                    event = new SellMultiplierEvent(eventId, displayName, duration, startMessages, endMessages, multiplier);
                    break;
                    
                case "drop_multiplier":
                    int dropMult = eventConfig.getInt("multiplier");
                    event = new DropMultiplierEvent(eventId, displayName, duration, startMessages, endMessages, dropMult);
                    break;
                    
                case "generator_speed":
                    double speedReduction = eventConfig.getDouble("speed-reduction");
                    event = new GeneratorSpeedEvent(eventId, displayName, duration, startMessages, endMessages, speedReduction);
                    break;
                    
                case "generator_upgrade":
                    int tierBoost = eventConfig.getInt("tier-boost");
                    event = new GeneratorUpgradeEvent(eventId, displayName, duration, startMessages, endMessages, tierBoost);
                    break;
                    
                case "mixed_up":
                    event = new MixedUpEvent(eventId, displayName, duration, startMessages, endMessages);
                    break;
                    
                default:
                    plugin.getLogger().warning("Unknown event type: " + type);
                    continue;
            }
            
            events.put(eventId, event);
            plugin.getLogger().info("Loaded event: " + eventId + " (" + type + ")");
        }
        
        plugin.getLogger().info("Loaded " + events.size() + " events");
    }
    
    /**
     * Start an event
     */
    public boolean startEvent(String eventId) {
        if (activeEvent != null) {
            plugin.getLogger().warning("An event is already active!");
            return false;
        }
        
        GeneratorEvent event = events.get(eventId);
        
        if (event == null) {
            plugin.getLogger().warning("Event not found: " + eventId);
            return false;
        }
        
        // Start event
        activeEvent = event;
        event.start(plugin);
        
        // Broadcast start messages
        for (String message : event.getStartMessages()) {
            Bukkit.broadcastMessage(plugin.getConfigManager().colorize(message));
        }
        
        // Schedule event end
        eventTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            stopEvent();
        }, event.getDuration() * 20L);
        
        return true;
    }
    
    /**
     * Stop active event
     */
    public void stopEvent() {
        if (activeEvent == null) {
            return;
        }
        
        // Stop event
        activeEvent.stop(plugin);
        
        // Broadcast end messages
        for (String message : activeEvent.getEndMessages()) {
            Bukkit.broadcastMessage(plugin.getConfigManager().colorize(message));
        }
        
        activeEvent = null;
        
        if (eventTask != null) {
            eventTask.cancel();
            eventTask = null;
        }
    }
    
    /**
     * Start random event scheduler
     */
    private void startRandomEvents() {
        int waitTime = plugin.getConfigManager().getEventsConfig().getInt("settings.wait-time", 900);
        
        randomEventTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (activeEvent == null && !events.isEmpty()) {
                // Pick random event
                List<String> eventIds = new ArrayList<>(events.keySet());
                String randomEventId = eventIds.get(new Random().nextInt(eventIds.size()));
                
                startEvent(randomEventId);
            }
        }, waitTime * 20L, waitTime * 20L);
        
        plugin.getLogger().info("Random events enabled (interval: " + waitTime + "s)");
    }
    
    /**
     * Get active event
     */
    public GeneratorEvent getActiveEvent() {
        return activeEvent;
    }
    
    /**
     * Check if event is active
     */
    public boolean hasActiveEvent() {
        return activeEvent != null;
    }
    
    /**
     * Get all events
     */
    public Map<String, GeneratorEvent> getEvents() {
        return events;
    }
    
    /**
     * Reload events
     */
    public void reload() {
        stopEvent();
        
        if (randomEventTask != null) {
            randomEventTask.cancel();
            randomEventTask = null;
        }
        
        loadEvents();
        
        if (plugin.getConfigManager().getEventsConfig().getBoolean("settings.enabled", true) &&
            plugin.getConfigManager().getEventsConfig().getBoolean("settings.random", false)) {
            startRandomEvents();
        }
    }
    
    /**
     * Shutdown
     */
    public void shutdown() {
        stopEvent();
        
        if (randomEventTask != null) {
            randomEventTask.cancel();
        }
    }
    
    // ==========================================
    // EVENT CLASSES
    // ==========================================
    
    /**
     * Base event class
     */
    public abstract static class GeneratorEvent {
        protected final String id;
        protected final String displayName;
        protected final int duration;
        protected final List<String> startMessages;
        protected final List<String> endMessages;
        
        public GeneratorEvent(String id, String displayName, int duration, List<String> startMessages, List<String> endMessages) {
            this.id = id;
            this.displayName = displayName;
            this.duration = duration;
            this.startMessages = startMessages;
            this.endMessages = endMessages;
        }
        
        public abstract void start(SeriaGens plugin);
        public abstract void stop(SeriaGens plugin);
        public abstract String getType();
        
        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public int getDuration() { return duration; }
        public List<String> getStartMessages() { return startMessages; }
        public List<String> getEndMessages() { return endMessages; }
    }
    
    /**
     * Sell multiplier event
     */
    public static class SellMultiplierEvent extends GeneratorEvent {
        private final double multiplier;
        
        public SellMultiplierEvent(String id, String displayName, int duration, List<String> startMessages, List<String> endMessages, double multiplier) {
            super(id, displayName, duration, startMessages, endMessages);
            this.multiplier = multiplier;
        }
        
        @Override
        public void start(SeriaGens plugin) {
            plugin.getSellManager().setEventMultiplier(multiplier);
        }
        
        @Override
        public void stop(SeriaGens plugin) {
            plugin.getSellManager().resetEventMultiplier();
        }
        
        @Override
        public String getType() {
            return "sell_multiplier";
        }
        
        public double getMultiplier() {
            return multiplier;
        }
    }
    
    /**
     * Drop multiplier event
     */
    public static class DropMultiplierEvent extends GeneratorEvent {
        private final int multiplier;
        
        public DropMultiplierEvent(String id, String displayName, int duration, List<String> startMessages, List<String> endMessages, int multiplier) {
            super(id, displayName, duration, startMessages, endMessages);
            this.multiplier = multiplier;
        }
        
        @Override
        public void start(SeriaGens plugin) {
            plugin.getGeneratorManager().setDropMultiplier(multiplier);
        }
        
        @Override
        public void stop(SeriaGens plugin) {
            plugin.getGeneratorManager().resetDropMultiplier();
        }
        
        @Override
        public String getType() {
            return "drop_multiplier";
        }
        
        public int getMultiplier() {
            return multiplier;
        }
    }
    
    /**
     * Generator speed event
     */
    public static class GeneratorSpeedEvent extends GeneratorEvent {
        private final double speedReduction;
        
        public GeneratorSpeedEvent(String id, String displayName, int duration, List<String> startMessages, List<String> endMessages, double speedReduction) {
            super(id, displayName, duration, startMessages, endMessages);
            this.speedReduction = speedReduction;
        }
        
        @Override
        public void start(SeriaGens plugin) {
            plugin.getGeneratorManager().setSpeedMultiplier(speedReduction);
        }
        
        @Override
        public void stop(SeriaGens plugin) {
            plugin.getGeneratorManager().resetSpeedMultiplier();
        }
        
        @Override
        public String getType() {
            return "generator_speed";
        }
        
        public double getSpeedReduction() {
            return speedReduction;
        }
    }
    
    /**
     * Generator upgrade event
     */
    public static class GeneratorUpgradeEvent extends GeneratorEvent {
        private final int tierBoost;
        
        public GeneratorUpgradeEvent(String id, String displayName, int duration, List<String> startMessages, List<String> endMessages, int tierBoost) {
            super(id, displayName, duration, startMessages, endMessages);
            this.tierBoost = tierBoost;
        }
        
        @Override
        public void start(SeriaGens plugin) {
            plugin.getGeneratorManager().setTierBoost(tierBoost);
        }
        
        @Override
        public void stop(SeriaGens plugin) {
            plugin.getGeneratorManager().resetTierBoost();
        }
        
        @Override
        public String getType() {
            return "generator_upgrade";
        }
        
        public int getTierBoost() {
            return tierBoost;
        }
    }
    
    /**
     * Mixed up event (random drops)
     */
    public static class MixedUpEvent extends GeneratorEvent {
        
        public MixedUpEvent(String id, String displayName, int duration, List<String> startMessages, List<String> endMessages) {
            super(id, displayName, duration, startMessages, endMessages);
        }
        
        @Override
        public void start(SeriaGens plugin) {
            plugin.getGeneratorManager().setMixedUpMode(true);
        }
        
        @Override
        public void stop(SeriaGens plugin) {
            plugin.getGeneratorManager().setMixedUpMode(false);
        }
        
        @Override
        public String getType() {
            return "mixed_up";
        }
    }
}