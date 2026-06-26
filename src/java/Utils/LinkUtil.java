package Utils;

public class LinkUtil {

    /**
     * Tạo URL an toàn trỏ đến thư mục.
     * Nếu folderId là null, đường dẫn sẽ trỏ về thư mục gốc (Giao diện chính).
     *
     * @param contextPath Đường dẫn gốc của ứng dụng (request.getContextPath())
     * @param folderId    ID của thư mục (có thể null nếu là thư mục gốc)
     * @return Chuỗi URL hoàn chỉnh
     */
    public static String getFolderUrl(String contextPath, Integer folderId) {
        String safeContext = (contextPath == null) ? "" : contextPath;
        
        if (folderId == null) {
            return safeContext + "/FolderController?action=viewFolder";
        }
        
        return safeContext + "/FolderController?action=viewFolder&folderId=" + folderId;
    }

    /**
     * @param contextPath Đường dẫn gốc của ứng dụng
     * @param docId       ID của tài liệu cần xem
     * @return Chuỗi URL hoàn chỉnh mở tài liệu
     */
    public static String getDocumentUrl(String contextPath, int docId) {
        String safeContext = (contextPath == null) ? "" : contextPath;
        
        return safeContext + "/DocumentController?action=viewPage&docId=" + docId;
    }
}