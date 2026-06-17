package Listener;

import Model.DAO.UserDAO;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class AppStartupListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("=== Application Starting: Seeding Database ===");
        
        UserDAO userDAO = new UserDAO();
        boolean success = userDAO.seedTestUsers();
        
        if (success) {
            System.out.println("Test users successfully added to the database!");
        } else {
            System.out.println("Failed to add test users (they might already exist).");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // This runs when the server stops. You can leave it empty.
        System.out.println("=== Application Shutting Down ===");
    }
}