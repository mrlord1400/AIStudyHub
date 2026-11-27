package Filter;

import Utils.Access;
import Utils.AccessRules;
import Utils.Csrf;
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
import javax.servlet.http.HttpSession;

/**
 * Chống CSRF (Conv §4). Chạy sau AuthFilter, chỉ cho dispatcher REQUEST (MainController forward nội bộ thì
 * request gốc đã được kiểm tra).
 *
 * - Method không an toàn (POST, PUT, DELETE, ...): bắt buộc có token đúng trong header X-CSRF-Token hoặc tham số _csrf,
 *   nếu không trả 403.
 * - GET/HEAD tới một action ghi dữ liệu (Utils.Csrf.isMutatingAction): trả 405, vì GET không được đổi trạng thái.
 */
public class CsrfFilter implements Filter {

    private static final Logger LOG = Logger.getLogger(CsrfFilter.class.getName());

    static final String MSG_INVALID = "Phiên làm việc không hợp lệ hoặc đã hết hạn. Vui lòng tải lại trang rồi thử lại.";

    private final Gson gson = new Gson();

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // Giống AuthFilter: chốt encoding trước khi đọc tham số.
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding("UTF-8");
        }

        if (Csrf.isSafeMethod(request.getMethod())) {
            String action = request.getParameter("action");
            if (Csrf.isMutatingAction(action) && !"OPTIONS".equalsIgnoreCase(request.getMethod())) {
                LOG.log(Level.WARNING, "mutating action by safe method rejected method={0} path={1} action={2}",
                        new Object[]{request.getMethod(), request.getServletPath(), action});
                response.setHeader("Allow", "POST");
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        String supplied = request.getHeader(Csrf.HEADER);
        if (supplied == null || supplied.isEmpty()) {
            supplied = request.getParameter(Csrf.PARAM);
        }
        if (Csrf.matches(session, supplied)) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getServletPath() + (request.getPathInfo() == null ? "" : request.getPathInfo());
        LOG.log(Level.WARNING, "csrf token rejected path={0} action={1} hasSession={2} hasToken={3}",
                new Object[]{path, request.getParameter("action"), session != null, supplied != null && !supplied.isEmpty()});
        reject(request, response, path);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
        String action = request.getParameter("action");
        // fetch() của csrf.js luôn gửi X-Requested-With, nên đây là lời gọi AJAX.
        boolean ajax = request.getHeader("X-Requested-With") != null || AccessRules.isJsonEndpoint(path, action);
        if (AccessRules.isPlainTextEndpoint(path)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(MSG_INVALID);
            return;
        }
        if (ajax) {
            Map<String, Object> body = new LinkedHashMap<String, Object>();
            body.put("ok", false);
            body.put("success", false);
            body.put("code", "CSRF_INVALID");
            body.put("message", MSG_INVALID);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(gson.toJson(body));
            return;
        }
        // Trang HTML thường: phiên hết hạn (chưa đăng nhập) thì về trang đăng nhập; đang đăng nhập mà token cũ
        // (trang mở từ trước, hoặc phiên vừa được tạo lại bởi "ghi nhớ đăng nhập") thì quay lại chính trang đó để có token mới.
        if (Access.currentUserId(request) == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=session_expired");
            return;
        }
        String back = sameOriginReferer(request);
        if (back != null) {
            response.sendRedirect(back);
            return;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN, MSG_INVALID);
    }

    /** Referer chỉ được dùng nếu cùng origin với request (không bao giờ chuyển hướng ra trang khác). */
    private static String sameOriginReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isEmpty()) {
            return null;
        }
        String origin = request.getScheme() + "://" + request.getServerName()
                + ((request.getServerPort() == 80 || request.getServerPort() == 443) ? "" : ":" + request.getServerPort());
        String ctx = request.getContextPath();
        return (referer.startsWith(origin + ctx + "/") || referer.equals(origin + ctx)) ? referer : null;
    }

    @Override
    public void destroy() {
    }
}
