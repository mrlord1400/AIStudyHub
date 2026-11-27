package Controller;

import Model.DTO.User;
import Model.DAO.UserDAO;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import Utils.PasswordUtil;
import Utils.Access;
import Utils.AuthLimits;
import java.util.logging.Level;
import java.util.logging.Logger;
import Model.Service.RememberMeService;
import Utils.Csrf;

@WebServlet(name = "AuthController", urlPatterns = {"/AuthController"})
public class AuthController extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(AuthController.class.getName());

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        // Đăng xuất là thao tác ghi nên chỉ nhận POST (CsrfFilter trả 405 cho GET ?action=logout, Conv §4).
        response.sendRedirect("login.jsp");
    }

    private static String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        // Hủy token ghi nhớ trong DB và xóa cookie khi người dùng chủ động đăng xuất
        RememberMeService.forget(request, response);

        response.sendRedirect("login.jsp");
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

            case "logout":
                handleLogout(request, response);
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

        // Giới hạn đăng nhập sai (T3.5): theo cặp (IP, email) và theo IP. Bị chặn thì không kiểm tra mật khẩu nữa.
        String ip = AuthLimits.clientIp(request);
        String pairKey = ip + "|" + AuthLimits.emailKey(email);
        // Đếm lượt TRƯỚC khi kiểm tra mật khẩu (nguyên tử): nếu đếm sau, 40 request song song đều qua được cổng kiểm tra.
        // Đăng nhập thành công thì trả lại lượt (xem bên dưới), chỉ lần sai mới bị tính.
        if (!AuthLimits.loginPair().tryAcquire(pairKey)) {
            LOG.log(Level.WARNING, "login throttled ip={0}", ip);
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=too_many_attempts");
            return;
        }
        if (!AuthLimits.loginIp().tryAcquire(ip)) {
            AuthLimits.loginPair().release(pairKey);
            LOG.log(Level.WARNING, "login throttled ip={0}", ip);
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=too_many_attempts");
            return;
        }

        UserDAO dao = new UserDAO();

        User user = dao.getUserByEmail(email);

        // Chặn tài khoản BANNED trước khi kiểm tra mật khẩu: trả lời không phụ thuộc mật khẩu đúng hay sai,
        // nên không dùng được để dò mật khẩu của tài khoản bị khóa (F-09).
        if (user != null && "BANNED".equalsIgnoreCase(user.getStatus())) {
            response.sendRedirect(
                    request.getContextPath()
                            + "/login.jsp?error=banned");
            return;
        }

        if(user != null &&
                PasswordUtil.verifyPassword(
                        password,
                        user.getPasswordHash())){

            AuthLimits.loginPair().clear(pairKey);
            AuthLimits.loginIp().release(ip);

            HttpSession session = request.getSession();
            // Chống session fixation: đổi id phiên khi quyền thay đổi, và bỏ token CSRF của phiên trước đăng nhập.
            request.changeSessionId();
            Csrf.rotate(session);

            Access.signIn(session, user);

            if ("true".equals(remember)) {
                RememberMeService.issue(request, response, user.getUserId());
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
                            + "/reset_password.jsp?email=" + urlEncode(email));
            return;
        }
        
        // Validate mật khẩu mới với BR-15, 16, 17, 17b
        if (!isValidPassword(newPassword)) {
            response.sendRedirect(
                    request.getContextPath()
                            + "/reset_password.jsp?email=" + urlEncode(email) + "&error=weak_password");
            return;
        }

        UserDAO dao = new UserDAO();

        String newPasswordHash = PasswordUtil.hashPassword(newPassword);

        boolean success = dao.updatePassword(email, newPasswordHash);

        session.removeAttribute("ALLOW_RESET_" + email);
        // Quyền đặt lại đã dùng xong: đổi id phiên và token CSRF.
        request.changeSessionId();
        Csrf.rotate(session);

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