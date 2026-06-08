package Controller;

import Model.User;
import Model.UserDAO;
import Utils.PasswordUtil;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "UserController", urlPatterns = {"/UserController"})
public class UserController extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        String action = request.getParameter("action");
        int userId = (int) session.getAttribute("userId");
        UserDAO dao = new UserDAO();

        try {
            if ("profile".equals(action)) {
                // Fetch fresh user data to populate the profile form
                User currentUser = dao.getUserById(userId);
                if (currentUser != null) {
                    request.setAttribute("currentUser", currentUser);
                    request.getRequestDispatcher("/profile.jsp").forward(request, response);
                } else {
                    response.sendRedirect(request.getContextPath() + "/login.jsp");
                }

            } else if ("updateProfile".equals(action)) {
                String username = request.getParameter("username");
                String email = request.getParameter("email");
                String currentPassword = request.getParameter("currentPassword");
                String newPassword = request.getParameter("newPassword");

                // Security Gate: Verify current password before allowing ANY changes
                if (!dao.checkPassword(userId, currentPassword)) {
                    response.sendRedirect(request.getContextPath() + "/MainController?action=profile&error=wrong_password");
                    return;
                }

                User existingUser = dao.getUserById(userId);
                String finalPasswordHash = existingUser.getPasswordHash();

                // If the user typed a new password, update it. Otherwise, keep the old one.
                if (newPassword != null && !newPassword.trim().isEmpty()) {
                    finalPasswordHash =
                            PasswordUtil.hashPassword(
                                    newPassword
                            );
                }

                User userToUpdate = new User();
                userToUpdate.setUserId(userId);
                userToUpdate.setUsername(username);
                userToUpdate.setEmail(email);
                userToUpdate.setPasswordHash(finalPasswordHash);

                boolean success = dao.updateUser(userToUpdate);
                if (success) {
                    // Sync the new username to the active HTTP session
                    session.setAttribute("username", username);
                    response.sendRedirect(request.getContextPath() + "/MainController?action=profile&updateSuccess=1");
                } else {
                    response.sendRedirect(request.getContextPath() + "/MainController?action=profile&error=update_failed");
                }

            } else if ("deleteAccount".equals(action)) {
                boolean success = dao.deleteUserAndAssociatedData(userId);
                if (success) {
                    session.invalidate();
                    response.sendRedirect(request.getContextPath() + "/login.jsp?account_deleted=1");
                } else {
                    response.sendRedirect(request.getContextPath() + "/MainController?action=profile&error=delete_failed");
                }
            }
        } catch (Exception e) {
            System.err.println("[UserController Error] " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/MainController?action=profile&error=system_error");
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