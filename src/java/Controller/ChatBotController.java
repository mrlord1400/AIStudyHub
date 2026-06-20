package Controller;

import DAO.ChatMessageDAO;
import Model.DAO.UserDAO;
import Model.DAO.SubscriptionDAO;
import Model.DTO.ChatMessage;
import Model.DTO.User;
import Model.DTO.Subscription;
import Utils.GeminiService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@WebServlet(name = "ChatBotController", urlPatterns = {"/ChatBotController"})
public class ChatBotController extends HttpServlet {

    private GeminiService geminiService;
    private ChatMessageDAO chatMessageDAO;
    private UserDAO userDAO;
    private SubscriptionDAO subscriptionDAO;

    @Override
    public void init() throws ServletException {
        geminiService = new GeminiService();
        chatMessageDAO = new ChatMessageDAO();
        userDAO = new UserDAO();
        subscriptionDAO = new SubscriptionDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain; charset=UTF-8");
        PrintWriter out = response.getWriter();

        // 1. Xác thực quyền truy cập
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            out.print("Vui lòng đăng nhập để sử dụng AI.");
            return;
        }

        int userId = (int) session.getAttribute("userId");

        // 2. Lấy dữ liệu từ Frontend
        String userMessage = request.getParameter("message");
        String sessionIdStr = request.getParameter("sessionId");

        if (userMessage == null || userMessage.trim().isEmpty() || sessionIdStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
            out.print("Dữ liệu không hợp lệ.");
            return;
        }

        try {
            int sessionId = Integer.parseInt(sessionIdStr);

            // 🚀 BƯỚC THÊM MỚI: KIỂM TRA GIỚI HẠN AI PROMPT (RATE LIMIT 24H)
            User user = userDAO.getUserById(userId);
            if (user == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("Tài khoản người dùng không tồn tại.");
                return;
            }

            // Lấy thông số gói dịch vụ của người dùng
            Subscription userSub = null;
            List<Subscription> allSubs = subscriptionDAO.getAllSubscriptions();
            if (allSubs != null) {
                for (Subscription s : allSubs) {
                    if (s.getTierId() == user.getTierId()) {
                        userSub = s;
                        break;
                    }
                }
            }

            if (userSub == null) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("Không thể xác định gói cấu hình dịch vụ của bạn.");
                return;
            }

            int limitPerDay = userSub.getAiPromptLimitPerDay();
            int currentPrompts = user.getAiPromptsToday();
            Timestamp lastResetTs = user.getLastPromptReset();
            LocalDateTime now = LocalDateTime.now();

            // Biến kiểm soát số lượt thực tế sau khi tính toán chu kỳ 24h
            int finalPromptsToday = currentPrompts;
            Timestamp finalResetTs = lastResetTs;

            if (lastResetTs == null) {
                // Nếu chưa bao giờ chat, khởi tạo mốc thời gian chu kỳ mới
                finalPromptsToday = 0;
                finalResetTs = Timestamp.valueOf(now);
            } else {
                LocalDateTime lastResetTime = lastResetTs.toLocalDateTime();
                long hoursSinceReset = Duration.between(lastResetTime, now).toHours();

                if (hoursSinceReset >= 24) {
                    // Nếu thời gian chat hiện tại đã vượt qua 24 tiếng -> Reset về chu kỳ mới hoàn toàn
                    finalPromptsToday = 0;
                    finalResetTs = Timestamp.valueOf(now);
                }
            }

            // Kiểm tra xem số câu hỏi trong chu kỳ hiện tại đã vượt ngưỡng của gói chưa
            if (finalPromptsToday >= limitPerDay) {
                // Tính toán chính xác thời gian hồi chiêu còn lại (đếm ngược thời gian)
                LocalDateTime nextResetTime = finalResetTs.toLocalDateTime().plusDays(1);
                Duration cooldown = Duration.between(now, nextResetTime);
                long hoursLeft = cooldown.toHours();
                long minsLeft = cooldown.toMinutes() % 60;

                response.setStatus(429); // HTTP Status 429: Too Many Requests
                out.print("Hết lượt câu hỏi! Gói của bạn tối đa " + limitPerDay + " câu/ngày.\nThời gian hồi lượt tiếp theo còn: " + hoursLeft + " giờ " + minsLeft + " phút.");
                return; // Ngắt luồng, chặn không cho gọi API Gemini
            }

            // Cập nhật tăng số lượt đếm lên 1 trước khi xử lý gọi AI
            finalPromptsToday += 1;
            userDAO.updateAiUsage(userId, finalPromptsToday, finalResetTs);


            // BƯỚC 3: LƯU TIN NHẮN CỦA USER VÀO CƠ SỞ DỮ LIỆU
            boolean isUserMsgSaved = chatMessageDAO.createUserMessage(userMessage, sessionId);
            if (!isUserMsgSaved) {
                throw new Exception("Không thể lưu tin nhắn của người dùng vào CSDL.");
            }

            // BƯỚC 4: LẤY TOÀN BỘ LỊCH SỬ CỦA SESSION NÀY
            List<ChatMessage> chatHistory = chatMessageDAO.getAllMessageFromSession(sessionId);

            // BƯỚC 5: GỬI LỊCH SỬ LÊN GEMINI API ĐỂ LẤY CÂU TRẢ LỜI
            String aiResponse = geminiService.getGeminiResponse(chatHistory);
            
            if (aiResponse.toUpperCase().indexOf("RESPONSE:") >= 0 && aiResponse.toUpperCase().indexOf("RESPONSE:") <= 1){
                //Insert "RESPONSE:" logic here
            } else if (aiResponse.toUpperCase().indexOf("SEARCH") >= 0 && aiResponse.toUpperCase().indexOf("SEARCH") <= 4){
                //Insert "SEARCH" logic here
            } else if (aiResponse.toUpperCase().indexOf("VIEW") >= 0 && aiResponse.toUpperCase().indexOf("VIEW") <= 6){
                //Insert "VIEW" logic here
            } else {
                // Default logic
            }
            
            // BƯỚC 6: LƯU CÂU TRẢ LỜI CỦA AI VÀO CƠ SỞ DỮ LIỆU
            boolean isBotMsgSaved = chatMessageDAO.createBotMessage(aiResponse, sessionId);
            if (!isBotMsgSaved) {
                System.err.println("[Cảnh báo] Trả lời AI thành công nhưng lỗi lưu CSDL!");
            }

            // BƯỚC 7: TRẢ KẾT QUẢ VỀ CHO FRONTEND
            out.print(aiResponse);

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("ID Phiên trò chuyện không hợp lệ.");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500
            out.print("Đã xảy ra lỗi hệ thống từ máy chủ AI: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/SessionController?action=chatMain");
    }
}