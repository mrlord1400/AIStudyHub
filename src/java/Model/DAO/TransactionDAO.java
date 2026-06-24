package Model.DAO;

import Model.DTO.Transaction;
import Utils.DBUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    /**
     * Tạo giao dịch mới — status mặc định là PENDING. started_at do DB tự set
     * (DEFAULT GETDATE()).
     */
    /**
     * Tạo giao dịch mới. Lấy status từ object truyền vào, nếu không có sẽ mặc
     * định là PENDING. started_at do DB tự set (DEFAULT GETDATE()).
     */
    public boolean createTransaction(Transaction t) {

        // FIX: Đổi 'PENDING' thành dấu chấm hỏi (?) để truyền giá trị động
        String sql
                = "INSERT INTO transactions(user_id, amount, type, status) "
                + "VALUES(?, ?, ?, ?)";

        Connection conn = null;

        try {
            conn = DBUtils.getConnection();

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, t.getUserId());
            ps.setDouble(2, t.getAmount());
            ps.setString(3, t.getType());

            // FIX: Kiểm tra status từ Controller truyền xuống.
            // Nếu có (như lúc mua Premium truyền "SUCCESS") thì dùng luôn. 
            // Nếu null hoặc rỗng thì gán mặc định là "PENDING".
            String status = (t.getStatus() != null && !t.getStatus().trim().isEmpty())
                    ? t.getStatus()
                    : "PENDING";
            ps.setString(4, status);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("[TransactionDAO.createTransaction] " + e.getMessage());
        } finally {
            DBUtils.closeConnection(conn);
        }

        return false;
    }

    public List<Transaction> getTransactionsByUserId(int userId) {

        List<Transaction> list = new ArrayList<>();

        String sql
                = "SELECT * FROM transactions "
                + "WHERE user_id = ? "
                + "ORDER BY started_at DESC";

        Connection conn = null;

        try {
            conn = DBUtils.getConnection();

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            System.out.println("[TransactionDAO.getTransactionsByUserId] " + e.getMessage());
        } finally {
            DBUtils.closeConnection(conn);
        }

        return list;
    }

    /**
     * Lấy TẤT CẢ giao dịch (cho Admin). JOIN với bảng users để lấy username.
     */
    public List<Transaction> getAllTransactions() {

        List<Transaction> list = new ArrayList<>();

        String sql
                = "SELECT t.*, u.username "
                + "FROM transactions t "
                + "JOIN users u ON t.user_id = u.user_id "
                + "ORDER BY t.started_at DESC";

        Connection conn = null;

        try {
            conn = DBUtils.getConnection();

            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Transaction t = mapRow(rs);
                t.setUsername(rs.getString("username"));
                list.add(t);
            }

        } catch (SQLException e) {
            System.out.println("[TransactionDAO.getAllTransactions] " + e.getMessage());
        } finally {
            DBUtils.closeConnection(conn);
        }

        return list;
    }

    /**
     * Lấy 1 giao dịch theo ID.
     */
    public Transaction getTransactionById(int transactionId) {

        String sql = "SELECT * FROM transactions WHERE transaction_id = ?";

        Connection conn = null;

        try {
            conn = DBUtils.getConnection();

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, transactionId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }

        } catch (SQLException e) {
            System.out.println("[TransactionDAO.getTransactionById] " + e.getMessage());
        } finally {
            DBUtils.closeConnection(conn);
        }

        return null;
    }

    /**
     * Admin cập nhật trạng thái giao dịch. Nếu status = SUCCESS hoặc CANCELLED
     * thì set completed_at = GETDATE().
     */
    public boolean updateTransactionStatus(int transactionId, String newStatus) {

        String sql;

        if ("SUCCESS".equals(newStatus) || "CANCELLED".equals(newStatus)) {
            sql = "UPDATE transactions SET status = ?, completed_at = GETDATE() "
                    + "WHERE transaction_id = ?";
        } else {
            sql = "UPDATE transactions SET status = ? "
                    + "WHERE transaction_id = ?";
        }

        Connection conn = null;

        try {
            conn = DBUtils.getConnection();

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, newStatus);
            ps.setInt(2, transactionId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("[TransactionDAO.updateTransactionStatus] " + e.getMessage());
        } finally {
            DBUtils.closeConnection(conn);
        }

        return false;
    }

    /**
     * Helper: Map 1 row từ ResultSet sang Transaction object.
     */
    private Transaction mapRow(ResultSet rs) throws SQLException {

        Transaction t = new Transaction();

        t.setTransactionId(rs.getInt("transaction_id"));
        t.setUserId(rs.getInt("user_id"));
        t.setAmount(rs.getDouble("amount"));
        t.setType(rs.getString("type"));
        t.setStatus(rs.getString("status"));

        Timestamp startedTs = rs.getTimestamp("started_at");
        if (startedTs != null) {
            t.setStartedAt(startedTs.toLocalDateTime());
        }

        Timestamp completedTs = rs.getTimestamp("completed_at");
        if (completedTs != null) {
            t.setCompletedAt(completedTs.toLocalDateTime());
        }

        return t;
    }
}
