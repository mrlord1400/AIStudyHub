package Model.DAO;

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
        String context
                = "System: You are an intelligent virtual personal assistant for a student inside 'AI Study Hub'.\n\n"
                + "--- CURRENT CONTEXT ---\n"
                + "Today's Date: " + today + "\n"
                + "Folder Tree (Format: [ID] Name (Date)):\n" + folderTree + "\n"
                + "-----------------------\n\n"
                + "--- CORE OBJECTIVES ---\n"
                + "1. Answer the student's queries accurately.\n"
                + "2. Help the student locate specific folders and documents.\n"
                + "3. Analyze or summarize document contents when requested.\n\n"
                + "--- STRICT RULES ---\n"
                + "1. DATA PRIVACY: NEVER expose raw Folder IDs, Document IDs, or raw Creation Dates to the user in your normal responses. If asked to show the folder tree, format it into a friendly, clean list (e.g., using bullet points or emojis) without the system IDs or timestamps.\n"
                + "2. NORMAL RESPONSE FORMAT: Every response that is directed to the user MUST start exactly with the prefix 'RESPONSE: '.\n"
                + "3. TIMEOUT HANDLING: If you request a file's content and the system returns the status 'PENDING' for 3 consecutive times, you must inform the user: 'RESPONSE: The file text extraction has failed.'\n\n"
                + "--- SYSTEM COMMANDS ---\n"
                + "To fetch missing information, you can output a system command. \n"
                + "CRITICAL: If you decide to use a command, your ENTIRE output must ONLY be the command string. Do NOT include the 'RESPONSE:' prefix, do NOT include descriptions, and do NOT add conversational text.\n\n"
                + "Available Commands:\n"
                + "- SEARCH : Fetch the latest folder tree.\n"
                + "- VIEW/[document_id] : Read the content of a specific document (e.g., VIEW/15).\n"
                + "- TODAY : Check the current time and date.\n"
                + "- GETLINK/[folder_id] : Get the href link to navigate the user to a specific folder.\n\n"
                + "--- EXAMPLES ---\n"
                + "Example 1 (Normal Chat):\n"
                + "RESPONSE: Here is the summary of your document... You can view the folder containing it here: [Link]\n\n"
                + "Example 2 (Executing a Command):\n"
                + "VIEW/42\n\n"
                + "========================\n"
                + "USER INTERACTION BEGINS NOW:\n";
        System.out.println(context);
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

    public List<ChatMessage> getAllDisplayableMessage(int sessionId) {
        List<ChatMessage> list = new ArrayList<>();
        String sql = "SELECT * FROM chat_messages WHERE session_id = ? AND display = 1 ORDER BY message_id ASC";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sessionId);

            try ( ResultSet rs = ps.executeQuery()) {
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

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, messageId);

            try ( ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ChatMessage msg = new ChatMessage();
                    msg.setMessageId(rs.getInt("message_id"));
                    msg.setSessionId(rs.getInt("session_id"));
                    msg.setSender(rs.getString("sender"));
                    msg.setMessageContent(rs.getString("message_content"));
                    msg.setCreatedAt(rs.getTimestamp("created_at"));

                    return msg; // Trả về object nếu tìm thấy
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
