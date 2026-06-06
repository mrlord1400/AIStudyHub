<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // 1. Kiểm tra trạng thái đăng nhập
    HttpSession userSession = request.getSession(false);
    if (userSession == null || userSession.getAttribute("userId") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    int userId = (Integer) userSession.getAttribute("userId");
    String username = (String) userSession.getAttribute("username");
    String role = (String) userSession.getAttribute("role");

    // 2. Lấy số dư ví Coin
    Integer userBalance = (Integer) userSession.getAttribute("balance");
    if (userBalance == null) {
        userBalance = 0;
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AI Chatbot - AI Study Hub</title>
    
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    
    <script>
        if (localStorage.getItem('darkMode') === 'enabled') {
            document.documentElement.classList.add('dark');
        } else {
            document.documentElement.classList.remove('dark');
        }
    </script>
    
    <style type="text/tailwindcss">
        /* Tinh chỉnh thanh cuộn cho giống ứng dụng */
        ::-webkit-scrollbar { width: 6px; height: 6px; }
        ::-webkit-scrollbar-track { background: transparent; }
        html.dark ::-webkit-scrollbar-thumb { background: #4b5563; }
        html.dark ::-webkit-scrollbar-thumb:hover { background: #6b7280; }
        ::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
        ::-webkit-scrollbar-thumb:hover { background: #94a3b8; }

        /* CSS THUẦN CHO CHẾ ĐỘ TỐI (DARK MODE) */
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

        /* SỬA LỖI ĐỒNG BỘ ĐỘC LẬP CHO KHUNG CHAT */
        html.dark .chat-container { background-color: #111827; }
        html.dark .chat-header { background-color: #1f2937 !important; border-color: #374151 !important; }
        html.dark .chat-header h2 { color: #ffffff !important; }
        html.dark .chat-sub-header { background-color: #1f2937 !important; border-color: #374151 !important; }
        html.dark .chat-sub-header h3 { color: #ffffff !important; }
        html.dark .chat-bg-area { background-color: #111827; }
        html.dark .bot-msg-box { background-color: #1f2937; border-color: #374151; color: #e5e7eb; }
        
        /* FIX LỖI KHUNG NHẬP LIỆU BỊ MẤT CHỮ TRÊN NỀN TỐI */
        html.dark .chat-footer-input { background-color: #111827 !important; border-color: #374151 !important; }
        html.dark .chat-input-wrapper { background-color: #1f2937 !important; border-color: #374151 !important; color: #ffffff !important; }
        html.dark .chat-input-wrapper input { background-color: transparent !important; color: #ffffff !important; }
        html.dark .chat-footer-bar { background-color: #1f2937; border-color: #374151; }
        
        /* ĐỔI GIAO DIỆN NÚT SUGGESTION TRÊN DARK MODE */
        html.dark .suggest-btn-1 { background-color: rgba(30, 58, 138, 0.4); border-color: rgba(59, 130, 246, 0.3); color: #60a5fa; }
        html.dark .suggest-btn-2 { background-color: rgba(120, 53, 4, 0.4); border-color: rgba(245, 158, 11, 0.3); color: #fbbf24; }
        html.dark .suggest-btn-3 { background-color: rgba(6, 78, 59, 0.4); border-color: rgba(16, 185, 129, 0.3); color: #34d399; }
        html.dark .suggest-btn-4 { background-color: rgba(88, 28, 135, 0.4); border-color: rgba(139, 92, 246, 0.3); color: #c084fc; }

        /* ÉP MÀU GIAO DIỆN DRAWER LỊCH SỬ CHAT TRÊN DARK MODE */
        html.dark #historyDrawer > div { background-color: #1f2937 !important; border-color: #374151 !important; color: #ffffff !important; }
        html.dark #historyDrawer h3 { color: #ffffff !important; }
        html.dark .history-item { background-color: #111827 !important; border-color: #374151 !important; }
        html.dark .history-item:hover { background-color: #2d3748 !important; }
        html.dark .history-title { color: #f3f4f6 !important; }

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
            
            .chat-container { @apply flex-1 flex flex-col h-full bg-white relative transition-colors duration-200; }
            .chat-header { @apply h-14 border-b border-gray-100 px-6 flex items-center justify-between flex-shrink-0 bg-white transition-colors duration-200; }
            .chat-bg-area { @apply flex-1 overflow-y-auto p-6 space-y-6 bg-white transition-colors duration-200; }
            
            .btn-secondary { @apply flex items-center justify-center space-x-2 px-4 py-2 bg-white border border-gray-200 text-gray-700 rounded-xl font-semibold hover:bg-gray-50 transition-colors text-xs shadow-sm cursor-pointer; }
            
            .suggest-btn-1 { @apply flex items-center space-x-3 p-3.5 rounded-xl bg-blue-50/60 border border-blue-100/40 hover:bg-blue-50 text-blue-600 font-semibold text-xs text-left transition-colors; }
            .suggest-btn-2 { @apply flex items-center space-x-3 p-3.5 rounded-xl bg-amber-50/60 border border-amber-100/40 hover:bg-amber-50 text-amber-600 font-semibold text-xs text-left transition-colors; }
            .suggest-btn-3 { @apply flex items-center space-x-3 p-3.5 rounded-xl bg-green-50/60 border border-green-100/40 hover:bg-green-50 text-green-600 font-semibold text-xs text-left transition-colors; }
            .suggest-btn-4 { @apply flex items-center space-x-3 p-3.5 rounded-xl bg-purple-50/60 border border-purple-100/40 hover:bg-purple-50 text-purple-600 font-semibold text-xs text-left transition-colors; }
            
            .bot-msg-box { @apply bg-gray-50 border border-gray-100/80 rounded-2xl p-4 max-w-[85%] text-sm text-gray-800 shadow-sm relative transition-colors duration-200; }
            .chat-footer-input { @apply p-4 border-t border-gray-100 bg-white flex-shrink-0 transition-colors duration-200; }
            .chat-footer-bar { @apply bg-gray-50 border-t border-gray-100 px-6 py-2.5 flex-shrink-0 transition-colors duration-200; }
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
                <a href="<%= request.getContextPath() %>/user_dashboard.jsp" class="nav-link">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z"/></svg>
                    <span>Tài liệu của tôi</span>
                </a>
                <a href="<%= request.getContextPath() %>/MainController?action=explore" class="nav-link">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
                    <span>Khám phá tài liệu</span>
                </a>
                <a href="<%= request.getContextPath() %>/MainController?action=aiChat" class="nav-link-active">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 8V4H8"/><rect width="16" height="12" x="4" y="8" rx="2"/><path d="M2 14h2"/><path d="M20 14h2"/><path d="M15 13v2"/><path d="M9 13v2"/></svg>
                    <span>AI Chatbot</span>
                </a>
                <a href="<%= request.getContextPath() %>/MainController?action=wallet" class="nav-link">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="20" height="14" x="2" y="5" rx="2"/><line x1="2" x2="22" y1="10" y2="10"/><path d="M16 14h2"/></svg>
                    <span>Ví cá nhân</span>
                </a>
                <a href="<%= request.getContextPath() %>/MainController?action=upgrade" class="nav-link text-amber-600 !text-amber-600 hover:bg-amber-50 dark:!text-amber-500 dark:hover:bg-amber-950/30">
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
                <div class="wallet-balance"><%= String.format("%,d", userBalance) %> Coin</div>
            </div>

            <div class="user-area">
                <div class="flex items-center justify-between w-full">
                    <a href="<%= request.getContextPath() %>/MainController?action=profile" class="user-profile-link flex-1 mr-2">
                        <div class="user-avatar">
                            <%= username != null && !username.isEmpty() ? username.substring(0, 1) : "U" %>
                        </div>
                        <div class="user-info">
                            <p class="user-name"><%= username != null ? username : "Khách" %></p>
                            <p class="user-role">Quyền: <%= role != null ? role : "Free" %></p>
                        </div>
                    </a>
                    
                    <button id="themeToggleBtn" onclick="toggleThemeMode()" class="p-2.5 bg-gray-100 hover:bg-gray-200 dark:bg-gray-700 dark:hover:bg-gray-600 rounded-xl transition-all duration-200 focus:outline-none flex items-center justify-center" title="Chuyển đổi giao diện">
                        <svg id="sunIcon" class="w-5 h-5 text-amber-500 hidden" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M10 2a1 1 0 011 1v1a1 1 0 11-2 0V3a1 1 0 011-1zm4 2.293a1 1 0 011.414 0l.707.707a1 1 0 01-1.414 1.414l-.707-.707a1 1 0 010-1.414zm4 4.707a1 1 0 011 1v1a1 1 0 11-2 0V10a1 1 0 011-1zm-4 4.707a1 1 0 010 1.414l-.707.707a1 1 0 01-1.414-1.414l.707-.707a1 1 0 011.414 0zM9 17a1 1 0 011-1v-1a1 1 0 112 0v1a1 1 0 01-1 1zM4.293 14.707a1 1 0 010-1.414l.707-.707a1 1 0 011.414 1.414l-.707.707a1 1 0 01-1.414 0zM1 10a1 1 0 011-1h1a1 1 0 110 2H2a1 1 0 01-1-1zm3.293-4.707a1 1 0 011.414-1.414l.707.707A1 1 0 015.707 5.707l-.707-.707zM10 5a5 5 0 100 10 5 5 0 000-10z" clip-rule="evenodd"/></svg>
                        <svg id="moonIcon" class="w-5 h-5 text-indigo-600 hidden" fill="currentColor" viewBox="0 0 20 20"><path d="M17.293 13.293A8 8 0 016.707 2.707a8.001 8.001 0 1010.586 10.586z"/></svg>
                    </button>
                </div>

                <a href="<%= request.getContextPath() %>/MainController?action=logout" class="logout-btn">
                    <svg xmlns="http://www.w3.org/2000/xl" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" x2="9" y1="12" y2="12"/></svg>
                    <span>Đăng xuất</span>
                </a>
            </div>
        </div>
    </aside>

    <div class="chat-container">
        
        <div class="chat-header">
            <h2 class="text-base font-bold text-gray-900 tracking-tight">AI Chatbot</h2>
            <div class="relative cursor-pointer p-1.5 hover:bg-gray-50 dark:hover:bg-gray-700 rounded-full transition-colors">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-gray-600 dark:text-gray-300"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
                <span class="absolute top-1 right-1 w-4 h-4 bg-red-500 text-white text-[9px] font-bold rounded-full flex items-center justify-center scale-90 border border-white dark:border-gray-800">3</span>
            </div>
        </div>

        <div class="flex-1 flex flex-col min-h-0">
            <div class="chat-sub-header px-6 py-3.5 border-b border-gray-100 flex items-center justify-between flex-shrink-0 bg-white transition-colors">
                <div class="flex items-center space-x-3">
                    <div class="w-9 h-9 bg-indigo-600 text-white rounded-full flex items-center justify-center shadow-sm shadow-indigo-600/10">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m12 3-1.912 5.886L4.2 10.8 10.088 12.714 12 18.6l1.912-5.886L19.8 10.8l-5.886-1.914Z"/></svg>
                    </div>
                    <div>
                        <h3 class="text-sm font-bold text-gray-900">AI Study Assistant</h3>
                        <p class="text-xs text-gray-400 font-medium mt-0.5">Luôn sẵn sàng hỗ trợ bạn</p>
                    </div>
                </div>
                
                <div class="flex items-center gap-2">
                    <button onclick="toggleHistoryDrawer(true)" class="btn-secondary !border-indigo-200 !text-indigo-600 hover:!bg-indigo-50/50 dark:!border-gray-700 dark:!text-indigo-400">
                        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                        <span>Lịch sử trò chuyện</span>
                    </button>
                    <button onclick="createNewChatSession()" class="btn-secondary">
                        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="text-gray-500 dark:text-gray-400"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                        <span>Cuộc trò chuyện mới</span>
                    </button>
                </div>
            </div>

            <div class="chat-bg-area" id="chatMessageLogs">
                <div class="max-w-4xl mx-auto space-y-6" id="chatLogsContainer">
                    </div>
            </div>

            <div class="chat-footer-input">
                <div class="max-w-4xl mx-auto">
                    
                    <div id="filePreviewContainer" class="hidden mb-2.5 flex flex-wrap gap-2">
                    </div>

                    <div class="flex items-center gap-3">
                        <input type="file" id="chatFileInput" class="hidden" 
                               accept=".docx, .doc, .pptx, .xlsx, .pdf, .txt" 
                               onchange="handleChatFileSelect(this)">
                               
                        <button onclick="document.getElementById('chatFileInput').click()" class="p-2.5 text-gray-400 hover:text-indigo-600 hover:bg-gray-50 dark:hover:bg-gray-700 rounded-xl transition-all flex-shrink-0" title="Đính kèm tài liệu (.docx, .doc, .pptx, .xlsx, .pdf, .txt)">
                            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l8.57-8.57A4 4 0 1 1 18 8.84l-8.59 8.57a2 2 0 0 1-2.83-2.83l8.49-8.48"/></svg>
                        </button>
                        
                        <div class="chat-input-wrapper flex-1 relative flex items-center border border-gray-200 rounded-xl bg-white shadow-sm px-4 py-3">
                            <input 
                                 type="text" 
                                 id="userChatInput"
                                 placeholder="Nhập câu hỏi của bạn... (Ấn Enter để gửi)"
                                 class="w-full bg-transparent border-none text-sm text-gray-700 placeholder-gray-400 focus:outline-none"
                                 onkeydown="handleInputKeyDown(event)"
                            />
                            <button onclick="processSendMessage()" class="absolute right-2 top-1/2 -translate-y-1/2 p-1.5 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors shadow-sm flex items-center justify-center">
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="22" x2="11" y1="2" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
                            </button>
                        </div>
                    </div>
                    <p class="text-[11px] text-gray-400 font-medium mt-2 text-center tracking-wide">
                        AI có thể mắc lỗi. Hãy kiểm tra kỹ thông tin quan trọng.
                    </p>
                </div>
            </div>

            <div class="chat-footer-bar">
                <div class="max-w-4xl mx-auto flex items-center justify-center gap-4 text-xs font-semibold text-gray-500 tracking-wide">
                    <div class="flex items-center gap-1.5">
                        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-indigo-500"><path d="m12 3-1.912 5.886L4.2 10.8 10.088 12.714 12 18.6l1.912-5.886L19.8 10.8l-5.886-1.914Z"/></svg>
                        <span>Hỗ trợ 24/7</span>
                    </div>
                    <span class="text-gray-300 font-light">•</span>
                    <div class="flex items-center gap-1.5">
                        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-indigo-500"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>
                        <span>Phân tích tài liệu</span>
                    </div>
                    <span class="text-gray-300 font-light">•</span>
                    <div class="flex items-center gap-1.5">
                        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-indigo-500"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>
                        <span>Hỗ trợ code</span>
                    </div>
                </div>
            </div>

        </div>
    </div>

    <div id="historyDrawer" class="fixed inset-0 z-[150] hidden bg-gray-900/40 backdrop-blur-xs flex justify-end">
        <div onclick="toggleHistoryDrawer(false)" class="flex-1 h-full"></div>
        <div class="w-full max-w-sm bg-white h-full shadow-2xl border-l border-gray-100 flex flex-col transform translate-x-full transition-transform duration-300 ease-out p-6">
            <div class="flex justify-between items-center pb-4 border-b border-gray-100 dark:border-gray-700 flex-shrink-0">
                <div class="flex items-center space-x-2">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-indigo-600"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                    <h3 class="text-base font-bold text-gray-900">Lịch sử hội thoại</h3>
                </div>
                <button onclick="toggleHistoryDrawer(false)" class="p-1.5 text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-gray-700 rounded-lg transition-colors">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="18" x2="18" y2="6"/></svg>
                </button>
            </div>

            <div class="flex-1 overflow-y-auto py-4 space-y-3" id="historyItemsContainer">
                </div>

            <div class="pt-4 border-t border-gray-100 dark:border-gray-700 text-center flex-shrink-0">
                <p class="text-[10px] font-medium text-gray-400">Lịch sử chat được lưu trữ cục bộ trên trình duyệt</p>
            </div>
        </div>
    </div>

    <div class="absolute bottom-11 right-4 z-50">
        <button class="w-9 h-9 bg-gray-900 dark:bg-gray-700 text-white rounded-full flex items-center justify-center hover:bg-slate-800 dark:hover:bg-gray-600 shadow-md transition-all">
            <svg xmlns="http://www.w3.org/2000/xl" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" x2="12.01" y1="17" y2="17"/></svg>
        </button>
    </div>

    <script>
        let attachedFileHolder = null;
        
        // --- LOGIC FRONT-END QUẢN LÝ STATE LỊCH SỬ CHAT (LOCALSTORAGE) ---
        let currentSessionId = localStorage.getItem('currentSessionId') || "session_" + Date.now();
        let chatSessions = JSON.parse(localStorage.getItem('chatSessions')) || {};

        // Lưu ID phiên làm việc hiện tại nếu chưa có
        if(!localStorage.getItem('currentSessionId')) {
            localStorage.setItem('currentSessionId', currentSessionId);
        }

        function saveSessions() {
            localStorage.setItem('chatSessions', JSON.stringify(chatSessions));
        }

        // Tải lịch sử tin nhắn lên giao diện
        function loadChatLogs() {
            const logsContainer = document.getElementById('chatLogsContainer');
            logsContainer.innerHTML = ""; // Reset khung chat

            // Lấy dữ liệu hội thoại hiện tại
            const currentSession = chatSessions[currentSessionId];

            if (!currentSession || currentSession.messages.length === 0) {
                // Nếu cuộc trò chuyện mới tinh, hiển thị lời chào mặc định và Box câu hỏi gợi ý
                logsContainer.innerHTML = `
                    <div class="flex items-start gap-3">
                        <div class="w-8 h-8 rounded-full bg-indigo-600 flex items-center justify-center text-white flex-shrink-0 shadow-sm">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m12 3-1.912 5.886L4.2 10.8 10.088 12.714 12 18.6l1.912-5.886L19.8 10.8l-5.886-1.914Z"/></svg>
                        </div>
                        <div class="bot-msg-box">
                            <p class="leading-relaxed font-medium">Xin chào! Tôi là AI Study Assistant. Tôi có thể giúp bạn giải đáp thắc mắc về tài liệu học tập, giải thích khái niệm, hoặc hỗ trợ làm bài tập. Bạn cần tôi giúp gì?</p>
                            <div class="flex items-center gap-3 mt-4 text-gray-400 dark:text-gray-500">
                                <button class="hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors" title="Sao chép"><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="14" height="14" x="8" y="8" rx="2" ry="2"/><path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"/></svg></button>
                            </div>
                        </div>
                    </div>
                    <div id="suggestionBox" class="pt-4 max-w-[85%] mx-auto">
                        <p class="text-center text-xs font-semibold text-gray-400 dark:text-gray-500 uppercase tracking-wider mb-3">Câu hỏi gợi ý:</p>
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                            <button onclick="useSuggestion('Giải thích về Design Patterns')" class="suggest-btn-1"><span class="truncate">Giải thích về Design Patterns</span></button>
                            <button onclick="useSuggestion('Gợi ý cách học SWP391 hiệu quả')" class="suggest-btn-2"><span class="truncate">Gợi ý cách học SWP391 hiệu quả</span></button>
                            <button onclick="useSuggestion('Viết code mẫu cho MVC pattern')" class="suggest-btn-3"><span class="truncate">Viết code mẫu cho MVC pattern</span></button>
                            <button onclick="useSuggestion('Tóm tắt tài liệu Database Design')" class="suggest-btn-4"><span class="truncate">Tóm tắt tài liệu Database Design</span></button>
                        </div>
                    </div>
                `;
            } else {
                // Nếu có tin nhắn cũ, render toàn bộ tin nhắn ra màn hình
                currentSession.messages.forEach(msg => {
                    const messageNode = document.createElement('div');
                    if (msg.sender === 'user') {
                        messageNode.className = "flex items-start gap-3 justify-end";
                        messageNode.innerHTML = `
                            <div class="bg-indigo-600 text-white rounded-2xl p-4 max-w-[85%] text-sm shadow-sm relative">
                                <p class="leading-relaxed font-medium">\${msg.text}</p>
                            </div>
                            <div class="w-8 h-8 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center font-bold text-xs uppercase flex-shrink-0 shadow-sm">
                                <%= username != null && !username.isEmpty() ? username.substring(0, 1) : "U" %>
                            </div>
                        `;
                    } else {
                        messageNode.className = "flex items-start gap-3";
                        messageNode.innerHTML = `
                            <div class="w-8 h-8 rounded-full bg-indigo-600 flex items-center justify-center text-white flex-shrink-0 shadow-sm">
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m12 3-1.912 5.886L4.2 10.8 10.088 12.714 12 18.6l1.912-5.886L19.8 10.8l-5.886-1.914Z"/></svg>
                            </div>
                            <div class="bot-msg-box">
                                <p class="leading-relaxed font-medium">\${msg.text}</p>
                            </div>
                        `;
                    }
                    logsContainer.appendChild(messageNode);
                });
            }
            // Auto scroll xuống cuối danh sách tin nhắn
            const scrollArea = document.getElementById('chatMessageLogs');
            scrollArea.scrollTop = scrollArea.scrollHeight;
        }

        // Tải danh sách các cuộc hội thoại vào Drawer Lịch sử
        function renderHistoryDrawerList() {
            const historyContainer = document.getElementById('historyItemsContainer');
            historyContainer.innerHTML = "";

            const sessionIds = Object.keys(chatSessions).sort((a, b) => chatSessions[b].timestamp - chatSessions[a].timestamp);

            if (sessionIds.length === 0) {
                historyContainer.innerHTML = `<p class="text-xs text-gray-400 text-center py-4">Chưa có lịch sử hội thoại</p>`;
                return;
            }

            sessionIds.forEach(id => {
                const session = chatSessions[id];
                const activeClass = (id === currentSessionId) ? "!bg-indigo-50 border-indigo-200 dark:!bg-gray-800" : "";
                
                const itemNode = document.createElement('div');
                itemNode.className = `history-item p-3 bg-gray-50 border border-gray-100/50 rounded-xl cursor-pointer hover:bg-indigo-50/40 transition-colors flex items-start space-x-3 group \${activeClass}`;
                itemNode.setAttribute('onclick', `selectHistorySession('\${id}')`);
                itemNode.innerHTML = `
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-gray-400 mt-0.5 group-hover:text-indigo-600"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                    <div class="flex-1 min-w-0">
                        <p class="history-title text-xs font-semibold text-gray-800 truncate mb-1">\${session.title}</p>
                        <span class="text-[10px] text-gray-400 font-medium">\${session.timeStr}</span>
                    </div>
                `;
                historyContainer.appendChild(itemNode);
            });
        }

        // Thực hiện chuyển đổi xem lại cuộc trò chuyện cũ từ lịch sử
        function selectHistorySession(sessionId) {
            currentSessionId = sessionId;
            localStorage.setItem('currentSessionId', sessionId);
            loadChatLogs();
            toggleHistoryDrawer(false);
        }

        // Thực hiện tạo cuộc trò chuyện mới độc lập
        function createNewChatSession() {
            currentSessionId = "session_" + Date.now();
            localStorage.setItem('currentSessionId', currentSessionId);
            loadChatLogs();
            renderHistoryDrawerList();
        }
        // -------------------------------------------------------------

        function updateThemeButtons() {
            const isDark = document.documentElement.classList.contains('dark');
            const sunIcon = document.getElementById('sunIcon');
            const moonIcon = document.getElementById('moonIcon');
            const toggleBtn = document.getElementById('themeToggleBtn');
            
            if (isDark) {
                sunIcon.classList.remove('hidden');
                moonIcon.classList.add('hidden');
                toggleBtn.className = "p-2.5 bg-gray-700 hover:bg-gray-600 rounded-xl transition-all duration-200 focus:outline-none flex items-center justify-center border border-gray-600 shadow-inner";
            } else {
                sunIcon.classList.add('hidden');
                moonIcon.classList.remove('hidden');
                toggleBtn.className = "p-2.5 bg-gray-100 hover:bg-gray-200 rounded-xl transition-all duration-200 focus:outline-none flex items-center justify-center border border-gray-200 shadow-sm";
            }
        }

        function toggleThemeMode() {
            if (document.documentElement.classList.contains('dark')) {
                document.documentElement.classList.remove('dark');
                localStorage.setItem('darkMode', 'disabled');
            } else {
                document.documentElement.classList.add('dark');
                localStorage.setItem('darkMode', 'enabled');
            }
            updateThemeButtons();
        }

        function toggleHistoryDrawer(open) {
            const drawer = document.getElementById('historyDrawer');
            const innerContent = drawer.querySelector('div:nth-child(2)');
            
            if (open) {
                renderHistoryDrawerList(); // Làm mới danh sách khi mở
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

        function handleChatFileSelect(input) {
            const file = input.files[0];
            if (!file) return;

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
                <div class="flex items-center space-x-2 bg-indigo-50 border border-indigo-100 text-indigo-700 dark:bg-gray-800 dark:border-gray-700 dark:text-indigo-400 px-3 py-1.5 rounded-xl text-xs font-semibold shadow-sm">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/><path d="M14 2v4a2 2 0 0 0 2 2h4"/></svg>
                    <span class="max-w-[200px] truncate">\${filename}</span>
                    <span class="text-[10px] opacity-60">(\${sizeKB} KB)</span>
                    <button onclick="removeAttachedFile()" class="hover:text-red-500 transition-colors ml-1 p-0.5 rounded-full hover:bg-indigo-100 dark:hover:bg-gray-700">
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

        function handleInputKeyDown(event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                processSendMessage();
            }
        }

        function useSuggestion(text) {
            document.getElementById('userChatInput').value = text;
            document.getElementById('userChatInput').focus();
        }

        function processSendMessage() {
            const inputElement = document.getElementById('userChatInput');
            const messageText = inputElement.value.trim();

            if (!messageText && !attachedFileHolder) return;

            // Đảm bảo Session tồn tại trong Object bộ nhớ
            if (!chatSessions[currentSessionId]) {
                const now = new Date();
                chatSessions[currentSessionId] = {
                    title: messageText ? messageText : "Tập tin đính kèm",
                    timestamp: Date.now(),
                    timeStr: `Hôm nay, \${now.getHours().toString().padStart(2, '0')}:\${now.getMinutes().toString().padStart(2, '0')}`,
                    messages: []
                };
            }

            // Đẩy tin nhắn của User vào mảng lưu trữ
            chatSessions[currentSessionId].messages.push({
                sender: 'user',
                text: messageText
            });

            // Giả lập bot trả lời ngay lập tức để đồng bộ hóa trạng thái lưu trữ
            chatSessions[currentSessionId].messages.push({
                sender: 'bot',
                text: "Tôi đã nhận được thông tin: '" + messageText + "'. Hệ thống đang xử lý câu trả lời của bạn..."
            });

            // Lưu thay đổi vào localStorage
            saveSessions();

            // Render lại khung chat để cập nhật UI ngay lập tức
            loadChatLogs();

            inputElement.value = "";
            removeAttachedFile();
        }

        document.addEventListener("DOMContentLoaded", function() {
            updateThemeButtons();
            loadChatLogs(); // Gọi hàm tự động load tin nhắn cũ/mới khi f5 trang
        });
    </script>
</body>
</html>
