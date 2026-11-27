package Controller;

import Model.DTO.Document;
import Model.DAO.DocumentDAO;
import Model.DAO.DocumentTextDAO;
import Model.DAO.FolderDAO;
import Model.DTO.User;
import Model.DAO.UserDAO;
import Model.DAO.SubscriptionDAO;
import Model.DTO.Subscription;
import Utils.FileStorage;
import Utils.FileTypes;

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
import java.io.InputStream;
import java.time.LocalDateTime;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

@WebServlet(name = "UploadController", urlPatterns = {"/UploadController"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 100 * 1024 * 1024,
        maxRequestSize = 105 * 1024 * 1024
)
public class UploadController extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(UploadController.class.getName());

    private static final String MSG_SYSTEM_ERROR = "Đã xảy ra lỗi hệ thống. Vui lòng thử lại.";
    /** Trần cứng, trùng với maxFileSize của @MultipartConfig; dùng khi gói không có giới hạn hợp lệ. */
    private static final int HARD_MAX_UPLOAD_MB = 100;

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

    private void handleUpload(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            HttpSession session = request.getSession();
            int userId = (int) session.getAttribute("userId");

            // LƯU SESSION ID TỪ CHATBOT (Nếu có)
            String chatSessionId = request.getParameter("sessionId");
            if (chatSessionId != null && !chatSessionId.trim().isEmpty()) {
                session.setAttribute("returnChatSessionId", chatSessionId);
            }

            clearPendingSession(session);
            clearDuplicateSession(session);

            // folderId đến từ request: phải là thư mục của chính người dùng (F-05), nếu không thì từ chối trước khi ghi file
            Integer folderId = null;
            String folderIdParam = request.getParameter("folderId");
            if (folderIdParam != null && !folderIdParam.trim().isEmpty() && !folderIdParam.equals("null")) {
                try {
                    folderId = Integer.parseInt(folderIdParam);
                } catch (NumberFormatException ignored) {
                }
            }
            if (!isFolderUsable(folderId, userId)) {
                request.setAttribute("errorMessage", "Thư mục không hợp lệ.");
                request.getRequestDispatcher("/document_upload.jsp").forward(request, response);
                return;
            }

            Part filePart = request.getPart("file");
            if (filePart == null || filePart.getSize() == 0) {
                LOG.log(Level.WARNING, "upload rejected reason=empty_file userId={0}", userId);
                request.setAttribute("errorMessage", "Vui lòng chọn một file hợp lệ.");
                request.getRequestDispatcher("/document_upload.jsp").forward(request, response);
                return;
            }

            double newFileSizeMb = filePart.getSize() / (1024.0 * 1024.0);
            DocumentDAO capacityDao = new DocumentDAO();

            double currentStorageMb = capacityDao.getTotalStorageUsed(userId);
            double maxStorageMb = 5 * 1024.0; // 5GB = 5120 MB
            //double maxStorageMb = 5.0; //

            // Nếu là tài khoản FREE (tierId < 3), kiểm tra quota
            // (Này tui check thêm cho chuẩn, lỡ User mua Premium dung lượng vô hạn thì sao)
            Integer tierId = (Integer) session.getAttribute("tierId");
            if (tierId == null) {
                tierId = 2; // Mặc định là Free
            }
            if (tierId < 3 && (currentStorageMb + newFileSizeMb) > maxStorageMb) {
                LOG.log(Level.WARNING, "upload rejected reason=quota userId={0} usedMb={1}", new Object[]{userId, currentStorageMb});
                request.setAttribute("errorMessage", "Không đủ dung lượng trống! File của bạn ("
                        + String.format("%.2f", newFileSizeMb) + " MB) sẽ làm vượt quá giới hạn 5GB của tài khoản miễn phí.");
                request.getRequestDispatcher("/document_upload.jsp").forward(request, response);
                return;
            }

            String originalFileName = filePart.getSubmittedFileName();
            if (originalFileName == null || originalFileName.trim().isEmpty()) {
                originalFileName = "document_" + System.currentTimeMillis();
            }
            // Chỉ lấy phần tên file (một số client gửi cả đường dẫn); tiêu đề lấy từ đây
            originalFileName = originalFileName.substring(Math.max(originalFileName.lastIndexOf('/'), originalFileName.lastIndexOf('\\')) + 1);
            if (originalFileName.trim().isEmpty()) {
                originalFileName = "document_" + System.currentTimeMillis();
            }

            // Danh sách đuôi cho phép (T2.3): kiểm tra trước khi ghi bất cứ thứ gì
            String fileExtension = FileTypes.extensionOf(originalFileName);
            if (!FileTypes.isAllowedExtension(fileExtension)) {
                LOG.log(Level.WARNING, "upload rejected reason=extension userId={0}", userId);
                request.setAttribute("errorMessage", "Định dạng file không được hỗ trợ. Chỉ chấp nhận: " + FileTypes.allowedList() + ".");
                request.getRequestDispatcher("/document_upload.jsp").forward(request, response);
                return;
            }

            int maxFileMb = perFileLimitMb(userId);
            if (filePart.getSize() > maxFileMb * 1024L * 1024L) {
                LOG.log(Level.WARNING, "upload rejected reason=file_size userId={0} limitMb={1}", new Object[]{userId, maxFileMb});
                request.setAttribute("errorMessage", "File quá lớn. Gói của bạn cho phép tối đa " + maxFileMb + " MB cho mỗi file.");
                request.getRequestDispatcher("/document_upload.jsp").forward(request, response);
                return;
            }

            // File nằm ngoài webapp, tên trên đĩa là UUID (Conv §9); cloud_storage_url lưu key
            String storageKey;
            try (InputStream in = filePart.getInputStream()) {
                storageKey = FileStorage.save(userId, in, fileExtension);
            }

            // Nội dung phải khớp đuôi khai báo (vd. file HTML đổi tên thành .pdf bị từ chối)
            if (!FileTypes.contentMatches(fileExtension, FileStorage.resolve(storageKey))) {
                FileStorage.delete(storageKey);
                LOG.log(Level.WARNING, "upload rejected reason=content_mismatch userId={0} ext={1}", new Object[]{userId, fileExtension});
                request.setAttribute("errorMessage", "Nội dung file không khớp với định dạng ." + fileExtension + ".");
                request.getRequestDispatcher("/document_upload.jsp").forward(request, response);
                return;
            }

            double fileSizeMb = filePart.getSize() / (1024.0 * 1024.0);

            Document doc = new Document();
            doc.setUserId(userId);
            doc.setFolderId(folderId);
            doc.setTitle(stripExtension(originalFileName));
            doc.setFileExtension(fileExtension);
            doc.setCloudStorageUrl(storageKey);
            doc.setFileSizeMb(Math.round(fileSizeMb * 100.0) / 100.0);
            doc.setAiParsingStatus("PENDING");
            doc.setSharingPermission("PRIVATE");
            doc.setShareLinkToken(java.util.UUID.randomUUID().toString());
            doc.setFlagged(false);
            doc.setCreatedAt(LocalDateTime.now());
            doc.setUpdatedAt(LocalDateTime.now());

            DocumentDAO dao = new DocumentDAO();
            int newDocumentId = dao.insertDocument(doc);

            if (newDocumentId == -1) {
                FileStorage.delete(storageKey);
                request.setAttribute("errorMessage", "Lỗi lưu cơ sở dữ liệu hệ thống.");
                request.getRequestDispatcher("/document_upload.jsp").forward(request, response);
                return;
            }

            try {
                String extractedText = extractTextFromFile(FileStorage.resolve(storageKey).toString(), fileExtension);
                if (extractedText != null && !extractedText.trim().isEmpty()) {
                    DocumentTextDAO documentTextDAO = new DocumentTextDAO();
                    boolean textSaved = documentTextDAO.saveExtractedText(newDocumentId, extractedText);
                    if (textSaved) {
                        DocumentDAO daoForStatus = new DocumentDAO();
                        daoForStatus.updateAiParsingStatus(newDocumentId, "READY");
                    }
                }
            } catch (Throwable t) {
                LOG.log(Level.WARNING, "text extraction failed docId=" + newDocumentId + " (the file is stored)", t);
            }

            session.setAttribute("pendingDocumentId", newDocumentId);
            session.setAttribute("pendingDocumentKey", storageKey);
            session.setAttribute("pendingDocumentTitle", doc.getTitle());
            session.setAttribute("pendingFileExtension", fileExtension);

            response.sendRedirect(request.getContextPath() + "/document_upload.jsp?step=edit&docId=" + newDocumentId);

        } catch (IllegalStateException e) {
            // Hoặc thiếu cấu hình STORAGE_DIR (AppConfig), hoặc Tomcat báo vượt maxFileSize/maxRequestSize của @MultipartConfig.
            if (e.getMessage() != null && e.getMessage().startsWith("Missing required config")) {
                LOG.log(Level.SEVERE, "upload failed: storage is not configured", e);
                request.setAttribute("errorMessage", MSG_SYSTEM_ERROR);
            } else {
                LOG.log(Level.WARNING, "upload rejected reason=multipart_limit", e);
                request.setAttribute("errorMessage", "File quá lớn. Kích thước tối đa là " + HARD_MAX_UPLOAD_MB + " MB.");
            }
            request.getRequestDispatcher("/document_upload.jsp").forward(request, response);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "upload failed", e);
            request.setAttribute("errorMessage", MSG_SYSTEM_ERROR);
            request.getRequestDispatcher("/document_upload.jsp").forward(request, response);
        }
    }

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

        // 🔥 MỚI: User đang bị SUSPENDED thì không được đặt tài liệu MỚI là PUBLIC
        boolean blockedBySuspend = false;
        if ("PUBLIC".equals(sharingPermission) && isUserSuspended(userId)) {
            sharingPermission = "PRIVATE";
            blockedBySuspend = true;
        }

        Integer newFolderId = null;
        String folderIdParam = request.getParameter("folderId");
        if (folderIdParam != null && !folderIdParam.trim().isEmpty() && !folderIdParam.equals("null")) {
            try {
                newFolderId = Integer.parseInt(folderIdParam);
            } catch (NumberFormatException ignored) {
            }
        }

        if (!isFolderUsable(newFolderId, userId)) {
            request.setAttribute("errorMessage", "Thư mục không hợp lệ.");
            request.getRequestDispatcher("/document_upload.jsp").forward(request, response);
            return;
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

        // File đã nằm đúng chỗ từ bước upload (tên UUID), chỉ cần giữ nguyên key
        String newCloudUrl = (String) session.getAttribute("pendingDocumentKey");
        if (newCloudUrl == null) {
            response.sendRedirect(request.getContextPath() + "/document_upload.jsp?error=session_expired");
            return;
        }

        boolean updated = dao.updateDocumentInfoForOwner(documentId, userId, newTitle, newFolderId, sharingPermission, newCloudUrl);
        clearPendingSession(session);

        if (updated) {
            // KIỂM TRA ĐIỀU HƯỚNG QUAY VỀ CHATBOT
            String returnChatSessionId = (String) session.getAttribute("returnChatSessionId");
            if (returnChatSessionId != null) {
                session.removeAttribute("returnChatSessionId");
                session.setAttribute("newAttachedDocTitle", newTitle);
                response.sendRedirect(request.getContextPath() + "/SessionController?action=viewSession&sessionId=" + returnChatSessionId + "&attached=true");
                return;
            }

            String redirectTarget = "/FolderController?action=viewFolder";
            if (newFolderId != null) {
                redirectTarget += "&folderId=" + newFolderId;
            }
            // 🔥 MỚI: kèm cảnh báo nếu vừa bị chặn public do suspended, để FE có thể hiển thị nếu muốn
            String suspendFlag = blockedBySuspend ? "&warning=account_suspended" : "";
            response.sendRedirect(request.getContextPath() + redirectTarget + "&uploadSuccess=1" + suspendFlag);
        } else {
            request.setAttribute("errorMessage", "Không thể cập nhật thông tin tài liệu.");
            request.getRequestDispatcher("/document_upload.jsp").forward(request, response);
        }
    }

    private void handleReplace(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect(request.getContextPath() + "/document_upload.jsp?error=session_expired");
            return;
        }

        Integer pendingDocId = (Integer) session.getAttribute("pendingDocumentId");
        Integer duplicateDocId = (Integer) session.getAttribute("duplicateDocId");
        String conflictTitle = (String) session.getAttribute("conflictTitle");
        Integer conflictFolderId = (Integer) session.getAttribute("conflictFolderId");
        String conflictSharingPermission = (String) session.getAttribute("conflictSharingPermission");

        if (pendingDocId == null || duplicateDocId == null || conflictTitle == null) {
            response.sendRedirect(request.getContextPath() + "/document_upload.jsp?error=session_expired");
            return;
        }

        int userId = (int) session.getAttribute("userId");

        // 🔥 MỚI: Chặn lại lần nữa ở bước "Ghi đè" phòng trường hợp request bị gọi trực tiếp,
        // bỏ qua bước handleConfirm ở trên (VD gọi thẳng API).
        if ("PUBLIC".equals(conflictSharingPermission) && isUserSuspended(userId)) {
            conflictSharingPermission = "PRIVATE";
        }

        DocumentDAO dao = new DocumentDAO();
        Document oldDoc = dao.findById(duplicateDocId);
        Document pendingDoc = dao.findById(pendingDocId);

        if (oldDoc != null && pendingDoc != null
                && oldDoc.getUserId() == userId && pendingDoc.getUserId() == userId) {
            // File mới của tài liệu cũ chính là file đã tải lên ở bước upload (cùng key)
            String oldKey = oldDoc.getCloudStorageUrl();
            String newKey = pendingDoc.getCloudStorageUrl();

            boolean replaced = dao.replaceDocumentFileForOwner(duplicateDocId, userId, newKey, pendingDoc.getFileSizeMb(),
                    pendingDoc.getFileExtension(), conflictTitle, conflictFolderId, conflictSharingPermission);

            if (replaced) {
                // Chỉ xóa file cũ sau khi DB đã trỏ sang file mới; dòng "pending" chỉ bị xóa khỏi DB (file đã chuyển sang tài liệu cũ)
                dao.deleteDocumentForOwner(pendingDocId, userId);
                FileStorage.delete(oldKey);

                try {
                    String newExtractedText = extractTextFromFile(FileStorage.resolve(newKey).toString(), pendingDoc.getFileExtension());
                    if (newExtractedText != null && !newExtractedText.trim().isEmpty()) {
                        DocumentTextDAO documentTextDAO = new DocumentTextDAO();
                        documentTextDAO.deleteExtractedText(duplicateDocId);
                        documentTextDAO.saveExtractedText(duplicateDocId, newExtractedText);
                        dao.updateAiParsingStatus(duplicateDocId, "READY");
                    }
                } catch (Throwable t) {
                    LOG.log(Level.WARNING, "re-extraction failed docId=" + duplicateDocId, t);
                }
            } else {
                // Không ghi đè được: bỏ bản tải lên, giữ nguyên tài liệu cũ
                LOG.log(Level.WARNING, "replace failed docId={0} userId={1}", new Object[]{duplicateDocId, userId});
                if (dao.deleteDocumentForOwner(pendingDocId, userId)) {
                    FileStorage.delete(newKey);
                }
            }
        }

        clearPendingSession(session);
        clearDuplicateSession(session);

        // KIỂM TRA ĐIỀU HƯỚNG QUAY VỀ CHATBOT
        String returnChatSessionId = (String) session.getAttribute("returnChatSessionId");
        if (returnChatSessionId != null) {
            session.removeAttribute("returnChatSessionId");
            session.setAttribute("newAttachedDocTitle", conflictTitle);
            response.sendRedirect(request.getContextPath() + "/SessionController?action=viewSession&sessionId=" + returnChatSessionId + "&attached=true");
            return;
        }

        String redirectTarget = "/FolderController?action=viewFolder";
        if (conflictFolderId != null) {
            redirectTarget += "&folderId=" + conflictFolderId;
        }
        response.sendRedirect(request.getContextPath() + redirectTarget + "&uploadSuccess=1");
    }

    private void handleKeepBoth(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        Integer pendingDocId = (Integer) session.getAttribute("pendingDocumentId");
        String pendingKey = (String) session.getAttribute("pendingDocumentKey");
        String conflictTitle = (String) session.getAttribute("conflictTitle");
        Integer conflictFolderId = (Integer) session.getAttribute("conflictFolderId");
        String conflictSharingPermission = (String) session.getAttribute("conflictSharingPermission");
        int userId = (int) session.getAttribute("userId");

        if (pendingDocId == null || pendingKey == null || conflictTitle == null) {
            response.sendRedirect(request.getContextPath() + "/document_upload.jsp?error=session_expired");
            return;
        }

        // 🔥 MỚI: Chặn lại lần nữa ở bước "Giữ cả hai" phòng trường hợp request bị gọi trực tiếp,
        // bỏ qua bước handleConfirm ở trên (VD gọi thẳng API).
        if ("PUBLIC".equals(conflictSharingPermission) && isUserSuspended(userId)) {
            conflictSharingPermission = "PRIVATE";
        }

        DocumentDAO dao = new DocumentDAO();
        String uniqueTitle = generateUniqueTitle(dao, userId, conflictTitle, conflictFolderId, pendingDocId);

        boolean updated = dao.updateDocumentInfoForOwner(pendingDocId, userId, uniqueTitle, conflictFolderId, conflictSharingPermission, pendingKey);
        clearPendingSession(session);
        clearDuplicateSession(session);

        // KIỂM TRA ĐIỀU HƯỚNG QUAY VỀ CHATBOT
        String returnChatSessionId = (String) session.getAttribute("returnChatSessionId");
        if (returnChatSessionId != null) {
            session.removeAttribute("returnChatSessionId");
            session.setAttribute("newAttachedDocTitle", uniqueTitle);
            response.sendRedirect(request.getContextPath() + "/SessionController?action=viewSession&sessionId=" + returnChatSessionId + "&attached=true");
            return;
        }

        String redirectTarget = "/FolderController?action=viewFolder";
        if (conflictFolderId != null) {
            redirectTarget += "&folderId=" + conflictFolderId;
        }
        response.sendRedirect(request.getContextPath() + redirectTarget + "&uploadSuccess=1");
    }

    private void handleCancel(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect(request.getContextPath() + "/document_upload.jsp");
            return;
        }

        Integer documentId = (Integer) session.getAttribute("pendingDocumentId");
        String pendingKey = (String) session.getAttribute("pendingDocumentKey");

        // Xóa dòng DB (có điều kiện chủ sở hữu) trước; chỉ khi đúng 1 dòng bị xóa mới xóa file
        if (documentId != null) {
            DocumentDAO dao = new DocumentDAO();
            if (dao.deleteDocumentForOwner(documentId, (int) session.getAttribute("userId")) && pendingKey != null) {
                FileStorage.delete(pendingKey);
            }
        }

        clearPendingSession(session);
        clearDuplicateSession(session);

        // KIỂM TRA ĐIỀU HƯỚNG QUAY VỀ CHATBOT KHI HỦY BỎ
        String returnChatSessionId = (String) session.getAttribute("returnChatSessionId");
        if (returnChatSessionId != null) {
            session.removeAttribute("returnChatSessionId");
            response.sendRedirect(request.getContextPath() + "/SessionController?action=viewSession&sessionId=" + returnChatSessionId);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/document_upload.jsp?cancelled=1");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    /** null (thư mục gốc) luôn hợp lệ; id khác phải thuộc về userId. */
    private boolean isFolderUsable(Integer folderId, int userId) {
        return folderId == null || new FolderDAO().isOwnedBy(folderId, userId);
    }

    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex > 0) ? fileName.substring(0, dotIndex) : fileName;
    }

    /**
     * 🔥 MỚI: Kiểm tra nhanh tài khoản có đang ở trạng thái SUSPENDED hay không.
     * Dùng ở các bước ghi sharing_permission trong luồng upload (confirm / replace / keepBoth)
     * để chặn việc đặt tài liệu MỚI thành PUBLIC khi tài khoản đang bị tạm khóa.
     */
    private boolean isUserSuspended(int userId) {
        UserDAO userDao = new UserDAO();
        User user = userDao.getUserById(userId);
        return user != null && "SUSPENDED".equalsIgnoreCase(user.getStatus());
    }

    /**
     * Giới hạn kích thước MỘT file (MB) theo gói hiện tại của user, đọc từ DB (không tin session).
     * Theo UI (user_dashboard.jsp) subscriptions.max_storage_mb là giới hạn mỗi file (Q-1, chờ xác nhận).
     * Giá trị không hợp lệ (<= 0) hoặc không tìm thấy gói thì dùng trần cứng của @MultipartConfig.
     */
    private int perFileLimitMb(int userId) {
        User user = new UserDAO().getUserById(userId);
        if (user != null) {
            for (Subscription s : new SubscriptionDAO().getAllSubscriptions()) {
                if (s.getTierId() == user.getTierId() && s.getMaxStorageMb() > 0) {
                    return Math.min(s.getMaxStorageMb(), HARD_MAX_UPLOAD_MB);
                }
            }
        }
        return HARD_MAX_UPLOAD_MB;
    }

    private void clearPendingSession(HttpSession session) {
        session.removeAttribute("pendingDocumentId");
        session.removeAttribute("pendingDocumentKey");
        session.removeAttribute("pendingDocumentTitle");
        session.removeAttribute("pendingFileExtension");
    }

    private void clearDuplicateSession(HttpSession session) {
        session.removeAttribute("conflictTitle");
        session.removeAttribute("conflictFolderId");
        session.removeAttribute("conflictSharingPermission");
        session.removeAttribute("duplicateDocId");
    }

    private String generateUniqueTitle(DocumentDAO dao, int userId, String baseTitle, Integer folderId, int excludeDocId) {
        int counter = 1;
        String candidate;
        do {
            candidate = baseTitle + " (" + counter + ")";
            counter++;
        } while (dao.titleExistsAtLocation(userId, candidate, folderId, excludeDocId));
        return candidate;
    }

    private String extractTextFromFile(String filePath, String fileExtension) {
        try {
            switch (fileExtension.toLowerCase()) {
                case "pdf": {
                    try (PDDocument document = Loader.loadPDF(new File(filePath))) {
                        PDFTextStripper stripper = new PDFTextStripper();
                        return stripper.getText(document);
                    }
                }
                case "docx": {
                    try (FileInputStream fis = new FileInputStream(filePath); XWPFDocument docx = new XWPFDocument(fis); XWPFWordExtractor extractor = new XWPFWordExtractor(docx)) {
                        return extractor.getText();
                    }
                }
                case "xlsx": {
                    try (FileInputStream fis = new FileInputStream(filePath); XSSFWorkbook xlsx = new XSSFWorkbook(fis); XSSFExcelExtractor extractor = new XSSFExcelExtractor(xlsx)) {
                        return extractor.getText();
                    }
                }
                case "pptx": {
                    try (FileInputStream fis = new FileInputStream(filePath); XMLSlideShow pptx = new XMLSlideShow(fis)) {
                        StringBuilder sb = new StringBuilder();
                        for (XSLFSlide slide : pptx.getSlides()) {
                            for (XSLFShape shape : slide.getShapes()) {
                                try {
                                    if (shape instanceof XSLFTextShape) {
                                        sb.append(((XSLFTextShape) shape).getText()).append("\n");
                                    }
                                } catch (Throwable innerError) {
                                }
                            }
                        }
                        return sb.toString();
                    }
                }
                case "txt":
                case "md": {
                    return new String(Files.readAllBytes(Paths.get(filePath)), "UTF-8");
                }
                default:
                    LOG.log(Level.INFO, "no text extraction for file type {0}", fileExtension);
                    return null;
            }
        } catch (Throwable t) {
            LOG.log(Level.WARNING, "text extraction error", t);
            return null;
        }
    }
}