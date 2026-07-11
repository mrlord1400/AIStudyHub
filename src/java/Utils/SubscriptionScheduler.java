package Utils;

import Model.Service.SubscriptionRenewalService;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@WebListener
public class SubscriptionScheduler implements ServletContextListener {

    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        SubscriptionRenewalService service = new SubscriptionRenewalService();

        // Chạy mỗi giờ (điều chỉnh chu kỳ tùy nhu cầu thực tế)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                service.processExpiryWarnings();
                service.processExpiredSubscriptions();
            } catch (Exception e) {
                System.err.println("[SubscriptionScheduler] " + e.getMessage());
            }
        }, 1, 60, TimeUnit.MINUTES);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) scheduler.shutdownNow();
    }
}