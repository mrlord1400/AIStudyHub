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
import Utils.PasswordUtil;

public class UserDAO {

    public boolean register(User user) {

        String sql
                = "INSERT INTO users(username,email,password_hash,role,tier_id,status) "
                + "VALUES(?,?,?,?,?,?)";

        Connection conn = null;

        try {
            conn = DBUtils.getConnection();

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());

            ps.setString(4, user.getRole());
            ps.setInt(5, user.getTierId());
            ps.setString(6, user.getStatus());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("[UserDAO.register] " + e.getMessage());
        } finally {
            DBUtils.closeConnection(conn);
        }

        return false;
    }

    public User login(String email, String password) {

        String sql
                = "SELECT * FROM users "
                + "WHERE email = ? AND password_hash = ?";

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
                if (expiresTs != null) {
                    user.setExpiresAt(expiresTs.toLocalDateTime());
                }

                Timestamp createdTs = rs.getTimestamp("created_at");
                if (createdTs != null) {
                    user.setCreatedAt(createdTs.toLocalDateTime());
                }

                Timestamp updatedTs = rs.getTimestamp("updated_at");
                if (updatedTs != null) {
                    user.setUpdatedAt(updatedTs.toLocalDateTime());
                }

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

        String sql
                = "UPDATE users "
                + "SET username = ?, email = ?, password_hash = ? "
                + "WHERE user_id = ?";

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

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

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
                if (expiresTs != null) {
                    user.setExpiresAt(expiresTs.toLocalDateTime());
                }

                Timestamp createdTs = rs.getTimestamp("created_at");
                if (createdTs != null) {
                    user.setCreatedAt(createdTs.toLocalDateTime());
                }

                Timestamp updatedTs = rs.getTimestamp("updated_at");
                if (updatedTs != null) {
                    user.setUpdatedAt(updatedTs.toLocalDateTime());
                }

                return user;
            }

        } catch (SQLException e) {
            System.out.println("[UserDAO.getUserById] " + e.getMessage());
        }

        return null;
    }

    public boolean deleteUserAndAssociatedData(int userId) {
        // 1. Define SQL statements from the bottom of the hierarchy up to the user
        String deleteBookmarksSql = "DELETE FROM bookmarks WHERE user_id = ?";
        String deleteChatSessionsSql = "DELETE FROM chat_sessions WHERE user_id = ?";
        String deleteDocsSql = "DELETE FROM documents WHERE user_id = ?";
        String deleteFolderSql = "DELETE FROM folders WHERE user_id = ?";
        String deleteUserSql = "DELETE FROM users WHERE user_id = ?";

        try ( Connection conn = DBUtils.getConnection()) {
            // 2. Start a single transaction
            conn.setAutoCommit(false);

            try (
                     PreparedStatement psBookmarks = conn.prepareStatement(deleteBookmarksSql);  PreparedStatement psChat = conn.prepareStatement(deleteChatSessionsSql);  PreparedStatement psDocs = conn.prepareStatement(deleteDocsSql);  PreparedStatement psFolder = conn.prepareStatement(deleteFolderSql);  PreparedStatement psUser = conn.prepareStatement(deleteUserSql)) {
                // Step A: Delete lowest-level dependencies (Bookmarks & Chat History)
                psBookmarks.setInt(1, userId);
                psBookmarks.executeUpdate();

                psChat.setInt(1, userId);
                psChat.executeUpdate();

                // Step B: Delete Documents
                psDocs.setInt(1, userId);
                psDocs.executeUpdate();

                // Step C: Delete Folders
                psFolder.setInt(1, userId);
                psFolder.executeUpdate();

                // Step D: Finally, delete the User
                psUser.setInt(1, userId);
                int userDeleted = psUser.executeUpdate();

                // 3. Commit the transaction if everything succeeds
                conn.commit();
                return userDeleted > 0;

            } catch (SQLException ex) {
                // 4. If anything fails, rollback everything to prevent partial data loss
                conn.rollback();
                System.err.println("[UserDAO.deleteUserAndAssociatedData] Transaction rolled back: " + ex.getMessage());
            } finally {
                // 5. Restore auto-commit behavior for the connection pool
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("[UserDAO.deleteUserAndAssociatedData] Connection error: " + e.getMessage());
        }

        return false;
    }

    public boolean checkPassword(int userId, String currentPassword) {

        String sql = "SELECT password_hash FROM users WHERE user_id = ?";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try ( ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String dbPasswordHash = rs.getString("password_hash");

                    return PasswordUtil.verifyPassword(
                            currentPassword,
                            dbPasswordHash);
                }
            }

        } catch (SQLException e) {
            System.out.println("[UserDAO.checkPassword] " + e.getMessage());
        }

        return false;
    }

    public User getUserByEmail(String email) {

        String sql
                = "SELECT * FROM users WHERE email = ?";

        try ( Connection conn
                = DBUtils.getConnection();  PreparedStatement ps
                = conn.prepareStatement(sql)) {

            ps.setString(1, email);

            ResultSet rs
                    = ps.executeQuery();

            if (rs.next()) {

                User user = new User();

                user.setUserId(
                        rs.getInt("user_id"));

                user.setUsername(
                        rs.getString("username"));

                user.setEmail(
                        rs.getString("email"));

                user.setPasswordHash(
                        rs.getString("password_hash"));

                user.setRole(
                        rs.getString("role"));

                user.setTierId(
                        rs.getInt("tier_id"));

                user.setStatus(
                        rs.getString("status"));

                return user;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void updateBalance(int userId, double amount) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
