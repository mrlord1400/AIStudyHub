package Model.DAO;

import Model.DTO.Friendship; // Adjust this package if your Friendship class is somewhere else
import Utils.DBUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class FriendshipDAO {

    /**
     * Creates a new friendship and synchronizes the generated ID with the
     * object.
     */
    public boolean createFriendship(Friendship fship) {
        String sql = "INSERT INTO friendships (requester_id, addressee_id, status) VALUES (?, ?, ?)";
        Connection conn = null;

        try {
            conn = DBUtils.getConnection();

            // Note the Statement.RETURN_GENERATED_KEYS parameter
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, fship.getRequesterId());
            ps.setInt(2, fship.getAddresseeId());
            ps.setString(3, fship.getStatus());

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                // Retrieve the auto-generated identity ID from the database
                try ( ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        fship.setFriendshipId(rs.getInt(1)); // Sync the object's ID
                    }
                }
                return true;
            }

        } catch (SQLException e) {
            System.out.println("[FriendshipDAO.createFriendship] " + e.getMessage());
        } finally {
            DBUtils.closeConnection(conn);
        }

        return false;
    }

    /**
     * Updates the status of an existing friendship.
     */
    public boolean updateFriendStatus(int friendshipId, String status) {
        // Also update the updated_at timestamp to keep data accurate
        String sql = "UPDATE friendships SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE friendship_id = ?";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, friendshipId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("[FriendshipDAO.updateFriendStatus] " + e.getMessage());
        }

        return false;
    }

    /**
     * Deletes the selected friendship entirely.
     */
    public boolean deleteFriendship(int friendshipId) {
        String sql = "DELETE FROM friendships WHERE friendship_id = ?";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, friendshipId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("[FriendshipDAO.deleteFriendship] " + e.getMessage());
        }

        return false;
    }

    /**
     * Gets the current friendship status between two users. Returns "NONE" if
     * no friendship record exists.
     */
    public String getFriendshipStatus(int requesterId, int addresseeId) {
        // We check bi-directionally just in case the users are swapped 
        // (e.g. A sent to B, or B sent to A)
        String sql = "SELECT status FROM friendships "
                + "WHERE (requester_id = ? AND addressee_id = ?) "
                + "   OR (requester_id = ? AND addressee_id = ?)";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            // First combination
            ps.setInt(1, requesterId);
            ps.setInt(2, addresseeId);
            // Second combination (flipped)
            ps.setInt(3, addresseeId);
            ps.setInt(4, requesterId);

            try ( ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status"); // Will return 'PENDING', 'ACCEPTED', or 'BLOCKED'
                }
            }

        } catch (SQLException e) {
            System.out.println("[FriendshipDAO.getFriendshipStatus] " + e.getMessage());
        }

        return "NONE"; // Indicates the friendship doesn't exist at all
    }

    /**
     * Cập nhật trạng thái bạn bè dựa trên cặp User ID
     */
    public boolean updateFriendStatusByUsers(int myUserId, int targetUserId, String status) {
        String sql = "UPDATE friendships SET status = ?, blocker_id = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE (requester_id = ? AND addressee_id = ?) "
                + "   OR (requester_id = ? AND addressee_id = ?)";

        try ( Connection conn = Utils.DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            if ("BLOCKED".equalsIgnoreCase(status)) {
                ps.setInt(2, myUserId); // người đang thao tác = người chặn
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setInt(3, myUserId);
            ps.setInt(4, targetUserId);
            ps.setInt(5, targetUserId);
            ps.setInt(6, myUserId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("[FriendshipDAO.updateFriendStatusByUsers] " + e.getMessage());
        }
        return false;
    }

    /**
     * Xóa quan hệ bạn bè dựa trên cặp User ID (Dùng cho Hủy kết bạn, Từ chối,
     * Bỏ chặn)
     */
    public boolean deleteFriendshipByUsers(int myUserId, int targetUserId) {
        String sql = "DELETE FROM friendships "
                + "WHERE (requester_id = ? AND addressee_id = ?) "
                + "   OR (requester_id = ? AND addressee_id = ?)";

        try ( Connection conn = Utils.DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, myUserId);
            ps.setInt(2, targetUserId);
            ps.setInt(3, targetUserId);
            ps.setInt(4, myUserId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("[FriendshipDAO.deleteFriendshipByUsers] " + e.getMessage());
        }
        return false;
    }

    public Friendship getFriendshipDetail(int userA, int userB) {
        String sql = "SELECT * FROM friendships "
                + "WHERE (requester_id = ? AND addressee_id = ?) "
                + "   OR (requester_id = ? AND addressee_id = ?)";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userA);
            ps.setInt(2, userB);
            ps.setInt(3, userB);
            ps.setInt(4, userA);

            try ( ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Friendship f = new Friendship();
                    f.setFriendshipId(rs.getInt("friendship_id"));
                    f.setRequesterId(rs.getInt("requester_id"));
                    f.setAddresseeId(rs.getInt("addressee_id"));
                    f.setStatus(rs.getString("status"));
                    int blockerId = rs.getInt("blocker_id");
                    f.setBlockerId(rs.wasNull() ? null : blockerId);
                    return f;
                }
            }
        } catch (SQLException e) {
            System.out.println("[FriendshipDAO.getFriendshipDetail] " + e.getMessage());
        }
        return null; // không có quan hệ nào
    }
}
