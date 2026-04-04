package id.seria.gens.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import id.seria.gens.SeriaGens;

public class ConfigManager {
    
    private final SeriaGens plugin;
    private FileConfiguration generatorsConfig;
    private FileConfiguration eventsConfig;
    private FileConfiguration guiConfig;
    private FileConfiguration commandConfig;
    private FileConfiguration playersConfig;
    private File playersFile;
    
    private static final Pattern HEX_PATTERN = Pattern.compile("(?:&#|#)([A-Fa-f0-9]{6})");
    
    public ConfigManager(SeriaGens plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfigs() {
        plugin.reloadConfig();
        loadGeneratorsConfig();
        loadEventsConfig();
        loadGuiConfig();
        loadCommandConfig();
        loadPlayersConfig(); // Tambahkan baris ini
        plugin.getLogger().info("Semua file konfigurasi berhasil dimuat!");
    }
    
    private void loadGeneratorsConfig() {
        File file = new File(plugin.getDataFolder(), "generators.yml");
        if (!file.exists()) plugin.saveResource("generators.yml", false);
        generatorsConfig = YamlConfiguration.loadConfiguration(file);
    }
    
    private void loadEventsConfig() {
        File file = new File(plugin.getDataFolder(), "events.yml");
        if (!file.exists()) plugin.saveResource("events.yml", false);
        eventsConfig = YamlConfiguration.loadConfiguration(file);
    }

    private void loadGuiConfig() {
        File file = new File(plugin.getDataFolder(), "gui.yml");
        if (!file.exists()) plugin.saveResource("gui.yml", false);
        guiConfig = YamlConfiguration.loadConfiguration(file);
    }

    private void loadCommandConfig() {
        File file = new File(plugin.getDataFolder(), "command.yml");
        if (!file.exists()) plugin.saveResource("command.yml", false);
        commandConfig = YamlConfiguration.loadConfiguration(file);
    }
    private void loadPlayersConfig() {
        playersFile = new File(plugin.getDataFolder(), "players.yml");
        if (!playersFile.exists()) {
            try { playersFile.createNewFile(); } catch (Exception ignored) {}
        }
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
    }
    
    public FileConfiguration getPlayersConfig() { return playersConfig; }
    
    public void savePlayersConfig() {
        try { playersConfig.save(playersFile); } catch (Exception e) {
            plugin.getLogger().warning("Gagal menyimpan players.yml!");
        }
    }
    
    public FileConfiguration getGeneratorsConfig() { return generatorsConfig; }
    public FileConfiguration getEventsConfig() { return eventsConfig; }
    public FileConfiguration getGuiConfig() { return guiConfig; }
    public FileConfiguration getCommandConfig() { return commandConfig; }
    
    public Set<String> getAllGeneratorTypes() { return generatorsConfig.getKeys(false); }
    public String getGeneratorMaterial(String type) { return generatorsConfig.getString(type + ".item.material", "STONE"); }
    public int getGeneratorInterval(String type) { return generatorsConfig.getInt(type + ".interval", 20); }
    public String getGeneratorDisplayName(String type) { return colorize(generatorsConfig.getString(type + ".display-name", type)); }
    public boolean generatorExists(String type) { return generatorsConfig.contains(type); }
    
    public String getMessage(String key) {
        String msg = commandConfig.getString("messages." + key);
        if (msg == null) return colorize("&c[SeriaGens] Pesan tidak ditemukan: " + key);
        return colorize(msg.replace("{prefix}", commandConfig.getString("messages.prefix", "&6[SeriaGens]")));
    }
    
    @SuppressWarnings("deprecation")
    public String colorize(String text) {
        if (text == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + hex).toString());
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
    
    public List<String> colorize(List<String> list) {
        List<String> colored = new ArrayList<>();
        for (String line : list) { colored.add(colorize(line)); }
        return colored;
    }
}