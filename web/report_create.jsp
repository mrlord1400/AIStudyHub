<%@page import="Model.DTO.User"%>
<%@page import="Model.DAO.UserDAO"%>
<%@page import="Model.DTO.ReportReason"%>
<%@page import="Model.DAO.ReportReasonDAO"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List" %>
<%
    // 1. Kiểm tra trạng thái đăng nhập hệ thống (Security Guard)
    HttpSession userSession = request.getSession(false);
    if (userSession == null || userSession.getAttribute("userId") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    // 2. Thu thập dữ liệu tài khoản người dùng hiển thị Sidebar
    Integer userId = (Integer) userSession.getAttribute("userId");
    String username = (String) userSession.getAttribute("username");
    String role = (String) userSession.getAttribute("role");
    Integer tierId = (Integer) userSession.getAttribute("tierId");

    if (role == null || !"ADMIN".equalsIgnoreCase(role.trim())) {
        role = "STUDENT";
    } else {
        role = "ADMIN";
    }

    if (tierId == null || tierId < 2) {
        tierId = 2;
    }
    boolean isPremiumUser = (tierId >= 3);

    // Lấy số dư ví thực tế từ Database
    UserDAO userDAO = new UserDAO();
    int userBalance = 0;
    if (userId != null) {
        User user = userDAO.getUserById(userId);
        if (user != null) {
            userBalance = user.getBalance();
        }
    }

    // 3. Đọc thông tin Document từ Request Attributes (được truyền từ ReportController)
    Integer documentId = (Integer) request.getAttribute("documentId");
    String errorMessage = (String) request.getAttribute("errorMessage");

    // 🔥 CƠ CHẾ BẢO VỆ: Nếu truy cập trực tiếp file jsp mà thiếu documentId, quay về trang explore
    if (documentId == null) {
        response.sendRedirect(request.getContextPath() + "/MainController?action=explore");
        return;
    }

    // 4. Lấy danh sách cấu hình lý do báo cáo để hiển thị lên thẻ <select>
    ReportReasonDAO reasonDAO = new ReportReasonDAO();
    List<ReportReason> reasonConfigs = reasonDAO.getAllReportReason();
%>
<!DOCTYPE html>
<html lang="vi" class="dark">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Báo cáo vi phạm tài liệu - AI Study Hub</title>

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

            /* Định dạng Form Card tương tự cấu trúc upload */
            html.dark .form-card {
                background-color: #1f2937;
                border-color: #374151;
            }
            html.dark .form-label {
                color: #e5e7eb;
            }
            html.dark .form-input {
                background-color: #374151;
                border-color: #4b5563;
                color: #ffffff;
            }
            html.dark .form-input:focus {
                background-color: #1f2937;
                border-color: #ef4444;
            }
            html.dark .btn-secondary {
                background-color: #1f2937;
                border-color: #374151;
                color: #e5e7eb;
            }
            html.dark .btn-secondary:hover {
                background-color: #374151;
            }
            html.dark .btn-danger {
                background-color: rgba(239, 68, 68, 0.1);
                border-color: #ef4444;
                color: #f87171;
            }
            html.dark .btn-danger:hover {
                background-color: #ef4444;
                color: white;
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
                .form-card {
                    @apply bg-white border border-gray-100 rounded-2xl p-7 shadow-sm max-w-3xl w-full;
                }
                .form-label {
                    @apply block text-sm font-semibold text-gray-700 mb-2;
                }
                .form-input {
                    @apply w-full px-4 py-3 rounded-xl border border-gray-200 focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none transition-all bg-gray-50 focus:bg-white text-sm;
                }
                .btn-primary {
                    @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-red-600 text-white rounded-xl font-semibold hover:bg-red-700 transition-colors text-sm shadow-sm cursor-pointer;
                }
                .btn-secondary {
                    @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-white border border-gray-200 text-gray-700 rounded-xl font-semibold hover:bg-gray-50 transition-colors text-sm shadow-sm cursor-pointer;
                }
                .btn-danger {
                    @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-white border border-red-200 text-red-600 rounded-xl font-semibold hover:bg-red-50 transition-colors text-sm cursor-pointer;
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
                                    <p class="user-name truncate"><%= username != null && !username.trim().isEmpty() ? username : "Học viên"%></p>
                                    <% if (isPremiumUser) { %>
                                    <span class="flex-shrink-0 px-1.5 py-0.5 bg-gradient-to-r from-amber-500 to-orange-500 text-white font-bold text-[9px] rounded-full shadow-sm scale-90 origin-left">PRO</span>
                                    <% } else { %>
                                    <span class="flex-shrink-0 px-2 py-0.5 bg-emerald-100 text-emerald-800 border border-emerald-300 dark:bg-emerald-900/60 dark:text-emerald-400 dark:border-emerald-700 font-bold text-[10px] rounded-full shadow-sm scale-90 origin-left tracking-wide">FREE</span>
                                    <% }%>
                                </div>
                                <p class="user-role">Quyền: <%= role%></p>
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
            <div class="header-container max-w-3xl">
                <h1 class="page-title dark:text-white">Báo cáo tài liệu vi phạm</h1>
                <a href="<%= request.getContextPath()%>/MainController?action=explore" class="flex items-center space-x-1.5 px-4 py-2 bg-gray-800 border border-gray-700 text-gray-300 hover:text-white hover:bg-gray-700 rounded-xl font-semibold text-sm transition-all shadow-sm">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path></svg>
                    <span>Quay lại Khám phá</span>
                </a>
            </div>

            <% if (errorMessage != null) {%>
            <div class="max-w-3xl mb-6 p-4 bg-red-950/40 border border-red-800 text-red-400 rounded-xl text-sm font-medium">
                <%= errorMessage%>
            </div>
            <% }%>

            <div class="form-card">
                <div class="mb-6 pb-6 border-b border-gray-700 flex items-start space-x-4">
                    <div class="p-3 bg-red-950/50 border border-red-900 rounded-xl text-red-500">
                        <svg class="w-8 h-8" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
                        </svg>
                    </div>
                    <div>
                        <h2 class="text-lg font-bold text-white">Gửi báo cáo nội dung vi phạm</h2>
                        <p class="text-sm text-gray-400 mt-1">
                            Bạn đang báo cáo mã tài liệu: <strong class="text-red-400">#<%= documentId%></strong>. Vui lòng chọn lý do chính xác để Đội ngũ Kiểm duyệt xử lý nhanh chóng.
                        </p>
                    </div>
                </div>

                <form id="createReportForm" action="<%= request.getContextPath()%>/MainController" method="POST">
                    <input type="hidden" name="action" value="createReport" />
                    <input type="hidden" name="documentId" value="<%= documentId%>" />

                    <div class="space-y-5">
                        <div>
                            <label class="form-label">Lý do vi phạm <span class="text-red-500">*</span></label>
                            <select name="reasonCode" required class="form-input">
                                <option value="">-- Chọn lý do vi phạm phù hợp --</option>
                                <%
                                    if (reasonConfigs != null) {
                                        for (ReportReason configs : reasonConfigs) {
                                %>
                                <option value="<%= configs.getReasonCode()%>">
                                    [<%= configs.getReasonCode()%>] <%= configs.getBaseScore()%> <%= configs.getDescription()%> (Mức độ: <%= configs.getSeverityLevel()%>)
                                </option>
                                <%
                                        }
                                    }
                                %>
                            </select>
                        </div>

                        <div>
                            <label class="form-label">Mô tả chi tiết bằng chứng vi phạm <span class="text-red-500">*</span></label>
                            <textarea name="details" required rows="4" class="form-input resize-none" placeholder="Vui lòng cung cấp thêm thông tin chi tiết..."></textarea>
                        </div>
                    </div>

                    <div class="mt-8 pt-6 border-t border-gray-700 flex items-center justify-end space-x-3">
                        <button type="button" onclick="window.location.href = '<%= request.getContextPath()%>/MainController?action=explore';" class="btn-secondary">
                            Hủy bỏ
                        </button>
                        <button type="submit" class="btn-primary">
                            Gửi báo cáo
                        </button>
                    </div>
                </form>
            </div>
        </main>
    </body>
</html>