import Model.DTO.Document;
import Model.Service.DocumentAccess;

public class T13Test {

    static Document doc(int owner, String perm, boolean flagged) {
        Document d = new Document();
        d.setUserId(owner);
        d.setSharingPermission(perm);
        d.setFlagged(flagged);
        return d;
    }

    public static void main(String[] a) throws Exception {
        Db.init();
        int owner = Db.user("owner", "STUDENT");
        int friend = Db.user("friend", "STUDENT");      // ACCEPTED, owner is requester
        int friend2 = Db.user("friend2", "STUDENT");    // ACCEPTED, owner is addressee (reverse direction)
        int pending = Db.user("pending", "STUDENT");    // PENDING only
        int blocked = Db.user("blocked", "STUDENT");    // BLOCKED
        int stranger = Db.user("stranger", "STUDENT");
        Db.exec("INSERT INTO friendships (requester_id, addressee_id, status) VALUES (?,?, 'ACCEPTED')", owner, friend);
        Db.exec("INSERT INTO friendships (requester_id, addressee_id, status) VALUES (?,?, 'ACCEPTED')", friend2, owner);
        Db.exec("INSERT INTO friendships (requester_id, addressee_id, status) VALUES (?,?, 'PENDING')", pending, owner);
        Db.exec("INSERT INTO friendships (requester_id, addressee_id, status, blocker_id) VALUES (?,?, 'BLOCKED', ?)", owner, blocked, owner);

        DocumentAccess acc = new DocumentAccess();
        Document pub = doc(owner, "PUBLIC", false), fo = doc(owner, "FRIENDS_ONLY", false),
                priv = doc(owner, "PRIVATE", false), flaggedPub = doc(owner, "PUBLIC", true),
                nullPerm = doc(owner, null, false);

        Db.check("guest sees PUBLIC", acc.canView(pub, null));
        Db.check("guest cannot see FRIENDS_ONLY", !acc.canView(fo, null));
        Db.check("guest cannot see PRIVATE", !acc.canView(priv, null));
        Db.check("stranger sees PUBLIC", acc.canView(pub, stranger));
        Db.check("stranger cannot see FRIENDS_ONLY (the old bug)", !acc.canView(fo, stranger));
        Db.check("stranger cannot see PRIVATE", !acc.canView(priv, stranger));
        Db.check("accepted friend (owner requested) sees FRIENDS_ONLY", acc.canView(fo, friend));
        Db.check("accepted friend (owner addressee) sees FRIENDS_ONLY", acc.canView(fo, friend2));
        Db.check("accepted friend cannot see PRIVATE", !acc.canView(priv, friend));
        Db.check("pending requester cannot see FRIENDS_ONLY", !acc.canView(fo, pending));
        Db.check("blocked user cannot see FRIENDS_ONLY", !acc.canView(fo, blocked));
        Db.check("owner sees PRIVATE", acc.canView(priv, owner));
        Db.check("owner sees own flagged doc", acc.canView(flaggedPub, owner));
        Db.check("stranger cannot see flagged PUBLIC doc", !acc.canView(flaggedPub, stranger));
        Db.check("guest cannot see flagged PUBLIC doc", !acc.canView(flaggedPub, null));
        Db.check("admin sees PRIVATE", acc.canView(priv, stranger, true));
        Db.check("admin sees flagged doc", acc.canView(flaggedPub, stranger, true));
        Db.check("null permission is treated as PRIVATE", !acc.canView(nullPerm, stranger) && acc.canView(nullPerm, owner));
        Db.check("null document is not viewable", !acc.canView(null, owner) && !acc.canView(null, null, true));
        Db.check("canEdit: owner only", acc.canEdit(pub, owner) && !acc.canEdit(pub, stranger) && !acc.canEdit(pub, friend) && !acc.canEdit(pub, null) && !acc.canEdit(null, owner));
        Db.done();
    }
}
