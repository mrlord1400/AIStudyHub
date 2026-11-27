package Model.Service;

import Model.DAO.FriendshipDAO;
import Model.DTO.Document;

/**
 * Mọi quyết định "ai được xem / sửa tài liệu này" nằm ở đây (Conv §2.2).
 * viewDoc, viewPublicPage, downloadDoc, toggleBookmark, lệnh VIEW/ của chatbot và
 * createReport đều phải gọi vào lớp này, không tự viết lại điều kiện.
 */
public class DocumentAccess {

    private final FriendshipDAO friendshipDao;

    public DocumentAccess() {
        this(new FriendshipDAO());
    }

    public DocumentAccess(FriendshipDAO friendshipDao) {
        this.friendshipDao = friendshipDao;
    }

    /** Người dùng thường (không phải admin). */
    public boolean canView(Document doc, Integer viewerId) {
        return canView(doc, viewerId, false);
    }

    /**
     * @param viewerId      id người xem, null nếu là khách
     * @param viewerIsAdmin lấy từ Utils.Access.isAdmin(request)
     *
     * Quy tắc: admin xem được tất cả; chủ sở hữu luôn xem được; tài liệu bị cắm cờ
     * (is_flagged) chỉ chủ sở hữu và admin thấy (giống Explore); PUBLIC ai cũng xem;
     * FRIENDS_ONLY cần quan hệ bạn bè ACCEPTED (một trong hai chiều); còn lại là không.
     */
    public boolean canView(Document doc, Integer viewerId, boolean viewerIsAdmin) {
        if (doc == null) {
            return false;
        }
        if (viewerIsAdmin) {
            return true;
        }
        if (viewerId != null && doc.getUserId() == viewerId) {
            return true;
        }
        if (doc.isFlagged()) {
            return false;
        }
        String permission = doc.getSharingPermission() == null ? "PRIVATE" : doc.getSharingPermission().toUpperCase();
        if ("PUBLIC".equals(permission)) {
            return true;
        }
        if ("FRIENDS_ONLY".equals(permission)) {
            return viewerId != null && friendshipDao.areFriends(viewerId, doc.getUserId());
        }
        return false;
    }

    /** Chỉ chủ sở hữu được sửa/xóa. Admin xóa qua endpoint admin riêng. */
    public boolean canEdit(Document doc, Integer userId) {
        return doc != null && userId != null && doc.getUserId() == userId;
    }
}
