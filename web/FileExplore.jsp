<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="Model.DTO.Document" %>
<%@ page import="Model.DTO.Folder" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);
%>
<%!
    private String escapeJs(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("'", "\\'")
                .replace("\r\n", " ")
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("</", "<\\/");
    }
%>
<%
    HttpSession userSession = request.getSession(false);
    if (userSession == null || userSession.getAttribute("userId") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    int userId = (Integer) userSession.getAttribute("userId");
    String username = (String) userSession.getAttribute("username");
    String role = (String) userSession.getAttribute("role");
    Integer tierId = (Integer) userSession.getAttribute("tierId");

    if (role == null || !"ADMIN".equalsIgnoreCase(role.trim())) { role = "STUDENT"; } else { role = "ADMIN"; }
    if (tierId == null || tierId < 2) { tierId = 2; }
    boolean isPremiumUser = (tierId >= 3);

    Integer userBalance = (Integer) userSession.getAttribute("balance");
    if (userBalance == null) { userBalance = 0; }

    List<Document> publicDocuments = (List<Document>) request.getAttribute("publicDocuments");
    if (publicDocuments == null) {
        response.sendRedirect(request.getContextPath() + "/MainController?action=explore");
        return;
    }
    
    Boolean isFriendsViewObj = (Boolean) request.getAttribute("isFriendsView");
    boolean isFriendsView = (isFriendsViewObj != null) ? isFriendsViewObj : false;

    Integer totalDocs = (Integer) request.getAttribute("realTotalDocs");
    Integer totalContributors = (Integer) request.getAttribute("realTotalContributors");
    Integer totalDownloads = (Integer) request.getAttribute("realTotalDownloads");
    
    String currentSearchQuery = (String) request.getAttribute("searchQuery");
    String currentSortBy = (String) request.getAttribute("sortBy");
    if (currentSortBy == null) currentSortBy = "date";
%>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Khám phá tài liệu - AI Study Hub</title>
    
    <script src="https://cdn.tailwindcss.com"></script>
    <script> tailwind.config = { darkMode: 'class' } </script>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    
    <style type="text/tailwindcss">
        html.dark .page-body { background-color: #111827; color: #f3f4f6; }
        html.dark .sidebar { background-color: #1f2937; border-color: #374151; }
        html.dark .brand-text { color: #ffffff; }
        html.dark .nav-link { color: #d1d5db; }
        html.dark .nav-link:hover { background-color: #374151; }
        html.dark .nav-link-active { background-color: rgba(49, 46, 129, 0.6); color: #818cf8; }
        html.dark .user-name { color: #ffffff; }
        html.dark .user-profile-link:hover { background-color: #374151; }
        html.dark .logout-btn { color: #9ca3af; }
        html.dark .logout-btn:hover { background-color: rgba(127, 29, 29, 0.3); color: #f87171; }
        html.dark .btn-secondary { background-color: #1f2937; border-color: #374151; color: #e5e7eb; }
        html.dark .btn-secondary:hover { background-color: #374151; }
        html.dark .doc-card { background-color: #1f2937; border-color: #374151; }
        html.dark .doc-title { color: #ffffff; }
        html.dark .doc-desc { color: #9ca3af; }
        html.dark .author-box { background-color: rgba(55, 65, 81, 0.4); border-color: #374151; }
        html.dark .author-name { color: #f3f4f6; }
        html.dark .search-bar { background-color: #1f2937; border-color: #374151; color: #ffffff; }
        html.dark .search-bar:focus { border-color: #5c3cf5; }
        html.dark .stat-card { background-color: #1f2937; border-color: #374151; }

        @layer components {
            .page-body { @apply flex min-h-screen w-full text-gray-800 bg-[#f8f9fa] font-sans transition-colors duration-200; }
            .sidebar { @apply w-64 bg-white border-r border-gray-100 flex flex-col justify-between p-4 flex-shrink-0 min-h-screen shadow-sm z-10 transition-colors duration-200; }
            .brand-container { @apply flex items-center space-x-3 px-2 py-1; }
            .brand-logo { @apply w-9 h-9 bg-indigo-600 rounded-xl flex items-center justify-center text-white shadow-sm shadow-indigo-600/20; }
            .brand-text { @apply font-bold text-gray-900 text-base tracking-tight; }
            .nav-link { @apply flex items-center space-x-3 px-4 py-2.5 text-gray-600 hover:bg-gray-50 rounded-xl font-medium text-sm transition-all w-full text-left; }
            .nav-link-active { @apply flex items-center space-x-3 px-4 py-2.5 bg-indigo-50 text-indigo-600 rounded-xl font-semibold text-sm transition-colors w-full text-left; }
            .wallet-widget { @apply w-full bg-gradient-to-br from-purple-500 to-indigo-600 text-white p-4 rounded-2xl shadow-md shadow-indigo-600/10 relative overflow-hidden; }
            .wallet-header { @apply flex justify-between items-center opacity-85; }
            .wallet-title { @apply text-xs font-medium tracking-wide; }
            .wallet-balance { @apply text-xl font-bold mt-2 tracking-tight; }
            .user-area { @apply pt-2 border-t border-gray-100 flex flex-col gap-1; }
            .user-profile-link { @apply flex items-center space-x-3 px-2 py-2 rounded-xl hover:bg-gray-50 transition-colors cursor-pointer; }
            .user-avatar { @apply w-8 h-8 rounded-full bg-indigo-100 flex items-center justify-center text-indigo-700 flex-shrink-0 font-bold text-xs uppercase; }
            .user-info { @apply flex-1 min-w-0; }
            .user-name { @apply text-sm font-bold text-gray-900 truncate; }
            .user-role { @apply text-[11px] text-gray-400 font-medium; }
            .logout-btn { @apply w-full flex items-center space-x-2.5 px-2 py-2 rounded-xl text-sm font-medium text-gray-500 hover:text-red-500 hover:bg-red-50 transition-colors text-left; }
            .main-content { @apply flex-1 p-8 overflow-y-auto h-screen relative; }
            .header-container { @apply flex justify-between items-center mb-6; }
            .page-title { @apply text-2xl font-bold text-gray-900 tracking-tight; }
            .btn-primary { @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-[#5c3cf5] text-white rounded-xl font-semibold hover:bg-indigo-700 transition-colors text-sm shadow-sm shadow-indigo-100 cursor-pointer; }
            .btn-secondary { @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-white border border-gray-200 text-gray-700 rounded-xl font-semibold hover:bg-gray-50 transition-colors text-sm shadow-sm cursor-pointer; }
            .doc-card { @apply bg-white border border-gray-100 rounded-2xl p-5 hover:shadow-md transition-all flex flex-col justify-between; }
        }
    </style>
</head>
<body class="page-body" onclick="closeMenuOnClickOutside(event)">

    <aside class="sidebar">
        <div class="space-y-6 w-full">
            <div class="brand-container">
                <div class="brand-logo">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21.42 10.922a1 1 0 0 0-.019-1.838L12.83 5.18a2 2 0 0 0-1.66 0L2.6 9.08a1 1 0 0 0 0 1.832l8.57 3.908a2 2 0 0 0 1.66 0z"/><path d="M6 18.8V13.5"/><path d="M18 13.5v5.3a2.1 2.1 0 0 1-2 2h-8a2.1 2.1 0 0 1-2-2v-5.3"/></svg>
                </div>
                <span class="brand-text">AI Study Hub</span>
            </div>

            html.dark .doc-card {
                background-color: #1f2937;
                border-color: #374151;
            }
            html.dark .doc-title {
                color: #ffffff;
            }
            html.dark .doc-desc {
                color: #9ca3af;
            }
            html.dark .author-box {
                background-color: rgba(55, 65, 81, 0.4);
                border-color: #374151;
            }
            html.dark .author-name {
                color: #f3f4f6;
            }
            html.dark .search-bar {
                background-color: #1f2937;
                border-color: #374151;
                color: #ffffff;
            }
            html.dark .search-bar:focus {
                border-color: #5c3cf5;
            }
            html.dark .stat-card {
                background-color: #1f2937;
                border-color: #374151;
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
                .btn-primary {
                    @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-[#5c3cf5] text-white rounded-xl font-semibold hover:bg-indigo-700 transition-colors text-sm shadow-sm shadow-indigo-100 cursor-pointer;
                }
                .btn-secondary {
                    @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-white border border-gray-200 text-gray-700 rounded-xl font-semibold hover:bg-gray-50 transition-colors text-sm shadow-sm cursor-pointer;
                }

                .doc-card {
                    @apply bg-white border border-gray-100 rounded-2xl p-5 hover:shadow-md transition-all flex flex-col justify-between;
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
                    <a href="<%= request.getContextPath()%>/user_dashboard.jsp" class="nav-link">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z"/></svg>
                        <span>Tài liệu của tôi</span>
                    </a>
                    <a href="<%= request.getContextPath()%>/MainController?action=explore" class="nav-link-active">
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
                                    <p class="user-name"><%= username != null && !username.trim().isEmpty() ? username : "Học viên"%></p>
                                    <% if (isPremiumUser) { %>
                                    <span class="flex-shrink-0 px-1.5 py-0.5 bg-gradient-to-r from-amber-500 to-orange-500 text-white font-bold text-[9px] rounded-full shadow-sm scale-90 origin-left">PRO</span>
                                    <% } else { %>
                                    <span class="flex-shrink-0 px-2 py-0.5 bg-emerald-100 text-emerald-800 border border-emerald-300 dark:bg-emerald-900/60 dark:text-emerald-400 dark:border-emerald-700 font-bold text-[10px] rounded-full shadow-sm scale-90 origin-left tracking-wide">FREE</span>
                                    <% }%>
                                </div>
                                <p class="user-role">Quyền: <%= role%></p>
                            </div>
                        </a>

                        <a href="<%= request.getContextPath()%>/MainController?action=friendList" 
                           class="p-2 text-gray-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-xl transition-colors dark:hover:text-indigo-400 dark:hover:bg-indigo-950/40 flex-shrink-0" 
                           title="Danh sách bạn bè">
                            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/>
                            <circle cx="9" cy="7" r="4"/>
                            <path d="M22 21v-2a4 4 0 0 0-3-3.87"/>
                            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                            </svg>
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
            <div class="header-container">
                <div>
                    <h1 class="page-title dark:text-white mb-1">
                        <%= isFriendsView ? "Tài liệu bạn bè chia sẻ" : "Khám phá tài liệu cộng đồng"%>
                    </h1>
                    <p class="text-sm text-gray-500 font-medium">Tìm kiếm tài liệu và đề cương ôn tập được đóng góp bởi sinh viên</p>
                </div>

                <a href="<%= request.getContextPath()%>/MainController?action=explore&view=<%= isFriendsView ? "public" : "friends"%>" 
                   class="btn-primary bg-gradient-to-r <%= isFriendsView ? "from-emerald-500 to-teal-500" : "from-indigo-600 to-purple-600"%>">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mr-2">
                    <%= isFriendsView ? "<path d='M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2'></path><circle cx='9' cy='7' r='4'></circle><path d='M23 21v-2a4 4 0 0 0-3-3.87'></path><path d='M16 3.13a4 4 0 0 1 0 7.75'></path>"
                            : "<path d='M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z'></path>"%>
                    </svg>
                    <%= isFriendsView ? "Xem tài liệu công khai" : "Xem tài liệu bạn bè chia sẻ"%>
                </a>
            </div>
        </div>
    </aside>

    <main class="main-content">
        <div class="header-container">
            <div>
                <h1 class="page-title dark:text-white mb-1">
                    <%= isFriendsView ? "Tài liệu bạn bè chia sẻ" : "Khám phá tài liệu cộng đồng" %>
                </h1>
                <p class="text-sm text-gray-500 font-medium">Tìm kiếm tài liệu và đề cương ôn tập được đóng góp bởi sinh viên</p>
            </div>
            
            <a href="<%= request.getContextPath()%>/MainController?action=explore&view=<%= isFriendsView ? "public" : "friends" %>" 
               class="btn-primary bg-gradient-to-r <%= isFriendsView ? "from-emerald-500 to-teal-500" : "from-indigo-600 to-purple-600" %>">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mr-2">
                    <%= isFriendsView ? "<path d='M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2'></path><circle cx='9' cy='7' r='4'></circle><path d='M23 21v-2a4 4 0 0 0-3-3.87'></path><path d='M16 3.13a4 4 0 0 1 0 7.75'></path>" 
                                     : "<path d='M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z'></path>" %>
                </svg>
                <%= isFriendsView ? "Xem tài liệu công khai" : "Xem tài liệu bạn bè chia sẻ" %>
            </a>
        </div>

        <div class="mb-6 relative">
            <form action="<%= request.getContextPath()%>/MainController" method="GET" class="relative">
                <input type="hidden" name="action" value="explore">
                <input type="hidden" name="view" value="<%= isFriendsView ? "friends" : "public" %>">
                <input type="hidden" name="sort" id="sort-input" value="<%= escapeJs(currentSortBy) %>">
                
                <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
                </span>
                
                <input type="text" name="query" value="<%= currentSearchQuery != null ? escapeJs(currentSearchQuery) : "" %>" placeholder="Nhập từ khóa và nhấn Enter để tìm..." class="search-bar w-full pl-9 pr-14 py-2.5 border border-gray-200 rounded-xl text-sm focus:outline-none shadow-sm transition-all">
                
                <button type="button" onclick="toggleSortMenu(event)" class="absolute right-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center justify-center text-gray-500 transition-colors" title="Sắp xếp tài liệu">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4h13M3 8h9m-9 4h6m4 0l4-4m0 0L17 4m4 4h-14"></path></svg>
                </button>
                
                <div id="sort-menu" class="hidden absolute right-0 top-12 w-56 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl shadow-lg z-50 overflow-hidden">
                    <div class="px-4 py-2 bg-gray-50 dark:bg-gray-900 border-b border-gray-100 dark:border-gray-700 text-xs font-bold text-gray-500 uppercase tracking-wider">
                        Sắp xếp theo
                    </div>
                    <ul class="text-sm text-gray-700 dark:text-gray-300">
                        <li onclick="submitSort('date')" class="px-4 py-2.5 hover:bg-indigo-50 hover:text-indigo-600 dark:hover:bg-gray-700 cursor-pointer flex items-center justify-between transition-colors <%= "date".equals(currentSortBy) ? "bg-indigo-50 text-indigo-600 font-semibold" : "" %>">
                            <span>Ngày cập nhật</span>
                            <%= "date".equals(currentSortBy) ? "<svg class='w-4 h-4' fill='none' stroke='currentColor' viewBox='0 0 24 24'><path stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M5 13l4 4L19 7'></path></svg>" : "" %>
                        </li>
                        <li onclick="submitSort('downloads')" class="px-4 py-2.5 hover:bg-indigo-50 hover:text-indigo-600 dark:hover:bg-gray-700 cursor-pointer flex items-center justify-between transition-colors <%= "downloads".equals(currentSortBy) ? "bg-indigo-50 text-indigo-600 font-semibold" : "" %>">
                            <span>Nhiều lượt tải nhất</span>
                            <%= "downloads".equals(currentSortBy) ? "<svg class='w-4 h-4' fill='none' stroke='currentColor' viewBox='0 0 24 24'><path stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M5 13l4 4L19 7'></path></svg>" : "" %>
                        </li>
                        <li onclick="submitSort('bookmarks')" class="px-4 py-2.5 hover:bg-indigo-50 hover:text-indigo-600 dark:hover:bg-gray-700 cursor-pointer flex items-center justify-between transition-colors <%= "bookmarks".equals(currentSortBy) ? "bg-indigo-50 text-indigo-600 font-semibold" : "" %>">
                            <span>Nhiều Bookmark nhất</span>
                            <%= "bookmarks".equals(currentSortBy) ? "<svg class='w-4 h-4' fill='none' stroke='currentColor' viewBox='0 0 24 24'><path stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M5 13l4 4L19 7'></path></svg>" : "" %>
                        </li>
                    </ul>
                </div>
            </form>
        </div>

            <div class="mb-6">
                <div class="relative">
                    <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"><svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg></span>
                    <input type="text" id="search-input" placeholder="Tìm nhanh theo tiêu đề file, môn học hoặc tên người đăng..." class="search-bar w-full pl-9 pr-4 py-2.5 border border-gray-200 rounded-xl text-sm focus:outline-none shadow-sm transition-all">
                </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                <div class="stat-card border bg-white rounded-xl p-4 dark:border-gray-700 shadow-sm flex items-center justify-between">
                    <div><p class="text-gray-400 text-xs font-medium">Tổng tài liệu</p><p class="text-2xl font-bold mt-1.5 tracking-tight text-blue-500"><%= totalDocs != null ? String.format("%,d", totalDocs) : "0"%> file</p></div>
                    <div class="w-12 h-12 bg-blue-100 dark:bg-blue-950/40 rounded-xl flex items-center justify-center"><svg class="w-6 h-6 text-blue-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"></path></svg></div>
                </div>
                <div class="stat-card border bg-white rounded-xl p-4 dark:border-gray-700 shadow-sm flex items-center justify-between">
                    <div><p class="text-gray-400 text-xs font-medium">Người đóng góp</p><p class="text-2xl font-bold mt-1.5 tracking-tight text-purple-500"><%= totalContributors != null ? String.format("%,d", totalContributors) : "0"%> thành viên</p></div>
                    <div class="w-12 h-12 bg-purple-100 dark:bg-purple-950/40 rounded-xl flex items-center justify-center"><svg class="w-6 h-6 text-purple-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path></svg></div>
                </div>
                <div class="stat-card border bg-white rounded-xl p-4 dark:border-gray-700 shadow-sm flex items-center justify-between">
                    <div><p class="text-gray-400 text-xs font-medium">Lượt tải hệ thống</p><p id="total-downloads-stat" class="text-2xl font-bold mt-1.5 tracking-tight text-emerald-500"><%= totalDownloads != null ? String.format("%,d", totalDownloads) : "0"%> lượt</p></div>
                    <div class="w-12 h-12 bg-emerald-100 dark:bg-emerald-950/40 rounded-xl flex items-center justify-center"><svg class="w-6 h-6 text-emerald-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg></div>
                </div>
            </div>

            <div id="documents-grid" class="grid grid-cols-1 lg:grid-cols-2 gap-5"></div>

        <div class="mt-8 text-center">
            <button id="load-more-btn" onclick="handleLoadMore()" class="btn-secondary px-5 py-2.5 mx-auto hidden">Xem thêm tài liệu</button>
        </div>
    </main>

    <script>
        // Mở menu Dropdown Sort
        function toggleSortMenu(event) {
            event.stopPropagation();
            document.getElementById('sort-menu').classList.toggle('hidden');
        }

        // Bấm ra ngoài là tắt Menu Sort
        function closeMenuOnClickOutside(event) {
            const menu = document.getElementById('sort-menu');
            if (!menu.classList.contains('hidden') && !menu.contains(event.target)) {
                menu.classList.add('hidden');
            }
        }

        // Xử lý khi nhấn vô nút đổi tiêu chí Sort
        function submitSort(sortValue) {
            document.getElementById('sort-input').value = sortValue;
            document.getElementById('sort-input').form.submit();
        }

        let itemsShown = 6; 

        // Khởi tạo mảng dữ liệu với thuộc tính isBookmarked, isFlagged, và Ngày Upload
        const internalDocs = [
            <%
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                if (publicDocuments != null && !publicDocuments.isEmpty()) {
                    for (int i = 0; i < publicDocuments.size(); i++) {
                        Document doc = publicDocuments.get(i);
                        String title = escapeJs(doc.getTitle());
                        String fileExt = doc.getFileExtension() != null ? doc.getFileExtension().toUpperCase() : "FILE";
                        String authorName = escapeJs(
                                (doc.getAuthorUsername() != null && !doc.getAuthorUsername().trim().isEmpty())
                                ? doc.getAuthorUsername()
                                : "Người dùng #" + doc.getUserId()
                        );
                        
                        // Lấy Formatted Date cho Ngày Upload
                        String uploadDateStr = (doc.getCreatedAt() != null) ? doc.getCreatedAt().format(formatter) : "Gần đây";
            %>
                { 
                    id: "<%= doc.getDocumentId() %>", 
                    title: "<%= title %>", 
                    author: "<%= authorName %>", 
                    authorAvatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=user<%= doc.getUserId() %>", 
                    fileType: "<%= fileExt %>",
                    downloads: <%= doc.getDownloadCount() != null ? doc.getDownloadCount() : 0 %>, 
                    bookmarks: <%= doc.getBookmarkCount() != null ? doc.getBookmarkCount() : 0 %>,
                    isBookmarked: <%= doc.isIsBookmarked() %>,
                    isFlagged: <%= doc.isFlagged() %>, 
                    size: "<%= doc.getFileSizeMb() %> MB", 
                    uploadDate: "<%= uploadDateStr %>", // <-- Dữ liệu Ngày Upload MỚI
                    description: "Tài liệu học tập được chia sẻ bởi cộng đồng sinh viên." 
                }<%= (i < publicDocuments.size() - 1) ? "," : "" %>
            <%
                    }
                }
            %>
        ];

        // Hàm vẽ HTML UI. Đã lược bỏ logic Filter/Sort vì Backend đã làm xong!
        function renderDocuments() {
            const grid = document.getElementById('documents-grid');
            const loadMoreBtn = document.getElementById('load-more-btn');

            const slicedDocs = internalDocs.slice(0, itemsShown);

            if (internalDocs.length === 0) {
                grid.innerHTML = `
                <div class="col-span-full flex flex-col items-center justify-center py-16 bg-white border border-gray-100 rounded-2xl shadow-sm dark:bg-gray-800 dark:border-gray-700">
                    <p class="text-gray-500 dark:text-gray-400 font-medium text-sm">Không tìm thấy tài liệu nào phù hợp.</p>
                </div>`;
                loadMoreBtn.classList.add('hidden');
                return;
            }

            if (itemsShown >= internalDocs.length) {
                loadMoreBtn.classList.add('hidden');
            } else {
                loadMoreBtn.classList.remove('hidden');
            }

            grid.innerHTML = slicedDocs.map(doc => {
                if (doc.isFlagged) {
                    return `
                    <div class="doc-card border border-red-200 bg-red-50/40 dark:bg-red-900/10 dark:border-red-900/30 shadow-sm transition-all duration-200 relative opacity-80 cursor-not-allowed">
                        <div class="absolute top-4 right-4 text-red-500" title="Tài liệu đã bị cấm do vi phạm">
                            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="currentColor" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"></path>
                                <line x1="4" y1="22" x2="4" y2="15"></line>
                            </svg>
                        </div>
                        <div>
                            <div class="flex items-start justify-between mb-2 pr-8">
                                <h3 class="doc-title font-bold text-[15px] leading-snug text-gray-400 dark:text-gray-500 line-through">\${doc.title}</h3>
                            </div>
                            <p class="doc-desc text-xs font-semibold text-red-500 mb-4">Tài liệu đã bị cấm do có nhiều báo cáo vi phạm.</p>
                        </div>
                        <div>
                            <div class="author-box flex items-center mb-1 p-2 rounded-xl border border-red-100 dark:border-red-900/20 bg-white/50 dark:bg-gray-800/50">
                                <img src="\${doc.authorAvatar}" class="w-7 h-7 rounded-full mr-2 border bg-white dark:border-gray-600 grayscale opacity-70">
                                <div class="flex-1 min-w-0">
                                    <p class="author-name text-xs font-bold truncate text-gray-400">Người đăng: \${doc.author}</p>
                                </div>
                            </div>
                        </div>
                    </div>`;
                } else {
                    return `
                    <div class="doc-card border shadow-sm transition-all duration-200 relative">
                        <div>
                            <div class="flex items-start justify-between mb-2 pr-6">
                                <h3 class="doc-title font-bold text-[15px] leading-snug">\${doc.title}</h3>
                                <span class="ml-2 flex-shrink-0 px-2 py-0.5 bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300 font-mono text-[10px] font-bold rounded">\${doc.fileType}</span>
                            </div>
                            <p class="doc-desc text-xs font-medium line-clamp-2 leading-relaxed mb-4">\${doc.description}</p>
                        </div>
                        <div>
                            <div class="author-box flex items-center mb-3 p-2 rounded-xl border">
                                <img src="\${doc.authorAvatar}" class="w-7 h-7 rounded-full mr-2 border bg-white dark:border-gray-600">
                                <div class="flex-1 min-w-0">
                                    <p class="author-name text-xs font-bold truncate">Người đăng: \${doc.author}</p>
                                </div>
                            </div>
                            <div class="flex items-center justify-between pt-3 border-t border-gray-100 dark:border-gray-700">
                                <div class="flex flex-wrap items-center gap-3 text-[11px] font-semibold text-gray-400">
                                    
                                    <button onclick="toggleBookmark('\${doc.id}')" class="flex items-center space-x-1 \${doc.isBookmarked ? 'text-amber-500' : 'hover:text-amber-500'} transition-all cursor-pointer" title="\${doc.isBookmarked ? 'Bỏ lưu' : 'Lưu tài liệu'}">
                                        <svg class="w-4 h-4" fill="\${doc.isBookmarked ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" viewBox="0 0 24 24">
                                            <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path>
                                        </svg>
                                        <span>\${doc.bookmarks}</span>
                                    </button>
                                    
                                    <div class="flex items-center" title="Lượt tải xuống"><svg class="w-3.5 h-3.5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>\${doc.downloads}</div>
                                    
                                    <button onclick="handleReport('\${doc.id}')" class="flex items-center text-gray-400 hover:text-red-500 dark:hover:text-red-400 transition-colors cursor-pointer" title="Báo cáo tài liệu này">
                                        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" viewBox="0 0 24 24">
                                            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                                            <line x1="12" y1="9" x2="12" y2="13"></line>
                                            <line x1="12" y1="17" x2="12.01" y2="17"></line>
                                        </svg>
                                    </button>
                                    
                                    <div title="Dung lượng"><span>\${doc.size}</span></div>
                                    <div class="flex items-center text-gray-400 border-l border-gray-200 dark:border-gray-600 pl-3" title="Ngày Upload">
                                        <svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
                                        <span>\${doc.uploadDate}</span>
                                    </div>
                                </div>
                                <button onclick="handleDownload('\${doc.id}')" class="flex-shrink-0 flex items-center space-x-2 px-4 py-2 bg-[#5c3cf5] text-white rounded-xl hover:bg-indigo-700 transition-all text-xs font-bold shadow-sm cursor-pointer ml-2">
                                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                                    <span>Tải</span>
                                </button>
                            </div>
                            <div class="flex items-center space-x-2">
                                <a href="<%= request.getContextPath()%>/MainController?action=viewPublicPage&docId=\${doc.id}" target="_blank" class="flex items-center space-x-2 px-4 py-2 bg-white text-gray-700 border border-gray-200 rounded-xl hover:bg-gray-50 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-700 transition-all text-xs font-bold shadow-sm cursor-pointer">
                                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path><path stroke-linecap="round" stroke-linejoin="round" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path></svg>
                                    <span>Xem</span>
                                </a>
                                <button onclick="handleDownload('\${doc.id}')" class="flex items-center space-x-2 px-4 py-2 bg-[#5c3cf5] text-white rounded-xl hover:bg-indigo-700 transition-all text-xs font-bold shadow-sm cursor-pointer">
                                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                                    <span>Tải</span>
                                </button>
                            </div>
                        </div>
                    </div>`;
                }
            }).join('');
        }

        function handleReport(docId) {
            window.location.href = "<%= request.getContextPath()%>/MainController?action=report&documentId=" + docId;
        }

        function toggleBookmark(docId) {
            fetch(`<%= request.getContextPath()%>/DocumentController?action=toggleBookmark&docId=\${docId}`, { method: 'POST' })
            .then(response => response.json())
            .then(data => {
                const docIndex = internalDocs.findIndex(d => d.id == docId);
                if (docIndex !== -1) {
                    internalDocs[docIndex].isBookmarked = data.isBookmarked;
                    internalDocs[docIndex].bookmarks = data.newCount;
                    renderDocuments();
                }

                const totalStatEl = document.getElementById('total-downloads-stat');
                if (totalStatEl) {
                    let currentTotal = parseInt(totalStatEl.innerText.replace(/,/g, '')) || 0;
                    totalStatEl.innerText = (currentTotal + 1).toLocaleString() + " lượt";
                }

                window.location.href = "<%= request.getContextPath()%>/MainController?action=downloadDoc&docId=" + docId;
            }

            window.location.href = "<%= request.getContextPath()%>/MainController?action=downloadDoc&docId=" + docId;
        }
        
        document.addEventListener("DOMContentLoaded", () => { 
            renderDocuments(); 
        });
    </script>
</body>
</html>