package Utils;

import java.util.regex.Pattern;

/**
 * Các hàm kiểm tra đầu vào dùng chung (hàm thuần, không phụ thuộc servlet) cho các
 * form quản trị. Mọi giá trị đến từ request đều phải qua đây trước khi ghi DB (F-11).
 */
public final class Validation {

    public static final int MAX_USERNAME = 50;
    public static final int MAX_EMAIL = 100;

    // Đủ chặt để loại giá trị rõ ràng sai, không cố bao quát toàn bộ RFC 5322.
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

    private Validation() {
    }

    /** true nếu value (so khớp chính xác, phân biệt hoa thường) nằm trong danh sách cho phép. */
    public static boolean isOneOf(String value, String... allowed) {
        if (value == null) {
            return false;
        }
        for (String a : allowed) {
            if (a.equals(value)) {
                return true;
            }
        }
        return false;
    }

    /** Tên hiển thị: không rỗng sau khi trim và không quá MAX_USERNAME ký tự. */
    public static boolean isValidUsername(String username) {
        return username != null && !username.trim().isEmpty() && username.trim().length() <= MAX_USERNAME;
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.trim().length() <= MAX_EMAIL && EMAIL.matcher(email.trim()).matches();
    }

    /** @return số nguyên trong [min, max], hoặc null nếu thiếu / không phải số / ngoài khoảng. */
    public static Integer parseIntInRange(String value, int min, int max) {
        if (value == null) {
            return null;
        }
        try {
            long n = Long.parseLong(value.trim());
            return (n >= min && n <= max) ? Integer.valueOf((int) n) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** @return số thực hữu hạn trong [min, max] (chấp nhận dấu phẩy thập phân), hoặc null. */
    public static Double parseDoubleInRange(String value, double min, double max) {
        if (value == null) {
            return null;
        }
        try {
            double d = Double.parseDouble(value.trim().replace(",", "."));
            if (Double.isNaN(d) || Double.isInfinite(d) || d < min || d > max) {
                return null;
            }
            return d;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
