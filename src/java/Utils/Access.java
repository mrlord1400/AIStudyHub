package Utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Nguồn duy nhất để biết "ai đang gọi" (Conv §2.2). Controller không tự đọc
 * session.userId / session.role nữa mà dùng các hàm ở đây.
 *
 * Danh tính chỉ lấy từ session, không bao giờ từ tham số request.
 */
public final class Access {

    public static final String ROLE_ADMIN = "ADMIN";

    private Access() {
    }

    /** @return id người dùng đang đăng nhập, hoặc null nếu chưa đăng nhập. */
    public static Integer currentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object id = session.getAttribute("userId");
        return (id instanceof Integer) ? (Integer) id : null;
    }

    /** Ghi danh tính vào session đã tạo; dùng chung cho đăng nhập bằng mật khẩu và bằng cookie ghi nhớ. */
    public static void signIn(HttpSession session, Model.DTO.User user) {
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", user.getRole());
        session.setAttribute("tierId", user.getTierId());
        session.setAttribute("balance", user.getBalance());
    }

    public static boolean isLoggedIn(HttpServletRequest request) {
        return currentUserId(request) != null;
    }

    /**
     * Role trong session được SessionSyncFilter làm mới mỗi 60 giây, nên một
     * admin bị hạ quyền có thể còn hiệu lực tối đa ~60 giây.
     */
    public static boolean isAdmin(HttpServletRequest request) {
        if (currentUserId(request) == null) {
            return false;
        }
        Object role = request.getSession(false).getAttribute("role");
        return role instanceof String && ROLE_ADMIN.equalsIgnoreCase(((String) role).trim());
    }
}
