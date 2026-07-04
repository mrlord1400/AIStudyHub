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
        String remember = request.getParameter("remember"); // Lấy trạng thái của ô Ghi nhớ đăng nhập

        UserDAO dao = new UserDAO();

        User user = dao.getUserByEmail(email);

        if(user != null &&
                PasswordUtil.verifyPassword(
                        password,
                        user.getPasswordHash())){

            HttpSession session = request.getSession();

            session.setAttribute("userId", user.getUserId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("role", user.getRole());
            session.setAttribute("tierId", user.getTierId());
            session.setAttribute("balance", user.getBalance());

            // Xử lý tạo Cookie nếu user chọn "Ghi nhớ đăng nhập"
            // Token được KÝ (HMAC) thay vì lưu thẳng email, tránh bị giả mạo Cookie
            // để đăng nhập vào tài khoản người khác mà không cần mật khẩu.
            if ("true".equals(remember)) {
                String token = CookieUtil.buildRememberToken(user.getEmail());
                Cookie c = new Cookie("remember_token", token);
                c.setMaxAge((int) CookieUtil.getRememberDurationSeconds());
                c.setPath("/");
                c.setHttpOnly(true); // Chặn JS đọc cookie -> giảm rủi ro XSS đánh cắp token
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
     * Đặt lại mật khẩu sau khi đã xác thực OTP thành công (luồng Quên mật khẩu).
     * Yêu cầu bảo mật:
     * - Chỉ cho phép đổi mật khẩu nếu session có cờ ALLOW_RESET_<email> = true
     *   (cờ này được ForgotPasswordController set sau khi verifyOTP đúng).
     * - Sau khi đổi xong phải xóa cờ này để không thể tái sử dụng để đổi mật khẩu lần nữa.
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

        // Chặn trường hợp cố tình gọi thẳng action này mà chưa qua bước xác thực OTP
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

        UserDAO dao = new UserDAO();

        String newPasswordHash = PasswordUtil.hashPassword(newPassword);

        boolean success = dao.updatePassword(email, newPasswordHash);

        // Xóa cờ cho phép reset, tránh việc gọi lại action này nhiều lần
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