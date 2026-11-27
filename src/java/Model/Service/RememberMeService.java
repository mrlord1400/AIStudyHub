package Model.Service;

import Model.DAO.RememberTokenDAO;
import Model.DAO.UserDAO;
import Model.DTO.User;
import Utils.AppConfig;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * "Ghi nhớ đăng nhập" có thể thu hồi (T3.4, selector/validator).
 *
 * Cookie {@code remember_token} = {@code selector.validator} (16 và 32 byte ngẫu nhiên, Base64url). DB chỉ giữ
 * SHA-256(validator) nên không dùng lại được từ DB. Mỗi lần dùng, validator được đổi (rotate) và hạn được gia hạn.
 * Hạn mặc định 30 ngày, đổi bằng REMEMBER_ME_DAYS (1..365). Xóa khi đăng xuất, đổi/đặt lại mật khẩu, khóa/cấm.
 */
public final class RememberMeService {

    public static final String COOKIE_NAME = "remember_token";

    private static final Logger LOG = Logger.getLogger(RememberMeService.class.getName());
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DEFAULT_DAYS = 30;
    private static final long RACE_GRACE_MS = 60_000L;

    private RememberMeService() {
    }

    /** Số ngày hiệu lực của một token. Giá trị sai trong config thì dùng mặc định và ghi log. */
    public static int days() {
        String raw = AppConfig.get("REMEMBER_ME_DAYS", String.valueOf(DEFAULT_DAYS));
        try {
            int d = Integer.parseInt(raw.trim());
            if (d >= 1 && d <= 365) {
                return d;
            }
        } catch (NumberFormatException ignored) {
            // rơi xuống mặc định
        }
        LOG.log(Level.WARNING, "REMEMBER_ME_DAYS must be a number 1..365; using {0}", DEFAULT_DAYS);
        return DEFAULT_DAYS;
    }

    /** Tạo token mới cho user và đặt cookie. */
    public static void issue(HttpServletRequest request, HttpServletResponse response, int userId) {
        RememberTokenDAO dao = new RememberTokenDAO();
        dao.deleteExpired();
        String selector = randomPart(16);
        String validator = randomPart(32);
        Timestamp expires = expiry();
        if (dao.insert(userId, selector, sha256(validator), expires)) {
            addCookie(request, response, selector + "." + validator);
        }
    }

    /**
     * Đăng nhập tự động từ cookie. Đổi validator và gửi cookie mới.
     *
     * @return user nếu token hợp lệ, còn hạn và tài khoản không bị BANNED; ngược lại null
     */
    public static User authenticate(HttpServletRequest request, HttpServletResponse response) {
        String value = cookieValue(request);
        if (value == null) {
            return null;
        }
        String[] parts = value.split("\\.", -1);
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty() || parts[0].length() > 32 || parts[1].length() > 64) {
            clearCookie(request, response);
            return null;
        }
        String selector = parts[0];
        RememberTokenDAO dao = new RememberTokenDAO();
        RememberTokenDAO.Row row = dao.find(selector);
        if (row == null) {
            clearCookie(request, response);
            return null;
        }
        boolean match = MessageDigest.isEqual(row.validatorHash.getBytes(StandardCharsets.UTF_8), sha256(parts[1]).getBytes(StandardCharsets.UTF_8));
        if (!match) {
            // Vừa được đổi validator cách đây ít giây: nhiều khả năng là hai request (hai tab) cùng mang cookie cũ.
            // Từ chối request này nhưng KHÔNG xóa token và KHÔNG xóa cookie (response của request kia vừa gửi cookie mới).
            if (row.lastUsedAt != null && System.currentTimeMillis() - row.lastUsedAt.getTime() < RACE_GRACE_MS) {
                LOG.log(Level.INFO, "remember token: stale cookie within grace period userId={0}", row.userId);
                return null;
            }
            // Có selector nhưng sai validator: cookie bị đoán hoặc đã bị dùng ở nơi khác. Hủy token này.
            LOG.log(Level.WARNING, "remember token rejected: validator mismatch userId={0}", row.userId);
            dao.deleteBySelector(selector);
            clearCookie(request, response);
            return null;
        }
        if (row.expiresAt.getTime() < System.currentTimeMillis()) {
            dao.deleteBySelector(selector);
            clearCookie(request, response);
            return null;
        }
        User user = new UserDAO().getUserById(row.userId);
        if (user == null || "BANNED".equalsIgnoreCase(user.getStatus())) {
            dao.deleteBySelector(selector);
            clearCookie(request, response);
            return null;
        }
        String newValidator = randomPart(32);
        if (!dao.rotate(selector, row.validatorHash, sha256(newValidator), expiry())) {
            // Request song song đã dùng token này trước: không đăng nhập ở request này, token mới của request kia vẫn sống.
            return null;
        }
        addCookie(request, response, selector + "." + newValidator);
        return user;
    }

    /** Đăng xuất: xóa dòng trong DB và cookie. */
    public static void forget(HttpServletRequest request, HttpServletResponse response) {
        String value = cookieValue(request);
        if (value != null) {
            int dot = value.indexOf('.');
            String selector = dot < 0 ? value : value.substring(0, dot);
            if (!selector.isEmpty() && selector.length() <= 32) {
                new RememberTokenDAO().deleteBySelector(selector);
            }
        }
        clearCookie(request, response);
    }

    // ---- nội bộ ----
    private static Timestamp expiry() {
        return new Timestamp(System.currentTimeMillis() + days() * 24L * 60 * 60 * 1000);
    }

    private static String cookieValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (COOKIE_NAME.equals(c.getName()) && c.getValue() != null && !c.getValue().isEmpty()) {
                return c.getValue();
            }
        }
        return null;
    }

    private static void addCookie(HttpServletRequest request, HttpServletResponse response, String value) {
        Cookie c = new Cookie(COOKIE_NAME, value);
        c.setMaxAge(days() * 24 * 60 * 60);
        c.setPath(cookiePath(request));
        c.setHttpOnly(true);
        c.setSecure(isHttps());
        response.addCookie(c);
    }

    private static void clearCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie c = new Cookie(COOKIE_NAME, "");
        c.setMaxAge(0);
        c.setPath(cookiePath(request));
        c.setHttpOnly(true);
        c.setSecure(isHttps());
        response.addCookie(c);
    }

    private static String cookiePath(HttpServletRequest request) {
        String ctx = request.getContextPath();
        return (ctx == null || ctx.isEmpty()) ? "/" : ctx;
    }

    private static boolean isHttps() {
        try {
            return AppConfig.getBoolean("APP_HTTPS", false);
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private static String randomPart(int bytes) {
        byte[] b = new byte[bytes];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte x : digest) {
                sb.append(String.format("%02x", x));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
