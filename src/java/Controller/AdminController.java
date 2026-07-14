package Controller;

import Model.DTO.Document;
import Model.DAO.DocumentDAO;
import Model.DAO.AdminDAO;
import Model.DTO.Transaction;
import Model.DAO.TransactionDAO;
import Model.DTO.User;
import Model.DAO.UserDAO;
import Utils.PasswordUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/AdminController")
public class AdminController extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

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

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");

        String action = request.getParameter("action");

        AdminDAO adminDAO = new AdminDAO();
        UserDAO userDAO = new UserDAO();

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

                        if (currentPasswordInput == null) {
                            currentPasswordInput = "";
                        }
                        currentPasswordInput = currentPasswordInput.trim();

                        // 1. KIỂM TRA MẬT KHẨU BẰNG HÀM CÓ SẴN CỦA USER_DAO (Hỗ trợ BCrypt/Verify)
                        boolean isPasswordValid = userDAO.checkPassword(adminId, currentPasswordInput);

                        // Fallback: Đề phòng admin được tạo bằng tay trong DB với mật khẩu chưa mã hóa (plain text)
                        if (!isPasswordValid && currentPasswordInput.equals(adminUser.getPasswordHash())) {
                            isPasswordValid = true;
                        }

                        if (!isPasswordValid) {
                            response.sendRedirect(request.getContextPath() + "/AdminController?action=profile&error=wrong_password");
                            return;
                        }

                        // 2. CẬP NHẬT THÔNG TIN CƠ BẢN (Không dính dáng tới password)
                        if (adminUsername != null && !adminUsername.trim().isEmpty()) {
                            adminUser.setUsername(adminUsername);
                        }
                        if (adminEmail != null && !adminEmail.trim().isEmpty()) {
                            adminUser.setEmail(adminEmail);
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

                switch (accountType.toLowerCase()) {
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
                newUser.setUsername(username);
                newUser.setEmail(email);
                newUser.setPasswordHash(PasswordUtil.hashPassword(password));
                newUser.setRole(role);
                newUser.setTierId(tierId);
                newUser.setStatus("ACTIVE");

                adminDAO.createUser(newUser);
                break;

            case "updateUser":
                int userId = Integer.parseInt(request.getParameter("user_id"));
                String newUsername = request.getParameter("newUsername");
                String newEmail = request.getParameter("newEmail");
                String newRole = request.getParameter("newRole");
                String newStatus = request.getParameter("newStatus");
                int newBalance = Integer.parseInt(request.getParameter("newBalance"));
                int newTierId = Integer.parseInt(request.getParameter("newTierId"));

                User user = userDAO.getUserById(userId);
                if (user != null) {
                    user.setUsername(newUsername);
                    user.setEmail(newEmail);
                    user.setRole(newRole);
                    user.setStatus(newStatus);
                    user.setBalance(newBalance);
                    user.setTierId(newTierId);
                    userDAO.updateUser(user);

                    if ("SUSPENDED".equalsIgnoreCase(newStatus)) {
                        DocumentDAO docDAO = new DocumentDAO();
                        docDAO.privatizePublicDocumentsByUserId(userId);
                    }
                }
                break;

            case "deleteUser":
                userDAO.deleteUserAndAssociatedData(Integer.parseInt(request.getParameter("user_id")));
                break;
        }

        response.sendRedirect(
                request.getContextPath()
                + "/AdminController?action=listUsers");
    }
}
