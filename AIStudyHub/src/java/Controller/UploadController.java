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
 * DocumentController — Servlet handling the 3-step document upload flow:
 *
 * POST /DocumentController?action=upload → Step 1: Receive file, save
 * temporarily, insert to DB, redirect to edit form POST
 * /DocumentController?action=confirm → Step 2: User confirms/edits document
 * info → update DB POST /DocumentController?action=cancel → Step 3: User
 * cancels → delete physical file + delete DB record
 *
 * Requirements: - User must be logged in (session must have "userId" attribute)
 * - @MultipartConfig allows receiving multipart/form-data file uploads
 */
@WebServlet(name = "UploadController", urlPatterns = {"/UploadController"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024, // 1 MB — ghi thẳng vào disk nếu vượt ngưỡng này
        maxFileSize = 50 * 1024 * 1024, // 50 MB — giới hạn mỗi file
        maxRequestSize = 55 * 1024 * 1024 // 55 MB — giới hạn toàn request
)
public class UploadController extends HttpServlet {

    // Directory to store files on the server (relative to web root).
    // In production, use an absolute path or cloud storage.
    private static final String UPLOAD_DIR = "uploads";

    // ─────────────────────────────────────────────────────────────────────────
    // doPost — dispatch by action
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // Check login
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        String action = request.getParameter("action");
        if (action == null) {
            action = "";
        }

        if ("upload".equals(action)) {
            handleUpload(request, response);
        } else if ("confirm".equals(action)) {
            handleConfirm(request, response);
        } else if ("cancel".equals(action)) {
            handleCancel(request, response);
        } else if ("replace".equals(action)) {
            handleReplace(request, response);
        } else if ("keepBoth".equals(action)) {
            handleKeepBoth(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid action.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 1 — Upload file → save temporarily → insert to DB → redirect to edit form
    // ─────────────────────────────────────────────────────────────────────────
    private void handleUpload(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            int userId = (int) request.getSession().getAttribute("userId");

            // 1. Safely grab the file part
            Part filePart = request.getPart("file");
            if (filePart == null || filePart.getSize() == 0) {
                System.err.println("[DocumentController] Upload failed: filePart is null or empty.");
                request.setAttribute("errorMessage", "Vui lòng chọn một file hợp lệ.");
                request.getRequestDispatcher("/document_upload.jsp").forward(request, response);
                return;
            }

            // 2. Use native Servlet 3.1 method instead of manual header parsing
            String originalFileName = filePart.getSubmittedFileName();
            if (originalFileName == null || originalFileName.trim().isEmpty()) {
                originalFileName = "document_" + System.currentTimeMillis();
            }

            // 3. Bulletproof Directory Pathing
            String realPath = getServletContext().getRealPath("");
            if (realPath == null) {
                // Fallback if running as a packed WAR
                realPath = System.getProperty("java.io.tmpdir");
            }
            String uploadPath = realPath + File.separator + UPLOAD_DIR;

            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists() && !uploadDir.mkdirs()) {
                throw new IOException("Failed to create upload directory at: " + uploadPath);
            }

            // 4. Save physical file
            String savedFileName = System.currentTimeMillis() + "_" + sanitizeFileName(originalFileName);
            String savedFilePath = uploadPath + File.separator + savedFileName;
            filePart.write(savedFilePath);

            double fileSizeMb = filePart.getSize() / (1024.0 * 1024.0);
            String cloudStorageUrl = request.getContextPath() + "/" + UPLOAD_DIR + "/" + savedFileName;

// 5. Parse optional Folder ID
            Integer folderId = null;
            String folderIdParam = request.getParameter("folderId");
            // 🚀 FIX: Ignore literal "null" strings that HTML forms sometimes send
            if (folderIdParam != null && !folderIdParam.trim().isEmpty() && !folderIdParam.equals("null")) {
                try {
                    folderId = Integer.parseInt(folderIdParam);
                } catch (NumberFormatException ignored) {
                }
            }

            // 6. Database Execution
            Document doc = new Document();
            doc.setUserId(userId);
            doc.setFolderId(folderId);
            doc.setTitle(stripExtension(originalFileName));
            doc.setCloudStorageUrl(cloudStorageUrl);
            doc.setFileSizeMb(Math.round(fileSizeMb * 100.0) / 100.0);
            doc.setAiParsingStatus("PENDING");
            doc.setSharingPermission("PRIVATE");
            doc.setShareLinkToken(java.util.UUID.randomUUID().toString());
            doc.setIsFlagged(false);
            doc.setCreatedAt(LocalDateTime.now());

            DocumentDAO dao = new DocumentDAO();
            int newDocumentId = dao.insertDocument(doc);

            if (newDocumentId == -1) {
                // DB Insert failed → clean up the orphaned file
                new File(savedFilePath).delete();
                request.setAttribute("errorMessage", "Lỗi lưu database. Vui lòng kiểm tra console server.");
                request.getRequestDispatcher("/document_upload.jsp").forward(request, response);
                return;
            }

            // 7. Route to Step 2
            HttpSession session = request.getSession();
            session.setAttribute("pendingDocumentId", newDocumentId);
            session.setAttribute("pendingDocumentPath", savedFilePath);
            session.setAttribute("pendingDocumentTitle", doc.getTitle());

            response.sendRedirect(request.getContextPath() + "/document_upload.jsp?step=edit&docId=" + newDocumentId);

        } catch (Exception e) {
            // Catch ALL exceptions to figure out exactly what "key" is failing
            System.err.println("[DocumentController] CRITICAL UPLOAD ERROR: " + e.getMessage());
            e.printStackTrace();
            request.setAttribute("errorMessage", "Hệ thống gặp lỗi: " + e.getMessage());
            request.getRequestDispatcher("/document_upload.jsp").forward(request, response);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 2 — User confirms / edits document info → update DB
    //          NOW WITH DUPLICATE CHECK!
    // ─────────────────────────────────────────────────────────────────────────
    private void handleConfirm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        // Get documentId from session (set in step 1)
        Integer documentId = (Integer) session.getAttribute("pendingDocumentId");
        if (documentId == null) {
            response.sendRedirect(request.getContextPath() + "/document_upload.jsp?error=session_expired");
            return;
        }

        int userId = (int) session.getAttribute("userId");

        // Read user-submitted data
        String newTitle = request.getParameter("title");
        if (newTitle == null || newTitle.trim().isEmpty()) {
            newTitle = (String) session.getAttribute("pendingDocumentTitle");
        }

        String sharingPermission = request.getParameter("sharingPermission");
        if (sharingPermission == null || sharingPermission.trim().isEmpty()) {
            sharingPermission = "PRIVATE";
        } else {
            sharingPermission = sharingPermission.trim().toUpperCase(); // Enforce uppercase for DB constraints
        }

        Integer newFolderId = null;
        String folderIdParam = request.getParameter("folderId");
        if (folderIdParam != null && !folderIdParam.trim().isEmpty()) {
            try {
                newFolderId = Integer.parseInt(folderIdParam);
            } catch (NumberFormatException ignored) {
            }
        }

        // ── DUPLICATE CHECK ──────────────────────────────────────────────────
        DocumentDAO dao = new DocumentDAO();
        Document duplicate = dao.findDuplicateByTitle(userId, newTitle, newFolderId);

        // Make sure we're not detecting the document itself as a duplicate
        if (duplicate != null && duplicate.getDocumentId() != documentId) {
            // Duplicate found! Save conflict info to session and redirect to conflict page
            session.setAttribute("conflictTitle", newTitle);
            session.setAttribute("conflictFolderId", newFolderId);
            session.setAttribute("conflictSharingPermission", sharingPermission);
            session.setAttribute("duplicateDocId", duplicate.getDocumentId());

            response.sendRedirect(request.getContextPath() + "/document_upload.jsp?step=duplicate");
            return;
        }
        // ─────────────────────────────────────────────────────────────────────

        // No duplicate → proceed normally
        boolean updated = dao.updateDocumentInfo(documentId, newTitle, newFolderId, sharingPermission);

        // Clean up session
        clearPendingSession(session);

        if (updated) {
            // 🚀 UX FIX: Redirect back to the specific folder if they uploaded it inside one
            if (newFolderId != null) {
                response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?folderId=" + newFolderId + "&uploadSuccess=1");
            } else {
                response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?uploadSuccess=1");
            }
        } else {
            request.setAttribute("errorMessage", "Failed to update document info. Please try again.");
            request.getRequestDispatcher("/document_upload.jsp").forward(request, response);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DUPLICATE OPTION 1 — Replace: Delete old file, keep new file with same name
    // ─────────────────────────────────────────────────────────────────────────
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

        DocumentDAO dao = new DocumentDAO();

        // 1. Get the old document to find its physical file
        Document oldDoc = dao.findById(duplicateDocId);
        if (oldDoc != null) {
            // Delete physical file of the old document
            String cloudUrl = oldDoc.getCloudStorageUrl();
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
                    File physicalFile = new File(realPath + File.separator + relativePath.replace("/", File.separator));
                    if (physicalFile.exists()) {
                        boolean deleted = physicalFile.delete();
                        System.out.println("[UploadController] Replaced old physical file: " + physicalFile.getAbsolutePath() + " → " + deleted);
                    }
                }
            }

            // Delete old document from DB
            dao.deleteDocument(duplicateDocId);
        }

        // 2. Update the new document with the confirmed title and folder
        boolean updated = dao.updateDocumentInfo(pendingDocId, conflictTitle, conflictFolderId, conflictSharingPermission);

        // 3. Clean up session
        clearPendingSession(session);
        clearDuplicateSession(session);

        if (updated) {
            if (conflictFolderId != null) {
                response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?folderId=" + conflictFolderId + "&uploadSuccess=replaced");
            } else {
                response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?uploadSuccess=replaced");
            }
        } else {
            response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?error=replace_failed");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DUPLICATE OPTION 2 — Keep Both: Rename new file with auto-increment (1), (2)...
    // ─────────────────────────────────────────────────────────────────────────
    private void handleKeepBoth(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect(request.getContextPath() + "/document_upload.jsp?error=session_expired");
            return;
        }

        Integer pendingDocId = (Integer) session.getAttribute("pendingDocumentId");
        String conflictTitle = (String) session.getAttribute("conflictTitle");
        Integer conflictFolderId = (Integer) session.getAttribute("conflictFolderId");
        String conflictSharingPermission = (String) session.getAttribute("conflictSharingPermission");
        int userId = (int) session.getAttribute("userId");

        if (pendingDocId == null || conflictTitle == null) {
            response.sendRedirect(request.getContextPath() + "/document_upload.jsp?error=session_expired");
            return;
        }

        // Generate a unique title: "file (1)", "file (2)", etc.
        DocumentDAO dao = new DocumentDAO();
        String uniqueTitle = generateUniqueTitle(dao, userId, conflictTitle, conflictFolderId, pendingDocId);

        // Update the new document with the unique title
        boolean updated = dao.updateDocumentInfo(pendingDocId, uniqueTitle, conflictFolderId, conflictSharingPermission);

        // Clean up session
        clearPendingSession(session);
        clearDuplicateSession(session);

        if (updated) {
            if (conflictFolderId != null) {
                response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?folderId=" + conflictFolderId + "&uploadSuccess=kept_both");
            } else {
                response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?uploadSuccess=kept_both");
            }
        } else {
            response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp?error=keep_both_failed");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 3 — User cancels → delete physical file + delete DB record
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

        // 1. Delete physical file from server
        if (savedFilePath != null) {
            File file = new File(savedFilePath);
            if (file.exists()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    System.err.println("[DocumentController] Could not delete file: " + savedFilePath);
                }
            }
        }

        // 2. Delete record from DB
        if (documentId != null) {
            DocumentDAO dao = new DocumentDAO();
            dao.deleteDocument(documentId);
        }

        // 3. Clean up session
        clearPendingSession(session);
        clearDuplicateSession(session);

        // Quay lại trang upload với thông báo đã huỷ
        response.sendRedirect(request.getContextPath() + "/document_upload.jsp?cancelled=1");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper methods
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Extract the filename from the Content-Disposition header of a Part.
     */
    private String extractFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition == null) {
            return null;
        }
        for (String token : contentDisposition.split(";")) {
            token = token.trim();
            if (token.startsWith("filename")) {
                return token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }

    /**
     * Remove special characters from the filename to prevent path traversal.
     */
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }

    /**
     * Remove the file extension from a filename.
     */
    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex > 0) ? fileName.substring(0, dotIndex) : fileName;
    }

    /**
     * Remove upload-related attributes from the session.
     */
    private void clearPendingSession(HttpSession session) {
        session.removeAttribute("pendingDocumentId");
        session.removeAttribute("pendingDocumentPath");
        session.removeAttribute("pendingDocumentTitle");
    }

    /**
     * Remove duplicate-conflict-related attributes from the session.
     */
    private void clearDuplicateSession(HttpSession session) {
        session.removeAttribute("conflictTitle");
        session.removeAttribute("conflictFolderId");
        session.removeAttribute("conflictSharingPermission");
        session.removeAttribute("duplicateDocId");
    }

    /**
     * Generates a unique title by appending (1), (2), (3)... until no conflict is found.
     * Example: "Bài giảng" → "Bài giảng (1)" → "Bài giảng (2)"
     */
    private String generateUniqueTitle(DocumentDAO dao, int userId, String baseTitle, Integer folderId, int excludeDocId) {
        int counter = 1;
        String candidate;
        do {
            candidate = baseTitle + " (" + counter + ")";
            counter++;
        } while (dao.titleExistsAtLocation(userId, candidate, folderId, excludeDocId));

        return candidate;
    }
}
