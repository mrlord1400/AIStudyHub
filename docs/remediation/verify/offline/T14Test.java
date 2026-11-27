import Model.DAO.ChatMessageDAO;
import Model.DAO.ChatSessionDAO;
import Model.DAO.FolderDAO;
import Model.DTO.ChatSession;
import java.util.List;

public class T14Test {

    static int session(int owner, String name) throws Exception {
        Db.exec("INSERT INTO chat_sessions (session_name, user_id) VALUES (?,?)", name, owner);
        return (int) Db.count("SELECT MAX(session_id) FROM chat_sessions");
    }

    static int message(int sessionId, String text) throws Exception {
        Db.exec("INSERT INTO chat_messages (session_id, sender, message_content) VALUES (?, 'USER', ?)", sessionId, text);
        return (int) Db.count("SELECT MAX(message_id) FROM chat_messages");
    }

    public static void main(String[] a) throws Exception {
        Db.init();
        int A = Db.user("A", "STUDENT"), B = Db.user("B", "STUDENT");
        int sA = session(A, "alpha notes"), sB = session(B, "bravo secrets");
        int mB1 = message(sB, "b-1"), mB2 = message(sB, "b-2");
        int mA1 = message(sA, "a-1");
        ChatSessionDAO sdao = new ChatSessionDAO();
        ChatMessageDAO mdao = new ChatMessageDAO();

        Db.check("A cannot rename B's session", !sdao.updateSessionNameForOwner(sB, A, "pwned"));
        Db.check("B's session name unchanged", Db.count("SELECT COUNT(*) FROM chat_sessions WHERE session_id=? AND session_name='bravo secrets'", sB) == 1);
        Db.check("A cannot pin B's session", !sdao.pinSessionForOwner(sB, A, true));
        Db.check("B's session still unpinned", Db.count("SELECT COUNT(*) FROM chat_sessions WHERE session_id=? AND is_pinned=FALSE", sB) == 1);
        Db.check("A cannot delete B's messages", !mdao.deleteMessagesFromId(mB1, sB, A));
        Db.check("B's messages intact", Db.count("SELECT COUNT(*) FROM chat_messages WHERE session_id=?", sB) == 2);
        mdao.deleteMessagesFromId(mB1, sA, A);   // only ever touches A's own session rows
        Db.check("pairing B's message id with A's session id never touches B's rows", Db.count("SELECT COUNT(*) FROM chat_messages WHERE session_id=?", sB) == 2);
        mA1 = message(sA, "a-1 again");

        List<ChatSession> found = sdao.findSessionByName(A, "a");
        boolean onlyMine = !found.isEmpty();
        for (ChatSession cs : found) {
            onlyMine &= cs.getUserId() == A;
        }
        Db.check("search by 'a' returns only A's sessions", onlyMine);
        Db.check("search for B's session name returns nothing for A", sdao.findSessionByName(A, "bravo").isEmpty());
        Db.check("search by '' returns only A's sessions", sdao.findSessionByName(A, "").size() == 1);

        Db.check("A can rename own session", sdao.updateSessionNameForOwner(sA, A, "renamed"));
        Db.check("A can pin own session", sdao.pinSessionForOwner(sA, A, true));
        Db.check("A can delete own messages", mdao.deleteMessagesFromId(mA1, sA, A));
        Db.check("A's messages gone", Db.count("SELECT COUNT(*) FROM chat_messages WHERE session_id=?", sA) == 0);

        int fA = folder(A, "A-folder");
        FolderDAO fdao = new FolderDAO();
        Db.check("folder isOwnedBy owner", fdao.isOwnedBy(fA, A));
        Db.check("folder not owned by other user", !fdao.isOwnedBy(fA, B));
        Db.check("missing folder is not owned", !fdao.isOwnedBy(99999, A));
        Db.done();
    }

    static int folder(int owner, String name) throws Exception {
        Db.exec("INSERT INTO folders (user_id, folder_name) VALUES (?,?)", owner, name);
        return (int) Db.count("SELECT MAX(folder_id) FROM folders");
    }
}
