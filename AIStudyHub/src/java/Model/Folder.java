package Model;

import java.time.LocalDateTime;

public class Folder {
    private int folderId;
    private int userId;
    private String folderName;
    private LocalDateTime createdAt;

    public Folder() {
    }

    public Folder(int folderId, int userId, String folderName, LocalDateTime createdAt) {
        this.folderId = folderId;
        this.userId = userId;
        this.folderName = folderName;
        this.createdAt = createdAt;
    }

    public int getFolderId() {
        return folderId;
    }

    public void setFolderId(int folderId) {
        this.folderId = folderId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}