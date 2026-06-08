package Model;

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
        String sql = "INSERT INTO folders (user_id, folder_name) VALUES (?, ?)";
        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, folder.getUserId());
            ps.setString(2, folder.getFolderName());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[FolderDAO.createFolder] " + e.getMessage());
        }
        return false;
    }

    public List<Folder> getFoldersByUserId(int userId) {
        List<Folder> list = new ArrayList<>();
        String sql = "SELECT * FROM folders WHERE user_id = ? ORDER BY created_at DESC";

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

    public boolean renameFolder(int folderId, int userId, String newName) {
        String sql = "UPDATE folders SET folder_name = ? WHERE folder_id = ? AND user_id = ?";
        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newName);
            ps.setInt(2, folderId);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[FolderDAO.renameFolder] " + e.getMessage());
        }
        return false;
    }

    public boolean deleteFolder(int folderId, int userId) {
        // Note: Make sure your SQL Server database has ON DELETE CASCADE 
        // on the documents table's foreign key, or you'll need to manually delete/update documents first!
        String sql = "DELETE FROM folders WHERE folder_id = ? AND user_id = ?";
        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, folderId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[FolderDAO.deleteFolder] " + e.getMessage());
        }
        return false;
    }

    public boolean deleteFolderAndDocuments(int folderId, int userId) {
        String deleteDocsSql = "DELETE FROM documents WHERE folder_id = ?";
        String deleteFolderSql = "DELETE FROM folders WHERE folder_id = ? AND user_id = ?";

        try ( Connection conn = DBUtils.getConnection()) {
            // 1. Start a single transaction
            conn.setAutoCommit(false);

            try ( PreparedStatement psDocs = conn.prepareStatement(deleteDocsSql);  PreparedStatement psFolder = conn.prepareStatement(deleteFolderSql)) {

                // 2. Delete the child documents first
                psDocs.setInt(1, folderId);
                psDocs.executeUpdate();

                // 3. Delete the parent folder
                psFolder.setInt(1, folderId);
                psFolder.setInt(2, userId);
                int folderDeleted = psFolder.executeUpdate();

                // 4. Commit the transaction if both succeed
                conn.commit();
                return folderDeleted > 0;

            } catch (SQLException ex) {
                // If anything fails, rollback everything to prevent partial data loss
                conn.rollback();
                System.err.println("[FolderDAO.deleteFolderAndDocuments] Transaction rolled back: " + ex.getMessage());
            } finally {
                // Restore auto-commit behavior for the connection pool
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("[FolderDAO.deleteFolderAndDocuments] Connection error: " + e.getMessage());
        }
        return false;
    }
}
