package Model.DTO;

import java.time.LocalDateTime;

public class ChatSession {
    private int sessionId;
    private String sessionName;
    private int userId;
    private LocalDateTime createdAt;
    private boolean isPinned;

    public ChatSession() {
    }

    public ChatSession(int sessionId, String sessionName, int userId, LocalDateTime createdAt, boolean isPinned) {
        this.sessionId = sessionId;
        this.sessionName = sessionName;
        this.userId = userId;
        this.createdAt = createdAt;
        this.isPinned = isPinned;
    }

    public ChatSession(String sessionName, int userId, boolean isPinned) {
        this.sessionName = sessionName;
        this.userId = userId;
        this.isPinned = isPinned;
    }

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean isPinned) {
        this.isPinned = isPinned;
    }

    //Hàm toString để in ra log debug dễ dàng hơn
    @Override
    public String toString() {
        return "ChatSession{" +
                "sessionId=" + sessionId +
                ", sessionName='" + sessionName + '\'' +
                ", userId=" + userId +
                ", createdAt=" + createdAt +
                ", isPinned=" + isPinned +
                '}';
    }
}