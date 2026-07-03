<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="Model.DTO.Transaction" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
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

    String currentUsername = (String) userSession.getAttribute("username");

    // 2. Nhận dữ liệu danh sách giao dịch từ Controller truyền sang
    List<Transaction> transList = (List<Transaction>) request.getAttribute("transaction_list");

    // Tính toán số liệu nhanh để hiển thị các Thẻ Thống kê (Stat Cards)
    int totalTrans = (transList != null) ? transList.size() : 0;
    double totalDeposit = 0;
    int pendingCount = 0;

    if (transList != null) {
        for (Transaction t : transList) {
            if ("SUCCESS".equalsIgnoreCase(t.getStatus()) && "DEPOSIT".equalsIgnoreCase(t.getType())) {
                totalDeposit += t.getAmount();
            }
            if ("PENDING".equalsIgnoreCase(t.getStatus())) {
                pendingCount++;
            }
        }
    }

    // Khởi tạo bộ định dạng ngày giờ
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
%>
<!DOCTYPE html>
<html lang="vi" class="dark">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Quản lý giao dịch - AI Study Hub Admin</title>

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
                .status-select {
                    @apply bg-gray-50 border border-gray-200 text-gray-700 text-xs rounded-lg focus:ring-indigo-500 focus:border-indigo-500 block p-1.5 dark:bg-gray-700 dark:border-gray-600 dark:text-gray-200 cursor-pointer transition-all;
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
                    <a href="<%= request.getContextPath()%>/MainController?action=listDashboard" class="nav-link">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/></svg>
                        <span>Dashboard</span>
                    </a>
                    <a href="<%= request.getContextPath()%>/MainController?action=listUsers" class="nav-link">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path></svg>
                        <span>Quản lý người dùng</span>
                    </a>
                    <a href="<%= request.getContextPath()%>/AdminController?action=listPublicDocs" class="nav-link">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
                        <span>Quản lý tài liệu</span>
                    </a>
                    <a href="<%= request.getContextPath()%>/MainController?action=adminListTransactions" class="nav-link-active">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/></svg>
                        <span>Quản lý giao dịch</span>
                    </a>
                    <a href="<%= request.getContextPath()%>/admin/system-config" class="nav-link">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path></svg>
                        <span>Cấu hình hệ thống</span>
                    </a>
                </nav>
            </div>

            <div class="pt-4 border-t border-gray-100 dark:border-gray-700">
                <a href="<%= request.getContextPath()%>/admin_profile.jsp" class="flex items-center space-x-3 px-2 py-2 mb-2 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors cursor-pointer group">
                    <div class="w-8 h-8 rounded-full bg-red-100 flex items-center justify-center text-red-700 font-bold text-xs uppercase group-hover:bg-red-200 transition-colors"><%= currentUsername != null && !currentUsername.isEmpty() ? currentUsername.substring(0, 1) : "A"%></div>
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
                <h1 class="text-2xl font-bold text-gray-900 tracking-tight dark:text-white">Tổng quan giao dịch</h1>
                <p class="text-gray-500 text-sm mt-0.5">Theo dõi chi tiết và phê duyệt các thanh toán từ hệ thống.</p>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                <div class="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm flex items-center justify-between dark:bg-gray-800 dark:border-gray-700">
                    <div>
                        <p class="text-xs font-medium text-gray-500 uppercase tracking-wider mb-1">Tổng số hóa đơn</p>
                        <h3 class="text-2xl font-bold text-gray-900 dark:text-white"><%= String.format("%,d", totalTrans)%></h3>
                    </div>
                    <div class="w-12 h-12 bg-indigo-100 text-indigo-600 rounded-2xl flex items-center justify-center dark:bg-indigo-900/30 dark:text-indigo-400">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/></svg>
                    </div>
                </div>

            <nav class="space-y-1 w-full">
                <a href="<%= request.getContextPath()%>/MainController?action=listDashboard" class="nav-link">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/></svg>
                    <span>Dashboard</span>
                </a>
                <a href="<%= request.getContextPath()%>/MainController?action=listUsers" class="nav-link">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path></svg>
                    <span>Quản lý người dùng</span>
                </a>
                <a href="<%= request.getContextPath()%>/MainController?action=adminListTransactions" class="nav-link-active">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/></svg>
                    <span>Quản lý giao dịch</span>
                </a>
                <a href="<%= request.getContextPath()%>/admin/system-config" class="nav-link">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path></svg>
                    <span>Cấu hình hệ thống</span>
                </a>
                <a href="<%= request.getContextPath()%>/ReportConfigController" class="nav-link">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path></svg>
                    <span>Cấu hình Báo cáo</span>
                </a>
            </nav>
        </div>

        <div class="pt-4 border-t border-gray-100 dark:border-gray-700">
            <a href="<%= request.getContextPath()%>/admin_profile.jsp" class="flex items-center space-x-3 px-2 py-2 mb-2 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors cursor-pointer group">
                <div class="w-8 h-8 rounded-full bg-red-100 flex items-center justify-center text-red-700 font-bold text-xs uppercase group-hover:bg-red-200 transition-colors"><%= currentUsername != null && !currentUsername.isEmpty() ? currentUsername.substring(0, 1) : "A"%></div>
                <div class="flex-1 min-w-0">
                    <p class="text-sm font-bold text-gray-900 truncate dark:text-white group-hover:text-red-600 transition-colors"><%= currentUsername != null ? currentUsername : "Admin"%></p>
                    <p class="text-[11px] text-gray-400 font-medium">Hồ sơ cá nhân</p>
                </div>

                <div class="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm flex items-center justify-between dark:bg-gray-800 dark:border-gray-700">
                    <div>
                        <p class="text-xs font-medium text-gray-500 uppercase tracking-wider mb-1">Đơn đang chờ duyệt</p>
                        <h3 class="text-2xl font-bold text-amber-500"><%= pendingCount%> <span class="text-sm font-semibold">Đơn</span></h3>
                    </div>
                    <div class="w-12 h-12 bg-amber-100 text-amber-600 rounded-2xl flex items-center justify-center dark:bg-amber-900/30 dark:text-amber-400">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                    </div>
                </div>
            </div>

            <div class="bg-white border border-gray-100 rounded-2xl shadow-sm overflow-hidden dark:bg-gray-800 dark:border-gray-700">
                <div class="overflow-x-auto">
                    <table class="w-full text-left text-sm text-gray-600 dark:text-gray-300">
                        <thead class="bg-gray-50 text-xs text-gray-500 uppercase tracking-wider dark:bg-gray-900/50 dark:text-gray-400 border-b border-gray-100 dark:border-gray-700">
                            <tr>
                                <th class="px-6 py-4 font-semibold">Mã đơn</th>
                                <th class="px-6 py-4 font-semibold">Khách hàng</th>
                                <th class="px-6 py-4 font-semibold">Biến động</th>
                                <th class="px-6 py-4 font-semibold">Hình thức</th>
                                <th class="px-6 py-4 font-semibold">Thời gian tạo</th>
                                <th class="px-6 py-4 font-semibold text-center">Trạng thái hệ thống</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-gray-100 dark:divide-gray-700">
                            <% if (transList != null && !transList.isEmpty()) {
                                    for (Transaction t : transList) {%>
                            <tr class="hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
                                <td class="px-6 py-4 font-medium text-indigo-600 dark:text-indigo-400">#<%= t.getTransactionId()%></td>
                                <td class="px-6 py-4">
                                    <p class="font-bold text-gray-900 dark:text-white"><%= t.getUsername() != null ? t.getUsername() : "Chưa cập nhật"%></p>
                                    <p class="text-[11px] text-gray-400">ID người dùng: <%= t.getUserId()%></p>
                                </td>
                                <td class="px-6 py-4">
                                    <% if ("DEPOSIT".equalsIgnoreCase(t.getType())) {%>
                                    <span class="font-bold text-emerald-600 dark:text-emerald-400">+<%= String.format("%,.0f", Math.abs(t.getAmount()))%></span>
                                    <% } else {%>
                                    <span class="font-bold text-rose-600 dark:text-rose-400">-<%= String.format("%,.0f", Math.abs(t.getAmount()))%></span>
                                    <% } %>
                                </td>
                                <td class="px-6 py-4">
                                    <% if ("DEPOSIT".equalsIgnoreCase(t.getType())) { %>
                                    <span class="px-2 py-0.5 text-[11px] font-semibold bg-emerald-50 text-emerald-700 rounded-md dark:bg-emerald-950/40 dark:text-emerald-400">Nạp ví</span>
                                    <% } else { %>
                                    <span class="px-2 py-0.5 text-[11px] font-semibold bg-rose-50 text-rose-700 rounded-md dark:bg-rose-950/40 dark:text-rose-400">Gói nâng cấp</span>
                                    <% } %>
                                </td>
                                <td class="px-6 py-4 text-xs text-gray-400">
                                    <% if (t.getStartedAt() != null) {%>
                                    <span><%= t.getStartedAt().format(formatter)%></span>
                                    <% } else { %>
                                    <span>---</span>
                                    <% } %>

                                    <% if ("SUCCESS".equalsIgnoreCase(t.getStatus()) && t.getCompletedAt() != null) {%>
                                    <p class="text-[10px] text-emerald-500 mt-0.5 font-medium">Khớp: <%= t.getCompletedAt().format(formatter)%></p>
                                    <% }%>
                                </td>
                                <td class="px-6 py-4">
                                    <div class="flex items-center justify-center">
                                        <form action="<%= request.getContextPath()%>/MainController" method="POST" class="flex items-center space-x-2">
                                            <input type="hidden" name="action" value="adminUpdateTransaction" />
                                            <input type="hidden" name="transactionId" value="<%= t.getTransactionId()%>" />

                                            <select name="newStatus" class="status-select" onchange="this.form.submit()">
                                                <option value="PENDING" <%= "PENDING".equalsIgnoreCase(t.getStatus()) ? "selected" : ""%>>⏳ Đang chờ</option>
                                                <option value="PROCESSING" <%= "PROCESSING".equalsIgnoreCase(t.getStatus()) ? "selected" : ""%>>⏳ Đang xử lý</option>
                                                <option value="SUCCESS" <%= "SUCCESS".equalsIgnoreCase(t.getStatus()) ? "selected" : ""%>>✅ Thành công</option>
                                                <option value="CANCELLED" <%= "CANCELLED".equalsIgnoreCase(t.getStatus()) ? "selected" : ""%>>❌ Thất bại / Hủy</option>
                                            </select>
                                        </form>
                                    </div>
                                </td>
                            </tr>
                            <%  }
                            } else { %>
                            <tr>
                                <td colspan="6" class="px-6 py-12 text-center text-gray-400 dark:text-gray-500">
                                    Chưa tìm thấy dữ liệu giao dịch từ yêu cầu hệ thống.
                                </td>
                            </tr>
                            <% }%>
                        </tbody>
                    </table>
                </div>
            </div>
        </main>

    </body>
</html>