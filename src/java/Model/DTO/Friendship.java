package Model.DTO;

import java.time.LocalDateTime;

public class Friendship {
    
    private Integer friendshipId;
    private int requesterId;
    private int addresseeId;
    private String status; // PENDING, ACCEPTED, BLOCKED
    private Integer blockerId; 
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- Constructors ---

    // Default No-Arg Constructor
    public Friendship() {
    }

    // Constructor without ID and timestamps (Useful for creating new friendships before inserting into DB)
    public Friendship(int requesterId, int addresseeId, String status) {
        this.requesterId = requesterId;
        this.addresseeId = addresseeId;
        this.status = status;
    }

    // Full Constructor (Useful when fetching data from DB)
    public Friendship(Integer friendshipId, int requesterId, int addresseeId, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.friendshipId = friendshipId;
        this.requesterId = requesterId;
        this.addresseeId = addresseeId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // --- Getters and Setters ---

    public Integer getFriendshipId() {
        return friendshipId;
    }

    public void setFriendshipId(Integer friendshipId) {
        this.friendshipId = friendshipId;
    }

    public int getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(int requesterId) {
        this.requesterId = requesterId;
    }

    public int getAddresseeId() {
        return addresseeId;
    }

    public void setAddresseeId(int addresseeId) {
        this.addresseeId = addresseeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getBlockerId() {
        return blockerId;
    }

    public void setBlockerId(Integer blockerId) {
        this.blockerId = blockerId;
    }
    
    

    // --- Optional: toString() for easy debugging ---
    @Override
    public String toString() {
        return "Friendship{" +
                "friendshipId=" + friendshipId +
                ", requesterId=" + requesterId +
                ", addresseeId=" + addresseeId +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}