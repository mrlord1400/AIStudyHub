import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.*;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** Minimal in-process servlet driver (proxy request/response). No Tomcat. */
public class Http {

    public static class Result {
        public String redirect;      // sendRedirect target
        public Integer error;        // sendError code
        public Integer status;       // setStatus code
        public String forward;       // RequestDispatcher.forward target
        public String body = "";
        public java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        public Map<String, String> headers = new HashMap<>();
        public String contentType;
        public Map<String, Object> requestAttrs = new HashMap<>();

        public String summary() {
            if (redirect != null) return "REDIRECT " + redirect;
            if (error != null) return "ERROR " + error;
            if (forward != null) return "FORWARD " + forward + (status != null ? " status=" + status : "");
            return "STATUS " + status + " body=" + body.trim();
        }
    }

    public static String webRoot;

    /** Optional: reuse one session map across calls (wizard flows). Reset to null when done. */
    public static Map<String, Object> sharedSession;

    /** Optional multipart file for request.getPart("file"). */
    public static class UploadPart {
        public final String fileName;
        public final byte[] data;

        public UploadPart(String fileName, byte[] data) {
            this.fileName = fileName;
            this.data = data;
        }
    }
    public static UploadPart part;
    public static final ServletContext CONTEXT = (ServletContext) Proxy.newProxyInstance(Http.class.getClassLoader(), new Class<?>[]{ServletContext.class}, (p, m, a) -> {
        switch (m.getName()) {
            case "getRealPath": return webRoot;
            case "getMimeType": return "text/plain";
            default: return null;
        }
    });

    /** Initialise a servlet with a stub config so getServletContext() works. */
    public static <T extends HttpServlet> T init(T servlet) throws Exception {
        servlet.init((ServletConfig) Proxy.newProxyInstance(Http.class.getClassLoader(), new Class<?>[]{ServletConfig.class}, (p, m, a) -> {
            if (m.getName().equals("getServletContext")) return CONTEXT;
            return null;
        }));
        return servlet;
    }

    /** who: null = anonymous; otherwise session attributes userId/role. */
    public static Result call(HttpServlet servlet, String method, Map<String, String[]> params, Integer userId, String role) throws Exception {
        final Map<String, Object> sess = sharedSession != null ? sharedSession : new HashMap<String, Object>();
        if (userId != null) {
            sess.put("userId", userId);
            sess.put("role", role);
        }
        final Result r = new Result();
        final StringWriter sw = new StringWriter();
        final HttpSession session = (HttpSession) Proxy.newProxyInstance(Http.class.getClassLoader(), new Class<?>[]{HttpSession.class}, (p, m, a) -> {
            switch (m.getName()) {
                case "getAttribute": return sess.get(a[0]);
                case "setAttribute": sess.put((String) a[0], a[1]); return null;
                case "removeAttribute": sess.remove(a[0]); return null;
                default: return null;
            }
        });
        HttpServletRequest req = (HttpServletRequest) Proxy.newProxyInstance(Http.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class}, (p, m, a) -> {
            switch (m.getName()) {
                case "getMethod": return method;
                case "getParameter": { String[] v = params.get(a[0]); return v == null || v.length == 0 ? null : v[0]; }
                case "getParameterValues": return params.get(a[0]);
                case "getSession": return (userId == null && a.length == 1 && Boolean.FALSE.equals(a[0])) ? null : session;
                case "getContextPath": return "/AIStudyHub";
                case "getPart": {
                    final UploadPart up = part;
                    if (up == null) return null;
                    return Proxy.newProxyInstance(Http.class.getClassLoader(), new Class<?>[]{javax.servlet.http.Part.class}, (p3, m3, a3) -> {
                        switch (m3.getName()) {
                            case "getSize": return (long) up.data.length;
                            case "getSubmittedFileName": return up.fileName;
                            case "getInputStream": return new java.io.ByteArrayInputStream(up.data);
                            default: return null;
                        }
                    });
                }
                case "getCharacterEncoding": return "UTF-8";
                case "setAttribute": r.requestAttrs.put((String) a[0], a[1]); return null;
                case "getAttribute": return r.requestAttrs.get(a[0]);
                case "getRequestDispatcher": {
                    final String path = (String) a[0];
                    return Proxy.newProxyInstance(Http.class.getClassLoader(), new Class<?>[]{RequestDispatcher.class}, (p2, m2, a2) -> {
                        if (m2.getName().equals("forward")) r.forward = path;
                        return null;
                    });
                }
                case "getServletContext": return CONTEXT;
                default: return null;
            }
        });
        HttpServletResponse res = (HttpServletResponse) Proxy.newProxyInstance(Http.class.getClassLoader(), new Class<?>[]{HttpServletResponse.class}, (p, m, a) -> {
            switch (m.getName()) {
                case "sendRedirect": r.redirect = (String) a[0]; return null;
                case "sendError": r.error = (Integer) a[0]; return null;
                case "setStatus": r.status = (Integer) a[0]; return null;
                case "getWriter": return new PrintWriter(sw, true);
                case "getOutputStream": return new ServletOutputStream() {
                    public void write(int b) { r.bytes.write(b); }
                    public boolean isReady() { return true; }
                    public void setWriteListener(WriteListener l) { }
                };
                case "setHeader": case "addHeader": r.headers.put((String) a[0], String.valueOf(a[1])); return null;
                case "setContentType": r.contentType = (String) a[0]; return null;
                default: return null;
            }
        });
        servlet.service(req, res);
        r.body = sw.toString();
        return r;
    }

    public static Map<String, String[]> params(String... kv) {
        Map<String, String[]> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.merge(kv[i], new String[]{kv[i + 1]}, (x, y) -> {
                String[] z = Arrays.copyOf(x, x.length + 1);
                z[x.length] = y[0];
                return z;
            });
        }
        return m;
    }
}
