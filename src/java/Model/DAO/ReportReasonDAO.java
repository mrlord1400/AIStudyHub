package Model.DAO;

import Model.DTO.ReportReason;
import Utils.DBUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReportReasonDAO {

    /**
     * Retrieves all configured report reasons from the database.
     */
    public ReportReasonDAO() {
    }

    public List<ReportReason> getAllReportReason() {
        List<ReportReason> list = new ArrayList<>();
        String sql = "SELECT reason_code, severity_level, base_score, auto_flag_threshold, description "
                + "FROM report_reason_configs";
        Connection conn = null;

        try {
            conn = DBUtils.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {
                
                while (rs.next()) {
                    ReportReason reason = new ReportReason();
                    reason.setReasonCode(rs.getString("reason_code"));
                    reason.setSeverityLevel(rs.getString("severity_level"));
                    reason.setBaseScore(rs.getDouble("base_score"));
                    reason.setAutoFlagThreshold(rs.getDouble("auto_flag_threshold"));
                    reason.setDescription(rs.getString("description"));
                    
                    list.add(reason);
                }
            }
        } catch (SQLException e) {
            System.out.println("[ReportReasonDAO.getAllReportReason] " + e.getMessage());
        } finally {
            DBUtils.closeConnection(conn);
        }
        return list;
    }

    /**
     * Updates all configurable fields of an existing report reason config based on its code.
     */
    public boolean updateReportReason(ReportReason reason) {
        String sql = "UPDATE report_reason_configs SET "
                   + "severity_level = ?, base_score = ?, auto_flag_threshold = ?, description = ? "
                   + "WHERE reason_code = ?";
        Connection conn = null;

        try {
            conn = DBUtils.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, reason.getSeverityLevel());
                ps.setDouble(2, reason.getBaseScore());
                ps.setDouble(3, reason.getAutoFlagThreshold());
                ps.setString(4, reason.getDescription());
                ps.setString(5, reason.getReasonCode());

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("[ReportReasonDAO.updateReportReason] " + e.getMessage());
        } finally {
            DBUtils.closeConnection(conn);
        }
        return false;
    }

    /**
     * Inserts a new report reason configuration into the database.
     */
    public boolean createReportReason(ReportReason reason) {
        String sql = "INSERT INTO report_reason_configs (reason_code, severity_level, base_score, auto_flag_threshold, description) "
                   + "VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;

        try {
            conn = DBUtils.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, reason.getReasonCode());
                ps.setString(2, reason.getSeverityLevel());
                ps.setDouble(3, reason.getBaseScore());
                ps.setDouble(4, reason.getAutoFlagThreshold());
                ps.setString(5, reason.getDescription());

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("[ReportReasonDAO.createReportReason] " + e.getMessage());
        } finally {
            DBUtils.closeConnection(conn);
        }
        return false;
    }

    /**
     * Deletes a report reason configuration using its reason code.
     */
    public boolean deleteReportReason(ReportReason reason) {
        String sql = "DELETE FROM report_reason_configs WHERE reason_code = ?";
        Connection conn = null;

        try {
            conn = DBUtils.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, reason.getReasonCode());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("[ReportReasonDAO.deleteReportReason] " + e.getMessage());
        } finally {
            DBUtils.closeConnection(conn);
        }
        return false;
    }

    /**
     * Fetches a specific report reason's details based on its unique reason code string.
     * Returns null if no matching record is found.
     */
    public ReportReason getReasonByCode(String reasonCode) {
        String sql = "SELECT *"
                   + "FROM report_reason_configs WHERE reason_code = ?";
        Connection conn = null;

        try {
            conn = DBUtils.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, reasonCode);
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ReportReason reason = new ReportReason();
                        reason.setReasonCode(rs.getString("reason_code"));
                        reason.setSeverityLevel(rs.getString("severity_level"));
                        reason.setBaseScore(rs.getDouble("base_score"));
                        reason.setAutoFlagThreshold(rs.getDouble("auto_flag_threshold"));
                        reason.setDescription(rs.getString("description"));
                        return reason;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("[ReportReasonDAO.getReasonByCode] " + e.getMessage());
        } finally {
            DBUtils.closeConnection(conn);
        }
        return null;
    }
}