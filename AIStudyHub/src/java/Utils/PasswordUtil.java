package Utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
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