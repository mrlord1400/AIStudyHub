package Utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Ba danh sách đường dẫn mà Filter.AuthFilter dùng (Conv §2.1). Đây là hàm thuần
 * (không đụng servlet API) nên có thể test độc lập.
 *
 * Quy tắc: mọi đường dẫn KHÔNG nằm trong danh sách PUBLIC hoặc ADMIN đều là
 * AUTHENTICATED. Mặc định không bao giờ là public.
 */
public final class AccessRules {

    public enum Level {
        PUBLIC, AUTHENTICATED, ADMIN
    }

    /** Trang/controller ai cũng vào được (so khớp chính xác, phân biệt hoa thường). */
    private static final Set<String> PUBLIC_PATHS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "", "/", "/index.jsp",
            "/login.jsp", "/register.jsp", "/forgot_password.jsp", "/reset_password.jsp",
            "/guest_dashboard.jsp", "/publicDocument_view.jsp",
            // Helper JS chống XSS (web/js/security.js) được các trang public nạp; không chứa dữ liệu gì.
            "/js/security.js", "/js/csrf.js",
            "/AuthController", "/ForgotPasswordController",
            // MainController chỉ là router; đích forward của nó được kiểm tra lại ở dispatcher FORWARD.
            "/MainController"
    )));

    /** Controller công khai nhưng chỉ với một số action (khách xem tài liệu public). */
    private static final String DOCUMENT_CONTROLLER = "/DocumentController";
    private static final Set<String> GUEST_DOCUMENT_ACTIONS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "viewPublicPage", "viewDoc"
    )));

    /** Segment đầu tiên (đã lowercase) của các controller chỉ dành cho admin. */
    private static final Set<String> ADMIN_CONTROLLERS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "/admincontroller", "/adminreportcontroller", "/reportconfigcontroller"
    )));

    /** TransactionController có cả action của user lẫn admin; action admin đều bắt đầu bằng "admin". */
    private static final String TRANSACTION_CONTROLLER = "/transactioncontroller";

    /** Endpoint trả JSON: khi bị từ chối phải trả 401/403 thay vì redirect (Conv §2.1). */
    private static final Set<String> JSON_SESSION_ACTIONS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "updateSessionName", "deleteSession", "findSessionByName", "pinSession", "deleteMessagesFrom"
    )));

    private AccessRules() {
    }

    /**
     * Thư mục /uploads trong webapp là nơi lưu file kiểu cũ (F-05). File giờ nằm ngoài webapp và chỉ được
     * phục vụ qua DocumentController, nên mọi URL dưới /uploads đều trả 404 cho tất cả mọi người, kể cả file còn sót
     * lại chưa migrate. So khớp không phân biệt hoa thường (Windows).
     */
    public static boolean isRetiredUploadsPath(String path) {
        if (path == null) {
            return false;
        }
        String p = path.toLowerCase(Locale.ROOT);
        return p.equals("/uploads") || p.startsWith("/uploads/");
    }

    /**
     * @param path   servletPath + pathInfo của request (không gồm context path)
     * @param action giá trị tham số "action" (có thể null)
     */
    public static Level classify(String path, String action) {
        if (path == null) {
            return Level.AUTHENTICATED;
        }
        if (isAdminPath(path, action)) {
            return Level.ADMIN;
        }
        if (PUBLIC_PATHS.contains(path)) {
            return Level.PUBLIC;
        }
        if (DOCUMENT_CONTROLLER.equals(path) && action != null && GUEST_DOCUMENT_ACTIONS.contains(action)) {
            return Level.PUBLIC;
        }
        return Level.AUTHENTICATED;
    }

    /**
     * Kiểm tra admin so khớp KHÔNG phân biệt hoa thường: Windows có hệ file không
     * phân biệt hoa thường, nên nhầm sang "admin" luôn là phía an toàn.
     */
    private static boolean isAdminPath(String path, String action) {
        String p = path.toLowerCase(Locale.ROOT);
        if (p.equals("/admin") || p.startsWith("/admin/")) {
            return true;
        }
        int slash = p.indexOf('/', 1);
        String first = (slash < 0) ? p : p.substring(0, slash);
        if (ADMIN_CONTROLLERS.contains(first)) {
            return true;
        }
        if (first.startsWith("/admin_") && first.endsWith(".jsp")) {
            return true;
        }
        return TRANSACTION_CONTROLLER.equals(first)
                && action != null && action.trim().toLowerCase(Locale.ROOT).startsWith("admin");
    }

    /** Endpoint AJAX trả JSON (dùng để chọn kiểu phản hồi khi từ chối). */
    public static boolean isJsonEndpoint(String path, String action) {
        if (path == null || action == null) {
            return false;
        }
        if ("/SessionController".equals(path)) {
            return JSON_SESSION_ACTIONS.contains(action);
        }
        return DOCUMENT_CONTROLLER.equals(path) && "toggleBookmark".equals(action);
    }

    /** ChatBotController trả text/plain; JS hiển thị nguyên văn nội dung lỗi. */
    public static boolean isPlainTextEndpoint(String path) {
        return "/ChatBotController".equals(path);
    }
}
