package Controller;

import Model.Document;
import Model.DocumentDAO;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "DocumentController", urlPatterns = {"/DocumentController"})
public class DocumentController extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Security check
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        String action = request.getParameter("action");
        DocumentDAO dao = new DocumentDAO();
        int userId = (int) session.getAttribute("userId");
        try {
            if ("editDoc".equals(action)) {
                // Fetch document details for the edit form
                int docId = Integer.parseInt(request.getParameter("docId"));
                Document doc = dao.findById(docId);

                if (doc != null) {
                    // Attach the document to the request and forward to JSP
                    request.setAttribute("document", doc);
                    request.getRequestDispatcher("/document_edit.jsp").forward(request, response);
                } else {
                    response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?error=not_found");
                }

            } else if ("deleteDoc".equals(action)) {
                int docId = Integer.parseInt(request.getParameter("docId"));
                // 1. Get document info BEFORE deleting (to find the physical file)
                Document doc = dao.findById(docId);
                
                if (doc != null) {
                    // 2. Delete physical file from server
                    String cloudUrl = doc.getCloudStorageUrl();
                    if (cloudUrl != null && !cloudUrl.trim().isEmpty()) {
                        String relativePath = cloudUrl;
                        String contextPath = request.getContextPath();
                        if (relativePath.startsWith(contextPath)) {
                            relativePath = relativePath.substring(contextPath.length());
                        }
                        if (relativePath.startsWith("/")) {
                            relativePath = relativePath.substring(1);
                        }
                        
                        String realPath = getServletContext().getRealPath("");
                        if (realPath != null) {
                            java.io.File physicalFile = new java.io.File(realPath + java.io.File.separator + relativePath.replace("/", java.io.File.separator));
                            if (physicalFile.exists()) {
                                boolean fileDeleted = physicalFile.delete();
                                System.out.println("[DocumentController] Deleted physical file: " + physicalFile.getAbsolutePath() + " → " + fileDeleted);
                            }
                        }
                    }
                    
                    // 3. Delete DB record
                    boolean deleted = dao.deleteDocument(docId);
                    if (deleted) {
                        response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?deleteSuccess=1");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?error=delete_failed");
                    }
                } else {
                    response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?error=not_found");
                }
            } else if ("updateDoc".equals(action)) {
                int docId = Integer.parseInt(request.getParameter("docId"));
                String newTitle = request.getParameter("title");

                String sharingPerm = request.getParameter("sharingPermission");
                if (sharingPerm == null) {
                    sharingPerm = "PRIVATE";
                }

                Integer folderId = null;
                String folderStr = request.getParameter("folderId");
                if (folderStr != null && !folderStr.trim().isEmpty()) {
                    try {
                        folderId = Integer.parseInt(folderStr);
                    } catch (NumberFormatException ignored) {
                    }
                }

                // Lấy cloud_storage_url hiện tại (không đổi khi chỉ edit metadata)
                Document existingDoc = dao.findById(docId);
                String existingCloudUrl = (existingDoc != null) ? existingDoc.getCloudStorageUrl() : "";

                // Execute Update
                boolean updated = dao.updateDocumentInfo(docId, newTitle, folderId, sharingPerm.toUpperCase(), existingCloudUrl);

                if (updated) {
                    response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?updateSuccess=1");
                } else {
                    // Send them back to the edit page with an error if it fails
                    request.setAttribute("errorMessage", "Không thể cập nhật. Vui lòng thử lại.");
                    request.getRequestDispatcher("/MainController?action=editDoc&docId=" + docId).forward(request, response);
                }
            } else if ("viewDoc".equals(action)) {
                int docId = Integer.parseInt(request.getParameter("docId"));
                Document doc = dao.findById(docId);

                // Security: Ensure the file exists and the user owns it
                if (doc != null && doc.getUserId() == userId) {

                    // 1. Trích xuất đường dẫn tương đối từ cloudStorageUrl
                    String url = doc.getCloudStorageUrl();
                    String relativePath = url;
                    String ctxPath = request.getContextPath();
                    if (relativePath.startsWith(ctxPath)) {
                        relativePath = relativePath.substring(ctxPath.length());
                    }
                    if (relativePath.startsWith("/")) {
                        relativePath = relativePath.substring(1);
                    }

                    // 2. Rebuild the physical server path
                    String realPath = getServletContext().getRealPath("");
                    if (realPath == null) {
                        realPath = System.getProperty("java.io.tmpdir");
                    }
                    String filePath = realPath + java.io.File.separator + relativePath.replace("/", java.io.File.separator);

                    java.io.File file = new java.io.File(filePath);

                    if (file.exists()) {
                        // 3. Detect file type so the browser knows how to render it
                        String mimeType = getServletContext().getMimeType(filePath);
                        if (mimeType == null) {
                            mimeType = "application/octet-stream";
                        }

                        // 4. Force INLINE viewing instead of downloading
                        response.setContentType(mimeType);
                        response.setHeader("Content-Disposition", "inline; filename=\"" + doc.getTitle() + "\"");

                        // 5. Stream the bytes directly to the iframe
                        try ( java.io.FileInputStream inStream = new java.io.FileInputStream(file);  java.io.OutputStream outStream = response.getOutputStream()) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = inStream.read(buffer)) != -1) {
                                outStream.write(buffer, 0, bytesRead);
                            }
                        }
                    } else {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Tài liệu không còn tồn tại trên máy chủ.");
                    }
                } else {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập tài liệu này.");
                }
            } else if ("downloadDoc".equals(action)) {
                int docId = Integer.parseInt(request.getParameter("docId"));
                Document doc = dao.findById(docId);

                if (doc != null && doc.getUserId() == userId) {
                    // Trích xuất đường dẫn tương đối từ cloudStorageUrl
                    String url = doc.getCloudStorageUrl();
                    String relativePath = url;
                    String ctxPath = request.getContextPath();
                    if (relativePath.startsWith(ctxPath)) {
                        relativePath = relativePath.substring(ctxPath.length());
                    }
                    if (relativePath.startsWith("/")) {
                        relativePath = relativePath.substring(1);
                    }
                    String savedFileName = url.substring(url.lastIndexOf("/") + 1);

                    // Rebuild the absolute path to the server's uploads folder
                    String realPath = getServletContext().getRealPath("");
                    if (realPath == null) {
                        realPath = System.getProperty("java.io.tmpdir");
                    }
                    String filePath = realPath + java.io.File.separator + relativePath.replace("/", java.io.File.separator);

                    java.io.File downloadFile = new java.io.File(filePath);
                    if (downloadFile.exists()) {
                        // Grab the file extension to append to the user's custom title
                        String ext = "";
                        if (savedFileName.lastIndexOf('.') > 0) {
                            ext = savedFileName.substring(savedFileName.lastIndexOf('.'));
                        }

                        // Force the browser to download the file instead of viewing it
                        response.setContentType("application/octet-stream");
                        response.setHeader("Content-Disposition", "attachment; filename=\"" + doc.getTitle() + ext + "\"");

                        // Stream the file bytes to the client
                        try ( java.io.FileInputStream inStream = new java.io.FileInputStream(downloadFile);  java.io.OutputStream outStream = response.getOutputStream()) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = inStream.read(buffer)) != -1) {
                                outStream.write(buffer, 0, bytesRead);
                            }
                        }
                    } else {
                        response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?error=file_not_found");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DocumentController Error] " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?error=system_error");
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
