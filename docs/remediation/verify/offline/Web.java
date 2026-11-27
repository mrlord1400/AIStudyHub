import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

/**
 * The real web application in an embedded Tomcat 9: web.xml (filters, servlets), the JSPs from web/ compiled by Jasper,
 * the controllers from the compiled src/java. Needs the tomcat-embed jars BEFORE servlet-api.jar on the classpath
 * (run.sh arranges it). Talks to the same in-memory H2 as Db.
 */
public class Web {

    public static class Resp {
        public int status;
        public String location;
        public String body = "";
    }

    private static Tomcat tomcat;
    private static Path baseDir;
    private static int port;
    public static final String CONTEXT = "/AIStudyHub";
    private static final CookieManager COOKIES = new CookieManager();

    /** @param webDir the repo's web/ folder; @param classesDir compiled src/java (what WEB-INF/classes would hold) */
    public static void start(String webDir, String classesDir) throws Exception {
        baseDir = Files.createTempDirectory("aish-tomcat");
        tomcat = new Tomcat();
        tomcat.setBaseDir(baseDir.toString());
        tomcat.setPort(0);
        tomcat.getConnector();
        Context ctx = tomcat.addWebapp(CONTEXT, new File(webDir).getAbsolutePath());
        WebResourceRoot res = new StandardRoot(ctx);
        res.addPreResources(new DirResourceSet(res, "/WEB-INF/classes", new File(classesDir).getAbsolutePath(), "/"));
        ctx.setResources(res);
        tomcat.start();
        port = tomcat.getConnector().getLocalPort();
        CookieHandler.setDefault(COOKIES);
    }

    public static void stop() throws Exception {
        if (tomcat != null) {
            tomcat.stop();
            tomcat.destroy();
        }
        if (baseDir != null) {
            try (java.util.stream.Stream<Path> w = Files.walk(baseDir)) {
                w.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    public static void forgetCookies() {
        COOKIES.getCookieStore().removeAll();
    }

    public static Resp get(String path) throws Exception {
        return send("GET", path, null);
    }

    public static Resp post(String path, String form) throws Exception {
        return send("POST", path, form, null, null);
    }

    public static Resp post(String path, String form, java.util.Map<String, String> headers) throws Exception {
        return send("POST", path, form, null, headers);
    }

    /** Raw body with a custom content type (multipart tests). */
    public static Resp postRaw(String path, byte[] body, String contentType, java.util.Map<String, String> headers) throws Exception {
        return send("POST", path, null, new Object[]{body, contentType}, headers);
    }

    public static Resp get(String path, java.util.Map<String, String> headers) throws Exception {
        return send("GET", path, null, null, headers);
    }

    private static Resp send(String method, String path, String form) throws Exception {
        return send(method, path, form, null, null);
    }

    private static Resp send(String method, String path, String form, Object[] raw, java.util.Map<String, String> headers) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL("http://localhost:" + port + CONTEXT + path).openConnection();
        c.setInstanceFollowRedirects(false);
        c.setRequestMethod(method);
        if (headers != null) {
            for (java.util.Map.Entry<String, String> h : headers.entrySet()) {
                c.setRequestProperty(h.getKey(), h.getValue());
            }
        }
        if (raw != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", (String) raw[1]);
            try (OutputStream o = c.getOutputStream()) {
                o.write((byte[]) raw[0]);
            }
        } else if (form != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            try (OutputStream o = c.getOutputStream()) {
                o.write(form.getBytes(StandardCharsets.UTF_8));
            }
        }
        Resp r = new Resp();
        r.status = c.getResponseCode();
        r.location = c.getHeaderField("Location");
        InputStream in = r.status >= 400 ? c.getErrorStream() : c.getInputStream();
        if (in != null) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                b.write(buf, 0, n);
            }
            r.body = new String(b.toByteArray(), StandardCharsets.UTF_8);
        }
        return r;
    }

    /** The CSRF token of the current session, read from the meta tag of a page. */
    public static String csrf(String pagePath) throws Exception {
        Resp r = get(pagePath);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("<meta name=\"csrf-token\" content=\"([^\"]*)\"").matcher(r.body);
        return m.find() ? m.group(1) : null;
    }

    /** Logs in the way the browser does: take the token from the login page, post the form with it. */
    public static Resp login(String email, String password) throws Exception {
        String token = csrf("/login.jsp");
        return post("/AuthController", "action=login&_csrf=" + enc(token) + "&email=" + enc(email) + "&password=" + enc(password));
    }

    public static String enc(String s) throws Exception {
        return URLEncoder.encode(s, "UTF-8");
    }
}
