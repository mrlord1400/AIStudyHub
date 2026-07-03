/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model.DAO;

import Model.DTO.User;
import Utils.DBUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import Utils.PasswordUtil;
import java.util.ArrayList;
import java.util.List;

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
                if (PasswordUtil.verifyPassword(password, dbPasswordHash)) {

                    User user = new User();
                    user.setUserId(rs.getInt("user_id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setPasswordHash(dbPasswordHash);
                    user.setRole(rs.getString("role"));
                    user.setTierId(rs.getInt("tier_id"));
                    user.setStatus(rs.getString("status"));

                    // --- MAP THÊM 2 CỘT QUẢN LÝ AI PROMPT ---
                    user.setAiPromptsToday(rs.getInt("ai_prompts_today"));
                    user.setLastPromptReset(rs.getTimestamp("last_prompt_reset"));

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

                // --- MAP THÊM 2 CỘT QUẢN LÝ AI PROMPT ---
                user.setAiPromptsToday(rs.getInt("ai_prompts_today"));
                user.setLastPromptReset(rs.getTimestamp("last_prompt_reset"));

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
        // 1. Khởi tạo các câu lệnh SQL theo thứ tự từ phụ thuộc thấp nhất lên cao nhất
        String updateAdminReportsSql = "UPDATE document_reports SET resolved_by_admin_id = NULL WHERE resolved_by_admin_id = ?";
        String deleteReporterReportsSql = "DELETE FROM document_reports WHERE reporter_id = ?";
        String deleteFriendshipsSql = "DELETE FROM friendships WHERE requester_id = ? OR addressee_id = ? OR blocker_id = ?";
        String deleteBookmarksSql = "DELETE FROM bookmarks WHERE user_id = ?";
        String deleteTransactionsSql = "DELETE FROM transactions WHERE user_id = ?";
        String deleteChatSessionsSql = "DELETE FROM chat_sessions WHERE user_id = ?";

        // Xóa documents (sẽ tự động cascade xóa document_extracted_text, document_reports và bookmarks của file này)
        String deleteDocumentsSql = "DELETE FROM documents WHERE user_id = ?";

        // Gỡ tham chiếu folder cha - con trước khi xóa folder để tránh lỗi Self-referencing FK
        String updateFoldersSql = "UPDATE folders SET parent_folder_id = NULL WHERE user_id = ?";
        String deleteFoldersSql = "DELETE FROM folders WHERE user_id = ?";

        String deleteUserSql = "DELETE FROM users WHERE user_id = ?";

        try ( Connection conn = DBUtils.getConnection()) {
            // 2. Bắt đầu Transaction
            conn.setAutoCommit(false);

            try (
                     PreparedStatement psUpdateAdmin = conn.prepareStatement(updateAdminReportsSql);  PreparedStatement psDeleteReporter = conn.prepareStatement(deleteReporterReportsSql);  PreparedStatement psFriendships = conn.prepareStatement(deleteFriendshipsSql);  PreparedStatement psBookmarks = conn.prepareStatement(deleteBookmarksSql);  PreparedStatement psTransactions = conn.prepareStatement(deleteTransactionsSql);  PreparedStatement psChat = conn.prepareStatement(deleteChatSessionsSql);  PreparedStatement psDocs = conn.prepareStatement(deleteDocumentsSql);  PreparedStatement psUpdateFolders = conn.prepareStatement(updateFoldersSql);  PreparedStatement psFolders = conn.prepareStatement(deleteFoldersSql);  PreparedStatement psUser = conn.prepareStatement(deleteUserSql)) {
                // Bước A: Xử lý các Report (Gỡ tên Admin đã xử lý & xóa report do user này tạo)
                psUpdateAdmin.setInt(1, userId);
                psUpdateAdmin.executeUpdate();

                psDeleteReporter.setInt(1, userId);
                psDeleteReporter.executeUpdate();

                // Bước B: Xóa các mối quan hệ bạn bè (Bất kể là người gửi, người nhận hay người block)
                psFriendships.setInt(1, userId);
                psFriendships.setInt(2, userId);
                psFriendships.setInt(3, userId);
                psFriendships.executeUpdate();

                // Bước C: Xóa Bookmarks, Giao dịch (Transactions) và Lịch sử Chat (Session & Message)
                psBookmarks.setInt(1, userId);
                psBookmarks.executeUpdate();

                psTransactions.setInt(1, userId);
                psTransactions.executeUpdate();

                psChat.setInt(1, userId);
                psChat.executeUpdate();

                // Bước D: Xóa Tài liệu (Documents) - Phải làm trước Folder vì FK không có CASCADE
                psDocs.setInt(1, userId);
                psDocs.executeUpdate();

                // Bước E: Xóa Thư mục (Folders)
                psUpdateFolders.setInt(1, userId);
                psUpdateFolders.executeUpdate(); // Cắt đứt quan hệ cha con giữa các folder

                psFolders.setInt(1, userId);
                psFolders.executeUpdate();       // Xóa an toàn

                // Bước F: Cuối cùng, xóa User
                psUser.setInt(1, userId);
                int userDeleted = psUser.executeUpdate();

                // 3. Commit Transaction nếu mọi thứ thành công
                conn.commit();
                return userDeleted > 0;

            } catch (SQLException ex) {
                // 4. Rollback nếu có bất kỳ lỗi nào để bảo toàn dữ liệu
                conn.rollback();
                System.err.println("[UserDAO.deleteUserAndAssociatedData] Transaction rolled back: " + ex.getMessage());
            } finally {
                // 5. Trả lại trạng thái auto-commit cho connection pool
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

                // --- MAP THÊM 2 CỘT QUẢN LÝ AI PROMPT ---
                user.setAiPromptsToday(rs.getInt("ai_prompts_today"));
                user.setLastPromptReset(rs.getTimestamp("last_prompt_reset"));

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

    // --- HÀM MỚI: DÙNG ĐỂ CẬP NHẬT HOẶC RESET LƯỢT HỎI AI ---
    public boolean updateAiUsage(int userId, int promptsToday, java.sql.Timestamp lastReset) {
        String sql = "UPDATE users SET ai_prompts_today = ?, last_prompt_reset = ? WHERE user_id = ?";
        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, promptsToday);
            ps.setTimestamp(2, lastReset);
            ps.setInt(3, userId);

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("[UserDAO.updateAiUsage] " + e.getMessage());
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

    public List<User> getAcceptedFriends(int myUserId) {

        List<User> friendList = new ArrayList<>();

        String sql = "SELECT u.* FROM users u "
                + "JOIN friendships f ON (u.user_id = f.requester_id OR u.user_id = f.addressee_id) "
                + "WHERE f.status = 'ACCEPTED' "
                + "AND (f.requester_id = ? OR f.addressee_id = ?) "
                + "AND u.user_id <> ?";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, myUserId);
            ps.setInt(2, myUserId);
            ps.setInt(3, myUserId); // Prevents the user from showing up in their own friend list

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setRole(rs.getString("role"));
                user.setTierId(rs.getInt("tier_id"));
                user.setStatus(rs.getString("status"));
                user.setBalance(rs.getInt("balance"));

                // --- MAP THÊM 2 CỘT QUẢN LÝ AI PROMPT ---
                user.setAiPromptsToday(rs.getInt("ai_prompts_today"));
                user.setLastPromptReset(rs.getTimestamp("last_prompt_reset"));

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

                friendList.add(user);
            }

        } catch (SQLException e) {
            System.out.println("[UserDAO.getAcceptedFriends] " + e.getMessage());
        }

        return friendList;
    }

    public List<User> getPendingRequests(int myUserId) {

        List<User> pendingList = new ArrayList<>();

        String sql = "SELECT u.* FROM users u "
                + "JOIN friendships f ON u.user_id = f.requester_id "
                + "WHERE f.addressee_id = ? AND f.status = 'PENDING'";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, myUserId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setRole(rs.getString("role"));
                user.setTierId(rs.getInt("tier_id"));
                user.setStatus(rs.getString("status"));
                user.setBalance(rs.getInt("balance"));

                // --- MAP THÊM 2 CỘT QUẢN LÝ AI PROMPT ---
                user.setAiPromptsToday(rs.getInt("ai_prompts_today"));
                user.setLastPromptReset(rs.getTimestamp("last_prompt_reset"));

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

                pendingList.add(user);
            }

        } catch (SQLException e) {
            System.out.println("[UserDAO.getPendingRequests] " + e.getMessage());
        }

        return pendingList;
    }

    public List<User> getBlockedUsers(int myUserId) {
        List<User> blockedList = new ArrayList<>();

        // Chỉ lấy friendship mà CHÍNH myUserId là người bấm nút Chặn (blocker_id)
        String sql = "SELECT u.* FROM users u "
                + "JOIN friendships f ON (u.user_id = f.requester_id OR u.user_id = f.addressee_id) "
                + "WHERE f.status = 'BLOCKED' "
                + "AND f.blocker_id = ? "
                + "AND u.user_id <> ?";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, myUserId);
            ps.setInt(2, myUserId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setRole(rs.getString("role"));
                user.setTierId(rs.getInt("tier_id"));
                user.setStatus(rs.getString("status"));
                user.setBalance(rs.getInt("balance"));
                user.setAiPromptsToday(rs.getInt("ai_prompts_today"));
                user.setLastPromptReset(rs.getTimestamp("last_prompt_reset"));
                blockedList.add(user);
            }
        } catch (SQLException e) {
            System.out.println("[UserDAO.getBlockedUsers] " + e.getMessage());
        }
        return blockedList;
    }
}
