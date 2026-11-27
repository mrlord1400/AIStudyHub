/**
 * T3.1: output escaping, checked on the RENDERED pages. The real JSPs run in an embedded Tomcat (Web) against H2 that
 * holds hostile names. For each page: the hostile markup must never appear raw, and the escaped form must be there
 * (so the page really rendered the data and the check is not vacuous).
 */
public class T31Test {

    static final String P1 = "\"><img src=x onerror=alert(1)>";
    static final String P2 = "'</script><script>alert(2)</script>";
    static final String P3 = "x\" autofocus onfocus=\"alert(9)";
    static final String[] FORBIDDEN = {
        "<img src=x onerror", "</script><script>alert(2)", "x\" autofocus onfocus=", "<svg onload", "<u>ZZ", "<b>ZZ", "<script>alert(5)", "onerror=alert(6)>"
    };
    static final String ADMIN_EMAIL = "o'x\"<b>ZZ@t31.com";
    static final String ESC1 = "&lt;img src=x onerror=alert(1)&gt;";

    static void page(String name, String path, String... mustContain) throws Exception {
        Web.Resp r = Web.get(path);
        boolean ok = r.status == 200;
        String why = ok ? "" : " (status " + r.status + " -> " + r.location + ")";
        for (String f : FORBIDDEN) {
            if (r.body.contains(f)) {
                ok = false;
                why += " RAW[" + f + "]";
            }
        }
        for (String m : mustContain) {
            if (!r.body.contains(m)) {
                ok = false;
                why += " MISSING[" + m + "]";
            }
        }
        Db.check(name + why, ok);
    }

    static void esc(String in, String expected) {
        String got = Utils.Html.esc(in);
        Db.check("Html.esc(" + in + ") = " + expected + (expected.equals(got) ? "" : " but got " + got), expected.equals(got));
    }

    public static void main(String[] a) throws Exception {
        esc(null, "");
        esc("", "");
        esc("plain Tiếng Việt", "plain Tiếng Việt");
        esc("a&b<c>d\"e'f", "a&amp;b&lt;c&gt;d&quot;e&#39;f");
        esc("&amp;", "&amp;amp;");
        esc(P1, "&quot;&gt;&lt;img src=x onerror=alert(1)&gt;");
        Db.check("Html.esc takes any object (Integer 42)", "42".equals(Utils.Html.esc(42)));
        Db.check("Html.escOr uses the fallback for null and empty", "—".equals(Utils.Html.escOr(null, "—")) && "—".equals(Utils.Html.escOr("", "—")) && "&lt;".equals(Utils.Html.escOr("<", "—")));

        Db.init();
        String hash = Utils.PasswordUtil.hashPassword("Passw0rd!");
        Db.exec("INSERT INTO users (username, email, password_hash, role) VALUES (?,?,?,?)", P1, ADMIN_EMAIL, hash, "ADMIN");
        Db.exec("INSERT INTO users (username, email, password_hash, role) VALUES (?,?,?,?)", P2, "a\"><svg onload=alert(3)>@x.com", hash, "STUDENT");
        Db.exec("INSERT INTO users (username, email, password_hash, role) VALUES (?,?,?,?)", P3, "v3@x.com", hash, "STUDENT");
        int victim = (int) Db.count("SELECT user_id FROM users WHERE email = 'v3@x.com'");
        Db.exec("INSERT INTO documents (user_id, title, file_extension, cloud_storage_url, file_size_mb, sharing_permission, total_report_score, is_flagged) VALUES (?,?,?,?,?,?,?,?)",
                victim, P1, "pdf", victim + "/0f0f0f0f-0f0f-0f0f-0f0f-0f0f0f0f0f0f.pdf", 1, "PUBLIC", 6, true);
        int doc = (int) Db.count("SELECT MAX(document_id) FROM documents");
        Db.exec("INSERT INTO report_reason_configs (reason_code, severity_level, base_score, auto_flag_threshold, description) VALUES (?,?,?,?,?)",
                "ZZ'\"><u>ZZ", "LOW", 1, 5, P2 + P3);
        Db.exec("INSERT INTO document_reports (document_id, reporter_id, reason_code, additional_details, status) VALUES (?,?,?,?,?)",
                doc, victim, "ZZ'\"><b>ZZ", P1 + P2, "PENDING");
        Db.exec("INSERT INTO transactions (user_id, amount, type, status) VALUES (?,?,?,?)", victim, 100000, "DEPOSIT", "PENDING");
        Db.exec("INSERT INTO subscriptions (tier_name, max_storage_mb, ai_prompt_limit_per_day, price, total_storage_mb) VALUES (?,?,?,?,?)", P2, 1, 1, 1, 1);

        String root = System.getProperty("AISH_WEBROOT", System.getenv("AISH_WEBROOT"));
        String classes = System.getProperty("AISH_CLASSES", System.getenv("AISH_CLASSES"));
        Web.start(root, classes);
        try {
            Web.Resp login = Web.login(ADMIN_EMAIL, "Passw0rd!");
            Db.check("admin login redirects to the admin dashboard (" + login.status + " " + login.location + ")",
                    login.status == 302 && login.location != null && login.location.endsWith("/admin_dashboard.jsp"));

            page("admin users page", "/MainController?action=listUsers", ESC1, "&#39;&lt;/script&gt;&lt;script&gt;alert(2)&lt;/script&gt;",
                    "x&quot; autofocus onfocus=&quot;alert(9)", "&lt;svg onload=alert(3)&gt;");
            page("admin documents page", "/AdminController?action=listPublicDocs", ESC1);
            page("admin transactions page", "/MainController?action=adminListTransactions", "x&quot; autofocus onfocus=&quot;alert(9)");
            page("admin report-config page", "/MainController?action=reportConfigList", "&lt;u&gt;ZZ", "&lt;/script&gt;&lt;script&gt;alert(2)");
            page("admin document reports page", "/MainController?action=adminReportList&documentId=" + doc, "&lt;b&gt;ZZ", ESC1);
            page("admin system-config page", "/admin/system-config", "&lt;/script&gt;&lt;script&gt;alert(2)");
            page("admin dashboard page", "/MainController?action=listDashboard", ESC1);
            page("admin profile page", "/AdminController?action=profile", "value=\"&quot;&gt;&lt;img src=x onerror=alert(1)&gt;\"", "&lt;b&gt;ZZ@t31.com");

            // ---------- a normal user with hostile data (owner view) ----------
            Web.forgetCookies();
            Db.exec("INSERT INTO users (username, email, password_hash, role) VALUES (?,?,?,?)", P2, "student@t31.com", hash, "STUDENT");
            int stu = (int) Db.count("SELECT user_id FROM users WHERE email = 'student@t31.com'");
            Db.exec("INSERT INTO friendships (requester_id, addressee_id, status) VALUES (?,?,?)", stu, victim, "ACCEPTED");
            Db.exec("INSERT INTO folders (user_id, folder_name) VALUES (?,?)", stu, P1);
            int folder = (int) Db.count("SELECT MAX(folder_id) FROM folders");
            Db.exec("INSERT INTO documents (user_id, folder_id, title, file_extension, cloud_storage_url, file_size_mb, sharing_permission, share_link_token) VALUES (?,?,?,?,?,?,?,?)",
                    stu, folder, P1, "pdf", stu + "/0f0f0f0f-0f0f-0f0f-0f0f-0f0f0f0f0f0e.pdf", 1, "PUBLIC", "tok-student-doc");
            int sdoc = (int) Db.count("SELECT MAX(document_id) FROM documents");
            Db.exec("INSERT INTO chat_sessions (session_name, user_id) VALUES (?,?)", P2, stu);
            int sess = (int) Db.count("SELECT MAX(session_id) FROM chat_sessions");
            Db.exec("INSERT INTO chat_messages (session_id, sender, message_content) VALUES (?,?,?)", sess, "USER", "<script>alert(5)</script> " + P1);
            Db.exec("INSERT INTO chat_messages (session_id, sender, message_content) VALUES (?,?,?)", sess, "BOT", "**x** <img src=x onerror=alert(6)>");

            Web.Resp sl = Web.login("student@t31.com", "Passw0rd!");
            Db.check("student login redirects to the user dashboard (" + sl.status + " " + sl.location + ")",
                    sl.status == 302 && sl.location != null && sl.location.endsWith("/user_dashboard.jsp"));

            String J1 = "\\u003cimg src\\u003dx onerror\\u003dalert(1)\\u003e"; // Gson's escaped form of the P1 markup
            page("user dashboard (folder view, Gson data)", "/FolderController?action=viewFolder", J1, "&#39;&lt;/script&gt;");
            page("user dashboard inside a folder", "/FolderController?action=viewFolder&folderId=" + folder, ESC1);
            page("explore page (Gson data)", "/DocumentController?action=explore", J1);
            page("chat main page", "/SessionController?action=chatMain", "&#39;&lt;/script&gt;&lt;script&gt;");
            page("chat session page (server-rendered messages)", "/SessionController?action=viewSession&sessionId=" + sess,
                    "&lt;script&gt;alert(5)&lt;/script&gt;", "&lt;img src=x onerror=alert(6)&gt;", "&#39;&lt;/script&gt;&lt;script&gt;");
            page("document view page", "/DocumentController?action=viewPage&docId=" + sdoc, ESC1);
            page("document edit page", "/MainController?action=editDoc&docId=" + sdoc, "value=\"&quot;&gt;&lt;img");
            page("folder edit page", "/FolderController?action=editFolder&folderId=" + folder, "value=\"&quot;&gt;&lt;img");
            page("search results page", "/SearchController?keyword=" + Web.enc(P1), ESC1);
            page("friends page", "/MainController?action=friendList", "x&quot; autofocus onfocus=&quot;alert(9)");
            page("profile page", "/MainController?action=profile", "value=\"&#39;&lt;/script&gt;");
            page("report create page", "/MainController?action=report&documentId=" + sdoc, "&lt;u&gt;ZZ", "&lt;/script&gt;&lt;script&gt;alert(2)");
            page("wallet page", "/MainController?action=wallet", "&#39;&lt;/script&gt;");

            // ---------- anonymous visitor ----------
            Web.forgetCookies();
            page("guest dashboard (Gson data)", "/MainController?action=guest", J1);
            page("public document page", "/DocumentController?action=viewPublicPage&docId=" + sdoc, ESC1);

            // ---------- every JSP still compiles under Jasper (as the admin, who is allowed on all of them) ----------
            Web.login(ADMIN_EMAIL, "Passw0rd!");
            java.io.File[] jsps = new java.io.File(root).listFiles((d, n) -> n.endsWith(".jsp"));
            java.util.Arrays.sort(jsps);
            for (java.io.File f : jsps) {
                Web.Resp r = Web.get("/" + f.getName());
                Db.check("JSP compiles and does not fail: " + f.getName() + " (" + r.status + ")", r.status < 500);
            }
        } finally {
            Web.stop();
        }
        Db.done();
    }
}
