package Controller;

import Model.DTO.Document;
import Model.DAO.DocumentDAO;
import Model.DTO.Folder;
import Model.DAO.FolderDAO;
import Model.DTO.User;
import Model.DAO.UserDAO;
import Model.Service.DocumentAccess;
import Utils.Access;
import Utils.FileStorage;
import Utils.FileTypes;
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

    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(DocumentController.class.getName());

    private final DocumentAccess documentAccess = new DocumentAccess();

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        // 1. Xử lý Session an toàn để không bị NullPointerException
        HttpSession session = request.getSession(false);
        Integer sessionUserId = null;

        if (session != null && session.getAttribute("userId") != null) {
            sessionUserId = (Integer) session.getAttribute("userId");
        }

        // 2. Phân quyền: Nếu là Khách (chưa đăng nhập), CHỈ cho phép xem trước tài liệu
        if (sessionUserId == null) {
            if (!"viewPublicPage".equals(action) && !"viewDoc".equals(action)) {
                // Nếu làm hành động khác, đá về trang login
                response.sendRedirect(request.getContextPath() + "/login.jsp");
                return;
            }
        }

        DocumentDAO dao = new DocumentDAO();
        boolean isAdmin = Access.isAdmin(request);

        // 3. Gán userId = 0 cho Khách để tránh lỗi dòng 38 của bạn
        int userId = (sessionUserId != null) ? sessionUserId : 0;

        try {
            if ("explore".equals(action)) {
                // Chặn cache để dữ liệu mới upload luôn hiển thị ngay
                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setDateHeader("Expires", 0);

                FolderDAO folderDao = new FolderDAO();
                String viewMode = request.getParameter("view");
                boolean isFriendsView = "friends".equals(viewMode);

                // LẤY TỪ KHÓA TÌM KIẾM VÀ TIÊU CHÍ SẮP XẾP
                String searchQuery = request.getParameter("query");
                String sortBy = request.getParameter("sort");
                if (sortBy == null || sortBy.trim().isEmpty()) {
                    sortBy = "date"; // Mặc định sắp xếp theo ngày
                }

                // 1. Lấy danh sách tài liệu
                List<Document> exploreDocs = dao.getExploreDocuments(userId, isFriendsView, searchQuery, sortBy);

                // 2. Lấy thống kê
                int[] stats = dao.getExploreStats(userId, isFriendsView);

                // 3. Lấy danh sách Folder Public
                List<Folder> publicFolders = folderDao.getPublicFolders();

                // 4. Set vào Request
                request.setAttribute("publicDocuments", exploreDocs);
                request.setAttribute("publicFolders", publicFolders);
                request.setAttribute("isFriendsView", isFriendsView);
                request.setAttribute("realTotalDocs", stats[0]);
                request.setAttribute("realTotalContributors", stats[1]);
                request.setAttribute("realTotalDownloads", stats[2]);
                request.setAttribute("searchQuery", searchQuery != null ? searchQuery : "");
                request.setAttribute("sortBy", sortBy);

                request.getRequestDispatcher("/FileExplore.jsp").forward(request, response);

            } else if ("editDoc".equals(action)) {
                int docId = Integer.parseInt(request.getParameter("docId"));
                Document doc = dao.findById(docId);

                // Chỉ chủ sở hữu được sửa; "không phải của bạn" trả lời y như "không tồn tại" (Conv §2.3)
                if (documentAccess.canEdit(doc, sessionUserId)) {
                    request.setAttribute("document", doc);
                    request.getRequestDispatcher("/document_edit.jsp").forward(request, response);
                } else {
                    response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?error=not_found");
                }

            } else if ("viewPage".equals(action)) {
                int docId = Integer.parseInt(request.getParameter("docId"));
                Document doc = dao.findById(docId);

                if (documentAccess.canEdit(doc, sessionUserId)) {
                    request.setAttribute("document", doc);
                    request.getRequestDispatcher("/document_view.jsp").forward(request, response);
                } else {
                    response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?error=not_found");
                }

            } else if ("viewPublicPage".equals(action)) {
                int docId = Integer.parseInt(request.getParameter("docId"));
                Document doc = dao.findById(docId);

                // "Không có quyền xem" trả lời y như "không tồn tại" (Conv §2.3, D-06)
                if (documentAccess.canView(doc, sessionUserId, isAdmin)) {
                    UserDAO userDao = new UserDAO();
                    User author = userDao.getUserById(doc.getUserId());
                    String authorName = (author != null) ? author.getUsername() : "Không rõ";

                    request.setAttribute("document", doc);
                    request.setAttribute("authorName", authorName);
                    request.getRequestDispatcher("/publicDocument_view.jsp").forward(request, response);
                } else {
                    response.sendRedirect(request.getContextPath() + "/MainController?action=explore&error=not_found");
                }

            } else if ("deleteDoc".equals(action)) {
                int docId = Integer.parseInt(request.getParameter("docId"));
                Document doc = dao.findById(docId);

                if (documentAccess.canEdit(doc, sessionUserId)) {
                    // Xóa DB trước (có điều kiện user_id); chỉ khi đúng 1 dòng bị xóa mới xóa file vật lý.
                    boolean deleted = dao.deleteDocumentForOwner(docId, userId);

                    if (deleted) {
                        deletePhysicalFile(doc.getCloudStorageUrl());
                        response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?updateSuccess=1");
                    } else {
                        request.setAttribute("errorMessage", "Không thể xóa. Vui lòng thử lại.");
                        request.getRequestDispatcher("/MainController?action=editDoc&docId=" + docId).forward(request, response);
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
                sharingPerm = sharingPerm.toUpperCase();

                // 🔥 MỚI: User đang bị SUSPENDED thì không được đặt tài liệu là PUBLIC
                String updateDocErrorMsg = null;
                if ("PUBLIC".equals(sharingPerm)) {
                    UserDAO userDaoCheck = new UserDAO();
                    User currentUserInfo = userDaoCheck.getUserById(userId);
                    if (currentUserInfo != null && "SUSPENDED".equalsIgnoreCase(currentUserInfo.getStatus())) {
                        sharingPerm = "PRIVATE";
                        updateDocErrorMsg = "Tài khoản của bạn đang bị tạm khóa (suspended) nên không thể đặt tài liệu ở chế độ công khai. Tài liệu đã được lưu ở chế độ riêng tư.";
                    }
                }

                Integer folderId = null;
                String folderStr = request.getParameter("folderId");
                if (folderStr != null && !folderStr.trim().isEmpty()) {
                    try {
                        folderId = Integer.parseInt(folderStr);
                    } catch (NumberFormatException ignored) {
                    }
                }

                Document existingDoc = dao.findById(docId);
                if (!documentAccess.canEdit(existingDoc, sessionUserId)) {
                    response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?error=not_found");
                    return;
                }

                // folderId đến từ request: phải là thư mục của chính người dùng
                if (folderId != null && !new FolderDAO().isOwnedBy(folderId, userId)) {
                    request.setAttribute("errorMessage", "Thư mục không hợp lệ.");
                    request.getRequestDispatcher("/MainController?action=editDoc&docId=" + docId).forward(request, response);
                    return;
                }

                boolean updated = dao.updateDocumentInfoForOwner(docId, userId, newTitle, folderId, sharingPerm,
                        existingDoc.getCloudStorageUrl());

                if (updated) {
                    // 🔥 MỚI: nếu bị chặn do suspended, đá về kèm cảnh báo thay vì báo thành công bình thường
                    if (updateDocErrorMsg != null) {
                        response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?warning=account_suspended");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?updateSuccess=1");
                    }
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
                sharingPerm = sharingPerm.toUpperCase();

                // 🔥 MỚI: chặn suspended user bật PUBLIC qua action riêng lẻ này
                if ("PUBLIC".equals(sharingPerm)) {
                    UserDAO userDaoCheck = new UserDAO();
                    User currentUserInfo = userDaoCheck.getUserById(userId);
                    if (currentUserInfo != null && "SUSPENDED".equalsIgnoreCase(currentUserInfo.getStatus())) {
                        response.sendRedirect(request.getContextPath() + "/DocumentController?action=viewPage&docId=" + docId + "&error=account_suspended");
                        return;
                    }
                }

                Document doc = dao.findById(docId);
                if (documentAccess.canEdit(doc, sessionUserId)) {
                    boolean updated = dao.updateSharingPermissionForOwner(docId, userId, sharingPerm);
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

                if (documentAccess.canView(doc, sessionUserId, isAdmin)) {
                    // File chỉ được phục vụ qua controller này, sau canView (Conv §9)
                    response.setHeader("X-Content-Type-Options", "nosniff");
                    java.io.File file = FileStorage.fileOrNull(doc.getCloudStorageUrl());

                    if (file != null) {
                        String ext = doc.getFileExtension() != null ? doc.getFileExtension().toLowerCase() : "";

                        // Kiểm tra danh sách file hỗ trợ xem trực tiếp
                        boolean isSupported = "docx".equals(ext) || "xlsx".equals(ext) || "pptx".equals(ext)
                                || "pdf".equals(ext) || "txt".equals(ext) || "md".equals(ext)
                                || "png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext) || "gif".equals(ext);

                        // 🔥 ĐÃ CHỈNH SỬA LẠI UI BÁO LỖI DỊU NHẤT VÀ CHUYÊN NGHIỆP HƠN
                        if (!isSupported) {
                            response.setContentType("text/html; charset=UTF-8");
                            java.io.PrintWriter pw = response.getWriter();
                            pw.println("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
                            pw.println("<style>");
                            pw.println("body { font-family: 'Segoe UI', Arial, sans-serif; background: #111827; color: #f3f4f6; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }");
                            pw.println(".info-box { background: #1f2937; padding: 40px; border-radius: 16px; text-align: center; max-width: 450px; box-shadow: 0 10px 25px rgba(0,0,0,0.2); border: 1px solid #374151; }");
                            pw.println(".icon { width: 64px; height: 64px; color: #6b7280; margin-bottom: 16px; }");
                            pw.println("h2 { color: #e5e7eb; margin-top: 0; margin-bottom: 12px; font-size: 20px; font-weight: 500; }");
                            pw.println("p { font-size: 15px; color: #9ca3af; line-height: 1.6; margin: 8px 0; }");
                            pw.println("strong { color: #f3f4f6; font-weight: 600; }");
                            pw.println("</style>");
                            pw.println("</head><body>");
                            pw.println("<div class='info-box'>");
                            // Icon SVG hình file thông tin nhẹ nhàng
                            pw.println("<svg class='icon' fill='none' stroke='currentColor' viewBox='0 0 24 24' xmlns='http://www.w3.org/2000/svg'><path stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z'></path></svg>");
                            pw.println("<h2>Chưa hỗ trợ xem trước định dạng này</h2>");
                            pw.println("<p>Hệ thống hiện tại chưa thể hiển thị trực tiếp file <strong>." + ext + "</strong> trên trình duyệt.</p>");
                            pw.println("<p>Bạn vui lòng đóng tab này và sử dụng chức năng <strong>Tải về</strong> để xem trên máy tính nhé.</p>");
                            pw.println("</div></body></html>");
                            pw.flush();
                            return; 
                        }

                        // FILE OFFICE: Convert sang HTML bằng Apache POI
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
                                    try ( java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
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
                                    try ( java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
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
                                                                            try {
                                                                                cellValue = cell.getStringCellValue();
                                                                            } catch (Exception ignored) {
                                                                            }
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
                                    try ( java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
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
                                LOG.log(java.util.logging.Level.WARNING, "office preview failed", poiEx);
                            }

                            pw.println("</div></body></html>");
                            pw.flush();

                        } else {
                            // FILE KHÁC (PDF, TXT, MD, ảnh...): Stream bình thường
                            // MIME theo đuôi đã được cho phép (md/txt là text/plain để trình duyệt đọc inline)
                            String mimeType = FileTypes.mimeType(ext);

                            // Do đã chặn file không hỗ trợ ở trên, tất cả các file lọt xuống đây đều được phép inline
                            String disposition = "inline";

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
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Tài liệu không tồn tại.");
                }

            } else if ("downloadDoc".equals(action)) {
                int docId = Integer.parseInt(request.getParameter("docId"));
                Document doc = dao.findById(docId);

                if (documentAccess.canView(doc, sessionUserId, isAdmin)) {
                    String url = doc.getCloudStorageUrl();
                    java.io.File downloadFile = FileStorage.fileOrNull(url);
                    if (downloadFile != null) {

                        dao.incrementDownloadCount(docId);

                        // Key có dạng <userId>/<uuid>.<ext>: đuôi lấy từ key
                        String ext = url.substring(url.lastIndexOf('.'));

                        String downloadFileName = doc.getTitle() + ext;
                        String encodedFileName = java.net.URLEncoder.encode(downloadFileName, "UTF-8").replaceAll("\\+", "%20");

                        response.setHeader("X-Content-Type-Options", "nosniff");
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
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Tài liệu không tồn tại.");
                }

            } else if ("toggleBookmark".equals(action)) {
                int docId = Integer.parseInt(request.getParameter("docId"));

                // Chỉ bookmark được tài liệu mình có quyền xem
                if (!documentAccess.canView(dao.findById(docId), sessionUserId, isAdmin)) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"ok\": false, \"code\": \"NOT_FOUND\", \"message\": \"Tài liệu không tồn tại.\"}");
                    return;
                }

                boolean isNowBookmarked = dao.toggleBookmark(userId, docId);
                int newCount = dao.getBookmarkCount(docId);

                // Trả về JSON chứa trạng thái mới
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"isBookmarked\": " + isNowBookmarked + ", \"newCount\": " + newCount + "}");
                return;
            }
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.SEVERE, "request failed", e);
            response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?error=system_error");
        }
    }

    /** Xóa file vật lý của một tài liệu đã bị xóa khỏi DB. */
    private void deletePhysicalFile(String storageKey) {
        FileStorage.delete(storageKey);
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