import Controller.AdminController;
import Controller.ReportConfigController;
import Controller.SystemConfigController;
import Controller.TransactionController;
import Model.DAO.SubscriptionDAO;
import Model.DTO.Subscription;
import Utils.Validation;
import java.util.ArrayList;
import java.util.List;


public class T17Test {
    static String s(Http.Result r) { return r.summary(); }

    public static void main(String[] a) throws Exception {
        Db.init();
        int admin = Db.user("root", "ADMIN");
        int victim = Db.user("victim", "STUDENT");
        AdminController ac = new AdminController();

        // ---- pure validators ----
        Db.check("parseIntInRange accepts in-range", Validation.parseIntInRange("5", 0, 10) == 5);
        Db.check("parseIntInRange rejects out-of-range / junk / null / overflow",
                Validation.parseIntInRange("11", 0, 10) == null && Validation.parseIntInRange("abc", 0, 10) == null
                && Validation.parseIntInRange(null, 0, 10) == null && Validation.parseIntInRange("99999999999999", 0, Integer.MAX_VALUE) == null
                && Validation.parseIntInRange("-1", 0, 10) == null && Validation.parseIntInRange("1.5", 0, 10) == null);
        Db.check("parseDoubleInRange rejects NaN/Infinity/out-of-range",
                Validation.parseDoubleInRange("NaN", 0, 10) == null && Validation.parseDoubleInRange("Infinity", 0, 10) == null
                && Validation.parseDoubleInRange("1e9", 0, 10) == null && Validation.parseDoubleInRange("2,5", 0, 10) == 2.5);
        Db.check("email validator", Validation.isValidEmail("a@b.co") && !Validation.isValidEmail("a@b") && !Validation.isValidEmail("<x>@b.co") && !Validation.isValidEmail(null));

        // ---- AdminController.createUser ----
        Http.Result r;
        r = Http.call(ac, "POST", Http.params("action", "createUser", "username", "newguy", "email", "n@x.com", "password", "Str0ng!pass", "role", "ADMIN"), admin, "ADMIN");
        Db.check("valid createUser (ADMIN) -> redirect to list, row created with ADMIN role", s(r).equals("REDIRECT /AIStudyHub/AdminController?action=listUsers")
                && Db.count("SELECT COUNT(*) FROM users WHERE email='n@x.com' AND role='ADMIN'") == 1);
        r = Http.call(ac, "POST", Http.params("action", "createUser", "username", "stu", "email", "s@x.com", "password", "Str0ng!pass", "role", "STUDENT"), admin, "ADMIN");
        Db.check("valid createUser (STUDENT, the form's value) works", Db.count("SELECT COUNT(*) FROM users WHERE email='s@x.com' AND role='STUDENT' AND tier_id=2") == 1);
        long before = Db.count("SELECT COUNT(*) FROM users");
        String[][] bad = {
            {"role", "SUPERUSER"}, {"role", ""}, {"password", "weak"}, {"email", "not-an-email"}, {"username", "   "},
        };
        for (String[] b : bad) {
            List<String> kv = new ArrayList<>(java.util.Arrays.asList("action", "createUser", "username", "u2", "email", "u2@x.com", "password", "Str0ng!pass", "role", "STUDENT"));
            for (int i = 0; i < kv.size(); i += 2) if (kv.get(i).equals(b[0])) kv.set(i + 1, b[1]);
            r = Http.call(ac, "POST", Http.params(kv.toArray(new String[0])), admin, "ADMIN");
            Db.check("createUser rejected for bad " + b[0] + "=" + b[1], s(r).contains("error=invalid_input"));
        }
        Db.check("no rows were created by invalid createUser calls", Db.count("SELECT COUNT(*) FROM users") == before);

        // ---- AdminController.updateUser ----
        r = Http.call(ac, "POST", Http.params("action", "updateUser", "user_id", "" + victim, "newUsername", "victim2", "newEmail", "v2@x.com", "newRole", "STUDENT", "newStatus", "BANNED", "newBalance", "500", "newTierId", "3"), admin, "ADMIN");
        Db.check("valid updateUser applies", Db.count("SELECT COUNT(*) FROM users WHERE user_id=? AND username='victim2' AND status='BANNED' AND balance=500 AND tier_id=3", victim) == 1);
        String[][] badUpd = {
            {"newRole", "ROOT"}, {"newStatus", "DELETED"}, {"newBalance", "-5"}, {"newBalance", "99999999999"}, {"newBalance", "1.5"},
            {"newTierId", "99"}, {"newTierId", "abc"}, {"newEmail", "bad"}, {"newUsername", ""}, {"user_id", "x"},
        };
        for (String[] b : badUpd) {
            List<String> kv = new ArrayList<>(java.util.Arrays.asList("action", "updateUser", "user_id", "" + victim, "newUsername", "zzz", "newEmail", "z@x.com", "newRole", "ADMIN", "newStatus", "ACTIVE", "newBalance", "1", "newTierId", "2"));
            for (int i = 0; i < kv.size(); i += 2) if (kv.get(i).equals(b[0])) kv.set(i + 1, b[1]);
            r = Http.call(ac, "POST", Http.params(kv.toArray(new String[0])), admin, "ADMIN");
            Db.check("updateUser rejected for bad " + b[0] + "=" + b[1], s(r).contains("error=invalid_input"));
        }
        Db.check("victim row unchanged by the invalid updates", Db.count("SELECT COUNT(*) FROM users WHERE user_id=? AND username='victim2' AND role='STUDENT' AND status='BANNED' AND balance=500", victim) == 1);

        // self-protection
        r = Http.call(ac, "POST", Http.params("action", "updateUser", "user_id", "" + admin, "newUsername", "root", "newEmail", "root@x.com", "newRole", "STUDENT", "newStatus", "ACTIVE", "newBalance", "0", "newTierId", "2"), admin, "ADMIN");
        Db.check("admin cannot demote self", s(r).contains("error=self_modify") && Db.count("SELECT COUNT(*) FROM users WHERE user_id=? AND role='ADMIN'", admin) == 1);
        r = Http.call(ac, "POST", Http.params("action", "updateUser", "user_id", "" + admin, "newUsername", "root", "newEmail", "root@x.com", "newRole", "ADMIN", "newStatus", "BANNED", "newBalance", "0", "newTierId", "2"), admin, "ADMIN");
        Db.check("admin cannot ban self", s(r).contains("error=self_modify") && Db.count("SELECT COUNT(*) FROM users WHERE user_id=? AND status='ACTIVE'", admin) == 1);
        r = Http.call(ac, "POST", Http.params("action", "deleteUser", "user_id", "" + admin), admin, "ADMIN");
        Db.check("admin cannot delete self", s(r).contains("error=self_modify") && Db.count("SELECT COUNT(*) FROM users WHERE user_id=?", admin) == 1);
        r = Http.call(ac, "POST", Http.params("action", "deleteUser", "user_id", "abc"), admin, "ADMIN");
        Db.check("deleteUser with junk id -> invalid_input", s(r).contains("error=invalid_input"));
        r = Http.call(ac, "POST", Http.params("action", "createUser", "username", "x", "email", "x@x.com", "password", "Str0ng!pass", "role", "ADMIN"), null, null);
        Db.check("anonymous POST to AdminController -> 403 from the defence-in-depth check", s(r).equals("ERROR 403"));
        r = Http.call(ac, "POST", Http.params("action", "createUser", "username", "x", "email", "x@x.com", "password", "Str0ng!pass", "role", "ADMIN"), victim, "STUDENT");
        Db.check("student POST to AdminController -> 403 and no row", s(r).equals("ERROR 403") && Db.count("SELECT COUNT(*) FROM users WHERE email='x@x.com'") == 0);

        // ---- TransactionController.adminUpdateTransaction ----
        Db.exec("INSERT INTO transactions (user_id, amount, type, status) VALUES (?, 100, 'DEPOSIT', 'PENDING')", victim);
        int tx = (int) Db.count("SELECT MAX(transaction_id) FROM transactions");
        TransactionController tc = new TransactionController();
        r = Http.call(tc, "POST", Http.params("action", "adminUpdateTransaction", "transactionId", "" + tx, "newStatus", "HACKED"), admin, "ADMIN");
        Db.check("transaction status 'HACKED' rejected, row unchanged", s(r).contains("error=invalid_input") && Db.count("SELECT COUNT(*) FROM transactions WHERE transaction_id=? AND status='PENDING'", tx) == 1);
        r = Http.call(tc, "POST", Http.params("action", "adminUpdateTransaction", "transactionId", "1; DROP", "newStatus", "SUCCESS"), admin, "ADMIN");
        Db.check("junk transactionId rejected", s(r).contains("error=invalid_input"));
        r = Http.call(tc, "POST", Http.params("action", "adminUpdateTransaction", "transactionId", "" + tx, "newStatus", "PENDING"), admin, "ADMIN");
        Db.check("cannot set a transaction back to PENDING", s(r).contains("error=invalid_input"));
        r = Http.call(tc, "POST", Http.params("action", "adminUpdateTransaction", "transactionId", "" + tx, "newStatus", "SUCCESS"), victim, "STUDENT");
        Db.check("student cannot approve a transaction", s(r).equals("ERROR 403") && Db.count("SELECT COUNT(*) FROM transactions WHERE status='PENDING'") == 1);
        r = Http.call(tc, "POST", Http.params("action", "adminUpdateTransaction", "transactionId", "" + tx, "newStatus", "SUCCESS"), admin, "ADMIN");
        Db.check("admin can approve", s(r).contains("updateSuccess=1") && Db.count("SELECT COUNT(*) FROM transactions WHERE transaction_id=? AND status='SUCCESS'", tx) == 1);

        // ---- ReportConfigController ----
        ReportConfigController rc = new ReportConfigController();
        r = Http.call(rc, "GET", Http.params("action", "reportConfigDelete", "reasonCode", "SPAM"), admin, "ADMIN");
        Db.check("GET reportConfigDelete -> 405 and row kept", s(r).equals("ERROR 405") && Db.count("SELECT COUNT(*) FROM report_reason_configs WHERE reason_code='SPAM'") == 1);
        r = Http.call(rc, "GET", Http.params("action", "reportConfigDelete", "reasonCode", "SPAM"), victim, "STUDENT");
        Db.check("student GET reportConfigDelete -> 403 (not 405, not deleted)", s(r).equals("ERROR 403"));
        r = Http.call(rc, "POST", Http.params("action", "reportConfigAdd", "reasonCode", "BAD CODE<script>", "severityLevel", "LOW", "baseScore", "1", "autoFlagThreshold", "5"), admin, "ADMIN");
        Db.check("bad reasonCode rejected", s(r).contains("error=invalid_input"));
        r = Http.call(rc, "POST", Http.params("action", "reportConfigAdd", "reasonCode", "PHISHING", "severityLevel", "EXTREME", "baseScore", "1", "autoFlagThreshold", "5"), admin, "ADMIN");
        Db.check("bad severity rejected", s(r).contains("error=invalid_input"));
        r = Http.call(rc, "POST", Http.params("action", "reportConfigAdd", "reasonCode", "PHISHING", "severityLevel", "HIGH", "baseScore", "abc", "autoFlagThreshold", "5"), admin, "ADMIN");
        Db.check("non-numeric score rejected (no silent 0)", s(r).contains("error=invalid_input"));
        r = Http.call(rc, "POST", Http.params("action", "reportConfigAdd", "reasonCode", "PHISHING", "severityLevel", "HIGH", "baseScore", "5000", "autoFlagThreshold", "5"), admin, "ADMIN");
        Db.check("score above DECIMAL(5,2) rejected", s(r).contains("error=invalid_input"));
        Db.check("nothing was added by invalid calls", Db.count("SELECT COUNT(*) FROM report_reason_configs") == 2);
        r = Http.call(rc, "POST", Http.params("action", "reportConfigAdd", "reasonCode", "PHISHING", "severityLevel", "HIGH", "baseScore", "2,5", "autoFlagThreshold", "6"), admin, "ADMIN");
        Db.check("valid add works (comma decimal accepted)", Db.count("SELECT COUNT(*) FROM report_reason_configs WHERE reason_code='PHISHING' AND base_score=2.5") == 1);
        r = Http.call(rc, "POST", Http.params("action", "reportConfigDelete", "reasonCode", "PHISHING"), admin, "ADMIN");
        Db.check("POST delete works", Db.count("SELECT COUNT(*) FROM report_reason_configs WHERE reason_code='PHISHING'") == 0);

        // ---- SystemConfigController: all-or-nothing ----
        SystemConfigController sc = new SystemConfigController();
        List<Subscription> before0 = new SubscriptionDAO().getAllSubscriptions();
        r = Http.call(sc, "POST", Http.params("tierId", "2", "maxStorage_2", "60", "aiLimit_2", "20", "price_2", "0", "totalStorage_2", "6000",
                "tierId", "3", "maxStorage_3", "abc", "aiLimit_3", "200", "price_3", "99000", "totalStorage_3", "60000"), admin, "ADMIN");
        Db.check("one bad row -> error message, forwards back", String.valueOf(r.requestAttrs.get("errorMessage")).contains("không hợp lệ"));
        Db.check("...and NOTHING was written (row 2 unchanged although it was valid)", Db.count("SELECT COUNT(*) FROM subscriptions WHERE tier_id=2 AND max_storage_mb=50 AND ai_prompt_limit_per_day=10") == 1);
        r = Http.call(sc, "POST", Http.params("tierId", "2", "maxStorage_2", "60", "aiLimit_2", "20", "price_2", "-5", "totalStorage_2", "6000"), admin, "ADMIN");
        Db.check("negative price rejected", r.requestAttrs.get("errorMessage") != null && r.requestAttrs.get("successMessage") == null);
        r = Http.call(sc, "POST", Http.params("tierId", "42", "maxStorage_42", "60", "aiLimit_42", "20", "price_42", "5", "totalStorage_42", "6000"), admin, "ADMIN");
        Db.check("unknown tier id rejected", r.requestAttrs.get("errorMessage") != null);
        r = Http.call(sc, "POST", Http.params("tierId", "2", "maxStorage_2", "60", "aiLimit_2", "20", "price_2", "0", "totalStorage_2", "6000",
                "tierId", "3", "maxStorage_3", "120", "aiLimit_3", "200", "price_3", "99000", "totalStorage_3", "60000"), admin, "ADMIN");
        Db.check("all-valid submission is written", r.requestAttrs.get("successMessage") != null
                && Db.count("SELECT COUNT(*) FROM subscriptions WHERE tier_id=2 AND max_storage_mb=60 AND total_storage_mb=6000") == 1
                && Db.count("SELECT COUNT(*) FROM subscriptions WHERE tier_id=3 AND ai_prompt_limit_per_day=200") == 1);
        r = Http.call(sc, "POST", Http.params("tierId", "2", "maxStorage_2", "60", "aiLimit_2", "20", "price_2", "0", "totalStorage_2", "6000"), victim, "STUDENT");
        Db.check("student cannot POST system config", s(r).equals("ERROR 403"));
        Db.done();
    }
}
