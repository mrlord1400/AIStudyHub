package Utils;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;

/**
 * Các bộ giới hạn cho đăng nhập và quên mật khẩu (T3.5). Mọi con số lấy từ config (AppConfig) với mặc định an toàn:
 *
 * <pre>
 * LOGIN_MAX_FAILURES_PER_ACCOUNT_IP   5   lần đăng nhập sai / cặp (IP, email) / cửa sổ
 * LOGIN_MAX_FAILURES_PER_IP          20   lần đăng nhập sai / IP / cửa sổ
 * LOGIN_WINDOW_SECONDS               60
 * OTP_SEND_COOLDOWN_SECONDS          60   tối thiểu giữa hai lần gửi OTP cho cùng (IP, email)
 * OTP_SEND_MAX_PER_ACCOUNT_IP_HOUR    5   lần gửi OTP / cặp (IP, email) / giờ
 * OTP_SEND_MAX_PER_EMAIL_HOUR        10   lần gửi OTP / email / giờ (mọi IP cộng lại, chống dội mail)
 * OTP_SEND_MAX_PER_IP_HOUR           20   lần gửi OTP / IP / giờ
 * OTP_MAX_ATTEMPTS                    3   lần nhập sai cho MỖI mã OTP đã phát hành
 * OTP_VALID_SECONDS                 120
 * </pre>
 *
 * Giới hạn theo cặp (IP, email) chứ không chỉ theo email để một người lạ không thể khóa việc đặt lại mật khẩu của người khác
 * chỉ bằng cách gọi liên tục; giới hạn theo email chỉ là trần cộng gộp.
 * IP là getRemoteAddr(): sau reverse proxy cần cấu hình proxy ghi lại địa chỉ thật (không tin X-Forwarded-For từ client).
 */
public final class AuthLimits {

    private static final Logger LOG = Logger.getLogger(AuthLimits.class.getName());
    private static final int MAX_KEYS = 20000;
    private static final long HOUR_MS = 60L * 60 * 1000;

    private static RateLimiter loginPair;
    private static RateLimiter loginIp;
    private static RateLimiter otpCooldown;
    private static RateLimiter otpPairHour;
    private static RateLimiter otpEmailHour;
    private static RateLimiter otpIpHour;

    private AuthLimits() {
    }

    public static synchronized RateLimiter loginPair() {
        if (loginPair == null) {
            loginPair = new RateLimiter(intSetting("LOGIN_MAX_FAILURES_PER_ACCOUNT_IP", 5, 1, 1000), loginWindowMs(), MAX_KEYS);
        }
        return loginPair;
    }

    public static synchronized RateLimiter loginIp() {
        if (loginIp == null) {
            loginIp = new RateLimiter(intSetting("LOGIN_MAX_FAILURES_PER_IP", 20, 1, 10000), loginWindowMs(), MAX_KEYS);
        }
        return loginIp;
    }

    public static synchronized RateLimiter otpCooldown() {
        if (otpCooldown == null) {
            otpCooldown = new RateLimiter(1, intSetting("OTP_SEND_COOLDOWN_SECONDS", 60, 1, 3600) * 1000L, MAX_KEYS);
        }
        return otpCooldown;
    }

    public static synchronized RateLimiter otpPairHour() {
        if (otpPairHour == null) {
            otpPairHour = new RateLimiter(intSetting("OTP_SEND_MAX_PER_ACCOUNT_IP_HOUR", 5, 1, 1000), HOUR_MS, MAX_KEYS);
        }
        return otpPairHour;
    }

    public static synchronized RateLimiter otpEmailHour() {
        if (otpEmailHour == null) {
            otpEmailHour = new RateLimiter(intSetting("OTP_SEND_MAX_PER_EMAIL_HOUR", 10, 1, 10000), HOUR_MS, MAX_KEYS);
        }
        return otpEmailHour;
    }

    public static synchronized RateLimiter otpIpHour() {
        if (otpIpHour == null) {
            otpIpHour = new RateLimiter(intSetting("OTP_SEND_MAX_PER_IP_HOUR", 20, 1, 100000), HOUR_MS, MAX_KEYS);
        }
        return otpIpHour;
    }

    public static int otpMaxAttempts() {
        return intSetting("OTP_MAX_ATTEMPTS", 3, 1, 10);
    }

    public static long otpValidMs() {
        return intSetting("OTP_VALID_SECONDS", 120, 30, 3600) * 1000L;
    }

    /** Xóa mọi trạng thái (dùng trong test; limiter được tạo lại theo config hiện tại). */
    public static synchronized void reset() {
        loginPair = null;
        loginIp = null;
        otpCooldown = null;
        otpPairHour = null;
        otpEmailHour = null;
        otpIpHour = null;
    }

    public static String clientIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return ip == null ? "unknown" : ip;
    }

    /** Email chuẩn hóa để làm khóa (chữ thường, bỏ khoảng trắng đầu cuối, cắt 100 ký tự). */
    public static String emailKey(String email) {
        String e = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return e.length() > 100 ? e.substring(0, 100) : e;
    }

    private static long loginWindowMs() {
        return intSetting("LOGIN_WINDOW_SECONDS", 60, 1, 3600) * 1000L;
    }

    private static int intSetting(String key, int def, int min, int max) {
        String raw = AppConfig.get(key, String.valueOf(def));
        try {
            int v = Integer.parseInt(raw.trim());
            if (v >= min && v <= max) {
                return v;
            }
        } catch (NumberFormatException ignored) {
            // rơi xuống mặc định
        }
        LOG.log(Level.WARNING, "{0} must be an integer {1}..{2}; using {3}", new Object[]{key, min, max, def});
        return def;
    }
}
