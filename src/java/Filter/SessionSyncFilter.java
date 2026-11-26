package Filter;

import Model.DAO.UserDAO;
import Model.DTO.User;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter("/*")
public class SessionSyncFilter implements Filter {

    // Khoảng thời gian tối thiểu giữa 2 lần đồng bộ, tránh query DB mỗi request
    private static final long SYNC_INTERVAL_MS = 60_000; // 1 phút (test có thể để 5_000)

    private final UserDAO userDAO = new UserDAO();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute("userId") != null) {
            Long lastSync = (Long) session.getAttribute("lastSessionSync");
            long now = System.currentTimeMillis();

            if (lastSync == null || (now - lastSync) >= SYNC_INTERVAL_MS) {
                int userId = (int) session.getAttribute("userId");
                User freshUser = userDAO.getUserById(userId);

                if (freshUser != null) {
                    // Nếu tài khoản bị BAN giữa chừng -> logout ngay
                    if ("BANNED".equalsIgnoreCase(freshUser.getStatus())) {
                        session.invalidate();
                        response.sendRedirect(request.getContextPath() + "/login.jsp?error=banned");
                        return;
                    }

                    session.setAttribute("tierId", freshUser.getTierId());
                    session.setAttribute("balance", freshUser.getBalance());
                    session.setAttribute("role", freshUser.getRole());
                    session.setAttribute("lastSessionSync", now);
                } else {
                    // User bị xóa khỏi DB -> logout
                    session.invalidate();
                    response.sendRedirect(request.getContextPath() + "/login.jsp?error=invalid_session");
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) { }

    @Override
    public void destroy() { }
}