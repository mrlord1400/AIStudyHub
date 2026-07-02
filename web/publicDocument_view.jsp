<%@page import="Model.DTO.User"%>
<%@page import="Model.DAO.UserDAO"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="Model.DTO.Document" %>
<%@ page import="Model.DTO.Folder" %>
<%@ page import="Model.DAO.FolderDAO" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%
    // 1. Check Login
    HttpSession userSession = request.getSession(false);
    if (userSession == null || userSession.getAttribute("userId") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    Integer userId = (Integer) userSession.getAttribute("userId");

    // 2. Fetch the Document to view (set by DocumentController?action=viewPublicPage)
    Document doc = (Document) request.getAttribute("document");
    if (doc == null) {
        response.sendRedirect(request.getContextPath() + "/MainController?action=explore");
        return;
    }

    // 3. Lấy thông tin tác giả
    String authorName = (String) request.getAttribute("authorName");
    if (authorName == null) authorName = "Không rõ";

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    String ext = (doc.getFileExtension() != null) ? doc.getFileExtension().toLowerCase().trim() : "";
    String currentPerm = doc.getSharingPermission() != null ? doc.getSharingPermission().toUpperCase() : "PUBLIC";
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
            html.dark .meta-row {
                border-color: #374151;
            }
            html.dark .community-banner {
                background-color: rgba(16, 185, 129, 0.1);
                border-color: rgba(16, 185, 129, 0.3);
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
                .community-banner {
                    @apply flex items-center gap-3 px-5 py-3 bg-emerald-50 border-b border-emerald-200 text-emerald-700 text-sm font-medium;
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
                <button id="btnViewMeta" onclick="showMetaPanel()" class="btn-secondary">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    <span>Xem thông tin</span>
                </button>

                <!-- 2. Download Document -->
                <a href="<%= request.getContextPath()%>/DocumentController?action=downloadDoc&docId=<%= doc.getDocumentId()%>" 
                   class="btn-action bg-indigo-100 text-indigo-700 border-transparent hover:bg-indigo-200 dark:bg-indigo-500 dark:text-white dark:hover:bg-indigo-400 shadow-sm shadow-indigo-500/20">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                    <span>Tải xuống</span>
                </a>

                <!-- KHÔNG CÓ nút "Chỉnh thông tin" và "Delete" vì đây là tài liệu của người khác -->
            </div>
        </header>

        <!-- ═══════════════ COMMUNITY BANNER ═══════════════ -->
        <div class="community-banner">
            <svg class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z"></path>
            </svg>
            <span>
                Tài liệu cộng đồng &mdash; được chia sẻ bởi <strong><%= authorName %></strong>
            </span>
            <%
                String permBadgeClass = "badge-public";
                String permLabel = "Công khai";
                if ("FRIENDS_ONLY".equals(currentPerm)) {
                    permBadgeClass = "badge-friends";
                    permLabel = "Chỉ bạn bè";
                }
            %>
            <span class="badge <%= permBadgeClass %> ml-auto"><%= permLabel %></span>
        </div>

        <!-- ═══════════════ MAIN LAYOUT ═══════════════ -->
        <div class="layout">

            <!-- Content viewer -->
            <div class="viewer-pane">
                <iframe src="<%= request.getContextPath()%>/DocumentController?action=viewDoc&docId=<%= doc.getDocumentId()%>" class="w-full h-full border-0"></iframe>
            </div>

            <!-- ═══════ SIDEBAR: VIEW METADATA (chỉ xem, không sửa) ═══════ -->
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
                    <span class="meta-label">Tác giả</span>
                    <span class="meta-value"><%= authorName %></span>
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
                    <span class="meta-label">Quyền chia sẻ</span>
                    <span class="meta-value">
                        <span class="badge <%= permBadgeClass %>"><%= permLabel %></span>
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

            <!-- KHÔNG CÓ sidebar metaEditPanel vì đây là tài liệu cộng đồng -->
        </div>

        <!-- KHÔNG CÓ modal xóa vì đây là tài liệu cộng đồng -->

        <script>
            // ─── Sidebar toggle logic ───────────────────────────────────────
            function showMetaPanel() {
                document.getElementById('metaViewPanel').classList.remove('sidebar-hidden');
            }

            function closeSidebar() {
                document.getElementById('metaViewPanel').classList.add('sidebar-hidden');
            }
        </script>
    </body>
</html>
