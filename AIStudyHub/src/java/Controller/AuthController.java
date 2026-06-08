/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controller;

import Model.User;
import Model.UserDAO;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet(name = "AuthController", urlPatterns = {"/AuthController"})
public class AuthController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        if ("logout".equals(action)) {

            HttpSession session = request.getSession(false);

            if (session != null) {
                session.invalidate();
            }

            response.sendRedirect("login.jsp");

        } else {

            response.sendRedirect("login.jsp");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");

        if (action == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        switch (action) {

            case "register":
                handleRegister(request, response);
                break;

            case "login":
                handleLogin(request, response);
                break;

            case "update":
                handleUpdate(request, response);
                break;

            case "delete":
                handleDelete(request, response);
                break;

            default:
                response.sendRedirect("login.jsp");
        }
    }

    /**
     * Register new account
     * Requirement:
     * - Create account
     * - Default role = STUDENT
     * - Save to database
     */
    private void handleRegister(HttpServletRequest request,
                                HttpServletResponse response)
            throws IOException {

        String email = request.getParameter("email");
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // Nếu frontend không gửi username thì dùng email
        if (username == null || username.trim().isEmpty()) {
            username = email;
        }

        User user = new User();

        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(password);

        // Assignment requirement
        user.setRole("STUDENT");

        // Free tier by default
        user.setTierId(1);

        // Active account by default
        user.setStatus("ACTIVE");

        UserDAO dao = new UserDAO();

        boolean success = dao.register(user);

        if (success) {

            response.sendRedirect(
                    request.getContextPath()
                            + "/login.jsp?register=success");

        } else {

            response.sendRedirect(
                    request.getContextPath()
                            + "/login.jsp?error=register_failed");
        }
    }

    /**
     * Login
     * Requirement:
     * - Receive data from login page
     * - Authenticate user
     * - Show corresponding dashboard
     */
    private void handleLogin(HttpServletRequest request,
                             HttpServletResponse response)
            throws IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        UserDAO dao = new UserDAO();

        User user = dao.login(email, password);

        if (user != null) {

            HttpSession session = request.getSession();

            session.setAttribute("userId", user.getUserId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("role", user.getRole());
            session.setAttribute("balance", user.getBalance());
            session.setAttribute("tierId", user.getTierId());

            if ("ADMIN".equalsIgnoreCase(user.getRole())) {

                response.sendRedirect(
                        request.getContextPath()
                                + "/admin_dashboard.jsp");

            } else {

                response.sendRedirect(
                        request.getContextPath()
                                + "/user_dashboard.jsp");
            }

        } else {

            response.sendRedirect(
                    request.getContextPath()
                            + "/login.jsp?error=invalid_credentials");
        }
    }

    private void handleUpdate(HttpServletRequest request,
                              HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);

        if (session == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        int userId = (int) session.getAttribute("userId");

        String username = request.getParameter("username");
        String email = request.getParameter("email");

        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");

        UserDAO dao = new UserDAO();

        // 1. verify current password
        if (!dao.checkPassword(userId, currentPassword)) {
            response.sendRedirect(
                    request.getContextPath()
                            + "/profile.jsp?error=wrong_password");
            return;
        }

        // 2. get current user data
        User existingUser = dao.getUserById(userId);

        // 3. decide password hash (Removed BCrypt to match DB logic)
        String finalPasswordHash = existingUser.getPasswordHash();

        if (newPassword != null && !newPassword.isEmpty()) {
            // If you decide to implement BCrypt later, do it in Register, Login, AND here.
            finalPasswordHash = newPassword; 
        }

        // 4. build updated user
        // We initialize a new object but retain the required values for the update query.
        User user = new User();
        user.setUserId(userId);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(finalPasswordHash);

        boolean success = dao.updateUser(user);

        if (success) {
            session.setAttribute("username", username);

            response.sendRedirect(
                    request.getContextPath()
                            + "/user_dashboard.jsp?update=success");
        } else {
            response.sendRedirect(
                    request.getContextPath()
                            + "/profile.jsp?error=update_failed");
        }
    }

    private void handleDelete(HttpServletRequest request,
                              HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);

        if (session == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        int userId = (int) session.getAttribute("userId");

        UserDAO dao = new UserDAO();

        boolean success = dao.deleteUser(userId);

        if (success) {

            session.invalidate();

            response.sendRedirect(
                    request.getContextPath()
                            + "/login.jsp?account_deleted=true");

        } else {

            response.sendRedirect(
                    request.getContextPath()
                            + "/user_dashboard.jsp?error=delete_failed");
        }
    }
}
