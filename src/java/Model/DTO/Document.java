package Model.DTO;

import java.time.LocalDateTime;

/**
 * Model class mapping to the `documents` table in the ai_study_hub database.
 *
 * Columns: document_id, user_id, folder_id, title, file_extension, cloud_storage_url, 
 * file_size_mb, ai_parsing_status, sharing_permission, share_link_token, is_flagged, 
 * created_at, updated_at
 */
public class Document {

    private int documentId;
    private int userId;
    private Integer folderId;
    private String title;
    private String fileExtension;       // Mới: Định dạng file (VD: 'pdf', 'docx')
    private String cloudStorageUrl;
    private double fileSizeMb;
    private String aiParsingStatus;
    private String sharingPermission;
    private String shareLinkToken;
    private boolean isFlagged;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;    // Mới: Cập nhật bởi Trigger tự động

    // ─── Constructors ────────────────────────────────────────────────────────

    public Document() {}

    public Document(int documentId, int userId, Integer folderId, String title, String fileExtension,
                    String cloudStorageUrl, double fileSizeMb, String aiParsingStatus,
                    String sharingPermission, String shareLinkToken, boolean isFlagged, 
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.documentId = documentId;
        this.userId = userId;
        this.folderId = folderId;
        this.title = title;
        this.fileExtension = fileExtension;
        this.cloudStorageUrl = cloudStorageUrl;
        this.fileSizeMb = fileSizeMb;
        this.aiParsingStatus = aiParsingStatus;
        this.sharingPermission = sharingPermission;
        this.shareLinkToken = shareLinkToken;
        this.isFlagged = isFlagged;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public int getDocumentId() { 
        return documentId; 
    }
    public void setDocumentId(int documentId) { 
        this.documentId = documentId; 
    }

    public int getUserId() { 
        return userId; 
    }
    public void setUserId(int userId) { 
        this.userId = userId; 
    }

    public Integer getFolderId() { 
        return folderId; 
    }
    public void setFolderId(Integer folderId) { 
        this.folderId = folderId; 
    }

    public String getTitle() { 
        return title; 
    }
    public void setTitle(String title) { 
        this.title = title; 
    }

    public String getFileExtension() {
        return fileExtension;
    }
    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getCloudStorageUrl() { 
        return cloudStorageUrl; 
    }
    public void setCloudStorageUrl(String cloudStorageUrl) { 
        this.cloudStorageUrl = cloudStorageUrl; 
    }

    public double getFileSizeMb() { 
        return fileSizeMb; 
    }
    public void setFileSizeMb(double fileSizeMb) { 
        this.fileSizeMb = fileSizeMb; 
    }

    public String getAiParsingStatus() { 
        return aiParsingStatus; 
    }
    public void setAiParsingStatus(String aiParsingStatus) { 
        this.aiParsingStatus = aiParsingStatus; 
    }

    public String getSharingPermission() { 
        return sharingPermission; 
    }
    public void setSharingPermission(String sharingPermission) { 
        this.sharingPermission = sharingPermission; 
    }

    public String getShareLinkToken() { 
        return shareLinkToken; 
    }
    public void setShareLinkToken(String shareLinkToken) { 
        this.shareLinkToken = shareLinkToken; 
    }

    public boolean isFlagged() { 
        return isFlagged; 
    }
    public void setFlagged(boolean flagged) { 
        this.isFlagged = flagged; 
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

    @Override
    public String toString() {
        return "Document{" +
               "documentId=" + documentId +
               ", userId=" + userId +
               ", folderId=" + folderId +
               ", title='" + title + '\'' +
               ", fileExtension='" + fileExtension + '\'' +
               ", fileSizeMb=" + fileSizeMb +
               ", aiParsingStatus='" + aiParsingStatus + '\'' +
               ", sharingPermission='" + sharingPermission + '\'' +
               ", isFlagged=" + isFlagged +
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               '}';
    }
}