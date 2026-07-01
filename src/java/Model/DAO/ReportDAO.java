package Model.DAO;

import Model.DTO.Report;
import Utils.DBUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {

    public boolean createReport(Report report) {
        String sql = "INSERT INTO document_reports(document_id, reporter_id, reason_code, additional_details, status) "
                   + "VALUES(?,?,?,?,?)";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, report.getDocumentId());
            ps.setInt(2, report.getReporterId());
            ps.setString(3, report.getReasonCode());
            ps.setString(4, report.getDetails());
            ps.setString(5, report.getStatus());

            boolean isInserted = ps.executeUpdate() > 0;

            if (isInserted) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int reportId = rs.getInt(1);
                        if (reportId > 0) {
                            report.setReportId(reportId); 
                            System.out.println("[ReportDAO.createReport] Đồng bộ thành công ID mới: " + reportId);
                        }
                    }
                }
            }
            return isInserted;
        } catch (SQLException e) {
            System.err.println("[ReportDAO.createReport] SQL Error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean updateReport(Report report) {
        String sql = "UPDATE document_reports SET "
                   + "document_id = ?, reporter_id = ?, reason_code = ?, additional_details = ?, "
                   + "status = ?, resolved_at = ?, resolved_by_admin_id = ? "
                   + "WHERE report_id = ?";

        // 🔥 Khắc phục: Đưa connection vào try-with-resources để tự động đóng an toàn
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, report.getDocumentId());
            ps.setInt(2, report.getReporterId());
            ps.setString(3, report.getReasonCode());
            ps.setString(4, report.getDetails());
            ps.setString(5, report.getStatus());
            
            // Xử lý an toàn trường ngày tháng LocalDateTime có thể bị null
            if (report.getResolvedAt() != null) {
                ps.setTimestamp(6, Timestamp.valueOf(report.getResolvedAt()));
            } else {
                ps.setNull(6, java.sql.Types.TIMESTAMP);
            }
            
            // Xử lý an toàn trường Admin ID có thể bị null khi report ở trạng thái PENDING
            if (report.getAdminId() > 0) {
                ps.setInt(7, report.getAdminId());
            } else {
                ps.setNull(7, java.sql.Types.INTEGER);
            }
            
            ps.setInt(8, report.getReportId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ReportDAO.updateReport] SQL Error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean checkValid(int reporterId, int documentId) {
        String sql = "SELECT COUNT(*) FROM document_reports "
                   + "WHERE reporter_id = ? AND document_id = ? AND status = 'PENDING'";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, reporterId);
            ps.setInt(2, documentId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) == 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ReportDAO.checkValid] SQL Error: " + e.getMessage());
        }
        return false; 
    }

    public List<Report> getReportList(int documentId) {
        List<Report> list = new ArrayList<>();
        String sql = "SELECT report_id, document_id, reporter_id, reason_code, additional_details, "
                   + "status, created_at, resolved_at, resolved_by_admin_id " // 🔥 Đồng bộ tên cột ở đây
                   + "FROM document_reports WHERE document_id = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, documentId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Report report = new Report();
                    report.setReportId(rs.getInt("report_id"));
                    report.setDocumentId(rs.getInt("document_id"));
                    report.setReporterId(rs.getInt("reporter_id"));
                    report.setReasonCode(rs.getString("reason_code"));
                    report.setDetails(rs.getString("additional_details"));
                    report.setStatus(rs.getString("status"));
                    
                    Timestamp createdTS = rs.getTimestamp("created_at");
                    if (createdTS != null) {
                        report.setCreatedAt(createdTS.toLocalDateTime());
                    }
                    
                    Timestamp resolvedTS = rs.getTimestamp("resolved_at");
                    if (resolvedTS != null) {
                        report.setResolvedAt(resolvedTS.toLocalDateTime());
                    }
                    
                    report.setAdminId(rs.getInt("resolved_by_admin_id")); // 🔥 Đồng bộ mapping
                    
                    list.add(report);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ReportDAO.getReportList] SQL Error: " + e.getMessage());
        }
        return list;
    }

    public boolean deleteReport(Report report) {
        String sql = "DELETE FROM document_reports WHERE report_id = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, report.getReportId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ReportDAO.deleteReport] SQL Error: " + e.getMessage());
        }
        return false;
    }
    
    public Report getReportByUserAndDoc(int reporterId, int documentId) {
        String sql = "SELECT report_id, document_id, reporter_id, reason_code, additional_details, "
                   + "status, created_at, resolved_at, resolved_by_admin_id " // 🔥 Đồng bộ tên cột ở đây
                   + "FROM document_reports "
                   + "WHERE reporter_id = ? AND document_id = ? AND status = 'PENDING'";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, reporterId);
            ps.setInt(2, documentId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Report report = new Report();
                    report.setReportId(rs.getInt("report_id"));
                    report.setDocumentId(rs.getInt("document_id"));
                    report.setReporterId(rs.getInt("reporter_id"));
                    report.setReasonCode(rs.getString("reason_code"));
                    report.setDetails(rs.getString("additional_details"));
                    report.setStatus(rs.getString("status"));
                    
                    java.sql.Timestamp createdTS = rs.getTimestamp("created_at");
                    if (createdTS != null) {
                        report.setCreatedAt(createdTS.toLocalDateTime());
                    }
                    
                    java.sql.Timestamp resolvedTS = rs.getTimestamp("resolved_at");
                    if (resolvedTS != null) {
                        report.setResolvedAt(resolvedTS.toLocalDateTime());
                    }
                    
                    report.setAdminId(rs.getInt("resolved_by_admin_id")); // 🔥 Đồng bộ mapping
                    
                    return report;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ReportDAO.getReportByUserAndDoc] SQL Error: " + e.getMessage());
        }
        return null;
    }
}