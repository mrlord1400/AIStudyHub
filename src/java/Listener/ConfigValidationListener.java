package Listener;

import Utils.AppConfig;
import Utils.FileStorage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Checks required configuration once at startup and logs the key NAMES that are present or missing
 * (never the values). A missing key does not stop deployment; instead every use of that setting fails
 * closed through {@link AppConfig#require(String)} ("Missing required config: KEY").
 */
@WebListener
public class ConfigValidationListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(ConfigValidationListener.class.getName());

    private static final String[] ALWAYS_REQUIRED = {
        "DB_URL", "DB_USER", "DB_PASSWORD",
        "SMTP_USER", "SMTP_PASSWORD",
        "REMEMBER_ME_SECRET",
        "STORAGE_DIR"
    };

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        List<String> required = new ArrayList<>(Arrays.asList(ALWAYS_REQUIRED));
        boolean aiEnabled = true;
        try {
            aiEnabled = AppConfig.getBoolean("AI_ENABLED", true);
        } catch (IllegalStateException e) {
            LOG.log(Level.SEVERE, "{0}; treating AI as enabled", e.getMessage());
        }
        if (aiEnabled) {
            required.add("GEMINI_API_KEY");
        }
        String[] keys = required.toArray(new String[0]);

        LOG.log(Level.INFO, "Config file: {0}", AppConfig.configFilePath());
        LOG.log(Level.INFO, "Config keys present: {0}", AppConfig.present(keys));

        for (String key : AppConfig.missing(keys)) {
            LOG.log(Level.SEVERE, "Missing required config: {0}", key);
        }

        // STORAGE_DIR: absolute, writable and outside the web application folder (T2.1)
        if (AppConfig.has(FileStorage.CONFIG_KEY)) {
            for (String problem : FileStorage.validateRoot(sce.getServletContext().getRealPath("/"))) {
                LOG.log(Level.SEVERE, "Storage misconfigured: {0}", problem);
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
