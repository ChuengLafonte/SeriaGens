package com.yourserver.bentogens.managers;

import com.yourserver.bentogens.BentoGens;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks requirements for generator placement and upgrades
 * Supports: PLACEHOLDER (AuraSkills, etc), PERMISSION (LuckPerms), MONEY (Vault)
 */
public class RequirementsChecker {
    
    private final BentoGens plugin;
    
    public RequirementsChecker(BentoGens plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Check all requirements for a generator type
     */
    public RequirementResult checkRequirements(Player player, String generatorType, RequirementType type) {
        ConfigurationSection genConfig = plugin.getConfigManager()
            .getGeneratorsConfig()
            .getConfigurationSection(generatorType);
        
        if (genConfig == null) {
            return new RequirementResult(true, null);
        }
        
        String sectionName = type == RequirementType.PLACE ? "place-requirements" : "upgrade-requirements";
        ConfigurationSection requirements = genConfig.getConfigurationSection(sectionName);
        
        if (requirements == null) {
            return new RequirementResult(true, null);
        }
        
        List<String> failedMessages = new ArrayList<>();
        
        for (String key : requirements.getKeys(false)) {
            ConfigurationSection req = requirements.getConfigurationSection(key);
            
            if (req == null) continue;
            
            String reqType = req.getString("type", "PLACEHOLDER");
            
            boolean passed = false;
            
            switch (reqType.toUpperCase()) {
                case "PLACEHOLDER":
                    passed = checkPlaceholder(player, req);
                    break;
                    
                case "PERMISSION":
                    passed = checkPermission(player, req);
                    break;
                    
                case "MONEY":
                    passed = checkMoney(player, req);
                    break;
                    
                default:
                    plugin.getLogger().warning("Unknown requirement type: " + reqType);
                    continue;
            }
            
            if (!passed) {
                String message = req.getString("message", "&cRequirement not met!");
                failedMessages.add(plugin.getConfigManager().colorize(message));
            }
        }
        
        if (failedMessages.isEmpty()) {
            return new RequirementResult(true, null);
        } else {
            return new RequirementResult(false, failedMessages);
        }
    }
    
    /**
     * Check placeholder requirement (for AuraSkills, etc)
     */
    private boolean checkPlaceholder(Player player, ConfigurationSection req) {
        // Check if PlaceholderAPI is available
        if (!plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            plugin.getLogger().warning("PlaceholderAPI not found! Placeholder requirements will fail.");
            return false;
        }
        
        String placeholder = req.getString("placeholder");
        String requiredValue = req.getString("value");
        String operator = req.getString("operator", ">="); // >=, >, <=, <, ==
        
        if (placeholder == null || requiredValue == null) {
            plugin.getLogger().warning("Invalid placeholder requirement config!");
            return false;
        }
        
        // Parse placeholder
        String parsedValue = PlaceholderAPI.setPlaceholders(player, placeholder);
        
        // Try to compare as numbers
        try {
            double actualValue = Double.parseDouble(parsedValue);
            double required = Double.parseDouble(requiredValue);
            
            return compareNumbers(actualValue, required, operator);
            
        } catch (NumberFormatException e) {
            // Compare as strings
            return parsedValue.equalsIgnoreCase(requiredValue);
        }
    }
    
    /**
     * Check permission requirement (for LuckPerms ranks, etc)
     */
    private boolean checkPermission(Player player, ConfigurationSection req) {
        String permission = req.getString("permission");
        
        if (permission == null) {
            plugin.getLogger().warning("Invalid permission requirement config!");
            return false;
        }
        
        return player.hasPermission(permission);
    }
    
    /**
     * Check money requirement (Vault balance)
     */
    private boolean checkMoney(Player player, ConfigurationSection req) {
        if (plugin.getEconomy() == null) {
            plugin.getLogger().warning("Economy not available for money requirement!");
            return false;
        }
        
        double required = req.getDouble("amount", 0.0);
        double balance = plugin.getEconomy().getBalance(player);
        
        return balance >= required;
    }
    
    /**
     * Compare two numbers based on operator
     */
    private boolean compareNumbers(double actual, double required, String operator) {
        switch (operator) {
            case ">=":
                return actual >= required;
            case ">":
                return actual > required;
            case "<=":
                return actual <= required;
            case "<":
                return actual < required;
            case "==":
            case "=":
                return actual == required;
            default:
                plugin.getLogger().warning("Unknown operator: " + operator);
                return false;
        }
    }
    
    /**
     * Requirement types
     */
    public enum RequirementType {
        PLACE,
        UPGRADE
    }
    
    /**
     * Requirement check result
     */
    public static class RequirementResult {
        private final boolean passed;
        private final List<String> messages;
        
        public RequirementResult(boolean passed, List<String> messages) {
            this.passed = passed;
            this.messages = messages;
        }
        
        public boolean hasPassed() {
            return passed;
        }
        
        public List<String> getMessages() {
            return messages != null ? messages : new ArrayList<>();
        }
        
        public void sendMessages(Player player) {
            if (messages != null) {
                for (String msg : messages) {
                    player.sendMessage(msg);
                }
            }
        }
    }
}