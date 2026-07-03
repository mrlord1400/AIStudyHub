<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="Model.DTO.Document" %>
<%@ page import="java.util.List" %>
<%
    // 1. Kiểm tra đăng nhập và quyền Admin
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

    // 2. Lấy danh sách document public từ AdminController
    List<Document> docList = (List<Document>) request.getAttribute("doc_list");
%>
<!DOCTYPE html>
<html lang="vi" class="dark">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Quản lý tài liệu công khai - AI Study Hub Admin</title>

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
                .btn-primary {
                    @apply flex items-center justify-center space-x-2 px-5 py-2.5 bg-[#5c3cf5] text-white rounded-xl font-semibold hover:bg-indigo-700 transition-colors text-sm shadow-sm cursor-pointer;
                    @ap ply focus:outline-none focus:ring-0;
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
                    <a href="<%= request.getContextPath()%>/AdminController?action=listPublicDocs" class="nav-link-active">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
                        <span>Quản lý tài liệu</span>
                    </a>
                    <a href="<%= request.getContextPath()%>/MainController?action=adminListTransactions" class="nav-link">
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
            <div class="flex justify-between items-center mb-6">
                <div>
                    <h1 class="text-2xl font-bold text-gray-900 tracking-tight dark:text-white">Quản lý tài liệu công khai</h1>
                    <p class="text-gray-500 text-sm mt-0.5">Tài liệu bị cắm cờ hiển thị lên đầu, phần còn lại sắp xếp theo điểm báo cáo giảm dần.</p>
                </div>
            </div>

            <div class="bg-white border border-gray-100 rounded-2xl shadow-sm overflow-hidden dark:bg-gray-800 dark:border-gray-700">
                <div class="overflow-x-auto">
                    <table class="w-full text-left text-sm text-gray-600 dark:text-gray-300">
                        <thead class="bg-gray-50 text-xs text-gray-500 uppercase tracking-wider dark:bg-gray-900/50 dark:text-gray-400 border-b border-gray-100 dark:border-gray-700">
                            <tr>
                                <th class="px-6 py-4 font-semibold">#</th>
                                <th class="px-6 py-4 font-semibold">Tên tài liệu</th>
                                <th class="px-6 py-4 font-semibold">Người sở hữu</th>
                                <th class="px-6 py-4 font-semibold">Total Report Score</th>
                                <th class="px-6 py-4 font-semibold">Trạng thái</th>
                                <th class="px-6 py-4 font-semibold text-right">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-gray-100 dark:divide-gray-700">
                            <% if (docList != null && !docList.isEmpty()) {
                                    int stt = 1;
                                    for (Document d : docList) {
                                        boolean flagged = d.isFlagged();
                                        Double score = d.getTotalReportScore();
                                        double scoreVal = (score != null) ? score : 0.0;
                            %>
                            <tr class="hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors <%= flagged ? "bg-red-50/40 dark:bg-red-950/20" : ""%>">
                                <td class="px-6 py-4 font-medium"><%= stt++%></td>
                                <td class="px-6 py-4">
                                    <p class="font-bold text-gray-900 dark:text-white"><%= d.getTitle() != null ? d.getTitle() : "—"%></p>
                                    <p class="text-[11px] text-gray-400">ID: <%= d.getDocumentId()%></p>
                                </td>
                                <td class="px-6 py-4">
                                    <p class="font-semibold text-gray-700 dark:text-gray-200"><%= d.getAuthorUsername() != null ? d.getAuthorUsername() : "—"%></p>
                                </td>
                                <td class="px-6 py-4">
                                    <span class="font-bold <%= scoreVal > 0 ? "text-rose-600 dark:text-rose-400" : "text-gray-400"%>"><%= String.format("%.2f", scoreVal)%></span>
                                </td>
                                <td class="px-6 py-4">
                                    <% if (flagged) { %>
                                    <span class="flex items-center space-x-1.5 text-rose-600 dark:text-rose-400 font-bold text-xs">
                                        <span class="w-1.5 h-1.5 bg-rose-500 rounded-full animate-pulse"></span>
                                        <span>Đã cắm cờ</span>
                                    </span>
                                    <% } else { %>
                                    <span class="flex items-center space-x-1.5 text-emerald-600 dark:text-emerald-400 font-medium text-xs">
                                        <span class="w-1.5 h-1.5 bg-emerald-500 rounded-full"></span>
                                        <span>Bình thường</span>
                                    </span>
                                    <% }%>
                                </td>
                                <td class="px-6 py-4 text-right">
                                    <a href="<%= request.getContextPath()%>/AdminController?action=adminViewDoc&docId=<%= d.getDocumentId()%>" target="_blank" class="btn-primary px-4 py-2 inline-flex">
                                        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/><path stroke-linecap="round" stroke-linejoin="round" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/></svg>
                                        <span>Xem content</span>
                                    </a>
                                </td>
                            </tr>
                            <%  }
                            } else { %>
                            <tr>
                                <td colspan="6" class="px-6 py-12 text-center text-gray-400 dark:text-gray-500">Chưa có tài liệu công khai nào.</td>
                            </tr>
                            <% }%>
                        </tbody>
                    </table>
                </div>
            </div>
        </main>
    </body>
</html>
