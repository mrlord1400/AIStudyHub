package Controller;

import Model.DTO.Document;
import Model.DAO.DocumentDAO;
import Model.DTO.Folder;
import Model.DAO.FolderDAO;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

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
            if ("explore".equals(action)) {
                // Chặn cache để dữ liệu mới upload luôn hiển thị ngay
                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setDateHeader("Expires", 0);

                FolderDAO folderDao = new FolderDAO();
                String viewMode = request.getParameter("view");
                boolean isFriendsView = "friends".equals(viewMode);

                // 1. Lấy danh sách tài liệu
                List<Document> exploreDocs = dao.getExploreDocuments(userId, isFriendsView);
                
                // 2. Lấy thống kê ĐÚNG theo view đang xem
                int[] stats = dao.getExploreStats(userId, isFriendsView);
                
                // 3. Lấy danh sách Folder Public
                List<Folder> publicFolders = folderDao.getPublicFolders();

                // 4. Set vào Request để gửi xuống JSP
                request.setAttribute("publicDocuments", exploreDocs);
                request.setAttribute("publicFolders", publicFolders);
                request.setAttribute("isFriendsView", isFriendsView);
                request.setAttribute("realTotalDocs", stats[0]);
                request.setAttribute("realTotalContributors", stats[1]);
                request.setAttribute("realTotalDownloads", stats[2]);

                request.getRequestDispatcher("/FileExplore.jsp").forward(request, response);
                
            } else if ("editDoc".equals(action)) {
                int docId = Integer.parseInt(request.getParameter("docId"));
                Document doc = dao.findById(docId);

                if (doc != null) {
                    request.setAttribute("document", doc);
                    request.getRequestDispatcher("/document_edit.jsp").forward(request, response);
                } else {
                    response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?error=not_found");
                }

            } else if ("viewPage".equals(action)) {
                int docId = Integer.parseInt(request.getParameter("docId"));
                Document doc = dao.findById(docId);

                if (doc != null && doc.getUserId() == userId) {
                    request.setAttribute("document", doc);
                    request.getRequestDispatcher("/document_view.jsp").forward(request, response);
                } else {
                    response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?error=not_found");
                }

            } else if ("deleteDoc".equals(action)) {
                int docId = Integer.parseInt(request.getParameter("docId"));
                Document doc = dao.findById(docId);

                if (doc != null) {
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
                    } catch (NumberFormatException ignored) {}
                }

                Document existingDoc = dao.findById(docId);
                String existingCloudUrl = (existingDoc != null) ? existingDoc.getCloudStorageUrl() : "";

                boolean updated = dao.updateDocumentInfo(docId, newTitle, folderId, sharingPerm.toUpperCase(), existingCloudUrl);

                if (updated) {
                    response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?updateSuccess=1");
                } else {
                    request.setAttribute("errorMessage", "Không thể cập nhật. Vui lòng thử lại.");
                    request.getRequestDispatcher("/MainController?action=editDoc&docId=" + docId).forward(request, response);
                }
            } else if ("updatePermission".equals(action)) {
                int docId = Integer.parseInt(request.getParameter("docId"));
                String sharingPerm = request.getParameter("sharingPermission");
                if (sharingPerm == null || sharingPerm.trim().isEmpty()) {
                    sharingPerm = "PRIVATE";
                }

                Document doc = dao.findById(docId);
                if (doc != null && doc.getUserId() == userId) {
                    boolean updated = dao.updateSharingPermission(docId, sharingPerm.toUpperCase());
                    if (updated) {
                        response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?updateSuccess=1");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/DocumentController?action=viewPage&docId=" + docId + "&error=permission_failed");
                    }
                } else {
                    response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?error=not_found");
                }
            } else if ("viewDoc".equals(action)) {
                int docId = Integer.parseInt(request.getParameter("docId"));
                Document doc = dao.findById(docId);

                boolean canAccess = false;
                if (doc != null) {
                    String permission = doc.getSharingPermission() != null ? doc.getSharingPermission().toUpperCase() : "PRIVATE";
                    if (doc.getUserId() == userId || "PUBLIC".equals(permission) || "FRIENDS_ONLY".equals(permission)) {
                        canAccess = true;
                    }
                }

                if (canAccess) {
                    String url = doc.getCloudStorageUrl();
                    String relativePath = url;
                    String ctxPath = request.getContextPath();
                    if (relativePath.startsWith(ctxPath)) {
                        relativePath = relativePath.substring(ctxPath.length());
                    }
                    if (relativePath.startsWith("/")) {
                        relativePath = relativePath.substring(1);
                    }

                    String realPath = getServletContext().getRealPath("");
                    if (realPath == null) {
                        realPath = System.getProperty("java.io.tmpdir");
                    }
                    String filePath = realPath + java.io.File.separator + relativePath.replace("/", java.io.File.separator);
                    java.io.File file = new java.io.File(filePath);

                    if (file.exists()) {
                        String mimeType = getServletContext().getMimeType(filePath);
                        if (mimeType == null) {
                            mimeType = "application/octet-stream";
                        }

                        String ext = doc.getFileExtension() != null ? doc.getFileExtension().toLowerCase() : "";
                        String disposition = "attachment";
                        
                        if ("pdf".equals(ext) || "txt".equals(ext) || "md".equals(ext)) {
                            disposition = "inline";
                        }

                        response.setContentType(mimeType);
                        
                        String downloadFileName = doc.getTitle();
                        if (!downloadFileName.toLowerCase().endsWith("." + ext)) {
                            downloadFileName += "." + ext;
                        }
                        
                        String encodedFileName = java.net.URLEncoder.encode(downloadFileName, "UTF-8").replaceAll("\\+", "%20");
                        
                        response.setHeader("Content-Disposition", disposition + "; filename*=UTF-8''" + encodedFileName);

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

                boolean canAccess = false;
                if (doc != null) {
                    String permission = doc.getSharingPermission() != null ? doc.getSharingPermission().toUpperCase() : "PRIVATE";
                    if (doc.getUserId() == userId || "PUBLIC".equals(permission) || "FRIENDS_ONLY".equals(permission)) {
                        canAccess = true;
                    }
                }

                if (canAccess) {
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

                    String realPath = getServletContext().getRealPath("");
                    if (realPath == null) {
                        realPath = System.getProperty("java.io.tmpdir");
                    }
                    String filePath = realPath + java.io.File.separator + relativePath.replace("/", java.io.File.separator);

                    java.io.File downloadFile = new java.io.File(filePath);
                    if (downloadFile.exists()) {
                        
                        // 🔥 ĐÃ FIX: CHẠY HÀM TĂNG LƯỢT TẢI VÀO CƠ SỞ DỮ LIỆU
                        dao.incrementDownloadCount(docId);
                        
                        String ext = "";
                        if (savedFileName.lastIndexOf('.') > 0) {
                            ext = savedFileName.substring(savedFileName.lastIndexOf('.'));
                        }

                        String downloadFileName = doc.getTitle() + ext;
                        String encodedFileName = java.net.URLEncoder.encode(downloadFileName, "UTF-8").replaceAll("\\+", "%20");

                        response.setContentType("application/octet-stream");
                        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

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
                } else {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền tải tài liệu này.");
                }
            } else if ("toggleBookmark".equals(action)) {
                // 🔥 THÊM ACTION NÀY VÀO ĐỂ NHẬN AJAX TỪ GIAO DIỆN
                int docId = Integer.parseInt(request.getParameter("docId"));
                
                boolean isNowBookmarked = dao.toggleBookmark(userId, docId);
                int newCount = dao.getBookmarkCount(docId);
                
                // Trả về JSON chứa trạng thái mới
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"isBookmarked\": " + isNowBookmarked + ", \"newCount\": " + newCount + "}");
                return; // Ngắt luồng ở đây, không redirect nữa vì đây là Request ngầm
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