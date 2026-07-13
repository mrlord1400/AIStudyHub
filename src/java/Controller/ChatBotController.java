package Controller;

import Model.DAO.ChatMessageDAO;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
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

    private static final int MAX_AI_LOOP = 5;

    // Danh sách các định dạng file mà hệ thống hiện tại hỗ trợ trích xuất văn bản
    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList("pdf", "docx", "txt", "xlsx", "pptx", "md");

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

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); 
            out.print("Vui lòng đăng nhập để sử dụng AI.");
            return;
        }

        int userId = (int) session.getAttribute("userId");

        String userMessage = request.getParameter("message");
        String attachment = request.getParameter("attachment"); 
        String sessionIdStr = request.getParameter("sessionId");

        boolean hasMessage = userMessage != null && !userMessage.trim().isEmpty();
        boolean hasAttachment = attachment != null && !attachment.trim().isEmpty();

        if ((!hasMessage && !hasAttachment) || sessionIdStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); 
            out.print("Dữ liệu không hợp lệ.");
            return;
        }

        try {
            int sessionId = Integer.parseInt(sessionIdStr);

            User user = userDAO.getUserById(userId);
            if (user == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("Tài khoản người dùng không tồn tại.");
                return;
            }

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

            int finalPromptsToday = currentPrompts;
            Timestamp finalResetTs = lastResetTs;

            if (lastResetTs == null) {
                finalPromptsToday = 0;
                finalResetTs = Timestamp.valueOf(now);
            } else {
                LocalDateTime lastResetTime = lastResetTs.toLocalDateTime();
                long hoursSinceReset = Duration.between(lastResetTime, now).toHours();

                if (hoursSinceReset >= 24) {
                    finalPromptsToday = 0;
                    finalResetTs = Timestamp.valueOf(now);
                }
            }

            if (finalPromptsToday >= limitPerDay) {
                LocalDateTime nextResetTime = finalResetTs.toLocalDateTime().plusDays(1);
                Duration cooldown = Duration.between(now, nextResetTime);
                long hoursLeft = cooldown.toHours();
                long minsLeft = cooldown.toMinutes() % 60;

                response.setStatus(429); 
                out.print("Hết lượt câu hỏi! Gói của bạn tối đa " + limitPerDay + " câu/ngày.\nThời gian hồi lượt tiếp theo còn: " + hoursLeft + " giờ " + minsLeft + " phút.");
                return;
            }

            finalPromptsToday += 1;
            userDAO.updateAiUsage(userId, finalPromptsToday, finalResetTs);

            // --- BƯỚC 3: XỬ LÝ LƯU THÔNG TIN TIN NHẮN ĐỂ LẤY ID THẬT ---
            int firstInsertedId = -1;

            if (hasMessage) {
                firstInsertedId = chatMessageDAO.createUserMessage(userMessage, sessionId);
                if (firstInsertedId == -1) {
                    throw new Exception("Không thể lưu tin nhắn của người dùng vào CSDL.");
                }
            }

            if (hasAttachment) {
                int sysMsgId = chatMessageDAO.handleAttachmentIfPresent(attachment, userMessage, sessionId);
                if (sysMsgId == -1) {
                    throw new Exception("Không thể lưu thông tin tài liệu đính kèm vào CSDL.");
                }
                if (firstInsertedId == -1) {
                    firstInsertedId = sysMsgId;
                }
            }

            List<ChatMessage> chatHistory = chatMessageDAO.getAllMessageFromSession(sessionId);

            String aiResponse = geminiService.getGeminiResponse(chatHistory);

            int loopCount = 0;
            String finalResponse = null;

            while (loopCount < MAX_AI_LOOP) {
                loopCount++;
                String trimmedResponse = aiResponse.trim();

                if (trimmedResponse.toUpperCase().startsWith("RESPONSE:")) {
                    finalResponse = trimmedResponse.substring("RESPONSE:".length()).trim();
                    break;

                } else if (trimmedResponse.toUpperCase().startsWith("SEARCH")) {
                    chatMessageDAO.createNonDisplayBotMessage(trimmedResponse, sessionId);

                    List<Folder> allFolders = folderDAO.getAllFoldersByUserId(userId);
                    List<Document> allDocuments = documentDAO.getDocumentsByUserId(userId);
                    String folderTree = folderDAO.buildFolderTree(allFolders, allDocuments);

                    String treeMessage = "Đây là cấu trúc cây tài liệu hiện tại của sinh viên:\n" + folderTree;
                    chatMessageDAO.createSystemMessage(treeMessage, sessionId);

                    chatHistory = chatMessageDAO.getAllMessageFromSession(sessionId);
                    aiResponse = geminiService.getGeminiResponse(chatHistory);

                } else if (trimmedResponse.toUpperCase().startsWith("VIEW/") || trimmedResponse.toUpperCase().startsWith("VIEW /")) {
                    chatMessageDAO.createNonDisplayBotMessage(trimmedResponse, sessionId);
                    try {
                        String docId = trimmedResponse.substring(trimmedResponse.indexOf("/") + 1).trim();
                        Integer docIdInt = Integer.parseInt(docId);
                        String systemMessage;
                        Document foundDoc = null;
                        
                        if (docIdInt != null) {
                            foundDoc = documentDAO.findById(docIdInt);
                        }
                        
                        if (foundDoc == null) {
                            systemMessage = "Hệ thống không tìm thấy tài liệu có id \"" + docId
                                    + "\" trong kho lưu trữ của sinh viên. "
                                    + "Hãy thông báo cho sinh viên biết và hỏi lại tên chính xác.";
                        } else {
                            String parsingStatus = foundDoc.getAiParsingStatus();
                            String ext = foundDoc.getFileExtension() != null ? foundDoc.getFileExtension().toLowerCase() : "";

                            // MÀNG LỌC 1: Kiểm tra đuôi file cứng (Bảo vệ lỗi kẹt Processing)
                            if (!SUPPORTED_EXTENSIONS.contains(ext)) {
                                systemMessage = "Tài liệu \"" + foundDoc.getTitle() + "\" có định dạng (." + ext 
                                        + ") không được hệ thống hỗ trợ đọc chữ. "
                                        + "Hãy xin lỗi và thông báo cho sinh viên biết AI hiện tại chỉ hỗ trợ: PDF, DOCX, TXT, XLSX, PPTX.";
                            } 
                            // MÀNG LỌC 2: Kiểm tra trạng thái FAILED rõ ràng từ CSDL
                            else if ("FAILED".equalsIgnoreCase(parsingStatus) || "ERROR".equalsIgnoreCase(parsingStatus)) {
                                systemMessage = "Tài liệu \"" + foundDoc.getTitle() + "\" đã bị lỗi trong quá trình trích xuất nội dung (Trạng thái: FAILED). "
                                        + "Hãy xin lỗi sinh viên, giải thích rằng file có thể bị hỏng, cài mật khẩu, hoặc định dạng bị lỗi, và khuyên họ tải lên file chuẩn khác.";
                            } 
                            // MÀNG LỌC 3: File đang xử lý
                            else if (!"READY".equalsIgnoreCase(parsingStatus)) {
                                systemMessage = "Tài liệu \"" + foundDoc.getTitle() + "\" đang trong quá trình trích xuất văn bản "
                                        + "(trạng thái hiện tại: " + parsingStatus + "). "
                                        + "Hãy báo cho sinh viên vui lòng đợi vài giây và gửi lại yêu cầu để kiểm tra lại nội dung.";
                            } 
                            // MÀNG LỌC 4: Lấy nội dung
                            else {
                                String extractedText = documentTextDAO.getExtractedText(foundDoc.getDocumentId());

                                if (extractedText == null || extractedText.trim().isEmpty()) {
                                    systemMessage = "Tài liệu \"" + foundDoc.getTitle() 
                                            + "\" được tìm thấy nhưng nội dung trống rỗng hoặc hệ thống không thể quét được chữ (có thể là file chỉ toàn ảnh). "
                                            + "Hãy thông báo cho sinh viên biết vấn đề này.";
                                } else {
                                    systemMessage = "Đây là nội dung tài liệu \"" + foundDoc.getTitle()
                                            + "\" mà sinh viên yêu cầu:\n\n"
                                            + extractedText
                                            + "\n\nDựa trên nội dung trên, hãy trả lời câu hỏi của sinh viên.";
                                }
                            }
                        }

                        chatMessageDAO.createSystemMessage(systemMessage, sessionId);
                    } catch (Exception ex) {
                        chatMessageDAO.createSystemMessage("Lỗi định dạng lệnh: ID tài liệu phải là số. Ví dụ: VIEW/123", sessionId);
                    }
                    chatHistory = chatMessageDAO.getAllMessageFromSession(sessionId);
                    aiResponse = geminiService.getGeminiResponse(chatHistory);

                } else if (trimmedResponse.toUpperCase().startsWith("TODAY")) {
                    chatMessageDAO.createNonDisplayBotMessage(trimmedResponse, sessionId);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    String today = now.format(formatter);
                    String timeMessage = "Đây là thời gian của hiện tại:\n" + today;

                    chatMessageDAO.createSystemMessage(timeMessage, sessionId);

                    chatHistory = chatMessageDAO.getAllMessageFromSession(sessionId);
                    aiResponse = geminiService.getGeminiResponse(chatHistory);
                    
                } else if (trimmedResponse.toUpperCase().startsWith("GETLINK/") || trimmedResponse.toUpperCase().startsWith("GETLINK /")) {
                    chatMessageDAO.createNonDisplayBotMessage(trimmedResponse, sessionId);
                    try {
                        String folderId = trimmedResponse.substring(trimmedResponse.indexOf("/") + 1).trim();

                        Integer folderIdInt = Integer.parseInt(folderId);
                        String contextPath = request.getContextPath();
                        String folderUrl = Utils.LinkUtil.getFolderUrl(contextPath, folderIdInt);
                        String systemMessage;
                        if (folderDAO.getFolderById(folderIdInt) != null) {
                            systemMessage = "Thư mục có id \"" + folderId
                                    + "\" đã được tìm thấy tại đường link: " + folderUrl
                                    + "\n Hãy thông báo lại với học sinh và gửi đường link cho học sinh.";
                        } else {
                            systemMessage = "Hệ thống không tìm thấy thư mục có id \"" + folderId
                                    + "\" trong kho lưu trữ của sinh viên. "
                                    + "Hãy kiểm tra lại cấu trúc folder tree của học sinh hiện tại.";
                        }

                        chatMessageDAO.createSystemMessage(systemMessage, sessionId);
                    } catch (Exception ex) {
                        chatMessageDAO.createSystemMessage("Lỗi định dạng lệnh: ID folder phải là số. Ví dụ: GETLINK/123", sessionId);
                    }
                    chatHistory = chatMessageDAO.getAllMessageFromSession(sessionId);
                    aiResponse = geminiService.getGeminiResponse(chatHistory);
                    
                } else {
                    System.err.println("[ChatBotController] AI response không đúng format: " + trimmedResponse.substring(0, Math.min(50, trimmedResponse.length())));
                    finalResponse = aiResponse;
                    break;
                }
            }

            if (finalResponse == null) {
                System.err.println("[ChatBotController] AI loop vượt quá " + MAX_AI_LOOP + " lần.");
                finalResponse = "Xin lỗi, hệ thống AI đang gặp sự cố xử lý. Vui lòng thử lại sau.";
            }

            boolean isBotMsgSaved = chatMessageDAO.createBotMessage(finalResponse, sessionId);
            if (!isBotMsgSaved) {
                System.err.println("[Cảnh báo] Trả lời AI thành công nhưng lỗi lưu CSDL!");
            }

            // GẮN ID THẬT VÀO CUSTOM HEADER TRƯỚC KHI FLUSH TEXT ĐỂ JS BẮT ĐƯỢC
            if (firstInsertedId != -1) {
                response.setHeader("X-Message-Id", String.valueOf(firstInsertedId));
            }

            out.print(finalResponse);

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("ID Phiên trò chuyện không hợp lệ.");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); 
            out.print("Đã xảy ra lỗi hệ thống từ máy chủ AI: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/SessionController?action=chatMain");
    }
}