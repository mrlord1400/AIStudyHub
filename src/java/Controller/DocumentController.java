package Controller;

import Model.DTO.Document;
import Model.DAO.DocumentDAO;
import Model.DTO.Folder;
import Model.DAO.FolderDAO;
import Model.DTO.User;
import Model.DAO.UserDAO;
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

            } else if ("viewPublicPage".equals(action)) {
                int docId = Integer.parseInt(request.getParameter("docId"));
                Document doc = dao.findById(docId);

                if (doc != null) {
                    String permission = doc.getSharingPermission() != null ? doc.getSharingPermission().toUpperCase() : "PRIVATE";
                    // Cho phép xem nếu tài liệu là PUBLIC hoặc FRIENDS_ONLY, hoặc chính chủ
                    if ("PUBLIC".equals(permission) || "FRIENDS_ONLY".equals(permission) || doc.getUserId() == userId) {
                        // Lấy tên tác giả
                        UserDAO userDao = new UserDAO();
                        User author = userDao.getUserById(doc.getUserId());
                        String authorName = (author != null) ? author.getUsername() : "Không rõ";

                        request.setAttribute("document", doc);
                        request.setAttribute("authorName", authorName);
                        request.getRequestDispatcher("/publicDocument_view.jsp").forward(request, response);
                    } else {
                        response.sendRedirect(request.getContextPath() + "/MainController?action=explore&error=no_access");
                    }
                } else {
                    response.sendRedirect(request.getContextPath() + "/MainController?action=explore&error=not_found");
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
                        String ext = doc.getFileExtension() != null ? doc.getFileExtension().toLowerCase() : "";
                        
                        // ═══ FILE OFFICE: Convert sang HTML bằng Apache POI ═══
                        if ("docx".equals(ext) || "xlsx".equals(ext) || "pptx".equals(ext)) {
                            response.setContentType("text/html; charset=UTF-8");
                            java.io.PrintWriter pw = response.getWriter();
                            
                            pw.println("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
                            pw.println("<style>");
                            pw.println("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 0; padding: 24px; background: #1f2937; color: #f3f4f6; line-height: 1.7; }");
                            pw.println(".doc-content { max-width: 900px; margin: 0 auto; background: #111827; border-radius: 16px; padding: 32px 40px; box-shadow: 0 4px 24px rgba(0,0,0,0.3); }");
                            pw.println("h1,h2,h3 { color: #a5b4fc; margin-top: 1.5em; }");
                            pw.println("p { margin: 0.6em 0; }");
                            pw.println("table { width: 100%; border-collapse: collapse; margin: 16px 0; }");
                            pw.println("th { background: #374151; color: #a5b4fc; padding: 10px 14px; text-align: left; font-weight: 600; border: 1px solid #4b5563; }");
                            pw.println("td { padding: 8px 14px; border: 1px solid #4b5563; }");
                            pw.println("tr:nth-child(even) { background: #1a2332; }");
                            pw.println(".slide { background: #111827; border: 1px solid #4b5563; border-radius: 12px; padding: 24px 32px; margin: 20px 0; }");
                            pw.println(".slide-header { color: #818cf8; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid #374151; }");
                            pw.println(".file-badge { display: inline-block; padding: 4px 12px; border-radius: 8px; font-size: 11px; font-weight: 700; margin-bottom: 16px; }");
                            pw.println(".badge-docx { background: #1e3a5f; color: #60a5fa; }");
                            pw.println(".badge-xlsx { background: #14532d; color: #4ade80; }");
                            pw.println(".badge-pptx { background: #5b2100; color: #fb923c; }");
                            pw.println("</style></head><body><div class='doc-content'>");
                            
                            try {
                                if ("docx".equals(ext)) {
                                    pw.println("<span class='file-badge badge-docx'>DOCX Document</span>");
                                    try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                                        org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument(fis);
                                        for (org.apache.poi.xwpf.usermodel.IBodyElement element : document.getBodyElements()) {
                                            if (element instanceof org.apache.poi.xwpf.usermodel.XWPFParagraph) {
                                                org.apache.poi.xwpf.usermodel.XWPFParagraph para = (org.apache.poi.xwpf.usermodel.XWPFParagraph) element;
                                                String text = para.getText();
                                                if (text != null && !text.trim().isEmpty()) {
                                                    String style = para.getStyle();
                                                    String escapedText = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
                                                    if ("Heading1".equals(style) || "heading 1".equalsIgnoreCase(style)) {
                                                        pw.println("<h1>" + escapedText + "</h1>");
                                                    } else if ("Heading2".equals(style) || "heading 2".equalsIgnoreCase(style)) {
                                                        pw.println("<h2>" + escapedText + "</h2>");
                                                    } else if ("Heading3".equals(style) || "heading 3".equalsIgnoreCase(style)) {
                                                        pw.println("<h3>" + escapedText + "</h3>");
                                                    } else {
                                                        pw.println("<p>" + escapedText + "</p>");
                                                    }
                                                }
                                            } else if (element instanceof org.apache.poi.xwpf.usermodel.XWPFTable) {
                                                org.apache.poi.xwpf.usermodel.XWPFTable table = (org.apache.poi.xwpf.usermodel.XWPFTable) element;
                                                pw.println("<table>");
                                                boolean isFirstRow = true;
                                                for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : table.getRows()) {
                                                    pw.println("<tr>");
                                                    for (org.apache.poi.xwpf.usermodel.XWPFTableCell cell : row.getTableCells()) {
                                                        String tag = isFirstRow ? "th" : "td";
                                                        String cellText = cell.getText().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
                                                        pw.println("<" + tag + ">" + cellText + "</" + tag + ">");
                                                    }
                                                    pw.println("</tr>");
                                                    isFirstRow = false;
                                                }
                                                pw.println("</table>");
                                            }
                                        }
                                        document.close();
                                    }
                                    
                                } else if ("xlsx".equals(ext)) {
                                    pw.println("<span class='file-badge badge-xlsx'>XLSX Spreadsheet</span>");
                                    try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                                        org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(fis);
                                        for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                                            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(s);
                                            pw.println("<h2>" + sheet.getSheetName().replace("&", "&amp;").replace("<", "&lt;") + "</h2>");
                                            pw.println("<table>");
                                            
                                            int lastRow = sheet.getLastRowNum();
                                            int maxCol = 0;
                                            for (int i = 0; i <= Math.min(lastRow, 500); i++) {
                                                org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
                                                if (row != null && row.getLastCellNum() > maxCol) {
                                                    maxCol = row.getLastCellNum();
                                                }
                                            }
                                            
                                            for (int i = 0; i <= Math.min(lastRow, 500); i++) {
                                                org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
                                                pw.println("<tr>");
                                                for (int c = 0; c < maxCol; c++) {
                                                    String tag = (i == 0) ? "th" : "td";
                                                    String cellValue = "";
                                                    if (row != null) {
                                                        org.apache.poi.ss.usermodel.Cell cell = row.getCell(c);
                                                        if (cell != null) {
                                                            try {
                                                                switch (cell.getCellType()) {
                                                                    case STRING:
                                                                        cellValue = cell.getStringCellValue();
                                                                        break;
                                                                    case NUMERIC:
                                                                        if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                                                                            cellValue = cell.getLocalDateTimeCellValue().toString();
                                                                        } else {
                                                                            double num = cell.getNumericCellValue();
                                                                            cellValue = (num == Math.floor(num)) ? String.valueOf((long) num) : String.valueOf(num);
                                                                        }
                                                                        break;
                                                                    case BOOLEAN:
                                                                        cellValue = String.valueOf(cell.getBooleanCellValue());
                                                                        break;
                                                                    case FORMULA:
                                                                        try {
                                                                            cellValue = String.valueOf(cell.getNumericCellValue());
                                                                        } catch (Exception fe) {
                                                                            try { cellValue = cell.getStringCellValue(); } catch (Exception ignored) {}
                                                                        }
                                                                        break;
                                                                    default:
                                                                        cellValue = "";
                                                                }
                                                            } catch (Exception ce) {
                                                                cellValue = "";
                                                            }
                                                        }
                                                    }
                                                    cellValue = cellValue.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
                                                    pw.println("<" + tag + ">" + cellValue + "</" + tag + ">");
                                                }
                                                pw.println("</tr>");
                                            }
                                            pw.println("</table>");
                                            if (lastRow > 500) {
                                                pw.println("<p style='color:#f59e0b; font-size:13px;'>⚠ Chỉ hiển thị 500 dòng đầu tiên. Vui lòng tải file để xem đầy đủ.</p>");
                                            }
                                        }
                                        workbook.close();
                                    }
                                    
                                } else if ("pptx".equals(ext)) {
                                    pw.println("<span class='file-badge badge-pptx'>PPTX Presentation</span>");
                                    try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                                        org.apache.poi.xslf.usermodel.XMLSlideShow ppt = new org.apache.poi.xslf.usermodel.XMLSlideShow(fis);
                                        java.util.List<org.apache.poi.xslf.usermodel.XSLFSlide> slides = ppt.getSlides();
                                        for (int i = 0; i < slides.size(); i++) {
                                            org.apache.poi.xslf.usermodel.XSLFSlide slide = slides.get(i);
                                            pw.println("<div class='slide'>");
                                            pw.println("<div class='slide-header'>Slide " + (i + 1) + " / " + slides.size() + "</div>");
                                            for (org.apache.poi.xslf.usermodel.XSLFShape shape : slide.getShapes()) {
                                                if (shape instanceof org.apache.poi.xslf.usermodel.XSLFTextShape) {
                                                    org.apache.poi.xslf.usermodel.XSLFTextShape textShape = (org.apache.poi.xslf.usermodel.XSLFTextShape) shape;
                                                    String shapeText = textShape.getText();
                                                    if (shapeText != null && !shapeText.trim().isEmpty()) {
                                                        String escaped = shapeText.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
                                                        pw.println("<p>" + escaped + "</p>");
                                                    }
                                                }
                                            }
                                            pw.println("</div>");
                                        }
                                        ppt.close();
                                    }
                                }
                            } catch (Exception poiEx) {
                                pw.println("<p style='color: #ef4444;'>Lỗi khi đọc file: " + poiEx.getMessage() + "</p>");
                                System.err.println("[DocumentController] POI Error: " + poiEx.getMessage());
                            }
                            
                            pw.println("</div></body></html>");
                            pw.flush();
                            
                        } else {
                            // ═══ FILE KHÁC (PDF, TXT, MD, ảnh...): Stream bình thường ═══
                            String mimeType = getServletContext().getMimeType(filePath);
                            if (mimeType == null) {
                                mimeType = "application/octet-stream";
                            }
                            
                            String disposition = "attachment";
                            if ("pdf".equals(ext) || "txt".equals(ext) || "md".equals(ext)
                                || "png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext) || "gif".equals(ext)) {
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