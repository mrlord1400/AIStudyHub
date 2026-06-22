package Model.DAO;

import Model.DTO.ChatSession;
import Utils.DBUtils;
import DAO.ChatMessageDAO;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatSessionDAO {

    private static final String SYSTEM_PROMPT
            = "You are a student's personal assistant, your mission is to answer students queries, "
            + "help the student find where their missing documents or folders are, find the right folder "
            + "for the student to place their documents into and analyze the student's document to summarize "
            + "or to answer questions relating to that specific documents. "
            + "To help with this, whenever the student ask a question relating to location of folders and document, "
            + "you must respond with only one word SEARCH, this helps our system to know that you want to know "
            + "the student's folders and our system will send you the folder tree data, "
            + "if the student asks a question relating to documents content, you must respond with only VIEW/[insert Document Name], "
            + "this will tell our system to send you that document's vectorized data for you to analyze. "
            + "Finally, if you want to respond, make sure to include RESPONSE: at the beginning of your response "
            + "to tell our system that you are responding and not trying to view data. "
            + "Note: If students ask a question that might relate to a certain file but you do not know the exact file name,"
            + "You can ask the system for the folder structure then look through the file's names to find documents"
            + "That might relates to the Student's question, if the answer is multiple, you can list the related documents"
            + "And ask the Student to choose."
            + "Prompts after this will be from the students";

    /**
     * Tạo 1 session mới, lưu vào database và trả về object ChatSession vừa tạo
     */
    public ChatSession createSession(String sessionName, int userId) {
        String sql = "INSERT INTO chat_sessions (session_name, user_id, is_pinned) VALUES (?, ?, 0)";
        ChatSession newSession = null;

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, sessionName);
            ps.setInt(2, userId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                // Lấy ID vừa được tự động sinh ra bởi IDENTITY(1,1)
                try ( ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int generatedId = rs.getInt(1);

                        // Để có đầy đủ dữ liệu (bao gồm created_at do DB tự sinh), ta query lại (hoặc tự set)
                        // Ở đây query lại để đảm bảo đồng bộ hoàn toàn với SQL Server
                        newSession = getSessionById(generatedId, conn);
                        if (newSession != null) {
                            ChatMessageDAO chatMessageDAO = new ChatMessageDAO();
                            chatMessageDAO.createSystemMessage(SYSTEM_PROMPT, generatedId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newSession;
    }

    /**
     * Cập nhật tên của session cũ thành tên mới
     */
    public boolean updateSessionName(ChatSession session) {
        String sql = "UPDATE chat_sessions SET session_name = ? WHERE session_id = ?";
        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, session.getSessionName());
            ps.setInt(2, session.getSessionId());

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Cập nhật trạng thái pin của Session
     */
    public boolean pinSession(ChatSession session) {
        String sql = "UPDATE chat_sessions SET is_pinned = ? WHERE session_id = ?";
        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, session.isPinned());
            ps.setInt(2, session.getSessionId());

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Kiểm tra tính hợp lệ, xóa chat_messages trước rồi mới xóa chat_sessions
     */
    public boolean deleteSession(ChatSession session) {
        String checkSql = "SELECT session_name, user_id FROM chat_sessions WHERE session_id = ?";
        String deleteMessagesSql = "DELETE FROM chat_messages WHERE session_id = ?";
        String deleteSessionSql = "DELETE FROM chat_sessions WHERE session_id = ?";

        try ( Connection conn = DBUtils.getConnection()) {
            // Bước 1: Kiểm tra xem 2 session có giống nhau không (so sánh trong DB và Object truyền vào)
            boolean isMatch = false;
            try ( PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
                psCheck.setInt(1, session.getSessionId());
                try ( ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        String dbSessionName = rs.getString("session_name");
                        int dbUserId = rs.getInt("user_id");

                        if (dbSessionName.equals(session.getSessionName()) && dbUserId == session.getUserId()) {
                            isMatch = true;
                        }
                    }
                }
            }

            // Bước 2: Nếu khớp, tiến hành xóa (Sử dụng Transaction)
            if (isMatch) {
                try {
                    conn.setAutoCommit(false); // Bắt đầu Transaction

                    // Xóa các tin nhắn liên quan trước (khóa ngoại)
                    try ( PreparedStatement psMsg = conn.prepareStatement(deleteMessagesSql)) {
                        psMsg.setInt(1, session.getSessionId());
                        psMsg.executeUpdate();
                    }

                    // Xóa session
                    try ( PreparedStatement psSess = conn.prepareStatement(deleteSessionSql)) {
                        psSess.setInt(1, session.getSessionId());
                        psSess.executeUpdate();
                    }

                    conn.commit(); // Xác nhận Transaction
                    return true;

                } catch (SQLException ex) {
                    conn.rollback(); // Nếu có lỗi ở bất kỳ bước nào thì hoàn tác lại
                    ex.printStackTrace();
                } finally {
                    conn.setAutoCommit(true); // Trả lại trạng thái mặc định
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Tìm session theo tên (Sử dụng LIKE để tìm kiếm tương đối)
     */
    public List<ChatSession> findSessionByName(String sessionName) {
        List<ChatSession> list = new ArrayList<>();
        String sql = "SELECT * FROM chat_sessions WHERE session_name LIKE ?";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            // Thêm ký tự % để tìm kiếm chứa chuỗi (chứa từ khóa)
            ps.setString(1, "%" + sessionName + "%");

            try ( ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(extractSessionFromResultSet(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Lấy toàn bộ danh sách Chat Session của một user cụ thể, sắp xếp theo thời
     * gian tạo mới nhất (giảm dần)
     */
    public List<ChatSession> getAllSessionsByUserId(int userId) {
        List<ChatSession> list = new ArrayList<>();
        String sql = "SELECT * FROM chat_sessions WHERE user_id = ? ORDER BY created_at DESC";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try ( ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Tận dụng lại hàm helper extractSessionFromResultSet đã có
                    list.add(extractSessionFromResultSet(rs));
                }
            }
        } catch (Exception e) {
            System.err.println("[ChatSessionDAO.getAllSessionsByUserId] Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // ================== CÁC HÀM HỖ TRỢ (HELPER METHODS) ================== //
    /**
     * Hàm hỗ trợ lấy 1 Session bằng ID (Dùng trong lúc tạo mới session)
     */
    public ChatSession getSessionById(int sessionId, Connection conn) throws SQLException {
        String sql = "SELECT * FROM chat_sessions WHERE session_id = ?";
        try ( PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            try ( ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractSessionFromResultSet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Hàm lấy Session cho Controller sử dụng (Tự động quản lý Connection)
     */
    public ChatSession getSessionById(int sessionId) {
        try ( Connection conn = DBUtils.getConnection()) {
            return getSessionById(sessionId, conn);
        } catch (SQLException e) {
            System.err.println("[ChatSessionDAO.getSessionById] Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Hàm hỗ trợ map dữ liệu từ ResultSet sang Object ChatSession để tái sử
     * dụng code
     */
    private ChatSession extractSessionFromResultSet(ResultSet rs) throws SQLException {
        ChatSession session = new ChatSession();
        session.setSessionId(rs.getInt("session_id"));
        session.setSessionName(rs.getString("session_name"));
        session.setUserId(rs.getInt("user_id"));

        // Map DATETIME2 của SQL Server sang LocalDateTime của Java
        Timestamp timestamp = rs.getTimestamp("created_at");
        if (timestamp != null) {
            session.setCreatedAt(timestamp.toLocalDateTime());
        }

        session.setPinned(rs.getBoolean("is_pinned"));
        return session;
    }
}
