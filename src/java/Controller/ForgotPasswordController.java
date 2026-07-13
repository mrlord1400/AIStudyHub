package Controller;

import Model.DAO.UserDAO;
import Model.DTO.User;
import Utils.EmailUtil;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(name = "ForgotPasswordController", urlPatterns = {"/ForgotPasswordController"})
public class ForgotPasswordController extends HttpServlet {

    private static final Map<String, Long> lockMap = new ConcurrentHashMap<>(); 
    private static final Map<String, Integer> attemptMap = new ConcurrentHashMap<>(); 
    
    private static final long LOCK_DURATION = 12 * 60 * 60 * 1000L; // 12 tiếng
    private static final long OTP_VALID_DURATION = 120 * 1000L; // 120 giây

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String action = request.getParameter("action");
        String email = request.getParameter("email");
        
        if (email == null || email.trim().isEmpty()) {
            out.print("{\"status\":\"error\", \"message\":\"Email không được để trống\"}");
            return;
        }

        if (lockMap.containsKey(email)) {
            long unlockTime = lockMap.get(email);
            if (System.currentTimeMillis() < unlockTime) {
                out.print("{\"status\":\"error\", \"locked\":true, \"message\":\"Tính năng tạm khóa do nhập sai quá 3 lần. Vui lòng thử lại sau 12 tiếng.\"}");
                return;
            } else {
                lockMap.remove(email);
                attemptMap.remove(email);
            }
        }

        try {
            if ("sendOTP".equals(action)) {
                UserDAO dao = new UserDAO();
                User user = dao.getUserByEmail(email);

                // BR-18 Update: Luôn trả về thành công để tránh dò quét Email (User Enumeration).
                // Nếu email CÓ TỒN TẠI, tiến hành gửi OTP.
                if (user != null) {
                    String otp = String.format("%06d", new Random().nextInt(999999));
                    boolean isSent = EmailUtil.sendOTP(email, otp);
                    
                    if (isSent) {
                        HttpSession session = request.getSession();
                        session.setAttribute("RECOVERY_OTP_" + email, otp);
                        session.setAttribute("RECOVERY_TIME_" + email, System.currentTimeMillis());
                    } else {
                        // Log nội bộ, không throw lỗi ra frontend
                        System.err.println("Failed to send OTP to: " + email);
                    }
                } else {
                    // Nếu email KHÔNG TỒN TẠI: Giả lập thời gian delay của việc gửi mail để chống Timing Attack.
                    try {
                        Thread.sleep((long) (Math.random() * 500 + 500));
                    } catch (InterruptedException ignored) {}
                }
                
                // Mặc định luôn trả về True cho Client
                out.print("{\"status\":\"success\"}");
                
            } else if ("verifyOTP".equals(action)) {
                String inputOtp = request.getParameter("otp");
                HttpSession session = request.getSession();
                
                String realOtp = (String) session.getAttribute("RECOVERY_OTP_" + email);
                Long createTime = (Long) session.getAttribute("RECOVERY_TIME_" + email);

                if (realOtp == null || createTime == null) {
                    out.print("{\"status\":\"error\", \"message\":\"Mã OTP không tồn tại hoặc đã bị hủy.\"}");
                    return;
                }

                if (System.currentTimeMillis() - createTime > OTP_VALID_DURATION) {
                    out.print("{\"status\":\"error\", \"message\":\"Mã OTP đã hết hạn sau 120 giây.\"}");
                    return;
                }

                if (realOtp.equals(inputOtp)) {
                    attemptMap.remove(email);
                    session.removeAttribute("RECOVERY_OTP_" + email);
                    session.removeAttribute("RECOVERY_TIME_" + email);
                    session.setAttribute("ALLOW_RESET_" + email, true);
                    
                    out.print("{\"status\":\"success\"}");
                } else {
                    int attempts = attemptMap.getOrDefault(email, 0) + 1;
                    attemptMap.put(email, attempts);
                    
                    if (attempts >= 3) {
                        lockMap.put(email, System.currentTimeMillis() + LOCK_DURATION);
                        out.print("{\"status\":\"error\", \"locked\":true, \"message\":\"Nhập sai 3 lần! Khóa tính năng trong 12 tiếng.\"}");
                    } else {
                        out.print("{\"status\":\"error\", \"message\":\"Mã OTP không đúng. Bạn còn " + (3 - attempts) + " lần thử.\"}");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"status\":\"error\", \"message\":\"Đã xảy ra lỗi trên máy chủ nội bộ.\"}");
        }
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