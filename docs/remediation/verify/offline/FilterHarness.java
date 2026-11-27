import java.lang.reflect.*;
import java.util.*;
import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import Filter.AuthFilter;
import javax.servlet.http.*;

/** Drives Filter.AuthFilter with proxy request/response/session objects. No Tomcat needed. */
public class FilterHarness {
    static int fail = 0;

    static Object proxy(Class<?> c, InvocationHandler h) {
        return Proxy.newProxyInstance(c.getClassLoader(), new Class<?>[]{c}, h);
    }

    /** returns "PASS-THROUGH", "REDIRECT:<url>", "STATUS:<n>" or "ERROR:<n>" */
    static String run(String servletPath, String action, String who) throws Exception {
        final Map<String, Object> sessionAttrs = new HashMap<>();
        if (!"anon".equals(who)) {
            sessionAttrs.put("userId", 7);
            sessionAttrs.put("role", "admin".equals(who) ? "ADMIN" : "STUDENT");
        }
        final HttpSession session = (HttpSession) proxy(HttpSession.class, (p, m, a) -> {
            if (m.getName().equals("getAttribute")) return sessionAttrs.get(a[0]);
            return null;
        });
        final String[] result = {"PASS-THROUGH"};
        final java.io.StringWriter sw = new java.io.StringWriter();
        HttpServletRequest req = (HttpServletRequest) proxy(HttpServletRequest.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getServletPath": return servletPath;
                case "getPathInfo": return null;
                case "getParameter": return "action".equals(a[0]) ? action : null;
                case "getSession": return sessionAttrs.isEmpty() ? null : session;
                case "getContextPath": return "/AIStudyHub";
                case "getCharacterEncoding": return "UTF-8";
                case "getDispatcherType": return DispatcherType.FORWARD;
                default: return null;
            }
        });
        HttpServletResponse res = (HttpServletResponse) proxy(HttpServletResponse.class, (p, m, a) -> {
            switch (m.getName()) {
                case "sendRedirect": result[0] = "REDIRECT:" + a[0]; return null;
                case "sendError": result[0] = "ERROR:" + a[0]; return null;
                case "setStatus": result[0] = "STATUS:" + a[0]; return null;
                case "getWriter": return new java.io.PrintWriter(sw);
                default: return null;
            }
        });
        FilterChain chain = (rq, rs) -> { /* reached the servlet */ };
        new AuthFilter().doFilter(req, res, chain);
        return result[0];
    }

    static void check(String path, String action, String who, String expectPrefix) throws Exception {
        String got = run(path, action, who);
        boolean ok = got.startsWith(expectPrefix);
        if (!ok) fail++;
        System.out.printf("%-5s %-6s %-34s action=%-24s -> %s%s%n", ok ? "PASS" : "FAIL", who, path, action, got, ok ? "" : "   (expected " + expectPrefix + ")");
    }

    public static void main(String[] a) throws Exception {
        String P = "PASS-THROUGH", L = "REDIRECT:/AIStudyHub/login.jsp";
        // T1.1 verify list
        check("/AdminController", "createUser", "anon", L);
        check("/AdminController", "listUsers", "student", "ERROR:403");
        check("/admin/system-config", null, "student", "ERROR:403");
        check("/ReportConfigController", "reportConfigDelete", "student", "ERROR:403");
        check("/admin_manageUser.jsp", null, "student", "ERROR:403");
        check("/AdminController", "listUsers", "admin", P);
        check("/admin/system-config", null, "admin", P);
        check("/admin_manageUser.jsp", null, "admin", P);
        check("/AdminReportController", "adminDeleteDocument", "student", "ERROR:403");
        check("/TransactionController", "adminUpdateTransaction", "student", "ERROR:403");
        check("/TransactionController", "adminListTransactions", "anon", L);
        check("/TransactionController", "adminUpdateTransaction", "admin", P);
        check("/TransactionController", "createTransaction", "student", P);
        check("/TransactionController", "buyPremium", "anon", L);
        // case-insensitive admin matching
        check("/ADMINCONTROLLER", "listUsers", "student", "ERROR:403");
        check("/Admin_manageUser.jsp", null, "student", "ERROR:403");
        check("/TransactionController", "ADMINUpdateTransaction", "student", "ERROR:403");
        // guest / public
        check("/login.jsp", null, "anon", P);
        check("/register.jsp", null, "anon", P);
        check("/forgot_password.jsp", null, "anon", P);
        check("/reset_password.jsp", null, "anon", P);
        check("/index.jsp", null, "anon", P);
        check("/", null, "anon", P);
        check("/guest_dashboard.jsp", null, "anon", P);
        check("/publicDocument_view.jsp", null, "anon", P);
        check("/AuthController", "login", "anon", P);
        check("/ForgotPasswordController", "sendOTP", "anon", P);
        check("/MainController", "guest", "anon", P);
        check("/MainController", "listUsers", "anon", P);          // router is public; the FORWARD target is what gets checked
        check("/DocumentController", "viewPublicPage", "anon", P);
        check("/DocumentController", "viewDoc", "anon", P);
        // default deny
        check("/DocumentController", "deleteDoc", "anon", L);
        check("/DocumentController", "toggleBookmark", "anon", "STATUS:401");
        check("/DocumentController", "explore", "anon", L);
        check("/user_dashboard.jsp", null, "anon", L);
        check("/FileExplore.jsp", null, "anon", L);
        check("/uploads/1/secret.pdf", null, "anon", "ERROR:404");      // T2.1: legacy uploads folder is dead for everyone
        check("/uploads/1/secret.pdf", null, "student", "ERROR:404");
        check("/UPLOADS/1/x.jsp", null, "admin", "ERROR:404");
        check("/ChatBotController", null, "anon", "STATUS:401");
        check("/SessionController", "pinSession", "anon", "STATUS:401");
        check("/SessionController", "chatMain", "anon", L);
        check("/SomethingNew.jsp", null, "anon", L);
        check("/SomethingNew.jsp", null, "student", P);
        check("/user_dashboard.jsp", null, "student", P);
        check("/UploadController", "upload", "anon", L);
        System.out.println(fail == 0 ? "ALL PASS" : fail + " FAILED");
        System.exit(fail == 0 ? 0 : 1);
    }
}
