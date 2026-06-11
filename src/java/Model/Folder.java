package Model;

import java.time.LocalDateTime;

/**
 * Model class mapping to the `folders` table in the ai_study_hub database.
 * * Columns: folder_id, user_id, parent_folder_id, folder_name, sharing_permission, created_at
 */
public class Folder {
    private int folderId;
    private int userId;
    private Integer parentFolderId;     // Mới: Cho phép thư mục con (NULL nếu là thư mục gốc)
    private String folderName;
    private String sharingPermission;   // Mới: 'PRIVATE', 'FRIENDS_ONLY', 'PUBLIC'
    private LocalDateTime createdAt;

    // ─── Constructors ────────────────────────────────────────────────────────

    public Folder() {
    }

    public Folder(int folderId, int userId, Integer parentFolderId, String folderName, String sharingPermission, LocalDateTime createdAt) {
        this.folderId = folderId;
        this.userId = userId;
        this.parentFolderId = parentFolderId;
        this.folderName = folderName;
        this.sharingPermission = sharingPermission;
        this.createdAt = createdAt;
    }

    // ─── Getters & Setters ───────────────────────────────────────────────────

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

    public Integer getParentFolderId() {
        return parentFolderId;
    }

    public void setParentFolderId(Integer parentFolderId) {
        this.parentFolderId = parentFolderId;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public String getSharingPermission() {
        return sharingPermission;
    }

    public void setSharingPermission(String sharingPermission) {
        this.sharingPermission = sharingPermission;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Folder{" +
                "folderId=" + folderId +
                ", userId=" + userId +
                ", parentFolderId=" + parentFolderId +
                ", folderName='" + folderName + '\'' +
                ", sharingPermission='" + sharingPermission + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}