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
        <title>${currentChat.sessionName} - AI Study Hub</title>

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
            html.dark .btn-secondary {
                background-color: #1f2937;
                border-color: #374151;
                color: #e5e7eb;
            }
            html.dark .btn-secondary:hover {
                background-color: #374151;
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
            .chat-sub-header {
                background-color: #1f2937 !important;
                border-color: #374151 !important;
            }
            .chat-sub-header h3 {
                color: #ffffff !important;
            }
            .chat-bg-area {
                background-color: #111827 !important;
            }
            .bot-msg-box {
                background-color: #1f2937 !important;
                border-color: #374151 !important;
                color: #e5e7eb !important;
            }

            .chat-footer-input {
                background-color: #1f2937 !important;
                border-t-color: #374151 !important;
            }
            .chat-input-wrapper {
                background-color: #111827 !important;
                border-color: #374151 !important;
                color: #ffffff !important;
            }
            .chat-input-wrapper input {
                background-color: transparent !important;
                color: #ffffff !important;
            }
            .chat-footer-bar {
                background-color: #1f2937 !important;
                border-t-color: #374151 !important;
            }

            .suggest-btn-1 {
                background-color: rgba(30, 58, 138, 0.4) !important;
                border-color: rgba(59, 130, 246, 0.3) !important;
                color: #60a5fa !important;
            }
            .suggest-btn-2 {
                background-color: rgba(120, 53, 4, 0.4) !important;
                border-color: rgba(245, 158, 11, 0.3) !important;
                color: #fbbf24 !important;
            }
            .suggest-btn-3 {
                background-color: rgba(6, 78, 59, 0.4) !important;
                border-color: rgba(16, 185, 129, 0.3) !important;
                color: #34d399 !important;
            }
            .suggest-btn-4 {
                background-color: rgba(88, 28, 135, 0.4) !important;
                border-color: rgba(139, 92, 246, 0.3) !important;
                color: #c084fc !important;
            }

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
                .chat-bg-area {
                    @apply flex-1 overflow-y-auto p-6 space-y-6 transition-colors duration-200;
                }

                .btn-secondary {
                    @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-white border border-gray-200 text-gray-700 rounded-xl font-semibold hover:bg-gray-50 transition-colors text-sm shadow-sm cursor-pointer;
                }
                .suggest-btn-1, .suggest-btn-2, .suggest-btn-3, .suggest-btn-4 {
                    @apply flex items-center space-x-3 p-3.5 rounded-xl border hover:opacity-90 font-semibold text-xs text-left transition-colors;
                }
                .bot-msg-box {
                    @apply border rounded-2xl p-4 max-w-[85%] text-sm shadow-sm relative transition-colors duration-200;
                }
                .chat-footer-input {
                    @apply p-4 border-t flex-shrink-0 transition-colors duration-200;
                }
                .chat-footer-bar {
                    @apply border-t px-6 py-2.5 flex-shrink-0 transition-colors duration-200;
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
                <h2 class="text-base font-bold tracking-tight">AI Chatbot</h2>
                <div class="relative cursor-pointer p-1.5 hover:bg-gray-700 rounded-full transition-colors">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-gray-300"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
                    <span class="absolute top-1 right-1 w-4 h-4 bg-red-500 text-white text-[9px] font-bold rounded-full flex items-center justify-center scale-90 border border-gray-800">3</span>
                </div>
            </div>

            <div class="flex-1 flex flex-col min-h-0">
                <div class="chat-sub-header px-6 py-3.5 border-b border-gray-700 flex items-center justify-between flex-shrink-0 transition-colors">
                    <div class="flex items-center space-x-3">
                        <div class="w-9 h-9 bg-indigo-600 text-white rounded-full flex items-center justify-center shadow-sm shadow-indigo-600/10">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m12 3-1.912 5.886L4.2 10.8 10.088 12.714 12 18.6l1.912-5.886L19.8 10.8l-5.886-1.914Z"/></svg>
                        </div>
                        <div>
                            <h3 class="text-sm font-bold" id="currentSessionTitle">${currentChat.sessionName}</h3>
                            <p class="text-xs text-gray-400 font-medium mt-0.5">Tạo lúc: ${currentChat.createdAt}</p>
                        </div>
                    </div>

                    <div class="flex items-center gap-2">
                        <button onclick="toggleHistoryDrawer(true)" class="btn-secondary">
                            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                            <span>Lịch sử trò chuyện</span>
                        </button>
                        <button onclick="toggleCreateModal(true)" class="btn-secondary">
                            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="text-gray-400"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                            <span>Cuộc trò chuyện mới</span>
                        </button>
                    </div>
                </div>

                <div class="chat-bg-area" id="chatMessageLogs">
                    <div class="max-w-4xl mx-auto space-y-6" id="chatLogsContainer">
                        <c:choose>
                            <%-- Nếu danh sách tin nhắn rỗng, hiển thị lời chào mặc định --%>
                            <c:when test="${empty messageList}">
                                <div class="flex items-start gap-3">
                                    <div class="w-8 h-8 rounded-full bg-indigo-600 flex items-center justify-center text-white flex-shrink-0 shadow-sm">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m12 3-1.912 5.886L4.2 10.8 10.088 12.714 12 18.6l1.912-5.886L19.8 10.8l-5.886-1.914Z"/></svg>
                                    </div>
                                    <div class="bot-msg-box">
                                        <p class="leading-relaxed font-medium">Xin chào! Bạn đang ở chủ đề <b>${currentChat.sessionName}</b>. Tôi có thể giúp bạn giải đáp thắc mắc về tài liệu học tập, hoặc hỗ trợ làm bài tập. Bạn cần tôi giúp gì?</p>
                                    </div>
                                </div>
                            </c:when>

                            <%-- Nếu có tin nhắn, lặp qua danh sách và in ra --%>
                            <c:otherwise>
                                <c:forEach var="msg" items="${messageList}">
                                    <c:choose>
                                        <%-- Tin nhắn của người dùng --%>
                                        <c:when test="${msg.sender == 'USER'}">
                                            <div class="flex items-start gap-3 justify-end">
                                                <div class="bg-indigo-600 text-white rounded-2xl p-4 max-w-[85%] text-sm shadow-sm relative">
                                                    <p class="leading-relaxed font-medium">${msg.messageContent}</p>
                                                </div>
                                                <div class="w-8 h-8 rounded-full bg-indigo-900/60 text-indigo-300 flex items-center justify-center font-bold text-xs uppercase flex-shrink-0 shadow-sm">
                                                    <%= username != null && !username.isEmpty() ? username.substring(0, 1) : "U"%>
                                                </div>
                                            </div>
                                        </c:when>

                                        <%-- Tin nhắn của AI --%>
                                        <c:otherwise>
                                            <div class="flex items-start gap-3">
                                                <div class="w-8 h-8 rounded-full bg-indigo-600 flex items-center justify-center text-white flex-shrink-0 shadow-sm">
                                                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m12 3-1.912 5.886L4.2 10.8 10.088 12.714 12 18.6l1.912-5.886L19.8 10.8l-5.886-1.914Z"/></svg>
                                                </div>
                                                <div class="bot-msg-box">
                                                    <p class="leading-relaxed font-medium">${msg.messageContent}</p>
                                                </div>
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>

                <div class="chat-footer-input">
                    <div class="max-w-4xl mx-auto">
                        <div id="filePreviewContainer" class="hidden mb-2.5 flex flex-wrap gap-2"></div>
                        <div class="flex items-center gap-3">
                            <input type="file" id="chatFileInput" class="hidden" accept=".docx, .doc, .pptx, .xlsx, .pdf, .txt" onchange="handleChatFileSelect(this)">
                            <button onclick="document.getElementById('chatFileInput').click()" class="p-2.5 text-gray-400 hover:text-indigo-400 hover:bg-gray-700 rounded-xl transition-all flex-shrink-0" title="Đính kèm tài liệu (.docx, .doc, .pptx, .xlsx, .pdf, .txt)">
                                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l8.57-8.57A4 4 0 1 1 18 8.84l-8.59 8.57a2 2 0 0 1-2.83-2.83l8.49-8.48"/></svg>
                            </button>

                            <div class="chat-input-wrapper flex-1 relative flex items-center border rounded-xl shadow-sm px-4 py-3">
                                <input type="text" id="userChatInput" placeholder="Nhập câu hỏi của bạn... (Ấn Enter để gửi)" class="w-full bg-transparent border-none text-sm text-gray-200 placeholder-gray-500 focus:outline-none" onkeydown="handleInputKeyDown(event)"/>
                                <button onclick="processSendMessage()" class="absolute right-2 top-1/2 -translate-y-1/2 p-1.5 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors shadow-sm flex items-center justify-center">
                                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="22" x2="11" y1="2" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
                                </button>
                            </div>
                        </div>
                        <p class="text-[11px] text-gray-500 font-medium mt-2 text-center tracking-wide">
                            AI có thể mắc lỗi. Hãy kiểm tra kỹ thông tin quan trọng.
                        </p>
                    </div>
                </div>

                <div class="chat-footer-bar">
                    <div class="max-w-4xl mx-auto flex items-center justify-center gap-4 text-xs font-semibold text-gray-400 tracking-wide">
                        <div class="flex items-center gap-1.5">
                            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-indigo-400"><path d="m12 3-1.912 5.886L4.2 10.8 10.088 12.714 12 18.6l1.912-5.886L19.8 10.8l-5.886-1.914Z"/></svg>
                            <span>Hỗ trợ 24/7</span>
                        </div>
                        <span class="text-gray-600 font-light">•</span>
                        <div class="flex items-center gap-1.5">
                            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-indigo-400"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>
                            <span>Phân tích tài liệu</span>
                        </div>
                        <span class="text-gray-600 font-light">•</span>
                        <div class="flex items-center gap-1.5">
                            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-indigo-400"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>
                            <span>Hỗ trợ code</span>
                        </div>
                    </div>
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
                                <c:set var="isActive" value="${sessionItem.sessionId == currentChat.sessionId}" />

                                <div class="session-row p-3 rounded-xl border transition-all flex items-center justify-between group ${isActive ? 'bg-gray-800 border-gray-700' : 'bg-[#111827] border-transparent hover:border-gray-700 hover:bg-gray-800'}" 
                                     data-name="${sessionItem.sessionName.toLowerCase()}">

                                    <div id="display-area-${sessionItem.sessionId}" class="flex-1 min-w-0 flex items-start space-x-3 cursor-pointer" onclick="selectHistorySession('${sessionItem.sessionId}')">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-gray-500 mt-0.5 group-hover:text-indigo-400 flex-shrink-0"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                                        <div class="flex-1 min-w-0 pr-2">
                                            <p id="title-text-${sessionItem.sessionId}" class="text-sm font-semibold ${isActive ? 'text-indigo-400' : 'text-gray-200'} truncate mb-0.5">${sessionItem.sessionName}</p>
                                            <span class="text-[10px] text-gray-500 font-medium">${sessionItem.createdAt}</span>
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
            // --- Modal & Drawer Logic ---
            function toggleCreateModal(open) {
                const modal = document.getElementById('createSessionModal');
                if (open) {
                    modal.classList.remove('hidden');
                } else {
                    modal.classList.add('hidden');
                }
            }

            function selectHistorySession(sessionId) {
                window.location.href = "<%= request.getContextPath()%>/SessionController?action=viewSession&sessionId=" + sessionId;
            }

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

            // --- Inline Edit ---
            function startEditSession(id) {
                document.getElementById('display-area-' + id).classList.add('hidden');
                document.getElementById('action-area-' + id).classList.remove('group-hover:flex');
                document.getElementById('action-area-' + id).classList.add('hidden');

                const editArea = document.getElementById('edit-area-' + id);
                editArea.classList.remove('hidden');
                editArea.classList.add('flex');

                const input = document.getElementById('edit-input-' + id);
                input.focus();
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
                                document.getElementById('title-text-' + id).textContent = newName;
                                document.getElementById('display-area-' + id).closest('.session-row').setAttribute('data-name', newName.toLowerCase());

                                // Nếu đang đổi tên chính session hiện tại, cập nhật luôn Header
                                if (id == "${currentChat.sessionId}") {
                                    document.getElementById('currentSessionTitle').textContent = newName;
                                }
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

            // --- Delete Session ---
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
                                    // Nếu xóa đúng cái đang xem, đẩy ra trang chủ chat. Ngược lại chỉ reload.
                                    if (id == "${currentChat.sessionId}") {
                                        window.location.href = "<%= request.getContextPath()%>/SessionController?action=chatMain";
                                    } else {
                                        window.location.reload();
                                    }
                                } else {
                                    alert("Có lỗi xảy ra, không thể xóa cuộc trò chuyện.");
                                }
                            })
                            .catch(err => console.error("Delete Error:", err));
                }
            }


            // ==============================================================================
            // MOCK TIN NHẮN BẰNG LOCAL STORAGE (Dùng tạm cho tới khi có Message DB)
            // Lấy sessionId thật từ DB truyền qua để làm key lưu trữ
            // ==============================================================================
            // Cuộn khung chat xuống cuối cùng mỗi khi load trang
            // ==============================================================================
            // CÁC HÀM XỬ LÝ GIAO DIỆN VÀ GỬI TIN NHẮN
            // ==============================================================================

            // Biến toàn cục để lưu file đính kèm
            let attachedFileHolder = null;

            // Cuộn khung chat xuống cuối cùng mỗi khi load trang
            function scrollToBottom() {
                const scrollArea = document.getElementById('chatMessageLogs');
                scrollArea.scrollTop = scrollArea.scrollHeight;
            }

            document.addEventListener("DOMContentLoaded", function () {
                scrollToBottom();
            });

            // --- XỬ LÝ FILE ĐÍNH KÈM ---
            function handleChatFileSelect(input) {
                const file = input.files[0];
                if (!file)
                    return;

                const allowedExtensions = /(\.docx|\.doc|\.pptx|\.xlsx|\.pdf|\.txt)$/i;
                if (!allowedExtensions.exec(file.name)) {
                    alert("Hệ thống chỉ chấp nhận định dạng: docx, doc, pptx, xlsx, pdf, txt");
                    input.value = '';
                    return;
                }
                attachedFileHolder = file;
                renderFilePreview(file.name, (file.size / 1024).toFixed(1));
            }

            function renderFilePreview(filename, sizeKB) {
                const container = document.getElementById('filePreviewContainer');
                container.innerHTML = `
                    <div class="flex items-center space-x-2 bg-gray-800 border border-gray-700 text-indigo-400 px-3 py-1.5 rounded-xl text-xs font-semibold shadow-sm">
                        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/><path d="M14 2v4a2 2 0 0 0 2 2h4"/></svg>
                        <span class="max-w-[200px] truncate">${filename}</span>
                        <span class="text-[10px] opacity-60">(${sizeKB} KB)</span>
                        <button onclick="removeAttachedFile()" class="hover:text-red-400 transition-colors ml-1 p-0.5 rounded-full hover:bg-gray-700">
                            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="18" x2="18" y2="6"/></svg>
                        </button>
                    </div>
                `;
                container.classList.remove('hidden');
            }

            function removeAttachedFile() {
                attachedFileHolder = null;
                document.getElementById('chatFileInput').value = '';
                document.getElementById('filePreviewContainer').classList.add('hidden');
            }

            // --- XỬ LÝ PHÍM ENTER ---
            function handleInputKeyDown(event) {
                if (event.key === 'Enter') {
                    event.preventDefault(); // Ngăn hành vi xuống dòng của ô input
                    processSendMessage();
                }
            }

            // --- XỬ LÝ GỬI TIN NHẮN TỚI MAIN CONTROLLER ---
            function processSendMessage() {
                const inputElement = document.getElementById('userChatInput');
                const logsContainer = document.getElementById('chatLogsContainer');
                const messageText = inputElement.value.trim();

                // Nếu không có chữ và không có file thì không làm gì cả
                if (!messageText && !attachedFileHolder)
                    return;

                // Chuẩn bị text gửi đi
                let finalMessage = messageText;
                if (attachedFileHolder && !messageText) {
                    finalMessage = "[Gửi tệp: " + attachedFileHolder.name + "]";
                }
                if (attachedFileHolder && messageText) {
                    finalMessage += " [Kèm tệp: " + attachedFileHolder.name + "]";
                }

                // 1. Vẽ tin nhắn của User lên giao diện ngay lập tức
                const userMsgHtml = `
                    <div class="flex items-start gap-3 justify-end">
                        <div class="bg-indigo-600 text-white rounded-2xl p-4 max-w-[85%] text-sm shadow-sm relative">
                            <p class="leading-relaxed font-medium">${finalMessage}</p>
                        </div>
                        <div class="w-8 h-8 rounded-full bg-indigo-900/60 text-indigo-300 flex items-center justify-center font-bold text-xs uppercase flex-shrink-0 shadow-sm">
            <%= username != null && !username.isEmpty() ? username.substring(0, 1) : "U"%>
                        </div>
                    </div>
                `;
                logsContainer.insertAdjacentHTML('beforeend', userMsgHtml);
                scrollToBottom();

                // Reset input và khóa lại để tránh spam
                inputElement.value = "";
                removeAttachedFile();
                inputElement.disabled = true;

                // Nếu vùng chat chỉ có lời chào mặc định, ta xóa nó đi cho đẹp
                if (logsContainer.children.length === 2 && logsContainer.innerHTML.includes("Xin chào! Bạn đang ở chủ đề")) {
                    logsContainer.firstElementChild.remove();
                }

                // 2. Gửi request POST đến MainController
                fetch('<%= request.getContextPath()%>/MainController', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: 'action=chatbotPrompt&message=' + encodeURIComponent(finalMessage) + '&sessionId=${currentChat.sessionId}'
                })
                        .then(response => {
                            if (!response.ok)
                                throw new Error("Network error");
                            return response.text();
                        })
                        .then(data => {
                            // 3. Vẽ câu trả lời của AI
                            const formattedData = data.replace(/\n/g, "<br>");
                            const botMsgHtml = `
                        <div class="flex items-start gap-3">
                            <div class="w-8 h-8 rounded-full bg-indigo-600 flex items-center justify-center text-white flex-shrink-0 shadow-sm">
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m12 3-1.912 5.886L4.2 10.8 10.088 12.714 12 18.6l1.912-5.886L19.8 10.8l-5.886-1.914Z"/></svg>
                            </div>
                            <div class="bot-msg-box">
                                <p class="leading-relaxed font-medium">` + formattedData + `</p>
                            </div>
                        </div>
                    `;
                            logsContainer.insertAdjacentHTML('beforeend', botMsgHtml);
                            scrollToBottom();
                        })
                        .catch(error => {
                            console.error('Error:', error);
                            logsContainer.insertAdjacentHTML('beforeend', `<div class="text-red-500 text-center text-sm mt-2">Đã xảy ra lỗi khi kết nối với máy chủ.</div>`);
                            scrollToBottom();
                        })
                        .finally(() => {
                            // Mở khóa input
                            inputElement.disabled = false;
                            inputElement.focus();
                        });
            }
        </script>
    </body>
</html>