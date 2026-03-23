package com.yourserver.bentogens.managers;

import com.yourserver.bentogens.BentoGens;
import com.yourserver.bentogens.models.Generator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {
    
    private final BentoGens plugin;
    private Connection connection;
    private final String type;
    
    public DatabaseManager(BentoGens plugin) {
        this.plugin = plugin;
        this.type = plugin.getConfig().getString("database.type", "SQLITE");
    }
    
    /**
     * Initialize database connection with auto-recovery
     */
    public void initialize() {
        try {
            if (type.equalsIgnoreCase("MYSQL")) {
                connectMySQL();
            } else {
                connectSQLite();
            }
            
            // Create tables with error handling
            createTables();
            
            // Verify database integrity
            if (!verifyDatabaseIntegrity()) {
                plugin.getLogger().warning("Database integrity check failed! Attempting recovery...");
                recoverDatabase();
            }
            
            plugin.getLogger().info("Database initialized (" + type + ")");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            
            // Try recovery
            tryRecovery();
        }
    }
    
    /**
     * Connect to SQLite with WAL mode for corruption prevention
     */
    private void connectSQLite() throws SQLException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        String path = dataFolder.getAbsolutePath() + File.separator + "database.db";
        String url = "jdbc:sqlite:" + path;
        
        connection = DriverManager.getConnection(url);
        
        // Enable WAL mode (Write-Ahead Logging) to prevent corruption
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA synchronous=NORMAL;");
            plugin.getLogger().info("SQLite WAL mode enabled for corruption prevention");
        }
        
        plugin.getLogger().info("Connected to SQLite database: " + path);
    }
    
    /**
     * Connect to MySQL with connection pooling
     */
    private void connectMySQL() throws SQLException {
        String host = plugin.getConfig().getString("database.mysql.host");
        int port = plugin.getConfig().getInt("database.mysql.port");
        String database = plugin.getConfig().getString("database.mysql.database");
        String username = plugin.getConfig().getString("database.mysql.user");
        String password = plugin.getConfig().getString("database.mysql.password");
        boolean useSSL = plugin.getConfig().getBoolean("database.mysql.useSSL", false);
        
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database 
                   + "?useSSL=" + useSSL 
                   + "&autoReconnect=true"
                   + "&useUnicode=true"
                   + "&characterEncoding=utf8";
        
        connection = DriverManager.getConnection(url, username, password);
        
        plugin.getLogger().info("Connected to MySQL database: " + host + ":" + port);
    }
    
    /**
     * Create database tables with proper schema
     */
    private void createTables() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS generators ("
                + "id VARCHAR(36) PRIMARY KEY,"
                + "owner VARCHAR(36) NOT NULL,"
                + "world VARCHAR(100) NOT NULL,"
                + "x INT NOT NULL,"
                + "y INT NOT NULL,"
                + "z INT NOT NULL,"
                + "type VARCHAR(50) NOT NULL,"
                + "last_drop BIGINT,"
                + "placed_at BIGINT"
                + ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            
            // Create index for faster queries
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_owner ON generators(owner)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_location ON generators(world, x, y, z)");
        }
    }
    
    /**
     * Verify database integrity
     */
    private boolean verifyDatabaseIntegrity() {
        if (type.equalsIgnoreCase("SQLITE")) {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("PRAGMA integrity_check")) {
                
                if (rs.next()) {
                    String result = rs.getString(1);
                    return "ok".equalsIgnoreCase(result);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Integrity check failed: " + e.getMessage());
            }
        }
        return true; // MySQL doesn't need integrity check
    }
    
    /**
     * Attempt database recovery
     */
    private void recoverDatabase() {
        if (type.equalsIgnoreCase("SQLITE")) {
            plugin.getLogger().warning("Attempting SQLite database recovery...");
            
            try {
                // Backup corrupt database
                File dbFile = new File(plugin.getDataFolder(), "database.db");
                File backupFile = new File(plugin.getDataFolder(), "database.db.corrupt");
                
                if (dbFile.exists()) {
                    // Close connection
                    if (connection != null && !connection.isClosed()) {
                        connection.close();
                    }
                    
                    // Rename corrupt database
                    dbFile.renameTo(backupFile);
                    plugin.getLogger().warning("Corrupt database backed up to: database.db.corrupt");
                    
                    // Create new database
                    connectSQLite();
                    createTables();
                    
                    plugin.getLogger().info("New database created successfully!");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Recovery failed: " + e.getMessage());
            }
        }
    }
    
    /**
     * Try recovery on initialization failure
     */
    private void tryRecovery() {
        plugin.getLogger().warning("Database initialization failed! Attempting recovery...");
        
        try {
            Thread.sleep(1000); // Wait 1 second
            recoverDatabase();
            
            // Try initialize again
            if (type.equalsIgnoreCase("SQLITE")) {
                connectSQLite();
            } else {
                connectMySQL();
            }
            createTables();
            
            plugin.getLogger().info("Database recovery successful!");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Recovery failed completely: " + e.getMessage());
            plugin.getLogger().severe("Plugin will continue but data persistence is disabled!");
        }
    }
    
    /**
     * Save generator to database (async with retry)
     */
    public CompletableFuture<Void> saveGenerator(Generator generator) {
        return CompletableFuture.runAsync(() -> {
            int retries = 3;
            Exception lastError = null;
            
            for (int i = 0; i < retries; i++) {
                try {
                    saveGeneratorSync(generator);
                    return; // Success!
                } catch (SQLException e) {
                    lastError = e;
                    plugin.getLogger().warning("Failed to save generator (attempt " + (i+1) + "/" + retries + "): " + e.getMessage());
                    
                    // Wait before retry
                    try {
                        Thread.sleep(100 * (i + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            
            // All retries failed
            plugin.getLogger().severe("Failed to save generator after " + retries + " attempts: " + lastError.getMessage());
        });
    }
    
    /**
     * Save generator synchronously
     */
    private void saveGeneratorSync(Generator generator) throws SQLException {
        String sql;
        
        if (type.equalsIgnoreCase("MYSQL")) {
            sql = "REPLACE INTO generators (id, owner, world, x, y, z, type, last_drop, placed_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            sql = "INSERT OR REPLACE INTO generators (id, owner, world, x, y, z, type, last_drop, placed_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }
        
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
            
            stmt.executeUpdate();
        }
    }
    
    /**
     * Load all generators from database with error recovery
     */
    public List<Generator> loadAllGenerators() {
        List<Generator> generators = new ArrayList<>();
        String sql = "SELECT * FROM generators";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                try {
                    Generator gen = loadGeneratorFromResultSet(rs);
                    if (gen != null) {
                        generators.add(gen);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load generator: " + e.getMessage());
                    // Continue loading other generators
                }
            }
            
            plugin.getLogger().info("Loaded " + generators.size() + " generators from database");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load generators: " + e.getMessage());
            e.printStackTrace();
        }
        
        return generators;
    }
    
    /**
     * Load generator from ResultSet
     */
    private Generator loadGeneratorFromResultSet(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        UUID owner = UUID.fromString(rs.getString("owner"));
        
        String worldName = rs.getString("world");
        World world = Bukkit.getWorld(worldName);
        
        if (world == null) {
            plugin.getLogger().warning("World not found for generator: " + worldName);
            return null;
        }
        
        Location location = new Location(
            world,
            rs.getInt("x"),
            rs.getInt("y"),
            rs.getInt("z")
        );
        
        String type = rs.getString("type");
        
        return new Generator(id, owner, location, type);
    }
    
    /**
     * Delete generator from database (async)
     */
    public CompletableFuture<Void> deleteGenerator(String id) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM generators WHERE id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, id);
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete generator: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Close database connection safely
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close database: " + e.getMessage());
        }
    }
    
    /**
     * Check if database is connected
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Backup database (for SQLite only)
     */
    public void backupDatabase() {
        if (type.equalsIgnoreCase("SQLITE")) {
            try {
                File dbFile = new File(plugin.getDataFolder(), "database.db");
                File backupFile = new File(plugin.getDataFolder(), "database.db.backup");
                
                if (dbFile.exists()) {
                    java.nio.file.Files.copy(
                        dbFile.toPath(),
                        backupFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    );
                    plugin.getLogger().info("Database backed up to: database.db.backup");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to backup database: " + e.getMessage());
            }
        }
    }
}