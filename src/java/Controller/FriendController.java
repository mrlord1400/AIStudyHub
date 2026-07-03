package Controller;

import Model.DAO.FriendshipDAO;
import Model.DAO.UserDAO;
import Model.DTO.Friendship;
import Model.DTO.User;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "FriendController", urlPatterns = {"/FriendController"})
public class FriendController extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // 1. Kiểm tra trạng thái đăng nhập của tài khoản (Security Guard)
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        int userId = (int) session.getAttribute("userId");
        String action = request.getParameter("action");

        // Cơ chế Failsafe: Nếu không chỉ định action, mặc định sẽ là tải danh sách bạn bè
        if (action == null || action.trim().isEmpty()) {
            action = "friendList";
        }

        FriendshipDAO friendshipDao = new FriendshipDAO();
        UserDAO userDao = new UserDAO();
        
        try {
            // ====================================================================================
            // NHÓM 1: CÁC ACTION XỬ LÝ DỮ LIỆU (CREATE, UPDATE, DELETE) - SẼ REDIRECT SAU KHI XONG
            // ====================================================================================
            if ("createFriendship".equals(action)) {
                String addresseeIdParam = request.getParameter("addresseeId");
                if (addresseeIdParam != null && !addresseeIdParam.trim().isEmpty()) {
                    int addresseeId = Integer.parseInt(addresseeIdParam);

                    if (userId == addresseeId) {
                        response.sendRedirect(request.getContextPath() + "/FriendController?action=findUserByEmail&error=self_request");
                        return;
                    }

                    String currentStatus = friendshipDao.getFriendshipStatus(userId, addresseeId);
                    if (!"NONE".equals(currentStatus)) {
                        response.sendRedirect(request.getContextPath() + "/FriendController?action=findUserByEmail&error=already_exists");
                        return;
                    }

                    Friendship newFriendship = new Friendship(userId, addresseeId, "PENDING");
                    boolean success = friendshipDao.createFriendship(newFriendship);

                    if (success) {
                        response.sendRedirect(request.getContextPath() + "/FriendController?action=pendingList&success=request_sent");
                        return;
                    }
                }
                response.sendRedirect(request.getContextPath() + "/FriendController?action=friendList&error=create_failed");

            } else if ("updateFriendshipStatus".equals(action)) {
                int targetUserId = Integer.parseInt(request.getParameter("targetUserId"));
                String status = request.getParameter("status"); 

                if (status != null && (status.equalsIgnoreCase("ACCEPTED") || status.equalsIgnoreCase("BLOCKED"))) {
                    boolean success = friendshipDao.updateFriendStatusByUsers(userId, targetUserId, status.toUpperCase());
                    if (success) {
                        String redirectAction = status.equalsIgnoreCase("ACCEPTED") ? "friendList" : "blockedList";
                        response.sendRedirect(request.getContextPath() + "/FriendController?action=" + redirectAction + "&success=status_updated");
                        return;
                    }
                }
                response.sendRedirect(request.getContextPath() + "/FriendController?action=pendingList&error=update_failed");

            } else if ("deleteFriendship".equals(action)) {
                int targetUserId = Integer.parseInt(request.getParameter("targetUserId"));
                String returnPath = request.getParameter("returnPath");
                if (returnPath == null || returnPath.trim().isEmpty()) {
                    returnPath = "friendList";
                }

                Friendship existing = friendshipDao.getFriendshipDetail(userId, targetUserId);
                if (existing != null && "BLOCKED".equalsIgnoreCase(existing.getStatus())
                        && (existing.getBlockerId() == null || existing.getBlockerId() != userId)) {
                    response.sendRedirect(request.getContextPath() + "/FriendController?action=" + returnPath + "&error=not_authorized");
                    return;
                }

                boolean success = friendshipDao.deleteFriendshipByUsers(userId, targetUserId);
                if (success) {
                    response.sendRedirect(request.getContextPath() + "/FriendController?action=" + returnPath + "&success=deleted");
                } else {
                    response.sendRedirect(request.getContextPath() + "/FriendController?action=" + returnPath + "&error=delete_failed");
                }
            } 
            // ====================================================================================
            // NHÓM 2: CÁC ACTION HIỂN THỊ GIAO DIỆN (READ) - SẼ FORWARD SANG friendMain.jsp
            // ====================================================================================
            else {
                // 1. CHUẨN BỊ SẴN CÁC CON SỐ CHO 3 TAB (Dùng luôn List có sẵn để an toàn, không cần viết DAO mới)
                List<User> acceptedList = userDao.getAcceptedFriends(userId);
                List<User> pendingList = userDao.getPendingRequests(userId);
                List<User> blockedList = userDao.getBlockedUsers(userId);

                request.setAttribute("friendCount", acceptedList != null ? acceptedList.size() : 0);
                request.setAttribute("pendingCount", pendingList != null ? pendingList.size() : 0);
                request.setAttribute("blockedCount", blockedList != null ? blockedList.size() : 0);

                // 2. XỬ LÝ LOGIC RIÊNG CHO TỪNG TAB ĐƯỢC CHỌN
                if ("friendList".equals(action)) {
                    request.setAttribute("friends", acceptedList);
                } 
                else if ("pendingList".equals(action)) {
                    request.setAttribute("pendingRequests", pendingList);
                } 
                else if ("blockedList".equals(action)) {
                    request.setAttribute("blockedUsers", blockedList);
                } 
                else if ("findUserByEmail".equals(action)) {
                    String searchEmail = request.getParameter("email");
                    if (searchEmail != null && !searchEmail.trim().isEmpty()) {
                        User searchedUser = userDao.getUserByEmail(searchEmail.trim());

                        if (searchedUser != null) {
                            request.setAttribute("searchedUser", searchedUser);

                            if (searchedUser.getUserId() == userId) {
                                request.setAttribute("friendshipStatus", "SELF");
                            } else {
                                Friendship rel = friendshipDao.getFriendshipDetail(userId, searchedUser.getUserId());
                                String viewStatus;

                                if (rel == null) {
                                    viewStatus = "NONE";
                                } else if ("PENDING".equalsIgnoreCase(rel.getStatus())) {
                                    viewStatus = (rel.getRequesterId() == userId) ? "PENDING_SENT" : "PENDING_RECEIVED";
                                } else if ("BLOCKED".equalsIgnoreCase(rel.getStatus())) {
                                    viewStatus = (rel.getBlockerId() != null && rel.getBlockerId() == userId)
                                            ? "BLOCKED_BY_ME" : "BLOCKED_BY_THEM";
                                } else {
                                    viewStatus = rel.getStatus(); // ACCEPTED
                                }
                                request.setAttribute("friendshipStatus", viewStatus);
                            }
                        } else {
                            request.setAttribute("searchError", "Không tìm thấy người dùng với email này.");
                        }
                    }
                }

                // 3. ĐẨY DATA SANG JSP (Chỉ cần gọi 1 lần duy nhất ở đây)
                request.getRequestDispatcher("/friendMain.jsp").forward(request, response);
            }

        } catch (Exception e) {
            System.err.println("[FriendController Error] " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?error=system_error");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}