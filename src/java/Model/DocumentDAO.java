package Model;

import Utils.DBUtils;
import java.sql.*;
import java.time.LocalDateTime;

/**
 * DAO (Data Access Object) for the `documents` table.
 *
 * Provides the following operations: - insertDocument() : Insert a new document
 * into the DB (Step 1 — save file) - updateDocumentInfo() : Update document
 * info after user edits (Step 2) - updateSharingPermission() : Quick-update
 * only the sharing permission (used by the "Chỉnh permission" modal) -
 * deleteDocument() : Delete the record when user cancels (Step 3) - findById()
 * : Retrieve a document by ID (used to pre-fill the edit form)
 */
public class DocumentDAO {

    // ─── INSERT ──────────────────────────────────────────────────────────────
    /**
     * Inserts a new document into the database immediately after the file is
     * uploaded. Uses the original filename as the default title.
     *
     * @return the generated documentId (auto-increment), or -1 on failure.
     */
    /**
     * Chèn một tài liệu mới vào cơ sở dữ liệu ngay sau khi tải lên thành công.
     * Đã cập nhật để lưu thêm trường file_extension.
     *
     * @param doc Đối tượng Document chứa thông tin tệp tải lên
     * @return ID tự tăng (document_id) vừa được sinh ra, hoặc -1 nếu thất bại.
     */
    public int insertDocument(Document doc) {
        String sql = "INSERT INTO documents "
                + "(user_id, folder_id, title, file_extension, cloud_storage_url, file_size_mb, "
                + " ai_parsing_status, sharing_permission, share_link_token, is_flagged, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"; // 11 dấu chấm hỏi dữ liệu

        // Cấu hình String[]{"document_id"} để ép SQL Server trả về khóa tự tăng chính xác
        try ( Connection conn = Utils.DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql, new String[]{"document_id"})) {

            ps.setInt(1, doc.getUserId());

            if (doc.getFolderId() != null) {
                ps.setInt(2, doc.getFolderId());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }

            ps.setString(3, doc.getTitle());
            ps.setString(4, doc.getFileExtension()); // Trường mới thêm
            ps.setString(5, doc.getCloudStorageUrl());
            ps.setDouble(6, doc.getFileSizeMb());
            ps.setString(7, doc.getAiParsingStatus());
            ps.setString(8, doc.getSharingPermission());

            if (doc.getShareLinkToken() != null) {
                ps.setString(9, doc.getShareLinkToken());
            } else {
                ps.setNull(9, java.sql.Types.VARCHAR);
            }

            ps.setBoolean(10, doc.isFlagged());
            ps.setTimestamp(11, java.sql.Timestamp.valueOf(
                    doc.getCreatedAt() != null ? doc.getCreatedAt() : java.time.LocalDateTime.now()));

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                System.err.println("[DocumentDAO] Thêm tài liệu thất bại, không có hàng nào bị ảnh hưởng.");
                return -1;
            }

            try ( ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1); // Trả về document_id vừa sinh ra
                }
            }

        } catch (SQLException e) {
            System.err.println("[DocumentDAO] insertDocument SQL Error: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────
    /**
     * Updates document info after the user edits the form. Only the fields the
     * user is allowed to change are updated: title, folder_id,
     * sharing_permission.
     *
     * @return true if the update was successful.
     */
    public boolean updateDocumentInfo(int documentId, String newTitle,
            Integer newFolderId, String newSharingPermission, String newCloudStorageUrl) {
        String sql = "UPDATE documents "
                + "SET title = ?, folder_id = ?, sharing_permission = ?, cloud_storage_url = ? "
                + "WHERE document_id = ?";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newTitle);

            if (newFolderId != null) {
                ps.setInt(2, newFolderId);
            } else {
                ps.setNull(2, Types.INTEGER);
            }

            ps.setString(3, newSharingPermission);
            ps.setString(4, newCloudStorageUrl);
            ps.setInt(5, documentId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[DocumentDAO] updateDocumentInfo failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ─── UPDATE SHARING PERMISSION ONLY ─────────────────────────────────────
    /**
     * Quick-update for ONLY the sharing_permission column. Used by the "Chỉnh
     * permission" modal on document_view.jsp, which intentionally does not
     * carry title/folder data.
     *
     * @param documentId the document to update
     * @param newSharingPermission new value (PRIVATE / FRIENDS_ONLY / PUBLIC)
     * @return true if the update was successful.
     */
    public boolean updateSharingPermission(int documentId, String newSharingPermission) {
        String sql = "UPDATE documents SET sharing_permission = ? WHERE document_id = ?";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newSharingPermission);
            ps.setInt(2, documentId);
    // ─── REPLACE DOCUMENT FILE ──────────────────────────────────────────────
    /**
     * Cập nhật metadata file của bản ghi cũ khi người dùng chọn "Thay thế".
     * Giữ nguyên document_id, created_at, share_link_token, is_flagged.
     * Trigger trg_documents_updated_at tự động cập nhật updated_at.
     *
     * @param docId ID bản ghi cũ cần cập nhật
     * @param newCloudUrl URL file mới
     * @param newFileSizeMb Kích thước file mới
     * @param newFileExtension Đuôi file mới
     * @param newTitle Tiêu đề mới
     * @param newFolderId Thư mục mới
     * @param newSharingPermission Quyền chia sẻ mới
     * @return true nếu cập nhật thành công
     */
    public boolean replaceDocumentFile(int docId, String newCloudUrl,
            double newFileSizeMb, String newFileExtension,
            String newTitle, Integer newFolderId, String newSharingPermission) {
        String sql = "UPDATE documents "
                + "SET cloud_storage_url = ?, file_size_mb = ?, file_extension = ?, "
                + "    title = ?, folder_id = ?, sharing_permission = ? "
                + "WHERE document_id = ?";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newCloudUrl);
            ps.setDouble(2, newFileSizeMb);
            ps.setString(3, newFileExtension);
            ps.setString(4, newTitle);

            if (newFolderId != null) {
                ps.setInt(5, newFolderId);
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            ps.setString(6, newSharingPermission);
            ps.setInt(7, docId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[DocumentDAO] updateSharingPermission failed: " + e.getMessage());
            System.err.println("[DocumentDAO] replaceDocumentFile failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────
    /**
     * Deletes a document record from the database when the user clicks Cancel.
     * The Controller is also responsible for deleting the physical file on the
     * server/cloud.
     *
     * @return true if the deletion was successful.
     */
    public boolean deleteDocument(int documentId) {
        String sql = "DELETE FROM documents WHERE document_id = ?";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, documentId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[DocumentDAO] deleteDocument failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ─── FIND BY ID ──────────────────────────────────────────────────────────
    /**
     * Retrieves a document by its documentId. Used to pre-fill the edit form
     * for the user.
     *
     * @return a Document object, or null if not found.
     */
    public Document findById(int documentId) {
        String sql = "SELECT * FROM documents WHERE document_id = ?";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, documentId);

            try ( ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("[DocumentDAO] findById failed: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // ─── Helper ──────────────────────────────────────────────────────────────
    /**
     * Maps a ResultSet row to a Document object.
     */
    private Document mapRow(ResultSet rs) throws SQLException {
        Document doc = new Document();
        doc.setDocumentId(rs.getInt("document_id"));
        doc.setUserId(rs.getInt("user_id"));

        int folderId = rs.getInt("folder_id");
        doc.setFolderId(rs.wasNull() ? null : folderId);

        doc.setTitle(rs.getString("title"));
        doc.setFileExtension(rs.getString("file_extension"));
        doc.setCloudStorageUrl(rs.getString("cloud_storage_url"));
        doc.setFileSizeMb(rs.getDouble("file_size_mb"));
        doc.setAiParsingStatus(rs.getString("ai_parsing_status"));
        doc.setSharingPermission(rs.getString("sharing_permission"));
        doc.setShareLinkToken(rs.getString("share_link_token"));
        doc.setFlagged(rs.getBoolean("is_flagged"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            doc.setCreatedAt(ts.toLocalDateTime());
        }

        Timestamp tsUpdate = rs.getTimestamp("updated_at");
        if (tsUpdate != null) {
            doc.setUpdatedAt(tsUpdate.toLocalDateTime());
        }

        return doc;
    }

    /**
     * Retrieves all documents for a specific user to display on the dashboard.
     */
    public java.util.List<Document> getDocumentsByUserId(int userId) {
        java.util.List<Document> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM documents WHERE user_id = ? ORDER BY created_at DESC";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try ( ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DocumentDAO] getDocumentsByUserId failed: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * * Retrieves documents for a user inside a specific folder. If folderId
     * is null, it retrieves documents in the root directory.
     */
    public java.util.List<Document> getDocumentsByFolder(int userId, Integer folderId) {
        java.util.List<Document> list = new java.util.ArrayList<>();

        // Dynamic SQL based on whether we are looking inside a folder or at the root
        String sql;
        if (folderId == null) {
            sql = "SELECT * FROM documents WHERE user_id = ? AND folder_id IS NULL ORDER BY created_at DESC";
        } else {
            sql = "SELECT * FROM documents WHERE user_id = ? AND folder_id = ? ORDER BY created_at DESC";
        }

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            if (folderId != null) {
                ps.setInt(2, folderId);
            }

            try ( ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs)); // Assuming you have your mapRow helper method from before
                }
            }
        } catch (SQLException e) {
            System.err.println("[DocumentDAO] getDocumentsByFolder failed: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // ─── FOLDER CASCADE DELETE SUPPORT ──────────────────────────────────────
    /**
     * Retrieves all documents belonging to a specific folder. Used before
     * folder deletion to identify physical files that need cleanup.
     *
     * @param folderId the folder whose documents to retrieve
     * @return list of Document objects in the folder
     */
    public java.util.List<Document> getDocumentsByFolderId(int folderId) {
        java.util.List<Document> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM documents WHERE folder_id = ?";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, folderId);
            try ( ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DocumentDAO] getDocumentsByFolderId failed: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Deletes ALL document records that belong to a specific folder. Must be
     * called BEFORE deleting the folder itself.
     *
     * @param folderId the folder whose documents should be deleted
     * @return the number of documents deleted
     */
    public int deleteDocumentsByFolderId(int folderId) {
        String sql = "DELETE FROM documents WHERE folder_id = ?";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, folderId);
            return ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[DocumentDAO] deleteDocumentsByFolderId failed: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    // ─── DUPLICATE CHECK SUPPORT ────────────────────────────────────────────
    /**
     * Checks if a document with the same title already exists at the same
     * location (same user, same folder). Used during upload confirmation to
     * detect duplicates.
     *
     * @param userId the owner of the document
     * @param title the title to check for duplicates
     * @param folderId the folder to check in (null = root directory)
     * @return the existing Document if a duplicate is found, null otherwise
     */
    public Document findDuplicateByTitle(int userId, String title, Integer folderId) {
        String sql;
        if (folderId == null) {
            sql = "SELECT * FROM documents WHERE user_id = ? AND title = ? AND folder_id IS NULL";
        } else {
            sql = "SELECT * FROM documents WHERE user_id = ? AND title = ? AND folder_id = ?";
        }

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setString(2, title);
            if (folderId != null) {
                ps.setInt(3, folderId);
            }

            try ( ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DocumentDAO] findDuplicateByTitle failed: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Checks if a title already exists at a location, excluding a specific
     * document. Used when auto-generating names like "file (1)", "file (2)" to
     * find the next available number.
     *
     * @param userId the owner
     * @param title the title to check
     * @param folderId the folder (null = root)
     * @param excludeDocId document ID to exclude from the check (the new upload
     * itself)
     * @return true if a document with this title exists at this location
     */
    public boolean titleExistsAtLocation(int userId, String title, Integer folderId, int excludeDocId) {
        String sql;
        if (folderId == null) {
            sql = "SELECT COUNT(*) FROM documents WHERE user_id = ? AND title = ? AND folder_id IS NULL AND document_id != ?";
        } else {
            sql = "SELECT COUNT(*) FROM documents WHERE user_id = ? AND title = ? AND folder_id = ? AND document_id != ?";
        }

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setString(2, title);
            if (folderId == null) {
                ps.setInt(3, excludeDocId);
            } else {
                ps.setInt(3, folderId);
                ps.setInt(4, excludeDocId);
            }

            try ( ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[DocumentDAO] titleExistsAtLocation failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}
