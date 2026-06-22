<%@page import="Model.DTO.User"%>
<%@page import="Model.DAO.UserDAO"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="Model.DTO.Document" %>
<%@ page import="Model.Folder" %>
<%@ page import="Model.DAO.FolderDAO" %>
<%@ page import="java.util.List" %>
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

    // 2. Fetch User Info for Sidebar & Queries
    Integer userId = (Integer) userSession.getAttribute("userId");
    String username = (String) userSession.getAttribute("username");
    String role = (String) userSession.getAttribute("role");
    Integer tierId = (Integer) userSession.getAttribute("tierId");

    // Ép kiểu Quyền (Đồng bộ logic từ dashboard)
    if (role == null || !"ADMIN".equalsIgnoreCase(role.trim())) {
        role = "STUDENT";
    } else {
        role = "ADMIN";
    }

    // Quản lý gói
    if (tierId == null || tierId < 2) {
        tierId = 2;
    }
    boolean isPremiumUser = (tierId >= 3);

    // Khởi tạo số dư ví Coin
    UserDAO dao = new UserDAO();
    int userBalance = 0;
    if (userId != null) {
        User user = dao.getUserById(userId);
        userBalance = user.getBalance();
    }

    // 3. Fetch the Document to Edit
    Document doc = (Document) request.getAttribute("document");
    if (doc == null) {
        response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp");
        return;
    }

    // 4. Fetch User's Folders for the dropdown
    FolderDAO folderDao = new FolderDAO();
    // 🚀 SỬA TẠI ĐÂY: Đổi sang getAllFoldersByUserId để lấy cả thư mục cha và con
    List<Folder> myFolders = folderDao.getAllFoldersByUserId(userId);

    String errorMessage = (String) request.getAttribute("errorMessage");
%>
<!DOCTYPE html>
<html lang="vi" class="dark">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Chỉnh sửa tài liệu - AI Study Hub</title>

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
            html.dark .page-body {
                background-color: #111827;
                color: #f3f4f6;
            }
            html.dark .sidebar {
                background-color: #1f2937;
                border-color: #374151;
            }
            html.dark .brand-text {
                color: #ffffff;
            }
            html.dark .nav-link {
                color: #d1d5db;
            }
            html.dark .nav-link:hover {
                background-color: #374151;
            }
            html.dark .nav-link-active {
                background-color: rgba(49, 46, 129, 0.6);
                color: #818cf8;
            }
            html.dark .user-name {
                color: #ffffff;
            }
            html.dark .user-profile-link:hover {
                background-color: #374151;
            }
            html.dark .logout-btn {
                color: #9ca3af;
            }
            html.dark .logout-btn:hover {
                background-color: rgba(127, 29, 29, 0.3);
                color: #f87171;
            }

            /* Dark mode overrides cho form */
            html.dark .form-card {
                background-color: #1f2937;
                border-color: #374151;
            }
            html.dark .form-label {
                color: #e5e7eb;
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
            html.dark .btn-secondary {
                background-color: #1f2937;
                border-color: #374151;
                color: #e5e7eb;
            }
            html.dark .btn-secondary:hover {
                background-color: #374151;
            }

            @layer components {
                .page-body {
                    @apply flex min-h-screen w-full text-gray-800 bg-[#f8f9fa] font-sans transition-colors duration-200;
                }
                .sidebar {
                    @apply w-64 bg-white border-r border-gray-100 flex flex-col justify-between p-4 flex-shrink-0 min-h-screen shadow-sm z-10 transition-colors duration-200;
                }
                .brand-container {
                    @apply flex items-center space-x-3 px-2 py-1;
                }
                .brand-logo {
                    @apply w-9 h-9 bg-indigo-600 rounded-xl flex items-center justify-center text-white shadow-sm shadow-indigo-600/20;
                }
                .brand-text {
                    @apply font-bold text-gray-900 text-base tracking-tight;
                }
                .nav-link {
                    @apply flex items-center space-x-3 px-4 py-2.5 text-gray-600 hover:bg-gray-50 rounded-xl font-medium text-sm transition-all w-full text-left;
                }
                .nav-link-active {
                    @apply flex items-center space-x-3 px-4 py-2.5 bg-indigo-50 text-indigo-600 rounded-xl font-semibold text-sm transition-colors w-full text-left;
                }
                .wallet-widget {
                    @apply w-full bg-gradient-to-br from-purple-500 to-indigo-600 text-white p-4 rounded-2xl shadow-md shadow-indigo-600/10 relative overflow-hidden;
                }
                .wallet-header {
                    @apply flex justify-between items-center opacity-85;
                }
                .wallet-title {
                    @apply text-xs font-medium tracking-wide;
                }
                .wallet-balance {
                    @apply text-xl font-bold mt-2 tracking-tight;
                }
                .user-area {
                    @apply pt-2 border-t border-gray-100 flex flex-col gap-1;
                }
                .user-profile-link {
                    @apply flex items-center space-x-3 px-2 py-2 rounded-xl hover:bg-gray-50 transition-colors cursor-pointer;
                }
                .user-avatar {
                    @apply w-8 h-8 rounded-full bg-indigo-100 flex items-center justify-center text-indigo-700 flex-shrink-0 font-bold text-xs uppercase;
                }
                .user-info {
                    @apply flex-1 min-w-0;
                }
                .user-name {
                    @apply text-sm font-bold text-gray-900 truncate;
                }
                .user-role {
                    @apply text-[11px] text-gray-400 font-medium;
                }
                .logout-btn {
                    @apply w-full flex items-center space-x-2.5 px-2 py-2 rounded-xl text-sm font-medium text-gray-500 hover:text-red-500 hover:bg-red-50 transition-colors text-left;
                }
                .main-content {
                    @apply flex-1 p-8 overflow-y-auto h-screen relative;
                }
                .header-container {
                    @apply flex justify-between items-center mb-6;
                }
                .page-title {
                    @apply text-2xl font-bold text-gray-900 tracking-tight;
                }
                .form-card {
                    @apply bg-white border border-gray-100 rounded-2xl p-7 shadow-sm max-w-3xl w-full;
                }
                .form-label {
                    @apply block text-sm font-semibold text-gray-700 mb-2;
                }
                .form-input {
                    @apply w-full px-4 py-3 rounded-xl border border-gray-200 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all bg-gray-50 focus:bg-white text-sm;
                }
                .btn-primary {
                    @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-[#5c3cf5] text-white rounded-xl font-semibold hover:bg-indigo-700 transition-colors text-sm shadow-sm shadow-indigo-100 cursor-pointer;
                }
                .btn-secondary {
                    @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-white border border-gray-200 text-gray-700 rounded-xl font-semibold hover:bg-gray-50 transition-colors text-sm shadow-sm cursor-pointer;
                }
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
                    <a href="<%= request.getContextPath()%>/FolderController?action=viewFolder" class="nav-link-active">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z"/></svg>
                        <span>Tài liệu của tôi</span>
                    </a>
                    <a href="<%= request.getContextPath()%>/MainController?action=explore" class="nav-link">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
                        <span>Khám phá tài liệu</span>
                    </a>
                    <a href="<%= request.getContextPath()%>/MainController?action=chatMain" class="nav-link">
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
                            <div class="user-avatar">
                                <%= username != null && !username.trim().isEmpty() ? username.trim().substring(0, 1).toUpperCase() : "U"%>
                            </div>
                            <div class="user-info">
                                <div class="flex items-center gap-1.5 min-w-0">
                                    <p class="user-name truncate"><%= username != null && !username.trim().isEmpty() ? username : "Học viên"%></p>
                                    <% if (isPremiumUser) { %>
                                    <span class="flex-shrink-0 px-1.5 py-0.5 bg-gradient-to-r from-amber-500 to-orange-500 text-white font-bold text-[9px] rounded-full shadow-sm scale-90 origin-left">PRO</span>
                                    <% } else { %>
                                    <span class="flex-shrink-0 px-2 py-0.5 bg-emerald-100 text-emerald-800 border border-emerald-300 dark:bg-emerald-900/60 dark:text-emerald-400 dark:border-emerald-700 font-bold text-[10px] rounded-full shadow-sm scale-90 origin-left tracking-wide">FREE</span>
                                    <% }%>
                                </div>
                                <p class="user-role">Quyền: <%= role%></p>
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

        <main class="main-content">
            <div class="header-container max-w-3xl">
                <h1 class="page-title dark:text-white">Cập nhật tài liệu</h1>
                <a href="<%= request.getContextPath()%>/FolderController?action=viewFolder" class="flex items-center space-x-1.5 px-4 py-2 bg-gray-800 border border-gray-700 text-gray-300 hover:text-white hover:bg-gray-700 rounded-xl font-semibold text-sm transition-all shadow-sm">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path></svg>
                    <span>Quay lại Dashboard</span>
                </a>
            </div>

            <% if (errorMessage != null) {%>
            <div class="max-w-3xl mb-6 p-4 bg-red-950/40 border border-red-800 text-red-400 rounded-xl text-sm font-medium">
                <%= errorMessage%>
            </div>
            <% }%>

            <div class="form-card">
                <div class="mb-6 pb-6 border-b border-gray-700 flex items-start space-x-4">
                    <div class="p-3 bg-indigo-950/50 border border-indigo-900 rounded-xl text-indigo-400">
                        <svg class="w-8 h-8" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path></svg>
                    </div>
                    <div>
                        <h2 class="text-lg font-bold text-white">Thay đổi thông tin</h2>
                        <p class="text-sm text-gray-400 mt-1">Cập nhật tên, thư mục, và quyền chia sẻ cho tài liệu này.</p>
                    </div>
                </div>

                <form action="<%= request.getContextPath()%>/MainController" method="POST">

                    <input type="hidden" name="action" value="updateDoc" />
                    <input type="hidden" name="docId" value="<%= doc.getDocumentId()%>" />

                    <div class="space-y-5">
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
                                            // Gọi hàm đệ quy để hiển thị đường dẫn đầy đủ thay vì chỉ hiện tên
                                            String fullPath = getFolderPath(f, myFolders);
                                %>
                                <option value="<%= f.getFolderId()%>" <%= isSelected ? "selected" : ""%>>
                                    <%= fullPath.replace("<", "&lt;").replace(">", "&gt;")%>
                                </option>
                                <%      }
                                } %>
                            </select>
                        </div>

                        <div>
                            <label class="form-label">Quyền chia sẻ</label>
                            <% String perm = doc.getSharingPermission() != null ? doc.getSharingPermission().toUpperCase() : "PRIVATE";%>
                            <select name="sharingPermission" class="form-input">
                                <option value="PRIVATE" <%= "PRIVATE".equals(perm) ? "selected" : ""%>>Riêng tư (Chỉ mình tôi)</option>
                                <option value="FRIENDS_ONLY" <%= "FRIENDS_ONLY".equals(perm) ? "selected" : ""%>>Chỉ bạn bè</option>
                                <option value="PUBLIC" <%= "PUBLIC".equals(perm) ? "selected" : ""%>>Công khai (Mọi người có thể xem)</option>
                            </select>
                        </div>
                    </div>

                    <div class="mt-8 pt-6 border-t border-gray-700 flex items-center justify-end space-x-3">
                        <a href="<%= request.getContextPath()%>/FolderController?action=viewFolder" class="btn-secondary">
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