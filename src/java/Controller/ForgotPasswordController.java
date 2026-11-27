package Controller;

import Model.DAO.UserDAO;
import Model.DTO.User;
import Utils.AuthLimits;
import Utils.Csrf;
import Utils.EmailUtil;
import Utils.RateLimiter;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Quên mật khẩu bằng OTP (T3.5).
 *
 * - OTP sinh bằng SecureRandom, đủ 000000..999999.
 * - Gửi OTP bị giới hạn theo (IP, email), theo email (trần cộng gộp) và theo IP; cùng một phản hồi dù email có tồn tại hay không.
 * - Số lần nhập sai được đếm cho TỪNG mã đã phát hành, lưu trong session của người yêu cầu: sai quá OTP_MAX_ATTEMPTS lần thì mã
 *   bị hủy và phải xin mã mới. Không còn khóa theo email, nên một phiên khác không khóa được mã của bạn (trần theo email chỉ là giới hạn cộng gộp chống dội mail).
 * - Xác minh thành công đổi id phiên và token CSRF (quyền đặt lại là một thay đổi đặc quyền).
 *
 * Các phản hồi giữ nguyên dạng JSON cũ {status, message, locked?} với HTTP 200 vì forgot_password.jsp đọc thế; T5.5 sẽ đổi sang mã trạng thái thật.
 */
@WebServlet(name = "ForgotPasswordController", urlPatterns = {"/ForgotPasswordController"})
public class ForgotPasswordController extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(ForgotPasswordController.class.getName());
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Gson GSON = new Gson();
    private static final int MAX_EMAIL_LENGTH = 100;

    /** Cách gửi OTP; test thay bằng bản bắt mã thay vì gọi SMTP. */
    private static volatile BiPredicate<String, String> otpSender = new BiPredicate<String, String>() {
        @Override
        public boolean test(String email, String otp) {
            return EmailUtil.sendOTP(email, otp);
        }
    };

    public static void setOtpSenderForTests(BiPredicate<String, String> sender) {
        otpSender = sender;
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String action = request.getParameter("action");
        String email = request.getParameter("email");
        email = email == null ? "" : email.trim();

        if (email.isEmpty()) {
            error(out, "Email không được để trống");
            return;
        }
        if (email.length() > MAX_EMAIL_LENGTH) {
            error(out, "Email không hợp lệ");
            return;
        }

        try {
            if ("sendOTP".equals(action)) {
                sendOtp(request, out, email);
            } else if ("verifyOTP".equals(action)) {
                verifyOtp(request, out, email);
            } else {
                error(out, "Yêu cầu không hợp lệ");
            }
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "forgot-password failed action=" + action, e);
            error(out, "Đã xảy ra lỗi trên máy chủ nội bộ.");
        }
    }

    private void sendOtp(HttpServletRequest request, PrintWriter out, String email) {
        String ip = AuthLimits.clientIp(request);
        String emailKey = AuthLimits.emailKey(email);
        String pair = ip + "|" + emailKey;

        RateLimiter cooldown = AuthLimits.otpCooldown();
        RateLimiter pairHour = AuthLimits.otpPairHour();
        RateLimiter emailHour = AuthLimits.otpEmailHour();
        RateLimiter ipHour = AuthLimits.otpIpHour();

        if (cooldown.isBlocked(pair)) {
            long seconds = (cooldown.retryAfterMs(pair) + 999) / 1000;
            LOG.log(Level.WARNING, "otp send throttled reason=cooldown ip={0}", ip);
            throttled(out, "Vui lòng đợi " + seconds + " giây trước khi yêu cầu mã mới.", seconds);
            return;
        }
        if (pairHour.isBlocked(pair) || emailHour.isBlocked(emailKey) || ipHour.isBlocked(ip)) {
            LOG.log(Level.WARNING, "otp send throttled reason=hourly ip={0}", ip);
            long wait = Math.max(pairHour.retryAfterMs(pair), Math.max(emailHour.retryAfterMs(emailKey), ipHour.retryAfterMs(ip)));
            throttled(out, "Bạn đã yêu cầu mã quá nhiều lần. Vui lòng thử lại sau ít phút.", (wait + 999) / 1000);
            return;
        }
        // Đếm trước khi biết email có tồn tại hay không: cùng một hành vi cho cả hai trường hợp.
        cooldown.hit(pair);
        pairHour.hit(pair);
        emailHour.hit(emailKey);
        ipHour.hit(ip);

        User user = new UserDAO().getUserByEmail(email);

        // BR-18: luôn trả về thành công để không dò được email nào có tài khoản.
        if (user != null) {
            String otp = String.format("%06d", RANDOM.nextInt(1000000));
            if (otpSender.test(email, otp)) {
                HttpSession session = request.getSession();
                session.setAttribute("RECOVERY_OTP_" + email, otp);
                session.setAttribute("RECOVERY_TIME_" + email, System.currentTimeMillis());
                session.setAttribute("RECOVERY_ATTEMPTS_" + email, 0);
                session.removeAttribute("ALLOW_RESET_" + email);
            } else {
                LOG.log(Level.WARNING, "otp email could not be sent userId={0}", user.getUserId());
            }
        } else {
            // Giả lập thời gian gửi mail để chống timing attack.
            try {
                Thread.sleep((long) (Math.random() * 500 + 500));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        out.print("{\"status\":\"success\"}");
    }

    private void verifyOtp(HttpServletRequest request, PrintWriter out, String email) {
        String inputOtp = request.getParameter("otp");
        HttpSession session = request.getSession();

        String realOtp = (String) session.getAttribute("RECOVERY_OTP_" + email);
        Long createTime = (Long) session.getAttribute("RECOVERY_TIME_" + email);

        if (realOtp == null || createTime == null) {
            error(out, "Mã OTP không tồn tại hoặc đã bị hủy. Vui lòng bấm \"Gửi lại mã\".");
            return;
        }
        if (System.currentTimeMillis() - createTime > AuthLimits.otpValidMs()) {
            discardOtp(session, email);
            error(out, "Mã OTP đã hết hạn sau " + (AuthLimits.otpValidMs() / 1000) + " giây. Vui lòng bấm \"Gửi lại mã\".");
            return;
        }

        boolean match = inputOtp != null && MessageDigest.isEqual(
                realOtp.getBytes(StandardCharsets.UTF_8), inputOtp.getBytes(StandardCharsets.UTF_8));
        if (match) {
            discardOtp(session, email);
            session.setAttribute("ALLOW_RESET_" + email, true);
            // Quyền đặt lại là thay đổi đặc quyền: đổi id phiên và token CSRF.
            request.changeSessionId();
            Csrf.rotate(session);
            out.print("{\"status\":\"success\"}");
            return;
        }

        Integer previous = (Integer) session.getAttribute("RECOVERY_ATTEMPTS_" + email);
        int attempts = (previous == null ? 0 : previous) + 1;
        int max = AuthLimits.otpMaxAttempts();
        if (attempts >= max) {
            discardOtp(session, email);
            LOG.log(Level.WARNING, "otp discarded after {0} wrong attempts ip={1}", new Object[]{attempts, AuthLimits.clientIp(request)});
            error(out, "Bạn đã nhập sai " + max + " lần. Mã đã bị hủy, vui lòng bấm \"Gửi lại mã\" để nhận mã mới.");
        } else {
            session.setAttribute("RECOVERY_ATTEMPTS_" + email, attempts);
            error(out, "Mã OTP không đúng. Bạn còn " + (max - attempts) + " lần thử.");
        }
    }

    private static void discardOtp(HttpSession session, String email) {
        session.removeAttribute("RECOVERY_OTP_" + email);
        session.removeAttribute("RECOVERY_TIME_" + email);
        session.removeAttribute("RECOVERY_ATTEMPTS_" + email);
    }

    private static void error(PrintWriter out, String message) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("status", "error");
        body.put("message", message);
        out.print(GSON.toJson(body));
    }

    private static void throttled(PrintWriter out, String message, long retryAfterSeconds) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("status", "error");
        body.put("throttled", true);
        body.put("retryAfterSeconds", retryAfterSeconds);
        body.put("message", message);
        out.print(GSON.toJson(body));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }
}
