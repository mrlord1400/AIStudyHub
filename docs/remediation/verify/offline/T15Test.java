import Model.DAO.FriendshipDAO;
import Model.DTO.Friendship;

public class T15Test {

    static String status(int a, int b) throws Exception {
        return new FriendshipDAO().getFriendshipStatus(a, b);
    }

    public static void main(String[] x) throws Exception {
        Db.init();
        FriendshipDAO dao = new FriendshipDAO();
        int A = Db.user("A", "STUDENT"), B = Db.user("B", "STUDENT"), C = Db.user("C", "STUDENT");

        // A sends request to B
        Db.check("A->B request created", dao.createFriendship(new Friendship(A, B, "PENDING")));
        Db.check("reverse duplicate B->A is refused", !dao.createFriendship(new Friendship(B, A, "PENDING")));
        Db.check("same-direction duplicate A->B is refused", !dao.createFriendship(new Friendship(A, B, "PENDING")));
        Db.check("still exactly one row for the pair", Db.count("SELECT COUNT(*) FROM friendships") == 1);

        // requester cannot accept their own request
        Db.check("requester accepting own request does nothing", !dao.acceptRequest(A, B) && "PENDING".equals(status(A, B)));
        Db.check("area: acceptRequest(me=A, target=B) is not 'A accepts B' because A is the requester", !dao.acceptRequest(A, B));
        // addressee accepts
        Db.check("addressee B accepts A's request", dao.acceptRequest(B, A));
        Db.check("status is ACCEPTED", "ACCEPTED".equals(status(A, B)));
        Db.check("accepting again does nothing", !dao.acceptRequest(B, A));
        Db.check("areFriends true both directions", dao.areFriends(A, B) && dao.areFriends(B, A));

        // B blocks A (from ACCEPTED)
        Db.check("B blocks A", dao.block(B, A));
        Db.check("BLOCKED with blocker B", Db.count("SELECT COUNT(*) FROM friendships WHERE status='BLOCKED' AND blocker_id=?", B) == 1);
        Db.check("areFriends false once blocked", !dao.areFriends(A, B));

        // blocked A cannot flip it
        Db.check("blocked A cannot 'accept' (status stays BLOCKED)", !dao.acceptRequest(A, B) && !dao.acceptRequest(B, A) && "BLOCKED".equals(status(A, B)));
        Db.check("blocked A cannot re-block to steal the blocker role", !dao.block(A, B) && Db.count("SELECT COUNT(*) FROM friendships WHERE blocker_id=?", B) == 1);
        Db.check("blocked A cannot unblock (delete)", !dao.deleteFriendshipByUsers(A, B) && Db.count("SELECT COUNT(*) FROM friendships") == 1);
        Db.check("A cannot send a new request while blocked", !dao.createFriendship(new Friendship(A, B, "PENDING")));
        Db.check("blocker B can unblock (delete)", dao.deleteFriendshipByUsers(B, A));
        Db.check("row gone", Db.count("SELECT COUNT(*) FROM friendships") == 0);

        // block from PENDING; addressee blocks requester
        dao.createFriendship(new Friendship(A, C, "PENDING"));
        Db.check("addressee C blocks pending requester A", dao.block(C, A));
        Db.check("blocker is C", Db.count("SELECT COUNT(*) FROM friendships WHERE status='BLOCKED' AND blocker_id=?", C) == 1);
        dao.deleteFriendshipByUsers(C, A);

        // block with no relationship does nothing
        Db.check("block without any relationship does nothing", !dao.block(A, C));

        // requester can cancel own pending request; addressee can reject
        dao.createFriendship(new Friendship(A, B, "PENDING"));
        Db.check("requester cancels own pending request", dao.deleteFriendshipByUsers(A, B));
        dao.createFriendship(new Friendship(A, B, "PENDING"));
        Db.check("addressee rejects pending request", dao.deleteFriendshipByUsers(B, A));
        // either side can unfriend
        dao.createFriendship(new Friendship(A, B, "PENDING"));
        dao.acceptRequest(B, A);
        Db.check("either party can unfriend", dao.deleteFriendshipByUsers(A, B));
        Db.done();
    }
}
