package Controller;

import DAO.ChatMessageDAO;
import Model.DTO.ChatMessage;
import Utils.GeminiService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet(name = "ChatBotController", urlPatterns = {"/ChatBotController"})
public class ChatBotController extends HttpServlet {

    private GeminiService geminiService;
    private ChatMessageDAO chatMessageDAO;

    @Override
    public void init() throws ServletException {
        // Khởi tạo các Service và DAO một lần khi Servlet được load vào bộ nhớ
        geminiService = new GeminiService();
        chatMessageDAO = new ChatMessageDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // 1. Cấu hình định dạng tiếng Việt
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain; charset=UTF-8");
        PrintWriter out = response.getWriter();

        // 2. Xác thực quyền truy cập (Kiểm tra đăng nhập)
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // Mã 401
            out.print("Vui lòng đăng nhập để sử dụng AI.");
            return;
        }

        // 3. Lấy dữ liệu từ Frontend gửi lên
        String userMessage = request.getParameter("message");
        String sessionIdStr = request.getParameter("sessionId");

        // Kiểm tra tính hợp lệ của dữ liệu đầu vào
        if (userMessage == null || userMessage.trim().isEmpty() || sessionIdStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // Mã 400
            out.print("Dữ liệu không hợp lệ.");
            return;
        }

        try {
            int sessionId = Integer.parseInt(sessionIdStr);

            // BƯỚC 1: LƯU TIN NHẮN CỦA USER VÀO CƠ SỞ DỮ LIỆU
            boolean isUserMsgSaved = chatMessageDAO.createUserMessage(userMessage, sessionId);
            if (!isUserMsgSaved) {
                throw new Exception("Không thể lưu tin nhắn của người dùng vào CSDL.");
            }

            // BƯỚC 2: LẤY TOÀN BỘ LỊCH SỬ CỦA SESSION NÀY
            // (Lịch sử lúc này đã bao gồm cả câu hỏi user vừa hỏi ở Bước 1)
            List<ChatMessage> chatHistory = chatMessageDAO.getAllMessageFromSession(sessionId);

            // BƯỚC 3: GỬI LỊCH SỬ LÊN GEMINI API ĐỂ LẤY CÂU TRẢ LỜI
            String aiResponse = geminiService.getGeminiResponse(chatHistory);
            
            if (aiResponse.toUpperCase().indexOf("RESPONSE:") >=0 && aiResponse.toUpperCase().indexOf("RESPONSE:") <=1){
                //Insert "RESPONSE:" logic here
                
            } else if (aiResponse.toUpperCase().indexOf("SEARCH") >=0 && aiResponse.toUpperCase().indexOf("SEARCH") <=4){
                //Insert "SEARCH" logic here
                
            } else if (aiResponse.toUpperCase().indexOf("VIEW") >=0 && aiResponse.toUpperCase().indexOf("VIEW") <=6){
                //Insert "VIEW" logic here
                
            } else {
                
            }
            
            // BƯỚC 4: LƯU CÂU TRẢ LỜI CỦA AI VÀO CƠ SỞ DỮ LIỆU
            boolean isBotMsgSaved = chatMessageDAO.createBotMessage(aiResponse, sessionId);
            if (!isBotMsgSaved) {
                System.err.println("[Cảnh báo] Trả lời AI thành công nhưng lỗi lưu CSDL!");
            }

            // BƯỚC 5: TRẢ KẾT QUẢ VỀ CHO FRONTEND (JSP hiển thị ra màn hình)
            out.print(aiResponse);

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("ID Phiên trò chuyện không hợp lệ.");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // Mã 500
            out.print("Đã xảy ra lỗi hệ thống từ máy chủ AI: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // AI Chatbot chỉ nhận yêu cầu dạng POST qua API, nếu gọi GET thì chuyển hướng đi chỗ khác
        response.sendRedirect(request.getContextPath() + "/SessionController?action=chatMain");
    }
}