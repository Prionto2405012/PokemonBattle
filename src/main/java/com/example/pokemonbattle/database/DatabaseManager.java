package com.example.pokemonbattle.database;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Database Manager for SQLite connections.
 * Implements singleton pattern to manage database connections.
 * Handles database initialization and schema creation.
 */
public class DatabaseManager {
    
    private static DatabaseManager instance;
    private Connection connection;
    
    // Database location in user's home directory
    private static final String DB_DIR = System.getProperty("user.home") + 
                                         System.getProperty("file.separator") + 
                                         "pokemon_battle";
    private static final String DB_NAME = "battle.db";
    private static final String DB_PATH = DB_DIR + System.getProperty("file.separator") + DB_NAME;
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;
    
    /**
     * Private constructor for singleton pattern.
     */
    private DatabaseManager() {
        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            initializeDatabase();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }
    }
    
    /**
     * Get singleton instance of DatabaseManager.
     * 
     * @return DatabaseManager instance
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }
    
    /**
     * Get database connection.
     * Creates new connection if needed.
     * 
     * @return Active database connection
     * @throws SQLException if connection fails
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
            connection.setAutoCommit(true);
            // Enable foreign keys
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
        }
        return connection;
    }
    
    /**
     * Initialize database and create tables if they don't exist.
     */
    private void initializeDatabase() {
        try {
            // Create database directory if it doesn't exist
            Path dbDirectory = Paths.get(DB_DIR);
            if (!Files.exists(dbDirectory)) {
                Files.createDirectories(dbDirectory);
                System.out.println("Created database directory: " + DB_DIR);
            }
            
            // Check if database file exists
            Path dbPath = Paths.get(DB_PATH);
            boolean isNewDatabase = !Files.exists(dbPath);
            
            // Get connection (creates file if doesn't exist)
            Connection conn = getConnection();
            
            if (isNewDatabase) {
                System.out.println("Creating new database: " + DB_PATH);
            } else {
                System.out.println("Using existing database: " + DB_PATH);
            }
            
            // Execute schema SQL (creates tables if they don't exist)
            executeSQLScript(conn);
            
            // Verify tables were created
            verifyTables(conn);
            
            System.out.println("Database initialized successfully");
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database: " + e.getMessage(), e);
        }
    }
    
    /**
     * Execute SQL schema script from resources.
     */
    private void executeSQLScript(Connection conn) throws SQLException {
        try {
            // Read schema.sql from resources
            InputStream is = getClass().getResourceAsStream(
                "/com/example/pokemonbattle/database/schema.sql"
            );
            
            if (is == null) {
                // Fallback: create tables programmatically if schema file not found
                System.out.println("Schema file not found, creating tables programmatically");
                createTablesDirectly(conn);
                return;
            }
            
            System.out.println("Reading schema.sql from resources...");
            String sql;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                // Filter out comment lines and empty lines while reading
                sql = reader.lines()
                        .filter(line -> !line.trim().isEmpty())
                        .filter(line -> !line.trim().startsWith("--"))
                        .collect(Collectors.joining("\n"));
            }
            
            System.out.println("SQL content length: " + sql.length() + " characters");
            System.out.println("Executing SQL statements...");
            
            // Execute SQL statements
            try (Statement stmt = conn.createStatement()) {
                // Split by semicolon and execute each statement
                String[] statements = sql.split(";");
                System.out.println("Found " + statements.length + " statements to execute");
                
                for (int i = 0; i < statements.length; i++) {
                    String trimmed = statements[i].trim();
                    if (!trimmed.isEmpty()) {
                        System.out.println("Statement " + (i+1) + ": " + trimmed.substring(0, Math.min(60, trimmed.length())) + "...");
                        stmt.execute(trimmed);
                        System.out.println("  ✓ Executed successfully");
                    }
                }
            }
            System.out.println("Schema executed successfully");
            
        } catch (Exception e) {
            System.err.println("Error reading/executing schema file: " + e.getMessage());
            e.printStackTrace();
            // Fallback to direct table creation
            System.out.println("Attempting fallback table creation...");
            createTablesDirectly(conn);
        }
    }
    
    /**
     * Create tables directly (fallback if schema file not found).
     */
    private void createTablesDirectly(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Create users table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE NOT NULL, " +
                "email TEXT UNIQUE NOT NULL, " +
                "password_hash TEXT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "last_login TIMESTAMP, " +
                "is_active INTEGER DEFAULT 1, " +
                "CHECK(length(username) >= 3), " +
                "CHECK(length(email) >= 5)" +
                ")"
            );
            
            // Create indexes
            stmt.execute(
                "CREATE INDEX IF NOT EXISTS idx_username ON users(username)"
            );
            stmt.execute(
                "CREATE INDEX IF NOT EXISTS idx_email ON users(email)"
            );
            
            // Create user_profiles table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS user_profiles (" +
                "user_id INTEGER PRIMARY KEY, " +
                "display_name TEXT, " +
                "wins INTEGER DEFAULT 0, " +
                "losses INTEGER DEFAULT 0, " +
                "total_battles INTEGER DEFAULT 0, " +
                "favorite_pokemon TEXT, " +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")"
            );
            
            System.out.println("Tables created successfully");
        }
    }
    
    /**
     * Verify that required tables exist in the database.
     */
    private void verifyTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Check if users table exists
            var rs = stmt.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='users'"
            );
            
            if (rs.next()) {
                System.out.println("✓ Table 'users' verified");
            } else {
                System.err.println("✗ Table 'users' NOT FOUND - attempting fallback creation");
                createTablesDirectly(conn);
                return;
            }
            
            // Check if user_profiles table exists
            rs = stmt.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='user_profiles'"
            );
            
            if (rs.next()) {
                System.out.println("✓ Table 'user_profiles' verified");
            } else {
                System.out.println("! Table 'user_profiles' not found (optional)");
            }
        }
    }
    
    /**
     * Close database connection.
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }
    
    /**
     * Test database connection.
     * 
     * @return true if connection is successful
     */
    public boolean testConnection() {
        try {
            Connection conn = getConnection();
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
