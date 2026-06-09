<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="Model.User" %>
<%@ page import="java.util.List" %>
<%
    // 1. Kiểm tra đăng nhập và quyền Admin (Ngăn chặn truy cập trái phép)
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

    String currentUsername = (String) userSession.getAttribute("username");

    // 2. Lấy danh sách người dùng được nạp từ AdminController sang
    List<User> userList = (List<User>) request.getAttribute("user_list");
    
    // 3. Tính toán số lượng người dùng thực tế (loại trừ các tài khoản quyền ADMIN)
    int totalUsers = 0;
    if (userList != null) {
        totalUsers = userList.size();
    }

    // Lấy dữ liệu thống kê giao dịch từ Controller truyền sang thông qua Attribute
    Integer totalTransactions = (Integer) request.getAttribute("totalTransactionAmount");
    if (totalTransactions == null) {
        totalTransactions = 0;
    }
%>
<!DOCTYPE html>
<html lang="vi" class="dark">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Bảng điều khiển - AI Study Hub Admin</title>

        <script src="https://cdn.tailwindcss.com"></script>
        <script>
            tailwind.config = {darkMode: 'class'}
        </script>
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">

        <style type="text/tailwindcss">
            @layer components {
                .page-body {
                    @apply flex min-h-screen w-full text-gray-800 bg-[#f8f9fa] font-sans dark:bg-gray-900 dark:text-gray-100;
                }
                .sidebar {
                    @apply w-64 bg-white border-r border-gray-100 flex flex-col justify-between p-4 flex-shrink-0 min-h-screen shadow-sm dark:bg-gray-800 dark:border-gray-700;
                }
                .nav-link {
                    @apply flex items-center space-x-3 px-4 py-2.5 text-gray-600 hover:bg-gray-50 rounded-xl font-medium text-sm transition-all w-full text-left dark:text-gray-300 dark:hover:bg-gray-700;
                }
                .nav-link-active {
                    @apply flex items-center space-x-3 px-4 py-2.5 bg-indigo-50 text-indigo-600 rounded-xl font-semibold text-sm transition-colors w-full text-left dark:bg-indigo-900/50 dark:text-indigo-400;
                }
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
                    <a href="<%= request.getContextPath()%>/MainController?action=listDashboard" class="nav-link-active">
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
                <a href="<%= request.getContextPath()%>/AdminController?action=profile" class="flex items-center space-x-3 px-2 py-2 mb-2 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors cursor-pointer group">
                    <div class="w-8 h-8 rounded-full bg-red-100 flex items-center justify-center text-red-700 font-bold text-xs uppercase group-hover:bg-red-200 transition-colors"><%= currentUsername != null ? currentUsername.substring(0, 1) : "A"%></div>
                    <div class="flex-1 min-w-0">
                        <p class="text-sm font-bold text-gray-900 truncate dark:text-white group-hover:text-red-600 transition-colors"><%= currentUsername != null ? currentUsername : "Admin"%></p>
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
            <div class="mb-8">
                <h1 class="text-2xl font-bold text-gray-900 tracking-tight dark:text-white">Tổng quan hệ thống</h1>
                <p class="text-gray-500 text-sm mt-0.5">Chào mừng quay trở lại, <%= currentUsername%>!</p>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">

                <div class="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm flex items-center justify-between dark:bg-gray-800 dark:border-gray-700">
                    <div>
                        <p class="text-xs font-medium text-gray-500 uppercase tracking-wider mb-1">Tổng người dùng</p>
                        <h3 class="text-2xl font-bold text-gray-900 dark:text-white"><%= String.format("%,d", totalUsers)%></h3>
                    </div>
                    <div class="w-12 h-12 bg-indigo-100 text-indigo-600 rounded-2xl flex items-center justify-center dark:bg-indigo-900/30 dark:text-indigo-400">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                        <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"></path>
                        <circle cx="9" cy="7" r="4"></circle>
                        <path d="M23 21v-2a4 4 0 00-3-3.87"></path>
                        <path d="M16 3.13a4 4 0 010 7.75"></path>
                        </svg>
                    </div>
                </div>

                <div class="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm flex items-center justify-between dark:bg-gray-800 dark:border-gray-700">
                    <div>
                        <p class="text-xs font-medium text-gray-500 uppercase tracking-wider mb-1">Tổng giao dịch</p>
                        <h3 class="text-2xl font-bold text-gray-900 dark:text-white"><%= String.format("%,d", totalTransactions)%></h3>
                    </div>
                    <div class="w-12 h-12 bg-emerald-100 text-emerald-600 rounded-2xl flex items-center justify-center dark:bg-emerald-900/30 dark:text-emerald-400">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                        <rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/>
                        </svg>
                    </div>
                </div>

                <div class="bg-indigo-600 p-6 rounded-2xl shadow-sm flex items-center justify-between border-none dark:bg-indigo-700">
                    <div>
                        <p class="text-white/80 text-xs font-medium uppercase tracking-wider mb-1">Hạ tầng hệ thống</p>
                        <h3 class="text-xl font-bold text-white">Hoạt động ổn định</h3>
                    </div>
                    <div class="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center text-white">
                        <svg class="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
                        </svg>
                    </div>
                </div>

            </div>

            <div class="bg-white p-7 rounded-2xl border border-gray-100 shadow-sm dark:bg-gray-800 dark:border-gray-700">
                <h2 class="text-lg font-bold text-gray-900 mb-2 dark:text-white">Hệ thống phân tích AI Study Hub</h2>
                <p class="text-gray-500 text-sm leading-relaxed">
                    Đây là khu vực hiển thị các thông số tổng quan lõi của ứng dụng. 
                    Sử dụng menu thanh điều hướng bên trái để quản lý chi tiết danh sách tài khoản người dùng, cấp bậc phân quyền hoặc kiểm duyệt các trạng thái giao dịch nạp tiền trên hệ thống.
                </p>
            </div>
        </main>

        <script>
            <% if (request.getAttribute("user_list") == null) { %>
                // Kiểm tra nếu vào trực tiếp file jsp không thông qua Controller
                // Tự động chuyển hướng về Controller để nạp danh sách dữ liệu thực tế
                window.location.href = "<%= request.getContextPath() %>/AdminController?action=listDashboard";
            <% } %>
        </script>
    </body>
</html>