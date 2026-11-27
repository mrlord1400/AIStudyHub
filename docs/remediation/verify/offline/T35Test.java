import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * T3.5: rate limiting and OTP hardening.
 * Part 1: Utils.RateLimiter on a fake clock (window, bound on keys, retry-after).
 * Part 2: the real login / forgot-password flow on embedded Tomcat (raw requests, so cookies are explicit).
 */
public class T35Test {

    static final String PASSWORD = "Passw0rd!";
    static final List<String> SENT = new ArrayList<>();

    static void check(String name, boolean ok, String detail) {
        Db.check(name + (ok ? "" : " [" + detail + "]"), ok);
    }

    /** a browser session: the cookie header and the CSRF token of its current page */
    static final class Browser {

        String sid;
        String token;

        Browser() throws Exception {
            refresh("/login.jsp");
        }

        void refresh(String path) throws Exception {
            Web.Raw r = Web.raw("GET", path, null, sid == null ? null : "JSESSIONID=" + sid);
            if (r.cookie("JSESSIONID") != null) {
                sid = r.cookie("JSESSIONID");
            }
            Matcher m = Pattern.compile("<meta name=\"csrf-token\" content=\"([^\"]*)\"").matcher(r.body);
            if (m.find()) {
                token = m.group(1);
            }
        }

        Web.Raw post(String path, String body) throws Exception {
            Web.Raw r = Web.raw("POST", path, body + "&_csrf=" + Web.enc(token), "JSESSIONID=" + sid);
            if (r.cookie("JSESSIONID") != null) {
                sid = r.cookie("JSESSIONID");
            }
            return r;
        }

        Web.Raw login(String email, String password) throws Exception {
            return post("/AuthController", "action=login&email=" + Web.enc(email) + "&password=" + Web.enc(password));
        }

        Web.Raw otp(String action, String email, String otp) throws Exception {
            return post("/ForgotPasswordController", "action=" + action + "&email=" + Web.enc(email) + (otp == null ? "" : "&otp=" + otp));
        }
    }

    static boolean has(Web.Raw r, String fragment) {
        return r.body.contains(fragment);
    }

    static void resetLimits() throws Exception {
        Web.invokeInApp("Utils.AuthLimits", "reset", new Class<?>[0]);
    }

    public static void main(String[] a) throws Exception {
        // ---------- RateLimiter on a fake clock ----------
        final long[] now = {1000};
        Utils.RateLimiter rl = new Utils.RateLimiter(3, 1000, 4, () -> now[0]);
        check("limiter: 3 hits allowed, then blocked", !rl.isBlocked("k") && rl.tryAcquire("k") && rl.tryAcquire("k") && rl.tryAcquire("k") && rl.isBlocked("k") && !rl.tryAcquire("k"), "");
        check("limiter: retry-after counts down", rl.retryAfterMs("k") == 1000, String.valueOf(rl.retryAfterMs("k")));
        now[0] += 400;
        check("limiter: still blocked inside the window", rl.isBlocked("k") && rl.retryAfterMs("k") == 600, String.valueOf(rl.retryAfterMs("k")));
        now[0] += 600;
        check("limiter: opens again after the window", !rl.isBlocked("k") && rl.retryAfterMs("k") == 0 && rl.tryAcquire("k"), "");
        rl.clear("k");
        check("limiter: clear resets a key", rl.size() == 0 && !rl.isBlocked("k"), "");
        for (int i = 0; i < 50; i++) {
            rl.hit("flood-" + i);
        }
        check("limiter: the number of keys is bounded (4)", rl.size() <= 4, String.valueOf(rl.size()));
        now[0] += 5000;
        rl.hit("late");
        check("limiter: expired keys are dropped first when making room", rl.size() == 1, String.valueOf(rl.size()));
        boolean rejected = false;
        try {
            new Utils.RateLimiter(0, 1000, 10);
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        check("limiter: nonsense settings are rejected", rejected, "");

        // ---------- the real flow ----------
        Db.init();
        String hash = Utils.PasswordUtil.hashPassword(PASSWORD);
        int alice = Db.user("alice", "STUDENT");
        int bob = Db.user("bob", "STUDENT");
        Db.exec("UPDATE users SET password_hash = ? WHERE user_id IN (?, ?)", hash, alice, bob);
        System.setProperty("OTP_SEND_COOLDOWN_SECONDS", "2");
        Web.start(System.getProperty("AISH_WEBROOT"), System.getProperty("AISH_CLASSES"));
        try {
            java.util.function.BiPredicate<String, String> capture = (email, otp) -> {
                SENT.add(email + "|" + otp);
                return true;
            };
            Web.invokeInApp("Controller.ForgotPasswordController", "setOtpSenderForTests", new Class<?>[]{java.util.function.BiPredicate.class}, capture);
            // ----- login throttling -----
            Browser b1 = new Browser();
            boolean firstFiveInvalid = true;
            for (int i = 1; i <= 5; i++) {
                Web.Raw r = b1.login("alice@x.com", "wrong" + i);
                firstFiveInvalid &= r.status == 302 && r.location.contains("error=invalid_credentials");
            }
            check("login: the first 5 failures say invalid_credentials", firstFiveInvalid, "");
            Web.Raw sixth = b1.login("alice@x.com", "wrong6");
            check("login: the 6th attempt within the minute is throttled", sixth.status == 302 && sixth.location.contains("error=too_many_attempts"), sixth.location);
            Web.Raw rightButBlocked = b1.login("alice@x.com", PASSWORD);
            check("login: even the right password is refused while throttled", rightButBlocked.location.contains("error=too_many_attempts"), rightButBlocked.location);
            Web.Raw other = b1.login("bob@x.com", PASSWORD);
            check("login: another account from the same IP is not throttled", other.status == 302 && other.location.endsWith("/user_dashboard.jsp"), other.location);
            check("login page explains the throttle", has(Web.raw("GET", "/login.jsp?error=too_many_attempts", null, null), "quá nhiều lần"), "");

            resetLimits();
            Browser b2 = new Browser();
            b2.login("alice@x.com", "wrong1");
            b2.login("alice@x.com", "wrong2");
            Web.Raw ok = b2.login("alice@x.com", PASSWORD);
            check("login: a success clears the counter for that account", ok.location.endsWith("/user_dashboard.jsp"), ok.location);
            Browser b2b = new Browser();
            for (int i = 0; i < 4; i++) {
                b2b.login("alice@x.com", "bad" + i);
            }
            check("login: after the success four more failures are still allowed", !b2b.login("alice@x.com", PASSWORD).location.contains("too_many"), "");

            // ----- the throttle holds under parallel requests (the count happens before the password check) -----
            resetLimits();
            final int[] invalid = {0};
            final int[] throttledCount = {0};
            java.util.List<Thread> threads = new java.util.ArrayList<>();
            for (int i = 0; i < 12; i++) {
                final Browser bp = new Browser();
                Thread t = new Thread(() -> {
                    try {
                        for (int k = 0; k < 4; k++) {
                            Web.Raw r = bp.login("alice@x.com", "wrong-parallel");
                            synchronized (invalid) {
                                if (r.location != null && r.location.contains("error=invalid_credentials")) {
                                    invalid[0]++;
                                } else if (r.location != null && r.location.contains("too_many_attempts")) {
                                    throttledCount[0]++;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                threads.add(t);
            }
            for (Thread t : threads) {
                t.start();
            }
            for (Thread t : threads) {
                t.join();
            }
            check("login: 48 parallel wrong passwords for one (IP, email) -> at most 5 are actually checked", invalid[0] <= 5 && invalid[0] + throttledCount[0] == 48, "checked=" + invalid[0] + " throttled=" + throttledCount[0]);

            // successful logins must not eat the per-IP allowance
            resetLimits();
            boolean allOk = true;
            for (int i = 0; i < 25; i++) {
                Browser bs = new Browser();
                Web.Raw r = bs.login("bob@x.com", PASSWORD);
                allOk &= r.location != null && r.location.endsWith("/user_dashboard.jsp");
            }
            check("login: 25 successful logins from one IP in a minute are not throttled (limit is on failures)", allOk, "");

            // ----- per-IP login limit (many different accounts) -----
            resetLimits();
            System.setProperty("LOGIN_MAX_FAILURES_PER_IP", "20");
            Browser b3 = new Browser();
            Web.Raw last = null;
            for (int i = 0; i < 20; i++) {
                last = b3.login("nobody" + i + "@x.com", "x");
            }
            Web.Raw afterIpLimit = b3.login("alice@x.com", PASSWORD);
            check("login: 20 failures from one IP (different emails) block the IP", afterIpLimit.location.contains("error=too_many_attempts"), afterIpLimit.location);

            // ----- OTP -----
            resetLimits();
            SENT.clear();
            Browser v = new Browser();
            Web.Raw s1 = v.otp("sendOTP", "alice@x.com", null);
            check("otp: first send succeeds", has(s1, "\"status\":\"success\"") && SENT.size() == 1, s1.body + " sent=" + SENT);
            String code1 = SENT.get(0).substring(SENT.get(0).indexOf('|') + 1);
            check("otp: the code is 6 digits", code1.matches("\\d{6}"), code1);
            Web.Raw s2 = v.otp("sendOTP", "alice@x.com", null);
            check("otp: sending again within the cooldown is refused politely, nothing is sent",
                    has(s2, "\"throttled\":true") && has(s2, "đợi") && SENT.size() == 1 && !has(s2, "\"status\":\"success\""), s2.body);
            Web.Raw sUnknown = v.otp("sendOTP", "ghost@x.com", null);
            check("otp: an unknown email gets the same success answer (no enumeration)", has(sUnknown, "\"status\":\"success\"") && SENT.size() == 1, sUnknown.body);

            // wrong guesses are counted per issued code, in the requester's own session
            Web.Raw w1 = v.otp("verifyOTP", "alice@x.com", "000000".equals(code1) ? "111111" : "000000");
            check("otp: first wrong guess reports 2 attempts left", has(w1, "còn 2 lần"), w1.body);
            Browser attacker = new Browser();
            for (int i = 0; i < 5; i++) {
                attacker.otp("verifyOTP", "alice@x.com", "123456");
            }
            String sidBefore = v.sid;
            Web.Raw stillOk = v.otp("verifyOTP", "alice@x.com", code1);
            check("otp: another browser failing 5 times does not lock the victim out; the right code still works", has(stillOk, "\"status\":\"success\""), stillOk.body);
            check("otp: the session id changes when reset is allowed", v.sid != null && !v.sid.equals(sidBefore), sidBefore + " -> " + v.sid);

            // three failures on one code destroy that code
            resetLimits();
            SENT.clear();
            Browser w = new Browser();
            w.otp("sendOTP", "alice@x.com", null);
            String code2 = SENT.get(0).substring(SENT.get(0).indexOf('|') + 1);
            String wrong = "000000".equals(code2) ? "111111" : "000000";
            w.otp("verifyOTP", "alice@x.com", wrong);
            w.otp("verifyOTP", "alice@x.com", wrong);
            Web.Raw third = w.otp("verifyOTP", "alice@x.com", wrong);
            check("otp: the 3rd wrong guess destroys the code", has(third, "Mã đã bị hủy") && !has(third, "\"locked\""), third.body);
            Web.Raw afterDead = w.otp("verifyOTP", "alice@x.com", code2);
            check("otp: the destroyed code no longer works even if guessed", has(afterDead, "không tồn tại hoặc đã bị hủy") && !has(afterDead, "\"status\":\"success\""), afterDead.body);
            Thread.sleep(2200);
            Web.Raw again = w.otp("sendOTP", "alice@x.com", null);
            check("otp: no email-wide lock: after the cooldown a new code can be requested", has(again, "\"status\":\"success\"") && SENT.size() == 2, again.body);

            // hourly caps: per (ip, email) 5/hour
            resetLimits();
            System.setProperty("OTP_SEND_COOLDOWN_SECONDS", "1");
            resetLimits();
            SENT.clear();
            Browser h = new Browser();
            int accepted = 0;
            Web.Raw lastHour = null;
            for (int i = 0; i < 7; i++) {
                lastHour = h.otp("sendOTP", "alice@x.com", null);
                if (has(lastHour, "\"status\":\"success\"")) {
                    accepted++;
                }
                Thread.sleep(1100);
            }
            check("otp: no more than 5 sends per hour for one (IP, email)", accepted == 5 && SENT.size() == 5 && has(lastHour, "quá nhiều lần"), accepted + " " + SENT.size() + " " + lastHour.body);

            // the same email from "another IP" is not possible on loopback; the per-email aggregate is checked on the limiter
            resetLimits();
            Object emailHour = Web.invokeInApp("Utils.AuthLimits", "otpEmailHour", new Class<?>[0]);
            java.lang.reflect.Method hit = emailHour.getClass().getMethod("hit", String.class);
            for (int i = 0; i < 10; i++) {
                hit.invoke(emailHour, "alice@x.com");
            }
            SENT.clear();
            Browser agg = new Browser();
            Web.Raw aggR = agg.otp("sendOTP", "Alice@X.com ", null);
            check("otp: the per-email aggregate cap (10/hour, all IPs together) stops a new send for that email", has(aggR, "\"throttled\":true") && SENT.isEmpty(), aggR.body);

            // ----- the whole reset flow, end to end -----
            resetLimits();
            SENT.clear();
            Browser rf = new Browser();
            rf.otp("sendOTP", "bob@x.com", null);
            String rcode = SENT.get(0).substring(SENT.get(0).indexOf('|') + 1);
            String sidBeforeReset = rf.sid;
            Web.Raw okOtp = rf.otp("verifyOTP", "bob@x.com", rcode);
            check("reset flow: the right code is accepted and the session id changes", has(okOtp, "\"status\":\"success\"") && !rf.sid.equals(sidBeforeReset), okOtp.body);
            rf.refresh("/reset_password.jsp?email=" + Web.enc("bob@x.com"));
            Web.Raw page = Web.raw("GET", "/reset_password.jsp?email=" + Web.enc("bob@x.com"), null, "JSESSIONID=" + rf.sid);
            check("reset flow: the reset page opens for that session and carries a fresh token", page.status == 200 && page.body.contains("csrf-token"), page.status + " " + page.location);
            Web.Raw weak = rf.post("/MainController", "action=processResetPassword&email=" + Web.enc("bob@x.com") + "&newPassword=weak");
            check("reset flow: a weak password is refused and sent back", weak.status == 302 && weak.location.contains("error=weak_password"), weak.location);
            Web.Raw done = rf.post("/MainController", "action=processResetPassword&email=" + Web.enc("bob@x.com") + "&newPassword=" + Web.enc("N3w!Passw0rd"));
            check("reset flow: the new password is saved", done.status == 302 && done.location.contains("reset=success"), done.status + " " + done.location);
            Browser relog = new Browser();
            Web.Raw oldPw = relog.login("bob@x.com", PASSWORD);
            Web.Raw newPw = relog.login("bob@x.com", "N3w!Passw0rd");
            check("reset flow: the old password stops working and the new one logs in", oldPw.location.contains("invalid_credentials") && newPw.location.endsWith("/user_dashboard.jsp"), oldPw.location + " | " + newPw.location);
            rf.refresh("/login.jsp");
            Web.Raw replayReset = rf.post("/MainController", "action=processResetPassword&email=" + Web.enc("bob@x.com") + "&newPassword=" + Web.enc("An0ther!Pass"));
            check("reset flow: the permission is single-use", replayReset.location != null && replayReset.location.contains("error=unauthorized"), String.valueOf(replayReset.location));

            // the OTP generator reaches all six digits and is not java.util.Random
            check("otp: generator source is SecureRandom", new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(System.getProperty("AISH_WEBROOT")).resolveSibling("src/java/Controller/ForgotPasswordController.java")), "UTF-8").contains("SecureRandom")
                    && !new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(System.getProperty("AISH_WEBROOT")).resolveSibling("src/java/Controller/ForgotPasswordController.java")), "UTF-8").contains("new Random()"), "");
        } finally {
            Web.stop();
        }
        Db.done();
    }
}
