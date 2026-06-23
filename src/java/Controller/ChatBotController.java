package Controller;

import DAO.ChatMessageDAO;
import Model.DAO.UserDAO;
import Model.DAO.SubscriptionDAO;
import Model.DAO.FolderDAO;
import Model.DAO.DocumentDAO;
import Model.DTO.ChatMessage;
import Model.DTO.User;
import Model.DTO.Subscription;
import Model.DTO.Document;
import Model.DTO.Folder;
import Model.DAO.DocumentTextDAO;
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
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "ChatBotController", urlPatterns = {"/ChatBotController"})
public class ChatBotController extends HttpServlet {

    private GeminiService geminiService;
    private ChatMessageDAO chatMessageDAO;
    private UserDAO userDAO;
    private SubscriptionDAO subscriptionDAO;
    private FolderDAO folderDAO;
    private DocumentDAO documentDAO;
    private DocumentTextDAO documentTextDAO;

    // Giới hạn số lần loop để tránh infinite loop khi AI liên tục trả SEARCH/VIEW
    private static final int MAX_AI_LOOP = 5;

    @Override
    public void init() throws ServletException {
        geminiService = new GeminiService();
        chatMessageDAO = new ChatMessageDAO();
        userDAO = new UserDAO();
        subscriptionDAO = new SubscriptionDAO();
        folderDAO = new FolderDAO();
        documentDAO = new DocumentDAO();
        documentTextDAO = new DocumentTextDAO();
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

            // BƯỚC 5.5: VÒNG LẶP XỬ LÝ AI RESPONSE (SEARCH / VIEW / RESPONSE)
            // AI có thể trả về SEARCH hoặc VIEW, cần xử lý rồi gọi lại Gemini
            // Loop tối đa MAX_AI_LOOP lần để tránh infinite loop
            int loopCount = 0;
            String finalResponse = null;

            while (loopCount < MAX_AI_LOOP) {
                loopCount++;
                String trimmedResponse = aiResponse.trim();

                if (trimmedResponse.toUpperCase().startsWith("RESPONSE:")) {
                    // ══════════════════════════════════════════════════════════
                    // CASE 1: "RESPONSE:" → AI muốn trả lời cho user
                    // ══════════════════════════════════════════════════════════
                    finalResponse = trimmedResponse.substring("RESPONSE:".length()).trim();
                    break; // Thoát loop, có câu trả lời cuối cùng

                } else if (trimmedResponse.toUpperCase().startsWith("SEARCH")) {
                    // ══════════════════════════════════════════════════════════
                    // CASE 2: "SEARCH" → AI cần xem cấu trúc cây tài liệu
                    // ══════════════════════════════════════════════════════════

                    // 2a. Lưu response "SEARCH" của AI vào DB như BOT message
                    chatMessageDAO.createNonDisplayBotMessage(trimmedResponse, sessionId);

                    // 2b. Lấy tất cả folders và documents của user
                    List<Folder> allFolders = folderDAO.getAllFoldersByUserId(userId);
                    List<Document> allDocuments = documentDAO.getDocumentsByUserId(userId);

                    // 2c. Build cây thư mục dạng string
                    String folderTree = buildFolderTree(allFolders, allDocuments);

                    // 2d. Tạo nội dung gửi lại cho AI
                    String treeMessage = "Đây là cấu trúc cây tài liệu của sinh viên:\n" + folderTree;

                    // 2e. Lưu tree data vào DB như USER message (hệ thống gửi thay user)
                    chatMessageDAO.createSystemMessage(treeMessage, sessionId);

                    // 2f. Reload lịch sử và gọi Gemini lần tiếp theo
                    chatHistory = chatMessageDAO.getAllMessageFromSession(sessionId);
                    aiResponse = geminiService.getGeminiResponse(chatHistory);
                    // Quay lại đầu while loop để kiểm tra response mới

                } else if (trimmedResponse.toUpperCase().startsWith("VIEW/") || trimmedResponse.toUpperCase().startsWith("VIEW /")) {
                    // ══════════════════════════════════════════════════════════
                    // CASE 3: "VIEW/[Document Name]" → AI cần xem nội dung tài liệu
                    // ══════════════════════════════════════════════════════════

                    // 3a. Lưu response "VIEW/..." của AI vào DB như BOT message
                    chatMessageDAO.createNonDisplayBotMessage(trimmedResponse, sessionId);

                    // 3b. Extract tên document từ response
                    String docName;
                    if (trimmedResponse.toUpperCase().startsWith("VIEW/")) {
                        docName = trimmedResponse.substring("VIEW/".length()).trim();
                    } else {
                        docName = trimmedResponse.substring("VIEW /".length()).trim();
                    }

                    // 3c. Tìm document theo tên trong DB
                    String systemMessage;
                    Document foundDoc = documentDAO.findByTitleAndUserId(userId, docName);

                    if (foundDoc == null) {
                        // Document not found — unchanged from original
                        systemMessage = "Hệ thống không tìm thấy tài liệu có tên \"" + docName
                                + "\" trong kho lưu trữ của sinh viên. "
                                + "Hãy thông báo cho sinh viên biết và hỏi lại tên chính xác.";
                    } else {
                        String parsingStatus = foundDoc.getAiParsingStatus();

                        if (!"READY".equalsIgnoreCase(parsingStatus)) {
                            // Document not ready — unchanged from original
                            systemMessage = "Tài liệu \"" + foundDoc.getTitle()
                                    + "\" được tìm thấy nhưng chưa sẵn sàng để phân tích "
                                    + "(trạng thái hiện tại: " + parsingStatus + "). "
                                    + "Hãy thông báo cho sinh viên rằng tài liệu đang được xử lý "
                                    + "và yêu cầu họ thử lại sau.";
                        } else {
                            // ─── CHANGED: Instead of sending metadata, now retrieve and send
                            // the actual extracted text content from the document_extracted_text table.
                            // This allows the AI to properly analyze the document content. ──────────
                            String extractedText = documentTextDAO.getExtractedText(foundDoc.getDocumentId());

                            if (extractedText == null || extractedText.trim().isEmpty()) {
                                // Extracted text not found in DB even though status is READY
                                systemMessage = "Tài liệu \"" + foundDoc.getTitle()
                                        + "\" được tìm thấy nhưng nội dung chưa được trích xuất. "
                                        + "Hãy thông báo cho sinh viên rằng tài liệu đang được xử lý "
                                        + "và yêu cầu họ thử lại sau.";
                            } else {
                                // Send extracted text to AI for analysis
                                systemMessage = "Đây là nội dung tài liệu \"" + foundDoc.getTitle()
                                        + "\" mà sinh viên yêu cầu:\n\n"
                                        + extractedText
                                        + "\n\nDựa trên nội dung trên, hãy trả lời câu hỏi của sinh viên.";
                            }
                            // ─────────────────────────────────────────────────────────
                        }
                    }
                    // 3d. Lưu system message vào DB như USER message
                    chatMessageDAO.createSystemMessage(systemMessage, sessionId);

                    // 3e. Reload lịch sử và gọi Gemini lần tiếp theo
                    chatHistory = chatMessageDAO.getAllMessageFromSession(sessionId);
                    aiResponse = geminiService.getGeminiResponse(chatHistory);
                    // Quay lại đầu while loop để kiểm tra response mới

                } else {
                    // ══════════════════════════════════════════════════════════
                    // CASE 4: AI không theo format → thông báo lỗi
                    // ══════════════════════════════════════════════════════════
                    System.err.println("[ChatBotController] AI response không đúng format: " + trimmedResponse.substring(0, Math.min(50, trimmedResponse.length())));
                    finalResponse = aiResponse; // Trả nguyên response để debug
                    break;
                }
            }

            // Nếu loop hết MAX_AI_LOOP mà vẫn chưa có RESPONSE
            if (finalResponse == null) {
                System.err.println("[ChatBotController] AI loop vượt quá " + MAX_AI_LOOP + " lần.");
                finalResponse = "Xin lỗi, hệ thống AI đang gặp sự cố xử lý. Vui lòng thử lại sau.";
            }

            // BƯỚC 6: LƯU CÂU TRẢ LỜI CUỐI CÙNG CỦA AI VÀO CƠ SỞ DỮ LIỆU
            boolean isBotMsgSaved = chatMessageDAO.createBotMessage(finalResponse, sessionId);
            if (!isBotMsgSaved) {
                System.err.println("[Cảnh báo] Trả lời AI thành công nhưng lỗi lưu CSDL!");
            }

            // BƯỚC 7: TRẢ KẾT QUẢ VỀ CHO FRONTEND
            out.print(finalResponse);

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("ID Phiên trò chuyện không hợp lệ.");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500
            out.print("Đã xảy ra lỗi hệ thống từ máy chủ AI: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPER: Build cây thư mục dạng string cho luồng SEARCH
    // ══════════════════════════════════════════════════════════════════════════
    /**
     * Xây dựng cấu trúc cây thư mục dạng string. VD output: - Folder Gốc 1 --
     * Folder Con A --- [Document] BaiGiang.pdf -- Folder Con B - [Document]
     * TaiLieu_Root.docx
     */
    private String buildFolderTree(List<Folder> allFolders, List<Document> allDocuments) {
        StringBuilder tree = new StringBuilder();

        // 1. Tìm tất cả folders gốc (parent_folder_id == null)
        List<Folder> rootFolders = new ArrayList<>();
        for (Folder f : allFolders) {
            if (f.getParentFolderId() == null) {
                rootFolders.add(f);
            }
        }

        // 2. Đệ quy build cây cho từng folder gốc
        for (Folder rootFolder : rootFolders) {
            buildFolderTreeRecursive(tree, rootFolder, allFolders, allDocuments, 1);
        }

        // 3. Thêm documents ở root (folder_id == null) — không thuộc folder nào
        for (Document doc : allDocuments) {
            if (doc.getFolderId() == null) {
                tree.append("- [Document] ").append(doc.getTitle()).append("\n");
            }
        }

        // Nếu user chưa có gì
        if (tree.length() == 0) {
            tree.append("(Trống — Sinh viên chưa có thư mục hoặc tài liệu nào)");
        }

        return tree.toString().trim();
    }

    /**
     * Đệ quy build cây thư mục. Mỗi cấp tăng thêm 1 dấu "-".
     *
     * @param tree StringBuilder đang xây dựng
     * @param currentFolder Folder hiện tại đang xử lý
     * @param allFolders Danh sách tất cả folders của user
     * @param allDocuments Danh sách tất cả documents của user
     * @param depth Cấp độ hiện tại (1 = gốc)
     */
    private void buildFolderTreeRecursive(StringBuilder tree, Folder currentFolder,
            List<Folder> allFolders, List<Document> allDocuments, int depth) {
        // Tạo prefix dấu "-" theo cấp độ
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            prefix.append("-");
        }
        String depthPrefix = prefix.toString();

        // Thêm folder hiện tại vào cây
        tree.append(depthPrefix).append(" ").append(currentFolder.getFolderName()).append("\n");

        // Tìm documents thuộc folder này
        for (Document doc : allDocuments) {
            if (doc.getFolderId() != null && doc.getFolderId() == currentFolder.getFolderId()) {
                tree.append(depthPrefix).append("- [Document] ").append(doc.getTitle()).append("\n");
            }
        }

        // Tìm folders con và đệ quy
        for (Folder child : allFolders) {
            if (child.getParentFolderId() != null && child.getParentFolderId() == currentFolder.getFolderId()) {
                buildFolderTreeRecursive(tree, child, allFolders, allDocuments, depth + 1);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/SessionController?action=chatMain");
    }
}
