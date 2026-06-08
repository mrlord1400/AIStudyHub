<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="Model.Document" %>
<%@ page import="Model.Folder" %>
<%@ page import="Model.FolderDAO" %>
<%@ page import="java.util.List" %>
<%
    // 1. Check Login
    HttpSession userSession = request.getSession(false);
    if (userSession == null || userSession.getAttribute("userId") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    // 2. Fetch User Info for Sidebar & Queries
    Integer userId = (Integer) userSession.getAttribute("userId");
    String username = (String) userSession.getAttribute("username");
    String role = (String) userSession.getAttribute("role");

    // 3. Fetch the Document to Edit
    Document doc = (Document) request.getAttribute("document");
    if (doc == null) {
        // If someone tries to access this page directly without a document, kick them back
        response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp");
        return;
    }

    // 4. Fetch User's Folders for the dropdown
    FolderDAO folderDao = new FolderDAO();
    List<Folder> myFolders = folderDao.getFoldersByUserId(userId);

    String errorMessage = (String) request.getAttribute("errorMessage");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chỉnh sửa tài liệu - AI Study Hub</title>
    
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    
    <style type="text/tailwindcss">
        @layer components {
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
            .user-avatar { @apply w-8 h-8 rounded-full bg-indigo-50 flex items-center justify-center text-indigo-600 flex-shrink-0 font-bold text-xs uppercase; }
            .user-info { @apply flex-1 min-w-0; }
            .user-name { @apply text-sm font-bold text-gray-900 truncate; }
            .user-role { @apply text-[11px] text-gray-400 font-medium; }
            
            .main-content { @apply flex-1 p-8 overflow-y-auto h-screen; }
            .header-container { @apply flex justify-between items-center mb-8; }
            .page-title { @apply text-2xl font-bold text-gray-900 tracking-tight; }
            
            .form-card { @apply bg-white border border-gray-100 rounded-2xl p-7 shadow-sm max-w-3xl; }
            .form-label { @apply block text-sm font-semibold text-gray-700 mb-2; }
            .form-input { @apply w-full px-4 py-3 rounded-xl border border-gray-200 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all bg-gray-50 focus:bg-white text-sm; }
            .btn-primary { @apply flex items-center justify-center space-x-2 px-6 py-3 bg-[#5c3cf5] text-white rounded-xl font-semibold hover:bg-indigo-700 transition-colors text-sm shadow-sm shadow-indigo-100; }
            .btn-secondary { @apply flex items-center justify-center space-x-2 px-6 py-3 bg-white border border-gray-200 text-gray-700 rounded-xl font-semibold hover:bg-gray-50 transition-colors text-sm; }
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
                <a href="#" class="user-profile-link">
                    <div class="user-avatar">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                    </div>
                    <div class="user-info">
                        <p class="user-name"><%= username != null ? username : "Khách" %></p>
                        <p class="user-role">Vai trò: <%= role != null ? role : "N/A" %></p>
                    </div>
                </a>
            </div>
        </div>
    </aside>

    <main class="main-content">
        <div class="header-container max-w-3xl">
            <h1 class="page-title">Cập nhật tài liệu</h1>
            <a href="<%= request.getContextPath() %>/user_dashboard.jsp" class="text-sm font-medium text-indigo-600 hover:text-indigo-800">Quay lại Dashboard</a>
        </div>

        <% if (errorMessage != null) { %>
            <div class="max-w-3xl mb-6 p-4 bg-red-50 border border-red-200 text-red-600 rounded-xl text-sm font-medium">
                <%= errorMessage %>
            </div>
        <% } %>

        <div class="form-card">
            <div class="mb-6 pb-6 border-b border-gray-100 flex items-start space-x-4">
                <div class="p-3 bg-indigo-50 rounded-xl text-indigo-600">
                    <svg class="w-8 h-8" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path></svg>
                </div>
                <div>
                    <h2 class="text-lg font-bold text-gray-900">Thay đổi thông tin</h2>
                    <p class="text-sm text-gray-500 mt-1">Cập nhật tên, thư mục, và quyền chia sẻ cho tài liệu này.</p>
                </div>
            </div>

            <form action="<%= request.getContextPath() %>/MainController" method="POST">
                
                <input type="hidden" name="action" value="updateDoc" />
                <input type="hidden" name="docId" value="<%= doc.getDocumentId() %>" />
                
                <div class="space-y-5">
                    <div>
                        <label class="form-label">Tên tài liệu <span class="text-red-500">*</span></label>
                        <input type="text" name="title" value="<%= doc.getTitle() != null ? doc.getTitle().replace("\"", "&quot;") : "" %>" required class="form-input" placeholder="Nhập tên tài liệu..." />
                    </div>

                    <div>
                        <label class="form-label">Thư mục lưu trữ</label>
                        <select name="folderId" class="form-input">
                            <option value="" <%= doc.getFolderId() == null ? "selected" : "" %>>-- Lưu bên ngoài (Không chọn thư mục) --</option>
                            <% if (myFolders != null) {
                                   for (Folder f : myFolders) { 
                                       boolean isSelected = (doc.getFolderId() != null && doc.getFolderId() == f.getFolderId());
                            %>
                                <option value="<%= f.getFolderId() %>" <%= isSelected ? "selected" : "" %>>
                                    <%= f.getFolderName().replace("<", "&lt;").replace(">", "&gt;") %>
                                </option>
                            <%     }
                               } %>
                        </select>
                    </div>

                    <div>
                        <label class="form-label">Quyền chia sẻ</label>
                        <% String perm = doc.getSharingPermission() != null ? doc.getSharingPermission().toUpperCase() : "PRIVATE"; %>
                        <select name="sharingPermission" class="form-input">
                            <option value="PRIVATE" <%= "PRIVATE".equals(perm) ? "selected" : "" %>>Riêng tư (Chỉ mình tôi)</option>
                            <option value="FRIENDS_ONLY" <%= "FRIENDS_ONLY".equals(perm) ? "selected" : "" %>>Chỉ bạn bè</option>
                            <option value="PUBLIC" <%= "PUBLIC".equals(perm) ? "selected" : "" %>>Công khai (Mọi người có thể xem)</option>
                        </select>
                    </div>
                </div>

                <div class="mt-8 pt-6 border-t border-gray-100 flex items-center justify-end space-x-3">
                    <a href="<%= request.getContextPath() %>/user_dashboard.jsp" class="btn-secondary">
                        Hủy
                    </a>
                    <button type="submit" class="btn-primary">
                        Lưu thay đổi
                    </button>
                </div>
            </form>
        </div>
    </main>
</body>
</html>