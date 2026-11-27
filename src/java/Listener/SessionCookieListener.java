package Listener;

import Utils.AppConfig;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.SessionCookieConfig;
import javax.servlet.annotation.WebListener;

/**
 * Cookie phiên: HttpOnly luôn bật, Secure khi APP_HTTPS=true (T3.3). SameSite=Lax đặt trong META-INF/context.xml
 * (CookieProcessor), vì Servlet 3.1 chưa có API cho SameSite.
 */
@WebListener
public class SessionCookieListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(SessionCookieListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        boolean https = false;
        try {
            https = AppConfig.getBoolean("APP_HTTPS", false);
        } catch (IllegalStateException e) {
            LOG.log(Level.SEVERE, "{0}; session cookie will not be marked Secure", e.getMessage());
        }
        configure(sce.getServletContext().getSessionCookieConfig(), https);
        LOG.log(Level.INFO, "session cookie: HttpOnly=true Secure={0}", https);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    /** Tách ra để test được mà không cần servlet container. */
    public static void configure(SessionCookieConfig config, boolean https) {
        config.setHttpOnly(true);
        config.setSecure(https);
    }
}
