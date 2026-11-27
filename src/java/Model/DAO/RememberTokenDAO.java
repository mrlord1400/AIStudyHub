package Model.DAO;

import Utils.DBUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bảng remember_tokens (T3.4): mỗi dòng là một lần "ghi nhớ đăng nhập". Cookie giữ "selector.validator";
 * DB chỉ giữ SHA-256 của validator nên lộ DB cũng không dùng lại được token.
 */
public class RememberTokenDAO {

    private static final Logger LOG = Logger.getLogger(RememberTokenDAO.class.getName());

    /** Một dòng đã đọc ra. */
    public static final class Row {

        public final int userId;
        public final String validatorHash;
        public final Timestamp expiresAt;
        public final Timestamp lastUsedAt;

        Row(int userId, String validatorHash, Timestamp expiresAt, Timestamp lastUsedAt) {
            this.userId = userId;
            this.validatorHash = validatorHash;
            this.expiresAt = expiresAt;
            this.lastUsedAt = lastUsedAt;
        }
    }

    public boolean insert(int userId, String selector, String validatorHash, Timestamp expiresAt) {
        String sql = "INSERT INTO remember_tokens (user_id, selector, validator_hash, expires_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, selector);
            ps.setString(3, validatorHash);
            ps.setTimestamp(4, expiresAt);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "remember token insert failed userId=" + userId, e);
            return false;
        }
    }

    /** @return null nếu không có selector này. */
    public Row find(String selector) {
        String sql = "SELECT user_id, validator_hash, expires_at, last_used_at FROM remember_tokens WHERE selector = ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, selector);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Row(rs.getInt("user_id"), rs.getString("validator_hash").trim(), rs.getTimestamp("expires_at"), rs.getTimestamp("last_used_at"));
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "remember token lookup failed", e);
        }
        return null;
    }

    /**
     * Đổi validator và gia hạn, chỉ khi hash cũ còn khớp: hai request cùng dùng một token thì chỉ một request thắng.
     *
     * @return true nếu đúng một dòng được cập nhật
     */
    public boolean rotate(String selector, String oldHash, String newHash, Timestamp newExpiresAt) {
        String sql = "UPDATE remember_tokens SET validator_hash = ?, expires_at = ?, last_used_at = ? "
                + "WHERE selector = ? AND validator_hash = ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setTimestamp(2, newExpiresAt);
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            ps.setString(4, selector);
            ps.setString(5, oldHash);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "remember token rotate failed", e);
            return false;
        }
    }

    public void deleteBySelector(String selector) {
        execute("DELETE FROM remember_tokens WHERE selector = ?", selector);
    }

    /** Dùng khi đổi/đặt lại mật khẩu, khóa hoặc cấm tài khoản: mọi thiết bị đã ghi nhớ đều mất hiệu lực. */
    public void deleteAllForUser(int userId) {
        String sql = "DELETE FROM remember_tokens WHERE user_id = ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "remember token cleanup failed userId=" + userId, e);
        }
    }

    /** Theo email, cho luồng đặt lại mật khẩu (UserDAO.updatePassword nhận email). */
    public void deleteAllForEmail(String email) {
        execute("DELETE FROM remember_tokens WHERE user_id IN (SELECT user_id FROM users WHERE email = ?)", email);
    }

    public void deleteExpired() {
        // Giờ lấy từ JVM (cùng nguồn với lúc ghi expires_at), không dùng CURRENT_TIMESTAMP của DB phòng lệch múi giờ.
        String sql = "DELETE FROM remember_tokens WHERE expires_at < ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "remember token expiry cleanup failed", e);
        }
    }

    private void execute(String sql, String arg) {
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, arg);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "remember token delete failed", e);
        }
    }
}
