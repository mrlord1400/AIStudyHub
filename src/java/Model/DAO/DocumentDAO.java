package Model.DAO;

import Model.DTO.Document;
import Utils.DBUtils;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DocumentDAO {

    public int insertDocument(Document doc) {
        String sql = "INSERT INTO documents "
                + "(user_id, folder_id, title, file_extension, cloud_storage_url, file_size_mb, "
                + " ai_parsing_status, sharing_permission, share_link_token, is_flagged, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Utils.DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, new String[]{"document_id"})) {

            ps.setInt(1, doc.getUserId());

            if (doc.getFolderId() != null) {
                ps.setInt(2, doc.getFolderId());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }

            ps.setString(3, doc.getTitle());
            ps.setString(4, doc.getFileExtension());
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

            LocalDateTime now = LocalDateTime.now();
            ps.setTimestamp(11, java.sql.Timestamp.valueOf(
                    doc.getCreatedAt() != null ? doc.getCreatedAt() : now));
            ps.setTimestamp(12, java.sql.Timestamp.valueOf(
                    doc.getUpdatedAt() != null ? doc.getUpdatedAt() : now));

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                System.err.println("[DocumentDAO] Thêm tài liệu thất bại, không có hàng nào bị ảnh hưởng.");
                return -1;
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int docId = rs.getInt(1);
                    if (docId < 0) {
                        return docId;
                    } else {
                        doc.setDocumentId(docId);
                    }
                    return docId;
                }

            }

        } catch (SQLException e) {
            System.err.println("[DocumentDAO] insertDocument SQL Error: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public boolean updateDocumentInfo(int documentId, String newTitle,
            Integer newFolderId, String newSharingPermission, String newCloudStorageUrl) {
        String sql = "UPDATE documents "
                + "SET title = ?, folder_id = ?, sharing_permission = ?, cloud_storage_url = ?, updated_at = ? "
                + "WHERE document_id = ?";

        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newTitle);

            if (newFolderId != null) {
                ps.setInt(2, newFolderId);
            } else {
                ps.setNull(2, Types.INTEGER);
            }

            ps.setString(3, newSharingPermission);
            ps.setString(4, newCloudStorageUrl);
            ps.setTimestamp(5, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(6, documentId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[DocumentDAO] updateDocumentInfo failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateSharingPermission(int documentId, String newSharingPermission) {
        String sql = "UPDATE documents SET sharing_permission = ?, updated_at = ? WHERE document_id = ?";

        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newSharingPermission);
            ps.setTimestamp(2, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, documentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DocumentDAO] updateSharingPermission failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean replaceDocumentFile(int docId, String newCloudUrl,
            double newFileSizeMb, String newFileExtension,
            String newTitle, Integer newFolderId, String newSharingPermission) {
        String sql = "UPDATE documents "
                + "SET cloud_storage_url = ?, file_size_mb = ?, file_extension = ?, "
                + "    title = ?, folder_id = ?, sharing_permission = ?, updated_at = ? "
                + "WHERE document_id = ?";

        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
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
            ps.setTimestamp(7, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(8, docId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[DocumentDAO] replaceDocumentFile failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteDocument(int documentId) {
        String sql = "DELETE FROM documents WHERE document_id = ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, documentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DocumentDAO] deleteDocument failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public Document findById(int documentId) {
        String sql = "SELECT * FROM documents WHERE document_id = ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, documentId);
            try (ResultSet rs = ps.executeQuery()) {
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

        try {
            doc.setBookmarkCount(rs.getInt("bookmark_count"));
        } catch (SQLException ignored) {
        }
        try {
            doc.setDownloadCount(rs.getInt("download_count"));
        } catch (SQLException ignored) {
        }
        try {
            doc.setTotalReportScore(rs.getDouble("total_report_score"));
        } catch (SQLException ignored) {
        }

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

    public java.util.List<Document> getDocumentsByUserId(int userId) {
        java.util.List<Document> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM documents WHERE user_id = ? ORDER BY created_at DESC";

        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
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

    public java.util.List<Document> getDocumentsByFolder(int userId, Integer folderId) {
        java.util.List<Document> list = new java.util.ArrayList<>();
        String sql;
        if (folderId == null) {
            sql = "SELECT * FROM documents WHERE user_id = ? AND folder_id IS NULL ORDER BY created_at DESC";
        } else {
            sql = "SELECT * FROM documents WHERE user_id = ? AND folder_id = ? ORDER BY created_at DESC";
        }

        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            if (folderId != null) {
                ps.setInt(2, folderId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DocumentDAO] getDocumentsByFolder failed: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public java.util.List<Document> getDocumentsByFolderId(int folderId) {
        java.util.List<Document> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM documents WHERE folder_id = ?";

        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, folderId);
            try (ResultSet rs = ps.executeQuery()) {
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

    public int deleteDocumentsByFolderId(int folderId) {
        String sql = "DELETE FROM documents WHERE folder_id = ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, folderId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DocumentDAO] deleteDocumentsByFolderId failed: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    public Document findDuplicateByTitle(int userId, String title, Integer folderId) {
        String sql;
        if (folderId == null) {
            sql = "SELECT * FROM documents WHERE user_id = ? AND title = ? AND folder_id IS NULL";
        } else {
            sql = "SELECT * FROM documents WHERE user_id = ? AND title = ? AND folder_id = ?";
        }

        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, title);
            if (folderId != null) {
                ps.setInt(3, folderId);
            }
            try (ResultSet rs = ps.executeQuery()) {
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

    public boolean titleExistsAtLocation(int userId, String title, Integer folderId, int excludeDocId) {
        String sql;
        if (folderId == null) {
            sql = "SELECT COUNT(*) FROM documents WHERE user_id = ? AND title = ? AND folder_id IS NULL AND document_id != ?";
        } else {
            sql = "SELECT COUNT(*) FROM documents WHERE user_id = ? AND title = ? AND folder_id = ? AND document_id != ?";
        }

        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, title);
            if (folderId == null) {
                ps.setInt(3, excludeDocId);
            } else {
                ps.setInt(3, folderId);
                ps.setInt(4, excludeDocId);
            }
            try (ResultSet rs = ps.executeQuery()) {
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

    public Document findByTitleAndUserId(int userId, String title) {
        String sql = "SELECT * FROM documents WHERE user_id = ? AND title = ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, title);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DocumentDAO] findByTitleAndUserId exact failed: " + e.getMessage());
        }

        String sqlLike = "SELECT TOP 1 * FROM documents WHERE user_id = ? AND title LIKE ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sqlLike)) {
            ps.setInt(1, userId);
            ps.setString(2, "%" + title + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DocumentDAO] findByTitleAndUserId LIKE failed: " + e.getMessage());
        }
        return null;
    }

    public boolean updateAiParsingStatus(int documentId, String status) {
        String sql = "UPDATE documents SET ai_parsing_status = ? WHERE document_id = ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, documentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DocumentDAO] updateAiParsingStatus failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ─── TĂNG LƯỢT TẢI ────────────────────────────────────────────────────
    public boolean incrementDownloadCount(int documentId) {
        String sql = "UPDATE documents SET download_count = download_count + 1 WHERE document_id = ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, documentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DocumentDAO] incrementDownloadCount failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ─── LẤY DANH SÁCH KHÁM PHÁ (PUBLIC & FRIENDS) ──────────────────────────
    public List<Document> getExploreDocuments(int currentUserId, boolean isFriendsView) {
        List<Document> list = new ArrayList<>();
        String sql;

        if (isFriendsView) {
            sql = "SELECT d.*, u.username AS author_username, "
                    + "CASE WHEN b.bookmark_id IS NOT NULL THEN 1 ELSE 0 END AS is_bookmarked "
                    + "FROM documents d "
                    + "INNER JOIN users u ON d.user_id = u.user_id "
                    + "INNER JOIN friendships f ON (d.user_id = f.addressee_id OR d.user_id = f.requester_id) "
                    + "LEFT JOIN bookmarks b ON d.document_id = b.document_id AND b.user_id = ? "
                    + "WHERE d.sharing_permission = 'FRIENDS_ONLY' AND d.is_flagged = 0 "
                    + "AND (f.requester_id = ? OR f.addressee_id = ?) AND d.user_id != ? "
                    + "AND f.status = 'ACCEPTED' "
                    + "ORDER BY is_bookmarked DESC, COALESCE(d.updated_at, d.created_at) DESC";
        } else {
            sql = "SELECT d.*, u.username AS author_username, "
                    + "CASE WHEN b.bookmark_id IS NOT NULL THEN 1 ELSE 0 END AS is_bookmarked "
                    + "FROM documents d "
                    + "INNER JOIN users u ON d.user_id = u.user_id "
                    + "LEFT JOIN bookmarks b ON d.document_id = b.document_id AND b.user_id = ? "
                    + "WHERE d.sharing_permission = 'PUBLIC' AND d.is_flagged = 0 "
                    + "ORDER BY is_bookmarked DESC, COALESCE(d.updated_at, d.created_at) DESC";
        }

        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (isFriendsView) {
                ps.setInt(1, currentUserId);
                ps.setInt(2, currentUserId);
                ps.setInt(3, currentUserId);
                ps.setInt(4, currentUserId);
            } else {
                ps.setInt(1, currentUserId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Document doc = mapRow(rs);
                    try {
                        doc.setAuthorUsername(rs.getString("author_username"));
                    } catch (SQLException ignored) {
                    }

                    // 🔥 THÊM ĐOẠN NÀY ĐỂ LẤY TRẠNG THÁI BOOKMARK TỪ SQL
                    try {
                        doc.setIsBookmarked(rs.getInt("is_bookmarked") == 1);
                    } catch (SQLException ignored) {
                    }

                    list.add(doc);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DocumentDAO] getExploreDocuments Error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // ─── LẤY THỐNG KÊ (TOTAL DOCS, CONTRIBUTORS, DOWNLOADS) — theo đúng view ──
    public int[] getExploreStats(int currentUserId, boolean isFriendsView) {
        int[] stats = new int[3];
        String sql;

        if (isFriendsView) {
            sql = "SELECT COUNT(d.document_id) as total_docs, "
                    + "COUNT(DISTINCT d.user_id) as total_contributors, "
                    + "ISNULL(SUM(d.download_count), 0) as total_downloads "
                    + "FROM documents d "
                    + "INNER JOIN friendships f ON (d.user_id = f.addressee_id OR d.user_id = f.requester_id) "
                    + "WHERE d.sharing_permission = 'FRIENDS_ONLY' AND d.is_flagged = 0 "
                    + "AND (f.requester_id = ? OR f.addressee_id = ?) AND d.user_id != ? "
                    + "AND f.status = 'ACCEPTED'";
        } else {
            sql = "SELECT COUNT(document_id) as total_docs, "
                    + "COUNT(DISTINCT user_id) as total_contributors, "
                    + "ISNULL(SUM(download_count), 0) as total_downloads "
                    + "FROM documents WHERE sharing_permission = 'PUBLIC' AND is_flagged = 0";
        }

        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (isFriendsView) {
                ps.setInt(1, currentUserId);
                ps.setInt(2, currentUserId);
                ps.setInt(3, currentUserId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats[0] = rs.getInt("total_docs");
                    stats[1] = rs.getInt("total_contributors");
                    stats[2] = rs.getInt("total_downloads");
                }
            }
        } catch (SQLException e) {
            System.err.println("[DocumentDAO] getExploreStats Error: " + e.getMessage());
            e.printStackTrace();
        }
        return stats;
    }

    // ─── THAO TÁC TOGGLE BOOKMARK ──────────────────────────────────────────
    public boolean toggleBookmark(int userId, int documentId) {
        boolean isNowBookmarked = false;
        String checkSql = "SELECT bookmark_id FROM bookmarks WHERE user_id = ? AND document_id = ?";
        boolean exists = false;

        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, userId);
            ps.setInt(2, documentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    exists = true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try (Connection conn = DBUtils.getConnection()) {
            if (exists) {
                // Đã bookmark -> Xóa
                String delSql = "DELETE FROM bookmarks WHERE user_id = ? AND document_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(delSql)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, documentId);
                    ps.executeUpdate();
                }
                updateDocumentBookmarkCount(conn, documentId, -1);
                isNowBookmarked = false;
            } else {
                // Chưa bookmark -> Thêm
                String insSql = "INSERT INTO bookmarks (user_id, document_id) VALUES (?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insSql)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, documentId);
                    ps.executeUpdate();
                }
                updateDocumentBookmarkCount(conn, documentId, 1);
                isNowBookmarked = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return isNowBookmarked;
    }

    private void updateDocumentBookmarkCount(Connection conn, int documentId, int change) throws SQLException {
        String sql = "UPDATE documents SET bookmark_count = bookmark_count + ? WHERE document_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, change);
            ps.setInt(2, documentId);
            ps.executeUpdate();
        }
    }

    public int getBookmarkCount(int documentId) {
        String sql = "SELECT bookmark_count FROM documents WHERE document_id = ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, documentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Cập nhật điểm tích lũy báo cáo vi phạm và trạng thái cắm cờ của tài liệu.
     * Sử dụng trong luồng xử lý createReport khi tính toán lại điểm phạt.
     */
    public boolean updateReportMetrics(int documentId, double totalReportScore, boolean isFlagged) {
        String sql = "UPDATE documents SET total_report_score = ?, is_flagged = ?, updated_at = ? WHERE document_id = ?";

        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, totalReportScore);
            ps.setBoolean(2, isFlagged);
            ps.setTimestamp(3, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(4, documentId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DocumentDAO] updateReportMetrics failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteDocumentAndDependencies(int documentId) {
        boolean isSuccess = false;
        Connection conn = null;
        PreparedStatement psDeleteReports = null;
        PreparedStatement psDeleteBookmarks = null;
        PreparedStatement psDeleteExtractedText = null;
        PreparedStatement psDeleteDoc = null;

        try {
            conn = DBUtils.getConnection();
            conn.setAutoCommit(false);

            // 1. Dọn dẹp Document Reports
            String sqlReports = "DELETE FROM document_reports WHERE document_id = ?";
            psDeleteReports = conn.prepareStatement(sqlReports);
            psDeleteReports.setInt(1, documentId);
            psDeleteReports.executeUpdate();

            // 2. Dọn dẹp Bookmarks
            String sqlBookmarks = "DELETE FROM bookmarks WHERE document_id = ?";
            psDeleteBookmarks = conn.prepareStatement(sqlBookmarks);
            psDeleteBookmarks.setInt(1, documentId);
            psDeleteBookmarks.executeUpdate();

            // 3. Dọn dẹp Text đã trích xuất
            String sqlExtract = "DELETE FROM document_extracted_text WHERE document_id = ?";
            psDeleteExtractedText = conn.prepareStatement(sqlExtract);
            psDeleteExtractedText.setInt(1, documentId);
            psDeleteExtractedText.executeUpdate();

            // 4. Xử lí Document
            String sqlDoc = "DELETE FROM documents WHERE document_id = ?";
            psDeleteDoc = conn.prepareStatement(sqlDoc);
            psDeleteDoc.setInt(1, documentId);
            int rowsAffected = psDeleteDoc.executeUpdate();

            conn.commit(); // Chốt giao dịch
            isSuccess = (rowsAffected > 0);

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            System.err.println("[DocumentDAO] deleteDocumentAndDependencies failed: " + e.getMessage());
        } finally {
            try {
                if (psDeleteReports != null) {
                    psDeleteReports.close();
                }
                if (psDeleteBookmarks != null) {
                    psDeleteBookmarks.close();
                }
                if (psDeleteExtractedText != null) {
                    psDeleteExtractedText.close();
                }
                if (psDeleteDoc != null) {
                    psDeleteDoc.close();
                }
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return isSuccess;
    }
}
