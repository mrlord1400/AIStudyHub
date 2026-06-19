package Model.DTO;

import java.sql.Timestamp;

public class ChatMessage {
    private int messageId;
    private int sessionId;
    private String sender;          // Giữ giá trị 'USER' hoặc 'BOT'
    private String messageContent;
    private Timestamp createdAt;    

    // 1. Constructor mặc định (Cần thiết khi sử dụng các framework hoặc DAO)
    public ChatMessage() {
    }

    // 2. Constructor dùng để INSERT dữ liệu mới vào DB
    public ChatMessage(int sessionId, String sender, String messageContent) {
        this.sessionId = sessionId;
        this.sender = sender;
        this.messageContent = messageContent;
    }

    // 3. Constructor đầy đủ dùng để SELECT dữ liệu từ DB lên
    public ChatMessage(int messageId, int sessionId, String sender, String messageContent, Timestamp createdAt) {
        this.messageId = messageId;
        this.sessionId = sessionId;
        this.sender = sender;
        this.messageContent = messageContent;
        this.createdAt = createdAt;
    }

    // --- GETTERS & SETTERS ---

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    // Ghi đè phương thức toString() để dễ dàng in ra console khi debug code DAO
    @Override
    public String toString() {
        return "ChatMessage{" +
                "messageId=" + messageId +
                ", sessionId=" + sessionId +
                ", sender='" + sender + '\'' +
                ", messageContent='" + messageContent + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}