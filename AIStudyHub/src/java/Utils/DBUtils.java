package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utility class for creating JDBC connections to the ai_study_hub database.
 *
 * NOTE: Update DB_URL, DB_USER, and DB_PASSWORD to match your environment.
 */
public class DBUtils {

    // ─── Connection Settings ──────────────────────────────────────────────────
    private static final String DB_URL      = "jdbc:mysql://localhost:3306/ai_study_hub?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER     = "ai_study_hub";      // ← change if needed
    private static final String DB_PASSWORD = "!Huna2k5";          // ← change if needed

    static {
        try {
            // Register MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found. " +
                    "Please add mysql-connector-j to the project libraries.", e);
        }
    }

    /**
     * Returns a new Connection to the database.
     * The caller is responsible for closing the connection after use.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}
