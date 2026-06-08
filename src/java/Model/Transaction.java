package Model;

import java.time.LocalDateTime;

public class Transaction {

    private int transactionId;
    private int userId;
    private double amount;
    private String type;       // DEPOSIT, WITHDRAW
    private String status;     // PENDING, PROCESSING, SUCCESS, CANCELLED
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    // Thêm username để hiển thị trong admin (JOIN từ bảng users)
    private String username;

    public Transaction() {
    }

    public Transaction(int transactionId, int userId, double amount,
                       String type, String status,
                       LocalDateTime startedAt, LocalDateTime completedAt) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
