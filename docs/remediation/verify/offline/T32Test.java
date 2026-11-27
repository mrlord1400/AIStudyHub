import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * T3.2: CSRF. The real CsrfFilter, web.xml and JSPs on embedded Tomcat:
 *  - every page that can post carries the token (meta tag) and csrf.js;
 *  - POST without / with a wrong token -> 403 and nothing changes; with the token (param or header) it works;
 *  - GET on a mutating action -> 405 and nothing changes;
 *  - AJAX rejections are JSON, the chat endpoint answers text/plain;
 *  - multipart uploads are covered too.
 * Also unit checks for Utils.Csrf.
 */
public class T32Test {

    static void check(String name, boolean ok, String detail) {
        Db.check(name + (ok ? "" : " [" + detail + "]"), ok);
    }

    static String d(Web.Resp r) {
        return r.status + " " + r.location;
    }

    public static void main(String[] a) throws Exception {
        // ---------- Utils.Csrf unit checks ----------
        Db.check("Csrf: action list is case-insensitive", Utils.Csrf.isMutatingAction("DeleteDoc") && Utils.Csrf.isMutatingAction(" logout "));
        Db.check("Csrf: read-only actions are not mutating",
                !Utils.Csrf.isMutatingAction("viewDoc") && !Utils.Csrf.isMutatingAction("explore") && !Utils.Csrf.isMutatingAction(null)
                && !Utils.Csrf.isMutatingAction("viewFolder") && !Utils.Csrf.isMutatingAction("listUsers"));
        Db.check("Csrf: safe methods", Utils.Csrf.isSafeMethod("GET") && Utils.Csrf.isSafeMethod("head") && !Utils.Csrf.isSafeMethod("POST") && !Utils.Csrf.isSafeMethod("DELETE"));

        Db.init();
        String hash = Utils.PasswordUtil.hashPassword("Passw0rd!");
        int alice = Db.user("alice", "STUDENT");
        Db.exec("UPDATE users SET password_hash = ? WHERE user_id = ?", hash, alice);
        Db.exec("INSERT INTO documents (user_id, title, file_extension, cloud_storage_url, file_size_mb, share_link_token) VALUES (?,?,?,?,?,?)",
                alice, "doc-one", "pdf", alice + "/0f0f0f0f-0f0f-0f0f-0f0f-0f0f0f0f0f01.pdf", 1, "t1");
        Db.exec("INSERT INTO documents (user_id, title, file_extension, cloud_storage_url, file_size_mb, share_link_token) VALUES (?,?,?,?,?,?)",
                alice, "doc-two", "pdf", alice + "/0f0f0f0f-0f0f-0f0f-0f0f-0f0f0f0f0f02.pdf", 1, "t2");
        int doc1 = (int) Db.count("SELECT document_id FROM documents WHERE title = 'doc-one'");
        int doc2 = (int) Db.count("SELECT document_id FROM documents WHERE title = 'doc-two'");
        Db.exec("INSERT INTO folders (user_id, folder_name) VALUES (?,?)", alice, "f-one");
        int folder = (int) Db.count("SELECT MAX(folder_id) FROM folders");
        Db.exec("INSERT INTO chat_sessions (session_name, user_id) VALUES (?,?)", "chat-one", alice);
        int sess = (int) Db.count("SELECT MAX(session_id) FROM chat_sessions");

        Web.start(System.getProperty("AISH_WEBROOT"), System.getProperty("AISH_CLASSES"));
        try {
            // ---------- login page and login ----------
            String loginToken = Web.csrf("/login.jsp");
            check("login page carries a CSRF token", loginToken != null && loginToken.length() >= 40, "token=" + loginToken);
            Web.Resp r = Web.get("/login.jsp");
            check("login page loads csrf.js", r.body.contains("/js/csrf.js"), "");
            Web.Resp js = Web.get("/js/csrf.js");
            check("/js/csrf.js is public (anonymous 200, javascript)", js.status == 200 && js.body.contains("X-CSRF-Token"), d(js));

            r = Web.post("/AuthController", "action=login&email=alice%40x.com&password=" + Web.enc("Passw0rd!"));
            check("login POST without token -> 403", r.status == 403, d(r));
            r = Web.post("/AuthController", "action=login&_csrf=wrong-token&email=alice%40x.com&password=" + Web.enc("Passw0rd!"));
            check("login POST with a wrong token -> 403", r.status == 403, d(r));
            r = Web.post("/AuthController", "action=login&_csrf=" + Web.enc(loginToken) + "&email=alice%40x.com&password=" + Web.enc("Passw0rd!"));
            check("login POST with the token -> redirect to the dashboard", r.status == 302 && r.location != null && r.location.endsWith("/user_dashboard.jsp"), d(r));

            String token = Web.csrf("/FolderController?action=viewFolder");
            check("dashboard carries a token", token != null && token.length() >= 40, "token=" + token);

            // ---------- POST needs the token ----------
            r = Web.post("/DocumentController", "action=deleteDoc&docId=" + doc1);
            check("deleteDoc POST without token -> 403", r.status == 403, d(r));
            check("... and the document is still there", Db.count("SELECT COUNT(*) FROM documents WHERE document_id = ?", doc1) == 1, "");
            r = Web.post("/DocumentController", "action=deleteDoc&_csrf=nope&docId=" + doc1);
            check("deleteDoc POST with a wrong token -> 403", r.status == 403 && Db.count("SELECT COUNT(*) FROM documents WHERE document_id = ?", doc1) == 1, d(r));
            r = Web.post("/DocumentController", "action=deleteDoc&_csrf=" + Web.enc(token) + "&docId=" + doc1);
            check("deleteDoc POST with the token (form field) -> works", r.status == 302 && Db.count("SELECT COUNT(*) FROM documents WHERE document_id = ?", doc1) == 0, d(r));
            Map<String, String> h = new HashMap<>();
            h.put("X-CSRF-Token", token);
            r = Web.post("/DocumentController", "action=deleteDoc&docId=" + doc2, h);
            check("deleteDoc POST with the token (header) -> works", r.status == 302 && Db.count("SELECT COUNT(*) FROM documents WHERE document_id = ?", doc2) == 0, d(r));

            // a token from another session is worthless
            Web.forgetCookies();
            String otherToken = Web.csrf("/login.jsp");
            Web.post("/AuthController", "action=login&_csrf=" + Web.enc(otherToken) + "&email=alice%40x.com&password=" + Web.enc("Passw0rd!"));
            Db.exec("INSERT INTO documents (user_id, title, file_extension, cloud_storage_url, file_size_mb, share_link_token) VALUES (?,?,?,?,?,?)",
                    alice, "doc-three", "pdf", alice + "/0f0f0f0f-0f0f-0f0f-0f0f-0f0f0f0f0f03.pdf", 1, "t3");
            int doc3 = (int) Db.count("SELECT document_id FROM documents WHERE title = 'doc-three'");
            r = Web.post("/DocumentController", "action=deleteDoc&_csrf=" + Web.enc(token) + "&docId=" + doc3);
            check("a token from another session is rejected", r.status == 403 && Db.count("SELECT COUNT(*) FROM documents WHERE document_id = ?", doc3) == 1, d(r));
            token = Web.csrf("/FolderController?action=viewFolder");

            // ---------- GET never changes state ----------
            r = Web.get("/DocumentController?action=deleteDoc&docId=" + doc3);
            check("GET deleteDoc -> 405, document kept", r.status == 405 && Db.count("SELECT COUNT(*) FROM documents WHERE document_id = ?", doc3) == 1, d(r));
            r = Web.get("/MainController?action=deleteDoc&docId=" + doc3);
            check("GET deleteDoc through MainController -> 405", r.status == 405, d(r));
            r = Web.get("/FolderController?action=deleteFolder&folderId=" + folder);
            check("GET deleteFolder -> 405, folder kept", r.status == 405 && Db.count("SELECT COUNT(*) FROM folders WHERE folder_id = ?", folder) == 1, d(r));
            r = Web.get("/MainController?action=logout");
            check("GET logout -> 405 (still logged in)", r.status == 405 && Web.get("/FolderController?action=viewFolder").status == 200, d(r));
            r = Web.get("/SessionController?action=deleteSession&sessionId=" + sess);
            check("GET deleteSession -> 405, session kept", r.status == 405 && Db.count("SELECT COUNT(*) FROM chat_sessions WHERE session_id = ?", sess) == 1, d(r));
            r = Web.get("/FriendController?action=deleteFriendship&targetUserId=9999&returnPath=friendList");
            check("GET deleteFriendship -> 405", r.status == 405, d(r));
            r = Web.get("/FolderController?action=viewFolder");
            check("GET of a read-only action still works", r.status == 200, d(r));
            r = Web.get("/DocumentController?action=explore");
            check("GET explore still works", r.status == 200, d(r));

            // ---------- AJAX endpoints ----------
            Map<String, String> ajax = new HashMap<>();
            ajax.put("X-Requested-With", "fetch");
            r = Web.post("/SessionController?action=pinSession", "sessionId=" + sess + "&isPinned=true", ajax);
            check("AJAX POST without token -> 403 JSON with code CSRF_INVALID", r.status == 403 && r.body.contains("\"CSRF_INVALID\"") && r.body.contains("\"success\":false"), d(r) + " " + r.body);
            check("... and nothing changed", Db.count("SELECT COUNT(*) FROM chat_sessions WHERE session_id = ? AND is_pinned = TRUE", sess) == 0, "");
            ajax.put("X-CSRF-Token", token);
            r = Web.post("/SessionController?action=pinSession", "sessionId=" + sess + "&isPinned=true", ajax);
            check("AJAX POST with the header token -> works", r.status == 200 && Db.count("SELECT COUNT(*) FROM chat_sessions WHERE session_id = ? AND is_pinned = TRUE", sess) == 1, d(r) + " " + r.body);
            r = Web.post("/ChatBotController", "message=hi&sessionId=" + sess);
            check("chat POST without token -> 403 text/plain", r.status == 403 && !r.body.startsWith("{") && r.body.contains("tải lại trang"), d(r) + " " + r.body);

            // ---------- multipart ----------
            String boundary = "----t32boundary";
            String multipart = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"a.txt\"\r\nContent-Type: text/plain\r\n\r\nhello\r\n--" + boundary + "--\r\n";
            Map<String, String> mp = new HashMap<>();
            r = Web.postRaw("/UploadController?action=upload", multipart.getBytes(StandardCharsets.UTF_8), "multipart/form-data; boundary=" + boundary, mp);
            check("multipart upload without token -> 403", r.status == 403, d(r));
            r = Web.postRaw("/UploadController?action=upload&_csrf=" + Web.enc(token), multipart.getBytes(StandardCharsets.UTF_8), "multipart/form-data; boundary=" + boundary, mp);
            check("multipart upload with _csrf in the URL gets past the filter (not 403)", r.status != 403, d(r));

            // ---------- logout is a POST ----------
            r = Web.post("/MainController", "action=logout");
            check("logout POST without token -> 403", r.status == 403, d(r));
            r = Web.post("/MainController", "action=logout&_csrf=" + Web.enc(token));
            Web.Resp after = Web.get("/FolderController?action=viewFolder");
            check("logout POST with the token -> back to login, session over", r.status == 302 && after.status == 302, d(r) + " then " + d(after) + " body=" + after.body.substring(0, Math.min(300, after.body.length())).replaceAll("\s+", " "));

            // ---------- anonymous: nothing to forge, still blocked ----------
            Web.forgetCookies();
            r = Web.post("/DocumentController", "action=deleteDoc&docId=" + doc3);
            check("anonymous POST (no session) -> not executed", Db.count("SELECT COUNT(*) FROM documents WHERE document_id = ?", doc3) == 1 && (r.status == 403 || r.status == 302), d(r));

            // ---------- every page that can post carries the token ----------
            Web.forgetCookies();
            java.io.File[] jsps = new java.io.File(System.getProperty("AISH_WEBROOT")).listFiles((dd, n) -> n.endsWith(".jsp"));
            int missing = 0;
            StringBuilder names = new StringBuilder();
            for (java.io.File f : jsps) {
                String src = new String(java.nio.file.Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                boolean posts = src.toLowerCase().contains("method=\"post\"") || src.contains("method: 'POST'") || src.contains("data-post") || src.contains("action=logout");
                if (posts && src.contains("</head>") && !src.contains("csrf_head.jspf")) {
                    missing++;
                    names.append(f.getName()).append(' ');
                }
            }
            check("every JSP that posts includes csrf_head.jspf", missing == 0, names.toString());
        } finally {
            Web.stop();
        }
        Db.done();
    }
}
