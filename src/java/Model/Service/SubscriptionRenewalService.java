package Model.Service;

import Model.DAO.SubscriptionDAO;
import Model.DAO.TransactionDAO;
import Model.DAO.UserDAO;
import Model.DTO.Transaction;
import Model.DTO.User;
import Utils.EmailUtil;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class SubscriptionRenewalService {

    private static final int RENEW_PERIOD_DAYS = 30;
    private static final int WARNING_HOURS_BEFORE_EXPIRY = 72;

    private final UserDAO userDAO = new UserDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final SubscriptionDAO subscriptionDAO = new SubscriptionDAO();

    private double getPremiumPrice() {
        double price = subscriptionDAO.getPremiumPrice();
        return price < 0 ? 99000 : price;
    }

    /** Gửi cảnh báo cho user Premium sắp hết hạn trong 72h mà không đủ tiền gia hạn */
    public void processExpiryWarnings() {
        double premiumPrice = getPremiumPrice();
        List<User> soonExpiring = userDAO.getPremiumUsersNeedingWarning(WARNING_HOURS_BEFORE_EXPIRY);

        for (User u : soonExpiring) {
            if (u.getBalance() < premiumPrice) {
                boolean sent = EmailUtil.sendPremiumExpiryWarning(
                        u.getEmail(), u.getUsername(), WARNING_HOURS_BEFORE_EXPIRY);
                if (sent) userDAO.markExpiryNotified(u.getUserId());
            } else {
                // Đủ tiền -> không cần cảnh báo, đánh dấu để không quét lại liên tục
                userDAO.markExpiryNotified(u.getUserId());
            }
        }
    }

    /** Tự động gia hạn hoặc hạ cấp các user Premium đã hết hạn */
    public void processExpiredSubscriptions() {
        double premiumPrice = getPremiumPrice();
        List<User> expiredUsers = userDAO.getExpiredPremiumUsers();

        for (User u : expiredUsers) {
            if (u.getBalance() >= premiumPrice) {
                renew(u, premiumPrice);
            } else {
                downgrade(u);
            }
        }
    }

    private void renew(User u, double premiumPrice) {
        boolean balanceOk = userDAO.updateBalance(u.getUserId(), -Math.abs((int) premiumPrice));
        if (!balanceOk) return;

        Transaction t = new Transaction();
        t.setUserId(u.getUserId());
        t.setAmount(-premiumPrice);
        t.setType("WITHDRAW");
        t.setStatus("SUCCESS");
        transactionDAO.createTransaction(t);

        Timestamp newExpiry = Timestamp.valueOf(LocalDateTime.now().plusDays(RENEW_PERIOD_DAYS));
        userDAO.renewPremium(u.getUserId(), newExpiry);
    }

    private void downgrade(User u) {
        userDAO.downgradeToFree(u.getUserId());
        EmailUtil.sendPremiumDowngraded(u.getEmail(), u.getUsername());
    }
}