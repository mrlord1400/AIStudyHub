package Model.DAO;

import Model.DTO.User;
import Utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDAO {

    public List<User> getAllUsers() {

        List<User> users = new ArrayList<>();

        String sql = "SELECT * FROM users";

        try (
                Connection conn = DBUtils.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {

                User user = new User();

                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setRole(rs.getString("role"));
                user.setTierId(rs.getInt("tier_id"));
                user.setStatus(rs.getString("status"));
                user.setBalance(rs.getInt("balance"));

                users.add(user);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return users;
    }

    public boolean createUser(User user) {

        String sql =
                "INSERT INTO users " +
                        "(username,email,password_hash,role,tier_id,status) " +
                        "VALUES(?,?,?,?,?,?)";

        try (
                Connection conn = DBUtils.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole());
            ps.setInt(5, user.getTierId());
            ps.setString(6, user.getStatus());

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteUser(int userId) {

        Connection conn = null;

        try {

            conn = DBUtils.getConnection();
            conn.setAutoCommit(false);

            PreparedStatement ps1 =
                    conn.prepareStatement(
                            "DELETE FROM bookmarks WHERE user_id = ?");

            ps1.setInt(1, userId);
            ps1.executeUpdate();

            PreparedStatement ps2 =
                    conn.prepareStatement(
                            "DELETE FROM chat_sessions WHERE user_id = ?");

            ps2.setInt(1, userId);
            ps2.executeUpdate();

            PreparedStatement ps3 =
                    conn.prepareStatement(
                            "DELETE FROM documents WHERE user_id = ?");

            ps3.setInt(1, userId);
            ps3.executeUpdate();

            PreparedStatement ps4 =
                    conn.prepareStatement(
                            "DELETE FROM folders WHERE user_id = ?");

            ps4.setInt(1, userId);
            ps4.executeUpdate();

            PreparedStatement ps5 =
                    conn.prepareStatement(
                            "DELETE FROM users WHERE user_id = ?");

            ps5.setInt(1, userId);

            boolean success = ps5.executeUpdate() > 0;

            conn.commit();

            return success;

        } catch (Exception e) {

            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            e.printStackTrace();

        } finally {

            DBUtils.closeConnection(conn);
        }

        return false;
    }
}