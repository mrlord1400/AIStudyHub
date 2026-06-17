<%@page import="Model.User"%>
<%@page import="Model.UserDAO"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List" %>
<%
    HttpSession userSession = request.getSession(false);
    if (userSession == null || userSession.getAttribute("userId") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    Integer userId = (Integer) userSession.getAttribute("userId");
    String username = (String) userSession.getAttribute("username");
    String role = (String) userSession.getAttribute("role");
    Integer tierId = (Integer) userSession.getAttribute("tierId");

    // ------------------------------------------------------------------
    // FIX 1: QUẢN LÝ QUYỀN (ROLE)
    // Ép kiểu Quyền: Bất kỳ ai không phải ADMIN thì đều mặc định là quyền STUDENT
    if (role == null || !"ADMIN".equalsIgnoreCase(role.trim())) {
        role = "STUDENT"; 
    } else {
        role = "ADMIN";
    }

    // ------------------------------------------------------------------
    // FIX 2: QUẢN LÝ GÓI (TIER)
    // Theo DB hệ thống: tierId = 2 là FREE, tierId = 3 là PREMIUM.
    if (tierId == null || tierId < 2) {
        tierId = 2; // Mặc định gán 2 cho người mới đăng ký (Gói FREE)
    }

    // Từ tier 3 trở lên mới được hệ thống nhận diện là tài khoản Premium
    boolean isPremiumUser = (tierId >= 3);

    UserDAO dao = new UserDAO();
    int userBalance = 0;
    if (userId != null) {
        User user = dao.getUserById(userId);
        userBalance = user.getBalance();
    }
    int premiumPrice = 99000;
%>
<!DOCTYPE html>
<html lang="vi" class="dark">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Nâng cấp Premium - AI Study Hub</title>

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
            html.dark .stat-widget {
                background-color: #1f2937;
                border-color: #374151;
            }
            html.dark .stat-label {
                color: #d1d5db;
            }
            html.dark .stat-value {
                color: #ffffff;
            }
            html.dark .content-box {
                background-color: #1f2937;
                border-color: #374151;
            }
            html.dark .content-title {
                color: #ffffff;
            }
            html.dark .empty-state-box {
                background-color: #1f2937 !important;
                border-color: #374151 !important;
            }
            html.dark .empty-state-icon {
                background-color: #374151 !important;
                color: #d1d5db !important;
            }

            html.dark #purchase-modal > div {
                background-color: #1f2937 !important;
                color: #ffffff !important;
                border-color: #374151 !important;
            }
            html.dark #purchase-modal h3, html.dark #purchase-modal h4 {
                color: #ffffff !important;
            }
            html.dark #purchase-modal .border-gray-100, html.dark #purchase-modal .border-gray-200 {
                border-color: #374151 !important;
            }
            html.dark #purchase-modal .text-gray-800 {
                color: #e5e7eb !important;
            }
            html.dark #purchase-modal .modal-alert-note {
                background-color: rgba(55, 65, 81, 0.4) !important;
                border-color: #4b5563 !important;
            }
            html.dark #purchase-modal .modal-alert-text {
                color: #cbd5e0 !important;
            }
            html.dark #purchase-modal .modal-alert-text p {
                color: #a78bfa !important;
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

                .stat-widget {
                    @apply bg-white border border-gray-100 rounded-2xl p-6 shadow-sm flex items-center justify-between transition-colors duration-200;
                }
                .stat-label {
                    @apply text-sm text-gray-600 font-medium;
                }
                .stat-value {
                    @apply text-xl font-bold text-gray-900;
                }
                .content-box {
                    @apply bg-white border border-gray-100 rounded-2xl shadow-sm overflow-hidden transition-colors duration-200;
                }
                .content-title {
                    @apply text-base font-bold text-gray-900;
                }
            }
        </style>
    </head>
    <body class="page-body">

        <aside class="sidebar">
            <div class="space-y-6 w-full">
                <div class="brand-container">
                    <div class="brand-logo">
                        <!-- LOGO 1: Vương miện 3 răng tròn - sidebar brand -->
                        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21.42 10.922a1 1 0 0 0-.019-1.838L12.83 5.18a2 2 0 0 0-1.66 0L2.6 9.08a1 1 0 0 0 0 1.832l8.57 3.908a2 2 0 0 0 1.66 0z"></path><path d="M6 18.8V13.5"></path><path d="M18 13.5v5.3a2.1 2.1 0 0 1-2 2h-8a2.1 2.1 0 0 1-2-2v-5.3"></path></svg>
                        <path d="M4 32 L4 14 L13 22 L22 6 L31 22 L40 14 L40 32 Z" fill="#F59E0B"/>
                        <rect x="4" y="32" width="36" height="5" rx="2.5" fill="#D97706"/>
                        <circle cx="4" cy="14" r="3.5" fill="#FBBF24"/>
                        <circle cx="22" cy="6" r="3.5" fill="#FBBF24"/>
                        <circle cx="40" cy="14" r="3.5" fill="#FBBF24"/>
                        </svg>
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
                    <a href="AIChatbotLanding.jsp" class="nav-link">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 8V4H8"/><rect width="16" height="12" x="4" y="8" rx="2"/><path d="M2 14h2"/><path d="M20 14h2"/><path d="M15 13v2"/><path d="M9 13v2"/></svg>
                        <span>AI Chatbot</span>
                    </a>
                    <a href="MainController?action=listTransactions" class="nav-link">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="20" height="14" x="2" y="5" rx="2"/><line x1="2" x2="22" y1="10" y2="10"/><path d="M16 14h2"/></svg>
                        <span>Ví cá nhân</span>
                    </a>
                    <a href="Membership.jsp" class="nav-link-active !bg-gradient-to-r !from-amber-500 !to-orange-500 !text-white shadow-md border-none hover:opacity-90">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 4l3 12h14l3-12-6 7-4-7-4 7-6-7z"/><path d="M5 20h14"/></svg>
                        <span class="font-semibold text-white">Nâng cấp Premium</span>
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

        <main class="main-content">
            <div class="flex justify-between items-center mb-10">
                <div>
                    <h1 class="text-2xl font-bold tracking-tight text-white">Nâng cấp tài khoản</h1>
                    <p class="text-sm text-gray-500 font-medium dark:text-gray-400">Bứt phá mọi giới hạn học tập cùng các tính năng AI thông minh</p>
                </div>
            </div>

            <div class="max-w-5xl mx-auto">
                <div class="text-center mb-16">
                    <div class="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-tr from-purple-500 to-indigo-600 text-white rounded-2xl mb-4 shadow-lg shadow-indigo-600/10 dark:shadow-none">
                        <svg xmlns="http://www.w3.org/2000/svg" width="36" height="32" viewBox="0 0 24 24" fill="none" stroke="gold" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 4l3 12h14l3-12-6 7-4-7-4 7-6-7z"/><path d="M5 20h14"/></svg>
                    </div>
                    <h2 class="text-3xl font-extrabold text-gray-900 dark:text-white mb-3 tracking-tight">Nâng cấp trải nghiệm học tập</h2>
                    <p class="text-gray-500 max-w-xl mx-auto text-base dark:text-gray-400">Chọn gói phù hợp với nhu cầu của bạn và tận hưởng các tính năng cao cấp</p>
                </div>

                <div class="grid grid-cols-1 md:grid-cols-2 gap-8 mb-20 items-start">
                    <div class="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden dark:bg-gray-800 dark:border-gray-700">
                        <div class="bg-gray-50 dark:bg-gray-700/50 p-8 text-gray-900 dark:text-white relative border-b border-gray-100 dark:border-gray-700">
                            <svg class="w-12 h-12 text-gray-400 dark:text-purple-300/40 mb-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path></svg>
                            <h3 class="text-2xl font-bold mb-1">Free</h3>
                            <p class="text-gray-500 dark:text-gray-400 text-xs mb-4">Dành cho sinh viên mới bắt đầu</p>
                            <div class="flex items-end"><span class="text-4xl font-extrabold tracking-tight">0 Coin</span><span class="text-sm ml-1.5 mb-1 text-gray-400 dark:text-gray-400">/tháng</span></div>
                        </div>
                        <div class="p-8 bg-white dark:bg-gray-800">
                            <ul class="space-y-4 mb-8 text-sm text-gray-600 dark:text-gray-300">
                                <li class="flex items-start text-indigo-600 dark:text-indigo-400 font-bold"><svg class="w-5 h-5 mr-3 flex-shrink-0 text-indigo-600 dark:text-indigo-400" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"></path></svg><span>Upload tối đa 50 MB / mỗi tài liệu</span></li>
                                <li class="flex items-start"><svg class="w-5 h-5 text-green-500 mr-3 flex-shrink-0" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"></path></svg><span>5 GB dung lượng lưu trữ đám mây</span></li>
                                <li class="flex items-start"><svg class="w-5 h-5 text-green-500 mr-3 flex-shrink-0" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"></path></svg><span>Tải lên tối đa 10 tài liệu/tháng</span></li>
                                <li class="flex items-start"><svg class="w-5 h-5 text-green-500 mr-3 flex-shrink-0" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"></path></svg><span>Truy cập kho tài liệu cộng đồng</span></li>
                                <li class="flex items-start"><svg class="w-5 h-5 text-green-500 mr-3 flex-shrink-0" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"></path></svg><span>AI Chatbot cơ bản (10 câu hỏi/ngày)</span></li>
                                <li class="flex items-start text-gray-400 line-through dark:text-gray-500"><svg class="w-5 h-5 text-gray-300 mr-3 flex-shrink-0 dark:text-gray-600" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"></path></svg><span>AI phân tích chuyên sâu nâng cao</span></li>
                            </ul>
                            <button disabled class="w-full py-3 bg-gray-100 text-gray-500 border border-gray-200 dark:bg-gray-700/60 dark:text-gray-400 dark:border-transparent rounded-xl font-semibold cursor-not-allowed">
                                <%= isPremiumUser ? "Gói cơ bản" : "Gói hiện tại"%>
                            </button>
                        </div>
                    </div>

                    <div class="bg-white rounded-2xl shadow-md border-2 border-[#5c3cf5] overflow-hidden relative dark:bg-gray-800 dark:border-purple-500">
                        <div class="absolute top-0 right-0 bg-gradient-to-tr from-purple-500 to-indigo-600 text-white px-4 py-1 text-xs font-bold rounded-bl-xl">Phổ biến nhất</div>
                        <div class="bg-gradient-to-tr from-purple-500 to-indigo-600 p-8 text-white">
                            <svg xmlns="http://www.w3.org/2000/svg" width="36" height="32" viewBox="0 0 24 24" fill="none" stroke="gold" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 4l3 12h14l3-12-6 7-4-7-4 7-6-7z"/><path d="M5 20h14"/></svg>
                            <h3 class="text-2xl font-bold mb-1">Premium</h3>
                            <p class="text-purple-100 text-xs mb-4">Dành cho những bước tiến xa hơn</p>
                            <div class="flex items-end"><span class="text-4xl font-extrabold tracking-tight">99.000 Coin</span><span class="text-sm ml-1.5 mb-1 text-purple-200">/tháng</span></div>
                        </div>
                        <div class="p-8 bg-white dark:bg-gray-800">
                            <ul class="space-y-4 mb-8 text-sm text-gray-600 dark:text-gray-300">
                                <li class="flex items-start font-medium text-amber-600 dark:text-amber-400"><svg class="w-5 h-5 mr-3 flex-shrink-0" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"></path></svg><span>Mở rộng Upload lên tới 100 MB / mỗi tài liệu</span></li>
                                <li class="flex items-start"><svg class="w-5 h-5 text-green-500 mr-3 flex-shrink-0" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"></path></svg><span>50 GB dung lượng lưu trữ đám mây</span></li>
                                <li class="flex items-start"><svg class="w-5 h-5 text-green-500 mr-3 flex-shrink-0" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"></path></svg><span>Tải lên & Tải xuống không giới hạn file (Lên tới 100MB/File)</span></li>
                                <li class="flex items-start"><svg class="w-5 h-5 text-green-500 mr-3 flex-shrink-0" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"></path></svg><span>Truy cập kho tài liệu cộng đồng</span></li>
                                <li class="flex items-start"><svg class="w-5 h-5 text-green-500 mr-3 flex-shrink-0" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"></path></svg><span>AI Chatbot nâng cao phản hồi không giới hạn</span></li>
                                <li class="flex items-start"><svg class="w-5 h-5 text-green-500 mr-3 flex-shrink-0" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"></path></svg><span>AI phân tích, tóm tắt và trích xuất tài liệu tự động</span></li>
                            </ul>
                            <% if (isPremiumUser) { %>
                            <button disabled class="w-full py-3 bg-gray-200 text-gray-500 dark:bg-gray-700 dark:text-gray-400 rounded-xl font-semibold cursor-not-allowed">Bạn đang sử dụng gói này</button>
                            <% } else { %>
                            <button onclick="openPurchaseModal();" class="w-full py-3 bg-gradient-to-tr from-purple-500 to-indigo-600 text-white rounded-xl font-semibold hover:opacity-90 transition-all text-sm shadow-md">Kích hoạt bằng Coin</button>
                            <% }%>
                            <div class="mt-4 flex items-center justify-center gap-2 text-xs text-gray-400 font-medium">
                                <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z"></path></svg>
                                <span>Yêu cầu 99,000 Coin</span>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="mb-24">
                    <h2 class="text-2xl font-bold text-center text-gray-900 dark:text-white mb-12">Tại sao chọn AI Study Hub Premium?</h2>
                    <div class="grid grid-cols-1 md:grid-cols-3 gap-10">
                        <div class="p-6 bg-white border border-gray-200 rounded-2xl shadow-sm text-center dark:bg-gray-800 dark:border-gray-700">
                            <div class="inline-flex items-center justify-center w-14 h-14 bg-indigo-50 rounded-2xl mb-4 text-[#5c3cf5] dark:bg-gray-700 dark:text-indigo-400">
                                <svg class="w-7 h-7" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z"></path></svg>
                            </div>
                            <h4 class="text-base font-bold text-gray-900 dark:text-white mb-1.5">AI phân tích thông minh</h4>
                            <p class="text-xs text-gray-500 leading-relaxed px-2 dark:text-gray-400">Tóm tắt tài liệu, trích xuất ý chính và kiến tạo mindmap tự động</p>
                        </div>
                        <div class="p-6 bg-white border border-gray-200 rounded-2xl shadow-sm text-center dark:bg-gray-800 dark:border-gray-700">
                            <div class="inline-flex items-center justify-center w-14 h-14 bg-indigo-50 rounded-2xl mb-4 text-[#5c3cf5] dark:bg-gray-700 dark:text-indigo-400">
                                <svg class="w-7 h-7" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
                            </div>
                            <h4 class="text-base font-bold text-gray-900 dark:text-white mb-1.5">Tốc độ xử lý ưu việt</h4>
                            <p class="text-xs text-gray-500 leading-relaxed px-2 dark:text-gray-400">Tải dữ liệu lên và xuống siêu tốc thông qua hạ tầng CDN toàn cầu</p>
                        </div>
                        <div class="p-6 bg-white border border-gray-200 rounded-2xl shadow-sm text-center dark:bg-gray-800 dark:border-gray-700">
                            <div class="inline-flex items-center justify-center w-14 h-14 bg-indigo-50 rounded-2xl mb-4 text-[#5c3cf5] dark:bg-gray-700 dark:text-indigo-400">
                                <svg class="w-7 h-7" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"></path></svg>
                            </div>
                            <h4 class="text-base font-bold text-gray-900 dark:text-white mb-1.5">Bảo mật mã hóa</h4>
                            <p class="text-xs text-gray-500 leading-relaxed px-2 dark:text-gray-400">Hệ thống mã hóa dữ liệu end-to-end, đảm bảo tài liệu cá nhân luôn an toàn</p>
                        </div>
                    </div>
                </div>

                <div class="space-y-6 max-w-4xl mx-auto mb-10 text-gray-900">
                    <div class="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm flex items-center justify-between dark:bg-gray-800 dark:border-gray-700">
                        <div class="flex items-center space-x-4">
                            <div class="w-11 h-11 bg-indigo-50 rounded-xl flex items-center justify-center text-[#5c3cf5] dark:bg-gray-700 dark:text-indigo-400">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z"></path></svg>
                            </div>
                            <div>
                                <p class="text-xs text-gray-400 font-medium dark:text-gray-500">Số dư xu hiện tại</p>
                                <p class="text-xl font-bold text-gray-900 dark:text-white"><%= String.format("%,d", userBalance)%> Coin</p>
                            </div>
                        </div>
                        <button onclick="window.location.href = '<%= request.getContextPath()%>/MainController?action=wallet'" class="px-5 py-2.5 bg-[#5c3cf5] text-white rounded-xl font-semibold hover:bg-indigo-700 text-sm transition-colors shadow-sm dark:bg-indigo-600 dark:hover:bg-indigo-700">
                            Nạp thêm Coin
                        </button>
                    </div>

                    <div class="bg-gradient-to-tr from-purple-500 to-indigo-600 rounded-2xl p-12 text-center text-white shadow-md relative overflow-hidden">
                        <h2 class="text-2xl font-bold mb-3 tracking-tight">Sẵn sàng nâng cấp?</h2>
                        <p class="text-sm text-purple-100 mb-8 max-w-xl mx-auto font-light leading-relaxed">
                            Tham gia cùng hàng nghìn sinh viên khác đang sử dụng AI Study Hub Premium để tăng tốc hiệu quả học tập đột phá.
                        </p>
                        <button onclick="openPurchaseModal();" class="px-7 py-3.5 bg-white text-[#5c3cf5] rounded-xl font-bold text-sm hover:bg-purple-50 transition-colors shadow-md">Nâng cấp ngay với Coin</button>
                    </div>
                </div>
            </div>
        </main>

        <!-- Modal xác nhận mua -->
        <div id="purchase-modal" class="hidden fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4 backdrop-blur-sm">
            <div class="bg-white rounded-2xl max-w-sm w-full shadow-2xl border border-gray-200 dark:border-gray-700 animate-[fadeIn_0.2s_ease-out]">
                <div class="p-5 border-b border-gray-200 dark:border-gray-700">
                    <h3 class="text-lg font-bold text-gray-900 dark:text-white">Xác nhận mua gói Premium</h3>
                </div>

                <div class="p-5 space-y-4">
                    <div class="bg-gradient-to-tr from-purple-500 to-indigo-600 rounded-xl p-5 text-white text-center">
                        <!-- Vương miện vàng trong modal -->
                        <svg width="40" height="36" viewBox="0 0 44 38" fill="none" xmlns="http://www.w3.org/2000/svg" class="mx-auto mb-2.5">
                        <path d="M4 32 L4 14 L13 22 L22 6 L31 22 L40 14 L40 32 Z" fill="#F59E0B"/>
                        <rect x="4" y="32" width="36" height="5" rx="2.5" fill="#D97706"/>
                        <circle cx="4" cy="14" r="3.5" fill="#FBBF24"/>
                        <circle cx="22" cy="6" r="3.5" fill="#FBBF24"/>
                        <circle cx="40" cy="14" r="3.5" fill="#FBBF24"/>
                        </svg>
                        <h4 class="text-xl font-bold mb-0.5">Premium Plan</h4>
                        <p class="text-xs text-purple-100/90">30 ngày sử dụng toàn diện</p>
                    </div>

                    <div class="space-y-2 text-xs font-medium">
                        <div class="flex items-center justify-between py-2 border-b border-gray-100 dark:border-gray-700">
                            <span class="text-gray-400">Giá kích hoạt</span>
                            <span class="font-bold text-gray-800 dark:text-gray-200"><%= String.format("%,d", premiumPrice)%> Coin</span>
                        </div>
                        <div class="flex items-center justify-between py-2 border-b border-gray-100 dark:border-gray-700">
                            <span class="text-gray-400">Số dư hiện tại</span>
                            <span class="font-bold text-gray-800 dark:text-gray-200"><%= String.format("%,d", userBalance)%> Coin</span>
                        </div>
                        <div class="flex items-center justify-between py-2">
                            <span class="text-gray-400">Số dư sau khi mua</span>
                            <span class="font-bold text-green-600 dark:text-green-400"><%= String.format("%,d", (userBalance - premiumPrice))%> Coin</span>
                        </div>
                    </div>

                    <div class="modal-alert-note bg-blue-50/80 border border-blue-100 dark:bg-gray-700/40 dark:border-gray-600 rounded-xl p-3.5 transition-all duration-200">
                        <div class="flex items-start gap-2">
                            <svg class="w-4 h-4 text-blue-600 dark:text-blue-400 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                            <div class="modal-alert-text text-[11px] text-blue-800 dark:text-blue-300 leading-relaxed font-medium">
                                <p class="font-bold mb-0.5 text-blue-900 dark:text-blue-200">Quy định gói học tập:</p>
                                <ul class="list-disc list-inside space-y-0.5">
                                    <li>Thời hạn kích hoạt: Có hiệu lực ngay lập tức</li>
                                    <li>Chu kỳ gia hạn: Không tự động trừ coin sau khi hết hạn</li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="p-5 border-t border-gray-100 dark:border-gray-700 flex gap-3">
                    <button onclick="closePurchaseModal();" class="flex-1 py-2.5 border border-gray-200 text-gray-700 rounded-xl font-semibold text-sm hover:bg-gray-50 transition-colors dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-700">Hủy</button>
                    <form class="flex-1" action="<%= request.getContextPath()%>/MainController" method="POST">
                        <input type="hidden" name="action" value="buyPremium" />
                        <button type="submit" class="w-full py-2.5 bg-gradient-to-tr from-purple-500 to-indigo-600 text-white rounded-xl font-semibold text-sm hover:opacity-90 transition-all shadow-md">Xác nhận mua</button>
                    </form>
                </div>
            </div>
        </div>

        <script>
            const userBalance = <%= userBalance%>;
            const premiumPrice = <%= premiumPrice%>;

            function openPurchaseModal() {
                if (userBalance < premiumPrice) {
                    alert("Số dư Coin trong ví không đủ! Hệ thống sẽ tự động chuyển hướng bạn đến trang nạp Coin.");
                    window.location.href = "<%= request.getContextPath()%>/MainController?action=wallet";
                    return;
                }
                document.getElementById('purchase-modal').classList.remove('hidden');
            }

            document.getElementById('purchase-modal').addEventListener('click', function (e) {
                if (e.target === this)
                    closePurchaseModal();
            });

            function closePurchaseModal() {
                document.getElementById('purchase-modal').classList.add('hidden');
            }
        </script>
    </body>
</html>
