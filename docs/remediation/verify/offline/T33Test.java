import java.lang.reflect.Proxy;

/**
 * T3.3: session hardening and security headers on the real web.xml (embedded Tomcat):
 * headers on pages, redirects and error pages; session id and CSRF token change at login;
 * session cookie flags; banned accounts answer the same whatever password is sent.
 */
public class T33Test {

    static void check(String name, boolean ok, String detail) {
        Db.check(name + (ok ? "" : " [" + detail + "]"), ok);
    }

    static boolean hasSecurityHeaders(Web.Resp r) {
        return "nosniff".equals(r.header("X-Content-Type-Options"))
                && "SAMEORIGIN".equals(r.header("X-Frame-Options"))
                && "same-origin".equals(r.header("Referrer-Policy"));
    }

    public static void main(String[] a) throws Exception {
        // the listener logic without a container
        final boolean[] seen = new boolean[2];
        javax.servlet.SessionCookieConfig stub = (javax.servlet.SessionCookieConfig) Proxy.newProxyInstance(T33Test.class.getClassLoader(),
                new Class<?>[]{javax.servlet.SessionCookieConfig.class}, (p, m, args) -> {
                    if (m.getName().equals("setHttpOnly")) {
                        seen[0] = (Boolean) args[0];
                    } else if (m.getName().equals("setSecure")) {
                        seen[1] = (Boolean) args[0];
                    }
                    return null;
                });
        Listener.SessionCookieListener.configure(stub, true);
        check("APP_HTTPS=true marks the session cookie Secure + HttpOnly", seen[0] && seen[1], "");
        Listener.SessionCookieListener.configure(stub, false);
        check("APP_HTTPS=false: HttpOnly yes, Secure no", seen[0] && !seen[1], "");

        Db.init();
        String hash = Utils.PasswordUtil.hashPassword("Passw0rd!");
        int alice = Db.user("alice", "STUDENT");
        int mallory = Db.user("mallory", "STUDENT");
        Db.exec("UPDATE users SET password_hash = ? WHERE user_id IN (?, ?)", hash, alice, mallory);
        Db.exec("UPDATE users SET status = 'BANNED' WHERE user_id = ?", mallory);

        Web.start(System.getProperty("AISH_WEBROOT"), System.getProperty("AISH_CLASSES"));
        try {
            Web.Resp login = Web.get("/login.jsp");
            check("page response has the three security headers", login.status == 200 && hasSecurityHeaders(login), login.headers.toString());
            Web.Resp redirect = Web.get("/FolderController?action=viewFolder");
            check("redirect response has them too", redirect.status == 302 && hasSecurityHeaders(redirect), redirect.headers.toString());
            Web.Resp notFound = Web.get("/uploads/anything.pdf");
            check("error response (404) has them too", notFound.status == 404 && hasSecurityHeaders(notFound), notFound.status + " " + notFound.headers);
            Web.Resp js = Web.get("/js/security.js");
            check("static file has them too", js.status == 200 && hasSecurityHeaders(js), js.headers.toString());

            String raw = Web.rawHeaders("/login.jsp");
            String cookie = null;
            for (String line : raw.split("\r?\n")) {
                if (line.toLowerCase().startsWith("set-cookie: jsessionid")) {
                    cookie = line;
                }
            }
            check("session cookie is HttpOnly and SameSite=Lax", cookie != null && cookie.contains("HttpOnly") && cookie.contains("SameSite=Lax"), String.valueOf(cookie));

            // session id and CSRF token change at login
            String sidBefore = Web.sessionId();
            String tokenBefore = Web.csrf("/login.jsp");
            Web.Resp ok = Web.post("/AuthController", "action=login&_csrf=" + Web.enc(tokenBefore) + "&email=alice%40x.com&password=" + Web.enc("Passw0rd!"));
            String sidAfter = Web.sessionId();
            check("login succeeds", ok.status == 302 && ok.location != null && ok.location.endsWith("/user_dashboard.jsp"), ok.status + " " + ok.location);
            check("JSESSIONID differs before and after login", sidBefore != null && sidAfter != null && !sidBefore.equals(sidAfter), sidBefore + " -> " + sidAfter);
            String tokenAfter = Web.csrf("/FolderController?action=viewFolder");
            check("the CSRF token is renewed at login", tokenAfter != null && !tokenAfter.equals(tokenBefore), tokenBefore + " -> " + tokenAfter);
            check("the old CSRF token no longer works", Web.post("/DocumentController", "action=updateDoc&_csrf=" + Web.enc(tokenBefore)).status == 403, "");

            // without the new cookie nothing is reachable
            Web.forgetCookies();
            Web.Resp fixed = Web.get("/FolderController?action=viewFolder");
            check("without the session cookie nothing is reachable", fixed.status == 302, fixed.status + " " + fixed.location);

            // banned: same answer for right and wrong password
            Web.forgetCookies();
            String t1 = Web.csrf("/login.jsp");
            Web.Resp bannedRight = Web.post("/AuthController", "action=login&_csrf=" + Web.enc(t1) + "&email=mallory%40x.com&password=" + Web.enc("Passw0rd!"));
            Web.Resp bannedWrong = Web.post("/AuthController", "action=login&_csrf=" + Web.enc(t1) + "&email=mallory%40x.com&password=" + Web.enc("not-the-password"));
            check("banned account: right and wrong password get the same answer",
                    bannedRight.status == 302 && bannedRight.location != null && bannedRight.location.equals(bannedWrong.location)
                    && bannedRight.location.contains("error=banned"), bannedRight.location + " vs " + bannedWrong.location);
            Web.Resp wrong = Web.post("/AuthController", "action=login&_csrf=" + Web.enc(t1) + "&email=alice%40x.com&password=" + Web.enc("wrong"));
            check("wrong password still gets invalid_credentials", wrong.location != null && wrong.location.contains("error=invalid_credentials"), String.valueOf(wrong.location));
        } finally {
            Web.stop();
        }
        Db.done();
    }
}
