package Model.DTO;

public class ReportReasonConfig {
    private String reasonCode;
    private String severityLevel;
    private double baseScore;
    private double autoFlagThreshold;
    private String description;

    // Constructor rỗng
    public ReportReasonConfig() {
    }

    // Constructor đầy đủ
    public ReportReasonConfig(String reasonCode, String severityLevel, double baseScore, double autoFlagThreshold, String description) {
        this.reasonCode = reasonCode;
        this.severityLevel = severityLevel;
        this.baseScore = baseScore;
        this.autoFlagThreshold = autoFlagThreshold;
        this.description = description;
    }

    // --- GETTER & SETTER ---
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }

    public String getSeverityLevel() { return severityLevel; }
    public void setSeverityLevel(String severityLevel) { this.severityLevel = severityLevel; }

    public double getBaseScore() { return baseScore; }
    public void setBaseScore(double baseScore) { this.baseScore = baseScore; }

    public double getAutoFlagThreshold() { return autoFlagThreshold; }
    public void setAutoFlagThreshold(double autoFlagThreshold) { this.autoFlagThreshold = autoFlagThreshold; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}