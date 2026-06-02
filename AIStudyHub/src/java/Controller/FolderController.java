package Controller;

import Model.Folder;
import Model.FolderDAO;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;

@WebServlet(name = "FolderController", urlPatterns = {"/FolderController"})
public class FolderController extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        
        // 1. Security Check
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        int userId = (int) session.getAttribute("userId");
        String action = request.getParameter("action");
        FolderDAO dao = new FolderDAO();

        // Check if we are currently inside a folder so we can redirect back to it
        String currentFolderId = request.getParameter("currentFolderId");
        String redirectUrl = request.getContextPath() + "/user_dashboard.jsp";
        if (currentFolderId != null && !currentFolderId.trim().isEmpty()) {
            redirectUrl += "?folderId=" + currentFolderId;
        }

        try {
            if ("createFolder".equals(action)) {
                String folderName = request.getParameter("folderName");
                if (folderName != null && !folderName.trim().isEmpty()) {
                    Folder f = new Folder(0, userId, folderName, LocalDateTime.now());
                    boolean success = dao.createFolder(f);
                    if (success) {
                        response.sendRedirect(redirectUrl + (redirectUrl.contains("?") ? "&" : "?") + "folderSuccess=created");
                        return;
                    }
                }
                response.sendRedirect(redirectUrl + (redirectUrl.contains("?") ? "&" : "?") + "error=create_folder_failed");

            } else if ("deleteFolder".equals(action)) {
                int folderId = Integer.parseInt(request.getParameter("folderId"));
                boolean success = dao.deleteFolder(folderId, userId);
                if (success) {
                    // Always redirect to root when deleting a folder
                    response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?folderSuccess=deleted");
                } else {
                    response.sendRedirect(redirectUrl + (redirectUrl.contains("?") ? "&" : "?") + "error=delete_folder_failed");
                }
            } 
            // Note: You can easily add renameFolder here later following the same pattern!
            
        } catch (Exception e) {
            System.err.println("[FolderController Error] " + e.getMessage());
            response.sendRedirect(redirectUrl + (redirectUrl.contains("?") ? "&" : "?") + "error=system_error");
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