package Listener;

import Model.DAO.UserDAO;
import Utils.AppConfig;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class AppStartupListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppStartupListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOG.info("=== Application starting ===");

        // Development convenience only. Off unless DEV_SEED_TEST_USERS=true, so a default
        // deployment never contains well-known accounts.
        if (!AppConfig.getBoolean("DEV_SEED_TEST_USERS", false)) {
            return;
        }

        LOG.warning("DEV_SEED_TEST_USERS=true: creating well-known test accounts "
                + "(user@gmail.com, admin@gmail.com). NEVER enable this outside a local machine.");
        try {
            boolean success = new UserDAO().seedTestUsers(AppConfig.require("DEV_SEED_PASSWORD"));
            LOG.info(success
                    ? "Test users added to the database."
                    : "Test users were not added (they may already exist).");
        } catch (RuntimeException e) {
            // e.g. DEV_SEED_PASSWORD or DB settings missing: report it but don't abort deployment.
            LOG.log(Level.SEVERE, "Seeding test users failed", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOG.info("=== Application shutting down ===");
    }
}
