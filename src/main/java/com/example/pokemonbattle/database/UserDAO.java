package com.example.pokemonbattle.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import com.example.pokemonbattle.model.User;

public class UserDAO {
    
    private final DatabaseManager dbManager;
    
    public UserDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }
    public Optional<User> createUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, email, password_hash, created_at, is_active) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setInt(5, user.isActive() ? 1 : 0);
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows == 0) {
                return Optional.empty();
            }
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                    user.setCreatedAt(LocalDateTime.now());
                    return Optional.of(user);
                }
            }
            
            return Optional.empty();
            
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                if (e.getMessage().contains("username")) {
                    throw new SQLException("Username already exists");
                } else if (e.getMessage().contains("email")) {
                    throw new SQLException("Email already exists");
                }
            }
            throw e;
        }
    }
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, email, password_hash, created_at, last_login, is_active " +
                     "FROM users WHERE username = ? AND is_active = 1";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT id, username, email, password_hash, created_at, last_login, is_active " +
                     "FROM users WHERE email = ? AND is_active = 1";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        
        return false;
    }
    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        
        return false;
    }
    public void updateLastLogin(int userId) throws SQLException {
        String sql = "UPDATE users SET last_login = ? WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setInt(2, userId);
            
            pstmt.executeUpdate();
        }
    }
    public Optional<User> authenticateUser(String usernameOrEmail, String passwordHash) throws SQLException {
        Optional<User> user = findByUsername(usernameOrEmail);
        if (user.isEmpty()) {
            user = findByEmail(usernameOrEmail);
        }
        
        if (user.isPresent() && user.get().getPasswordHash().equals(passwordHash)) {
            updateLastLogin(user.get().getId());
            user.get().setLastLogin(LocalDateTime.now());
            return user;
        }
        
        return Optional.empty();
    }
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) {
            user.setLastLogin(lastLogin.toLocalDateTime());
        }
        
        user.setActive(rs.getInt("is_active") == 1);
        
        return user;
    }
    public void deleteUser(int userId) throws SQLException {
        String sql = "UPDATE users SET is_active = 0 WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        }
    }
}
