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
        String sql = "INSERT INTO folders (user_id, parent_folder_id, folder_name, sharing_permission) VALUES (?, ?, ?, ?)";

        try ( Connection conn = Utils.DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, folder.getUserId());
            if (folder.getParentFolderId() != null) {
                ps.setInt(2, folder.getParentFolderId());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setString(3, folder.getFolderName());
            ps.setString(4, folder.getSharingPermission() != null ? folder.getSharingPermission() : "PRIVATE");
            
            boolean check = ps.executeUpdate() > 0;
            
            try ( ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int folderId = rs.getInt(1);
                    folder.setFolderId(folderId);
                }
            }
            return check;
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

    public List<Folder> getAllFoldersByUserId(int userId) {
        List<Folder> list = new ArrayList<>();
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

    public boolean deleteFolderAndDocumentsRecursive(int folderId, int userId) {
        String deleteDocsSql
                = "WITH FolderCTE AS ("
                + "    SELECT folder_id FROM folders WHERE folder_id = ? AND user_id = ?"
                + "    UNION ALL"
                + "    SELECT f.folder_id FROM folders f"
                + "    INNER JOIN FolderCTE cte ON f.parent_folder_id = cte.folder_id"
                + ") "
                + "DELETE FROM documents WHERE folder_id IN (SELECT folder_id FROM FolderCTE);";

        String deleteFoldersSql
                = "WITH FolderCTE AS ("
                + "    SELECT folder_id FROM folders WHERE folder_id = ? AND user_id = ?"
                + "    UNION ALL"
                + "    SELECT f.folder_id FROM folders f"
                + "    INNER JOIN FolderCTE cte ON f.parent_folder_id = cte.folder_id"
                + ") "
                + "DELETE FROM folders WHERE folder_id IN (SELECT folder_id FROM FolderCTE);";

        try ( Connection conn = Utils.DBUtils.getConnection()) {
            conn.setAutoCommit(false);
            try ( PreparedStatement psDocs = conn.prepareStatement(deleteDocsSql);  PreparedStatement psFolder = conn.prepareStatement(deleteFoldersSql)) {
                psDocs.setInt(1, folderId);
                psDocs.setInt(2, userId);
                psDocs.executeUpdate();

                psFolder.setInt(1, folderId);
                psFolder.setInt(2, userId);
                int foldersDeleted = psFolder.executeUpdate();

                conn.commit();
                return foldersDeleted > 0;
            } catch (SQLException ex) {
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

    public String buildFolderTree(List<Folder> allFolders, List<Document> allDocuments) {
        StringBuilder tree = new StringBuilder();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

        List<Folder> rootFolders = new ArrayList<>();
        for (Folder f : allFolders) {
            if (f.getParentFolderId() == null) {
                rootFolders.add(f);
            }
        }

        for (Folder rootFolder : rootFolders) {
            buildFolderTreeRecursive(tree, rootFolder, allFolders, allDocuments, 1, formatter);
        }

        for (Document doc : allDocuments) {
            if (doc.getFolderId() == null) {
                String docDateStr = (doc.getCreatedAt() != null)
                        ? " (" + doc.getCreatedAt().format(formatter) + ")"
                        : "";
                tree.append("- [Doc ID: ").append(doc.getDocumentId()).append("] ") 
                        .append(doc.getTitle())
                        .append(docDateStr)
                        .append("\n");
            }
        }

        if (tree.length() == 0) {
            tree.append("(Trống — Sinh viên chưa có thư mục hoặc tài liệu nào)");
        }

        return tree.toString().trim();
    }

    private void buildFolderTreeRecursive(StringBuilder tree, Folder currentFolder,
            List<Folder> allFolders, List<Document> allDocuments, int depth, java.time.format.DateTimeFormatter formatter) {

        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            prefix.append("-");
        }
        String depthPrefix = prefix.toString();

        String folderDateStr = (currentFolder.getCreatedAt() != null)
                ? " (" + currentFolder.getCreatedAt().format(formatter) + ")"
                : "";

        tree.append(depthPrefix)
                .append(" [ID: ").append(currentFolder.getFolderId()).append("] ")
                .append(currentFolder.getFolderName())
                .append(folderDateStr)
                .append("\n");

        for (Document doc : allDocuments) {
            if (doc.getFolderId() != null && doc.getFolderId() == currentFolder.getFolderId()) {
                String docDateStr = (doc.getCreatedAt() != null)
                        ? " (" + doc.getCreatedAt().format(formatter) + ")"
                        : "";
                tree.append(depthPrefix)
                        .append("- [Doc ID: ").append(doc.getDocumentId()).append("] ") 
                        .append(doc.getTitle())
                        .append(docDateStr)
                        .append("\n");
            }
        }

        for (Folder child : allFolders) {
            if (child.getParentFolderId() != null && child.getParentFolderId() == currentFolder.getFolderId()) {
                buildFolderTreeRecursive(tree, child, allFolders, allDocuments, depth + 1, formatter);
            }
        }
    }

    public boolean isFolderNameExists(int userId, String folderName, Integer parentFolderId) {
        String sql;
        if (parentFolderId == null) {
            sql = "SELECT COUNT(*) FROM folders WHERE user_id = ? AND folder_name = ? AND parent_folder_id IS NULL";
        } else {
            sql = "SELECT COUNT(*) FROM folders WHERE user_id = ? AND folder_name = ? AND parent_folder_id = ?";
        }

        try ( java.sql.Connection conn = Utils.DBUtils.getConnection();  java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, folderName);
            if (parentFolderId != null) {
                ps.setInt(3, parentFolderId);
            }
            try ( java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("[FolderDAO.isFolderNameExists] Lỗi kiểm tra trùng tên: " + e.getMessage());
        }
        return false;
    }

    // ─── LẤY FOLDER CÔNG KHAI CHO TRANG KHÁM PHÁ ──────────────────────────
    public List<Folder> getPublicFolders() {
        List<Folder> list = new ArrayList<>();
        String sql = "SELECT * FROM folders WHERE sharing_permission = 'PUBLIC' ORDER BY created_at DESC";
        
        try (Connection conn = Utils.DBUtils.getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
             
            while (rs.next()) {
                Folder f = new Folder();
                f.setFolderId(rs.getInt("folder_id"));
                f.setUserId(rs.getInt("user_id"));
                f.setFolderName(rs.getString("folder_name"));
                f.setSharingPermission(rs.getString("sharing_permission"));
                list.add(f);
            }
        } catch (SQLException e) {
            System.err.println("[FolderDAO] getPublicFolders Error: " + e.getMessage());
        }
        return list;
    }
}