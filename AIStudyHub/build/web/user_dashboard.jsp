<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="Model.Document" %>
<%@ page import="Model.DocumentDAO" %>
<%@ page import="Model.Folder" %>
<%@ page import="Model.FolderDAO" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%
    // 1. Ensure user is logged in
    HttpSession userSession = request.getSession(false);
    if (userSession == null || userSession.getAttribute("userId") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    int userId = (Integer) userSession.getAttribute("userId");
    String username = (String) userSession.getAttribute("username");
    String role = (String) userSession.getAttribute("role");

    // 2. Check if we are inside a specific folder
    String folderIdParam = request.getParameter("folderId");
    Integer currentFolderId = null;
    if (folderIdParam != null && !folderIdParam.trim().isEmpty() && !folderIdParam.equals("null")) {
        try { currentFolderId = Integer.parseInt(folderIdParam); } catch (Exception e) {}
    }

    // 3. Fetch Folders (Only show folders if we are in the root directory)
    FolderDAO folderDao = new FolderDAO();
    List<Folder> myFolders = new ArrayList<>();
    if (currentFolderId == null) {
        myFolders = folderDao.getFoldersByUserId(userId);
    }

    // 4. Fetch Documents (For the current folder)
    DocumentDAO docDao = new DocumentDAO();
    List<Document> myDocuments = docDao.getDocumentsByFolder(userId, currentFolderId);

    // 5. Calculate global storage
    List<Document> allDocs = docDao.getDocumentsByUserId(userId);
    double totalSizeMb = 0.0;
    if (allDocs != null) {
        for (Document doc : allDocs) { totalSizeMb += doc.getFileSizeMb(); }
    }
    double totalSizeGb = totalSizeMb / 1024.0;
    double maxStorageGb = 5.0;
    double storagePercent = (totalSizeGb / maxStorageGb) * 100.0;
    if (storagePercent > 100) storagePercent = 100;
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tài liệu của tôi - AI Study Hub</title>
    
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    
    <style type="text/tailwindcss">
        @layer components {
            /* Inherited General Layout */
            .page-body { @apply flex min-h-screen w-full text-gray-800 bg-[#f8f9fa] font-sans; }
            .sidebar { @apply w-64 bg-white border-r border-gray-100 flex flex-col justify-between p-4 flex-shrink-0 min-h-screen shadow-sm z-10; }
            .brand-container { @apply flex items-center space-x-3 px-2 py-1; }
            .brand-logo { @apply w-9 h-9 bg-indigo-600 rounded-xl flex items-center justify-center text-white shadow-sm shadow-indigo-600/20; }
            .brand-text { @apply font-bold text-gray-900 text-base tracking-tight; }
            .nav-link { @apply flex items-center space-x-3 px-4 py-2.5 text-gray-600 hover:bg-gray-50 rounded-xl font-medium text-sm transition-colors; }
            .nav-link-active { @apply flex items-center space-x-3 px-4 py-2.5 bg-indigo-50 text-indigo-600 rounded-xl font-semibold text-sm transition-colors; }
            
            .wallet-widget { @apply w-full bg-gradient-to-br from-purple-500 to-indigo-600 text-white p-4 rounded-2xl shadow-md shadow-indigo-600/10 relative overflow-hidden; }
            .wallet-header { @apply flex justify-between items-center opacity-85; }
            .wallet-title { @apply text-xs font-medium tracking-wide; }
            .wallet-balance { @apply text-xl font-bold mt-2 tracking-tight; }
            
            .user-area { @apply pt-2 border-t border-gray-100; }
            .user-profile-link { @apply flex items-center space-x-3 px-2 py-2 rounded-xl hover:bg-gray-50 transition-colors cursor-pointer block; }
            .user-avatar { @apply w-8 h-8 rounded-full bg-indigo-100 flex items-center justify-center text-indigo-700 flex-shrink-0 font-bold text-xs uppercase; }
            .user-info { @apply flex-1 min-w-0; }
            .user-name { @apply text-sm font-bold text-gray-900 truncate; }
            .user-role { @apply text-[11px] text-gray-400 font-medium; }
            .logout-btn { @apply w-full flex items-center space-x-2.5 px-2 py-2 mt-1 rounded-xl text-sm font-medium text-gray-500 hover:text-red-500 hover:bg-red-50 transition-colors text-left; }
            
            /* Main Content Area */
            .main-content { @apply flex-1 p-8 overflow-y-auto h-screen relative; }
            .header-container { @apply flex justify-between items-center mb-6; }
            .page-title { @apply text-2xl font-bold text-gray-900 tracking-tight; }
            
            /* Forms & Buttons */
            .btn-primary { @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-[#5c3cf5] text-white rounded-xl font-semibold hover:bg-indigo-700 transition-colors text-sm shadow-sm shadow-indigo-100 cursor-pointer; }
            .btn-secondary { @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-white border border-gray-200 text-gray-700 rounded-xl font-semibold hover:bg-gray-50 transition-colors text-sm shadow-sm cursor-pointer; }
            
            /* Storage Widget */
            .storage-widget { @apply mb-8 bg-white border border-gray-100 rounded-2xl p-6 shadow-sm; }
            .storage-header { @apply flex items-center justify-between mb-3; }
            .storage-label { @apply text-sm text-gray-600 font-medium; }
            .storage-value { @apply text-sm font-bold text-gray-900; }
            .storage-bar-bg { @apply w-full bg-gray-100 rounded-full h-2.5; }
            .storage-bar-fill { @apply bg-[#5c3cf5] h-2.5 rounded-full transition-all duration-500 ease-out; }
            .storage-footer { @apply text-xs text-gray-400 mt-3 font-medium; }
            
            /* File Grid */
            .file-grid { @apply grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5; }
            .file-card { @apply bg-white border border-gray-100 rounded-2xl p-5 hover:shadow-md transition-all cursor-pointer; }
            .file-icon-wrapper { @apply mb-5 flex justify-between items-start; }
            .file-icon-box { @apply p-4 rounded-[16px] flex items-center justify-center; }
            .file-title { @apply font-semibold text-gray-800 text-[15px] mb-1 truncate; }
            .file-size { @apply text-xs text-gray-400 font-medium mb-3; }
            .file-date { @apply text-[11px] text-gray-400 font-medium; }
        }
    </style>
</head>
<body class="page-body">

    <aside class="sidebar">
        <div class="space-y-6 w-full">
            <div class="brand-container">
                <div class="brand-logo">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21.42 10.922a1 1 0 0 0-.019-1.838L12.83 5.18a2 2 0 0 0-1.66 0L2.6 9.08a1 1 0 0 0 0 1.832l8.57 3.908a2 2 0 0 0 1.66 0z"/><path d="M6 18.8V13.5"/><path d="M18 13.5v5.3a2.1 2.1 0 0 1-2 2h-8a2.1 2.1 0 0 1-2-2v-5.3"/></svg>
                </div>
                <span class="brand-text">AI Study Hub</span>
            </div>
            <nav class="space-y-1 w-full">
                <a href="<%= request.getContextPath() %>/user_dashboard.jsp" class="nav-link-active">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z"/></svg>
                    <span>Tài liệu của tôi</span>
                </a>
            </nav>
        </div>

        <div class="w-full space-y-4">
            <div class="wallet-widget">
                <div class="wallet-header">
                    <span class="wallet-title">Số dư ví</span>
                </div>
                <div class="wallet-balance">150,000đ</div>
            </div>

            <div class="user-area">
                <a href="<%= request.getContextPath() %>/MainController?action=profile" class="user-profile-link">
                    <div class="user-avatar">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                    </div>
                    <div class="user-info">
                        <p class="user-name"><%= username != null ? username : "Khách" %></p>
                        <p class="user-role">Vai trò: <%= role != null ? role : "N/A" %></p>
                    </div>
                </a>
                <a href="<%= request.getContextPath() %>/MainController?action=logout" class="logout-btn">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" x2="9" y1="12" y2="12"/></svg>
                    <span>Đăng xuất</span>
                </a>
            </div>
        </div>
    </aside>

    <main class="main-content">
        <div class="header-container">
            <div class="flex items-center space-x-3">
                <% if (currentFolderId != null) { %>
                    <a href="<%= request.getContextPath() %>/user_dashboard.jsp" class="p-2.5 bg-white border border-gray-200 rounded-xl hover:bg-gray-50 text-gray-600 transition-colors">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path></svg>
                    </a>
                    <h1 class="page-title">Thư mục đã chọn</h1>
                <% } else { %>
                    <h1 class="page-title">Tài liệu của tôi</h1>
                <% } %>
            </div>
            
            <div class="flex gap-3">
                <% if (currentFolderId == null) { %>
                    <button onclick="document.getElementById('createFolderModal').classList.remove('hidden')" class="btn-secondary">
                        <svg class="w-4 h-4 text-gray-600" fill="none" stroke="currentColor" stroke-width="2.2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 13h6m-3-3v6m-9 1V7a2 2 0 012-2h6l2 2h6a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2z"></path></svg>
                        <span>Tạo thư mục</span>
                    </button>
                <% } %>

                <form action="<%= request.getContextPath()%>/UploadController?action=upload" method="post" enctype="multipart/form-data" class="inline-block">
                    <input type="hidden" name="folderId" value="<%= currentFolderId != null ? currentFolderId : "" %>" />
                    <input type="file" name="file" id="fileUpload" class="hidden" onchange="this.form.submit()" />
                    <button type="button" onclick="document.getElementById('fileUpload').click()" class="btn-primary">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2.2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                        <span>Tải lên tài liệu</span>
                    </button>
                </form>
            </div>
        </div>

        <div class="storage-widget">
            <div class="storage-header">
                <span class="storage-label">Dung lượng đã sử dụng</span>
                <span class="storage-value"><%= String.format(java.util.Locale.US, "%.2f", totalSizeGb)%> GB / <%= (int) maxStorageGb%> GB</span>
            </div>
            <div class="storage-bar-bg">
                <div class="storage-bar-fill" style="width: <%= String.format(java.util.Locale.US, "%.1f", storagePercent)%>%"></div>
            </div>
            <p class="storage-footer">Nâng cấp lên Premium để có thêm 50GB dung lượng</p>
        </div>

        <div id="file-grid-list" class="file-grid"></div>

        <div id="fileViewerModal" class="fixed inset-0 z-50 hidden bg-gray-900/60 backdrop-blur-sm flex justify-center items-center">
            <div class="bg-white w-11/12 max-w-5xl h-[90vh] rounded-2xl flex flex-col shadow-2xl overflow-hidden animate-[fadeIn_0.2s_ease-out]">
                <div class="flex justify-between items-center px-6 py-4 border-b border-gray-100 bg-gray-50/50">
                    <h2 id="modalFileTitle" class="text-lg font-bold text-gray-800 truncate pr-4">Đang tải...</h2>
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
                <div class="flex-1 bg-gray-100">
                    <iframe id="modalIframe" src="" class="w-full h-full border-0"></iframe>
                </div>
            </div>
        </div>

        <div id="createFolderModal" class="fixed inset-0 z-50 hidden bg-gray-900/60 backdrop-blur-sm flex justify-center items-center">
            <div class="bg-white w-full max-w-md rounded-2xl p-6 shadow-2xl">
                <h2 class="text-xl font-bold text-gray-900 mb-4">Tạo thư mục mới</h2>
                <form action="<%= request.getContextPath() %>/MainController" method="POST">
                    <input type="hidden" name="action" value="createFolder" />
                    <input type="hidden" name="currentFolderId" value="<%= currentFolderId != null ? currentFolderId : "" %>" />
                    <input type="text" name="folderName" required class="w-full px-4 py-3 rounded-xl border border-gray-200 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 mb-6 outline-none transition-all bg-gray-50 focus:bg-white text-sm" placeholder="Nhập tên thư mục..." />
                    <div class="flex justify-end space-x-3">
                        <button type="button" onclick="document.getElementById('createFolderModal').classList.add('hidden')" class="btn-secondary px-5 py-2">Hủy</button>
                        <button type="submit" class="btn-primary px-5 py-2">Tạo mới</button>
                    </div>
                </form>
            </div>
        </div>

    </main>

    <script>
        // Catch System Alerts
        const urlParams = new URLSearchParams(window.location.search);
        if (urlParams.has('uploadSuccess')) alert("Tài liệu đã được cập nhật thành công!");
        if (urlParams.has('updateSuccess')) alert("Cập nhật thành công!");
        if (urlParams.has('deleteSuccess')) alert("Xóa tài liệu thành công!");
        if (urlParams.has('folderSuccess')) alert("Thao tác thư mục thành công!");

        const dbItems = [
        <%
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            
            if (myFolders != null) {
                for (int i = 0; i < myFolders.size(); i++) {
                    Folder f = myFolders.get(i);
                    String safeName = f.getFolderName().replace("\\", "\\\\").replace("\"", "\\\"");
                    String safeDate = f.getCreatedAt() != null ? f.getCreatedAt().format(formatter) : "N/A";
        %>
            { id: "<%= f.getFolderId() %>", name: "<%= safeName %>", type: "folder", size: "--", uploadDate: "<%= safeDate %>", isFolder: true },
        <%      }
            }
            
            if (myDocuments != null) {
                for (int i = 0; i < myDocuments.size(); i++) {
                    Document doc = myDocuments.get(i);
                    String ext = "file";
                    String url = doc.getCloudStorageUrl();
                    if (url != null && url.lastIndexOf('.') > 0) ext = url.substring(url.lastIndexOf('.') + 1).toLowerCase();
                    
                    String rawTitle = doc.getTitle() != null ? doc.getTitle() : "Tài liệu";
                    String safeTitle = rawTitle.replace("\\", "\\\\").replace("\"", "\\\"");
                    String safeDate = doc.getCreatedAt() != null ? doc.getCreatedAt().format(formatter) : "N/A";
        %>
            { id: "<%= doc.getDocumentId() %>", name: "<%= safeTitle %>", type: "<%= ext %>", size: "<%= doc.getFileSizeMb() %> MB", uploadDate: "<%= safeDate %>", isFolder: false }<%= (i < myDocuments.size() - 1) ? "," : "" %>
        <%      }
            }
        %>
        ];

        function getFileStyle(doc) {
            if (doc.isFolder) return { bg: "bg-[#eff6ff]", icon: `<svg class="w-7 h-7 text-[#3b82f6]" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path></svg>` };
            return { bg: "bg-[#f0f9ff]", icon: `<svg class="w-7 h-7 text-[#0284c7]" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>` };
        }

        function openFileModal(docId, docName) {
            document.getElementById('modalFileTitle').innerText = docName;
            document.getElementById('modalIframe').src = "<%= request.getContextPath() %>/MainController?action=viewDoc&docId=" + docId;
            document.getElementById('modalDownloadBtn').href = "<%= request.getContextPath() %>/MainController?action=downloadDoc&docId=" + docId;
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
            if (dbItems.length === 0) {
                gridContainer.innerHTML = `<div class="col-span-full flex flex-col items-center justify-center py-16 bg-white border border-gray-100 rounded-2xl shadow-sm">
                    <div class="w-16 h-16 bg-gray-50 rounded-full flex items-center justify-center text-gray-400 mb-4">
                        <svg class="w-8 h-8" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 13h6m-3-3v6m5 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
                    </div>
                    <p class="text-gray-500 font-medium text-sm">Chưa có dữ liệu. Hãy tải lên tài liệu mới!</p>
                </div>`;
                return;
            }

            gridContainer.innerHTML = dbItems.map(item => {
                const style = getFileStyle(item);
                
                if (item.isFolder) {
                    return `
                    <div onclick="window.location.href='<%= request.getContextPath() %>/user_dashboard.jsp?folderId=\${item.id}'" class="file-card group relative">
                        <div class="file-icon-wrapper">
                            <div class="file-icon-box \${style.bg} transition-transform group-hover:scale-105">\${style.icon}</div>
                            <div class="opacity-0 group-hover:opacity-100 transition-opacity">
                                <a href="<%= request.getContextPath() %>/MainController?action=deleteFolder&folderId=\${item.id}" onclick="event.stopPropagation(); return confirm('CẢNH BÁO: Xóa thư mục sẽ làm mất liên kết các tài liệu bên trong. Bạn có chắc chắn tiếp tục?');" class="p-1.5 text-red-500 hover:bg-red-50 rounded-lg block">
                                    <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                                </a>
                            </div>
                        </div>
                        <h3 class="file-title">\${item.name}</h3>
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
                    <h3 class="file-title">\${item.name}</h3>
                    <p class="file-size">\${item.size}</p>
                    <p class="file-date">\${item.uploadDate}</p>
                </div>`;
            }).join('');
        }

        document.addEventListener("DOMContentLoaded", renderFileGrid);
    </script>
</body>
</html>