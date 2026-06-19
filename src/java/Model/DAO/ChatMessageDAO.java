package DAO;

import Model.DTO.ChatMessage;
import Utils.DBUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ChatMessageDAO {

    // Hàm 1: Lưu tin nhắn của người dùng
    public boolean createUserMessage(String userMessage, int sessionId) {
        String sql = "INSERT INTO chat_messages (session_id, sender, message_content) VALUES (?, 'USER', ?)";
        
        try (Connection conn = DBUtils.getConnection(); // Thay bằng class lấy connection của bạn
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

    // Hàm 2: Lưu câu trả lời của AI (Bot)
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

    // Hàm 3: Lấy toàn bộ lịch sử trò chuyện của một Session (Sắp xếp từ cũ tới mới)
    public List<ChatMessage> getAllMessageFromSession(int sessionId) {
        List<ChatMessage> list = new ArrayList<>();
        // Quan trọng: Phải ORDER BY message_id ASC để AI đọc đúng luồng thời gian
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

    // Hàm 4: Lấy một tin nhắn cụ thể dựa trên ID
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
                    
                    return msg; // Trả về object nếu tìm thấy
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Trả về null nếu không tìm thấy ID này trong DB
    }
}