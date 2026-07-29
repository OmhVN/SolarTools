package com.omhvn.tools.database;

import com.omhvn.tools.SolarTool;
import com.omhvn.tools.utils.SecurityManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class DatabaseManager {
    private final SolarTool plugin;

    // SQLite - single persistent connection
    private Connection sqliteConnection;

    // MySQL - HikariCP connection pool
    private HikariDataSource hikariDataSource;

    private String dbType;

    public DatabaseManager(SolarTool plugin) {
        SecurityManager.checkLink(this);
        this.plugin = plugin;
    }

    public void load() {
        // Save databases.yml default if not present
        File dbConfigFile = new File(plugin.getDataFolder(), "databases.yml");
        if (!dbConfigFile.exists()) {
            plugin.saveResource("databases.yml", false);
        }

        org.bukkit.configuration.file.YamlConfiguration dbConfig =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(dbConfigFile);

        dbType = dbConfig.getString("type", "sqlite");
        if (dbType == null || dbType.isBlank()) dbType = "sqlite";
        dbType = dbType.trim().toLowerCase();

        if (dbType.equals("mysql")) {
            loadMySQL(dbConfig);
        } else {
            dbType = "sqlite";
            loadSQLite();
        }

        createTables();
        plugin.getLogger().info("[Database] Connected via " + dbType.toUpperCase() + ".");
    }

    // ── SQLite ────────────────────────────────────────────────────────────────

    private void loadSQLite() {
        File dataFile = new File(plugin.getDataFolder(), "database.db");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "File write error: database.db", e);
            }
        }
        try {
            Class.forName("org.sqlite.JDBC");
            sqliteConnection = DriverManager.getConnection("jdbc:sqlite:" + dataFile.getAbsolutePath());
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("[Database] SQLite JDBC driver not found!");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Database] SQLite connection failed", e);
        }
    }

    // ── MySQL ─────────────────────────────────────────────────────────────────

    private void loadMySQL(org.bukkit.configuration.file.YamlConfiguration cfg) {
        String host     = cfg.getString("mysql.host", "localhost");
        int    port     = cfg.getInt   ("mysql.port", 3306);
        String database = cfg.getString("mysql.database", "solartool");
        String user     = cfg.getString("mysql.user", "root");
        String password = cfg.getString("mysql.password", "");
        int    maxPool  = cfg.getInt   ("mysql.max-pool", 15);
        boolean useSSL  = cfg.getBoolean("mysql.use-ssl", false);

        if (database == null || database.isBlank() || user == null || user.isBlank()) {
            plugin.getLogger().warning("[Database] MySQL credentials are not configured (database or user is empty). Falling back to SQLite.");
            dbType = "sqlite";
            loadSQLite();
            return;
        }

        HikariConfig hikariCfg = new HikariConfig();
        hikariCfg.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=" + useSSL + "&autoReconnect=true&characterEncoding=utf8");
        hikariCfg.setUsername(user);
        hikariCfg.setPassword(password);
        hikariCfg.setMaximumPoolSize(maxPool);
        hikariCfg.setMinimumIdle(2);
        hikariCfg.setConnectionTimeout(30_000);
        hikariCfg.setIdleTimeout(600_000);
        hikariCfg.setMaxLifetime(1_800_000);
        hikariCfg.setPoolName("SolarTool-MySQL");

        try {
            hikariDataSource = new HikariDataSource(hikariCfg);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE,
                    "[Database] MySQL connection failed — falling back to SQLite.", e);
            dbType = "sqlite";
            loadSQLite();
        }
    }

    // ── Shared ────────────────────────────────────────────────────────────────

    private void createTables() {
        Connection conn = getConnection();
        if (conn == null) return;
        try (Statement s = conn.createStatement()) {
            s.executeUpdate(
                "CREATE TABLE IF NOT EXISTS tool_usage (" +
                "  player_uuid VARCHAR(36)  NOT NULL," +
                "  tool_id     VARCHAR(64)  NOT NULL," +
                "  last_tick   BIGINT       NOT NULL," +
                "  PRIMARY KEY (player_uuid, tool_id)" +
                ");"
            );
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Database] Could not create tables", e);
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Returns a live connection.
     * MySQL  → borrowed from HikariCP pool (MUST be released via releaseConnection).
     * SQLite → single persistent connection (releaseConnection is a no-op).
     */
    public Connection getConnection() {
        if (dbType.equals("mysql") && hikariDataSource != null) {
            try {
                return hikariDataSource.getConnection();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "[Database] Could not get MySQL connection", e);
                return null;
            }
        }
        // SQLite
        try {
            if (sqliteConnection == null || sqliteConnection.isClosed()) {
                loadSQLite();
            }
            return sqliteConnection;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Database] SQLite connection check failed", e);
            return null;
        }
    }

    /**
     * Release a connection.
     * MySQL: closes (returns to pool). SQLite: no-op.
     */
    public void releaseConnection(Connection conn) {
        if (conn == null) return;
        if (dbType.equals("mysql")) {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    public void close() {
        if (hikariDataSource != null && !hikariDataSource.isClosed()) {
            hikariDataSource.close();
        }
        try {
            if (sqliteConnection != null && !sqliteConnection.isClosed()) {
                sqliteConnection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
