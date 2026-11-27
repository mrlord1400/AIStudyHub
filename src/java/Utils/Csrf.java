package Utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.servlet.http.HttpSession;

/**
 * CSRF bằng synchronizer token (Conv §4). Token nằm trong session ({@code csrfToken}), trang in ra
 * {@code <meta name="csrf-token">} (WEB-INF/jspf/csrf_head.jspf), js/csrf.js gắn nó vào mọi form POST và mọi
 * fetch không phải GET. Filter.CsrfFilter từ chối POST không có token đúng.
 *
 * Hàm thuần (chỉ phụ thuộc HttpSession) nên test được.
 */
public final class Csrf {

    public static final String SESSION_ATTR = "csrfToken";
    public static final String PARAM = "_csrf";
    public static final String HEADER = "X-CSRF-Token";

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Mọi action làm thay đổi dữ liệu hoặc trạng thái. Request GET tới các action này trả 405 (Conv §4: GET không bao giờ
     * đổi trạng thái). Danh sách nằm một chỗ; thêm action ghi mới thì thêm vào đây. So khớp không phân biệt hoa thường.
     */
    private static final Set<String> MUTATING_ACTIONS = lower(
            // phiên / tài khoản
            "login", "register", "logout", "processResetPassword", "sendOTP", "verifyOTP",
            "updateProfile", "deleteAccount", "update", "delete",
            // tài liệu / thư mục / upload
            "deleteDoc", "updateDoc", "updatePermission", "toggleBookmark",
            "createFolder", "deleteFolder", "updateFolder",
            "upload", "confirm", "cancel", "replace", "keepBoth",
            // chat
            "createSession", "updateSessionName", "deleteSession", "pinSession", "deleteMessagesFrom", "chatbotPrompt",
            // ví / giao dịch
            "createTransaction", "buyPremium", "adminUpdateTransaction",
            // báo cáo
            "createReport", "updateReport", "adminUpdateReportStatus", "adminDeleteReport", "adminDeleteDocument",
            "reportConfigAdd", "reportConfigUpdate", "reportConfigDelete",
            // bạn bè
            "createFriendship", "updateFriendshipStatus", "deleteFriendship",
            // quản trị
            "createUser", "updateUser", "deleteUser", "updateAdminProfile"
    );

    private Csrf() {
    }

    private static Set<String> lower(String... names) {
        Set<String> set = new HashSet<String>();
        for (String n : names) {
            set.add(n.toLowerCase(Locale.ROOT));
        }
        return Collections.unmodifiableSet(set);
    }

    /** true nếu action này ghi dữ liệu nên chỉ được gọi bằng POST. */
    public static boolean isMutatingAction(String action) {
        return action != null && MUTATING_ACTIONS.contains(action.trim().toLowerCase(Locale.ROOT));
    }

    /** Method không đổi trạng thái theo RFC 9110. */
    public static boolean isSafeMethod(String method) {
        return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)
                || "OPTIONS".equalsIgnoreCase(method) || "TRACE".equalsIgnoreCase(method);
    }

    /** Lấy token của session, tạo mới nếu chưa có (32 byte ngẫu nhiên, Base64url). */
    public static String token(HttpSession session) {
        synchronized (Csrf.class) {
            Object existing = session.getAttribute(SESSION_ATTR);
            if (existing instanceof String && !((String) existing).isEmpty()) {
                return (String) existing;
            }
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String created = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(SESSION_ATTR, created);
            return created;
        }
    }

    /** Bỏ token (gọi khi đăng nhập/đăng xuất để token cũ không dùng lại được). */
    public static void rotate(HttpSession session) {
        synchronized (Csrf.class) {
            session.removeAttribute(SESSION_ATTR);
        }
    }

    /** So sánh hằng thời gian token của session với giá trị client gửi. null hoặc rỗng là sai. */
    public static boolean matches(HttpSession session, String supplied) {
        if (session == null || supplied == null || supplied.isEmpty()) {
            return false;
        }
        Object expected = session.getAttribute(SESSION_ATTR);
        if (!(expected instanceof String)) {
            return false;
        }
        return MessageDigest.isEqual(((String) expected).getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }

    /** Dùng cho test: danh sách action (đã lowercase). */
    public static Set<String> mutatingActions() {
        return MUTATING_ACTIONS;
    }
}
