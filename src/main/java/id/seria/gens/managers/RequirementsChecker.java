package id.seria.gens.managers;

import id.seria.gens.SeriaGens;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RequirementsChecker {
    
    private final SeriaGens plugin;
    
    public RequirementsChecker(SeriaGens plugin) {
        this.plugin = plugin;
    }
    
    public RequirementResult checkRequirements(Player player, String generatorType, RequirementType type) {
        ConfigurationSection genConfig = plugin.getConfigManager()
            .getGeneratorsConfig()
            .getConfigurationSection(generatorType);
        
        if (genConfig == null) return new RequirementResult(true, null);
        
        String sectionName = type == RequirementType.PLACE ? "place-requirements" : "upgrade-requirements";
        ConfigurationSection requirements = genConfig.getConfigurationSection(sectionName);
        
        if (requirements == null) return new RequirementResult(true, null);
        
        List<String> failedMessages = new ArrayList<>();
        
        for (String key : requirements.getKeys(false)) {
            ConfigurationSection req = requirements.getConfigurationSection(key);
            if (req == null) continue;
            
            String reqType = req.getString("type", "PERMISSION").toUpperCase();
            boolean passed = true;
            
            switch (reqType) {
                case "PERMISSION":
                    String permission = req.getString("permission");
                    if (permission != null && !player.hasPermission(permission)) passed = false;
                    break;
                    
                case "PLACEHOLDER":
                    String placeholder = req.getString("placeholder");
                    String expectedValueStr = req.getString("value");
                    String operator = req.getString("operator", ">=");
                    
                    if (placeholder != null && expectedValueStr != null) {
                        String actualValueStr = PlaceholderAPI.setPlaceholders(player, placeholder);
                        try {
                            double actual = Double.parseDouble(actualValueStr);
                            double expected = Double.parseDouble(expectedValueStr);
                            passed = checkOperator(actual, expected, operator);
                        } catch (NumberFormatException e) {
                            passed = actualValueStr.equals(expectedValueStr);
                        }
                    }
                    break;
                    
                case "MONEY":
                    if (plugin.getEconomy() != null) {
                        double amount = req.getDouble("amount", 0);
                        if (plugin.getEconomy().getBalance(player) < amount) passed = false;
                    }
                    break;
            }
            
            if (!passed) {
                String message = req.getString("message");
                if (message != null) {
                    failedMessages.add(plugin.getConfigManager().colorize(message));
                }
                return new RequirementResult(false, failedMessages);
            }
        }
        
        return new RequirementResult(true, null);
    }
    
    private boolean checkOperator(double actual, double required, String operator) {
        switch (operator) {
            case ">=": return actual >= required;
            case ">": return actual > required;
            case "<=": return actual <= required;
            case "<": return actual < required;
            case "==":
            case "=": return actual == required;
            default:
                plugin.getLogger().warning("Unknown operator: " + operator);
                return false;
        }
    }
    
    public enum RequirementType {
        PLACE,
        UPGRADE
    }
    
    public static class RequirementResult {
        private final boolean passed;
        private final List<String> messages;
        
        public RequirementResult(boolean passed, List<String> messages) {
            this.passed = passed;
            this.messages = messages;
        }
        
        public boolean hasPassed() { return passed; }
        public List<String> getMessages() { return messages != null ? messages : new ArrayList<>(); }
        
        public void sendMessages(Player player) {
            if (messages != null) {
                for (String msg : messages) {
                    player.sendMessage(msg);
                }
            }
        }
    }
}