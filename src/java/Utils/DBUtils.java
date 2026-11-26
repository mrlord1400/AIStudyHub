package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for creating JDBC connections to the ai_study_hub database.
 *
 * Connection settings (DB_URL, DB_USER, DB_PASSWORD) come from {@link AppConfig}.
 */
public class DBUtils {

    private static final Logger LOG = Logger.getLogger(DBUtils.class.getName());

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
        return DriverManager.getConnection(
                AppConfig.require("DB_URL"),
                AppConfig.require("DB_USER"),
                AppConfig.require("DB_PASSWORD"));
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
                LOG.log(Level.WARNING, "Error closing the database connection", e);
            }
        }
    }
}