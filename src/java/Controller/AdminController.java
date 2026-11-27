package Controller;

import Model.DTO.Document;
import Model.DAO.DocumentDAO;
import Model.DAO.AdminDAO;
import Model.DTO.Transaction;
import Model.DAO.TransactionDAO;
import Model.DTO.User;
import Model.DAO.UserDAO;
import Model.DAO.SubscriptionDAO;
import Model.DTO.Subscription;
import Utils.Access;
import Utils.PasswordUtil;
import Utils.Validation;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/AdminController")
public class AdminController extends HttpServlet {

    /** Trần cho số dư admin được nhập tay (users.balance là INT). */
    private static final int MAX_BALANCE = 1_000_000_000;

    private boolean tierExists(int tierId) {
        for (Subscription sub : new SubscriptionDAO().getAllSubscriptions()) {
            if (sub.getTierId() == tierId) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        // AuthFilter đã chặn trước; đây là lớp phòng thủ thứ hai.
        if (!Access.isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");

        String action = request.getParameter("action");
        AdminDAO dao = new AdminDAO();
        UserDAO userDAO = new UserDAO();
        TransactionDAO tranDAO = new TransactionDAO();

        if ("listUsers".equals(action)) {
            List<User> users = dao.getAllUsers();
            request.setAttribute("user_list", users);
            request.getRequestDispatcher("/admin_manageUser.jsp").forward(request, response);
            return;
        }

        if ("listDashboard".equals(action)) {
            List<User> users = dao.getAllUsers();
            List<Transaction> trans = tranDAO.getAllTransactions();
            request.setAttribute("totalUserAmount", users.size());
            request.setAttribute("totalTransactionAmount", trans.size());

            request.setAttribute("user_list", users);
            request.getRequestDispatcher("/admin_dashboard.jsp").forward(request, response);
            return;
        }

        if ("listTransactions".equals(action)) {
            List<User> users = dao.getAllUsers();
            request.setAttribute("user_list", users);
            request.getRequestDispatcher("/admin_manageTrans.jsp").forward(request, response);
            return;
        }

        if ("profile".equals(action)) {
            HttpSession userSession = request.getSession(false);
            if (userSession != null && userSession.getAttribute("userId") != null) {
                int adminId = (int) userSession.getAttribute("userId");
                User adminUser = userDAO.getUserById(adminId);

                if (adminUser != null) {
                    request.setAttribute("currentUser", adminUser);
                    request.getRequestDispatcher("/admin_profile.jsp").forward(request, response);
                } else {
                    response.sendRedirect(request.getContextPath() + "/login.jsp");
                }
            } else {
                response.sendRedirect(request.getContextPath() + "/login.jsp");
            }
            return;
        }

        if ("listPublicDocs".equals(action)) {
            DocumentDAO docDAO = new DocumentDAO();
            List<Document> docList = docDAO.getPublicDocumentsForAdmin();
            request.setAttribute("doc_list", docList);
            request.getRequestDispatcher("/admin_manageDocument.jsp").forward(request, response);
            return;
        }

        if ("adminViewDoc".equals(action)) {
            int docId = Integer.parseInt(request.getParameter("docId"));
            DocumentDAO docDAO = new DocumentDAO();
            Document doc = docDAO.findById(docId);

            if (doc != null) {
                request.setAttribute("document", doc);
                request.getRequestDispatcher("/document_view.jsp").forward(request, response);
            } else {
                response.sendRedirect(request.getContextPath() + "/AdminController?action=listPublicDocs&error=not_found");
            }
            return;
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        if (!Access.isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");

        String action = request.getParameter("action");

        AdminDAO adminDAO = new AdminDAO();
        UserDAO userDAO = new UserDAO();
        int currentAdminId = Access.currentUserId(request);

        if (action == null) {
            response.sendRedirect(request.getContextPath() + "/AdminController?action=listUsers");
            return;
        }

        switch (action) {
            case "updateAdminProfile":
                HttpSession session = request.getSession(false);
                if (session != null && session.getAttribute("userId") != null) {
                    int adminId = (int) session.getAttribute("userId");
                    String adminUsername = request.getParameter("username");
                    String adminEmail = request.getParameter("email");
                    String currentPasswordInput = request.getParameter("currentPassword");
                    String newPassword = request.getParameter("newPassword");

                    User adminUser = userDAO.getUserById(adminId);

                    if (adminUser != null) {

                        // Kiểm tra đầu vào (F-11): thông tin hiển thị hợp lệ, mật khẩu mới đủ mạnh nếu có nhập
                        boolean nameGiven = adminUsername != null && !adminUsername.trim().isEmpty();
                        boolean emailGiven = adminEmail != null && !adminEmail.trim().isEmpty();
                        boolean newPassGiven = newPassword != null && !newPassword.trim().isEmpty();
                        if ((nameGiven && !Validation.isValidUsername(adminUsername))
                                || (emailGiven && !Validation.isValidEmail(adminEmail))) {
                            response.sendRedirect(request.getContextPath() + "/AdminController?action=profile&error=invalid_input");
                            return;
                        }
                        if (newPassGiven && !PasswordUtil.isValidPassword(newPassword.trim())) {
                            response.sendRedirect(request.getContextPath() + "/AdminController?action=profile&error=weak_password");
                            return;
                        }

                        if (currentPasswordInput == null) {
                            currentPasswordInput = "";
                        }
                        currentPasswordInput = currentPasswordInput.trim();

                        // 1. KIỂM TRA MẬT KHẨU BẰNG HÀM CÓ SẴN CỦA USER_DAO (Hỗ trợ BCrypt/Verify)
                        boolean isPasswordValid = userDAO.checkPassword(adminId, currentPasswordInput);

                        if (!isPasswordValid) {
                            response.sendRedirect(request.getContextPath() + "/AdminController?action=profile&error=wrong_password");
                            return;
                        }

                        // 2. CẬP NHẬT THÔNG TIN CƠ BẢN (Không dính dáng tới password)
                        if (adminUsername != null && !adminUsername.trim().isEmpty()) {
                            adminUser.setUsername(adminUsername.trim());
                        }
                        if (adminEmail != null && !adminEmail.trim().isEmpty()) {
                            adminUser.setEmail(adminEmail.trim());
                        }

                        boolean success = userDAO.updateUser(adminUser);

                        // 3. CẬP NHẬT MẬT KHẨU MỚI BẰNG HÀM RIÊNG
                        if (success) {
                            if (newPassword != null && !newPassword.trim().isEmpty()) {
                                // Gọi hàm updatePassword đã có sẵn trong UserDAO
                                String hashedNewPass = PasswordUtil.hashPassword(newPassword.trim());
                                userDAO.updatePassword(adminUser.getEmail(), hashedNewPass);
                            }

                            session.setAttribute("username", adminUser.getUsername());
                            response.sendRedirect(request.getContextPath() + "/AdminController?action=profile&updateSuccess=1");
                        } else {
                            response.sendRedirect(request.getContextPath() + "/AdminController?action=profile&error=update_failed");
                        }
                    }
                }
                return;

            case "createUser":
                String username = request.getParameter("username");
                String email = request.getParameter("email");
                String password = request.getParameter("password");
                String accountType = request.getParameter("role");

                String role = "STUDENT";
                int tierId = 2;

                // Whitelist (F-11). Form hiện gửi STUDENT hoặc ADMIN; free/premium là giá trị cũ vẫn được chấp nhận.
                String accountKey = accountType == null ? "" : accountType.trim().toLowerCase();
                if (!Validation.isOneOf(accountKey, "student", "free", "premium", "admin")
                        || !Validation.isValidUsername(username)
                        || !Validation.isValidEmail(email)
                        || !PasswordUtil.isValidPassword(password)) {
                    response.sendRedirect(request.getContextPath() + "/AdminController?action=listUsers&error=invalid_input");
                    return;
                }

                switch (accountKey) {
                    case "student":
                    case "free":
                        role = "STUDENT";
                        tierId = 2;
                        break;
                    case "premium":
                        role = "STUDENT";
                        tierId = 3;
                        break;
                    case "admin":
                        role = "ADMIN";
                        tierId = 2;
                        break;
                }

                User newUser = new User();
                newUser.setUsername(username.trim());
                newUser.setEmail(email.trim());
                newUser.setPasswordHash(PasswordUtil.hashPassword(password));
                newUser.setRole(role);
                newUser.setTierId(tierId);
                newUser.setStatus("ACTIVE");

                if (!adminDAO.createUser(newUser)) {
                    response.sendRedirect(request.getContextPath() + "/AdminController?action=listUsers&error=create_failed");
                    return;
                }
                break;

            case "updateUser":
                Integer userId = Validation.parseIntInRange(request.getParameter("user_id"), 1, Integer.MAX_VALUE);
                String newUsername = request.getParameter("newUsername");
                String newEmail = request.getParameter("newEmail");
                String newRole = request.getParameter("newRole");
                String newStatus = request.getParameter("newStatus");
                Integer newBalance = Validation.parseIntInRange(request.getParameter("newBalance"), 0, MAX_BALANCE);
                Integer newTierId = Validation.parseIntInRange(request.getParameter("newTierId"), 1, Integer.MAX_VALUE);

                if (userId == null || newBalance == null || newTierId == null
                        || !Validation.isValidUsername(newUsername)
                        || !Validation.isValidEmail(newEmail)
                        || !Validation.isOneOf(newRole, "STUDENT", "ADMIN")
                        || !Validation.isOneOf(newStatus, "ACTIVE", "SUSPENDED", "BANNED")
                        || !tierExists(newTierId)) {
                    response.sendRedirect(request.getContextPath() + "/AdminController?action=listUsers&error=invalid_input");
                    return;
                }

                // Admin không được tự hạ quyền / tự khóa mình (tránh mất quyền quản trị)
                if (userId == currentAdminId && (!"ADMIN".equals(newRole) || !"ACTIVE".equals(newStatus))) {
                    response.sendRedirect(request.getContextPath() + "/AdminController?action=listUsers&error=self_modify");
                    return;
                }

                User user = userDAO.getUserById(userId);
                if (user != null) {
                    user.setUsername(newUsername.trim());
                    user.setEmail(newEmail.trim());
                    user.setRole(newRole);
                    user.setStatus(newStatus);
                    user.setBalance(newBalance);
                    user.setTierId(newTierId);
                    userDAO.updateUser(user);

                    if ("SUSPENDED".equals(newStatus)) {
                        DocumentDAO docDAO = new DocumentDAO();
                        docDAO.privatizePublicDocumentsByUserId(userId);
                    }
                }
                break;

            case "deleteUser":
                Integer deleteId = Validation.parseIntInRange(request.getParameter("user_id"), 1, Integer.MAX_VALUE);
                if (deleteId == null) {
                    response.sendRedirect(request.getContextPath() + "/AdminController?action=listUsers&error=invalid_input");
                    return;
                }
                if (deleteId == currentAdminId) {
                    response.sendRedirect(request.getContextPath() + "/AdminController?action=listUsers&error=self_modify");
                    return;
                }
                userDAO.deleteUserAndAssociatedData(deleteId);
                break;
        }

        response.sendRedirect(
                request.getContextPath()
                + "/AdminController?action=listUsers");
    }
}
