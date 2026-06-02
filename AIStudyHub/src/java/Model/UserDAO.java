/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model;

import Utils.DBUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class UserDAO {

    public boolean register(User user) {

        String sql =
                "INSERT INTO users(username,email,password_hash,role,tier_id,status) " +
                        "VALUES(?,?,?,?,?,?)";

        Connection conn = null;

        try {
            conn = DBUtils.getConnection();

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());

            ps.setString(4, "STUDENT");
            ps.setInt(5, 1);
            ps.setString(6, "ACTIVE");

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("[UserDAO.register] " + e.getMessage());
        } finally {
            DBUtils.closeConnection(conn);
        }

        return false;
    }

    public User login(String email, String password) {

        String sql =
                "SELECT * FROM users " +
                        "WHERE email = ? AND password_hash = ?";

        Connection conn = null;

        try {
            conn = DBUtils.getConnection();

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, email);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                User user = new User();

                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setRole(rs.getString("role"));
                user.setTierId(rs.getInt("tier_id"));
                user.setStatus(rs.getString("status"));
                
                // Backwards-compatible DATETIME2 parsing
                Timestamp expiresTs = rs.getTimestamp("expires_at");
                if (expiresTs != null) user.setExpiresAt(expiresTs.toLocalDateTime());
                
                Timestamp createdTs = rs.getTimestamp("created_at");
                if (createdTs != null) user.setCreatedAt(createdTs.toLocalDateTime());
                
                Timestamp updatedTs = rs.getTimestamp("updated_at");
                if (updatedTs != null) user.setUpdatedAt(updatedTs.toLocalDateTime());

                return user;
            }

        } catch (SQLException e) {
            System.out.println("[UserDAO.login] " + e.getMessage());
        } finally {
            DBUtils.closeConnection(conn);
        }

        return null;
    }

    public boolean updateUser(User user) {

        String sql =
                "UPDATE users " +
                        "SET username = ?, email = ?, password_hash = ? " +
                        "WHERE user_id = ?";

        Connection conn = null;

        try {

            conn = DBUtils.getConnection();

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setInt(4, user.getUserId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {

            System.out.println("[UserDAO.updateUser] " + e.getMessage());

        } finally {

            DBUtils.closeConnection(conn);
        }

        return false;
    }

    public User getUserById(int userId) {

        String sql = "SELECT * FROM users WHERE user_id = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setRole(rs.getString("role"));
                user.setTierId(rs.getInt("tier_id"));
                user.setStatus(rs.getString("status"));
                
                // Backwards-compatible DATETIME2 parsing
                Timestamp expiresTs = rs.getTimestamp("expires_at");
                if (expiresTs != null) user.setExpiresAt(expiresTs.toLocalDateTime());
                
                Timestamp createdTs = rs.getTimestamp("created_at");
                if (createdTs != null) user.setCreatedAt(createdTs.toLocalDateTime());
                
                Timestamp updatedTs = rs.getTimestamp("updated_at");
                if (updatedTs != null) user.setUpdatedAt(updatedTs.toLocalDateTime());
                
                return user;
            }

        } catch (SQLException e) {
            System.out.println("[UserDAO.getUserById] " + e.getMessage());
        }

        return null;
    }

    public boolean deleteUser(int userId) {

        String sql =
                "DELETE FROM users WHERE user_id = ?";

        Connection conn = null;

        try {

            conn = DBUtils.getConnection();

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {

            System.out.println("[UserDAO.deleteUser] " + e.getMessage());

        } finally {

            DBUtils.closeConnection(conn);
        }

        return false;
    }

    public boolean checkPassword(int userId, String currentPassword) {
        
        String sql = "SELECT password_hash FROM users WHERE user_id = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String dbPasswordHash = rs.getString("password_hash");
                    
                    return dbPasswordHash != null && dbPasswordHash.equals(currentPassword);
                }
            }

        } catch (SQLException e) {
            System.out.println("[UserDAO.checkPassword] " + e.getMessage());
        }

        return false;
    }
}