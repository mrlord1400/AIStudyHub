package DAO;

import Model.DAO.DocumentDAO;
import Model.DAO.FolderDAO;
import Model.DTO.ChatMessage;
import Utils.DBUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ChatMessageDAO {

    public String createInitMessage(int userID) {
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String today = currentDateTime.format(formatter);
        FolderDAO fDao = new FolderDAO();
        DocumentDAO dDao = new DocumentDAO();
        String folderTree = fDao.buildFolderTree(fDao.getAllFoldersByUserId(userID), dDao.getDocumentsByUserId(userID));
        String context = "You are a student's virtual personal assistant.\n"
                + "\n"
                + "These are the current data that you need to know:\n"
                + "\n"
                + "Today's Date:" + today
                + "\n"
                + "The Student's Folder Tree and its created date: " + folderTree
                + "\n"
                + "Based on these informations, Do the following:\n"
                + "- With every response that isn't a command, make sure to include RESPONSE: at the beginning of your response\n"
                + "- Answer the Students queries\n"
                + "- Help Student locate folders or documents location\n"
                + "- Help Student Analyze or summarize the document's content\n"
                + "- If after 3 times of requesting to see a file's content but the system returns PENDING, tell the user that the file's text failed to be extracted\n"
                + "\n"
                + "If you are missing any Information, use the command list below\n"
                + "NOTE: If you want to use a command, only send the response as the command and nothing else, if you fail to do so, the command will fail. \n"
                + "Make sure to not type the commands description\n"
                + "\n"
                + "Command List (command - description):\n"
                + "- SEARCH - to see the folder tree again\n"
                + "- VIEW/[insert document name] - to see one specific document's content\n"
                + "- TODAY - to see current time and date\n"
                + "\n"
                + "Prompts after this will be from the students";
        return context;
    }

    /**
     * Build nội dung "system prompt" khi người dùng vừa đính kèm một tài liệu mới.
     * Đây là logic được chuyển từ front-end (JS) sang back-end.
     *
     * Hàm này CHỈ build chuỗi, không đụng tới DB — để có thể tái sử dụng / test độc lập.
     *
     * @param currentAttachment tên file vừa đính kèm (null hoặc rỗng nếu không có)
     * @param messageText       câu hỏi gốc của user (có thể null hoặc rỗng)
     * @return chuỗi system prompt hoàn chỉnh, hoặc null nếu không có attachment
     */
    public String buildAttachmentSystemMessage(String currentAttachment, String messageText) {
        if (currentAttachment == null || currentAttachment.trim().isEmpty()) {
            return null;
        }

        String cleanAttachment = currentAttachment.trim();
        boolean hasMessage = messageText != null && !messageText.trim().isEmpty();

        String finalMessage;
        if (!hasMessage) {
            finalMessage = "[HỆ THỐNG: Người dùng vừa đính kèm tài liệu mới tên là '"
                    + cleanAttachment
                    + "'. BẠN PHẢI DÙNG LỆNH: VIEW/" + cleanAttachment + " để đọc nó.]";
        } else {
            finalMessage = "[HỆ THỐNG: Người dùng vừa đính kèm tài liệu mới tên là '"
                    + cleanAttachment
                    + "'. BẠN PHẢI DÙNG LỆNH: VIEW/" + cleanAttachment
                    + " để đọc nội dung tài liệu này TRƯỚC KHI trả lời câu hỏi bên dưới.]\n\n"
                    + "Câu hỏi của người dùng: " + messageText.trim();
        }

        return finalMessage;
    }

    // Hàm 1: Lưu tin nhắn của người dùng (HIỂN THỊ cho user, display = 1 mặc định trong schema)
    public boolean createUserMessage(String userMessage, int sessionId) {
        String sql = "INSERT INTO chat_messages (session_id, sender, message_content) VALUES (?, 'USER', ?)";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sessionId);
            ps.setString(2, userMessage);

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Hàm 3: Lưu câu trả lời của AI không display
    public boolean createBotMessage(String botMessage, int sessionId) {
        String sql = "INSERT INTO chat_messages (session_id, sender, message_content) VALUES (?, 'BOT', ?)";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sessionId);
            ps.setString(2, botMessage);

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Hàm 4: Lưu tin nhắn của System (ẨN khỏi UI, display = 0). Lưu với sender = 'USER'
    // để AI đọc lại lịch sử và hiểu đây là input đến từ phía "user/hệ thống".
    public boolean createSystemMessage(String systemMessage, int sessionId) {
        String sql = "INSERT INTO chat_messages (session_id, sender, message_content, display) VALUES (?, 'USER', ?, 0)";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sessionId);
            ps.setString(2, systemMessage);

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean createNonDisplayBotMessage(String botMessage, int sessionId) {
        String sql = "INSERT INTO chat_messages (session_id, sender, message_content, display) VALUES (?, 'BOT', ?, 0)";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sessionId);
            ps.setString(2, botMessage);

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Tiện ích gộp: nếu có file đính kèm, build system prompt và lưu nó vào DB
     * dưới dạng tin nhắn ẨN (display = 0) — không ảnh hưởng đến tin nhắn hiển thị
     * trên UI mà createUserMessage đã lưu riêng.
     *
     * Trả về true nếu có attachment và đã xử lý xong (lưu thành công).
     * Trả về false nếu không có attachment (không cần xử lý gì) HOẶC lưu thất bại.
     */
    public boolean handleAttachmentIfPresent(String currentAttachment, String messageText, int sessionId) {
        String attachmentPrompt = buildAttachmentSystemMessage(currentAttachment, messageText);
        if (attachmentPrompt == null) {
            return false; // Không có đính kèm, không cần làm gì
        }
        return createSystemMessage(attachmentPrompt, sessionId);
    }

    // Hàm: Lấy toàn bộ lịch sử trò chuyện của một Session (bao gồm cả message ẩn) — DÙNG CHO AI
    public List<ChatMessage> getAllMessageFromSession(int sessionId) {
        List<ChatMessage> list = new ArrayList<>();
        String sql = "SELECT * FROM chat_messages WHERE session_id = ? ORDER BY message_id ASC";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sessionId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChatMessage msg = new ChatMessage();
                    msg.setMessageId(rs.getInt("message_id"));
                    msg.setSessionId(rs.getInt("session_id"));
                    msg.setSender(rs.getString("sender"));
                    msg.setMessageContent(rs.getString("message_content"));
                    msg.setCreatedAt(rs.getTimestamp("created_at"));

                    list.add(msg);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Hàm: Lấy chỉ những message CÓ THỂ HIỂN THỊ (display = 1) — DÙNG CHO JSP RENDER UI
    public List<ChatMessage> getAllDisplayableMessage(int sessionId) {
        List<ChatMessage> list = new ArrayList<>();
        String sql = "SELECT * FROM chat_messages WHERE session_id = ? AND display = 1 ORDER BY message_id ASC";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sessionId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChatMessage msg = new ChatMessage();
                    msg.setMessageId(rs.getInt("message_id"));
                    msg.setSessionId(rs.getInt("session_id"));
                    msg.setSender(rs.getString("sender"));
                    msg.setMessageContent(rs.getString("message_content"));
                    msg.setCreatedAt(rs.getTimestamp("created_at"));

                    list.add(msg);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Hàm: Lấy một tin nhắn cụ thể dựa trên ID
    public ChatMessage getMessageFromId(int messageId) {
        String sql = "SELECT * FROM chat_messages WHERE message_id = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, messageId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ChatMessage msg = new ChatMessage();
                    msg.setMessageId(rs.getInt("message_id"));
                    msg.setSessionId(rs.getInt("session_id"));
                    msg.setSender(rs.getString("sender"));
                    msg.setMessageContent(rs.getString("message_content"));
                    msg.setCreatedAt(rs.getTimestamp("created_at"));

                    return msg;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}