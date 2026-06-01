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

    public User login(String username, String password) {

        String sql =
                "SELECT * FROM users " +
                        "WHERE username = ? AND password_hash = ?";

        Connection conn = null;

        try {
            conn = DBUtils.getConnection();

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, username);
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

                return user;
            }

        } catch (SQLException e) {
            System.out.println("[UserDAO.login] " + e.getMessage());
        } finally {
            DBUtils.closeConnection(conn);
        }

        return null;
    }
}