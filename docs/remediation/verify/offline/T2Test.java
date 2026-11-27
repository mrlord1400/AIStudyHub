import Controller.*;
import Utils.FileStorage;
import Utils.FileTypes;
import Utils.StorageMigration;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/** Checks for T2.1 (storage outside the web root), T2.2 (migration) and T2.3 (type allow-list + content checks). */
public class T2Test {

    static byte[] b(String s) {
        return s.getBytes(StandardCharsets.ISO_8859_1);
    }

    static final byte[] PDF = b("%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF\n");
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    static final byte[] ZIP = {'P', 'K', 3, 4, 0, 0, 0, 0};
    static final byte[] HTML = b("<html><script>alert(1)</script></html>");

    static Path tmp(String p) throws Exception {
        return Files.createTempDirectory(p);
    }

    static int filesOf(Path root, int uid) throws Exception {
        Path d = root.resolve(Integer.toString(uid));
        if (!Files.isDirectory(d)) {
            return 0;
        }
        try (java.util.stream.Stream<Path> s = Files.list(d)) {
            return (int) s.count();
        }
    }

    static Http.Result upload(UploadController uc, int uid, String name, byte[] data, Map<String, Object> sess, String... extra) throws Exception {
        Http.sharedSession = sess;
        Http.part = new Http.UploadPart(name, data);
        String[] kv = new String[2 + extra.length];
        kv[0] = "action";
        kv[1] = "upload";
        System.arraycopy(extra, 0, kv, 2, extra.length);
        try {
            return Http.call(uc, "POST", Http.params(kv), uid, "STUDENT");
        } finally {
            Http.part = null;
            Http.sharedSession = null;
        }
    }

    static Http.Result act(UploadController uc, int uid, Map<String, Object> sess, String... kv) throws Exception {
        Http.sharedSession = sess;
        try {
            return Http.call(uc, "POST", Http.params(kv), uid, "STUDENT");
        } finally {
            Http.sharedSession = null;
        }
    }

    /** Storage key of the pending row (the session no longer holds the key). */
    static String keyOf(Map<String, Object> sess) throws Exception {
        return Db.str("SELECT cloud_storage_url FROM documents WHERE document_id=" + sess.get("pendingDocumentId"));
    }

    static String err(Http.Result r) {
        return String.valueOf(r.requestAttrs.get("errorMessage"));
    }

    public static void main(String[] args) throws Exception {
        Db.init();
        Path storage = tmp("aish-storage");
        System.setProperty("STORAGE_DIR", storage.toString());

        // ============ T2.3: FileTypes ============
        Db.check("T2.3 extensionOf takes the last extension, lower-cased", "pdf".equals(FileTypes.extensionOf("My.Report.PDF")) && "jsp".equals(FileTypes.extensionOf("a.pdf.jsp")));
        Db.check("T2.3 extensionOf: none / dotfile / trailing dot / path parts", "".equals(FileTypes.extensionOf("README")) && "".equals(FileTypes.extensionOf(".gitignore"))
                && "".equals(FileTypes.extensionOf("x.")) && "pdf".equals(FileTypes.extensionOf("C:\\temp\\x.pdf")) && "".equals(FileTypes.extensionOf(null)));
        Db.check("T2.3 allow-list is exactly the Conv §9 set", FileTypes.ALLOWED_EXTENSIONS.size() == 10 && FileTypes.isAllowedExtension("jpeg") && FileTypes.isAllowedExtension("md"));
        Db.check("T2.3 jsp/html/exe/svg/doc/empty are not allowed", !FileTypes.isAllowedExtension("jsp") && !FileTypes.isAllowedExtension("html")
                && !FileTypes.isAllowedExtension("exe") && !FileTypes.isAllowedExtension("svg") && !FileTypes.isAllowedExtension("doc") && !FileTypes.isAllowedExtension("") && !FileTypes.isAllowedExtension(null));
        Path probe = tmp("aish-probe");
        java.util.function.BiFunction<String, byte[], Boolean> match = (ext, data) -> {
            try {
                Path f = probe.resolve("f-" + System.nanoTime());
                Files.write(f, data);
                return FileTypes.contentMatches(ext, f);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        Db.check("T2.3 pdf magic: real pdf accepted, html and PK rejected", match.apply("pdf", PDF) && !match.apply("pdf", HTML) && !match.apply("pdf", ZIP));
        Db.check("T2.3 pdf magic accepted after a short preamble (within 1024 bytes)", match.apply("pdf", b("\n\n  %PDF-1.7\n")));
        Db.check("T2.3 office magic: PK accepted, pdf/html rejected", match.apply("docx", ZIP) && match.apply("xlsx", ZIP) && match.apply("pptx", ZIP) && !match.apply("docx", PDF) && !match.apply("pptx", HTML));
        Db.check("T2.3 image magic: png/jpg/gif accepted only with their own signature",
                match.apply("png", PNG) && match.apply("jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}) && match.apply("jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0})
                && match.apply("gif", b("GIF89a....")) && match.apply("gif", b("GIF87a....")) && !match.apply("png", HTML) && !match.apply("gif", PNG) && !match.apply("jpg", PNG));
        Db.check("T2.3 txt/md: text accepted, NUL byte anywhere rejected", match.apply("txt", b("xin chào")) && match.apply("md", b("# hi")) && !match.apply("txt", new byte[]{'a', 0, 'b'})
                && !match.apply("md", b("ok\u0000")));
        Db.check("T2.3 empty / too short files do not match binary types", !match.apply("pdf", new byte[0]) && !match.apply("png", new byte[]{1}) && !match.apply("docx", new byte[]{'P'}));
        Db.check("T2.3 a disallowed extension never matches", !match.apply("jsp", b("<%%>")) && !match.apply("html", HTML));

        // ============ T2.1: FileStorage ============
        String k1 = FileStorage.save(5, new java.io.ByteArrayInputStream(PDF), "pdf");
        Db.check("T2.1 save returns <uid>/<uuid>.<ext> and lands under STORAGE_DIR", k1.matches("^5/[0-9a-f-]{36}\\.pdf$") && Files.isRegularFile(storage.resolve(k1)) && FileStorage.isValidKey(k1));
        try (java.io.InputStream in = FileStorage.open(k1)) {
            Db.check("T2.1 open returns the bytes", java.util.Arrays.equals(PDF, readAll(in)));
        }
        String k2 = FileStorage.save(5, new java.io.ByteArrayInputStream(PDF), "pdf");
        Db.check("T2.1 two saves never collide (UUID names)", !k1.equals(k2));
        boolean threw = false;
        try {
            FileStorage.save(5, new java.io.ByteArrayInputStream(HTML), "jsp");
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        Db.check("T2.1 save refuses a disallowed extension (defence in depth)", threw && filesOf(storage, 5) == 2);
        threw = false;
        try {
            FileStorage.save(-1, new java.io.ByteArrayInputStream(PDF), "pdf");
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        Db.check("T2.1 save refuses a negative user id", threw);

        String[] bad = {"../x.pdf", "5/../../x.pdf", "/etc/passwd", "C:/x.pdf", "5\\a.pdf", "5/a.pdf", "5/../" + k1, "5/" + k1.substring(2).toUpperCase(),
            "5/00000000-0000-0000-0000-000000000000.jsp.pdf", "5/00000000-0000-0000-0000-000000000000.", "", " ", "/AIStudyHub/uploads/5/a.pdf", "5/00000000-0000-0000-0000-000000000000.pdf\n", "5/00000000-0000-0000-0000-000000000000.pdf/../../x"};
        boolean allBad = true;
        for (String s : bad) {
            if (FileStorage.isValidKey(s)) {
                allBad = false;
                System.out.println("   key unexpectedly valid: [" + s + "]");
            }
            try {
                FileStorage.resolve(s);
                allBad = false;
                System.out.println("   resolve unexpectedly succeeded: [" + s + "]");
            } catch (IllegalArgumentException expected) {
            }
        }
        Db.check("T2.1 traversal / absolute / legacy-URL / malformed keys are all rejected by isValidKey and resolve", allBad && !FileStorage.isValidKey(null));
        Db.check("T2.1 fileOrNull: null for invalid or missing key, File for a stored one", FileStorage.fileOrNull("../x") == null && FileStorage.fileOrNull("/AIStudyHub/uploads/1/a.pdf") == null
                && FileStorage.fileOrNull("5/00000000-0000-0000-0000-000000000000.pdf") == null && FileStorage.fileOrNull(k1) != null);
        Db.check("T2.1 delete on a legacy URL deletes nothing and does not throw", !FileStorage.delete("/AIStudyHub/uploads/1/a.pdf") && !FileStorage.delete(null) && !FileStorage.delete("../../x"));
        Db.check("T2.1 delete removes the file once, then reports false", FileStorage.delete(k1) && !FileStorage.delete(k1) && !Files.exists(storage.resolve(k1)));
        Db.check("T2.1 deleteUserFiles removes the whole user directory and only theirs",
                FileStorage.save(6, new java.io.ByteArrayInputStream(PDF), "pdf") != null && FileStorage.deleteUserFiles(5) == 1 && !Files.exists(storage.resolve("5")) && filesOf(storage, 6) == 1);
        FileStorage.deleteUserFiles(6);

        System.setProperty("STORAGE_DIR", "relative/dir");
        threw = false;
        try {
            FileStorage.root();
        } catch (IllegalStateException e) {
            threw = e.getMessage().contains("absolute");
        }
        Db.check("T2.1 a relative STORAGE_DIR is refused", threw);
        System.clearProperty("STORAGE_DIR");
        threw = false;
        try {
            FileStorage.root();
        } catch (IllegalStateException e) {
            threw = "Missing required config: STORAGE_DIR".equals(e.getMessage());
        }
        Db.check("T2.1 a missing STORAGE_DIR fails closed with 'Missing required config: STORAGE_DIR'", threw);
        System.setProperty("STORAGE_DIR", storage.toString());
        Db.check("T2.1 validateRoot: fine when outside the webapp", FileStorage.validateRoot(tmp("aish-otherweb").toString()).isEmpty());
Path cat = tmp("aish-catalina");        System.setProperty("catalina.base", cat.toString());        System.setProperty("STORAGE_DIR", cat.resolve("webapps").resolve("data").toString());        Db.check("T2.1 validateRoot: flags a storage dir under Tomcat webapps/", FileStorage.validateRoot(null).toString().contains("webapps"));        System.setProperty("STORAGE_DIR", cat.resolve("data").toString());        Db.check("T2.1 validateRoot: a sibling of webapps/ is fine", FileStorage.validateRoot(null).isEmpty());        System.clearProperty("catalina.base");        System.setProperty("STORAGE_DIR", storage.toString());
        Db.check("T2.1 validateRoot: flags a storage dir inside the webapp", !FileStorage.validateRoot(storage.getParent().toString()).isEmpty()
                && !FileStorage.validateRoot(storage.toString()).isEmpty());

        // ============ T2.1: /uploads is dead ============
        Db.check("T2.1 AccessRules.isRetiredUploadsPath", Utils.AccessRules.isRetiredUploadsPath("/uploads/1/a.pdf") && Utils.AccessRules.isRetiredUploadsPath("/Uploads/x")
                && Utils.AccessRules.isRetiredUploadsPath("/uploads") && !Utils.AccessRules.isRetiredUploadsPath("/uploads_notes.jsp") && !Utils.AccessRules.isRetiredUploadsPath("/DocumentController")
                && !Utils.AccessRules.isRetiredUploadsPath(null));

        // ============ UploadController end to end (T2.1 + T2.3) ============
        int A = Db.user("A", "STUDENT"), B = Db.user("B", "STUDENT");
        UploadController uc = Http.init(new UploadController());
        Map<String, Object> sess = new HashMap<>();
        Http.Result r;

        for (String evil : new String[]{"shell.jsp", "page.html", "run.exe", "a.svg", "noext", "x.pdf.jsp", "old.doc", "x.jspx"}) {
            r = upload(uc, A, evil, b("<%= 1 %>"), sess);
            Db.check("T2.3 upload '" + evil + "' is rejected with a Vietnamese message, nothing stored",
                    "/document_upload.jsp".equals(r.forward) && err(r).contains("không được hỗ trợ") && Db.count("SELECT COUNT(*) FROM documents WHERE user_id=?", A) == 0 && filesOf(storage, A) == 0);
        }
        r = upload(uc, A, "fake.pdf", HTML, sess);
        Db.check("T2.3 an HTML file renamed .pdf is rejected (content mismatch), no row, no file", "/document_upload.jsp".equals(r.forward) && err(r).contains("không khớp")
                && Db.count("SELECT COUNT(*) FROM documents WHERE user_id=?", A) == 0 && filesOf(storage, A) == 0);
        r = upload(uc, A, "bin.txt", new byte[]{'a', 0, 'b'}, sess);
        Db.check("T2.3 a .txt with NUL bytes is rejected", err(r).contains("không khớp") && filesOf(storage, A) == 0);
        r = upload(uc, A, "empty.pdf", new byte[0], sess);
        Db.check("T2.3 an empty file is rejected", err(r).contains("chọn một file") && filesOf(storage, A) == 0);
        r = upload(uc, A, "../../evil.pdf", PDF, sess);
        String evilKey = keyOf(sess);
        Db.check("T2.1 a path-like submitted name cannot influence the on-disk name", evilKey != null && evilKey.matches("^" + A + "/[0-9a-f-]{36}\\.pdf$")
                && Db.count("SELECT COUNT(*) FROM documents WHERE user_id=? AND title='evil'", A) == 1);
        act(uc, A, sess, "action", "cancel");

        Db.exec("UPDATE subscriptions SET max_storage_mb = 1 WHERE tier_id = 2");
        r = upload(uc, A, "big.pdf", concat(PDF, new byte[2 * 1024 * 1024]), sess);
        Db.check("T2.3 a file over the tier's per-file limit is rejected (limit read from the DB)", err(r).contains("File quá lớn") && err(r).contains("1 MB") && filesOf(storage, A) == 0);
        Db.exec("UPDATE subscriptions SET max_storage_mb = 50 WHERE tier_id = 2");
        r = upload(uc, A, "ok-after-raise.pdf", concat(PDF, new byte[2 * 1024 * 1024]), sess);
        Db.check("T2.3 raising the tier limit in the DB takes effect immediately", r.redirect != null && r.redirect.contains("step=edit"));
        act(uc, A, sess, "action", "cancel");
        Db.exec("UPDATE subscriptions SET max_storage_mb = 0 WHERE tier_id = 2");
        r = upload(uc, A, "tiny.pdf", PDF, sess);
        Db.check("T2.3 a tier whose limit is 0 means 'no uploads' (not the 100 MB maximum), nothing stored", err(r).contains("không cho phép") && filesOf(storage, A) == 0);
        Db.exec("UPDATE subscriptions SET max_storage_mb = 50 WHERE tier_id = 2");

        // valid upload -> confirm
        r = upload(uc, A, "Lecture 1.pdf", PDF, sess);
        String key1 = keyOf(sess);
        Integer pend1 = (Integer) sess.get("pendingDocumentId");
        Db.check("T2.1 valid pdf: redirect to step=edit, row holds a storage key (not a URL), file under STORAGE_DIR/<uid>/",
                r.redirect != null && r.redirect.contains("step=edit") && key1 != null && key1.matches("^" + A + "/[0-9a-f-]{36}\\.pdf$")
                && Db.count("SELECT COUNT(*) FROM documents WHERE document_id=? AND cloud_storage_url=? AND file_extension='pdf' AND title='Lecture 1'", pend1, key1) == 1
                && Files.isRegularFile(storage.resolve(key1)));
        r = act(uc, A, sess, "action", "confirm", "title", "Lecture 1", "sharingPermission", "PUBLIC");
        Db.check("T2.1 confirm keeps the same key and file (no rename), title/permission updated", r.redirect != null && r.redirect.contains("uploadSuccess=1")
                && Db.count("SELECT COUNT(*) FROM documents WHERE document_id=? AND cloud_storage_url=? AND sharing_permission='PUBLIC'", pend1, key1) == 1 && Files.isRegularFile(storage.resolve(key1)));
        int docA = pend1;

        // duplicate title -> keepBoth
        upload(uc, A, "Lecture 1.pdf", concat(PDF, b("v2")), sess);
        String key2 = keyOf(sess);
        int pend2 = (Integer) sess.get("pendingDocumentId");
        r = act(uc, A, sess, "action", "confirm", "title", "Lecture 1", "sharingPermission", "PRIVATE");
        Db.check("T2.1 duplicate title goes to the duplicate step", r.redirect != null && r.redirect.contains("step=duplicate"));
        r = act(uc, A, sess, "action", "keepBoth");
        Db.check("T2.1 keepBoth: second document 'Lecture 1 (1)' with its own file, first untouched",
                Db.count("SELECT COUNT(*) FROM documents WHERE document_id=? AND title='Lecture 1 (1)' AND cloud_storage_url=?", pend2, key2) == 1
                && Files.isRegularFile(storage.resolve(key2)) && Files.isRegularFile(storage.resolve(key1)));

        // duplicate title -> replace
        upload(uc, A, "Lecture 1.pdf", concat(PDF, b("v3")), sess);
        String key3 = keyOf(sess);
        int pend3 = (Integer) sess.get("pendingDocumentId");
        act(uc, A, sess, "action", "confirm", "title", "Lecture 1", "sharingPermission", "PRIVATE");
        r = act(uc, A, sess, "action", "replace");
        Db.check("T2.1 replace: the original document now points at the new file, the old file is gone, the pending row is gone",
                Db.count("SELECT COUNT(*) FROM documents WHERE document_id=? AND cloud_storage_url=?", docA, key3) == 1
                && Db.count("SELECT COUNT(*) FROM documents WHERE document_id=?", pend3) == 0
                && !Files.exists(storage.resolve(key1)) && Files.isRegularFile(storage.resolve(key3)));

        // cancel
        upload(uc, A, "Temp.pdf", PDF, sess);
        String key4 = keyOf(sess);
        r = act(uc, A, sess, "action", "cancel");
        Db.check("T2.1 cancel removes the pending row and its file", Db.count("SELECT COUNT(*) FROM documents WHERE cloud_storage_url=?", key4) == 0 && !Files.exists(storage.resolve(key4)));

        // A session that carries someone else's pending id must not delete or re-point anything (keys come from the DB row, owner-checked)
        String kBsecret = FileStorage.save(B, new java.io.ByteArrayInputStream(PDF), "pdf");
        int bPending = insertDoc(B, null, "b-pending", kBsecret);
        Map<String, Object> hijack = new HashMap<>();
        hijack.put("pendingDocumentId", bPending);
        act(uc, A, hijack, "action", "cancel");
        Db.check("T2.1 cancel with another user's pending id: B's row and file survive", Db.count("SELECT COUNT(*) FROM documents WHERE document_id=?", bPending) == 1 && Files.exists(storage.resolve(kBsecret)));
        hijack.put("pendingDocumentId", bPending);
        hijack.put("conflictTitle", "x");
        r = act(uc, A, hijack, "action", "confirm", "title", "stolen", "sharingPermission", "PUBLIC");
        Db.check("T2.1 confirm with another user's pending id is refused (session_expired), B's row unchanged",
                r.redirect != null && r.redirect.contains("session_expired") && Db.count("SELECT COUNT(*) FROM documents WHERE document_id=? AND title='b-pending' AND sharing_permission='PRIVATE'", bPending) == 1);
        hijack.put("pendingDocumentId", bPending);
        hijack.put("conflictTitle", "x");
        r = act(uc, A, hijack, "action", "keepBoth");
        Db.check("T2.1 keepBoth with another user's pending id is refused, B's file survives", r.redirect != null && r.redirect.contains("session_expired") && Files.exists(storage.resolve(kBsecret)));

        // foreign folder still rejected before anything is written
        Db.exec("INSERT INTO folders (user_id, folder_name) VALUES (?,?)", B, "B-folder");
        int bFolder = (int) Db.count("SELECT MAX(folder_id) FROM folders");
        int filesBefore = filesOf(storage, A);
        r = upload(uc, A, "x.pdf", PDF, sess, "folderId", "" + bFolder);
        Db.check("T1.6/T2.1 upload into someone else's folder: rejected, no file written", err(r).contains("Thư mục không hợp lệ") && filesOf(storage, A) == filesBefore);

        // ============ DocumentController serving ============
        DocumentController dc = Http.init(new DocumentController());
        r = Http.call(dc, "GET", Http.params("action", "viewDoc", "docId", "" + docA), A, "STUDENT");
        Db.check("T2.1 viewDoc serves from the storage dir with nosniff and a fixed MIME type", java.util.Arrays.equals(r.bytes.toByteArray(), concat(PDF, b("v3")))
                && "nosniff".equals(r.headers.get("X-Content-Type-Options")) && "application/pdf".equals(r.contentType));
        r = Http.call(dc, "GET", Http.params("action", "downloadDoc", "docId", "" + docA), A, "STUDENT");
        Db.check("T2.1 downloadDoc: attachment, nosniff, name = title + extension from the key", r.bytes.size() > 0 && "nosniff".equals(r.headers.get("X-Content-Type-Options"))
                && String.valueOf(r.headers.get("Content-Disposition")).contains("attachment") && String.valueOf(r.headers.get("Content-Disposition")).endsWith("Lecture%201.pdf")
                && "application/octet-stream".equals(r.contentType));
        r = Http.call(dc, "GET", Http.params("action", "viewDoc", "docId", "" + docA), null, null);
        Db.check("T2.1 guest cannot view it while it is PRIVATE (the replace step set PRIVATE)", "ERROR 404".equals(r.summary()) && r.bytes.size() == 0);
        Db.exec("UPDATE documents SET sharing_permission='PUBLIC' WHERE document_id=?", docA);
        r = Http.call(dc, "GET", Http.params("action", "viewDoc", "docId", "" + docA), null, null);
        Db.check("T2.1 guest can view a PUBLIC document through the controller", r.bytes.size() > 0);

        Db.exec("INSERT INTO documents (user_id, folder_id, title, file_extension, cloud_storage_url, file_size_mb, ai_parsing_status, sharing_permission, share_link_token) VALUES (?,NULL,'legacy','pdf',?,1,'READY','PUBLIC',?)",
                A, "/AIStudyHub/uploads/" + A + "/legacy.pdf", java.util.UUID.randomUUID().toString());
        int legacyDoc = (int) Db.count("SELECT MAX(document_id) FROM documents");
        r = Http.call(dc, "GET", Http.params("action", "viewDoc", "docId", "" + legacyDoc), A, "STUDENT");
        Db.check("T2.1 an un-migrated legacy row answers 404 (no file leak, no crash)", "ERROR 404".equals(r.summary()) && r.bytes.size() == 0);
        r = Http.call(dc, "GET", Http.params("action", "downloadDoc", "docId", "" + legacyDoc), A, "STUDENT");
        Db.check("T2.1 download of a legacy row redirects to file_not_found", r.redirect != null && r.redirect.contains("error=file_not_found"));

        r = Http.call(dc, "GET", Http.params("action", "deleteDoc", "docId", "" + docA), A, "STUDENT");
        Db.check("T2.1 deleteDoc removes the row and then the stored file", Db.count("SELECT COUNT(*) FROM documents WHERE document_id=?", docA) == 0 && !Files.exists(storage.resolve(key3)));

        // ============ FolderController.deleteFolder ============
        Db.exec("INSERT INTO folders (user_id, folder_name) VALUES (?,?)", A, "A-tree");
        int tree = (int) Db.count("SELECT MAX(folder_id) FROM folders");
        Db.exec("INSERT INTO folders (user_id, parent_folder_id, folder_name) VALUES (?,?,?)", A, tree, "A-sub");
        int sub = (int) Db.count("SELECT MAX(folder_id) FROM folders");
        String kt = FileStorage.save(A, new java.io.ByteArrayInputStream(PDF), "pdf");
        String ks = FileStorage.save(A, new java.io.ByteArrayInputStream(PDF), "pdf");
        String ko = FileStorage.save(A, new java.io.ByteArrayInputStream(PDF), "pdf");
        insertDoc(A, tree, "in-tree", kt);
        insertDoc(A, sub, "in-sub", ks);
        insertDoc(A, null, "outside", ko);
        FolderController fc = Http.init(new FolderController());
        r = Http.call(fc, "POST", Http.params("action", "deleteFolder", "folderId", "" + tree), B, "STUDENT");
        Db.check("T2.1 B cannot delete A's folder tree: rows and files survive", Db.count("SELECT COUNT(*) FROM folders WHERE folder_id=?", tree) == 1 && Files.exists(storage.resolve(kt)) && Files.exists(storage.resolve(ks)));
        r = Http.call(fc, "POST", Http.params("action", "deleteFolder", "folderId", "" + tree), A, "STUDENT");
        // The recursive delete is a T-SQL `WITH cte AS (...) DELETE`, which H2 cannot run, so on this harness the DB delete fails.
        // Either way the rule must hold: files go only if the DB delete succeeded. (The success path needs SQL Server.)
        boolean folderGone = r.redirect != null && r.redirect.contains("folderSuccess=deleted");
        boolean treeFilesGone = !Files.exists(storage.resolve(kt)) && !Files.exists(storage.resolve(ks));
        Db.check("T2.1 deleteFolder: files are removed only when the DB delete succeeded (H2 cannot run the CTE delete; here it fails, so files must survive)",
                (folderGone ? treeFilesGone : !treeFilesGone && Db.count("SELECT COUNT(*) FROM folders WHERE folder_id=?", tree) == 1) && Files.exists(storage.resolve(ko)));

        // ============ account deletion removes the user's directory ============
        int D = Db.user("D", "STUDENT"), E = Db.user("E", "STUDENT"), ADM = Db.user("ADM", "ADMIN");
        String kd = FileStorage.save(D, new java.io.ByteArrayInputStream(PDF), "pdf");
        String ke = FileStorage.save(E, new java.io.ByteArrayInputStream(PDF), "pdf");
        insertDoc(D, null, "d-doc", kd);
        insertDoc(E, null, "e-doc", ke);
        AdminController ac = Http.init(new AdminController());
        r = Http.call(ac, "POST", Http.params("action", "deleteUser", "user_id", "" + D), ADM, "ADMIN");
        Db.check("T2.1 admin deleteUser removes the user's files, not other users'", Db.count("SELECT COUNT(*) FROM users WHERE user_id=?", D) == 0 && !Files.exists(storage.resolve(kd)) && !Files.exists(storage.resolve("" + D))
                && Files.exists(storage.resolve(ke)));
        UserController uctl = Http.init(new UserController());
        r = Http.call(uctl, "POST", Http.params("action", "deleteAccount"), E, "STUDENT");
        Db.check("T2.1 deleteAccount removes the user's own files", Db.count("SELECT COUNT(*) FROM users WHERE user_id=?", E) == 0 && !Files.exists(storage.resolve(ke)));

        // ============ AdminReportController.adminDeleteDocument ============
        String kr = FileStorage.save(A, new java.io.ByteArrayInputStream(PDF), "pdf");
        int rdoc = insertDoc(A, null, "reported", kr);
        AdminReportController arc = Http.init(new AdminReportController());
        r = Http.call(arc, "POST", Http.params("action", "adminDeleteDocument", "documentId", "" + rdoc), ADM, "ADMIN");
        Db.check("T2.1 adminDeleteDocument deletes the stored file after the DB row", Db.count("SELECT COUNT(*) FROM documents WHERE document_id=?", rdoc) == 0 && !Files.exists(storage.resolve(kr)));

        // ============ T2.2: migration ============
        Path legacy = tmp("aish-legacy");
        Path up = legacy.resolve("uploads");
        Files.createDirectories(up.resolve("" + A));
        Files.write(up.resolve(A + "/good.pdf"), PDF);
        Files.write(up.resolve(A + "/photo.png"), PNG);
        Files.write(up.resolve(A + "/fake.pdf"), HTML);
        Files.write(up.resolve(A + "/shell.jsp"), b("<% out.print(1); %>"));
        Files.write(up.resolve(A + "/orphan.jsp"), b("<% /* nobody points here */ %>"));
        Db.exec("DELETE FROM documents");
        Path secret = tmp("aish-secret").resolve("secret.pdf");
        Files.write(secret, PDF);
        String alreadyKey = FileStorage.save(A, new java.io.ByteArrayInputStream(PDF), "pdf");
        int mGood = insertDoc(A, null, "good", "/AIStudyHub/uploads/" + A + "/good.pdf", "pdf");
        int mPng = insertDoc(A, null, "photo", "/AIStudyHub/uploads/" + A + "/photo.png", "png");
        int mFake = insertDoc(A, null, "fake", "/AIStudyHub/uploads/" + A + "/fake.pdf", "pdf");
        int mJsp = insertDoc(A, null, "shell", "/AIStudyHub/uploads/" + A + "/shell.jsp", "jsp");
        int mMissing = insertDoc(A, null, "gone", "/AIStudyHub/uploads/" + A + "/gone.pdf", "pdf");
        int mTrav = insertDoc(A, null, "trav", "/AIStudyHub/uploads/../../" + secret.getParent().getFileName() + "/secret.pdf", "pdf");
        int mOther = insertDoc(A, null, "weird", "/somewhere/else.pdf", "pdf");
        int mDone = insertDoc(A, null, "done", alreadyKey, "pdf");
        long total = Db.count("SELECT COUNT(*) FROM documents");
        Path migStorage = tmp("aish-migstorage");

        StorageMigration.Report rep;
        try (Connection c = Utils.DBUtils.getConnection()) {
            rep = StorageMigration.migrate(c, legacy, migStorage, false, false);
        }
        Db.check("T2.2 dry run: counts add up to SELECT COUNT(*) FROM documents", rep.total == total && rep.sum() == total);
        Db.check("T2.2 dry run: 2 would migrate, 1 already, 1 missing, 2 bad (ext/content), 2 unsafe",
                rep.count(StorageMigration.Status.MIGRATED) == 2 && rep.count(StorageMigration.Status.ALREADY_MIGRATED) == 1 && rep.count(StorageMigration.Status.MISSING_FILE) == 1
                && rep.count(StorageMigration.Status.BAD_EXTENSION) == 1 && rep.count(StorageMigration.Status.BAD_CONTENT) == 1 && rep.count(StorageMigration.Status.UNSAFE_PATH) == 2);
        Db.check("T2.2 dry run writes nothing: no files in the new storage, rows unchanged",
                !Files.exists(migStorage.resolve("" + A)) && Db.count("SELECT COUNT(*) FROM documents WHERE cloud_storage_url LIKE '/AIStudyHub/uploads/%'") == 6);
        Db.check("T2.2 the report counts skipped file types by extension (jsp here), so the user can see what becomes unreachable", rep.skippedExtensions.size() == 1 && Integer.valueOf(1).equals(rep.skippedExtensions.get("jsp")));
        Db.check("T2.2 dry run lists the orphan .jsp under the legacy folder and the problem rows", rep.orphanFiles.size() == 1 && rep.orphanFiles.get(0).endsWith("orphan.jsp") && rep.problems.size() >= 5);

        try (Connection c = Utils.DBUtils.getConnection()) {
            rep = StorageMigration.migrate(c, legacy, migStorage, true, false);
        }
        String goodKey = Db.str("SELECT cloud_storage_url FROM documents WHERE document_id=" + mGood);
        String pngKey = Db.str("SELECT cloud_storage_url FROM documents WHERE document_id=" + mPng);
        Db.check("T2.2 apply: rows now hold valid keys and the files were copied (content identical)", FileStorage.isValidKey(goodKey) && FileStorage.isValidKey(pngKey)
                && java.util.Arrays.equals(Files.readAllBytes(FileStorage.resolve(migStorage, goodKey)), PDF) && java.util.Arrays.equals(Files.readAllBytes(FileStorage.resolve(migStorage, pngKey)), PNG)
                && goodKey.startsWith(A + "/") && rep.count(StorageMigration.Status.MIGRATED) == 2 && rep.sum() == total);
        Db.check("T2.2 apply: nothing dangerous was copied and skipped rows are untouched",
                Db.str("SELECT cloud_storage_url FROM documents WHERE document_id=" + mJsp).endsWith("shell.jsp") && Db.str("SELECT cloud_storage_url FROM documents WHERE document_id=" + mFake).endsWith("fake.pdf")
                && Db.str("SELECT cloud_storage_url FROM documents WHERE document_id=" + mTrav).contains("..") && Db.str("SELECT cloud_storage_url FROM documents WHERE document_id=" + mOther).equals("/somewhere/else.pdf")
                && Db.str("SELECT cloud_storage_url FROM documents WHERE document_id=" + mMissing).endsWith("gone.pdf") && filesOf(migStorage, A) == 2);
        Db.check("T2.2 apply copies, it does not move: legacy files are still there", Files.exists(up.resolve(A + "/good.pdf")) && Files.exists(up.resolve(A + "/photo.png")));
        try (Connection c = Utils.DBUtils.getConnection()) {
            rep = StorageMigration.migrate(c, legacy, migStorage, true, false);
        }
        Db.check("T2.2 re-running is safe: nothing migrated twice, no extra files", rep.count(StorageMigration.Status.MIGRATED) == 0 && rep.count(StorageMigration.Status.ALREADY_MIGRATED) == 3 && filesOf(migStorage, A) == 2
                && Db.str("SELECT cloud_storage_url FROM documents WHERE document_id=" + mGood).equals(goodKey));
        // --delete-old on a fresh legacy row
        Files.write(up.resolve(A + "/later.pdf"), PDF);
        int mLater = insertDoc(A, null, "later", "/AIStudyHub/uploads/" + A + "/later.pdf", "pdf");
        try (Connection c = Utils.DBUtils.getConnection()) {
            rep = StorageMigration.migrate(c, legacy, migStorage, true, true);
        }
        Db.check("T2.2 --delete-old removes the legacy copy only after migrating that row", rep.legacyFilesDeleted == 1 && !Files.exists(up.resolve(A + "/later.pdf"))
                && FileStorage.isValidKey(Db.str("SELECT cloud_storage_url FROM documents WHERE document_id=" + mLater)) && Files.exists(up.resolve(A + "/good.pdf")));
        Db.check("T2.2 legacyPathOf blocks traversal out of uploads/", legacyOf(up, "/AIStudyHub/uploads/../x.pdf") == null && legacyOf(up, "/AIStudyHub/uploads/") == null
                && legacyOf(up, "/x.pdf") == null && legacyOf(up, "/AIStudyHub/uploads/1/a.pdf") != null);

        Db.done();
    }

    static Path legacyOf(Path up, String url) throws Exception {
        java.lang.reflect.Method m = StorageMigration.class.getDeclaredMethod("legacyPathOf", Path.class, String.class);
        m.setAccessible(true);
        return (Path) m.invoke(null, up.toAbsolutePath().normalize(), url);
    }

    static int insertDoc(int owner, Integer folder, String title, String key) throws Exception {
        return insertDoc(owner, folder, title, key, "pdf");
    }

    static int insertDoc(int owner, Integer folder, String title, String key, String ext) throws Exception {
        Db.exec("INSERT INTO documents (user_id, folder_id, title, file_extension, cloud_storage_url, file_size_mb, ai_parsing_status, sharing_permission, share_link_token) VALUES (?,?,?,?,?,1,'READY','PRIVATE',?)",
                owner, folder, title, ext, key, java.util.UUID.randomUUID().toString());
        return (int) Db.count("SELECT MAX(document_id) FROM documents");
    }

    static byte[] readAll(java.io.InputStream in) throws Exception {
        java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            o.write(buf, 0, n);
        }
        return o.toByteArray();
    }

    static byte[] concat(byte[] a, byte[] b) {
        byte[] out = java.util.Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
