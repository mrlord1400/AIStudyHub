package Filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * Header bảo mật cho mọi response (T3.3). Đặt đầu chuỗi filter trong web.xml để cả response lỗi/redirect của các
 * filter sau cũng có.
 *
 * X-Frame-Options là SAMEORIGIN chứ không phải DENY: trang xem tài liệu nhúng chính file của nó bằng iframe cùng
 * origin (DocumentController?action=viewDoc). Trang nhúng từ origin khác vẫn bị chặn (decision D-19).
 */
public class SecurityHeadersFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("Referrer-Policy", "same-origin");
        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {
    }
}
