package com.cloudxplorer.authservice.repository;

import com.cloudxplorer.authservice.config.AuthProperties;
import com.cloudxplorer.authservice.model.UserRecord;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class UserRepository {
    private final AuthProperties authProperties;

    public UserRepository(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public UserRecord findByEmail(String email) throws SQLException {
        String sql = "SELECT user_id, name, username, email, phone FROM " + userTable() + " WHERE email = ?";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return toUserRecord(rs);
            }
        }
    }

    public UserRecord findByPhone(String phone) throws SQLException {
        String sql = "SELECT user_id, name, username, email, phone FROM " + userTable() + " WHERE phone = ?";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phone);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return toUserRecord(rs);
            }
        }
    }

    public void insert(String userId, String name, String username, String email, String phone) throws SQLException {
        String sql = "INSERT INTO " + userTable() + " (user_id, name, username, email, phone) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, name);
            stmt.setString(3, username);
            stmt.setString(4, email);
            stmt.setString(5, phone);
            stmt.executeUpdate();
        }
    }

    public void updateProfile(String userId, String name, String phone) throws SQLException {
        String sql = "UPDATE " + userTable() + " SET name = ?, phone = ?, updated_at = NOW() WHERE user_id = ?";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, phone);
            stmt.setString(3, userId);
            stmt.executeUpdate();
        }
    }

    public void deleteByUserId(String userId) throws SQLException {
        String sql = "DELETE FROM " + userTable() + " WHERE user_id = ?";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.executeUpdate();
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
                authProperties.getDb().getUrl(),
                authProperties.getDb().getUsername(),
                authProperties.getDb().getPassword());
    }

    private String userTable() {
        return authProperties.getDb().getUserTable();
    }

    private UserRecord toUserRecord(ResultSet rs) throws SQLException {
        return new UserRecord(
                rs.getString("user_id"),
                rs.getString("name"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("phone"));
    }
}
