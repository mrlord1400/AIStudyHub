package Controller;

import DAO.ChatMessageDAO;
import Model.DTO.ChatSession;
import Model.DAO.ChatSessionDAO;
import Model.DTO.ChatMessage;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet(name = "SessionController", urlPatterns = {"/SessionController"})
public class SessionController extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        if (action == null) {
            action = "chatMain";
        }

        try {
            switch (action) {
                case "chatMain":
                    handleChatMain(request, response);
                    break;

                case "createSession":
                    handleCreateSession(request, response);
                    break;

                case "viewSession":
                    handleViewSession(request, response);
                    break;

                case "updateSessionName":
                    handleUpdateSessionName(request, response);
                    break;

                case "deleteSession":
                    handleDeleteSession(request, response);
                    break;

                case "findSessionByName":
                    handleFindSessionByName(request, response);
                    break;
                case "pinSession":
                    handlePinSession(request, response);
                    break;
                default:
                    response.sendRedirect(request.getContextPath() + "/SessionController?action=chatMain");
                    break;
            }
        } catch (Exception e) {
            System.err.println("[SessionController Error] " + e.getMessage());
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=system_error");
        }
    }

    private void handleChatMain(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        int userId = (int) session.getAttribute("userId");
        ChatSessionDAO dao = new ChatSessionDAO();

        List<ChatSession> chatHistory = dao.getAllSessionsByUserId(userId);
        request.setAttribute("chatHistory", chatHistory);

        request.getRequestDispatcher("/chat_main.jsp").forward(request, response);
    }

    private void handleViewSession(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        int userId = (int) session.getAttribute("userId");
        String sessionIdStr = request.getParameter("sessionId");

        try {
            int sessionId = Integer.parseInt(sessionIdStr);
            ChatSessionDAO dao = new ChatSessionDAO();

            ChatSession currentChat = dao.getSessionById(sessionId);

            if (currentChat == null || currentChat.getUserId() != userId) {
                response.sendRedirect(request.getContextPath() + "/SessionController?action=chatMain&error=unauthorized");
                return;
            }

            List<ChatSession> chatHistory = dao.getAllSessionsByUserId(userId);
            ChatMessageDAO messageDao = new ChatMessageDAO();
            List<ChatMessage> messageList = messageDao.getAllDisplayableMessage(sessionId);

            // BẮT FILE ĐÍNH KÈM NẾU CÓ TỪ UPLOAD CONTROLLER TRẢ VỀ QUA SESSION (Bảo vệ lỗi font)
            String isAttached = request.getParameter("attached");
            if ("true".equals(isAttached)) {
                String attachedDoc = (String) request.getSession().getAttribute("newAttachedDocTitle");
                if (attachedDoc != null) {
                    request.setAttribute("attachedDocName", attachedDoc);
                    request.getSession().removeAttribute("newAttachedDocTitle"); // Dọn dẹp
                }
            }

            request.setAttribute("messageList", messageList);
            request.setAttribute("currentChat", currentChat);
            request.setAttribute("chatHistory", chatHistory);
            request.getRequestDispatcher("/chat_session.jsp").forward(request, response);

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/SessionController?action=chatMain&error=invalid_id");
        }
    }

    private void handleCreateSession(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        int userId = (int) session.getAttribute("userId");
        String sessionName = request.getParameter("sessionName");

        if (sessionName == null || sessionName.trim().isEmpty()) {
            sessionName = "Cuộc trò chuyện mới";
        }

        ChatSessionDAO dao = new ChatSessionDAO();
        ChatSession newChatSession = dao.createSession(sessionName, userId);

        if (newChatSession != null) {
            response.sendRedirect(request.getContextPath() + "/SessionController?action=viewSession&sessionId=" + newChatSession.getSessionId());
        } else {
            response.sendRedirect(request.getContextPath() + "/SessionController?action=chatMain&error=create_failed");
        }
    }

    private void handleUpdateSessionName(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            out.print("{\"success\": false, \"message\": \"Vui lòng đăng nhập\"}");
            return;
        }

        String sessionIdStr = request.getParameter("sessionId");
        String sessionNewName = request.getParameter("sessionNewName");

        if (sessionIdStr == null || sessionNewName == null || sessionNewName.trim().isEmpty()) {
            out.print("{\"success\": false, \"message\": \"Tên không hợp lệ\"}");
            return;
        }

        try {
            int sessionId = Integer.parseInt(sessionIdStr);

            ChatSession sessionToUpdate = new ChatSession();
            sessionToUpdate.setSessionId(sessionId);
            sessionToUpdate.setSessionName(sessionNewName.trim());

            ChatSessionDAO dao = new ChatSessionDAO();
            boolean success = dao.updateSessionName(sessionToUpdate);

            if (success) {
                out.print("{\"success\": true}");
            } else {
                out.print("{\"success\": false, \"message\": \"Lỗi cập nhật CSDL\"}");
            }
        } catch (NumberFormatException e) {
            out.print("{\"success\": false, \"message\": \"ID không hợp lệ\"}");
        }
    }

    private void handleDeleteSession(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            out.print("{\"success\": false}");
            return;
        }

        int userId = (int) session.getAttribute("userId");
        String sessionIdStr = request.getParameter("sessionId");
        String sessionName = request.getParameter("sessionName");

        try {
            int sessionId = Integer.parseInt(sessionIdStr);

            ChatSession sessionToDelete = new ChatSession();
            sessionToDelete.setSessionId(sessionId);
            sessionToDelete.setSessionName(sessionName);
            sessionToDelete.setUserId(userId);

            ChatSessionDAO dao = new ChatSessionDAO();
            boolean success = dao.deleteSession(sessionToDelete);

            out.print("{\"success\": " + success + "}");
        } catch (NumberFormatException e) {
            out.print("{\"success\": false}");
        }
    }

    private void handleFindSessionByName(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            out.print("[]");
            return;
        }

        String searchName = request.getParameter("sessionName");
        if (searchName == null) {
            searchName = "";
        }

        ChatSessionDAO dao = new ChatSessionDAO();
        List<ChatSession> searchResults = dao.findSessionByName(searchName);

        StringBuilder jsonBuilder = new StringBuilder("[");
        for (int i = 0; i < searchResults.size(); i++) {
            ChatSession s = searchResults.get(i);
            jsonBuilder.append("{\"sessionId\":").append(s.getSessionId())
                    .append(", \"sessionName\":\"").append(s.getSessionName().replace("\"", "\\\""))
                    .append("\"}");
            if (i < searchResults.size() - 1) {
                jsonBuilder.append(",");
            }
        }
        jsonBuilder.append("]");
        out.print(jsonBuilder.toString());
    }

    // ADD this new private method alongside the other handle* methods
    private void handlePinSession(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            out.print("{\"success\": false, \"message\": \"Vui lòng đăng nhập\"}");
            return;
        }

        String sessionIdStr = request.getParameter("sessionId");
        String pinnedStr = request.getParameter("isPinned"); // "true" or "false"

        if (sessionIdStr == null || pinnedStr == null) {
            out.print("{\"success\": false, \"message\": \"Dữ liệu không hợp lệ\"}");
            return;
        }

        try {
            int sessionId = Integer.parseInt(sessionIdStr);
            boolean isPinned = Boolean.parseBoolean(pinnedStr);

            ChatSession sessionToPin = new ChatSession();
            sessionToPin.setSessionId(sessionId);
            sessionToPin.setPinned(isPinned);

            ChatSessionDAO dao = new ChatSessionDAO();
            boolean success = dao.pinSession(sessionToPin);

            if (success) {
                out.print("{\"success\": true, \"isPinned\": " + isPinned + "}");
            } else {
                out.print("{\"success\": false, \"message\": \"Lỗi cập nhật CSDL\"}");
            }
        } catch (NumberFormatException e) {
            out.print("{\"success\": false, \"message\": \"ID không hợp lệ\"}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
