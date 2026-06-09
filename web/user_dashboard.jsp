<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="Model.Document" %>
<%@ page import="Model.DocumentDAO" %>
<%@ page import="Model.Folder" %>
<%@ page import="Model.FolderDAO" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%
    // 1. Kiểm tra trạng thái đăng nhập của người dùng
    HttpSession userSession = request.getSession(false);
    if (userSession == null || userSession.getAttribute("userId") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    int userId = (Integer) userSession.getAttribute("userId");
    String username = (String) userSession.getAttribute("username");
    String role = (String) userSession.getAttribute("role");
    Integer tierId = (Integer) userSession.getAttribute("tierId");

    // ------------------------------------------------------------------
    // FIX 1: QUẢN LÝ QUYỀN (ROLE)
    // Ép kiểu Quyền: Bất kỳ ai không phải ADMIN thì đều mặc định là quyền STUDENT
    if (role == null || !"ADMIN".equalsIgnoreCase(role.trim())) {
        role = "STUDENT"; 
    } else {
        role = "ADMIN";
    }

    // ------------------------------------------------------------------
    // FIX 2: QUẢN LÝ GÓI (TIER)
    // Theo DB hệ thống: tierId = 2 là FREE, tierId = 3 là PREMIUM.
    if (tierId == null || tierId < 2) {
        tierId = 2; // Mặc định gán 2 cho người mới đăng ký (Gói FREE)
    }

    // Từ tier 3 trở lên mới được hệ thống nhận diện là tài khoản Premium
    boolean isPremiumUser = (tierId >= 3);
    
    // Khởi tạo số dư ví Coin
    Integer userBalance = (Integer) userSession.getAttribute("balance");
    if (userBalance == null) {
        userBalance = 0;
    }

    long maxUploadSizeBytes = isPremiumUser ? 100L * 1024 * 1024 : 50L * 1024 * 1024;
    double maxStorageGb = isPremiumUser ? 55.0 : 5.0; // Gói Free có 5GB, Gói Premium có 55GB

    String folderIdParam = request.getParameter("folderId");
    Integer currentFolderId = null;
    if (folderIdParam != null && !folderIdParam.trim().isEmpty() && !folderIdParam.equals("null")) {
        try {
            currentFolderId = Integer.parseInt(folderIdParam);
        } catch (Exception e) {
            // Xử lý ngoại lệ an toàn
        }
    }

    FolderDAO folderDao = new FolderDAO();
    DocumentDAO docDao = new DocumentDAO();
    List<Folder> myFolders = new ArrayList<>();

    if (currentFolderId == null) {
        myFolders = folderDao.getFoldersByUserId(userId);
    }
    List<Document> myDocuments = docDao.getDocumentsByFolder(userId, currentFolderId);

    // Tính toán dung lượng lưu trữ động thực tế của tài khoản
    List<Document> allDocs = docDao.getDocumentsByUserId(userId);
    double totalSizeMb = 0.0;
    if (allDocs != null) {
        for (Document doc : allDocs) {
            totalSizeMb += doc.getFileSizeMb();
        }
    }
    double totalSizeGb = totalSizeMb / 1024.0;
    double storagePercent = (totalSizeGb / maxStorageGb) * 100.0;
    if (storagePercent > 100) {
        storagePercent = 100;
    }
%>
<!DOCTYPE html>
<html lang="vi" class="dark">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Tài liệu của tôi - AI Study Hub</title>

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
            html.dark .page-body { background-color: #111827; color: #f3f4f6; }
            html.dark .sidebar { background-color: #1f2937; border-color: #374151; }
            html.dark .brand-text { color: #ffffff; }
            html.dark .nav-link { color: #d1d5db; }
            html.dark .nav-link:hover { background-color: #374151; }
            html.dark .nav-link-active { background-color: rgba(49, 46, 129, 0.6); color: #818cf8; }
            html.dark .user-name { color: #ffffff; }
            html.dark .user-profile-link:hover { background-color: #374151; }
            html.dark .logout-btn { color: #9ca3af; }
            html.dark .logout-btn:hover { background-color: rgba(127, 29, 29, 0.3); color: #f87171; }
            html.dark .btn-secondary { background-color: #1f2937; border-color: #374151; color: #e5e7eb; }
            html.dark .btn-secondary:hover { background-color: #374151; }
            html.dark .file-card { background-color: #1f2937; border-color: #374151; }
            html.dark .file-title { color: #f3f4f6; }
            html.dark .file-icon-box { background-color: rgba(55, 65, 81, 0.5); }
            html.dark .empty-state-box { background-color: #1f2937 !important; border-color: #374151 !important; }
            html.dark .empty-state-icon { background-color: #374151 !important; color: #d1d5db !important; }
            html.dark .empty-state-text { color: #9ca3af !important; }
            html.dark #welcomeModal > div { background-color: #1f2937 !important; border-color: #374151 !important; }
            html.dark #welcomeModal h2 { color: #ffffff !important; }
            html.dark #welcomeModal p { color: #9ca3af !important; }
            html.dark #welcomeModal h4 { color: #e5e7eb !important; }
            html.dark #welcomeModal span { color: #9ca3af !important; }
            html.dark #welcomeModal .bg-gray-50 { background-color: #2d3748 !important; }
            html.dark #welcomeModal .text-gray-800 { color: #f3f4f6 !important; }
            html.dark #welcomeModal .text-gray-500 { color: #cbd5e0 !important; }
            html.dark #createFolderModal > div { background-color: #1f2937; color: #ffffff; }
            html.dark #createFolderModal input { background-color: #374151; border-color: #4b5563; color: #ffffff; }
            html.dark #fileViewerModal > div { background-color: #1f2937; }
            html.dark #modalFileTitle { color: #ffffff; }

            @layer components {
                .page-body { @apply flex min-h-screen w-full text-gray-800 bg-[#f8f9fa] font-sans transition-colors duration-200; }
                .sidebar { @apply w-64 bg-white border-r border-gray-100 flex flex-col justify-between p-4 flex-shrink-0 min-h-screen shadow-sm z-10 transition-colors duration-200; }
                .brand-container { @apply flex items-center space-x-3 px-2 py-1; }
                .brand-logo { @apply w-9 h-9 bg-indigo-600 rounded-xl flex items-center justify-center text-white shadow-sm shadow-indigo-600/20; }
                .brand-text { @apply font-bold text-gray-900 text-base tracking-tight; }
                .nav-link { @apply flex items-center space-x-3 px-4 py-2.5 text-gray-600 hover:bg-gray-50 rounded-xl font-medium text-sm transition-all w-full text-left; }
                .nav-link-active { @apply flex items-center space-x-3 px-4 py-2.5 bg-indigo-50 text-indigo-600 rounded-xl font-semibold text-sm transition-colors w-full text-left; }
                .wallet-widget { @apply w-full bg-gradient-to-br from-purple-500 to-indigo-600 text-white p-4 rounded-2xl shadow-md shadow-indigo-600/10 relative overflow-hidden; }
                .wallet-header { @apply flex justify-between items-center opacity-85; }
                .wallet-title { @apply text-xs font-medium tracking-wide; }
                .wallet-balance { @apply text-xl font-bold mt-2 tracking-tight; }
                .user-area { @apply pt-2 border-t border-gray-100 flex flex-col gap-1; }
                .user-profile-link { @apply flex items-center space-x-3 px-2 py-2 rounded-xl hover:bg-gray-50 transition-colors cursor-pointer; }
                .user-avatar { @apply w-8 h-8 rounded-full bg-indigo-100 flex items-center justify-center text-indigo-700 flex-shrink-0 font-bold text-xs uppercase; }
                .user-info { @apply flex-1 min-w-0; }
                .user-name { @apply text-sm font-bold text-gray-900 truncate; }
                .user-role { @apply text-[11px] text-gray-400 font-medium; }
                .logout-btn { @apply w-full flex items-center space-x-2.5 px-2 py-2 rounded-xl text-sm font-medium text-gray-500 hover:text-red-500 hover:bg-red-50 transition-colors text-left; }
                .main-content { @apply flex-1 p-8 overflow-y-auto h-screen relative; }
                .header-container { @apply flex justify-between items-center mb-6; }
                .page-title { @apply text-2xl font-bold text-gray-900 tracking-tight; }
                .btn-primary { @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-[#5c3cf5] text-white rounded-xl font-semibold hover:bg-indigo-700 transition-colors text-sm shadow-sm shadow-indigo-100 cursor-pointer; }
                .btn-secondary { @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-white border border-gray-200 text-gray-700 rounded-xl font-semibold hover:bg-gray-50 transition-colors text-sm shadow-sm cursor-pointer; }
                .file-grid { @apply grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5; }
                .file-card { @apply bg-white border border-gray-100 rounded-2xl p-5 hover:shadow-md transition-all cursor-pointer; }
                .file-icon-wrapper { @apply mb-5 flex justify-between items-start; }
                .file-icon-box { @apply p-4 rounded-[16px] flex items-center justify-center; }
                .file-title { @apply font-semibold text-gray-800 text-[15px] mb-1 truncate; }
                .file-size { @apply text-xs text-gray-400 font-medium mb-3; }
                .file-grid .file-card .file-date { @apply text-[11px] text-gray-400 font-medium; }
            }
        </style>
    </head>
    <body class="page-body">

        <div id="toastContainer" class="fixed top-5 right-5 z-[200] flex flex-col gap-3 pointer-events-none"></div>

        <!-- SIDEBAR COMPONENT -->
        <aside class="sidebar">
            <div class="space-y-6 w-full">
                <div class="brand-container">
                    <div class="brand-logo">
                        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21.42 10.922a1 1 0 0 0-.019-1.838L12.83 5.18a2 2 0 0 0-1.66 0L2.6 9.08a1 1 0 0 0 0 1.832l8.57 3.908a2 2 0 0 0 1.66 0z"/><path d="M6 18.8V13.5"/><path d="M18 13.5v5.3a2.1 2.1 0 0 1-2 2h-8a2.1 2.1 0 0 1-2-2v-5.3"/></svg>
                    </div>
                    <span class="brand-text">AI Study Hub</span>
                </div>

                <nav class="space-y-1 w-full">
                    <a href="<%= request.getContextPath()%>/user_dashboard.jsp" class="nav-link-active">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z"/></svg>
                        <span>Tài liệu của tôi</span>
                    </a>
                    <a href="<%= request.getContextPath()%>/MainController?action=explore" class="nav-link">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
                        <span>Khám phá tài liệu</span>
                    </a>
                    <a href="AIChatbot.jsp" class="nav-link">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 8V4H8"/><rect width="16" height="12" x="4" y="8" rx="2"/><path d="M2 14h2"/><path d="M20 14h2"/><path d="M15 13v2"/><path d="M9 13v2"/></svg>
                        <span>AI Chatbot</span>
                    </a>
                    <a href="CreditWallet.jsp" class="nav-link">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="20" height="14" x="2" y="5" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/><path d="M16 14h2"/></svg>
                        <span>Ví cá nhân</span>
                    </a>
                    <a href="Membership.jsp" class="nav-link text-amber-600 !text-amber-600 hover:bg-amber-50 dark:!text-amber-500 dark:hover:bg-amber-950/30">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 4l3 12h14l3-12-6 7-4-7-4 7-6-7z"/><path d="M5 20h14"/></svg>
                        <span class="font-semibold">Nâng cấp Premium</span>
                    </a>
                </nav>
            </div>

            <div class="w-full space-y-4">
                <div class="wallet-widget">
                    <div class="wallet-header">
                        <span class="wallet-title">Số dư ví</span>
                    </div>
                    <div class="wallet-balance"><%= String.format("%,d", userBalance)%> Coin</div>
                </div>

                <div class="user-area">
                    <div class="flex items-center justify-between w-full">
                        <a href="<%= request.getContextPath()%>/MainController?action=profile" class="user-profile-link flex-1 min-w-0">
                            <!-- FIX 3: Luôn sử dụng username gốc, cắt ký tự đầu in hoa làm Avatar -->
                            <div class="user-avatar">
                                <%= username != null && !username.trim().isEmpty() ? username.trim().substring(0, 1).toUpperCase() : "U"%>
                            </div>
                            <div class="user-info">
                                <div class="flex items-center gap-1.5 min-w-0">
                                    <p class="user-name"><%= username != null && !username.trim().isEmpty() ? username : "Học viên"%></p>
                                    
                                    <!-- HUY HIỆU GÓI HIỂN THỊ DỰA TRÊN isPremiumUser ĐÃ FIX BÊN TRÊN -->
                                    <% if (isPremiumUser) { %>
                                    <span class="flex-shrink-0 px-1.5 py-0.5 bg-gradient-to-r from-amber-500 to-orange-500 text-white font-bold text-[9px] rounded-full shadow-sm scale-90 origin-left">PRO</span>
                                    <% } else { %>
                                    <span class="flex-shrink-0 px-2 py-0.5 bg-emerald-100 text-emerald-800 border border-emerald-300 dark:bg-emerald-900/60 dark:text-emerald-400 dark:border-emerald-700 font-bold text-[10px] rounded-full shadow-sm scale-90 origin-left tracking-wide">FREE</span>
                                    <% }%>
                                </div>
                                <!-- ĐOẠN NÀY LÀ QUYỀN (ROLE): SẼ HIỂN THỊ "STUDENT" THAY VÌ FREE/PREMIUM NHƯ CŨ -->
                                <p class="user-role">Quyền: <%= role %></p>
                            </div>
                        </a>
                    </div>

                    <a href="<%= request.getContextPath()%>/MainController?action=logout" class="logout-btn">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" x2="9" y1="12" y2="12"/></svg>
                        <span>Đăng xuất</span>
                    </a>
                </div>
            </div>
        </aside>

        <!-- MAIN CONTENT COMPONENT -->
        <main class="main-content">
            <div class="header-container">
                <div class="flex items-center space-x-3">
                    <% if (currentFolderId != null) {%>
                    <a href="<%= request.getContextPath()%>/user_dashboard.jsp" class="p-2.5 bg-white border border-gray-200 rounded-xl hover:bg-gray-50 text-gray-600 transition-colors dark:bg-gray-800 dark:border-gray-700 dark:text-gray-300">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path></svg>
                    </a>
                    <h1 class="page-title dark:text-white">Thư mục đã chọn</h1>
                    <% } else { %>
                    <h1 class="page-title dark:text-white">Tài liệu của tôi</h1>
                    <% } %>
                </div>

                <div class="flex items-center gap-4">
                    <div class="flex gap-3">
                        <% if (currentFolderId == null) { %>
                        <button onclick="document.getElementById('createFolderModal').classList.remove('hidden')" class="btn-secondary">
                            <svg class="w-4 h-4 text-gray-600 dark:text-gray-400" fill="none" stroke="currentColor" stroke-width="2.2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 13h6m-3-3v6m-9 1V7a2 2 0 012-2h6l2 2h6a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2z"></path></svg>
                            <span>Tạo thư mục</span>
                        </button>
                        <% }%>

                        <form id="uploadForm" action="<%= request.getContextPath()%>/UploadController?action=upload" method="post" enctype="multipart/form-data" class="inline-block">
                            <input type="hidden" name="folderId" value="<%= currentFolderId != null ? currentFolderId : ""%>" />
                            <input type="file" name="file" id="fileUpload" accept=".pptx,.docx,.xlsx,.pdf" class="hidden" onchange="handleFileSelect(this)" />
                            <button type="button" onclick="document.getElementById('fileUpload').click()" class="btn-primary">
                                <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2.2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                                <span>Tải lên tài liệu</span>
                            </button>
                        </form>
                    </div>
                </div>
            </div>

            <!-- Widget Thống kê Dung lượng Động -->
            <div class="mb-8 bg-white border border-gray-100 rounded-2xl p-6 shadow-sm transition-colors duration-200 dark:bg-gray-800 dark:border-gray-700">
                <div class="flex items-center justify-between mb-3">
                    <span class="text-sm font-medium text-gray-600 dark:text-gray-300">Dung lượng đã sử dụng (<%= isPremiumUser ? "PREMIUM" : "FREE" %>)</span>
                    <span class="text-sm font-bold text-black dark:text-white"><%= String.format("%.2f", totalSizeGb) %> GB / <%= (int)maxStorageGb %> GB</span>
                </div>
                <div class="w-full bg-gray-200 rounded-full h-2.5 dark:bg-gray-700">
                    <div class="bg-[#5c3cf5] h-2.5 rounded-full transition-all duration-500 ease-out" style="width: <%= storagePercent %>%"></div>
                </div>
                <p class="text-xs mt-3 font-medium text-gray-500 dark:text-gray-400">
                    <% if (!isPremiumUser) { %>
                    Nâng cấp lên Premium để có thêm 50GB dung lượng lưu trữ &amp; mở rộng giới hạn file tải lên đến 100MB.
                    <% } else { %>
                    Tài khoản Premium đang hoạt động tối ưu. Chúc bạn có những trải nghiệm học tập tuyệt vời!
                    <% } %>
                </p>
            </div>

            <div id="file-grid-list" class="file-grid"></div>

            <!-- MODAL CORES -->
            <div id="fileViewerModal" class="fixed inset-0 z-50 hidden bg-gray-900/60 backdrop-blur-sm flex justify-center items-center">
                <div class="bg-white w-11/12 max-w-5xl h-[90vh] rounded-2xl flex flex-col shadow-2xl overflow-hidden dark:bg-gray-800">
                    <div class="flex justify-between items-center px-6 py-4 border-b border-gray-100 bg-gray-50/50 dark:bg-gray-800 dark:border-gray-700">
                        <h2 id="modalFileTitle" class="text-lg font-bold text-gray-800 truncate pr-4 dark:text-white">Đang tải...</h2>
                        <div class="flex items-center space-x-3">
                            <a id="modalDownloadBtn" href="#" class="flex items-center space-x-2 px-4 py-2 bg-indigo-50 text-indigo-700 hover:bg-indigo-100 hover:text-indigo-800 rounded-xl font-semibold text-sm transition-colors">
                                <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                                <span>Tải xuống</span>
                            </a>
                            <button onclick="closeFileModal()" class="p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-xl transition-colors">
                                <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"></path></svg>
                            </button>
                        </div>
                    </div>
                    <div class="flex-1 bg-gray-100 dark:bg-gray-900">
                        <iframe id="modalIframe" src="" class="w-full h-full border-0"></iframe>
                    </div>
                </div>
            </div>

            <div id="createFolderModal" class="fixed inset-0 z-50 hidden bg-gray-900/60 backdrop-blur-sm flex justify-center items-center">
                <div class="bg-white w-full max-w-md rounded-2xl p-6 shadow-2xl dark:bg-gray-800">
                    <h2 class="text-xl font-bold text-gray-900 mb-4 dark:text-white">Tạo thư mục mới</h2>
                    <form action="<%= request.getContextPath()%>/MainController" method="POST">
                        <input type="hidden" name="action" value="createFolder" />
                        <input type="hidden" name="currentFolderId" value="<%= currentFolderId != null ? currentFolderId : ""%>" />
                        <input type="text" name="folderName" required class="w-full px-4 py-3 rounded-xl border border-gray-200 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 mb-6 outline-none transition-all bg-gray-50 focus:bg-white text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-white" placeholder="Nhập tên thư mục..." />
                        <div class="flex justify-end space-x-3">
                            <button type="button" onclick="document.getElementById('createFolderModal').classList.add('hidden')" class="btn-secondary px-5 py-2">Hủy</button>
                            <button type="submit" class="btn-primary px-5 py-2">Tạo mới</button>
                        </div>
                    </form>
                </div>
            </div>
        </main>

        <!-- POPUP CHÀO MỪNG THÀNH VIÊN MỚI -->
        <div id="welcomeModal" class="fixed inset-0 z-[100] hidden bg-gray-950/70 backdrop-blur-md flex justify-center items-center p-4">
            <div class="bg-white rounded-3xl max-w-lg w-full shadow-2xl p-8 border border-gray-100 text-center transform scale-95 transition-all duration-300">
                <div class="w-20 h-20 bg-gradient-to-tr from-purple-500 to-indigo-600 rounded-2xl flex items-center justify-center text-white mx-auto mb-6 shadow-lg shadow-indigo-500/30">
                    <svg xmlns="http://www.w3.org/2000/svg" class="w-10 h-10" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"/></svg>
                </div>

                <h2 class="text-2xl font-extrabold text-gray-900 mb-2 tracking-tight">
                    Chào mừng bạn, <span class="text-indigo-600"><%= username != null && !username.trim().isEmpty() ? username : "Học viên"%></span>! 👋
                </h2>
                <p class="text-sm text-gray-500 mb-6 font-medium">Khám phá không gian học tập công nghệ mới của riêng bạn tại AI Study Hub</p>

                <div class="space-y-4 text-left mb-6">
                    <div class="flex items-start space-x-3.5 p-3.5 bg-gray-50 rounded-2xl">
                        <div class="p-2 bg-blue-100 rounded-xl text-blue-600 flex-shrink-0">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M5 19a2 2 0 012-2h10a2 2 0 012 2v2H5v-2zM7 11V3a1 1 0 011-1h8a1 1 0 011 1v8M12 11V6"/></svg>
                        </div>
                        <div>
                            <h4 class="font-bold text-sm text-gray-800">Kho lưu trữ thông minh</h4>
                            <p class="text-xs text-gray-500 mt-0.5 leading-relaxed">Quản lý và lưu trữ dữ liệu tài liệu cá nhân đa định dạng hiệu quả, an toàn tuyệt đối.</p>
                        </div>
                    </div>

                    <div class="flex items-start space-x-3.5 p-3.5 bg-gray-50 rounded-2xl">
                        <div class="p-2 bg-purple-100 rounded-xl text-purple-600 flex-shrink-0">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"/></svg>
                        </div>
                        <div>
                            <h4 class="font-bold text-sm text-gray-800">Tương tác AI chuyên sâu</h4>
                            <p class="text-xs text-gray-500 mt-0.5 leading-relaxed">Trò chuyện trực tiếp với AI Chatbot để tóm tắt và bóc tách kiến thức dựa trên các tài liệu sẵn có.</p>
                        </div>
                    </div>

                    <div class="flex items-start space-x-3.5 p-3.5 bg-gray-50 rounded-2xl">
                        <div class="p-2 bg-emerald-100 rounded-xl text-emerald-600 flex-shrink-0">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/><path stroke-linecap="round" stroke-linejoin="round" d="M12 7v5l3 3"/></svg>
                        </div>
                        <div>
                            <h4 class="font-bold text-sm text-gray-800">Cộng đồng học tập lớn</h4>
                            <p class="text-xs text-gray-500 mt-0.5 leading-relaxed">Thỏa sức khám phá và chia sẻ nguồn tài liệu học thuật quý giá đóng góp từ cộng đồng sinh viên.</p>
                        </div>
                    </div>
                </div>

                <div class="flex items-center justify-start px-2 mb-5">
                    <label class="flex items-center space-x-2.5 cursor-pointer select-none">
                        <input type="checkbox" id="dontShowAgainCheckbox" class="w-4 h-4 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500 bg-gray-50">
                        <span class="text-xs text-gray-500 font-medium">Không hiển thị lại thông báo này lần sau</span>
                    </label>
                </div>

                <button onclick="closeWelcomeModal()" class="w-full py-3.5 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 text-white font-bold rounded-2xl text-sm transition-all shadow-md shadow-indigo-600/20">
                    Bắt đầu khám phá ngay
                </button>
            </div>
        </div>

        <script>
            const MAX_FILE_SIZE_BYTES = <%= maxUploadSizeBytes%>;
            const USER_ROLE_STR = "<%= isPremiumUser ? "Premium" : "Free" %>";
            const CURRENT_USER_ID = "<%= userId%>;";
            const ALLOWED_EXTENSIONS = ['pptx', 'docx', 'xlsx', 'pdf'];

            const dbItems = [
            <%
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

                if (myFolders != null) {
                    for (int i = 0; i < myFolders.size(); i++) {
                        Folder f = myFolders.get(i);
                        String safeName = f.getFolderName().replace("\\", "\\\\").replace("\"", "\\\"");
                        String safeDate = f.getCreatedAt() != null ? f.getCreatedAt().format(formatter) : "N/A";
                        int fileCount = docDao.getDocumentsByFolder(userId, f.getFolderId()).size();
            %>
            { id: "<%= f.getFolderId()%>", name: "<%= safeName%>", type: "folder", size: "--", uploadDate: "<%= safeDate%>", isFolder: true, fileCount: <%= fileCount%> },
            <%      }
                }

                if (myDocuments != null) {
                    for (int i = 0; i < myDocuments.size(); i++) {
                        Document doc = myDocuments.get(i);
                        String ext = "file";
                        String url = doc.getCloudStorageUrl();
                        if (url != null && url.lastIndexOf('.') > 0) {
                            ext = url.substring(url.lastIndexOf('.') + 1).toLowerCase();
                        }

                        String rawTitle = doc.getTitle() != null ? doc.getTitle() : "Tài liệu";
                        String safeTitle = rawTitle.replace("\\", "\\\\").replace("\"", "\\\"");
                        String safeDate = doc.getCreatedAt() != null ? doc.getCreatedAt().format(formatter) : "N/A";
            %>
            { id: "<%= doc.getDocumentId()%>", name: "<%= safeTitle%>", type: "<%= ext%>", size: "<%= doc.getFileSizeMb()%> MB", uploadDate: "<%= safeDate%>", isFolder: false }<%= (i < myDocuments.size() - 1) ? "," : ""%>
            <%      }
                }
            %>
            ];

            function showToast(message, type = 'success') {
                const container = document.getElementById('toastContainer');
                if (!container) return;

                const toast = document.createElement('div');
                toast.className = `flex items-center space-x-3 px-5 py-3.5 bg-white dark:bg-gray-800 text-gray-800 dark:text-white rounded-2xl shadow-xl border border-gray-100 dark:border-gray-700 pointer-events-auto transition-all duration-300 translate-x-20 opacity-0 min-w-[280px] max-w-md`;

                const icon = type === 'success'
                        ? `<div class="p-1.5 bg-emerald-100 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400 rounded-xl"><svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"></path></svg></div>`
                        : `<div class="p-1.5 bg-blue-100 dark:bg-blue-950/50 text-blue-600 dark:text-blue-400 rounded-xl"><svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg></div>`;

                toast.innerHTML = icon + `<span class="text-sm font-semibold tracking-tight">\${message}</span>`;
                container.appendChild(toast);

                setTimeout(() => {
                    toast.classList.remove('translate-x-20', 'opacity-0');
                }, 50);
                setTimeout(() => {
                    toast.classList.add('opacity-0', 'translate-x-10');
                    setTimeout(() => { toast.remove(); }, 300);
                }, 4000);
            }

            function checkWelcomeModal() {
                sessionStorage.removeItem('hasSeenWelcomeThisSession');
                if (localStorage.getItem('blockWelcomeModalForever_User_' + CURRENT_USER_ID) === 'true') {
                    return;
                }
                document.getElementById('welcomeModal').classList.remove('hidden');
            }

            function handleFileSelect(input) {
                const file = input.files[0];
                if (!file) return;

                const fileExtension = file.name.split('.').pop().toLowerCase();
                if (!ALLOWED_EXTENSIONS.includes(fileExtension)) {
                    alert("Hệ thống không hỗ trợ định dạng này! Chỉ chấp nhận file: .pptx, .docx, .xlsx, .pdf");
                    input.value = '';
                    return;
                }

                if (file.size > MAX_FILE_SIZE_BYTES) {
                    const limitMb = <%= isPremiumUser ? "100" : "50" %>;
                    alert(`Tài khoản của bạn (\${USER_ROLE_STR}) bị giới hạn kích thước dung lượng tải lên tối đa là \${limitMb}MB cho mỗi tài liệu.\n\nTập tin hiện tại của bạn nặng: \${(file.size / (1024 * 1024)).toFixed(2)} MB.`);
                    input.value = '';
                    return;
                }

                document.getElementById('uploadForm').submit();
            }

            function closeWelcomeModal() {
                document.getElementById('welcomeModal').classList.add('hidden');
                const checkbox = document.getElementById('dontShowAgainCheckbox');
                if (checkbox && checkbox.checked) {
                    localStorage.setItem('blockWelcomeModalForever_User_' + CURRENT_USER_ID, 'true');
                } else {
                    localStorage.removeItem('blockWelcomeModalForever_User_' + CURRENT_USER_ID);
                }
            }

            function initToastNotifications() {
                const urlParams = new URLSearchParams(window.location.search);

                if (urlParams.has('uploadSuccess')) showToast("🎉 Đã upload file thành công!", "success");
                if (urlParams.has('createFolderSuccess')) showToast("📁 Đã tạo folder thành công!", "success");
                if (urlParams.has('deleteSuccess')) showToast("🗑️ Đã xóa folder hoặc file thành công!", "success");
                if (urlParams.has('depositSuccess')) showToast("🔑 Nạp Coin thành công! Số dư đã cập nhật.", "success");
                if (urlParams.has('upgradeSuccess')) showToast("👑 Nâng cấp tài khoản Premium thành công!", "success");

                if (<%= storagePercent%> >= 85.0) {
                    showToast("⚠️ Cảnh báo: Dung lượng lưu trữ sắp đầy!", "info");
                }

                if (urlParams.toString() !== "") {
                    const cleanUrl = window.location.protocol + "//" + window.location.host + window.location.pathname;
                    window.history.replaceState({}, document.title, cleanUrl);
                }
            }

            function getFileStyle(item) {
                if (item.isFolder)
                    return {bg: "bg-[#eff6ff]", icon: `<svg class="w-7 h-7 text-[#3b82f6]" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path></svg>`};
                return {bg: "bg-[#f0f9ff]", icon: `<svg class="w-7 h-7 text-[#0284c7]" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>`};
            }

            function openFileModal(docId, docName) {
                document.getElementById('modalFileTitle').innerText = docName;
                document.getElementById('modalIframe').src = "<%= request.getContextPath()%>/MainController?action=viewDoc&docId=" + docId;
                document.getElementById('modalDownloadBtn').href = "<%= request.getContextPath()%>/MainController?action=downloadDoc&docId=" + docId;
                document.getElementById('fileViewerModal').classList.remove('hidden');
                document.body.style.overflow = "hidden";
            }

            function closeFileModal() {
                document.getElementById('fileViewerModal').classList.add('hidden');
                document.getElementById('modalIframe').src = "";
                document.body.style.overflow = "";
            }

            function renderFileGrid() {
                const gridContainer = document.getElementById('file-grid-list');
                if (!gridContainer) return;
                
                if (dbItems.length === 0) {
                    gridContainer.innerHTML = `<div class="empty-state-box col-span-full flex flex-col items-center justify-center py-16 bg-white border border-gray-100 rounded-2xl shadow-sm transition-colors duration-200">
                    <div class="empty-state-icon w-16 h-16 bg-gray-50 rounded-full flex items-center justify-center text-gray-400 mb-4 transition-colors duration-200">
                        <svg class="w-8 h-8" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 13h6m-3-3v6m5 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
                    </div>
                    <p class="empty-state-text text-gray-500 font-medium text-sm transition-colors duration-200">Chưa có dữ liệu. Hãy tải lên tài liệu mới!</p>
                </div>`;
                    return;
                }

                gridContainer.innerHTML = dbItems.map(item => {
                    const style = getFileStyle(item);

                    if (item.isFolder) {
                        return `
                    <div onclick="window.location.href='<%= request.getContextPath()%>/user_dashboard.jsp?folderId=\${item.id}'" class="file-card group relative">
                        <div class="file-icon-wrapper">
                            <div class="file-icon-box \${style.bg} transition-transform group-hover:scale-105">\${style.icon}</div>
                            <div class="opacity-0 group-hover:opacity-100 transition-opacity">
                                <a href="<%= request.getContextPath()%>/MainController?action=deleteFolder&folderId=\${item.id}" onclick="event.stopPropagation(); return confirm('CẢNH BÁO: Xóa thư mục sẽ làm mất liên kết các tài liệu bên trong. Bạn có chắc chắn tiếp tục?');" class="p-1.5 text-red-500 hover:bg-red-50 rounded-lg block">
                                    <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                                </a>
                            </div>
                        </div>
                        <h3 class="file-title" title="\${item.name}">\${item.name}</h3>
                        <p class="text-xs font-semibold text-indigo-500 mb-2">\${item.fileCount} tài liệu</p>
                        <p class="file-date">\${item.uploadDate}</p>
                    </div>`;
                    }

                    return `
                <div onclick="openFileModal('\${item.id}', '\${item.name}')" class="file-card group relative">
                    <div class="file-icon-wrapper">
                        <div class="file-icon-box \${style.bg} transition-transform group-hover:scale-105">\${style.icon}</div>
                        <div class="flex space-x-1 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                            <a href="<%= request.getContextPath()%>/MainController?action=editDoc&docId=\${item.id}" onclick="event.stopPropagation();" class="p-1.5 text-gray-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors" title="Chỉnh sửa">
                                <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path></svg>
                            </a>
                            <a href="<%= request.getContextPath()%>/MainController?action=deleteDoc&docId=\${item.id}" onclick="event.stopPropagation(); return confirm('Bạn có chắc chắn muốn xóa tài liệu này?');" class="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors" title="Xóa">
                                <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                            </a>
                        </div>
                    </div>
                    <h3 class="file-title" title="\${item.name}">\${item.name}</h3>
                    <p class="file-size">\${item.size}</p>
                    <p class="file-date">\${item.uploadDate}</p>
                </div>`;
                }).join('');
            }

            document.addEventListener("DOMContentLoaded", function () {
                renderFileGrid();
                checkWelcomeModal();
                initToastNotifications();
            });
        </script>
    </body>
</html>