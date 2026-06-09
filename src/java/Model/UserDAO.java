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

        // ONLY query by email. Do not include password in the WHERE clause!
        String sql = "SELECT * FROM users WHERE email = ?";
        Connection conn = null;

        try {
            conn = DBUtils.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // Get the hash stored in the database
                String dbPasswordHash = rs.getString("password_hash");

                // Verify the plain text password against the DB hash
                // Note: I am assuming verifyPassword exists based on your checkPassword method!
                // If it doesn't, use: org.mindrot.jbcrypt.BCrypt.checkpw(password, dbPasswordHash)
                if (PasswordUtil.verifyPassword(password, dbPasswordHash)) {

                    User user = new User();
                    user.setUserId(rs.getInt("user_id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setPasswordHash(dbPasswordHash);
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

                    return user; // Successful login
                } else {
                    System.out.println("[UserDAO.login] Invalid password for email: " + email);
                    return null; // Passwords did not match
                }
            }

        } catch (SQLException e) {
            System.out.println("[UserDAO.login] " + e.getMessage());
        } finally {
            DBUtils.closeConnection(conn);
        }

        return null; // User not found or error occurred
    }

    public boolean updateUser(User user) {

        String sql
                = "UPDATE users "
                + "SET username = ?, email = ?, role = ?, status = ?, balance = ?, tier_id = ? "
                + "WHERE user_id = ?";

        Connection conn = null;

        try {

            conn = DBUtils.getConnection();

            PreparedStatement ps = conn.prepareStatement(sql);

            // Set the original fields
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());

            // Set the new admin-editable fields
            ps.setString(3, user.getRole());
            ps.setString(4, user.getStatus());
            ps.setInt(5, user.getBalance());
            ps.setInt(6, user.getTierId());

            // Set the WHERE condition
            ps.setInt(7, user.getUserId());

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
                user.setBalance(rs.getInt("balance"));

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
        String deleteTransactionSql = "DELETE FROM transactions WHERE user_id = ?";
        String deleteUserSql = "DELETE FROM users WHERE user_id = ?";

        try ( Connection conn = DBUtils.getConnection()) {
            // 2. Start a single transaction
            conn.setAutoCommit(false);

            try (
                     PreparedStatement psBookmarks = conn.prepareStatement(deleteBookmarksSql);  PreparedStatement psChat = conn.prepareStatement(deleteChatSessionsSql);  PreparedStatement psDocs = conn.prepareStatement(deleteDocsSql);  PreparedStatement psFolder = conn.prepareStatement(deleteFolderSql);  PreparedStatement psUser = conn.prepareStatement(deleteUserSql); PreparedStatement psTrans = conn.prepareStatement(deleteTransactionSql)) {
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
                psTrans.setInt(1, userId);
                psTrans.executeUpdate();
                
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
                user.setBalance(
                        rs.getInt("balance"));

                return user;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean updateBalance(int userId, int amount) {
        String sql
                = "UPDATE users "
                + "SET balance = ? "
                + "WHERE user_id = ?";

        Connection conn = null;

        try {

            conn = DBUtils.getConnection();
            User user = getUserById(userId);
            int newBalance = user.getBalance() + amount;
            PreparedStatement ps = conn.prepareStatement(sql);

            // Set the original fields
            ps.setInt(1, newBalance);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {

            System.out.println("[UserDAO.updateUserBalance] " + e.getMessage());

        } finally {

            DBUtils.closeConnection(conn);
        }

        return false;
    }

    //Insert Test Users into database
    public boolean seedTestUsers() {
        String sql = "INSERT INTO users(username, email, password_hash, role, tier_id, status) VALUES(?,?,?,?,?,?)";
        Connection conn = null;

        try {
            conn = DBUtils.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            // --- Insert User 1: Student ---
            ps.setString(1, "student_user");
            ps.setString(2, "user@gmail.com");
            ps.setString(3, PasswordUtil.hashPassword("123")); // Hashing via Java
            ps.setString(4, "STUDENT");
            ps.setInt(5, 1); // Assuming 1 is the default Free/Guest tier
            ps.setString(6, "ACTIVE");
            ps.addBatch();

            // --- Insert User 2: Admin ---
            ps.setString(1, "admin_user");
            ps.setString(2, "admin@gmail.com");
            ps.setString(3, PasswordUtil.hashPassword("123")); // Hashing via Java
            ps.setString(4, "ADMIN");
            ps.setInt(5, 3); // Assuming 3 is a higher tier
            ps.setString(6, "ACTIVE");
            ps.addBatch();

            // Execute both inserts
            int[] results = ps.executeBatch();
            return results.length == 2;

        } catch (SQLException e) {
            System.out.println("[UserDAO.seedTestUsers] " + e.getMessage());
        } finally {
            DBUtils.closeConnection(conn);
        }

        return false;
    }
}
