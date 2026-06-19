package Model.DAO;

import Model.DTO.Subscription;
import Utils.DBUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SubscriptionDAO {

    // Lấy toàn bộ các gói (Guest, Free, Premium) để hiển thị lên UI
    public List<Subscription> getAllSubscriptions() {
        List<Subscription> list = new ArrayList<>();
        String sql = "SELECT * FROM subscriptions";

        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Subscription sub = new Subscription(
                        rs.getInt("tier_id"),
                        rs.getString("tier_name"),
                        rs.getInt("max_storage_mb"),
                        rs.getInt("ai_prompt_limit_per_day"),
                        rs.getDouble("price"),
                        rs.getInt("total_storage_mb")
                );
                list.add(sub);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // Cập nhật cấu hình khi Admin bấm Lưu
    public boolean updateSubscription(Subscription sub) {
        String sql = "UPDATE subscriptions SET max_storage_mb = ?, ai_prompt_limit_per_day = ?, price = ?, total_storage_mb = ? WHERE tier_id = ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sub.getMaxStorageMb());
            ps.setInt(2, sub.getAiPromptLimitPerDay());
            ps.setDouble(3, sub.getPrice());
            ps.setInt(4, sub.getTotalStorageMb());
            ps.setInt(5, sub.getTierId());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
