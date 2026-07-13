package Controller;

import Model.DTO.User;
import Model.DAO.UserDAO;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import Utils.PasswordUtil;
import Utils.CookieUtil;

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

            // Xóa Cookie "remember_token" khi người dùng chủ động đăng xuất
            Cookie cookie = new Cookie("remember_token", "");
            cookie.setMaxAge(0);
            cookie.setPath("/"); // Đảm bảo xóa đúng cookie trên toàn bộ project
            response.addCookie(cookie);

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

            case "processResetPassword":
                handleResetPassword(request, response);
                break;

            default:
                response.sendRedirect("login.jsp");
        }
    }
    
    /**
     * Helper method to enforce Password Business Rules:
     * BR-15 (>= 8 chars), BR-16 (Uppercase), BR-17 (Number), BR-17b (Special char)
     */
    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) return false;
        if (!password.matches(".*[A-Z].*")) return false;
        if (!password.matches(".*[0-9].*")) return false;
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) return false;
        return true;
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
        
        // Validate password rules before proceeding
        if (!isValidPassword(password)) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=weak_password");
            return;
        }

        User user = new User();

        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(
                PasswordUtil.hashPassword(
                        password));

        user.setRole("STUDENT");
        user.setTierId(2); // Free Tier
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
        String remember = request.getParameter("remember"); 

        UserDAO dao = new UserDAO();

        User user = dao.getUserByEmail(email);

        if(user != null &&
                PasswordUtil.verifyPassword(
                        password,
                        user.getPasswordHash())){

            // Chặn tài khoản BANNED
            if ("BANNED".equalsIgnoreCase(user.getStatus())) {
                response.sendRedirect(
                        request.getContextPath()
                                + "/login.jsp?error=banned");
                return;
            }

            HttpSession session = request.getSession();

            session.setAttribute("userId", user.getUserId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("role", user.getRole());
            session.setAttribute("tierId", user.getTierId());
            session.setAttribute("balance", user.getBalance());

            if ("true".equals(remember)) {
                String token = CookieUtil.buildRememberToken(user.getEmail());
                Cookie c = new Cookie("remember_token", token);
                c.setMaxAge((int) CookieUtil.getRememberDurationSeconds());
                c.setPath("/");
                c.setHttpOnly(true); 
                response.addCookie(c);
            }

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

    /**
     * Đặt lại mật khẩu sau khi đã xác thực OTP thành công
     */
    private void handleResetPassword(HttpServletRequest request,
                                      HttpServletResponse response)
            throws IOException {

        String email = request.getParameter("email");
        String newPassword = request.getParameter("newPassword");

        HttpSession session = request.getSession(false);
        Boolean canReset = (session != null)
                ? (Boolean) session.getAttribute("ALLOW_RESET_" + email)
                : null;

        if (canReset == null || !canReset) {
            response.sendRedirect(
                    request.getContextPath()
                            + "/login.jsp?error=unauthorized");
            return;
        }

        if (email == null || newPassword == null || newPassword.trim().isEmpty()) {
            response.sendRedirect(
                    request.getContextPath()
                            + "/reset_password.jsp?email=" + email);
            return;
        }
        
        // Validate mật khẩu mới với BR-15, 16, 17, 17b
        if (!isValidPassword(newPassword)) {
            response.sendRedirect(
                    request.getContextPath()
                            + "/reset_password.jsp?email=" + email + "&error=weak_password");
            return;
        }

        UserDAO dao = new UserDAO();

        String newPasswordHash = PasswordUtil.hashPassword(newPassword);

        boolean success = dao.updatePassword(email, newPasswordHash);

        session.removeAttribute("ALLOW_RESET_" + email);

        if (success) {

            response.sendRedirect(
                    request.getContextPath()
                            + "/login.jsp?reset=success");

        } else {

            response.sendRedirect(
                    request.getContextPath()
                            + "/login.jsp?error=reset_failed");
        }
    }
}