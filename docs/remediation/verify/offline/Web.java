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
        public java.util.Map<String, java.util.List<String>> headers = new java.util.HashMap<>();

        public String header(String name) {
            for (java.util.Map.Entry<String, java.util.List<String>> e : headers.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(name) && !e.getValue().isEmpty()) {
                    return e.getValue().get(0);
                }
            }
            return null;
        }
    }

    private static Tomcat tomcat;
    private static Context webapp;
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
        webapp = ctx;
        tomcat.start();
        port = tomcat.getConnector().getLocalPort();
        CookieHandler.setDefault(COOKIES);
    }

    /**
     * Calls a static method on the class as the WEB APP loaded it. Tomcat's webapp class loader keeps its own copies of
     * the controllers and Utils, so state set on the test JVM's copies (limiters, hooks) would not reach the app.
     */
    public static Object invokeInApp(String className, String method, Class<?>[] types, Object... args) throws Exception {
        ClassLoader cl = webapp.getLoader().getClassLoader();
        Class<?> c = Class.forName(className, true, cl);
        return c.getMethod(method, types).invoke(null, args);
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

    /** Value of the JSESSIONID cookie the client currently holds (null if none). */
    public static String sessionId() {
        for (HttpCookie c : COOKIES.getCookieStore().getCookies()) {
            if ("JSESSIONID".equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    /** The raw header block of a GET (the JDK client hides Set-Cookie when a CookieHandler is installed). */
    public static String rawHeaders(String path) throws Exception {
        try (java.net.Socket sock = new java.net.Socket("localhost", port)) {
            String crlf = "\r\n";
            sock.getOutputStream().write(("GET " + CONTEXT + path + " HTTP/1.0" + crlf + "Host: localhost" + crlf + crlf).getBytes(StandardCharsets.UTF_8));
            sock.getOutputStream().flush();
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = sock.getInputStream().read(buf)) > 0) {
                b.write(buf, 0, n);
            }
            String all = new String(b.toByteArray(), StandardCharsets.UTF_8);
            int end = all.indexOf(crlf + crlf);
            return end < 0 ? all : all.substring(0, end);
        }
    }

    /** A response read straight from the socket: no cookie jar, every Set-Cookie line visible. */
    public static class Raw {
        public int status;
        public String location;
        public String body = "";
        public java.util.List<String> setCookies = new java.util.ArrayList<>();

        /** The value of a cookie the server just set (null if not set in this response). */
        public String cookie(String name) {
            for (String line : setCookies) {
                if (line.startsWith(name + "=")) {
                    int end = line.indexOf(';');
                    return line.substring(name.length() + 1, end < 0 ? line.length() : end);
                }
            }
            return null;
        }

        /** The full Set-Cookie line for a cookie name (attributes included). */
        public String cookieLine(String name) {
            for (String line : setCookies) {
                if (line.startsWith(name + "=")) {
                    return line;
                }
            }
            return null;
        }
    }

    /** One request without the cookie jar. @param cookie the Cookie header value, or null; @param body form body for POST, or null */
    public static Raw raw(String method, String path, String body, String cookie) throws Exception {
        return rawInternal(method, path, body, cookie, null);
    }

    private static Raw rawInternal(String method, String path, String body, String cookie, String extraHeaderLine) throws Exception {
        try (java.net.Socket sock = new java.net.Socket("localhost", port)) {
            sock.setSoTimeout(30000);
            String crlf = "\r\n";
            StringBuilder req = new StringBuilder();
            req.append(method).append(' ').append(CONTEXT).append(path).append(" HTTP/1.0").append(crlf);
            req.append("Host: localhost:").append(port).append(crlf);
            if (cookie != null) {
                req.append("Cookie: ").append(cookie).append(crlf);
            }
            if (extraHeaderLine != null) {
                req.append(extraHeaderLine).append(crlf);
            }
            byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
            if (body != null) {
                req.append("Content-Type: application/x-www-form-urlencoded").append(crlf);
                req.append("Content-Length: ").append(bytes.length).append(crlf);
            }
            req.append(crlf);
            sock.getOutputStream().write(req.toString().getBytes(StandardCharsets.UTF_8));
            sock.getOutputStream().write(bytes);
            sock.getOutputStream().flush();
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = sock.getInputStream().read(buf)) > 0) {
                b.write(buf, 0, n);
            }
            String all = new String(b.toByteArray(), StandardCharsets.UTF_8);
            int end = all.indexOf(crlf + crlf);
            String head = end < 0 ? all : all.substring(0, end);
            Raw r = new Raw();
            r.body = end < 0 ? "" : all.substring(end + 4);
            String[] lines = head.split("\r?\n");
            r.status = Integer.parseInt(lines[0].split(" ")[1]);
            for (int i = 1; i < lines.length; i++) {
                String l = lines[i];
                if (l.toLowerCase().startsWith("set-cookie:")) {
                    r.setCookies.add(l.substring(11).trim());
                } else if (l.toLowerCase().startsWith("location:")) {
                    r.location = l.substring(9).trim();
                }
            }
            return r;
        }
    }

    public static int port() {
        return port;
    }

    /** The Cookie header value that matches what the jar would send for the app (name=value pairs). */
    public static String cookieHeader() {
        StringBuilder sb = new StringBuilder();
        for (HttpCookie c : COOKIES.getCookieStore().getCookies()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(c.getName()).append('=').append(c.getValue());
        }
        return sb.toString();
    }

    /** Like raw(), with one extra request header line ("Name: value"). */
    public static Raw rawWithHeaders(String method, String path, String body, String cookie, String extraHeaderLine) throws Exception {
        return rawInternal(method, path, body, cookie, extraHeaderLine);
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
        c.setConnectTimeout(30000);
        c.setReadTimeout(30000);
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
        r.headers = c.getHeaderFields();
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
