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
     * Initialize database connection
     */
    public void initialize() {
        try {
            if (type.equalsIgnoreCase("MYSQL")) {
                connectMySQL();
            } else {
                connectSQLite();
            }
            
            createTables();
            plugin.getLogger().info("Database initialized (" + type + ")");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Connect to SQLite
     */
    private void connectSQLite() throws SQLException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        String path = dataFolder.getAbsolutePath() + File.separator + "database.db";
        String url = "jdbc:sqlite:" + path;
        
        connection = DriverManager.getConnection(url);
        plugin.getLogger().info("Connected to SQLite database: " + path);
    }
    
    /**
     * Connect to MySQL
     */
    private void connectMySQL() throws SQLException {
        String host = plugin.getConfig().getString("database.mysql.host");
        int port = plugin.getConfig().getInt("database.mysql.port");
        String database = plugin.getConfig().getString("database.mysql.database");
        String username = plugin.getConfig().getString("database.mysql.username");
        String password = plugin.getConfig().getString("database.mysql.password");
        
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false";
        connection = DriverManager.getConnection(url, username, password);
        
        plugin.getLogger().info("Connected to MySQL database: " + host + ":" + port);
    }
    
    /**
     * Create database tables
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
        }
    }
    
    /**
     * Save generator to database (async)
     */
    public CompletableFuture<Void> saveGenerator(Generator generator) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR REPLACE INTO generators (id, owner, world, x, y, z, type, last_drop, placed_at) "
                       + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            // For MySQL, use different syntax
            if (type.equalsIgnoreCase("MYSQL")) {
                sql = "REPLACE INTO generators (id, owner, world, x, y, z, type, last_drop, placed_at) "
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
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save generator: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Load all generators from database
     */
    public List<Generator> loadAllGenerators() {
        List<Generator> generators = new ArrayList<>();
        String sql = "SELECT * FROM generators";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String id = rs.getString("id");
                UUID owner = UUID.fromString(rs.getString("owner"));
                
                String worldName = rs.getString("world");
                World world = Bukkit.getWorld(worldName);
                
                if (world == null) {
                    plugin.getLogger().warning("World not found for generator: " + worldName);
                    continue;
                }
                
                Location location = new Location(
                    world,
                    rs.getInt("x"),
                    rs.getInt("y"),
                    rs.getInt("z")
                );
                
                String type = rs.getString("type");
                
                Generator gen = new Generator(id, owner, location, type);
                generators.add(gen);
            }
            
            plugin.getLogger().info("Loaded " + generators.size() + " generators from database");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load generators: " + e.getMessage());
            e.printStackTrace();
        }
        
        return generators;
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
     * Close database connection
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
}