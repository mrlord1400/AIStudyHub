package Utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * Quy tắc mật khẩu (BR-15, 16, 17, 17b): >= 8 ký tự, có chữ hoa, có chữ số, có ký tự đặc biệt.
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) return false;
        if (!password.matches(".*[A-Z].*")) return false;
        if (!password.matches(".*[0-9].*")) return false;
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) return false;
        return true;
    }

    public static boolean verifyPassword(String password,
                                         String hashedPassword) {

        if(password == null ||
                hashedPassword == null) {
            return false;
        }

        return BCrypt.checkpw(
                password,
                hashedPassword
        );
    }
}
