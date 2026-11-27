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
    private static int port;
    public static final String CONTEXT = "/AIStudyHub";
    private static final CookieManager COOKIES = new CookieManager();

    /** @param webDir the repo's web/ folder; @param classesDir compiled src/java (what WEB-INF/classes would hold) */
    public static void start(String webDir, String classesDir) throws Exception {
        Path base = Files.createTempDirectory("aish-tomcat");
        tomcat = new Tomcat();
        tomcat.setBaseDir(base.toString());
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
    }

    public static void forgetCookies() {
        COOKIES.getCookieStore().removeAll();
    }

    public static Resp get(String path) throws Exception {
        return send("GET", path, null);
    }

    public static Resp post(String path, String form) throws Exception {
        return send("POST", path, form);
    }

    private static Resp send(String method, String path, String form) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL("http://localhost:" + port + CONTEXT + path).openConnection();
        c.setInstanceFollowRedirects(false);
        c.setRequestMethod(method);
        if (form != null) {
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

    public static String enc(String s) throws Exception {
        return URLEncoder.encode(s, "UTF-8");
    }
}
