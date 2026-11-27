package Filter;

import Model.DTO.User;
import Model.Service.RememberMeService;
import Utils.Access;
import java.io.IOException;
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
 * Đăng nhập tự động bằng cookie remember_token (T3.4). Thay cho scriptlet trong login.jsp: chạy cho mọi request chưa
 * đăng nhập mà có cookie, nên đường dẫn sâu (link tài liệu) cũng vào được sau khi phiên hết hạn.
 * Đứng trước SessionSyncFilter và AuthFilter trong web.xml.
 */
public class RememberMeFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (Access.currentUserId(request) == null && request.getCookies() != null) {
            boolean hasCookie = false;
            for (javax.servlet.http.Cookie c : request.getCookies()) {
                if (RememberMeService.COOKIE_NAME.equals(c.getName())) {
                    hasCookie = true;
                    break;
                }
            }
            if (hasCookie) {
                User user = RememberMeService.authenticate(request, response);
                if (user != null) {
                    HttpSession old = request.getSession(false);
                    if (old != null) {
                        old.invalidate();
                    }
                    Access.signIn(request.getSession(true), user);
                }
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
