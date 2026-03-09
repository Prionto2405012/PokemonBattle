package com.example.pokemonbattle.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.example.pokemonbattle.database.DatabaseManager;
import com.example.pokemonbattle.model.BattleRecord;

/**
 * Manages battle history persistence and retrieval.
 * Clean separation: all DB logic lives here, not in controllers.
 */
public class BattleHistoryManager {

    private static BattleHistoryManager instance;
    private final DatabaseManager dbManager;

    private BattleHistoryManager() {
        this.dbManager = DatabaseManager.getInstance();
        ensureTableExists();
    }

    public static BattleHistoryManager getInstance() {
        if (instance == null) {
            instance = new BattleHistoryManager();
        }
        return instance;
    }

    // ── Schema ──────────────────────────────────────────────────

    private void ensureTableExists() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS battle_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "result TEXT NOT NULL, " +
                "pokemon_used TEXT NOT NULL, " +
                "opponent_type TEXT NOT NULL, " +
                "opponent_name TEXT, " +
                "battle_log TEXT, " +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ")"
            );
            stmt.execute(
                "CREATE INDEX IF NOT EXISTS idx_battle_history_user ON battle_history(user_id)"
            );
            // Add battle_log column if it doesn't exist (migration for older DBs)
            try {
                stmt.execute("ALTER TABLE battle_history ADD COLUMN battle_log TEXT");
            } catch (SQLException ignored) {
                // Column already exists
            }
            // Migrate old schema: if table has 'battle_date' column (from old schema.sql)
            // instead of 'timestamp', and CHECK constraint blocks 'ONLINE' — recreate table
            migrateOldSchema(conn);
            System.out.println("[BattleHistoryManager] Table verified");
        } catch (SQLException e) {
            System.err.println("[BattleHistoryManager] Failed to create table: " + e.getMessage());
        }
    }

    /**
     * Detects and migrates old battle_history schema that has 'battle_date'
     * instead of 'timestamp' and a CHECK constraint blocking 'ONLINE' opponent_type.
     */
    private void migrateOldSchema(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Check if old 'battle_date' column exists
            boolean hasBattleDate = false;
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT sql FROM sqlite_master WHERE type='table' AND name='battle_history'")) {
                if (rs.next()) {
                    String createSql = rs.getString("sql");
                    if (createSql != null && createSql.contains("battle_date")) {
                        hasBattleDate = true;
                    }
                }
            }
            if (!hasBattleDate) return;

            System.out.println("[BattleHistoryManager] Detected old schema — migrating battle_history table...");
            stmt.execute("ALTER TABLE battle_history RENAME TO battle_history_old");
            stmt.execute(
                "CREATE TABLE battle_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "result TEXT NOT NULL, " +
                "pokemon_used TEXT NOT NULL, " +
                "opponent_type TEXT NOT NULL, " +
                "opponent_name TEXT, " +
                "battle_log TEXT, " +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(id))"
            );
            stmt.execute(
                "INSERT INTO battle_history (id, user_id, result, pokemon_used, opponent_type, opponent_name, battle_log, timestamp) " +
                "SELECT id, user_id, result, pokemon_used, opponent_type, opponent_name, battle_log, battle_date FROM battle_history_old"
            );
            stmt.execute("DROP TABLE battle_history_old");
            stmt.execute(
                "CREATE INDEX IF NOT EXISTS idx_battle_history_user ON battle_history(user_id)"
            );
            System.out.println("[BattleHistoryManager] Migration complete");
        } catch (SQLException e) {
            System.err.println("[BattleHistoryManager] Migration failed: " + e.getMessage());
        }
    }

    // ── Write ───────────────────────────────────────────────────

    /**
     * Save a battle record to the database.
     */
    public void saveBattleRecord(BattleRecord record) {
        String sql = "INSERT INTO battle_history (user_id, result, pokemon_used, opponent_type, opponent_name, battle_log, timestamp) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, record.getUserId());
            pstmt.setString(2, record.getResult());
            pstmt.setString(3, String.join(",", record.getPokemonUsed()));
            pstmt.setString(4, record.getOpponentType());
            pstmt.setString(5, record.getOpponentName());
            pstmt.setString(6, record.getBattleLog());
            pstmt.setTimestamp(7, Timestamp.valueOf(
                    record.getTimestamp() != null ? record.getTimestamp() : LocalDateTime.now()));
            pstmt.executeUpdate();
            System.out.println("[BattleHistoryManager] Battle record saved: " + record.getResult());
        } catch (SQLException e) {
            System.err.println("[BattleHistoryManager] Failed to save record: " + e.getMessage());
        }
    }

    // ── Read ────────────────────────────────────────────────────

    /**
     * Get all battle records for a user, newest first.
     */
    public List<BattleRecord> getBattleHistory(int userId) {
        List<BattleRecord> records = new ArrayList<>();
        String sql = "SELECT id, user_id, result, pokemon_used, opponent_type, opponent_name, battle_log, timestamp " +
                     "FROM battle_history WHERE user_id = ? ORDER BY timestamp DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BattleRecord record = new BattleRecord();
                    record.setId(rs.getInt("id"));
                    record.setUserId(rs.getInt("user_id"));
                    record.setResult(rs.getString("result"));
                    String pokemonStr = rs.getString("pokemon_used");
                    record.setPokemonUsed(pokemonStr != null && !pokemonStr.isEmpty()
                            ? Arrays.asList(pokemonStr.split(","))
                            : new ArrayList<>());
                    record.setOpponentType(rs.getString("opponent_type"));
                    record.setOpponentName(rs.getString("opponent_name"));
                    record.setBattleLog(rs.getString("battle_log"));
                    Timestamp ts = rs.getTimestamp("timestamp");
                    if (ts != null) record.setTimestamp(ts.toLocalDateTime());
                    records.add(record);
                }
            }
        } catch (SQLException e) {
            System.err.println("[BattleHistoryManager] Failed to load history: " + e.getMessage());
        }
        return records;
    }

    /**
     * Get battle records filtered by opponent type (AI, ONLINE, or null for all).
     */
    public List<BattleRecord> getBattleHistoryByType(int userId, String opponentType) {
        if (opponentType == null || opponentType.isEmpty()) return getBattleHistory(userId);

        List<BattleRecord> records = new ArrayList<>();
        String sql = "SELECT id, user_id, result, pokemon_used, opponent_type, opponent_name, battle_log, timestamp " +
                     "FROM battle_history WHERE user_id = ? AND opponent_type = ? ORDER BY timestamp DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, opponentType);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BattleRecord record = new BattleRecord();
                    record.setId(rs.getInt("id"));
                    record.setUserId(rs.getInt("user_id"));
                    record.setResult(rs.getString("result"));
                    String pokemonStr = rs.getString("pokemon_used");
                    record.setPokemonUsed(pokemonStr != null && !pokemonStr.isEmpty()
                            ? Arrays.asList(pokemonStr.split(","))
                            : new ArrayList<>());
                    record.setOpponentType(rs.getString("opponent_type"));
                    record.setOpponentName(rs.getString("opponent_name"));
                    record.setBattleLog(rs.getString("battle_log"));
                    Timestamp ts = rs.getTimestamp("timestamp");
                    if (ts != null) record.setTimestamp(ts.toLocalDateTime());
                    records.add(record);
                }
            }
        } catch (SQLException e) {
            System.err.println("[BattleHistoryManager] Failed to load filtered history: " + e.getMessage());
        }
        return records;
    }

    /**
     * Get recent N battle records for a user.
     */
    public List<BattleRecord> getRecentBattles(int userId, int limit) {
        List<BattleRecord> all = getBattleHistory(userId);
        return all.subList(0, Math.min(limit, all.size()));
    }

    // ── Aggregate stats ─────────────────────────────────────────

    public int getWinCount(int userId) {
        return getCountByResult(userId, "WIN");
    }

    public int getLossCount(int userId) {
        return getCountByResult(userId, "LOSS");
    }

    public int getTotalBattles(int userId) {
        String sql = "SELECT COUNT(*) FROM battle_history WHERE user_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[BattleHistoryManager] Total count failed: " + e.getMessage());
        }
        return 0;
    }

    private int getCountByResult(int userId, String result) {
        String sql = "SELECT COUNT(*) FROM battle_history WHERE user_id = ? AND result = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, result);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[BattleHistoryManager] Count failed: " + e.getMessage());
        }
        return 0;
    }
}
