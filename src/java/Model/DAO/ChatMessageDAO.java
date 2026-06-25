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
    
    public String createInitMessage(int userID){
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
    
        // Hàm 4: Lưu tin nhắn của System
    public boolean createSystemMessage(String systemMessage, int sessionId) {
        String sql = "INSERT INTO chat_messages (session_id, sender, message_content, display) VALUES (?, 'USER', ?, 0)";
        
        try (Connection conn = DBUtils.getConnection(); // Thay bằng class lấy connection của bạn
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
        
        try (Connection conn = DBUtils.getConnection(); // Thay bằng class lấy connection của bạn
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
    
        public List<ChatMessage> getAllDisplayableMessage(int sessionId) {
        List<ChatMessage> list = new ArrayList<>();
        // Quan trọng: Phải ORDER BY message_id ASC để AI đọc đúng luồng thời gian
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