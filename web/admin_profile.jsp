<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="Model.DTO.User" %>
<%
    // 1. Kiểm tra quyền Admin bảo mật hệ thống
    HttpSession userSession = request.getSession(false);
    if (userSession == null || userSession.getAttribute("userId") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    String currentUserRole = (String) userSession.getAttribute("role");
    if (!"ADMIN".equalsIgnoreCase(currentUserRole)) {
        response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp");
        return;
    }

    // Thiết lập Safety net hướng riêng về AdminController thay vì MainController của User
    User currentUser = (User) request.getAttribute("currentUser");
    if (currentUser == null) {
        response.sendRedirect(request.getContextPath() + "/AdminController?action=profile");
        return;
    }

    String currentUsername = (String) userSession.getAttribute("username");

    // Lấy thông báo trạng thái lỗi/thành công từ URL Parameters
    String error = request.getParameter("error");
    String updateSuccess = request.getParameter("updateSuccess");
%>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hồ sơ Admin - AI Study Hub Admin</title>

    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = { darkMode: 'class' }
    </script>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">

    <style type="text/tailwindcss">
        @layer components {
            .page-body { @apply flex min-h-screen w-full text-gray-800 bg-[#f8f9fa] font-sans dark:bg-gray-900 dark:text-gray-100; }
            .sidebar { @apply w-64 bg-white border-r border-gray-100 flex flex-col justify-between p-4 flex-shrink-0 min-h-screen shadow-sm dark:bg-gray-800 dark:border-gray-700; }
            .nav-link { @apply flex items-center space-x-3 px-4 py-2.5 text-gray-600 hover:bg-gray-50 rounded-xl font-medium text-sm transition-all w-full text-left dark:text-gray-300 dark:hover:bg-gray-700; }
            .nav-link-active { @apply flex items-center space-x-3 px-4 py-2.5 bg-indigo-50 text-indigo-600 rounded-xl font-semibold text-sm transition-colors w-full text-left dark:bg-indigo-900/50 dark:text-indigo-400; }
            .form-card { @apply bg-white border border-gray-100 rounded-2xl p-7 shadow-sm max-w-3xl mb-6 dark:bg-gray-800 dark:border-gray-700; }
            .form-label { @apply block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2; }
            .form-input { @apply w-full px-4 py-3 rounded-xl border border-gray-200 outline-none transition-all bg-gray-50 focus:bg-white text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 dark:bg-gray-700 dark:border-gray-600 dark:text-white dark:focus:bg-gray-800; }
            .btn-primary { @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-[#5c3cf5] text-white rounded-xl font-semibold hover:bg-indigo-700 transition-colors text-sm shadow-sm cursor-pointer; }
            .btn-secondary { @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-white border border-gray-200 text-gray-700 rounded-xl font-semibold hover:bg-gray-50 transition-colors text-sm shadow-sm cursor-pointer dark:bg-gray-800 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-700; }
            .tab-btn-active { @apply px-5 py-2.5 bg-indigo-600 text-white font-semibold text-sm rounded-xl shadow-sm transition-all; }
            .tab-btn-inactive { @apply px-5 py-2.5 text-gray-600 hover:bg-gray-100 font-medium text-sm rounded-xl transition-all dark:text-gray-400 dark:hover:bg-gray-700; }
        }
    </style>
</head>
<body class="page-body">

    <aside class="sidebar">
        <div class="space-y-6 w-full">
            <a href="<%= request.getContextPath()%>/MainController?action=listDashboard" class="flex items-center space-x-3 px-2 py-1 transition-opacity hover:opacity-80 block w-full">
                <div class="w-9 h-9 bg-red-600 rounded-xl flex items-center justify-center text-white shadow-sm">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path></svg>
                </div>
                <span class="font-bold text-gray-900 text-base tracking-tight dark:text-white">Admin Panel</span>
            </a>

            <nav class="space-y-1 w-full">
                <a href="<%= request.getContextPath()%>/MainController?action=listDashboard" class="nav-link">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/></svg>
                    <span>Dashboard</span>
                </a>
                <a href="<%= request.getContextPath()%>/MainController?action=listUsers" class="nav-link">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path></svg>
                    <span>Quản lý người dùng</span>
                </a>
                <a href="<%= request.getContextPath()%>/MainController?action=adminListTransactions" class="nav-link">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/></svg>
                    <span>Quản lý giao dịch</span>
                </a>
            </nav>
        </div>

        <div class="pt-4 border-t border-gray-100 dark:border-gray-700">
            <a href="<%= request.getContextPath()%>/admin_profile.jsp" class="flex items-center space-x-3 px-2 py-2 mb-2 rounded-xl bg-gray-50 dark:bg-gray-700/50 transition-colors cursor-pointer group">
                <div class="w-8 h-8 rounded-full bg-red-100 flex items-center justify-center text-red-700 font-bold text-xs uppercase"><%= currentUsername != null ? currentUsername.substring(0, 1) : "A"%></div>
                <div class="flex-1 min-w-0">
                    <p class="text-sm font-bold text-red-600 truncate dark:text-red-400"><%= currentUsername != null ? currentUsername : "Admin"%></p>
                    <p class="text-[11px] text-gray-400 font-medium">Hồ sơ cá nhân</p>
                </div>
            </a>
            <a href="<%= request.getContextPath()%>/MainController?action=logout" class="flex items-center space-x-2.5 px-2 py-2 rounded-xl text-sm font-medium text-gray-500 hover:text-red-500 hover:bg-red-50 transition-colors w-full dark:hover:bg-red-900/30">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" x2="9" y1="12" y2="12"/></svg>
                <span>Đăng xuất</span>
            </a>
        </div>
    </aside>

    <main class="flex-1 p-8 overflow-y-auto h-screen relative">
        <div class="flex justify-between items-center mb-6 max-w-3xl">
            <h1 class="text-2xl font-bold text-gray-900 tracking-tight dark:text-white">Cấu hình tài khoản Admin</h1>
            <a href="<%= request.getContextPath()%>/AdminController?action=dashboard" class="btn-secondary !px-4 !py-2 flex items-center space-x-1.5">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path></svg>
                <span>Dashboard</span>
            </a>
        </div>

        <% if ("wrong_password".equals(error)) { %>
            <div class="max-w-3xl mb-6 p-4 bg-red-50 border border-red-200 text-red-600 rounded-xl text-sm font-medium dark:bg-red-950/40 dark:border-red-900 dark:text-red-400">Mật khẩu hiện tại không chính xác. Không thể cập nhật cấu hình Admin.</div>
        <% } else if ("update_failed".equals(error)) { %>
            <div class="max-w-3xl mb-6 p-4 bg-red-50 border border-red-200 text-red-600 rounded-xl text-sm font-medium dark:bg-red-950/40 dark:border-red-900 dark:text-red-400">Lỗi cơ sở dữ liệu: Không thể cập nhật thông tin quản trị viên.</div>
        <% } else if ("1".equals(updateSuccess)) { %>
            <div class="max-w-3xl mb-6 p-4 bg-green-50 border border-green-200 text-green-700 rounded-xl text-sm font-medium dark:bg-green-950/40 dark:border-green-900 dark:text-green-400">Hệ thống đã cập nhật hồ sơ Admin thành công!</div>
        <% } %>

        <div class="flex space-x-2 max-w-3xl mb-6 bg-gray-100 dark:bg-gray-800 p-1.5 rounded-2xl w-fit">
            <button type="button" id="tab-info-btn" onclick="switchTab('info')" class="tab-btn-active">Thông tin cơ bản</button>
            <button type="button" id="tab-security-btn" onclick="switchTab('security')" class="tab-btn-inactive">Thay đổi mật khẩu</button>
        </div>

        <form action="<%= request.getContextPath()%>/MainController" method="POST">
            <input type="hidden" name="action" value="updateProfile" />
            <input type="hidden" name="fromAdmin" value="true" />

            <div id="tab-info-content" class="form-card">
                <div class="mb-6 pb-6 border-b border-gray-100 dark:border-gray-700 flex items-start space-x-4">
                    <div class="p-3 bg-red-50 dark:bg-red-950/50 rounded-xl text-red-600 dark:text-red-400">
                        <svg class="w-7 h-7" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M5.121 17.804A13.937 13.937 0 0112 16c2.5 0 4.847.655 6.879 1.804M15 10a3 3 0 11-6 0 3 3 0 016 0zm6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                    </div>
                    <div>
                        <h2 class="text-lg font-bold text-gray-900 dark:text-white">Danh tính quản trị viên</h2>
                        <p class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">Cập nhật tên tài khoản đăng nhập Admin chính thức và email tiếp nhận thông báo.</p>
                    </div>
                </div>

                <div class="grid grid-cols-1 sm:grid-cols-2 gap-5 mb-6">
                    <div>
                        <label class="form-label">Tên đăng nhập Admin</label>
                        <input type="text" name="username" value="<%= currentUser.getUsername()%>" required class="form-input" />
                    </div>
                    <div>
                        <label class="form-label">Email tiếp nhận</label>
                        <input type="email" name="email" value="<%= currentUser.getEmail()%>" required class="form-input" />
                    </div>
                </div>

                <div class="p-4 bg-gray-50 dark:bg-gray-900/50 rounded-xl border border-gray-200 dark:border-gray-700">
                    <label class="form-label">Xác nhận mật khẩu cấp quản trị <span class="text-red-500">*</span></label>
                    <input type="password" name="currentPassword" required class="form-input bg-white dark:bg-gray-700" placeholder="Nhập mật khẩu hiện tại của bạn để lưu thông tin" />
                </div>

                <div class="mt-6 flex justify-end space-x-3">
                    <a href="<%= request.getContextPath()%>/AdminController?action=dashboard" class="btn-secondary">Hủy bỏ</a>
                    <button type="submit" class="btn-primary">Lưu cấu hình</button>
                </div>
            </div>

            <div id="tab-security-content" class="form-card hidden">
                <div class="mb-6 pb-6 border-b border-gray-100 dark:border-gray-700 flex items-start space-x-4">
                    <div class="p-3 bg-red-50 dark:bg-red-950/50 rounded-xl text-red-600 dark:text-red-400">
                        <svg class="w-7 h-7" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path></svg>
                    </div>
                    <div>
                        <h2 class="text-lg font-bold text-gray-900 dark:text-white">Mật khẩu bảo mật hệ thống</h2>
                        <p class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">Định kỳ thay đổi mật khẩu hệ thống để phòng ngừa các rủi ro bảo mật an ninh.</p>
                    </div>
                </div>

                <div class="space-y-5">
                    <div>
                        <label class="form-label text-[#5c3cf5]">Mật khẩu quản trị mới</label>
                        <input type="password" name="newPassword" class="form-input" placeholder="Thiết lập chuỗi mật khẩu mạnh mới" />
                    </div>

                    <div class="p-4 bg-gray-50 dark:bg-gray-900/50 rounded-xl border border-gray-200 dark:border-gray-700">
                        <label class="form-label">Mật khẩu hiện tại <span class="text-red-500">*</span></label>
                        <input type="password" name="currentPassword" disabled class="form-input bg-white dark:bg-gray-700" placeholder="Nhập mật khẩu hiện tại của bạn để lưu thông tin" />
                    </div>
                </div>

                <div class="mt-6 flex justify-end space-x-3">
                    <a href="<%= request.getContextPath()%>/AdminController?action=dashboard" class="btn-secondary">Hủy bỏ</a>
                    <button type="submit" class="btn-primary">Cập nhật mật khẩu mới</button>
                </div>
            </div>
        </form>
    </main>

    <script>
        function switchTab(tab) {
            const infoBtn = document.getElementById('tab-info-btn');
            const securityBtn = document.getElementById('tab-security-btn');
            const infoContent = document.getElementById('tab-info-content');
            const securityContent = document.getElementById('tab-security-content');

            const infoCurrentPass = infoContent.querySelector('input[name="currentPassword"]');
            const securityCurrentPass = securityContent.querySelector('input[name="currentPassword"]');

            if (tab === 'info') {
                infoBtn.className = "tab-btn-active";
                securityBtn.className = "tab-btn-inactive";

                infoContent.classList.remove('hidden');
                securityContent.classList.add('hidden');

                infoCurrentPass.required = true;
                infoCurrentPass.disabled = false;

                securityCurrentPass.required = false;
                securityCurrentPass.disabled = true;

            } else if (tab === 'security') {
                infoBtn.className = "tab-btn-inactive";
                securityBtn.className = "tab-btn-active";

                infoContent.classList.add('hidden');
                securityContent.classList.remove('hidden');

                infoCurrentPass.required = false;
                infoCurrentPass.disabled = true;

                securityCurrentPass.required = true;
                securityCurrentPass.disabled = false;
            }
        }
    </script>
</body>
</html>