import Controller.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

/** Controller-level checks for T1.2-T1.6 and T1.8 (real controllers + DAOs against H2, no Tomcat). */
public class T18Test {

    static final java.util.Map<String, String> KEYS = new java.util.HashMap<>();

    /** Inserts a document whose file lives in the storage dir (T2.1): key = <owner>/<uuid>.txt. */
    static int doc(int owner, String title, String perm, boolean flagged, Integer folder) throws Exception {
        String key = Utils.FileStorage.save(owner, new java.io.ByteArrayInputStream(("SECRET-" + title).getBytes(StandardCharsets.UTF_8)), "txt");
        KEYS.put(owner + "/" + title, key);
        Db.exec("INSERT INTO documents (user_id, folder_id, title, file_extension, cloud_storage_url, file_size_mb, ai_parsing_status, sharing_permission, is_flagged, share_link_token) VALUES (?,?,?,?,?,1,'READY',?,?,?)",
                owner, folder, title, "txt", key, perm, flagged, java.util.UUID.randomUUID().toString());
        return (int) Db.count("SELECT MAX(document_id) FROM documents");
    }

    static boolean fileExists(int owner, String title) {
        return Utils.FileStorage.fileOrNull(KEYS.get(owner + "/" + title)) != null;
    }

    static int folder(int owner, String name) throws Exception {
        Db.exec("INSERT INTO folders (user_id, folder_name) VALUES (?,?)", owner, name);
        return (int) Db.count("SELECT MAX(folder_id) FROM folders");
    }

    static String S(Http.Result r) {
        return r.summary();
    }

    public static void main(String[] a) throws Exception {
        Db.init();
        System.setProperty("STORAGE_DIR", Files.createTempDirectory("aish-storage").toString());
        int A = Db.user("A", "STUDENT"), B = Db.user("B", "STUDENT"), F = Db.user("F", "STUDENT"), C = Db.user("C", "STUDENT"), ADM = Db.user("ADM", "ADMIN");
        Db.exec("INSERT INTO friendships (requester_id, addressee_id, status) VALUES (?,?, 'ACCEPTED')", F, B);   // F is B's friend

        int bPriv = doc(B, "bpriv", "PRIVATE", false, null);
        int bFriends = doc(B, "bfriends", "FRIENDS_ONLY", false, null);
        int bPub = doc(B, "bpub", "PUBLIC", false, null);
        int bFlag = doc(B, "bflag", "PUBLIC", true, null);
        int bFolder = folder(B, "B-folder");
        int aFolder = folder(A, "A-folder");
        int aDoc = doc(A, "adoc", "PRIVATE", false, null);

        DocumentController dc = Http.init(new DocumentController());
        Http.Result r;

        // ================= T1.2: edit/update/delete/permission =================
        r = Http.call(dc, "GET", Http.params("action", "editDoc", "docId", "" + bPriv), A, "STUDENT");
        Db.check("T1.2 A cannot open B's edit form (not_found redirect, no forward)", S(r).contains("error=not_found") && r.forward == null);
        r = Http.call(dc, "POST", Http.params("action", "updateDoc", "docId", "" + bPriv, "title", "pwned", "sharingPermission", "PUBLIC"), A, "STUDENT");
        Db.check("T1.2 A cannot updateDoc B's doc", S(r).contains("error=not_found")
                && Db.count("SELECT COUNT(*) FROM documents WHERE document_id=? AND title='bpriv' AND sharing_permission='PRIVATE'", bPriv) == 1);
        r = Http.call(dc, "GET", Http.params("action", "deleteDoc", "docId", "" + bPriv), A, "STUDENT");
        Db.check("T1.2 A cannot deleteDoc B's doc: row AND file survive", S(r).contains("error=not_found")
                && Db.count("SELECT COUNT(*) FROM documents WHERE document_id=?", bPriv) == 1 && fileExists(B, "bpriv"));
        r = Http.call(dc, "POST", Http.params("action", "updatePermission", "docId", "" + bPriv, "sharingPermission", "PUBLIC"), A, "STUDENT");
        Db.check("T1.2 A cannot flip B's permission", S(r).contains("error=not_found")
                && Db.count("SELECT COUNT(*) FROM documents WHERE document_id=? AND sharing_permission='PRIVATE'", bPriv) == 1);
        r = Http.call(dc, "GET", Http.params("action", "deleteDoc", "docId", "" + bPriv), ADM, "ADMIN");
        Db.check("T1.2 even an admin cannot use DocumentController.deleteDoc on someone's doc", S(r).contains("error=not_found") && fileExists(B, "bpriv"));
        r = Http.call(dc, "POST", Http.params("action", "updateDoc", "docId", "" + aDoc, "title", "adoc-renamed", "sharingPermission", "PRIVATE", "folderId", "" + aFolder), A, "STUDENT");
        Db.check("T1.2 A can update own doc into own folder", S(r).contains("updateSuccess=1")
                && Db.count("SELECT COUNT(*) FROM documents WHERE document_id=? AND title='adoc-renamed' AND folder_id=?", aDoc, aFolder) == 1);

        // ================= T1.6: foreign folder ids =================
        r = Http.call(dc, "POST", Http.params("action", "updateDoc", "docId", "" + aDoc, "title", "adoc-renamed", "sharingPermission", "PRIVATE", "folderId", "" + bFolder), A, "STUDENT");
        Db.check("T1.6 updateDoc into B's folder is rejected and nothing moves", r.forward != null && r.forward.contains("action=editDoc")
                && String.valueOf(r.requestAttrs.get("errorMessage")).contains("Thư mục không hợp lệ")
                && Db.count("SELECT COUNT(*) FROM documents WHERE document_id=? AND folder_id=?", aDoc, aFolder) == 1);

        // ================= T1.3: view / download / bookmark =================
        r = Http.call(dc, "GET", Http.params("action", "viewDoc", "docId", "" + bFriends), C, "STUDENT");
        Db.check("T1.3 stranger C cannot viewDoc FRIENDS_ONLY (404, no content)", S(r).equals("ERROR 404") && r.bytes.size() == 0);
        r = Http.call(dc, "GET", Http.params("action", "viewDoc", "docId", "" + bFriends), F, "STUDENT");
        Db.check("T1.3 accepted friend F can viewDoc FRIENDS_ONLY", new String(r.bytes.toByteArray(), "UTF-8").equals("SECRET-bfriends"));
        r = Http.call(dc, "GET", Http.params("action", "viewDoc", "docId", "" + bFriends), null, null);
        Db.check("T1.3 guest cannot viewDoc FRIENDS_ONLY", S(r).equals("ERROR 404"));
        r = Http.call(dc, "GET", Http.params("action", "viewDoc", "docId", "" + bPub), null, null);
        Db.check("T1.3 guest can viewDoc PUBLIC", new String(r.bytes.toByteArray(), "UTF-8").equals("SECRET-bpub"));
        r = Http.call(dc, "GET", Http.params("action", "viewDoc", "docId", "" + bPriv), C, "STUDENT");
        Db.check("T1.3 stranger cannot viewDoc PRIVATE", S(r).equals("ERROR 404"));
        r = Http.call(dc, "GET", Http.params("action", "viewDoc", "docId", "" + bPriv), B, "STUDENT");
        Db.check("T1.3 owner can viewDoc PRIVATE", new String(r.bytes.toByteArray(), "UTF-8").equals("SECRET-bpriv"));
        r = Http.call(dc, "GET", Http.params("action", "viewDoc", "docId", "" + bPriv), ADM, "ADMIN");
        Db.check("T1.3 admin can viewDoc PRIVATE (moderation)", new String(r.bytes.toByteArray(), "UTF-8").equals("SECRET-bpriv"));
        r = Http.call(dc, "GET", Http.params("action", "viewDoc", "docId", "" + bFlag), C, "STUDENT");
        Db.check("T1.3 flagged PUBLIC doc hidden from strangers", S(r).equals("ERROR 404"));
        r = Http.call(dc, "GET", Http.params("action", "viewDoc", "docId", "99999"), C, "STUDENT");
        Db.check("T1.3 missing doc looks identical to a forbidden one (404)", S(r).equals("ERROR 404"));

        long dlBefore = Db.count("SELECT download_count FROM documents WHERE document_id=?", bFriends);
        r = Http.call(dc, "GET", Http.params("action", "downloadDoc", "docId", "" + bFriends), C, "STUDENT");
        Db.check("T1.3 stranger C cannot download FRIENDS_ONLY", S(r).equals("ERROR 404") && r.bytes.size() == 0
                && Db.count("SELECT download_count FROM documents WHERE document_id=?", bFriends) == dlBefore);
        r = Http.call(dc, "GET", Http.params("action", "downloadDoc", "docId", "" + bFriends), F, "STUDENT");
        Db.check("T1.3 friend F can download FRIENDS_ONLY, counter incremented", new String(r.bytes.toByteArray(), "UTF-8").equals("SECRET-bfriends")
                && Db.count("SELECT download_count FROM documents WHERE document_id=?", bFriends) == dlBefore + 1);
        r = Http.call(dc, "GET", Http.params("action", "downloadDoc", "docId", "" + bPriv), A, "STUDENT");
        Db.check("T1.3 A cannot download B's PRIVATE doc", S(r).equals("ERROR 404") && r.bytes.size() == 0);

        r = Http.call(dc, "GET", Http.params("action", "viewPublicPage", "docId", "" + bFriends), C, "STUDENT");
        Db.check("T1.3 viewPublicPage FRIENDS_ONLY for stranger -> not_found (old code allowed any logged-in user)", S(r).contains("error=not_found") && r.forward == null);
        r = Http.call(dc, "GET", Http.params("action", "viewPublicPage", "docId", "" + bFriends), F, "STUDENT");
        Db.check("T1.3 viewPublicPage FRIENDS_ONLY for friend -> page", "/publicDocument_view.jsp".equals(r.forward));
        r = Http.call(dc, "GET", Http.params("action", "viewPublicPage", "docId", "" + bPub), null, null);
        Db.check("T1.3 viewPublicPage PUBLIC for guest -> page", "/publicDocument_view.jsp".equals(r.forward));
        r = Http.call(dc, "GET", Http.params("action", "viewPublicPage", "docId", "" + bPriv), null, null);
        Db.check("T1.3 viewPublicPage PRIVATE for guest -> not_found", S(r).contains("error=not_found"));

        r = Http.call(dc, "POST", Http.params("action", "toggleBookmark", "docId", "" + bPriv), C, "STUDENT");
        Db.check("T1.3 cannot bookmark an invisible doc (404 JSON, no row, counter 0)", S(r).contains("status=404") || (r.status != null && r.status == 404));
        Db.check("T1.3 ...no bookmark row and bookmark_count untouched", Db.count("SELECT COUNT(*) FROM bookmarks WHERE document_id=?", bPriv) == 0
                && Db.count("SELECT bookmark_count FROM documents WHERE document_id=?", bPriv) == 0);
        r = Http.call(dc, "POST", Http.params("action", "toggleBookmark", "docId", "" + bFriends), C, "STUDENT");
        Db.check("T1.3 cannot bookmark FRIENDS_ONLY as a stranger", Db.count("SELECT COUNT(*) FROM bookmarks WHERE document_id=?", bFriends) == 0);
        r = Http.call(dc, "POST", Http.params("action", "toggleBookmark", "docId", "" + bPub), C, "STUDENT");
        Db.check("T1.3 can bookmark a PUBLIC doc", r.body.contains("\"isBookmarked\": true") && Db.count("SELECT COUNT(*) FROM bookmarks WHERE document_id=? AND user_id=?", bPub, C) == 1);

        // ReportController (T1.3 + T1.8)
        ReportController rc = new ReportController();
        r = Http.call(rc, "POST", Http.params("action", "createReport", "documentId", "" + bPriv, "reasonCode", "SPAM", "details", "x"), C, "STUDENT");
        Db.check("T1.3 cannot report a doc you cannot see", S(r).contains("error=not_found") && Db.count("SELECT COUNT(*) FROM document_reports") == 0);
        r = Http.call(rc, "GET", Http.params("action", "report", "documentId", "" + bPriv), C, "STUDENT");
        Db.check("T1.3 report form for an invisible doc -> not_found", S(r).contains("error=not_found") && r.forward == null);
        r = Http.call(rc, "POST", Http.params("action", "createReport", "documentId", "" + bPub, "reasonCode", "SPAM", "details", "x"), C, "STUDENT");
        Db.check("T1.3 can report a visible doc", Db.count("SELECT COUNT(*) FROM document_reports WHERE document_id=? AND reporter_id=?", bPub, C) == 1);
        double score1 = Db.count("SELECT CAST(total_report_score AS INT) FROM documents WHERE document_id=?", bPub);
        r = Http.call(rc, "POST", Http.params("action", "createReport", "documentId", "" + bPub, "reasonCode", "SPAM", "details", "again"), C, "STUDENT");
        Db.check("T1.8 a second PENDING report by the same user is refused (cannot inflate the score)", S(r).contains("create_report_failed")
                && Db.count("SELECT COUNT(*) FROM document_reports WHERE document_id=? AND reporter_id=?", bPub, C) == 1
                && Db.count("SELECT CAST(total_report_score AS INT) FROM documents WHERE document_id=?", bPub) == (long) score1);

        // ================= T1.4: chat =================
        Db.exec("INSERT INTO chat_sessions (session_name, user_id) VALUES ('B secret chat', ?)", B);
        int sB = (int) Db.count("SELECT MAX(session_id) FROM chat_sessions");
        Db.exec("INSERT INTO chat_messages (session_id, sender, message_content) VALUES (?, 'USER', 'b-private-message')", sB);
        ChatBotController cb = Http.init(new ChatBotController());
        r = Http.call(cb, "POST", Http.params("message", "hello", "sessionId", "" + sB), A, "STUDENT");
        Db.check("T1.4 A posting into B's chat session -> 404 and the quota/messages are untouched", r.status != null && r.status == 404
                && Db.count("SELECT COUNT(*) FROM chat_messages WHERE session_id=?", sB) == 1
                && Db.count("SELECT ai_prompts_today FROM users WHERE user_id=?", A) == 0);
        r = Http.call(cb, "POST", Http.params("message", "hello", "sessionId", "99999"), A, "STUDENT");
        Db.check("T1.4 a non-existent session id answers the same way", r.status != null && r.status == 404);
        r = Http.call(cb, "POST", Http.params("message", "hello", "sessionId", "" + sB), null, null);
        Db.check("T1.4 anonymous -> 401", r.status != null && r.status == 401);

        SessionController sc = new SessionController();
        r = Http.call(sc, "POST", Http.params("action", "updateSessionName", "sessionId", "" + sB, "sessionNewName", "pwned"), A, "STUDENT");
        Db.check("T1.4 A cannot rename B's session (404 JSON)", r.status != null && r.status == 404 && r.body.contains("\"success\": false")
                && Db.count("SELECT COUNT(*) FROM chat_sessions WHERE session_id=? AND session_name='B secret chat'", sB) == 1);
        r = Http.call(sc, "POST", Http.params("action", "pinSession", "sessionId", "" + sB, "isPinned", "true"), A, "STUDENT");
        Db.check("T1.4 A cannot pin B's session", r.status != null && r.status == 404 && Db.count("SELECT COUNT(*) FROM chat_sessions WHERE session_id=? AND is_pinned=TRUE", sB) == 0);
        int mB = (int) Db.count("SELECT MIN(message_id) FROM chat_messages WHERE session_id=?", sB);
        r = Http.call(sc, "POST", Http.params("action", "deleteMessagesFrom", "sessionId", "" + sB, "messageId", "" + mB), A, "STUDENT");
        Db.check("T1.4 A cannot delete B's messages", r.status != null && r.status == 404 && Db.count("SELECT COUNT(*) FROM chat_messages WHERE session_id=?", sB) == 1);
        Db.exec("INSERT INTO chat_sessions (session_name, user_id) VALUES ('A \"quoted\" chat', ?)", A);
        r = Http.call(sc, "GET", Http.params("action", "findSessionByName", "sessionName", "chat"), A, "STUDENT");
        com.google.gson.JsonArray arr = new com.google.gson.JsonParser().parse(r.body).getAsJsonArray();
        Db.check("T1.4 search returns only A's session, JSON is valid and the quotes round-trip", arr.size() == 1
                && arr.get(0).getAsJsonObject().get("sessionName").getAsString().equals("A \"quoted\" chat"));
        r = Http.call(sc, "GET", Http.params("action", "findSessionByName", "sessionName", "secret"), A, "STUDENT");
        Db.check("T1.4 searching B's session name finds nothing", r.body.trim().equals("[]"));

        // ================= T1.5: friendships through the controller =================
        FriendController fc = new FriendController();
        r = Http.call(fc, "POST", Http.params("action", "createFriendship", "addresseeId", "" + C), A, "STUDENT");
        Db.check("T1.5 A sends C a request", S(r).contains("success=request_sent"));
        r = Http.call(fc, "GET", Http.params("action", "updateFriendshipStatus", "targetUserId", "" + C, "status", "ACCEPTED"), A, "STUDENT");
        Db.check("T1.5 A cannot accept their own request", S(r).contains("error=update_failed") && Db.count("SELECT COUNT(*) FROM friendships WHERE requester_id=? AND addressee_id=? AND status='PENDING'", A, C) == 1);
        r = Http.call(fc, "POST", Http.params("action", "createFriendship", "addresseeId", "" + A), C, "STUDENT");
        Db.check("T1.5 C cannot open a reverse request while A->C exists", S(r).contains("error=already_exists") && Db.count("SELECT COUNT(*) FROM friendships WHERE (requester_id=? AND addressee_id=?) OR (requester_id=? AND addressee_id=?)", A, C, C, A) == 1);
        r = Http.call(fc, "GET", Http.params("action", "updateFriendshipStatus", "targetUserId", "" + A, "status", "ACCEPTED"), C, "STUDENT");
        Db.check("T1.5 C (addressee) accepts", S(r).contains("success=status_updated") && Db.count("SELECT COUNT(*) FROM friendships WHERE status='ACCEPTED' AND requester_id=? AND addressee_id=?", A, C) == 1);
        r = Http.call(fc, "GET", Http.params("action", "updateFriendshipStatus", "targetUserId", "" + C, "status", "BLOCKED"), A, "STUDENT");
        Db.check("T1.5 A blocks C (from ACCEPTED)", S(r).contains("blockedList") && Db.count("SELECT COUNT(*) FROM friendships WHERE status='BLOCKED' AND blocker_id=?", A) == 1);
        r = Http.call(fc, "GET", Http.params("action", "updateFriendshipStatus", "targetUserId", "" + A, "status", "ACCEPTED"), C, "STUDENT");
        Db.check("T1.5 blocked C cannot flip it back to ACCEPTED", S(r).contains("error=update_failed") && Db.count("SELECT COUNT(*) FROM friendships WHERE status='BLOCKED'") == 1);
        r = Http.call(fc, "GET", Http.params("action", "deleteFriendship", "targetUserId", "" + A, "returnPath", "friendList"), C, "STUDENT");
        Db.check("T1.5 blocked C cannot unblock by deleting", S(r).contains("error=not_authorized") && Db.count("SELECT COUNT(*) FROM friendships WHERE status='BLOCKED'") == 1);
        r = Http.call(fc, "GET", Http.params("action", "deleteFriendship", "targetUserId", "" + C, "returnPath", "evil&x=1"), A, "STUDENT");
        Db.check("T1.5 returnPath is whitelisted (unknown value falls back to friendList) and A can unblock", S(r).contains("action=friendList&success=deleted") && !S(r).contains("evil"));

        // ================= T1.6: FolderController =================
        FolderController fo = new FolderController();
        r = Http.call(fo, "POST", Http.params("action", "createFolder", "folderName", "sneaky", "currentFolderId", "" + bFolder), A, "STUDENT");
        Db.check("T1.6 cannot create a folder under B's folder", S(r).contains("error=invalid_folder") && Db.count("SELECT COUNT(*) FROM folders WHERE folder_name='sneaky'") == 0);
        r = Http.call(fo, "POST", Http.params("action", "createFolder", "folderName", "mine", "currentFolderId", "" + aFolder), A, "STUDENT");
        Db.check("T1.6 can create a folder under own folder", Db.count("SELECT COUNT(*) FROM folders WHERE folder_name='mine' AND parent_folder_id=? AND user_id=?", aFolder, A) == 1);
        r = Http.call(fo, "GET", Http.params("action", "viewFolder", "folderId", "" + bFolder), A, "STUDENT");
        Db.check("T1.6 cannot view B's folder", S(r).contains("error=not_found") && r.forward == null);
        r = Http.call(fo, "GET", Http.params("action", "editFolder", "folderId", "" + bFolder), A, "STUDENT");
        Db.check("T1.6 cannot open B's folder edit page", S(r).contains("error=not_found") && r.forward == null);
        r = Http.call(fo, "POST", Http.params("action", "updateFolder", "folderId", "" + aFolder, "folderName", "A-folder", "sharingPermission", "PRIVATE", "parentFolderId", "" + bFolder), A, "STUDENT");
        Db.check("T1.6 cannot re-parent own folder under B's folder", S(r).contains("error=invalid_folder") && Db.count("SELECT COUNT(*) FROM folders WHERE folder_id=? AND parent_folder_id IS NULL", aFolder) == 1);
        r = Http.call(fo, "POST", Http.params("action", "updateFolder", "folderId", "" + aFolder, "folderName", "A-folder", "sharingPermission", "PRIVATE", "parentFolderId", "" + aFolder), A, "STUDENT");
        Db.check("T1.6 cannot make a folder its own parent", S(r).contains("error=invalid_folder"));

        // ================= T1.8 sweep =================
        UserController uc = new UserController();
        Db.exec("UPDATE users SET password_hash=? WHERE user_id=?", Utils.PasswordUtil.hashPassword("Str0ng!pass"), A);
        r = Http.call(uc, "POST", Http.params("action", "updateProfile", "username", "A", "email", "not-an-email", "currentPassword", "Str0ng!pass", "newPassword", ""), A, "STUDENT");
        Db.check("T1.8 updateProfile rejects an invalid email", S(r).contains("error=invalid_input") && Db.count("SELECT COUNT(*) FROM users WHERE user_id=? AND email='A@x.com'", A) == 1);
        r = Http.call(uc, "POST", Http.params("action", "updateProfile", "username", "A", "email", "a2@x.com", "currentPassword", "wrong", "newPassword", ""), A, "STUDENT");
        Db.check("T1.8 updateProfile still requires the current password", S(r).contains("error=wrong_password"));

        AdminReportController ar = Http.init(new AdminReportController());
        Db.exec("INSERT INTO document_reports (document_id, reporter_id, reason_code, status) VALUES (?,?, 'SPAM', 'PENDING')", bFriends, C);
        int rep = (int) Db.count("SELECT MAX(report_id) FROM document_reports");
        r = Http.call(ar, "POST", Http.params("action", "adminUpdateReportStatus", "reportId", "" + rep, "documentId", "" + bFriends, "status", "HACKED"), ADM, "ADMIN");
        Db.check("T1.8 adminUpdateReportStatus rejects unknown status", S(r).contains("error=invalid_input") && Db.count("SELECT COUNT(*) FROM document_reports WHERE report_id=? AND status='PENDING'", rep) == 1);
        r = Http.call(ar, "POST", Http.params("action", "adminDeleteDocument", "documentId", "" + bFriends), C, "STUDENT");
        Db.check("T1.1/T1.8 student cannot call adminDeleteDocument (defence in depth, 403)", S(r).equals("ERROR 403") && Db.count("SELECT COUNT(*) FROM documents WHERE document_id=?", bFriends) == 1 && fileExists(B, "bfriends"));
        r = Http.call(ar, "POST", Http.params("action", "adminDeleteDocument", "documentId", "" + bFriends), ADM, "ADMIN");
        Db.check("T1.8 admin deleteDocument still works and removes the physical file too", Db.count("SELECT COUNT(*) FROM documents WHERE document_id=?", bFriends) == 0
                && !fileExists(B, "bfriends"));

        // owner delete happens last: DB row gone first, then file
        r = Http.call(dc, "GET", Http.params("action", "deleteDoc", "docId", "" + aDoc), A, "STUDENT");
        Db.check("T1.2 owner deleteDoc works: row removed and file removed", S(r).contains("updateSuccess=1")
                && Db.count("SELECT COUNT(*) FROM documents WHERE document_id=?", aDoc) == 0 && !fileExists(A, "adoc"));
        Db.done();
    }
}
