<%@page import="Model.DTO.User"%>
<%@page import="Model.DAO.UserDAO"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="Model.DTO.Document" %>
<%@ page import="Model.DTO.Folder" %>
<%@ page import="Model.DAO.FolderDAO" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%!
    // Hàm đệ quy hỗ trợ tự động build đường dẫn: "Thư mục cha / Thư mục con"
    public String getFolderPath(Folder current, List<Folder> allFolders) {
        if (current.getParentFolderId() == null) {
            return current.getFolderName();
        }
        for (Folder parent : allFolders) {
            if (parent.getFolderId() == current.getParentFolderId()) {
                return getFolderPath(parent, allFolders) + " / " + current.getFolderName();
            }
        }
        return current.getFolderName();
    }
%>
<%
    // 1. Check Login
    HttpSession userSession = request.getSession(false);
    if (userSession == null || userSession.getAttribute("userId") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    Integer userId = (Integer) userSession.getAttribute("userId");

    // 2. Fetch the Document to view (set by DocumentController?action=viewPage)
    Document doc = (Document) request.getAttribute("document");
    if (doc == null) {
        response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp");
        return;
    }

    // 3. Fetch folders for the "edit metadata" folder dropdown
    FolderDAO folderDao = new FolderDAO();
    List<Folder> myFolders = folderDao.getAllFoldersByUserId(userId);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    String error = request.getParameter("error");

    String ext = (doc.getFileExtension() != null) ? doc.getFileExtension().toLowerCase().trim() : "";
    String currentPerm = doc.getSharingPermission() != null ? doc.getSharingPermission().toUpperCase() : "PRIVATE";
%>
<!DOCTYPE html>
<html lang="vi" class="dark">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title><%= doc.getTitle() != null ? doc.getTitle() : "Xem tài liệu"%> - AI Study Hub</title>

        <script src="https://cdn.tailwindcss.com"></script>
        <script>
            tailwind.config = {
                darkMode: 'class'
            }
        </script>
        <link rel="preconnect" href="https://fonts.googleapis.com">
        <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">

        <style type="text/tailwindcss">
            html.dark body {
                background-color: #111827;
                color: #f3f4f6;
            }
            html.dark .topbar {
                background-color: #1f2937;
                border-color: #374151;
            }
            html.dark .doc-title {
                color: #ffffff;
            }
            html.dark .viewer-pane {
                background-color: #1f2937;
            }
            html.dark .sidebar-panel {
                background-color: #1f2937;
                border-color: #374151;
            }
            html.dark .meta-label {
                color: #9ca3af;
            }
            html.dark .meta-value {
                color: #f3f4f6;
            }
            html.dark .form-input {
                background-color: #374151;
                border-color: #4b5563;
                color: #ffffff;
            }
            html.dark .form-input:focus {
                background-color: #1f2937;
                border-color: #6366f1;
            }
            html.dark .form-label {
                color: #e5e7eb;
            }
            html.dark .btn-secondary {
                background-color: #374151;
                border-color: #4b5563;
                color: #e5e7eb;
            }
            html.dark .btn-secondary:hover {
                background-color: #4b5563;
            }
            html.dark .modal-card {
                background-color: #1f2937;
                border-color: #374151;
            }
            html.dark .meta-row {
                border-color: #374151;
            }

            @layer components {
                body {
                    @apply min-h-screen w-full text-gray-800 bg-[#f8f9fa] font-sans transition-colors duration-200;
                }
                .topbar {
                    @apply flex items-center justify-between px-6 py-4 bg-white border-b border-gray-100 shadow-sm sticky top-0 z-30 transition-colors duration-200;
                }
                .doc-title {
                    @apply text-lg font-bold text-gray-900 truncate max-w-[40vw];
                }
                .btn-action {
                    @apply flex items-center space-x-2 px-4 py-2.5 rounded-xl font-semibold text-sm transition-colors shadow-sm cursor-pointer border;
                }
                .btn-primary {
                    @apply btn-action bg-[#5c3cf5] text-white border-transparent hover:bg-indigo-700;
                }
                .btn-secondary {
                    @apply btn-action bg-white border-gray-200 text-gray-700 hover:bg-gray-50;
                }
                .btn-danger {
                    @apply btn-action bg-white border-gray-200 text-red-600 hover:bg-red-50 hover:border-red-200;
                }
                .layout {
                    @apply flex w-full;
                    height: calc(100vh - 73px);
                }
                .viewer-pane {
                    @apply flex-1 bg-gray-100 transition-colors duration-200;
                }
                .sidebar-panel {
                    @apply w-[380px] bg-white border-l border-gray-100 shadow-sm overflow-y-auto flex-shrink-0 transition-colors duration-200;
                }
                .sidebar-hidden {
                    @apply hidden;
                }
                .sidebar-header {
                    @apply flex items-center justify-between px-5 py-4 border-b border-gray-100 dark:border-gray-700;
                }
                .sidebar-title {
                    @apply text-base font-bold text-gray-900 dark:text-white;
                }
                .meta-row {
                    @apply flex flex-col gap-1 px-5 py-3.5 border-b border-gray-100;
                }
                .meta-label {
                    @apply text-xs font-semibold text-gray-400 uppercase tracking-wider;
                }
                .meta-value {
                    @apply text-sm font-medium text-gray-800 break-words;
                }
                .form-label {
                    @apply block text-sm font-semibold text-gray-700 mb-2;
                }
                .form-input {
                    @apply w-full px-4 py-2.5 rounded-xl border border-gray-200 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all bg-gray-50 focus:bg-white text-sm;
                }
                .modal-overlay {
                    @apply fixed inset-0 z-50 hidden bg-gray-900/60 backdrop-blur-sm flex justify-center items-center;
                }
                .modal-card {
                    @apply bg-white w-full max-w-md rounded-2xl p-6 shadow-2xl border border-gray-100 transition-colors duration-200;
                }
                .badge {
                    @apply inline-flex items-center px-2.5 py-1 rounded-full text-xs font-bold;
                }
                .badge-private {
                    @apply bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300;
                }
                .badge-friends {
                    @apply bg-blue-100 text-blue-700 dark:bg-blue-950/50 dark:text-blue-400;
                }
                .badge-public {
                    @apply bg-emerald-100 text-emerald-700 dark:bg-emerald-950/50 dark:text-emerald-400;
                }
            }
        </style>
    </head>
    <body>

        <!-- ═══════════════ TOP BAR ═══════════════ -->
        <header class="topbar">
            <div class="flex items-center space-x-4 min-w-0">
                <button onclick="window.close()" class="p-2.5 bg-white border border-gray-200 rounded-xl hover:bg-gray-50 text-gray-600 transition-colors dark:bg-gray-800 dark:border-gray-700 dark:text-gray-300" title="Đóng tab">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"></path></svg>
                </button>
                <div class="min-w-0">
                    <h1 class="doc-title"><%= doc.getTitle() != null ? doc.getTitle() : "Tài liệu"%></h1>
                    <p class="text-xs text-gray-400 font-medium mt-0.5">
                        <%= doc.getFileExtension() != null ? doc.getFileExtension().toUpperCase() : ""%>
                        &middot; <%= doc.getFileSizeMb()%> MB
                    </p>
                </div>
            </div>

            <div class="flex items-center gap-3">
                <!-- 1. Xem metadata -->
                <button id="btnViewMeta" onclick="showMetaPanel('view')" class="btn-secondary">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    <span>Xem thông tin</span>
                </button>

                <!-- 2. Edit metadata -->
                <button id="btnEditMeta" onclick="showMetaPanel('edit')" class="btn-secondary">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path></svg>
                    <span>Chỉnh thông tin</span>
                </button>

                <!-- 3. Download Document -->
                <a href="<%= request.getContextPath()%>/DocumentController?action=downloadDoc&docId=<%= doc.getDocumentId()%>" 
                   class="btn-action bg-indigo-100 text-indigo-700 border-transparent hover:bg-indigo-200 dark:bg-indigo-500 dark:text-white dark:hover:bg-indigo-400 shadow-sm shadow-indigo-500/20">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                    <span>Tải xuống</span>
                </a>

                <!-- 4. Delete -->
                <button onclick="openDeleteModal()" class="btn-danger">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                    <span>Delete</span>
                </button>
            </div>
        </header>

        <!-- ═══════════════ MAIN LAYOUT ═══════════════ -->
        <div class="layout">

            <!-- Content viewer -->
            <div class="viewer-pane">
                <iframe src="<%= request.getContextPath()%>/DocumentController?action=viewDoc&docId=<%= doc.getDocumentId()%>" class="w-full h-full border-0"></iframe>
            </div>

            <!-- ═══════ SIDEBAR: VIEW METADATA ═══════ -->
            <aside id="metaViewPanel" class="sidebar-panel sidebar-hidden">
                <div class="sidebar-header">
                    <h2 class="sidebar-title">Thông tin tài liệu</h2>
                    <button onclick="closeSidebar()" class="p-1.5 text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-950/30 rounded-lg transition-colors">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"></path></svg>
                    </button>
                </div>

                <div class="meta-row">
                    <span class="meta-label">Tên tài liệu</span>
                    <span class="meta-value"><%= doc.getTitle() != null ? doc.getTitle() : "—"%></span>
                </div>
                <div class="meta-row">
                    <span class="meta-label">Định dạng file</span>
                    <span class="meta-value"><%= ext.isEmpty() ? "—" : ext.toUpperCase()%></span>
                </div>
                <div class="meta-row">
                    <span class="meta-label">Dung lượng</span>
                    <span class="meta-value"><%= doc.getFileSizeMb()%> MB</span>
                </div>
                <div class="meta-row">
                    <span class="meta-label">Thư mục</span>
                    <span class="meta-value">
                        <%
                            String folderPath = "Thư mục gốc";
                            if (doc.getFolderId() != null && myFolders != null) {
                                for (Folder f : myFolders) {
                                    if (f.getFolderId() == doc.getFolderId()) {
                                        folderPath = getFolderPath(f, myFolders);
                                        break;
                                    }
                                }
                            }
                        %>
                        <%= folderPath%>
                    </span>
                </div>
                <div class="meta-row">
                    <span class="meta-label">Quyền chia sẻ</span>
                    <span class="meta-value">
                        <%
                            String permBadgeClass = "badge-private";
                            String permLabel = "Riêng tư";
                            if ("FRIENDS_ONLY".equals(currentPerm)) {
                                permBadgeClass = "badge-friends";
                                permLabel = "Chỉ bạn bè";
                            } else if ("PUBLIC".equals(currentPerm)) {
                                permBadgeClass = "badge-public";
                                permLabel = "Công khai";
                            }
                        %>
                        <span class="badge <%= permBadgeClass%>"><%= permLabel%></span>
                    </span>
                </div>
                <div class="meta-row">
                    <span class="meta-label">Trạng thái xử lý AI</span>
                    <span class="meta-value"><%= doc.getAiParsingStatus() != null ? doc.getAiParsingStatus() : "—"%></span>
                </div>
                <div class="meta-row">
                    <span class="meta-label">Ngày tạo</span>
                    <span class="meta-value"><%= doc.getCreatedAt() != null ? doc.getCreatedAt().format(formatter) : "—"%></span>
                </div>
                <div class="meta-row">
                    <span class="meta-label">Cập nhật lần cuối</span>
                    <span class="meta-value"><%= doc.getUpdatedAt() != null ? doc.getUpdatedAt().format(formatter) : "—"%></span>
                </div>
            </aside>

            <!-- ═══════ SIDEBAR: EDIT METADATA (inline form) ═══════ -->
            <aside id="metaEditPanel" class="sidebar-panel sidebar-hidden">
                <div class="sidebar-header">
                    <h2 class="sidebar-title">Chỉnh sửa metadata</h2>
                    <button onclick="closeSidebar()" class="p-1.5 text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-950/30 rounded-lg transition-colors">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"></path></svg>
                    </button>
                </div>

                <form action="<%= request.getContextPath()%>/DocumentController" method="POST" class="px-5 py-4 space-y-5">
                    <input type="hidden" name="action" value="updateDoc" />
                    <input type="hidden" name="docId" value="<%= doc.getDocumentId()%>" />

                    <div>
                        <label class="form-label">Tên tài liệu <span class="text-red-500">*</span></label>
                        <input type="text" name="title" value="<%= doc.getTitle() != null ? doc.getTitle().replace("\"", "&quot;") : ""%>" required class="form-input" placeholder="Nhập tên tài liệu..." />
                    </div>

                    <div>
                        <label class="form-label">Thư mục lưu trữ</label>
                        <select name="folderId" class="form-input">
                            <option value="" <%= doc.getFolderId() == null ? "selected" : ""%>>-- Lưu bên ngoài (Không chọn thư mục) --</option>
                            <% if (myFolders != null) {
                                    for (Folder f : myFolders) {
                                        boolean isSelected = (doc.getFolderId() != null && doc.getFolderId() == f.getFolderId());
                                        String fullPath = getFolderPath(f, myFolders);
                            %>
                            <option value="<%= f.getFolderId()%>" <%= isSelected ? "selected" : ""%>>
                                <%= fullPath.replace("<", "&lt;").replace(">", "&gt;")%>
                            </option>
                            <%      }
                                }%>
                        </select>
                    </div>

                    <div>
                        <label class="form-label">Quyền chia sẻ</label>
                        <select name="sharingPermission" class="form-input">
                            <option value="PRIVATE" <%= "PRIVATE".equals(currentPerm) ? "selected" : ""%>>Riêng tư (Chỉ mình tôi)</option>
                            <option value="FRIENDS_ONLY" <%= "FRIENDS_ONLY".equals(currentPerm) ? "selected" : ""%>>Chỉ bạn bè</option>
                            <option value="PUBLIC" <%= "PUBLIC".equals(currentPerm) ? "selected" : ""%>>Công khai (Mọi người có thể xem)</option>
                        </select>
                    </div>

                    <p class="text-xs text-gray-400">Sau khi lưu, bạn sẽ được đưa về trang Dashboard.</p>

                    <div class="flex items-center justify-end gap-3 pt-2">
                        <button type="button" onclick="closeSidebar()" class="btn-secondary">Hủy</button>
                        <button type="submit" class="btn-primary">Lưu thay đổi</button>
                    </div>
                </form>
            </aside>
        </div>

        <!-- ═══════════════ MODAL: DELETE CONFIRM ═══════════════ -->
        <div id="deleteModal" class="modal-overlay">
            <div class="modal-card">
                <div class="flex items-start space-x-4 mb-5">
                    <div class="p-3 bg-red-50 dark:bg-red-950/30 rounded-xl text-red-500 flex-shrink-0">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z"></path></svg>
                    </div>
                    <div>
                        <h2 class="text-lg font-bold text-gray-900 dark:text-white">Xác nhận xóa tài liệu</h2>
                        <p class="text-sm text-gray-400 mt-1">Bạn có chắc chắn muốn xóa <strong class="text-gray-700 dark:text-gray-200">"<%= doc.getTitle()%>"</strong>? Hành động này không thể hoàn tác.</p>
                    </div>
                </div>

                <div class="flex items-center justify-end gap-3">
                    <button type="button" onclick="closeDeleteModal()" class="btn-secondary">Hủy</button>
                    <a href="<%= request.getContextPath()%>/DocumentController?action=deleteDoc&docId=<%= doc.getDocumentId()%>" class="btn-danger !bg-red-600 !text-white !border-transparent hover:!bg-red-700">
                        Xóa tài liệu
                    </a>
                </div>
            </div>
        </div>

        <script>
            // ─── Sidebar toggle logic ───────────────────────────────────────
            function showMetaPanel(mode) {
                const viewPanel = document.getElementById('metaViewPanel');
                const editPanel = document.getElementById('metaEditPanel');

                if (mode === 'view') {
                    viewPanel.classList.remove('sidebar-hidden');
                    editPanel.classList.add('sidebar-hidden');
                } else {
                    editPanel.classList.remove('sidebar-hidden');
                    viewPanel.classList.add('sidebar-hidden');
                }
            }

            function closeSidebar() {
                document.getElementById('metaViewPanel').classList.add('sidebar-hidden');
                document.getElementById('metaEditPanel').classList.add('sidebar-hidden');
            }

            // ─── Permission modal ────────────────────────────────────────────
            function openPermissionModal() {
                document.getElementById('permissionModal').classList.add('flex');
                document.getElementById('permissionModal').classList.remove('hidden');
            }
            function closePermissionModal() {
                document.getElementById('permissionModal').classList.add('hidden');
                document.getElementById('permissionModal').classList.remove('flex');
            }

            // ─── Delete modal ─────────────────────────────────────────────────
            function openDeleteModal() {
                document.getElementById('deleteModal').classList.add('flex');
                document.getElementById('deleteModal').classList.remove('hidden');
            }
            function closeDeleteModal() {
                document.getElementById('deleteModal').classList.add('hidden');
                document.getElementById('deleteModal').classList.remove('flex');
            }
        </script>
    </body>
</html>
