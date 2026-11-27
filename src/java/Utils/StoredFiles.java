package Utils;

import java.io.File;
import javax.servlet.ServletContext;

/**
 * Xóa file vật lý của tài liệu từ giá trị documents.cloud_storage_url (hiện là đường dẫn dưới web root).
 * Gom vào một chỗ để T2.1 (FileStorage ngoài web root) chỉ phải đổi một nơi cho các đường xóa này.
 */
public final class StoredFiles {

    private StoredFiles() {
    }

    /** Gọi SAU KHI bản ghi DB đã được xóa thành công. Không ném lỗi; trả về true nếu đã xóa được file. */
    public static boolean delete(ServletContext context, String contextPath, String cloudUrl) {
        if (cloudUrl == null || cloudUrl.trim().isEmpty() || context == null) {
            return false;
        }
        String relativePath = cloudUrl;
        if (contextPath != null && relativePath.startsWith(contextPath)) {
            relativePath = relativePath.substring(contextPath.length());
        }
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        String realPath = context.getRealPath("");
        if (realPath == null) {
            return false;
        }
        File physicalFile = new File(realPath + File.separator + relativePath.replace("/", File.separator));
        return physicalFile.exists() && physicalFile.delete();
    }
}
