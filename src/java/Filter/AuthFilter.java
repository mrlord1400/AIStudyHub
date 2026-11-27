package Filter;

import Utils.Access;
import Utils.AccessRules;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Cổng phân quyền duy nhất, default-deny (Conv §2.1).
 *
 * Đăng ký trong web.xml (không dùng annotation) để thứ tự filter xác định:
 * SessionSyncFilter -> AuthFilter -> (CsrfFilter). Chạy cho cả REQUEST và FORWARD vì
 * MainController forward sang controller khác; lúc đó getServletPath() là đích forward.
 *
 * Controller vẫn giữ kiểm tra riêng như lớp phòng thủ thứ hai, nhưng đây là nơi quyết định.
 */
public class AuthFilter implements Filter {

    private static final Logger LOG = Logger.getLogger(AuthFilter.class.getName());

    private static final String MSG_UNAUTHORIZED = "Vui lòng đăng nhập để tiếp tục.";
    private static final String MSG_FORBIDDEN = "Bạn không có quyền thực hiện thao tác này.";
    private static final String MSG_CHAT_UNAUTHORIZED = "Vui lòng đăng nhập để sử dụng AI.";

    private final Gson gson = new Gson();

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // Đọc tham số "action" sẽ chốt encoding của body form, nên phải đặt UTF-8 trước
        // (các controller cũng gọi setCharacterEncoding nhưng sẽ quá muộn).
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding("UTF-8");
        }

        String path = request.getServletPath();
        if (request.getPathInfo() != null) {
            path += request.getPathInfo();
        }
        String action = request.getParameter("action");

        if (AccessRules.isRetiredUploadsPath(path)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        AccessRules.Level required = AccessRules.classify(path, action);
        if (required == AccessRules.Level.PUBLIC) {
            chain.doFilter(request, response);
            return;
        }

        Integer userId = Access.currentUserId(request);
        if (userId == null) {
            deny(request, response, path, action, false, required);
            return;
        }
        if (required == AccessRules.Level.ADMIN && !Access.isAdmin(request)) {
            deny(request, response, path, action, true, required);
            return;
        }

        chain.doFilter(request, response);
    }

    private void deny(HttpServletRequest request, HttpServletResponse response,
            String path, String action, boolean loggedIn, AccessRules.Level required)
            throws IOException {

        LOG.log(Level.WARNING, "access denied path={0} required={1} loggedIn={2} dispatcher={3}",
                new Object[]{path, required, loggedIn, request.getDispatcherType()});

        int status = loggedIn ? HttpServletResponse.SC_FORBIDDEN : HttpServletResponse.SC_UNAUTHORIZED;

        if (AccessRules.isJsonEndpoint(path, action)) {
            String message = loggedIn ? MSG_FORBIDDEN : MSG_UNAUTHORIZED;
            // "success" là cờ cũ mà JS hiện tại đọc; "ok"/"code" là envelope chuẩn (Conv §6, T5.5).
            Map<String, Object> body = new LinkedHashMap<String, Object>();
            body.put("ok", false);
            body.put("success", false);
            body.put("code", loggedIn ? "FORBIDDEN" : "UNAUTHORIZED");
            body.put("message", message);
            response.setStatus(status);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(gson.toJson(body));
            return;
        }

        if (AccessRules.isPlainTextEndpoint(path)) {
            response.setStatus(status);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(loggedIn ? MSG_FORBIDDEN : MSG_CHAT_UNAUTHORIZED);
            return;
        }

        if (!loggedIn) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    @Override
    public void destroy() {
    }
}
