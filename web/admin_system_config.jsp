<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
    // Kiểm tra bảo mật Admin
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
%>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cấu hình Hệ thống - AI Study Hub Admin</title>

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
            .btn-primary { @apply flex items-center justify-center space-x-2 px-6 py-3 bg-red-600 text-white rounded-xl font-semibold hover:bg-red-700 transition-colors text-sm shadow-sm cursor-pointer; }
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
                <a href="<%= request.getContextPath()%>/MainController?action=adminListTransactions" class="nav-link">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/></svg>
                    <span>Quản lý giao dịch</span>
                </a>
                <a href="<%= request.getContextPath()%>/admin/system-config" class="nav-link-active">
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
        <div class="max-w-6xl mx-auto">
            <div class="mb-6">
                <h2 class="text-2xl font-bold text-gray-900 tracking-tight dark:text-white">Cấu hình các gói dịch vụ (Tiers)</h2>
                <p class="text-sm text-gray-500 mt-1 dark:text-gray-400">Điều chỉnh giá tiền, dung lượng lưu trữ và giới hạn AI cho từng nhóm người dùng.</p>
            </div>

            <c:if test="${not empty successMessage}">
                <div class="mb-6 p-4 bg-green-50 border border-green-200 text-green-700 rounded-xl flex items-center space-x-2 dark:bg-green-950/40 dark:border-green-900 dark:text-green-400">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"></path></svg>
                    <span class="font-medium text-sm">${successMessage}</span>
                </div>
            </c:if>
            
            <c:if test="${not empty errorMessage}">
                <div class="mb-6 p-4 bg-red-50 border border-red-200 text-red-600 rounded-xl flex items-center space-x-2 dark:bg-red-950/40 dark:border-red-900 dark:text-red-400">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"></path></svg>
                    <span class="font-medium text-sm">${errorMessage}</span>
                </div>
            </c:if>

            <form action="${pageContext.request.contextPath}/admin/system-config" method="POST">
                <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    
                    <c:choose>
                        <c:when test="${not empty subList}">
                            <c:forEach var="sub" items="${subList}">
                                <div class="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden relative dark:bg-gray-800 dark:border-gray-700">
                                    <div class="bg-gray-50 px-6 py-4 border-b border-gray-200 flex justify-between items-center dark:bg-gray-900/50 dark:border-gray-700">
                                        <h3 class="text-lg font-bold text-gray-900 uppercase dark:text-white">${sub.tierName}</h3>
                                        <c:if test="${sub.tierName eq 'Premium'}">
                                            <span class="bg-amber-100 text-amber-700 text-xs px-2 py-1 rounded-md font-bold dark:bg-amber-900/30 dark:text-amber-400">PRO</span>
                                        </c:if>
                                    </div>
                                    
                                    <div class="p-6 space-y-5">
                                        <input type="hidden" name="tierId" value="${sub.tierId}" />
                                        
                                        <div>
                                            <label class="block text-sm font-semibold text-gray-700 mb-1 dark:text-gray-300">Giá (VND)</label>
                                            <div class="relative">
                                                <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                                    <span class="text-gray-500 sm:text-sm dark:text-gray-400">$</span>
                                                </div>
                                                <input type="number" step="0.01" min="0" name="price_${sub.tierId}" value="${sub.price}" 
                                                       class="w-full pl-7 pr-4 py-2.5 rounded-xl border border-gray-300 focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none transition-all text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-white" 
                                                       <c:if test="${sub.tierName eq 'Guest' or sub.tierName eq 'Free'}">readonly class="bg-gray-100 dark:bg-gray-800"</c:if> />
                                            </div>
                                            <c:if test="${sub.tierName eq 'Guest' or sub.tierName eq 'Free'}">
                                                <p class="text-xs text-gray-400 mt-1">Gói này mặc định miễn phí.</p>
                                            </c:if>
                                        </div>

                                        <div>
                                            <label class="block text-sm font-semibold text-gray-700 mb-1 dark:text-gray-300">Upload File Tối đa (MB)</label>
                                            <input type="number" min="0" name="maxStorage_${sub.tierId}" value="${sub.maxStorageMb}" required
                                                   class="w-full px-4 py-2.5 rounded-xl border border-gray-300 focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none transition-all text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-white" />
                                        </div>

                                        <div>
                                            <label class="block text-sm font-semibold text-gray-700 mb-1 dark:text-gray-300">Tổng kho lưu trữ (MB)</label>
                                            <input type="number" min="0" name="totalStorage_${sub.tierId}" value="${sub.totalStorageMb}" required
                                                   class="w-full px-4 py-2.5 rounded-xl border border-gray-300 focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none transition-all text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-white" />
                                            <p class="text-[11px] text-gray-400 mt-1">Gợi ý: 5GB = 5120MB, 50GB = 51200MB</p>
                                        </div>

                                        <div>
                                            <label class="block text-sm font-semibold text-gray-700 mb-1 dark:text-gray-300">Số lần hỏi AI / Ngày</label>
                                            <input type="number" min="0" name="aiLimit_${sub.tierId}" value="${sub.aiPromptLimitPerDay}" required
                                                   class="w-full px-4 py-2.5 rounded-xl border border-gray-300 focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none transition-all text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-white" />
                                        </div>
                                    </div>
                                </div>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <div class="col-span-3 text-center py-10 text-gray-500 dark:text-gray-400">
                                Chưa có dữ liệu. Hãy đảm bảo Database của bạn đã có dữ liệu trong bảng subscriptions.
                            </div>
                        </c:otherwise>
                    </c:choose>
                    
                </div>

                <div class="mt-8 flex justify-end">
                    <button type="submit" class="btn-primary">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4"></path></svg>
                        <span>Lưu cấu hình hệ thống</span>
                    </button>
                </div>
            </form>
        </div>
    </main>
</body>
</html>