package Model.DAO;

import Model.DTO.Document;
import Model.DTO.Folder;
import Utils.DBUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class FolderDAO {

    public boolean createFolder(Folder folder) {
        // Thêm trường parent_folder_id và sharing_permission vào câu lệnh SQL
        String sql = "INSERT INTO folders (user_id, parent_folder_id, folder_name, sharing_permission) VALUES (?, ?, ?, ?)";

        try ( Connection conn = Utils.DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, folder.getUserId());

            // Kiểm tra nếu parentFolderId có giá trị thì lưu vào, nếu null thì set Types.INTEGER về NULL
            if (folder.getParentFolderId() != null) {
                ps.setInt(2, folder.getParentFolderId());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }

            ps.setString(3, folder.getFolderName());
            ps.setString(4, folder.getSharingPermission() != null ? folder.getSharingPermission() : "PRIVATE");

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[FolderDAO.createFolder] Lỗi khi tạo thư mục: " + e.getMessage());
        }
        return false;
    }

    public List<Folder> getFoldersByUserId(int userId) {
        List<Folder> list = new ArrayList<>();
        String sql = "SELECT * FROM folders WHERE user_id = ? AND parent_folder_id IS NULL ORDER BY created_at DESC";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try ( ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Folder f = new Folder();
                    f.setFolderId(rs.getInt("folder_id"));
                    f.setUserId(rs.getInt("user_id"));
                    f.setFolderName(rs.getString("folder_name"));

                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        f.setCreatedAt(ts.toLocalDateTime());
                    }

                    list.add(f);
                }
            }
        } catch (SQLException e) {
            System.err.println("[FolderDAO.getFoldersByUserId] " + e.getMessage());
        }
        return list;
    }

    /**
     * Lấy TOÀN BỘ thư mục (cả gốc lẫn con) của người dùng để hiển thị vào
     * Dropdown list.
     */
    public List<Folder> getAllFoldersByUserId(int userId) {
        List<Folder> list = new ArrayList<>();
        // Không dùng parent_folder_id IS NULL ở đây để lấy hết mọi cấp
        String sql = "SELECT * FROM folders WHERE user_id = ? ORDER BY created_at DESC";

        try ( java.sql.Connection conn = Utils.DBUtils.getConnection();  java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try ( java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Folder f = new Folder();
                    f.setFolderId(rs.getInt("folder_id"));
                    f.setUserId(rs.getInt("user_id"));

                    int parentId = rs.getInt("parent_folder_id");
                    f.setParentFolderId(rs.wasNull() ? null : parentId);

                    f.setFolderName(rs.getString("folder_name"));
                    f.setSharingPermission(rs.getString("sharing_permission"));

                    java.sql.Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        f.setCreatedAt(ts.toLocalDateTime());
                    }
                    list.add(f);
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("[FolderDAO.getAllFoldersByUserId] " + e.getMessage());
        }
        return list;
    }

    /**
     * Cập nhật toàn diện thông tin thư mục bao gồm tên, thư mục cha và quyền
     * chia sẻ. Tránh bẫy logic khóa ngoại vòng lặp bằng cách lọc ở tầng hiển
     * thị.
     */
    public boolean updateFolderInfo(int folderId, int userId, String newName, Integer newParentId, String newPermission) {
        String sql = "UPDATE folders SET folder_name = ?, parent_folder_id = ?, sharing_permission = ? WHERE folder_id = ? AND user_id = ?";

        try ( Connection conn = Utils.DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newName);
            if (newParentId != null) {
                ps.setInt(2, newParentId);
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setString(3, newPermission);
            ps.setInt(4, folderId);
            ps.setInt(5, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[FolderDAO.updateFolderInfo] Lỗi cập nhật thư mục: " + e.getMessage());
        }
        return false;
    }

    // ─── LẤY TẤT CẢ TÀI LIỆU TRONG CÂY THƯ MỤC ĐỂ XOÁ FILE VẬT LÝ ─────────────────
    /**
     * Sử dụng SQL Server CTE để tìm toàn bộ tài liệu nằm trong thư mục hiện tại
     * và tất cả các thư mục con lồng bên trong nó (vô hạn cấp).
     */
    public List<Document> getRecursiveDocumentsInFolder(int folderId, int userId) {
        List<Document> list = new ArrayList<>();
        String sql = "WITH FolderCTE AS ("
                + "    SELECT folder_id FROM folders WHERE folder_id = ? AND user_id = ?"
                + "    UNION ALL"
                + "    SELECT f.folder_id FROM folders f"
                + "    INNER JOIN FolderCTE cte ON f.parent_folder_id = cte.folder_id"
                + ") "
                + "SELECT d.* FROM documents d "
                + "WHERE d.folder_id IN (SELECT folder_id FROM FolderCTE)";

        try ( Connection conn = Utils.DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, folderId);
            ps.setInt(2, userId);

            try ( ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Document doc = new Document();
                    doc.setDocumentId(rs.getInt("document_id"));
                    doc.setTitle(rs.getString("title"));
                    doc.setCloudStorageUrl(rs.getString("cloud_storage_url"));
                    doc.setFileSizeMb(rs.getDouble("file_size_mb"));
                    list.add(doc);
                }
            }
        } catch (SQLException e) {
            System.err.println("[FolderDAO.getRecursiveDocumentsInFolder] Lỗi: " + e.getMessage());
        }
        return list;
    }

// ─── XOÁ ĐỆ QUY THƯ MỤC VÀ TÀI LIỆU TRONG DATABASE (CẬP NHẬT) ─────────────────
    /**
     * Xóa sạch toàn bộ tài liệu và thư mục con trong một Transaction duy nhất
     * sử dụng CTE.
     */
    public boolean deleteFolderAndDocumentsRecursive(int folderId, int userId) {
        // Câu lệnh xóa sạch tài liệu thuộc cây thư mục
        String deleteDocsSql
                = "WITH FolderCTE AS ("
                + "    SELECT folder_id FROM folders WHERE folder_id = ? AND user_id = ?"
                + "    UNION ALL"
                + "    SELECT f.folder_id FROM folders f"
                + "    INNER JOIN FolderCTE cte ON f.parent_folder_id = cte.folder_id"
                + ") "
                + "DELETE FROM documents WHERE folder_id IN (SELECT folder_id FROM FolderCTE);";

        // Câu lệnh xóa sạch cấu trúc các thư mục thuộc cây thư mục
        String deleteFoldersSql
                = "WITH FolderCTE AS ("
                + "    SELECT folder_id FROM folders WHERE folder_id = ? AND user_id = ?"
                + "    UNION ALL"
                + "    SELECT f.folder_id FROM folders f"
                + "    INNER JOIN FolderCTE cte ON f.parent_folder_id = cte.folder_id"
                + ") "
                + "DELETE FROM folders WHERE folder_id IN (SELECT folder_id FROM FolderCTE);";

        try ( Connection conn = Utils.DBUtils.getConnection()) {
            // 1. Khởi động Transaction tập trung
            conn.setAutoCommit(false);

            try ( PreparedStatement psDocs = conn.prepareStatement(deleteDocsSql);  PreparedStatement psFolder = conn.prepareStatement(deleteFoldersSql)) {

                // 2. Xóa toàn bộ các tài liệu con trước để bảo toàn toàn vẹn dữ liệu
                psDocs.setInt(1, folderId);
                psDocs.setInt(2, userId);
                psDocs.executeUpdate();

                // 3. Xóa thư mục cha và toàn bộ hệ thống thư mục con lồng bên trong
                psFolder.setInt(1, folderId);
                psFolder.setInt(2, userId);
                int foldersDeleted = psFolder.executeUpdate();

                // 4. Commit bảo toàn dữ liệu nếu chuỗi hành động thành công
                conn.commit();
                return foldersDeleted > 0;

            } catch (SQLException ex) {
                // Quay lui trạng thái nếu gặp trục trặc bất kỳ
                conn.rollback();
                System.err.println("[FolderDAO.deleteFolderAndDocumentsRecursive] Transaction rolled back: " + ex.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("[FolderDAO.deleteFolderAndDocumentsRecursive] Connection error: " + e.getMessage());
        }
        return false;
    }

    public List<Folder> getChildFolders(int userId, Integer parentFolderId) {
        List<Folder> list = new ArrayList<>();
        String sql;

        // Kiểm tra nếu parentFolderId là null thì tìm các thư mục gốc, ngược lại tìm thư mục con tương ứng
        if (parentFolderId == null) {
            sql = "SELECT * FROM folders WHERE user_id = ? AND parent_folder_id IS NULL ORDER BY created_at DESC";
        } else {
            sql = "SELECT * FROM folders WHERE user_id = ? AND parent_folder_id = ? ORDER BY created_at DESC";
        }

        try ( Connection conn = Utils.DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            if (parentFolderId != null) {
                ps.setInt(2, parentFolderId);
            }

            try ( ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Folder f = new Folder();
                    f.setFolderId(rs.getInt("folder_id"));
                    f.setUserId(rs.getInt("user_id"));

                    // Xử lý trường parent_folder_id có thể bị NULL trong DB
                    int parentId = rs.getInt("parent_folder_id");
                    f.setParentFolderId(rs.wasNull() ? null : parentId);

                    f.setFolderName(rs.getString("folder_name"));
                    f.setSharingPermission(rs.getString("sharing_permission"));

                    java.sql.Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        f.setCreatedAt(ts.toLocalDateTime());
                    }

                    list.add(f);
                }
            }
        } catch (SQLException e) {
            System.err.println("[FolderDAO.getChildFolders] Lỗi truy vấn thư mục con: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public Folder getFolderById(int folderId) {
        String sql = "SELECT * FROM folders WHERE folder_id = ?";

        try ( Connection conn = Utils.DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, folderId);

            try ( ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Folder f = new Folder();
                    f.setFolderId(rs.getInt("folder_id"));
                    f.setUserId(rs.getInt("user_id"));

                    int parentId = rs.getInt("parent_folder_id");
                    f.setParentFolderId(rs.wasNull() ? null : parentId);

                    f.setFolderName(rs.getString("folder_name"));
                    f.setSharingPermission(rs.getString("sharing_permission"));

                    java.sql.Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        f.setCreatedAt(ts.toLocalDateTime());
                    }
                    return f;
                }
            }
        } catch (SQLException e) {
            System.err.println("[FolderDAO.getFolderById] Lỗi tìm thư mục theo ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
