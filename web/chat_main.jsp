<%@page import="Model.DTO.User"%>
<%@page import="Model.DAO.UserDAO"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // 1. Kiểm tra trạng thái đăng nhập
    HttpSession userSession = request.getSession(false);
    if (userSession == null || userSession.getAttribute("userId") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    Integer userId = (Integer) userSession.getAttribute("userId");
    String username = (String) userSession.getAttribute("username");
    String role = (String) userSession.getAttribute("role");
    Integer tierId = (Integer) userSession.getAttribute("tierId");

    // Quản lý quyền
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

    // Lấy số dư ví Coin
    UserDAO dao = new UserDAO();
    int userBalance = 0;
    if (userId != null) {
        User user = dao.getUserById(userId);
        if (user != null) {
            userBalance = user.getBalance();
        }
    }
%>
<!DOCTYPE html>
<html lang="vi" class="dark">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Trợ lý AI - AI Study Hub</title>

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
            ::-webkit-scrollbar {
                width: 6px;
                height: 6px;
            }
            ::-webkit-scrollbar-track {
                background: transparent;
            }
            ::-webkit-scrollbar-thumb {
                background: #4b5563;
                border-radius: 4px;
            }
            ::-webkit-scrollbar-thumb:hover {
                background: #6b7280;
            }

            /* ÉP GIAO DIỆN TỐI TOÀN PHẦN */
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

            .chat-container {
                background-color: #111827 !important;
            }
            .chat-header {
                background-color: #1f2937 !important;
                border-color: #374151 !important;
            }
            .chat-header h2 {
                color: #ffffff !important;
            }

            /* DRAWER LỊCH SỬ CHAT TRÊN DARK MODE */
            #historyDrawer > div:nth-child(2) {
                background-color: #1f2937 !important;
                border-color: #374151 !important;
                color: #ffffff !important;
            }
            #historyDrawer h3 {
                color: #ffffff !important;
            }

            @layer components {
                .page-body {
                    @apply flex min-h-screen w-full font-sans transition-colors duration-200;
                }
                .sidebar {
                    @apply w-64 flex flex-col justify-between p-4 flex-shrink-0 min-h-screen border-r border-gray-100 shadow-sm z-10 transition-colors duration-200;
                }
                .brand-container {
                    @apply flex items-center space-x-3 px-2 py-1;
                }
                .brand-logo {
                    @apply w-9 h-9 bg-indigo-600 rounded-xl flex items-center justify-center text-white shadow-sm shadow-indigo-600/20;
                }
                .brand-text {
                    @apply font-bold text-base tracking-tight;
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
                    @apply text-sm font-bold truncate;
                }
                .user-role {
                    @apply text-[11px] text-gray-400 font-medium;
                }
                .logout-btn {
                    @apply w-full flex items-center space-x-2.5 px-2 py-2 rounded-xl text-sm font-medium text-gray-500 hover:text-red-500 hover:bg-red-50 transition-colors text-left;
                }
                .chat-container {
                    @apply flex-1 flex flex-col h-full relative transition-colors duration-200;
                }
                .chat-header {
                    @apply h-14 border-b px-6 flex items-center justify-between flex-shrink-0 transition-colors duration-200;
                }
            }
        </style>
    </head>
    <body class="page-body h-screen overflow-hidden select-none">

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
                    <a href="FileExplore.jsp" class="nav-link">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
                        <span>Khám phá tài liệu</span>
                    </a>
                    <a href="<%= request.getContextPath()%>/SessionController?action=chatMain" class="nav-link-active">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 8V4H8"/><rect width="16" height="12" x="4" y="8" rx="2"/><path d="M2 14h2"/><path d="M20 14h2"/><path d="M15 13v2"/><path d="M9 13v2"/></svg>
                        <span>AI Chatbot</span>
                    </a>
                    <a href="MainController?action=listTransactions" class="nav-link">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="20" height="14" x="2" y="5" rx="2"/><line x1="2" x2="22" y1="10" y2="10"/><path d="M16 14h2"/></svg>
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
                                <%= username != null && !username.isEmpty() ? username.substring(0, 1) : "U"%>
                            </div>
                            <div class="user-info">
                                <div class="flex items-center gap-1.5 min-w-0">
                                    <p class="user-name"><%= username != null ? username : "Khách"%></p>
                                    <% if (isPremiumUser) { %>
                                    <span class="flex-shrink-0 px-1.5 py-0.5 bg-gradient-to-r from-amber-500 to-orange-500 text-white font-bold text-[9px] rounded-full shadow-sm scale-90 origin-left">PRO</span>
                                    <% } else { %>
                                    <span class="flex-shrink-0 px-2 py-0.5 bg-emerald-100 text-emerald-800 border border-emerald-300 dark:bg-emerald-900/60 dark:text-emerald-400 dark:border-emerald-700 font-bold text-[10px] rounded-full shadow-sm scale-90 origin-left tracking-wide">FREE</span>
                                    <% }%>
                                </div>
                                <p class="user-role">Quyền: <%= role != null ? role : "Free"%></p>
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

        <div class="chat-container">
            <div class="chat-header">
                <h2 class="text-base font-bold tracking-tight">Trang chủ Chatbot</h2>
                <div class="relative cursor-pointer p-1.5 hover:bg-gray-700 rounded-full transition-colors">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-gray-300"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
                    <span class="absolute top-1 right-1 w-4 h-4 bg-red-500 text-white text-[9px] font-bold rounded-full flex items-center justify-center scale-90 border border-gray-800">3</span>
                </div>
            </div>

            <div class="flex-1 flex flex-col items-center justify-center p-6 text-center bg-[#111827]">
                <div class="w-24 h-24 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-3xl flex items-center justify-center text-white shadow-xl shadow-indigo-600/20 mb-8 transform hover:scale-105 transition-transform duration-300">
                    <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m12 3-1.912 5.886L4.2 10.8 10.088 12.714 12 18.6l1.912-5.886L19.8 10.8l-5.886-1.914Z"/></svg>
                </div>

                <h1 class="text-3xl sm:text-4xl font-extrabold text-white tracking-tight mb-4">
                    AI Study Assistant
                </h1>

                <p class="text-gray-400 max-w-lg mb-10 text-sm sm:text-base leading-relaxed">
                    Tôi là trợ lý học tập thông minh được xây dựng để giúp bạn giải đáp thắc mắc, tóm tắt tài liệu và học tập hiệu quả hơn. Bạn muốn bắt đầu từ đâu?
                </p>

                <div class="flex flex-col sm:flex-row items-center gap-4">
                    <button onclick="toggleCreateModal(true)" 
                            class="flex items-center justify-center w-full sm:w-auto space-x-2 px-7 py-3.5 bg-indigo-600 text-white rounded-xl font-semibold hover:bg-indigo-500 transition-colors shadow-lg shadow-indigo-600/20">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                        <span>Tạo cuộc trò chuyện mới</span>
                    </button>

                    <button onclick="toggleHistoryDrawer(true)" 
                            class="flex items-center justify-center w-full sm:w-auto space-x-2 px-7 py-3.5 bg-[#1f2937] border border-[#374151] text-gray-200 rounded-xl font-semibold hover:bg-[#374151] transition-colors shadow-sm">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                        <span>Xem lịch sử trò chuyện</span>
                    </button>
                </div>
            </div>
        </div>

        <div id="createSessionModal" class="fixed inset-0 z-[200] hidden bg-gray-950/60 backdrop-blur-sm flex items-center justify-center transition-opacity">
            <div class="bg-[#1f2937] border border-gray-700 rounded-2xl p-6 w-full max-w-md shadow-2xl transform transition-all">
                <h3 class="text-lg font-bold text-white mb-4">Bắt đầu cuộc trò chuyện</h3>
                <form action="<%= request.getContextPath()%>/SessionController" method="POST">
                    <input type="hidden" name="action" value="createSession">
                    <div class="mb-5">
                        <label class="block text-sm font-medium text-gray-400 mb-2">Tên chủ đề (Tùy chọn)</label>
                        <input type="text" name="sessionName" placeholder="Ví dụ: Cấu trúc dữ liệu và Giải thuật..." 
                               class="w-full bg-[#111827] border border-gray-600 text-white rounded-xl px-4 py-3 focus:outline-none focus:border-indigo-500 transition-colors">
                    </div>
                    <div class="flex justify-end gap-3">
                        <button type="button" onclick="toggleCreateModal(false)" class="px-5 py-2.5 text-sm font-medium text-gray-300 hover:text-white hover:bg-gray-700 rounded-xl transition-colors">Hủy</button>
                        <button type="submit" class="px-5 py-2.5 text-sm font-medium bg-indigo-600 text-white hover:bg-indigo-500 rounded-xl transition-colors shadow-lg shadow-indigo-600/20">Tạo mới</button>
                    </div>
                </form>
            </div>
        </div>

        <div id="historyDrawer" class="fixed inset-0 z-[150] hidden bg-gray-950/40 backdrop-blur-xs flex justify-end">
            <div onclick="toggleHistoryDrawer(false)" class="flex-1 h-full"></div>
            <div class="w-full max-w-sm h-full shadow-2xl border-l border-gray-700 flex flex-col transform translate-x-full transition-transform duration-300 ease-out p-0 bg-[#1f2937]">

                <div class="p-6 pb-4 border-b border-gray-700 flex-shrink-0">
                    <div class="flex justify-between items-center mb-4">
                        <div class="flex items-center space-x-2">
                            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-indigo-400"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                            <h3 class="text-base font-bold text-white">Lịch sử hội thoại</h3>
                        </div>
                        <button onclick="toggleHistoryDrawer(false)" class="p-1.5 text-gray-400 hover:text-red-400 hover:bg-gray-700 rounded-lg transition-colors">
                            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="18" x2="18" y2="6"/></svg>
                        </button>
                    </div>

                    <div class="relative">
                        <svg xmlns="http://www.w3.org/2000/svg" class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                        <input type="text" id="searchSessionInput" onkeyup="filterSessions()" placeholder="Tìm kiếm cuộc trò chuyện..." 
                               class="w-full bg-[#111827] border border-gray-600 text-sm text-gray-200 rounded-xl pl-9 pr-4 py-2.5 focus:outline-none focus:border-indigo-500 transition-colors">
                    </div>
                </div>

                <div class="flex-1 overflow-y-auto p-4 space-y-2" id="historyItemsContainer">
                    <c:choose>
                        <c:when test="${empty chatHistory}">
                            <p class="text-xs text-gray-500 text-center py-4">Chưa có lịch sử hội thoại</p>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="sessionItem" items="${chatHistory}">
                                <div class="session-row p-3 rounded-xl border border-transparent hover:border-gray-700 transition-all flex items-center justify-between group bg-[#111827] hover:bg-gray-800" 
                                     data-name="${sessionItem.sessionName.toLowerCase()}">

                                    <div id="display-area-${sessionItem.sessionId}" 
                                         class="flex-1 min-w-0 flex items-start space-x-3 cursor-pointer" 
                                         onclick="selectHistorySession('${sessionItem.sessionId}')">

                                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" 
                                             fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" 
                                             stroke-linejoin="round" 
                                             class="text-gray-500 mt-0.5 group-hover:text-indigo-400 flex-shrink-0">
                                        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                                        </svg>

                                        <div class="flex-1 min-w-0 pr-2">
                                            <!-- Session name — ONE instance only -->
                                            <p id="title-text-${sessionItem.sessionId}" 
                                               class="text-sm font-semibold text-gray-200 truncate mb-0.5">
                                                ${sessionItem.sessionName}
                                            </p>

                                            <!-- ADDED: Pin badge — single span, visibility controlled by ternary, no c:if needed -->
                                            <span id="pin-badge-${sessionItem.sessionId}" 
                                                  class="${sessionItem.pinned ? 'inline-flex' : 'hidden'} items-center gap-1 text-[9px] font-bold text-yellow-400 flex-shrink-0">
                                                <svg xmlns="http://www.w3.org/2000/svg" width="9" height="9" viewBox="0 0 24 24"
                                                     fill="currentColor" stroke="currentColor" stroke-width="2">
                                                <line x1="12" y1="17" x2="12" y2="22"/>
                                                <path d="M5 17h14v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1v4.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24Z"/>
                                                </svg>
                                                Đã ghim
                                            </span>

                                            <span class="text-[10px] text-gray-500 font-medium block">
                                                ${sessionItem.createdAt}
                                            </span>
                                        </div>
                                    </div>

                                    <div id="edit-area-${sessionItem.sessionId}" class="hidden flex-1 items-center space-x-2">
                                        <input type="text" id="edit-input-${sessionItem.sessionId}" value="${sessionItem.sessionName}" 
                                               class="w-full bg-[#1f2937] border border-indigo-500 text-white text-sm rounded px-2 py-1 focus:outline-none" 
                                               onblur="handleBlurEdit('${sessionItem.sessionId}')" 
                                               onkeydown="handleKeyEdit(event, '${sessionItem.sessionId}')">
                                        <button onmousedown="confirmEditSession('${sessionItem.sessionId}')" class="text-emerald-400 hover:text-emerald-300 p-1 bg-gray-700 rounded transition-colors" title="Lưu">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                                        </button>
                                    </div>

                                    <div id="action-area-${sessionItem.sessionId}" class="hidden group-hover:flex items-center space-x-1 pl-1">
                                        <button id="pin-btn-${sessionItem.sessionId}"
                                                onclick="togglePinSession('${sessionItem.sessionId}', this)"
                                                class="p-1.5 text-gray-400 hover:text-yellow-400 rounded-md hover:bg-gray-700 transition-colors"
                                                title="${sessionItem.pinned ? 'Bỏ ghim' : 'Ghim'}">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14"
                                                 viewBox="0 0 24 24" fill="${sessionItem.pinned ? 'currentColor' : 'none'}"
                                                 stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
                                                 class="${sessionItem.pinned ? 'text-yellow-400' : ''}">
                                            <line x1="12" y1="17" x2="12" y2="22"/>
                                            <path d="M5 17h14v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1v4.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24Z"/>
                                            </svg>
                                        </button>
                                        <button onclick="startEditSession('${sessionItem.sessionId}')" class="p-1.5 text-gray-400 hover:text-blue-400 rounded-md hover:bg-gray-700 transition-colors" title="Đổi tên">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"/></svg>
                                        </button>
                                        <button onclick="deleteSession('${sessionItem.sessionId}', '${sessionItem.sessionName}')" class="p-1.5 text-gray-400 hover:text-red-400 rounded-md hover:bg-gray-700 transition-colors" title="Xóa">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                                        </button>
                                    </div>

                                </div>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>

        <script>
            // --- Modal Tạo Mới ---
            function toggleCreateModal(open) {
                const modal = document.getElementById('createSessionModal');
                if (open) {
                    modal.classList.remove('hidden');
                } else {
                    modal.classList.add('hidden');
                }
            }

            // --- Điều hướng sang trang xem Chat ---
            function selectHistorySession(sessionId) {
                window.location.href = "<%= request.getContextPath()%>/SessionController?action=viewSession&sessionId=" + sessionId;
            }

            // --- Ẩn/Hiện Lịch Sử ---
            function toggleHistoryDrawer(open) {
                const drawer = document.getElementById('historyDrawer');
                const innerContent = drawer.querySelector('div:nth-child(2)');

                if (open) {
                    drawer.classList.remove('hidden');
                    setTimeout(() => {
                        innerContent.classList.remove('translate-x-full');
                    }, 10);
                } else {
                    innerContent.classList.add('translate-x-full');
                    setTimeout(() => {
                        drawer.classList.add('hidden');
                    }, 300);
                }
            }

            // --- Lọc / Tìm kiếm Lịch Sử ---
            function filterSessions() {
                const input = document.getElementById("searchSessionInput").value.toLowerCase();
                const rows = document.querySelectorAll(".session-row");

                rows.forEach(row => {
                    const sessionName = row.getAttribute("data-name");
                    if (sessionName.includes(input)) {
                        row.classList.remove("hidden");
                        row.classList.add("flex");
                    } else {
                        row.classList.remove("flex");
                        row.classList.add("hidden");
                    }
                });
            }

            // --- Logic Cập Nhật Tên (Inline Edit) ---
            function startEditSession(id) {
                document.getElementById('display-area-' + id).classList.add('hidden');
                document.getElementById('action-area-' + id).classList.remove('group-hover:flex');
                document.getElementById('action-area-' + id).classList.add('hidden');

                const editArea = document.getElementById('edit-area-' + id);
                editArea.classList.remove('hidden');
                editArea.classList.add('flex');

                const input = document.getElementById('edit-input-' + id);
                input.focus();
                // Đưa con trỏ xuống cuối chữ
                input.setSelectionRange(input.value.length, input.value.length);
            }

            function cancelEditSession(id) {
                document.getElementById('edit-area-' + id).classList.add('hidden');
                document.getElementById('edit-area-' + id).classList.remove('flex');

                document.getElementById('display-area-' + id).classList.remove('hidden');
                document.getElementById('action-area-' + id).classList.remove('hidden');
                document.getElementById('action-area-' + id).classList.add('group-hover:flex');
            }

            function handleBlurEdit(id) {
                // Timeout nhỏ để nút Confirm có thể nhận sự kiện OnMouseDown trước khi Blur kích hoạt
                setTimeout(() => {
                    cancelEditSession(id);
                }, 150);
            }

            function handleKeyEdit(event, id) {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    confirmEditSession(id);
                } else if (event.key === 'Escape') {
                    cancelEditSession(id);
                }
            }

            function confirmEditSession(id) {
                const newName = document.getElementById('edit-input-' + id).value.trim();
                if (newName === "") {
                    cancelEditSession(id);
                    return;
                }

                const url = "<%= request.getContextPath()%>/SessionController?action=updateSessionName";
                const formData = new URLSearchParams();
                formData.append('sessionId', id);
                formData.append('sessionNewName', newName);

                fetch(url, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                    body: formData.toString()
                })
                        .then(response => response.json())
                        .then(data => {
                            if (data.success) {
                                // Cập nhật text UI
                                document.getElementById('title-text-' + id).textContent = newName;
                                // Cập nhật data-name cho chức năng search
                                document.getElementById('display-area-' + id).closest('.session-row').setAttribute('data-name', newName.toLowerCase());
                            } else {
                                alert("Lỗi: " + data.message);
                            }
                            cancelEditSession(id);
                        })
                        .catch(err => {
                            console.error("Edit Error:", err);
                            cancelEditSession(id);
                        });
            }

            // --- Logic Xóa Session ---
            function deleteSession(id, name) {
                if (confirm("Tất cả dữ liệu của cuộc trò chuyện '" + name + "' sẽ bị xóa vĩnh viễn. Bạn có chắc chắn không?")) {
                    const url = "<%= request.getContextPath()%>/SessionController?action=deleteSession";
                    const formData = new URLSearchParams();
                    formData.append('sessionId', id);
                    formData.append('sessionName', name);

                    fetch(url, {
                        method: 'POST',
                        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                        body: formData.toString()
                    })
                            .then(response => response.json())
                            .then(data => {
                                if (data.success) {
                                    // Tải lại trang để đồng bộ
                                    window.location.reload();
                                } else {
                                    alert("Có lỗi xảy ra, không thể xóa cuộc trò chuyện.");
                                }
                            })
                            .catch(err => console.error("Delete Error:", err));
                }
            }
            // ADDED: Toggle pin/unpin a chat session
            function togglePinSession(id, btnEl) {
                // Read current pin state from the button's icon fill attribute
                const svgEl = btnEl.querySelector('svg');
                const isPinned = svgEl.getAttribute('fill') === 'currentColor'; // currently pinned
                const newState = !isPinned; // toggle to opposite

                const url = "<%= request.getContextPath()%>/SessionController?action=pinSession";
                const formData = new URLSearchParams();
                formData.append('sessionId', id);
                formData.append('isPinned', newState);

                fetch(url, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                    body: formData.toString()
                })
                        .then(response => response.json())
                        .then(data => {
                            if (data.success) {
                                // --- Update pin button icon (filled = pinned, outline = unpinned) ---
                                svgEl.setAttribute('fill', newState ? 'currentColor' : 'none');
                                btnEl.setAttribute('title', newState ? 'Bỏ ghim' : 'Ghim');
                                if (newState) {
                                    svgEl.classList.add('text-yellow-400');
                                } else {
                                    svgEl.classList.remove('text-yellow-400');
                                }

                                // --- Show/hide the "Đã ghim" badge in the title area ---
                                const badge = document.getElementById('pin-badge-' + id);
                                if (badge) {
                                    if (newState) {
                                        badge.classList.remove('hidden');
                                    } else {
                                        badge.classList.add('hidden');
                                    }
                                }

                                // --- Move the session row to top (pinned) or re-sort (unpinned) ---
                                const row = btnEl.closest('.session-row');
                                const container = document.getElementById('historyItemsContainer');
                                if (newState) {
                                    // Pinned: move to the very top of the list
                                    container.prepend(row);
                                } else {
                                    // Unpinned: move below all other pinned rows
                                    const pinnedRows = container.querySelectorAll(
                                            '.session-row svg[fill="currentColor"]'
                                            );
                                    const lastPinned = pinnedRows.length > 0
                                            ? pinnedRows[pinnedRows.length - 1].closest('.session-row')
                                            : null;

                                    if (lastPinned && lastPinned !== row) {
                                        lastPinned.after(row);
                                    } else {
                                        container.prepend(row);
                                    }
                                }
                            } else {
                                alert("Lỗi: " + (data.message || "Không thể cập nhật trạng thái ghim."));
                            }
                        })
                        .catch(err => console.error("Pin Error:", err));
            }
        </script>
    </body>
</html>