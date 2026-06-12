<%-- 
    Document   : AdminLayout
    Created on : Jun 8, 2026, 1:36:57 PM
--%>

<%@ tag language="java" pageEncoding="UTF-8"%>
<%@ attribute name="title" required="true" type="java.lang.String" description="Tiêu đề của trang" %>
<%@ attribute name="activeMenu" required="true" type="java.lang.String" description="Đánh dấu menu đang active (users, documents, transactions)" %>

<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>${title} - AI Study Hub Admin</title>

        <script src="https://cdn.tailwindcss.com"></script>

        <link rel="preconnect" href="https://fonts.googleapis.com">
        <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">

        <style>
            body {
                font-family: 'Inter', sans-serif;
            }

            /* Mobile sidebar transitions */
            #sidebar {
                transition: transform 0.2s ease-in-out;
            }
            .sidebar-open #sidebar {
                transform: translateX(0);
            }
            .sidebar-closed #sidebar {
                transform: translateX(-100%);
            }
            @media (min-width: 1024px) {
                .sidebar-closed #sidebar {
                    transform: translateX(0);
                }
            }
        </style>
    </head>
    <body class="bg-gray-50 sidebar-closed">

        <div class="h-screen w-full flex overflow-hidden bg-gray-50">

            <div id="mobile-overlay" class="fixed inset-0 bg-black/50 z-40 hidden lg:hidden" onclick="toggleSidebar()"></div>

            <aside id="sidebar" class="fixed inset-y-0 left-0 z-50 w-64 bg-gray-900 lg:static flex flex-col h-full">

                <div class="flex items-center justify-between p-4 border-b border-gray-800">
                    <a href="${pageContext.request.contextPath}/MainController?action=listDashboard" class="flex items-center space-x-2">
                        <div class="w-10 h-10 bg-red-600 rounded-lg flex items-center justify-center">
                            <svg class="w-6 h-6 text-white" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2-1 4-3 5.99-5.11a2 2 0 0 1 2.02 0C14.97 2 17 4 19 5a1 1 0 0 1 1 1z"></path></svg>
                        </div>
                        <div>
                            <span class="font-semibold text-white">Admin Panel</span>
                            <p class="text-xs text-gray-400">AI Study Hub</p>
                        </div>
                    </a>
                    <button onclick="toggleSidebar()" class="lg:hidden text-gray-400 hover:text-white">
                        <svg class="w-5 h-5" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                    </button>
                </div>

                <nav class="flex-1 p-4 space-y-1">

                    <a href="${pageContext.request.contextPath}/MainController?action=listDashboard" 
                       class="flex items-center space-x-3 px-4 py-3 rounded-lg transition-colors ${activeMenu == 'dashboard' ? 'bg-red-600 text-white' : 'text-gray-300 hover:bg-gray-800'}">
                        <svg class="w-5 h-5" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/></svg>
                        <span>Dashboard</span>
                    </a>

                    <a href="${pageContext.request.contextPath}/MainController?action=listUsers" 
                       class="flex items-center space-x-3 px-4 py-3 rounded-lg transition-colors ${activeMenu == 'users' ? 'bg-red-600 text-white' : 'text-gray-300 hover:bg-gray-800'}">
                        <svg class="w-5 h-5" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M22 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
                        <span>Quản lý User</span>
                    </a>

                    <a href="${pageContext.request.contextPath}/MainController?action=adminListDocuments" 
                       class="flex items-center space-x-3 px-4 py-3 rounded-lg transition-colors ${activeMenu == 'documents' ? 'bg-red-600 text-white' : 'text-gray-300 hover:bg-gray-800'}">
                        <svg class="w-5 h-5" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><line x1="10" y1="9" x2="8" y2="9"></line></svg>
                        <span>Quản lý Tài liệu</span>
                    </a>

                    <a href="${pageContext.request.contextPath}/MainController?action=adminListTransactions" 
                       class="flex items-center space-x-3 px-4 py-3 rounded-lg transition-colors ${activeMenu == 'transactions' ? 'bg-red-600 text-white' : 'text-gray-300 hover:bg-gray-800'}">
                        <svg class="w-5 h-5" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="1" x2="12" y2="23"></line><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path></svg>
                        <span>Quản lý Giao dịch</span>
                    </a>

                    <a href="${pageContext.request.contextPath}/admin/system-config" 
                       class="flex items-center space-x-3 px-4 py-3 rounded-lg transition-colors ${activeMenu == 'system-config' ? 'bg-red-600 text-white' : 'text-gray-300 hover:bg-gray-800'}">
                        <svg class="w-5 h-5" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
                        <span>Cấu hình Hệ thống</span>
                    </a>

                </nav>

                <div class="p-4 border-t border-gray-800">
                    <div class="flex items-center space-x-3 px-4 py-3 text-gray-300 mb-2">
                        <div class="w-10 h-10 bg-red-600 rounded-full flex items-center justify-center text-white font-medium">AD</div>
                        <div class="flex-1">
                            <div class="font-medium text-white">Admin</div>
                            <div class="text-xs text-gray-400">Super Admin</div>
                        </div>
                    </div>

                    <form action="${pageContext.request.contextPath}/MainController" method="POST" class="w-full">
                        <input type="hidden" name="action" value="logout">
                        <button type="submit" class="w-full flex items-center space-x-3 px-4 py-3 text-gray-300 hover:bg-gray-800 rounded-lg transition-colors">
                            <svg class="w-5 h-5" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
                            <span>Đăng xuất</span>
                        </button>
                    </form>
                </div>

            </aside>

            <div class="flex-1 flex flex-col min-w-0">

                <header class="bg-white border-b border-gray-200 sticky top-0 z-30">
                    <div class="flex items-center justify-between px-4 py-3">
                        <button onclick="toggleSidebar()" class="lg:hidden text-gray-500 hover:text-gray-700">
                            <svg class="w-6 h-6" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="3" y1="12" x2="21" y2="12"></line><line x1="3" y1="6" x2="21" y2="6"></line><line x1="3" y1="18" x2="21" y2="18"></line></svg>
                        </button>

                        <div class="flex-1 lg:ml-0 ml-4">
                            <h1 class="text-xl font-semibold text-gray-900">${title}</h1>
                        </div>

                        <div class="flex items-center space-x-4">
                            <div class="text-right">
                                <p class="text-sm font-medium text-gray-900">Admin Panel</p>
                                <p class="text-xs text-gray-500">Quyền quản trị viên</p>
                            </div>
                        </div>
                    </div>
                </header>

                <main class="flex-1 overflow-auto p-6">
                    <jsp:doBody/>
                </main>

            </div>
        </div>

        <script>
            function toggleSidebar() {
                const body = document.body;
                const overlay = document.getElementById('mobile-overlay');

                if (body.classList.contains('sidebar-closed')) {
                    body.classList.remove('sidebar-closed');
                    body.classList.add('sidebar-open');
                    overlay.classList.remove('hidden');
                } else {
                    body.classList.remove('sidebar-open');
                    body.classList.add('sidebar-closed');
                    overlay.classList.add('hidden');
                }
            }
        </script>
    </body>
</html>