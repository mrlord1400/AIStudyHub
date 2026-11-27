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
        // Chỉ chèn khi chưa có dòng nào giữa hai user theo BẤT KỲ chiều nào (chặn cặp đảo B->A khi đã có A->B).
        // Ràng buộc UNIQUE ở DB sẽ bổ sung ở T4.6; câu lệnh này thu hẹp khoảng race.
        String sql = "INSERT INTO friendships (requester_id, addressee_id, status) "
                + "SELECT ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM friendships "
                + "WHERE (requester_id = ? AND addressee_id = ?) OR (requester_id = ? AND addressee_id = ?))";
        Connection conn = null;

        try {
            conn = DBUtils.getConnection();

            // Note the Statement.RETURN_GENERATED_KEYS parameter
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, fship.getRequesterId());
            ps.setInt(2, fship.getAddresseeId());
            ps.setString(3, fship.getStatus());
            ps.setInt(4, fship.getRequesterId());
            ps.setInt(5, fship.getAddresseeId());
            ps.setInt(6, fship.getAddresseeId());
            ps.setInt(7, fship.getRequesterId());

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
     * true nếu tồn tại một dòng ACCEPTED giữa hai user (theo bất kỳ chiều nào).
     * Dùng cho quyết định phân quyền (DocumentAccess), nên không dựa vào "dòng đầu tiên".
     */
    public boolean areFriends(int userA, int userB) {
        String sql = "SELECT COUNT(*) FROM friendships WHERE status = 'ACCEPTED' "
                + "AND ((requester_id = ? AND addressee_id = ?) OR (requester_id = ? AND addressee_id = ?))";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userA);
            ps.setInt(2, userB);
            ps.setInt(3, userB);
            ps.setInt(4, userA);
            try ( ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("[FriendshipDAO.areFriends] " + e.getMessage());
        }
        return false;
    }

    /**
     * Chấp nhận lời mời: người gọi (addresseeId) phải là NGƯỜI NHẬN và lời mời phải đang PENDING.
     * Người gửi tự chấp nhận lời mời của mình, hoặc chấp nhận một dòng BLOCKED/ACCEPTED, đều không đổi gì (Conv §2.4).
     */
    public boolean acceptRequest(int addresseeId, int requesterId) {
        String sql = "UPDATE friendships SET status = 'ACCEPTED', blocker_id = NULL, updated_at = CURRENT_TIMESTAMP "
                + "WHERE requester_id = ? AND addressee_id = ? AND status = 'PENDING'";

        try ( Connection conn = Utils.DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requesterId);
            ps.setInt(2, addresseeId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("[FriendshipDAO.acceptRequest] " + e.getMessage());
        }
        return false;
    }

    /**
     * Chặn từ trạng thái PENDING hoặc ACCEPTED; người gọi trở thành blocker_id.
     * Nếu quan hệ đã BLOCKED (bởi bất kỳ ai) thì không đổi, để người bị chặn không thể
     * chiếm quyền bỏ chặn của người chặn.
     */
    public boolean block(int myUserId, int targetUserId) {
        String sql = "UPDATE friendships SET status = 'BLOCKED', blocker_id = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE ((requester_id = ? AND addressee_id = ?) OR (requester_id = ? AND addressee_id = ?)) "
                + "AND status <> 'BLOCKED'";

        try ( Connection conn = Utils.DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, myUserId);
            ps.setInt(2, myUserId);
            ps.setInt(3, targetUserId);
            ps.setInt(4, targetUserId);
            ps.setInt(5, myUserId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("[FriendshipDAO.block] " + e.getMessage());
        }
        return false;
    }

    /**
     * Xóa quan hệ bạn bè dựa trên cặp User ID (Dùng cho Hủy kết bạn, Từ chối,
     * Bỏ chặn)
     */
    public boolean deleteFriendshipByUsers(int myUserId, int targetUserId) {
        // Dòng BLOCKED chỉ người chặn mới xóa (bỏ chặn) được; kiểm tra ngay trong SQL.
        String sql = "DELETE FROM friendships "
                + "WHERE ((requester_id = ? AND addressee_id = ?) OR (requester_id = ? AND addressee_id = ?)) "
                + "AND (status <> 'BLOCKED' OR blocker_id = ?)";

        try ( Connection conn = Utils.DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, myUserId);
            ps.setInt(2, targetUserId);
            ps.setInt(3, targetUserId);
            ps.setInt(4, myUserId);
            ps.setInt(5, myUserId);

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
