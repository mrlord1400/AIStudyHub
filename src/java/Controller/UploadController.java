package Controller;

import Model.Document;
import Model.DocumentDAO;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * UploadController — Servlet xử lý luồng tải lên tài liệu 3 bước: * POST
 * /UploadController?action=upload → Bước 1: Tiếp nhận file, lưu tạm, trích xuất
 * Extension, thêm DB, chuyển sang form chỉnh sửa POST
 * /UploadController?action=confirm → Bước 2: Xác nhận thông tin tài liệu từ
 * phía user → Cập nhật DB POST /UploadController?action=cancel → Bước 3: Người
 * dùng hủy tác vụ → Xóa file vật lý + Xóa bản ghi tạm trong DB
 */
@WebServlet(name = "UploadController", urlPatterns = {"/UploadController"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024, // 1 MB — ghi thẳng vào đĩa nếu vượt ngưỡng
        maxFileSize = 50 * 1024 * 1024, // 50 MB — giới hạn tối đa mỗi file
        maxRequestSize = 55 * 1024 * 1024 // 55 MB — giới hạn toàn bộ request payload
)
public class UploadController extends HttpServlet {

    private static final String UPLOAD_DIR = "uploads";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        String action = request.getParameter("action");
        if (action == null) {
            action = "";
        }

        switch (action) {
            case "upload":
                handleUpload(request, response);
                break;
            case "confirm":
                handleConfirm(request, response);
                break;
            case "cancel":
                handleCancel(request, response);
                break;
            case "replace":
                handleReplace(request, response);
                break;
            case "keepBoth":
                handleKeepBoth(request, response);
                break;
            default:
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Hành động không hợp lệ.");
                break;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BƯỚC 1 — Tiếp nhận file → Trích xuất thông tin & định dạng → Lưu DB tạm
    // ─────────────────────────────────────────────────────────────────────────
    private void handleUpload(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            int userId = (int) request.getSession().getAttribute("userId");

            Part filePart = request.getPart("file");
            if (filePart == null || filePart.getSize() == 0) {
                System.err.println("[UploadController] Tải lên thất bại: File trống hoặc null.");
                request.setAttribute("errorMessage", "Vui lòng chọn một file hợp lệ.");
                request.getRequestDispatcher("/document_upload.jsp").forward(request, response);
                return;
            }

            // Trích xuất tên tệp tin gốc
            String originalFileName = filePart.getSubmittedFileName();
            if (originalFileName == null || originalFileName.trim().isEmpty()) {
                originalFileName = "document_" + System.currentTimeMillis();
            }

            // 🚀 BỔ SUNG: Trích xuất phần mở rộng (File Extension) chuẩn hóa
            String fileExtension = "";
            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < originalFileName.length() - 1) {
                fileExtension = originalFileName.substring(dotIndex + 1).toLowerCase().trim();
            } else {
                fileExtension = "unknown"; // Trường hợp file không có đuôi mở rộng
            }

            // Thiết lập đường dẫn thư mục lưu trữ per-user trên Server
            String realPath = getServletContext().getRealPath("");
            if (realPath == null) {
                realPath = System.getProperty("java.io.tmpdir");
            }
            // Tạo thư mục riêng cho mỗi user: uploads/{userId}/
            String uploadPath = realPath + File.separator + UPLOAD_DIR + File.separator + userId;

            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists() && !uploadDir.mkdirs()) {
                throw new IOException("Không thể khởi tạo thư mục lưu trữ tại: " + uploadPath);
            }

            // Tạo tên file tạm dùng UUID ngắn (sẽ được rename sau khi xác nhận)
            String uuidPrefix = java.util.UUID.randomUUID().toString().substring(0, 8);
            String savedFileName = "temp_" + uuidPrefix + "_" + sanitizeFileName(originalFileName);
            String savedFilePath = uploadPath + File.separator + savedFileName;
            filePart.write(savedFilePath);

            double fileSizeMb = filePart.getSize() / (1024.0 * 1024.0);
            String cloudStorageUrl = request.getContextPath() + "/" + UPLOAD_DIR + "/" + userId + "/" + savedFileName;

            // Xử lý thông tin Folder ID đích chuyển lên từ Form hiển thị
            Integer folderId = null;
            String folderIdParam = request.getParameter("folderId");
            if (folderIdParam != null && !folderIdParam.trim().isEmpty() && !folderIdParam.equals("null")) {
                try {
                    folderId = Integer.parseInt(folderIdParam);
                } catch (NumberFormatException ignored) {
                }
            }

            // Đóng gói đối tượng mô hình dữ liệu Document
            Document doc = new Document();
            doc.setUserId(userId);
            doc.setFolderId(folderId);
            doc.setTitle(stripExtension(originalFileName)); // Chỉ lấy phần tên hiển thị làm Title ban đầu
            doc.setFileExtension(fileExtension);            // 🔥 Gán giá trị File Extension mới vào đây
            doc.setCloudStorageUrl(cloudStorageUrl);
            doc.setFileSizeMb(Math.round(fileSizeMb * 100.0) / 100.0);
            doc.setAiParsingStatus("PENDING");
            doc.setSharingPermission("PRIVATE");
            doc.setShareLinkToken(java.util.UUID.randomUUID().toString());
            doc.setFlagged(false);                          // Cập nhật theo JavaBean chuẩn hóa mới
            doc.setCreatedAt(LocalDateTime.now());
            doc.setUpdatedAt(LocalDateTime.now());

            DocumentDAO dao = new DocumentDAO();
            int newDocumentId = dao.insertDocument(doc);

            if (newDocumentId == -1) {
                // Nếu DB Insert lỗi → Tiến hành dọn dẹp xóa file rác vừa ghi trên đĩa tránh tràn ổ cứng
                new File(savedFilePath).delete();
                request.setAttribute("errorMessage", "Lỗi lưu cơ sở dữ liệu hệ thống.");
                request.getRequestDispatcher("/document_upload.jsp").forward(request, response);
                return;
            }

            // Lưu thông tin tạm vào Session để quản lý luồng Xác nhận/Hủy ở Bước 2 & 3
            HttpSession session = request.getSession();
            session.setAttribute("pendingDocumentId", newDocumentId);
            session.setAttribute("pendingDocumentPath", savedFilePath);
            session.setAttribute("pendingDocumentTitle", doc.getTitle());
            session.setAttribute("pendingFileExtension", fileExtension); // Lưu extension để dùng khi rename

            // Điều hướng sang Bước 2 (Form Edit/Confirm)
            response.sendRedirect(request.getContextPath() + "/document_upload.jsp?step=edit&docId=" + newDocumentId);

        } catch (Exception e) {
            System.err.println("[UploadController] CRITICAL UPLOAD ERROR: " + e.getMessage());
            e.printStackTrace();
            request.setAttribute("errorMessage", "Hệ thống trục trặc: " + e.getMessage());
            request.getRequestDispatcher("/document_upload.jsp").forward(request, response);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BƯỚC 2 — Người dùng xác nhận / chỉnh sửa thông tin tài liệu
    // ─────────────────────────────────────────────────────────────────────────
    private void handleConfirm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        Integer documentId = (Integer) session.getAttribute("pendingDocumentId");
        if (documentId == null) {
            response.sendRedirect(request.getContextPath() + "/document_upload.jsp?error=session_expired");
            return;
        }

        int userId = (int) session.getAttribute("userId");
        String newTitle = request.getParameter("title");
        if (newTitle == null || newTitle.trim().isEmpty()) {
            newTitle = (String) session.getAttribute("pendingDocumentTitle");
        }

        String sharingPermission = request.getParameter("sharingPermission");
        sharingPermission = (sharingPermission == null || sharingPermission.trim().isEmpty())
                ? "PRIVATE" : sharingPermission.trim().toUpperCase();

        Integer newFolderId = null;
        String folderIdParam = request.getParameter("folderId");
        if (folderIdParam != null && !folderIdParam.trim().isEmpty() && !folderIdParam.equals("null")) {
            try {
                newFolderId = Integer.parseInt(folderIdParam);
            } catch (NumberFormatException ignored) {
            }
        }

        DocumentDAO dao = new DocumentDAO();
        Document duplicate = dao.findDuplicateByTitle(userId, newTitle, newFolderId);

        if (duplicate != null && duplicate.getDocumentId() != documentId) {
            session.setAttribute("conflictTitle", newTitle);
            session.setAttribute("conflictFolderId", newFolderId);
            session.setAttribute("conflictSharingPermission", sharingPermission);
            session.setAttribute("duplicateDocId", duplicate.getDocumentId());

            response.sendRedirect(request.getContextPath() + "/document_upload.jsp?step=duplicate");
            return;
        }

        // Thêm đoạn này để lấy extension từ Session
        String fileExt = (String) session.getAttribute("pendingFileExtension");
        
        // Cập nhật lời gọi hàm
        String pendingFilePath = (String) session.getAttribute("pendingDocumentPath");
        String newCloudUrl = renameToFinalName(pendingFilePath, userId, newTitle, fileExt, request);

        boolean updated = dao.updateDocumentInfo(documentId, newTitle, newFolderId, sharingPermission, newCloudUrl);
        clearPendingSession(session);

        if (updated) {
            // Định tuyến phản hồi mượt mà về đúng địa chỉ thư mục chứa file đó
            String redirectTarget = "/FolderController?action=viewFolder";
            if (newFolderId != null) {
                redirectTarget += "&folderId=" + newFolderId;
            }
            response.sendRedirect(request.getContextPath() + redirectTarget + "&uploadSuccess=1");
        } else {
            request.setAttribute("errorMessage", "Không thể cập nhật thông tin tài liệu.");
            request.getRequestDispatcher("/document_upload.jsp").forward(request, response);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // XỬ LÝ TRÙNG LẶP FILE — THAY THẾ (Cập nhật bản ghi cũ, giữ metadata gốc)
    // ─────────────────────────────────────────────────────────────────────────
    private void handleReplace(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect(request.getContextPath() + "/document_upload.jsp?error=session_expired");
            return;
        }

        Integer pendingDocId = (Integer) session.getAttribute("pendingDocumentId");
        String pendingFilePath = (String) session.getAttribute("pendingDocumentPath");
        Integer duplicateDocId = (Integer) session.getAttribute("duplicateDocId");
        String conflictTitle = (String) session.getAttribute("conflictTitle");
        Integer conflictFolderId = (Integer) session.getAttribute("conflictFolderId");
        String conflictSharingPermission = (String) session.getAttribute("conflictSharingPermission");

        if (pendingDocId == null || duplicateDocId == null || conflictTitle == null) {
            response.sendRedirect(request.getContextPath() + "/document_upload.jsp?error=session_expired");
            return;
        }

        DocumentDAO dao = new DocumentDAO();
        Document oldDoc = dao.findById(duplicateDocId);
        Document pendingDoc = dao.findById(pendingDocId);

        if (oldDoc != null && pendingDoc != null) {
            // 1. Xóa file vật lý CŨ trên đĩa cứng
            deletePhysicalFile(oldDoc.getCloudStorageUrl(), request);

            // 2. Rename file vật lý MỚI → khớp tên hiển thị
            int userId = (int) session.getAttribute("userId");
            String newCloudUrl = renameToFinalName(pendingFilePath, userId, conflictTitle, pendingDoc.getFileExtension(), request);

            // 3. Cập nhật bản ghi CŨ với thông tin file mới
            //    Giữ nguyên: document_id, created_at, share_link_token, is_flagged
            //    Trigger trg_documents_updated_at tự cập nhật updated_at
            dao.replaceDocumentFile(duplicateDocId, newCloudUrl, pendingDoc.getFileSizeMb(),
                    pendingDoc.getFileExtension(), conflictTitle, conflictFolderId, conflictSharingPermission);

            // 4. Xóa bản ghi TẠM (pending) trong DB — file vật lý đã rename rồi
            dao.deleteDocument(pendingDocId);
        }

        clearPendingSession(session);
        clearDuplicateSession(session);

        String redirectTarget = "/FolderController?action=viewFolder";
        if (conflictFolderId != null) {
            redirectTarget += "&folderId=" + conflictFolderId;
        }
        response.sendRedirect(request.getContextPath() + redirectTarget + "&uploadSuccess=1");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // XỬ LÝ TRÙNG LẶP FILE — GIỮ CẢ HAI (Đổi tên file mới + server khớp web)
    // ─────────────────────────────────────────────────────────────────────────
    private void handleKeepBoth(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        Integer pendingDocId = (Integer) session.getAttribute("pendingDocumentId");
        String pendingFilePath = (String) session.getAttribute("pendingDocumentPath");
        String conflictTitle = (String) session.getAttribute("conflictTitle");
        Integer conflictFolderId = (Integer) session.getAttribute("conflictFolderId");
        String conflictSharingPermission = (String) session.getAttribute("conflictSharingPermission");
        int userId = (int) session.getAttribute("userId");

        if (pendingDocId == null || conflictTitle == null) {
            response.sendRedirect(request.getContextPath() + "/document_upload.jsp?error=session_expired");
            return;
        }

        DocumentDAO dao = new DocumentDAO();
        
        // Lấy extension từ Session
        String fileExt = (String) session.getAttribute("pendingFileExtension");

        // Tạo tên duy nhất (Lúc này hàm này chỉ trả về tên thuần như "BaoCao (1)")
        String uniqueTitle = generateUniqueTitle(dao, userId, conflictTitle, conflictFolderId, pendingDocId);

        // Rename file vật lý (Hàm rename sẽ tự động gắn đuôi fileExt vào)
        String newCloudUrl = renameToFinalName(pendingFilePath, userId, uniqueTitle, fileExt, request);

        boolean updated = dao.updateDocumentInfo(pendingDocId, uniqueTitle, conflictFolderId, conflictSharingPermission, newCloudUrl);
        clearPendingSession(session);
        clearDuplicateSession(session);

        String redirectTarget = "/FolderController?action=viewFolder";
        if (conflictFolderId != null) {
            redirectTarget += "&folderId=" + conflictFolderId;
        }
        response.sendRedirect(request.getContextPath() + redirectTarget + "&uploadSuccess=1");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HỦY TÁC VỤ — Xóa file vật lý + DB
    // ─────────────────────────────────────────────────────────────────────────
    private void handleCancel(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect(request.getContextPath() + "/document_upload.jsp");
            return;
        }

        Integer documentId = (Integer) session.getAttribute("pendingDocumentId");
        String savedFilePath = (String) session.getAttribute("pendingDocumentPath");

        if (savedFilePath != null) {
            File file = new File(savedFilePath);
            if (file.exists()) {
                file.delete();
            }
        }

        if (documentId != null) {
            DocumentDAO dao = new DocumentDAO();
            dao.deleteDocument(documentId);
        }

        clearPendingSession(session);
        clearDuplicateSession(session);

        response.sendRedirect(request.getContextPath() + "/document_upload.jsp?cancelled=1");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHƯƠNG THỨC HỖ TRỢ (HELPERS)
    // ─────────────────────────────────────────────────────────────────────────
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }

    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex > 0) ? fileName.substring(0, dotIndex) : fileName;
    }

    /**
     * Rename file vật lý từ tên tạm → tên hiển thị cuối cùng. File được lưu
     * trong thư mục per-user: uploads/{userId}/
     *
     * @param tempFilePath Đường dẫn file tạm hiện tại trên đĩa
     * @param userId ID người dùng
     * @param finalTitle Tên hiển thị cuối cùng (bao gồm extension, VD:
     * "report.pdf")
     * @param request HttpServletRequest để lấy contextPath
     * @return Cloud storage URL mới (contextPath-relative)
     */
    /**
     * Tên hiển thị (finalTitle) lúc này KHÔNG có extension. Cần nối
     * fileExtension vào để lưu đúng định dạng vật lý.
     */
    private String renameToFinalName(String tempFilePath, int userId, String finalTitle,
            String fileExtension, HttpServletRequest request) {
        String realPath = getServletContext().getRealPath("");
        if (realPath == null) {
            realPath = System.getProperty("java.io.tmpdir");
        }

        String userUploadPath = realPath + File.separator + UPLOAD_DIR + File.separator + userId;
        File userDir = new File(userUploadPath);
        if (!userDir.exists()) {
            userDir.mkdirs();
        }

        // Sanitize tên file hiển thị
        String sanitizedName = sanitizeFileName(finalTitle);

        // Chuẩn bị đuôi file (đảm bảo có dấu chấm)
        String ext = "";
        if (fileExtension != null && !fileExtension.trim().isEmpty() && !fileExtension.equals("unknown")) {
            ext = fileExtension.startsWith(".") ? fileExtension : "." + fileExtension;
        }

        // Ghép tên và đuôi file lại
        File targetFile = new File(userUploadPath + File.separator + sanitizedName + ext);

        int dupCounter = 0;
        while (targetFile.exists()) {
            dupCounter++;
            targetFile = new File(userUploadPath + File.separator + sanitizedName + "_dup" + dupCounter + ext);
        }

        String finalFileName = (dupCounter == 0)
                ? sanitizedName + ext
                : sanitizedName + "_dup" + dupCounter + ext;

        // Thực hiện rename/move file
        File tempFile = new File(tempFilePath);
        if (tempFile.exists()) {
            tempFile.renameTo(targetFile);
        }

        return request.getContextPath() + "/" + UPLOAD_DIR + "/" + userId + "/" + finalFileName;
    }

    /**
     * Xóa file vật lý trên đĩa dựa vào cloudStorageUrl từ DB.
     *
     * @param cloudStorageUrl URL lưu trong DB (VD:
     * /AIStudyHub/uploads/1/report.pdf)
     * @param request HttpServletRequest để lấy contextPath và realPath
     */
    private void deletePhysicalFile(String cloudStorageUrl, HttpServletRequest request) {
        if (cloudStorageUrl == null || cloudStorageUrl.trim().isEmpty()) {
            return;
        }

        String relativePath = cloudStorageUrl;
        String contextPath = request.getContextPath();
        if (relativePath.startsWith(contextPath)) {
            relativePath = relativePath.substring(contextPath.length());
        }
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        String realPath = getServletContext().getRealPath("");
        if (realPath != null) {
            File physicalFile = new File(realPath + File.separator + relativePath.replace("/", File.separator));
            if (physicalFile.exists()) {
                physicalFile.delete();
            }
        }
    }

    private void clearPendingSession(HttpSession session) {
        session.removeAttribute("pendingDocumentId");
        session.removeAttribute("pendingDocumentPath");
        session.removeAttribute("pendingDocumentTitle");
        session.removeAttribute("pendingFileExtension");
    }

    private void clearDuplicateSession(HttpSession session) {
        session.removeAttribute("conflictTitle");
        session.removeAttribute("conflictFolderId");
        session.removeAttribute("conflictSharingPermission");
        session.removeAttribute("duplicateDocId");
    }

    /**
     * Tạo tên duy nhất bằng cách thêm (1), (2),... TRƯỚC đuôi file. VD:
     * "report.pdf" → "report (1).pdf" → "report (2).pdf"
     */
    private String generateUniqueTitle(DocumentDAO dao, int userId, String baseTitle, Integer folderId, int excludeDocId) {
        // baseTitle hiện tại chỉ là tên thuần (VD: "BaoCao")
        int counter = 1;
        String candidate;
        do {
            candidate = baseTitle + " (" + counter + ")";
            counter++;
        } while (dao.titleExistsAtLocation(userId, candidate, folderId, excludeDocId));
        return candidate;
    }
}
