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
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@WebServlet(name = "FolderController", urlPatterns = {"/FolderController"})
public class FolderController extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // 1. Kiểm tra trạng thái đăng nhập của tài khoản (Security Guard)
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        int userId = (int) session.getAttribute("userId");
        String action = request.getParameter("action");

        // Cơ chế Failsafe: Nếu không chỉ định action, mặc định sẽ là tải và hiển thị cấu trúc File Explorer
        if (action == null || action.trim().isEmpty()) {
            action = "viewFolder";
        }

        FolderDAO folderDao = new FolderDAO();
        DocumentDAO docDao = new DocumentDAO();

        try {
            if ("createFolder".equals(action)) {
                String folderName = request.getParameter("folderName");
                String currentFolderIdParam = request.getParameter("currentFolderId");

                // 1. Đọc và ép kiểu parentFolderId an toàn
                Integer parentFolderId = null;
                if (currentFolderIdParam != null && !currentFolderIdParam.trim().isEmpty() && !currentFolderIdParam.equals("null")) {
                    try {
                        parentFolderId = Integer.parseInt(currentFolderIdParam);
                    } catch (NumberFormatException ignored) {
                    }
                }

                // 2. Thiết lập đường dẫn quay lại (Redirect) chính xác vị trí thư mục hiện hành sau khi tạo xong
                String redirectUrl = request.getContextPath() + "/FolderController?action=viewFolder";
                if (parentFolderId != null) {
                    redirectUrl += "&folderId=" + parentFolderId;
                }

                if (folderName != null && !folderName.trim().isEmpty()) {
                    String cleanFolderName = folderName.trim();

                    // 🔥 BƯỚC MỚI: KIỂM TRA TRÙNG LẶP
                    boolean exists = folderDao.isFolderNameExists(userId, cleanFolderName, parentFolderId);
                    if (exists) {
                        // Nếu trùng, chuyển hướng về kèm tham số lỗi
                        response.sendRedirect(redirectUrl + "&error=folder_exists");
                        return;
                    }

                    // Khởi tạo thực thể Folder truyền đủ tham số parentFolderId
                    Folder newFolder = new Folder(0, userId, parentFolderId, cleanFolderName, "PRIVATE", LocalDateTime.now());
                    boolean success = folderDao.createFolder(newFolder);

                    if (success) {
                        response.sendRedirect(redirectUrl + "&folderSuccess=created");
                        return;
                    }
                }
                response.sendRedirect(redirectUrl + "&error=create_folder_failed");
            } else if ("deleteFolder".equals(action)) {
                int folderId = Integer.parseInt(request.getParameter("folderId"));

                // ─── CASCADE OPERATIONS: DỌN SẠCH FILE TRÊN ĐĨA CỨNG TRƯỚC ───
                // 1. Gọi hàm đệ quy CTE mới để lấy TOÀN BỘ tài liệu trong thư mục hiện tại và các thư mục con sâu bên trong
                List<Document> allDocsInTree = folderDao.getRecursiveDocumentsInFolder(folderId, userId);

                String realPath = getServletContext().getRealPath("");
                if (realPath == null) {
                    realPath = System.getProperty("java.io.tmpdir");
                }

                // 2. Quét vòng lặp xóa triệt để từng file vật lý trên máy chủ
                for (Document doc : allDocsInTree) {
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

                        File physicalFile = new File(realPath + File.separator + relativePath.replace("/", File.separator));
                        if (physicalFile.exists()) {
                            boolean fileDeleted = physicalFile.delete();
                            System.out.println("[FolderController] Xóa file đệ quy thành công: " + physicalFile.getName() + " -> " + fileDeleted);
                        }
                    }
                }

                // 3. Thực thi hàm xóa đệ quy sạch sẽ toàn bộ dữ liệu trong cơ sở dữ liệu
                boolean success = folderDao.deleteFolderAndDocumentsRecursive(folderId, userId);

                if (success) {
                    response.sendRedirect(request.getContextPath() + "/FolderController?action=viewFolder&folderSuccess=deleted");
                } else {
                    response.sendRedirect(request.getContextPath() + "/FolderController?action=viewFolder&error=delete_folder_failed");
                }
            } else if ("viewFolder".equals(action)) {
                // ─── XỬ LÝ ĐIỀU PHỐI DỮ LIỆU FILE EXPLORER 2 NGĂN ───
                String folderIdParam = request.getParameter("folderId");
                Integer currentFolderId = null;
                if (folderIdParam != null && !folderIdParam.trim().isEmpty() && !folderIdParam.equals("null")) {
                    try {
                        currentFolderId = Integer.parseInt(folderIdParam);
                    } catch (NumberFormatException ignored) {
                    }
                }

                // 1. DỮ LIỆU NGĂN TRÁI: Tải toàn bộ cấu trúc thư mục của người dùng để vẽ cây thư mục
                List<Folder> allFolders = folderDao.getAllFoldersByUserId(userId);

                // 2. DỮ LIỆU NGĂN PHẢI: Trích xuất nội dung động nằm bên trong thư mục hiện hành
                // - Lấy danh sách các thư mục con trực thuộc (Sub-folders)
                List<Folder> childFolders = folderDao.getChildFolders(userId, currentFolderId);
                // - Lấy danh sách các tập tin trực thuộc (Files)
                List<Document> documents = docDao.getDocumentsByFolder(userId, currentFolderId);

                // 3. BREADCRUMB TITLE: Tìm kiếm thông tin chi tiết thư mục hiện tại để hiển thị tiêu đề
                if (currentFolderId != null) {
                    Folder currentFolder = folderDao.getFolderById(currentFolderId);
                    request.setAttribute("currentFolder", currentFolder);
                }

                // Đóng gói đẩy toàn bộ dữ liệu vào Request Scope
                request.setAttribute("allFolders", allFolders);
                request.setAttribute("childFolders", childFolders);
                request.setAttribute("documents", documents);
                request.setAttribute("currentFolderId", currentFolderId);

                // Thực hiện cơ chế Forward nội bộ giữ nguyên luồng để truyền các Attribute sang JSP
                request.getRequestDispatcher("/user_dashboard.jsp").forward(request, response);
            } else if ("editFolder".equals(action)) {
                int folderId = Integer.parseInt(request.getParameter("folderId"));

                Folder folder = folderDao.getFolderById(folderId);
                List<Folder> myFolders = folderDao.getAllFoldersByUserId(userId);

                if (folder != null) {
                    request.setAttribute("folder", folder);
                    request.setAttribute("myFolders", myFolders);
                    request.getRequestDispatcher("/folder_edit.jsp").forward(request, response);
                } else {
                    response.sendRedirect(request.getContextPath() + "/FolderController?action=viewFolder&error=not_found");
                }

            } else if ("updateFolder".equals(action)) {
                int folderId = Integer.parseInt(request.getParameter("folderId"));
                String newName = request.getParameter("folderName");
                String sharingPermission = request.getParameter("sharingPermission");
                String parentFolderIdParam = request.getParameter("parentFolderId");

                Integer newParentId = null;
                if (parentFolderIdParam != null && !parentFolderIdParam.trim().isEmpty() && !parentFolderIdParam.equals("null")) {
                    try {
                        newParentId = Integer.parseInt(parentFolderIdParam);
                    } catch (NumberFormatException ignored) {
                    }
                }

                sharingPermission = (sharingPermission == null || sharingPermission.trim().isEmpty())
                        ? "PRIVATE" : sharingPermission.trim().toUpperCase();

                boolean updated = folderDao.updateFolderInfo(folderId, userId, newName, newParentId, sharingPermission);

                if (updated) {
                    String redirectUrl = "/FolderController?action=viewFolder";
                    if (newParentId != null) {
                        redirectUrl += "&folderId=" + newParentId;
                    }
                    response.sendRedirect(request.getContextPath() + redirectUrl + "&folderSuccess=updated");
                } else {
                    request.setAttribute("errorMessage", "Không thể cập nhật thông tin thư mục.");
                    request.getRequestDispatcher("/FolderController?action=editFolder&folderId=" + folderId).forward(request, response);
                }
            }

        } catch (Exception e) {
            System.err.println("[FolderController Error] " + e.getMessage());
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
