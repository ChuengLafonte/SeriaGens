package id.seria.gens.managers;

import id.seria.gens.SeriaGens;
import id.seria.gens.models.Generator;
import id.seria.gens.utils.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {
    
    private final SeriaGens plugin;
    private Connection connection;
    private final String type;
    private volatile boolean isClosing = false;
    
    public DatabaseManager(SeriaGens plugin) {
        this.plugin = plugin;
        this.type = plugin.getConfig().getString("database.type", "SQLITE");
    }
    
    public void initialize() {
        try {
            if (type.equalsIgnoreCase("MYSQL")) {
                connectMySQL();
            } else {
                connectSQLite();
            }
            createTables();
            if (!verifyDatabaseIntegrity()) {
                plugin.getLogger().warning("Database integrity check failed! Attempting recovery...");
                recoverDatabase();
            }
            plugin.getLogger().info("Database initialized (" + type + ")");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            tryRecovery();
        }
    }
    
    private void connectSQLite() throws SQLException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        
        String path = dataFolder.getAbsolutePath() + File.separator + "database.db";
        String url = "jdbc:sqlite:" + path;
        connection = DriverManager.getConnection(url);
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA synchronous=NORMAL;");
            plugin.getLogger().info("SQLite WAL mode enabled");
        }
        plugin.getLogger().info("Connected to SQLite: " + path);
    }
    
    private void connectMySQL() throws SQLException {
        String host = plugin.getConfig().getString("database.mysql.host");
        int port = plugin.getConfig().getInt("database.mysql.port");
        String database = plugin.getConfig().getString("database.mysql.database");
        String username = plugin.getConfig().getString("database.mysql.user");
        String password = plugin.getConfig().getString("database.mysql.password");
        boolean useSSL = plugin.getConfig().getBoolean("database.mysql.useSSL", false);
        
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database 
                   + "?useSSL=" + useSSL 
                   + "&autoReconnect=true";
        
        connection = DriverManager.getConnection(url, username, password);
        plugin.getLogger().info("Connected to MySQL: " + host + ":" + port);
    }
    
    private void createTables() throws SQLException {
        String sqlGens = "CREATE TABLE IF NOT EXISTS generators ("
                + "id VARCHAR(36) PRIMARY KEY,"
                + "owner VARCHAR(36) NOT NULL,"
                + "world VARCHAR(100) NOT NULL,"
                + "x INT NOT NULL,"
                + "y INT NOT NULL,"
                + "z INT NOT NULL,"
                + "type VARCHAR(50) NOT NULL,"
                + "last_drop BIGINT,"
                + "placed_at BIGINT,"
                + "corrupted BOOLEAN DEFAULT 0,"
                + "last_corruption_check BIGINT"
                + ")";
                
        // TABEL BARU UNTUK GARDU INDUK GLOBAL
        String sqlGlobalFuel = "CREATE TABLE IF NOT EXISTS player_global_fuel ("
                + "player_uuid VARCHAR(36) PRIMARY KEY,"
                + "current_joules INT DEFAULT 0,"
                + "fuel_data TEXT"
                + ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sqlGens);
            stmt.execute(sqlGlobalFuel);
            
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_owner ON generators(owner)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_location ON generators(world, x, y, z)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_corrupted ON generators(corrupted)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_global_fuel ON player_global_fuel(player_uuid)");
        }
    }
    
    private boolean verifyDatabaseIntegrity() {
        if (type.equalsIgnoreCase("SQLITE") && connection != null) {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("PRAGMA integrity_check")) {
                if (rs.next()) {
                    return "ok".equalsIgnoreCase(rs.getString(1));
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Integrity check failed: " + e.getMessage());
            }
        }
        return true;
    }
    
    private void recoverDatabase() {
        if (type.equalsIgnoreCase("SQLITE")) {
            try {
                File dbFile = new File(plugin.getDataFolder(), "database.db");
                File backupFile = new File(plugin.getDataFolder(), "database.db.corrupt");
                
                if (dbFile.exists()) {
                    if (connection != null && !connection.isClosed()) connection.close();
                    dbFile.renameTo(backupFile);
                    plugin.getLogger().warning("Corrupt database backed up");
                    
                    connectSQLite();
                    createTables();
                    plugin.getLogger().info("New database created!");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Recovery failed: " + e.getMessage());
            }
        }
    }
    
    private void tryRecovery() {
        try {
            Thread.sleep(1000);
            recoverDatabase();
            if (type.equalsIgnoreCase("SQLITE")) {
                connectSQLite();
            } else {
                connectMySQL();
            }
            createTables();
            plugin.getLogger().info("Database recovery successful!");
        } catch (Exception e) {
            plugin.getLogger().severe("Recovery failed: " + e.getMessage());
        }
    }
    
    // --- METHOD GENERATORS ---
    
    public CompletableFuture<Void> saveGenerator(Generator generator) {
        return CompletableFuture.runAsync(() -> saveGeneratorSync(generator));
    }
    
    public void saveGeneratorSync(Generator generator) {
        if (isClosing || connection == null) return;
        
        String sql = type.equalsIgnoreCase("MYSQL") 
            ? "REPLACE INTO generators (id, owner, world, x, y, z, type, last_drop, placed_at, corrupted, last_corruption_check) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            : "INSERT OR REPLACE INTO generators (id, owner, world, x, y, z, type, last_drop, placed_at, corrupted, last_corruption_check) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            Location loc = generator.getLocation();
            stmt.setString(1, generator.getId());
            stmt.setString(2, generator.getOwner().toString());
            stmt.setString(3, loc.getWorld().getName());
            stmt.setInt(4, loc.getBlockX());
            stmt.setInt(5, loc.getBlockY());
            stmt.setInt(6, loc.getBlockZ());
            stmt.setString(7, generator.getType());
            stmt.setLong(8, generator.getLastDrop());
            stmt.setLong(9, generator.getPlacedAt());
            stmt.setBoolean(10, generator.isCorrupted());
            stmt.setLong(11, generator.getLastCorruptionCheck());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save generator: " + e.getMessage());
        }
    }
    
    public List<Generator> loadAllGenerators() {
        List<Generator> generators = new ArrayList<>();
        if (connection == null) return generators;
        
        String sql = "SELECT * FROM generators";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                try {
                    String id = rs.getString("id");
                    UUID owner = UUID.fromString(rs.getString("owner"));
                    String worldName = rs.getString("world");
                    World world = Bukkit.getWorld(worldName);
                    
                    if (world == null) continue;
                    
                    Location location = new Location(world, rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
                    String type = rs.getString("type");
                    long lastDrop = rs.getLong("last_drop");
                    long placedAt = rs.getLong("placed_at");
                    boolean corrupted = rs.getBoolean("corrupted");
                    long lastCorruptionCheck = rs.getLong("last_corruption_check");
                    
                    Generator gen = new Generator(id, owner, location, type, lastDrop, placedAt, corrupted, lastCorruptionCheck);
                    generators.add(gen);
                    
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load generator: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load generators: " + e.getMessage());
        }
        return generators;
    }
    
    public CompletableFuture<Void> deleteGenerator(String id) {
        return CompletableFuture.runAsync(() -> {
            if (connection == null) return;
            String sql = "DELETE FROM generators WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, id);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete generator: " + e.getMessage());
            }
        });
    }
    
    // --- METHOD GARDU INDUK GLOBAL FUEL ---
    
    public void saveGlobalFuelSync(UUID player, int joules, ItemStack[] fuels) {
        if (isClosing || connection == null) return;
        String base64 = ItemSerializer.itemStackArrayToBase64(fuels);
        String sql = type.equalsIgnoreCase("MYSQL") 
            ? "REPLACE INTO player_global_fuel (player_uuid, current_joules, fuel_data) VALUES (?, ?, ?)"
            : "INSERT OR REPLACE INTO player_global_fuel (player_uuid, current_joules, fuel_data) VALUES (?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, player.toString());
            stmt.setInt(2, joules);
            stmt.setString(3, base64);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save global fuel for " + player + ": " + e.getMessage());
        }
    }

    public GlobalFuelData loadGlobalFuelSync(UUID player) {
        if (connection == null) return new GlobalFuelData(0, new ItemStack[5]);
        
        String sql = "SELECT * FROM player_global_fuel WHERE player_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, player.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int joules = rs.getInt("current_joules");
                    String base64 = rs.getString("fuel_data");
                    ItemStack[] fuels = ItemSerializer.itemStackArrayFromBase64(base64);
                    return new GlobalFuelData(joules, fuels);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load global fuel for " + player + ": " + e.getMessage());
        }
        return new GlobalFuelData(0, new ItemStack[5]);
    }
    
    // --- UTILITIES ---
    
    public void close() {
        isClosing = true;
        try {
            // Paksa SQLite menulis semua jurnal WAL ke database utama
            if (type.equalsIgnoreCase("SQLITE") && connection != null && !connection.isClosed()) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA wal_checkpoint(FULL);");
                } catch (Exception ignored) {}
            }
            
            Thread.sleep(100);
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database closed safely.");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to close database safely: " + e.getMessage());
        }
    }
    
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && !isClosing;
        } catch (SQLException e) {
            return false;
        }
    }

    public static class GlobalFuelData {
        public final int joules;
        public final ItemStack[] fuelItems;
        public GlobalFuelData(int joules, ItemStack[] fuelItems) {
            this.joules = joules;
            this.fuelItems = fuelItems;
        }
    }
}