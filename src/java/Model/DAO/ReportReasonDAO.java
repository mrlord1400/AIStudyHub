package Model.DAO;

import Model.DTO.ReportReasonConfig;
import Utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportReasonDAO {

    // 1. LẤY DANH SÁCH (READ)
    public List<ReportReasonConfig> getAllReasons() {
        List<ReportReasonConfig> list = new ArrayList<>();
        String sql = "SELECT * FROM report_reason_configs";

        try (Connection conn = DBUtils.getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql); 
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                ReportReasonConfig config = new ReportReasonConfig(
                    rs.getString("reason_code"),
                    rs.getString("severity_level"),
                    rs.getDouble("base_score"),
                    rs.getDouble("auto_flag_threshold"),
                    rs.getString("description")
                );
                list.add(config);
            }
        } catch (SQLException e) {
            System.err.println("[ReportReasonDAO] getAllReasons Error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // 2. THÊM LÝ DO MỚI (CREATE)
    public boolean addReason(ReportReasonConfig config) {
        String sql = "INSERT INTO report_reason_configs (reason_code, severity_level, base_score, auto_flag_threshold, description) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBUtils.getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, config.getReasonCode());
            ps.setString(2, config.getSeverityLevel());
            ps.setDouble(3, config.getBaseScore());
            ps.setDouble(4, config.getAutoFlagThreshold());
            ps.setString(5, config.getDescription());
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ReportReasonDAO] addReason Error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // 3. CẬP NHẬT LÝ DO (UPDATE)
    // Lưu ý: reason_code là khóa chính nên không được sửa, chỉ sửa các thông số khác dựa trên reason_code
    public boolean updateReason(ReportReasonConfig config) {
        String sql = "UPDATE report_reason_configs SET severity_level = ?, base_score = ?, "
                   + "auto_flag_threshold = ?, description = ? WHERE reason_code = ?";

        try (Connection conn = DBUtils.getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, config.getSeverityLevel());
            ps.setDouble(2, config.getBaseScore());
            ps.setDouble(3, config.getAutoFlagThreshold());
            ps.setString(4, config.getDescription());
            ps.setString(5, config.getReasonCode());
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ReportReasonDAO] updateReason Error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // 4. XÓA LÝ DO (DELETE)
    public boolean deleteReason(String reasonCode) {
        String sql = "DELETE FROM report_reason_configs WHERE reason_code = ?";

        try (Connection conn = DBUtils.getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, reasonCode);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ReportReasonDAO] deleteReason Error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}