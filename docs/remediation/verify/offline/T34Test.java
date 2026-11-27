import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * T3.4: revocable remember-me, on the real filter chain (embedded Tomcat). Every request is raw (no cookie jar), so
 * the checks see exactly which cookies the server sets and which the "browser" sends back.
 */
public class T34Test {

    static final String PASSWORD = "Passw0rd!";

    static void check(String name, boolean ok, String detail) {
        Db.check(name + (ok ? "" : " [" + detail + "]"), ok);
    }

    static String sha256(String v) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8))) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /** Logs in like a browser (token from the login page) and returns the raw login response. */
    static Web.Raw login(String email, boolean remember) throws Exception {
        Web.Raw page = Web.raw("GET", "/login.jsp", null, null);
        Matcher m = Pattern.compile("<meta name=\"csrf-token\" content=\"([^\"]*)\"").matcher(page.body);
        check("login page has a token (" + email + ")", m.find(), page.body.substring(0, Math.min(80, page.body.length())));
        String token = m.group(1);
        String sid = page.cookie("JSESSIONID");
        return Web.raw("POST", "/AuthController", "action=login&_csrf=" + Web.enc(token) + "&email=" + Web.enc(email)
                + "&password=" + Web.enc(PASSWORD) + (remember ? "&remember=true" : ""), "JSESSIONID=" + sid);
    }

    static Web.Raw withRemember(String value) throws Exception {
        return Web.raw("GET", "/FolderController?action=viewFolder", null, "remember_token=" + value);
    }

    static boolean loggedInPage(Web.Raw r) {
        return r.status == 200;
    }

    static boolean redirectedToLogin(Web.Raw r) {
        return r.status == 302 && r.location != null && r.location.contains("login.jsp");
    }

    public static void main(String[] a) throws Exception {
        Db.init();
        String hash = Utils.PasswordUtil.hashPassword(PASSWORD);
        int alice = Db.user("alice", "STUDENT");
        Db.exec("UPDATE users SET password_hash = ? WHERE user_id = ?", hash, alice);
        Web.start(System.getProperty("AISH_WEBROOT"), System.getProperty("AISH_CLASSES"));
        try {
            // ---------- issue ----------
            Web.Raw noRemember = login("alice@x.com", false);
            check("login without the checkbox sets no remember cookie and stores nothing",
                    noRemember.status == 302 && noRemember.cookie("remember_token") == null && Db.count("SELECT COUNT(*) FROM remember_tokens") == 0,
                    noRemember.setCookies.toString());

            Web.Raw first = login("alice@x.com", true);
            String v1 = first.cookie("remember_token");
            check("login with the checkbox sets a remember cookie", first.status == 302 && v1 != null && v1.contains("."), String.valueOf(v1));
            String line = first.cookieLine("remember_token");
            check("cookie is HttpOnly, SameSite=Lax, 30 days, scoped to the app",
                    line != null && line.contains("HttpOnly") && line.contains("SameSite=Lax") && line.contains("Max-Age=2592000") && line.contains("Path=/AIStudyHub"), String.valueOf(line));
            String selector = v1.substring(0, v1.indexOf('.'));
            String validator = v1.substring(v1.indexOf('.') + 1);
            check("only a hash of the validator is stored",
                    Db.count("SELECT COUNT(*) FROM remember_tokens WHERE selector = ? AND validator_hash = ?", selector, sha256(validator)) == 1
                    && Db.count("SELECT COUNT(*) FROM remember_tokens WHERE validator_hash = ?", validator) == 0, "");
            check("the old stateless cookie format (email|expiry|hmac) is not accepted", redirectedToLogin(withRemember("YWxpY2VAeC5jb218OTk5OTk5OTk5OTk5OXxzaWc")), "");

            // ---------- use and rotation ----------
            Web.Raw use1 = withRemember(v1);
            String v2 = use1.cookie("remember_token");
            check("a new browser with the cookie is signed in", loggedInPage(use1), use1.status + " " + use1.location);
            check("the validator is rotated on use (same selector, new validator)", v2 != null && !v2.equals(v1) && v2.startsWith(selector + "."), String.valueOf(v2));
            check("the new session id is different from the login one", use1.cookie("JSESSIONID") != null && !use1.cookie("JSESSIONID").equals(first.cookie("JSESSIONID")), "");
            Web.Raw use2 = withRemember(v2);
            String v3 = use2.cookie("remember_token");
            check("the rotated cookie works and rotates again", loggedInPage(use2) && v3 != null && !v3.equals(v2), use2.status + "");

            Web.Raw loginPage = Web.raw("GET", "/login.jsp", null, "remember_token=" + v3);
            check("login.jsp with a valid cookie goes straight to the app", loginPage.status == 302 && loginPage.location != null && loginPage.location.contains("MainController?action=explore"), loginPage.status + " " + loginPage.location);
            String v4 = loginPage.cookie("remember_token");
            if (v4 == null) {
                v4 = v3;
            }

            // ---------- replay of an old value ----------
            Web.Raw replay = withRemember(v1);
            check("replaying an old cookie value fails", redirectedToLogin(replay), replay.status + " " + replay.location);
            check("... and that token is revoked (the stolen/rotated pair dies together)", Db.count("SELECT COUNT(*) FROM remember_tokens WHERE selector = ?", selector) == 0
                    && redirectedToLogin(withRemember(v4)), "");

            // ---------- logout ----------
            Web.Raw l2 = login("alice@x.com", true);
            String r2 = l2.cookie("remember_token");
            String sid2 = l2.cookie("JSESSIONID");
            Web.Raw page2 = Web.raw("GET", "/FolderController?action=viewFolder", null, "JSESSIONID=" + sid2 + "; remember_token=" + r2);
            Matcher tm = Pattern.compile("<meta name=\"csrf-token\" content=\"([^\"]*)\"").matcher(page2.body);
            check("signed-in page after remember login has a token", page2.status == 200 && tm.find(), page2.status + "");
            String tok2 = tm.group(1);
            String sid2b = page2.cookie("JSESSIONID") != null ? page2.cookie("JSESSIONID") : sid2;
            Web.Raw out = Web.raw("POST", "/MainController", "action=logout&_csrf=" + Web.enc(tok2), "JSESSIONID=" + sid2b + "; remember_token=" + r2);
            check("logout clears the cookie", out.status == 302 && out.cookieLine("remember_token") != null && out.cookieLine("remember_token").contains("Max-Age=0"), String.valueOf(out.setCookies));
            check("logout deletes the token: the old cookie value no longer logs in", Db.count("SELECT COUNT(*) FROM remember_tokens") == 0 && redirectedToLogin(withRemember(r2)), "");

            // ---------- password change / reset ----------
            String r3 = login("alice@x.com", true).cookie("remember_token");
            String r3b = login("alice@x.com", true).cookie("remember_token");
            check("two devices hold two tokens", Db.count("SELECT COUNT(*) FROM remember_tokens WHERE user_id = ?", alice) == 2, "");
            check("password reset succeeds", new Model.DAO.UserDAO().updatePassword("alice@x.com", Utils.PasswordUtil.hashPassword("An0ther!Pass")), "");
            check("password change revokes the tokens on every device",
                    Db.count("SELECT COUNT(*) FROM remember_tokens WHERE user_id = ?", alice) == 0 && redirectedToLogin(withRemember(r3)) && redirectedToLogin(withRemember(r3b)), "");
            Db.exec("UPDATE users SET password_hash = ? WHERE user_id = ?", hash, alice);

            // ---------- ban ----------
            String r4 = login("alice@x.com", true).cookie("remember_token");
            Model.DTO.User u = new Model.DAO.UserDAO().getUserById(alice);
            u.setStatus("BANNED");
            new Model.DAO.UserDAO().updateUser(u);
            check("banning the account revokes its tokens", Db.count("SELECT COUNT(*) FROM remember_tokens WHERE user_id = ?", alice) == 0 && redirectedToLogin(withRemember(r4)), "");
            // a token that somehow survives still cannot sign a banned user in
            Db.exec("INSERT INTO remember_tokens (user_id, selector, validator_hash, expires_at) VALUES (?,?,?,DATEADD('DAY', 5, CURRENT_TIMESTAMP))", alice, "bannedsel", sha256("bannedval"));
            check("a surviving token of a banned user is refused and removed", redirectedToLogin(withRemember("bannedsel.bannedval")) && Db.count("SELECT COUNT(*) FROM remember_tokens WHERE selector = 'bannedsel'") == 0, "");
            Db.exec("UPDATE users SET status = 'ACTIVE' WHERE user_id = ?", alice);

            // ---------- expiry, tampering, garbage ----------
            String r5 = login("alice@x.com", true).cookie("remember_token");
            Db.exec("UPDATE remember_tokens SET expires_at = DATEADD('DAY', -1, CURRENT_TIMESTAMP) WHERE selector = ?", r5.substring(0, r5.indexOf('.')));
            check("an expired token is refused and removed", redirectedToLogin(withRemember(r5)) && Db.count("SELECT COUNT(*) FROM remember_tokens WHERE selector = ?", r5.substring(0, r5.indexOf('.'))) == 0, "");

            String r6 = login("alice@x.com", true).cookie("remember_token");
            String sel6 = r6.substring(0, r6.indexOf('.'));
            check("a right selector with a wrong validator is refused and the token revoked",
                    redirectedToLogin(withRemember(sel6 + ".AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) && Db.count("SELECT COUNT(*) FROM remember_tokens WHERE selector = ?", sel6) == 0, "");

            boolean garbageOk = true;
            String detail = "";
            for (String junk : new String[]{"abc", "a.b.c", ".", "x.", ".y", "%00.%00", "'; DROP TABLE users;--.x", new String(new char[500]).replace('\0', 'z') + ".q"}) {
                Web.Raw g = withRemember(junk);
                if (!redirectedToLogin(g)) {
                    garbageOk = false;
                    detail += junk.substring(0, Math.min(10, junk.length())) + "=" + g.status + " ";
                }
            }
            check("malformed cookies are refused without errors", garbageOk, detail);
            check("users table is intact after the injection attempt", Db.count("SELECT COUNT(*) FROM users") >= 1, "");
        } finally {
            Web.stop();
        }
        Db.done();
    }
}
