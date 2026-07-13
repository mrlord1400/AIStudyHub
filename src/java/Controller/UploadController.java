package Controller;

import Model.DTO.Document;
import Model.DAO.DocumentDAO;
import Model.DAO.DocumentTextDAO;
import Model.DTO.User;
import Model.DAO.UserDAO;

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
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

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

            Part filePart = request.getPart("file");
            if (filePart == null || filePart.getSize() == 0) {
                System.err.println("[UploadController] Tải lên thất bại: File trống hoặc null.");
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
                System.err.println("[UploadController] Chặn: File làm vượt mức 5GB. (Đã dùng: " + currentStorageMb + "MB)");
                request.setAttribute("errorMessage", "Không đủ dung lượng trống! File của bạn ("
                        + String.format("%.2f", newFileSizeMb) + " MB) sẽ làm vượt quá giới hạn 5GB của tài khoản miễn phí.");
                request.getRequestDispatcher("/document_upload.jsp").forward(request, response);
                return;
            }

            String originalFileName = filePart.getSubmittedFileName();
            if (originalFileName == null || originalFileName.trim().isEmpty()) {
                originalFileName = "document_" + System.currentTimeMillis();
            }

            String fileExtension = "";
            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < originalFileName.length() - 1) {
                fileExtension = originalFileName.substring(dotIndex + 1).toLowerCase().trim();
            } else {
                fileExtension = "unknown";
            }

            String realPath = getServletContext().getRealPath("");
            if (realPath == null) {
                realPath = System.getProperty("java.io.tmpdir");
            }
            String uploadPath = realPath + File.separator + UPLOAD_DIR + File.separator + userId;

            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists() && !uploadDir.mkdirs()) {
                throw new IOException("Không thể khởi tạo thư mục lưu trữ tại: " + uploadPath);
            }

            String uuidPrefix = java.util.UUID.randomUUID().toString().substring(0, 8);
            String savedFileName = "temp_" + uuidPrefix + "_" + sanitizeFileName(originalFileName);
            String savedFilePath = uploadPath + File.separator + savedFileName;
            filePart.write(savedFilePath);

            double fileSizeMb = filePart.getSize() / (1024.0 * 1024.0);
            String cloudStorageUrl = request.getContextPath() + "/" + UPLOAD_DIR + "/" + userId + "/" + savedFileName;

            Integer folderId = null;
            String folderIdParam = request.getParameter("folderId");
            if (folderIdParam != null && !folderIdParam.trim().isEmpty() && !folderIdParam.equals("null")) {
                try {
                    folderId = Integer.parseInt(folderIdParam);
                } catch (NumberFormatException ignored) {
                }
            }

            Document doc = new Document();
            doc.setUserId(userId);
            doc.setFolderId(folderId);
            doc.setTitle(stripExtension(originalFileName));
            doc.setFileExtension(fileExtension);
            doc.setCloudStorageUrl(cloudStorageUrl);
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
                new File(savedFilePath).delete();
                request.setAttribute("errorMessage", "Lỗi lưu cơ sở dữ liệu hệ thống.");
                request.getRequestDispatcher("/document_upload.jsp").forward(request, response);
                return;
            }

            try {
                String extractedText = extractTextFromFile(savedFilePath, fileExtension);
                if (extractedText != null && !extractedText.trim().isEmpty()) {
                    DocumentTextDAO documentTextDAO = new DocumentTextDAO();
                    boolean textSaved = documentTextDAO.saveExtractedText(newDocumentId, extractedText);
                    if (textSaved) {
                        DocumentDAO daoForStatus = new DocumentDAO();
                        daoForStatus.updateAiParsingStatus(newDocumentId, "READY");
                    }
                }
            } catch (Throwable t) {
                System.err.println("[UploadController] Text extraction failed for docId " + newDocumentId + " (File vẫn được upload an toàn): " + t.getMessage());
            }

            session.setAttribute("pendingDocumentId", newDocumentId);
            session.setAttribute("pendingDocumentPath", savedFilePath);
            session.setAttribute("pendingDocumentTitle", doc.getTitle());
            session.setAttribute("pendingFileExtension", fileExtension);

            response.sendRedirect(request.getContextPath() + "/document_upload.jsp?step=edit&docId=" + newDocumentId);

        } catch (Exception e) {
            System.err.println("[UploadController] CRITICAL UPLOAD ERROR: " + e.getMessage());
            e.printStackTrace();
            request.setAttribute("errorMessage", "Hệ thống trục trặc: " + e.getMessage());
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

        String fileExt = (String) session.getAttribute("pendingFileExtension");
        String pendingFilePath = (String) session.getAttribute("pendingDocumentPath");
        String newCloudUrl = renameToFinalName(pendingFilePath, userId, newTitle, fileExt, request);

        boolean updated = dao.updateDocumentInfo(documentId, newTitle, newFolderId, sharingPermission, newCloudUrl);
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
        String pendingFilePath = (String) session.getAttribute("pendingDocumentPath");
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

        if (oldDoc != null && pendingDoc != null) {
            deletePhysicalFile(oldDoc.getCloudStorageUrl(), request);

            String newCloudUrl = renameToFinalName(pendingFilePath, userId, conflictTitle, pendingDoc.getFileExtension(), request);

            dao.replaceDocumentFile(duplicateDocId, newCloudUrl, pendingDoc.getFileSizeMb(),
                    pendingDoc.getFileExtension(), conflictTitle, conflictFolderId, conflictSharingPermission);

            dao.deleteDocument(pendingDocId);

            try {
                String newExtractedText = extractTextFromFile(pendingFilePath, pendingDoc.getFileExtension());
                if (newExtractedText != null && !newExtractedText.trim().isEmpty()) {
                    DocumentTextDAO documentTextDAO = new DocumentTextDAO();
                    documentTextDAO.deleteExtractedText(duplicateDocId);
                    documentTextDAO.saveExtractedText(duplicateDocId, newExtractedText);
                    dao.updateAiParsingStatus(duplicateDocId, "READY");
                }
            } catch (Throwable t) {
                System.err.println("[UploadController] Re-extraction failed: " + t.getMessage());
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
        String pendingFilePath = (String) session.getAttribute("pendingDocumentPath");
        String conflictTitle = (String) session.getAttribute("conflictTitle");
        Integer conflictFolderId = (Integer) session.getAttribute("conflictFolderId");
        String conflictSharingPermission = (String) session.getAttribute("conflictSharingPermission");
        int userId = (int) session.getAttribute("userId");

        if (pendingDocId == null || conflictTitle == null) {
            response.sendRedirect(request.getContextPath() + "/document_upload.jsp?error=session_expired");
            return;
        }

        // 🔥 MỚI: Chặn lại lần nữa ở bước "Giữ cả hai" phòng trường hợp request bị gọi trực tiếp,
        // bỏ qua bước handleConfirm ở trên (VD gọi thẳng API).
        if ("PUBLIC".equals(conflictSharingPermission) && isUserSuspended(userId)) {
            conflictSharingPermission = "PRIVATE";
        }

        DocumentDAO dao = new DocumentDAO();
        String fileExt = (String) session.getAttribute("pendingFileExtension");
        String uniqueTitle = generateUniqueTitle(dao, userId, conflictTitle, conflictFolderId, pendingDocId);
        String newCloudUrl = renameToFinalName(pendingFilePath, userId, uniqueTitle, fileExt, request);

        boolean updated = dao.updateDocumentInfo(pendingDocId, uniqueTitle, conflictFolderId, conflictSharingPermission, newCloudUrl);
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
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
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

        String sanitizedName = sanitizeFileName(finalTitle);
        String ext = "";
        if (fileExtension != null && !fileExtension.trim().isEmpty() && !fileExtension.equals("unknown")) {
            ext = fileExtension.startsWith(".") ? fileExtension : "." + fileExtension;
        }

        File targetFile = new File(userUploadPath + File.separator + sanitizedName + ext);
        int dupCounter = 0;
        while (targetFile.exists()) {
            dupCounter++;
            targetFile = new File(userUploadPath + File.separator + sanitizedName + "_dup" + dupCounter + ext);
        }

        String finalFileName = (dupCounter == 0)
                ? sanitizedName + ext
                : sanitizedName + "_dup" + dupCounter + ext;

        File tempFile = new File(tempFilePath);
        if (tempFile.exists()) {
            tempFile.renameTo(targetFile);
        }

        return request.getContextPath() + "/" + UPLOAD_DIR + "/" + userId + "/" + finalFileName;
    }

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
                    System.out.println("[UploadController] Unsupported file type for text extraction: " + fileExtension);
                    return null;
            }
        } catch (Throwable t) {
            System.err.println("[UploadController] extractTextFromFile error for " + filePath + ": " + t.getMessage());
            return null;
        }
    }
}