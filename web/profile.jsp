<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="Model.User" %>
<%
    HttpSession userSession = request.getSession(false);
    if (userSession == null || userSession.getAttribute("userId") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    // Safety net: If accessed directly, route back to the controller to fetch data
    User currentUser = (User) request.getAttribute("currentUser");
    if (currentUser == null) {
        response.sendRedirect(request.getContextPath() + "/MainController?action=profile");
        return;
    }

    String username = (String) userSession.getAttribute("username");
    String role = (String) userSession.getAttribute("role");
    
    // Status Parameters
    String error = request.getParameter("error");
    String updateSuccess = request.getParameter("updateSuccess");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hồ sơ của tôi - AI Study Hub</title>
    
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
            .user-profile-link { @apply flex items-center space-x-3 px-2 py-2 rounded-xl bg-gray-50 transition-colors cursor-pointer block; }
            .user-avatar { @apply w-8 h-8 rounded-full bg-indigo-100 flex items-center justify-center text-indigo-700 flex-shrink-0 font-bold text-xs uppercase; }
            .user-info { @apply flex-1 min-w-0; }
            .user-name { @apply text-sm font-bold text-gray-900 truncate; }
            .user-role { @apply text-[11px] text-gray-400 font-medium; }
            .logout-btn { @apply w-full flex items-center space-x-2.5 px-2 py-2 mt-1 rounded-xl text-sm font-medium text-gray-500 hover:text-red-500 hover:bg-red-50 transition-colors text-left; }
            
            .main-content { @apply flex-1 p-8 overflow-y-auto h-screen; }
            .header-container { @apply flex justify-between items-center mb-8; }
            .page-title { @apply text-2xl font-bold text-gray-900 tracking-tight; }
            
            .form-card { @apply bg-white border border-gray-100 rounded-2xl p-7 shadow-sm max-w-3xl mb-6; }
            .form-label { @apply block text-sm font-semibold text-gray-700 mb-2; }
            .form-input { @apply w-full px-4 py-3 rounded-xl border border-gray-200 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all bg-gray-50 focus:bg-white text-sm; }
            .btn-primary { @apply flex items-center justify-center space-x-2 px-6 py-3 bg-[#5c3cf5] text-white rounded-xl font-semibold hover:bg-indigo-700 transition-colors text-sm shadow-sm shadow-indigo-100; }
            .btn-danger { @apply flex items-center justify-center space-x-2 px-6 py-3 bg-red-500 text-white rounded-xl font-semibold hover:bg-red-600 transition-colors text-sm shadow-sm shadow-red-100; }
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
                <a href="<%= request.getContextPath() %>/user_dashboard.jsp" class="nav-link">
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
                <a href="<%= request.getContextPath() %>/MainController?action=logout" class="logout-btn">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" x2="9" y1="12" y2="12"/></svg>
                    <span>Đăng xuất</span>
                </a>
            </div>
        </div>
    </aside>

    <main class="main-content">
        <div class="header-container max-w-3xl">
            <h1 class="page-title">Hồ sơ cá nhân</h1>
            <a href="<%= request.getContextPath() %>/user_dashboard.jsp" class="text-sm font-medium text-indigo-600 hover:text-indigo-800">Quay lại Dashboard</a>
        </div>

        <% if ("wrong_password".equals(error)) { %>
            <div class="max-w-3xl mb-6 p-4 bg-red-50 border border-red-200 text-red-600 rounded-xl text-sm font-medium">Mật khẩu hiện tại không chính xác. Không thể lưu thay đổi.</div>
        <% } else if ("update_failed".equals(error)) { %>
            <div class="max-w-3xl mb-6 p-4 bg-red-50 border border-red-200 text-red-600 rounded-xl text-sm font-medium">Lỗi hệ thống: Không thể cập nhật thông tin.</div>
        <% } else if ("1".equals(updateSuccess)) { %>
            <div class="max-w-3xl mb-6 p-4 bg-green-50 border border-green-200 text-green-700 rounded-xl text-sm font-medium">Cập nhật thông tin thành công!</div>
        <% } %>

        <div class="form-card">
            <div class="mb-6 pb-6 border-b border-gray-100 flex items-start space-x-4">
                <div class="p-3 bg-indigo-50 rounded-xl text-indigo-600">
                    <svg class="w-8 h-8" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path></svg>
                </div>
                <div>
                    <h2 class="text-lg font-bold text-gray-900">Thông tin cơ bản</h2>
                    <p class="text-sm text-gray-500 mt-1">Cập nhật tên hiển thị, địa chỉ email và mật khẩu của bạn.</p>
                </div>
            </div>

            <form action="<%= request.getContextPath() %>/MainController" method="POST">
                <input type="hidden" name="action" value="updateProfile" />
                
                <div class="space-y-5">
                    <div class="grid grid-cols-2 gap-5">
                        <div>
                            <label class="form-label">Tên hiển thị</label>
                            <input type="text" name="username" value="<%= currentUser.getUsername() %>" required class="form-input" />
                        </div>
                        <div>
                            <label class="form-label">Email</label>
                            <input type="email" name="email" value="<%= currentUser.getEmail() %>" required class="form-input" />
                        </div>
                    </div>

                    <div class="mt-6 border-t border-gray-100 pt-6">
                        <label class="form-label text-indigo-600 mb-4">Thay đổi mật khẩu (Tùy chọn)</label>
                        <div>
                            <label class="form-label">Mật khẩu mới</label>
                            <input type="password" name="newPassword" class="form-input mb-2" placeholder="Để trống nếu không muốn đổi mật khẩu" />
                        </div>
                    </div>

                    <div class="p-4 bg-gray-50 rounded-xl border border-gray-200 mt-6">
                        <label class="form-label">Mật khẩu hiện tại <span class="text-red-500">*</span></label>
                        <input type="password" name="currentPassword" required class="form-input bg-white" placeholder="Bắt buộc nhập để xác nhận thay đổi" />
                    </div>
                </div>

                <div class="mt-8 flex justify-end">
                    <button type="submit" class="btn-primary">Lưu thay đổi</button>
                </div>
            </form>
        </div>

        <div class="form-card border-red-200 bg-red-50/30">
            <div class="flex items-start justify-between">
                <div>
                    <h2 class="text-lg font-bold text-red-600">Khu vực nguy hiểm</h2>
                    <p class="text-sm text-red-500 mt-1 max-w-xl">Xóa tài khoản của bạn sẽ xóa vĩnh viễn mọi dữ liệu, tài liệu, và lịch sử trò chuyện. Hành động này không thể hoàn tác.</p>
                </div>
                <form action="<%= request.getContextPath() %>/MainController" method="POST" onsubmit="return confirm('CẢNH BÁO: Bạn có chắc chắn muốn xóa vĩnh viễn tài khoản này không?');">
                    <input type="hidden" name="action" value="deleteAccount" />
                    <button type="submit" class="btn-danger">Xóa tài khoản</button>
                </form>
            </div>
        </div>

    </main>
</body>
</html>