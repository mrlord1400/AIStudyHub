package Utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Tiện ích tạo và xác thực "remember_token" dùng cho tính năng Ghi nhớ đăng nhập.
 *
 * Vì sao không lưu thẳng email vào Cookie (như code cũ)?
 * - Cookie value = email khiến bất kỳ ai cũng có thể tự tạo Cookie
 *   "remember_token=nannuoi@fpt.edu.vn" và đăng nhập vào tài khoản người khác
 *   mà không cần mật khẩu (giả mạo Cookie).
 * - Giải pháp: Ký (sign) nội dung Cookie bằng HMAC-SHA256 với 1 khóa bí mật
 *   chỉ Server biết. Client không thể tự tạo được chữ ký hợp lệ nếu không có khóa.
 *
 * Cấu trúc token (trước khi mã hoá Base64): email|expiry|signature
 */
public class CookieUtil {

    // TODO: Trong thực tế nên đưa SECRET_KEY vào biến môi trường / file config,
    // KHÔNG hard-code trong source. Để demo đồ án, tạm để ở đây.
    private static final String SECRET_KEY = "AIStudyHub-RememberMe-Secret-SWP391-ChangeThisInProduction";

    // Theo yêu cầu: ghi nhớ đăng nhập "cho đến khi user logout hoặc hệ thống update".
    // Vì cookie là stateless (không lưu DB) nên không thể tự huỷ khi server restart,
    // ta set thời hạn rất dài (10 năm) để mô phỏng hành vi "gần như vĩnh viễn"
    // cho tới khi người dùng chủ động bấm Đăng xuất (lúc đó Cookie sẽ bị xoá).
    private static final long REMEMBER_DURATION_MS = 10L * 365 * 24 * 60 * 60 * 1000; // 10 năm

    public static long getRememberDurationSeconds() {
        return REMEMBER_DURATION_MS / 1000;
    }

    /** Tạo token đã ký cho email cho trước, dùng làm giá trị Cookie remember_token */
    public static String buildRememberToken(String email) {
        long expiry = System.currentTimeMillis() + REMEMBER_DURATION_MS;
        String payload = email + "|" + expiry;
        String signature = hmac(payload);
        String raw = payload + "|" + signature;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Xác thực token lấy từ Cookie.
     * @return email nếu token hợp lệ & còn hạn, null nếu token giả mạo / sai định dạng / hết hạn
     */
    public static String verifyRememberToken(String token) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|");
            if (parts.length != 3) {
                return null;
            }

            String email = parts[0];
            long expiry = Long.parseLong(parts[1]);
            String signature = parts[2];

            String expectedSignature = hmac(email + "|" + expiry);

            // Dùng equals thường ở đây là đủ cho đồ án; production nên dùng
            // MessageDigest.isEqual để tránh timing attack.
            if (!expectedSignature.equals(signature)) {
                return null; // Chữ ký không khớp -> Cookie bị sửa / giả mạo
            }
            if (System.currentTimeMillis() > expiry) {
                return null; // Token hết hạn
            }

            return email;
        } catch (Exception e) {
            return null; // Bất kỳ lỗi parse nào -> coi như token không hợp lệ
        }
    }

    private static String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo chữ ký HMAC", e);
        }
    }
}