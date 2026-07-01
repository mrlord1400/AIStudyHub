
package Model.DTO;

import java.time.LocalDateTime;

public class Report {
    private int reportId;
    private int documentId;
    private int reporterId;
    private String reasonCode;
    private String details;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private int adminId;

    public Report() {
    }

    public Report(int reportId, int documentId, int reporterId, String reasonCode, String details, String status, LocalDateTime createdAt) {
        this.reportId = reportId;
        this.documentId = documentId;
        this.reporterId = reporterId;
        this.reasonCode = reasonCode;
        this.details = details;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Report(int reportId, int documentId, int reporterId, String reasonCode, String details, String status, LocalDateTime createdAt, LocalDateTime resolvedAt, int adminId) {
        this.reportId = reportId;
        this.documentId = documentId;
        this.reporterId = reporterId;
        this.reasonCode = reasonCode;
        this.details = details;
        this.status = status;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
        this.adminId = adminId;
    }

    public int getReportId() {
        return reportId;
    }

    public void setReportId(int reportId) {
        this.reportId = reportId;
    }

    public int getDocumentId() {
        return documentId;
    }

    public void setDocumentId(int documentId) {
        this.documentId = documentId;
    }

    public int getReporterId() {
        return reporterId;
    }

    public void setReporterId(int reporterId) {
        this.reporterId = reporterId;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
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

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }
}
