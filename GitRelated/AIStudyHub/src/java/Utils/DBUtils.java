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
    // SQL Server default port is 1433. Added trustServerCertificate to bypass local SSL issues.
    private static final String DB_URL      = "jdbc:sqlserver://localhost:1433;databaseName=ai_study_hub;encrypt=true;trustServerCertificate=true;";
    private static final String DB_USER     = "sa";      // ← change if needed
    private static final String DB_PASSWORD = "12345";   // ← change if needed

    static {
        try {
            // Register Microsoft SQL Server JDBC Driver
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQL Server JDBC Driver not found. " +
                    "Please add mssql-jdbc to the project libraries.", e);
        }
    }

    /**
     * Returns a new Connection to the database.
     * The caller is responsible for closing the connection after use.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Safely closes the database connection.
     * @param conn The Connection object to be closed.
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                // In a production environment, you would typically log this error
                // using a framework like SLF4J or Log4j instead of System.err.
                System.err.println("Error closing the database connection: " + e.getMessage());
            }
        }
    }
}