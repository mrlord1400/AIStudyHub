<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="Model.Transaction" %>
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

    // Phân quyền mặc định nếu chưa có
    if (role == null || role.trim().isEmpty()) {
        role = "Free";
    }
    boolean isPremiumUser = "Premium".equalsIgnoreCase(role);

    // 2. Lấy số dư ví Coin
    Integer userBalance = (Integer) userSession.getAttribute("balance");
    if (userBalance == null) {
        userBalance = 0;
    }
    
    // 3. Khởi tạo danh sách giao dịch từ TransactionController
    List<Transaction> transactionList = (List<Transaction>) request.getAttribute("transactions");
    if (transactionList == null) {
        transactionList = new ArrayList<>();
    }

    // 4. Khởi tạo các biến thống kê từ Controller gửi xuống (nếu có)
    Integer totalDeposit = (Integer) request.getAttribute("totalDeposit");
    if (totalDeposit == null) totalDeposit = 0;

    Integer totalSpent = (Integer) request.getAttribute("totalSpent");
    if (totalSpent == null) totalSpent = 0;

    Integer totalTransactions = (Integer) request.getAttribute("totalTransactions");
    if (totalTransactions == null) totalTransactions = 0;
    
    // Tự động tính toán sơ bộ bộ đếm thống kê nếu Controller chưa truyền xuống
    if (totalTransactions == 0 && !transactionList.isEmpty()) {
        totalTransactions = transactionList.size();
        for (Transaction t : transactionList) {
            if ("SUCCESS".equalsIgnoreCase(t.getStatus())) {
                if ("DEPOSIT".equalsIgnoreCase(t.getType())) {
                    totalDeposit += (int) t.getAmount();
                } else {
                    totalSpent += (int) t.getAmount();
                }
            }
        }
    }
%>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ví xu của tôi - AI Study Hub</title>
    
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
        html.dark .stat-widget { background-color: #1f2937; border-color: #374151; }
        html.dark .stat-label { color: #d1d5db; }
        html.dark .stat-value { color: #ffffff; }
        html.dark .content-box { background-color: #1f2937; border-color: #374151; }
        html.dark .content-title { color: #ffffff; }
        html.dark .empty-state-box { background-color: #1f2937 !important; border-color: #374151 !important; }
        html.dark .empty-state-icon { background-color: #374151 !important; color: #d1d5db !important; }

        html.dark #deposit-modal > div, html.dark #qr-modal > div, html.dark #admin-pending-modal > div { background-color: #1f2937 !important; color: #ffffff !important; }
        html.dark #deposit-modal h3, html.dark #qr-modal h3, html.dark #admin-pending-modal h3 { color: #ffffff !important; }
        html.dark #deposit-modal label { color: #e5e7eb !important; }
        html.dark #deposit-modal input { background-color: #374151; border-color: #4b5563; color: #ffffff; }
        html.dark .preset-btn { background-color: #1f2937; border-color: #374151; color: #e5e7eb; }
        html.dark .preset-btn:hover { border-color: #5c3cf5; background-color: #2d3748; }
        html.dark #qr-modal .bg-gray-50, html.dark #admin-pending-modal .bg-amber-50 { background-color: #2d3748 !important; }
        html.dark #qr-modal .bg-white { background-color: #1f2937 !important; border-color: #374151 !important; }
        html.dark #qr-modal span, html.dark #qr-modal p { color: #d1d5db !important; }
        html.dark #qr-modal #qr-amount-display, html.dark #qr-modal #qr-memo-text { color: #a78bfa !important; }

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
            
            .stat-widget { @apply bg-white border border-gray-100 rounded-2xl p-6 shadow-sm flex items-center justify-between transition-colors duration-200; }
            .stat-label { @apply text-sm text-gray-600 font-medium; }
            .stat-value { @apply text-xl font-bold text-gray-900; }
            .content-box { @apply bg-white border border-gray-100 rounded-2xl shadow-sm overflow-hidden transition-colors duration-200; }
            .content-title { @apply text-base font-bold text-gray-900; }
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
                <a href="<%= request.getContextPath() %>/user_dashboard.jsp" class="nav-link">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z"/></svg>
                    <span>Tài liệu của tôi</span>
                </a>
                <a href="FileExplore.jsp" class="nav-link">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
                    <span>Khám phá tài liệu</span>
                </a>
                <a href="AIChatbot.jsp" class="nav-link">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 8V4H8"/><rect width="16" height="12" x="4" y="8" rx="2"/><path d="M2 14h2"/><path d="M20 14h2"/><path d="M15 13v2"/><path d="M9 13v2"/></svg>
                    <span>AI Chatbot</span>
                </a>
                <a href="CreditWallet.jsp" class="nav-link-active">
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
                <div class="wallet-balance"><%= String.format("%,d", userBalance) %> Coin</div>
            </div>

            <div class="user-area">
                <div class="flex items-center justify-between w-full">
                    <a href="<%= request.getContextPath() %>/MainController?action=profile" class="user-profile-link flex-1 min-w-0">
                        <div class="user-avatar">
                            <%= username != null && !username.isEmpty() ? username.substring(0, 1) : "U" %>
                        </div>
                        <div class="user-info">
                            <div class="flex items-center gap-1.5 min-w-0">
                                <p class="user-name"><%= username != null ? username : "Khách" %></p>
                                <% if (isPremiumUser) { %>
                                <span class="flex-shrink-0 px-1.5 py-0.5 bg-gradient-to-r from-amber-500 to-orange-500 text-white font-bold text-[9px] rounded-full shadow-sm scale-90 origin-left">PRO</span>
                                <% } else { %>
                                <span class="flex-shrink-0 px-2 py-0.5 bg-emerald-100 text-emerald-800 border border-emerald-300 dark:bg-emerald-900/60 dark:text-emerald-400 dark:border-emerald-700 font-bold text-[10px] rounded-full shadow-sm scale-90 origin-left tracking-wide">FREE</span>
                                <% }%>
                            </div>
                            <p class="user-role">Quyền: <%= role != null ? role : "Free" %></p>
                        </div>
                    </a>
                </div>

                <a href="<%= request.getContextPath() %>/MainController?action=logout" class="logout-btn">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" x2="9" y1="12" y2="12"/></svg>
                    <span>Đăng xuất</span>
                </a>
            </div>
        </div>
    </aside>

    <main class="main-content">
        <div class="header-container">
            <div>
                <h1 class="page-title mb-1 text-white">Ví Coin hệ thống</h1>
                <p class="text-sm text-gray-500 font-medium">Sử dụng Coin để mở khóa tài liệu cao cấp và dịch vụ AI</p>
            </div>
            
            <div class="flex gap-3">
                <button onclick="openDepositModal()" class="btn-primary">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2.2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4"></path></svg>
                    <span>Nạp thêm Coin</span>
                </button>
            </div>
        </div>

        <div class="mb-8 w-full bg-gradient-to-br from-[#7c3aed] to-[#5c3cf5] text-white p-8 rounded-2xl shadow-sm relative overflow-hidden flex justify-between items-center">
            <div>
                <span class="text-sm text-purple-100 font-medium block mb-1">Số dư khả dụng</span>
                <span class="text-4xl font-extrabold tracking-tight"><%= String.format("%,d", userBalance) %> Coin</span>
            </div>
            <div class="opacity-15 hidden md:block">
                <svg class="w-24 h-24 text-white" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24"><circle cx="12" cy="12" r="8"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
            </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-5 mb-8">
            <div class="stat-widget">
                <div>
                    <p class="stat-label">Tổng nạp</p>
                    <p class="stat-value"><%= totalDeposit > 0 ? String.format("%,d", totalDeposit) + " Coin" : "0 Coin" %></p>
                </div>
            </div>
            <div class="stat-widget">
                <div>
                    <p class="stat-label">Đã tiêu dùng</p>
                    <p class="stat-value"><%= totalSpent > 0 ? String.format("%,d", totalSpent) + " Coin" : "0 Coin" %></p>
                </div>
            </div>
            <div class="stat-widget">
                <div>
                    <p class="stat-label">Tổng giao dịch</p>
                    <p class="stat-value"><%= totalTransactions %></p>
                </div>
            </div>
        </div>

        <div class="content-box">
            <div class="p-5 border-b border-gray-100 dark:border-gray-700">
                <h3 class="content-title">Nhật ký giao dịch Coin</h3>
            </div>
            <div class="divide-y divide-gray-100 dark:divide-gray-700">
                <% if (transactionList == null || transactionList.isEmpty()) { %>
                    <div class="empty-state-box p-12 flex flex-col items-center justify-center text-center">
                        <div class="empty-state-icon w-12 h-12 bg-gray-50 rounded-full flex items-center justify-center text-gray-400 mb-3 transition-colors duration-200">
                            <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                        </div>
                        <p class="text-gray-500 font-medium text-sm">Bạn chưa thực hiện bất kỳ giao dịch nào.</p>
                    </div>
                <% } else { %>
                    <div class="overflow-x-auto w-full">
                        <table class="w-full text-left text-sm border-collapse">
                            <thead>
                                <tr class="bg-gray-50 dark:bg-gray-800 text-gray-500 dark:text-gray-400 text-xs uppercase font-semibold border-b border-gray-100 dark:border-gray-700">
                                    <th class="p-4">Mã GD</th>
                                    <th class="p-4">Loại hình</th>
                                    <th class="p-4">Số lượng xu</th>
                                    <th class="p-4">Trạng thái</th>
                                </tr>
                            </thead>
                            <tbody class="divide-y divide-gray-100 dark:divide-gray-700">
                                <% for (Transaction t : transactionList) { 
                                    String transactionType = t.getType() != null ? t.getType() : "";
                                    boolean isDeposit = "nạp tiền vào ví".equalsIgnoreCase(transactionType) || "DEPOSIT".equalsIgnoreCase(transactionType);
                                %>
                                    <tr class="hover:bg-gray-50/50 dark:hover:bg-gray-800/30 transition-colors">
                                        <td class="p-4 font-mono text-xs text-gray-500 dark:text-gray-400">#<%= t.getTransactionId() != 0 ? t.getTransactionId() : "N/A" %></td>
                                        <td class="p-4 font-semibold">
                                            <% if (isDeposit) { %>
                                                <span class="text-emerald-600 dark:text-emerald-400">Nạp xu hệ thống</span>
                                            <% } else if ("WITHDRAW".equalsIgnoreCase(transactionType)) { %>
                                                <span class="text-rose-600 dark:text-rose-400">Rút tiền mặt</span>
                                            <% } else { %>
                                                <span class="text-indigo-600 dark:text-indigo-400">Nâng cấp tài khoản</span>
                                            <% } %>
                                        </td>
                                        <td class="p-4 font-bold text-base">
                                            <%= isDeposit ? "+" : "-" %> <%= String.format("%,d", (int)t.getAmount()) %> Coin
                                        </td>
                                        <td class="p-4">
                                            <% if ("SUCCESS".equalsIgnoreCase(t.getStatus())) { %>
                                                <span class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 dark:bg-emerald-950/50 dark:text-emerald-400">Thành công</span>
                                            <% } else if ("PENDING".equalsIgnoreCase(t.getStatus())) { %>
                                                <span class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-bold bg-amber-100 text-amber-800 dark:bg-amber-950/50 dark:text-amber-400">Đang chờ duyệt</span>
                                            <% } else { %>
                                                <span class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-bold bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-400">Đã hủy</span>
                                            <% } %>
                                        </td>
                                    </tr>
                                <% } %>
                            </tbody>
                        </table>
                    </div>
                <% } %>
            </div>
        </div>
    </main>

    <div id="deposit-modal" class="hidden fixed inset-0 bg-gray-900/60 flex items-center justify-center z-50 p-4 backdrop-blur-sm">
        <div class="bg-white rounded-2xl max-w-sm w-full shadow-2xl p-6 transition-all duration-200">
            <div class="flex justify-between items-center mb-4">
                <h3 class="text-lg font-bold text-gray-900">Mua thêm Coin</h3>
                <button onclick="closeDepositModal();" class="p-1 text-gray-400 hover:text-red-500 rounded-lg transition-colors">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"></path></svg>
                </button>
            </div>
            <p class="text-xs text-gray-400 font-medium mb-4">Tỷ lệ quy đổi hệ thống: 1.000đ = 1.000 Coin</p>
            
            <div class="grid grid-cols-2 gap-3 mb-5">
                <button onclick="selectPresetValue(this, 50000);" class="preset-btn py-3 border border-gray-700 rounded-xl font-bold text-sm text-gray-300 bg-gray-800 hover:border-[#5c3cf5] transition-all">50.000 Coin</button>
                <button onclick="selectPresetValue(this, 100000);" class="preset-btn py-3 border border-gray-700 rounded-xl font-bold text-sm text-gray-300 bg-gray-800 hover:border-[#5c3cf5] transition-all">100.000 Coin</button>
                <button onclick="selectPresetValue(this, 200000);" class="preset-btn py-3 border border-gray-700 rounded-xl font-bold text-sm text-gray-300 bg-gray-800 hover:border-[#5c3cf5] transition-all">200.000 Coin</button>
                <button onclick="selectPresetValue(this, 500000);" class="preset-btn py-3 border border-gray-700 rounded-xl font-bold text-sm text-gray-300 bg-gray-800 hover:border-[#5c3cf5] transition-all">500.000 Coin</button>
            </div>

            <div class="mb-6">
                <label class="block text-xs font-bold text-gray-700 mb-2">Hoặc nhập số lượng Coin khác</label>
                <input type="text" id="custom-amount-input" placeholder="Tối thiểu 10.000 Coin" class="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#5c3cf5] text-sm font-semibold bg-gray-50 focus:bg-white transition-all">
            </div>

            <div class="flex justify-end space-x-3">
                <button type="button" onclick="closeDepositModal()" class="btn-secondary px-5 py-2">Hủy</button>
                <button id="btn-submit-deposit" onclick="submitDeposit();" disabled class="px-5 py-2 bg-gray-200 text-gray-400 rounded-xl font-semibold text-sm cursor-not-allowed transition-all">Tiếp tục</button>
            </div>
        </div>
    </div>

    <div id="qr-modal" class="hidden fixed inset-0 bg-gray-900/60 flex items-center justify-center z-50 p-4 backdrop-blur-sm">
        <div class="bg-white rounded-2xl max-w-sm w-full shadow-2xl p-6 text-center transition-all duration-200">
            <div class="flex justify-between items-center mb-4">
                <h3 class="text-lg font-bold text-gray-900">Quét mã thanh toán VietQR</h3>
                <button onclick="closeQRModal();" class="p-1 text-gray-400 hover:text-red-500 rounded-lg transition-colors">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"></path></svg>
                </button>
            </div>

            <div class="bg-gray-50 p-4 rounded-xl mb-4 text-center">
                <p class="text-xs text-gray-400 font-semibold mb-0.5">Số tiền VNĐ cần thanh toán</p>
                <p id="qr-amount-display" class="text-2xl font-extrabold text-[#5c3cf5] mb-3">0đ</p>
                
                <div class="bg-white p-3 inline-block rounded-2xl border border-gray-200 shadow-sm mx-auto mb-2 relative">
                    <img id="qr-image-src" src="" alt="TPBank VietQR" class="w-48 h-48 mx-auto">
                </div>
                
                <div class="mt-2 bg-white px-3 py-2 rounded-lg border border-gray-150 text-[11px] font-semibold text-gray-700">
                    Nội dung: <span id="qr-memo-text" class="text-[#5c3cf5] font-bold">AI_Study_Hub_Nap_0_Coin</span>
                </div>
            </div>

            <div class="space-y-2">
                <form action="<%= request.getContextPath() %>/TransactionController" method="POST">
                    <input type="hidden" name="action" value="createTransaction" />
                    <input type="hidden" name="type" value="DEPOSIT" /> <input type="hidden" name="amount" id="formDepositAmount" value="0" />
                    <button type="submit" class="w-full py-3 bg-[#5c3cf5] hover:bg-indigo-700 text-white rounded-xl font-bold text-sm transition-all shadow-md">
                        Xác nhận đã chuyển khoản thành công
                    </button>
                </form>
                <button onclick="closeQRModal();" class="w-full py-2.5 border border-gray-200 text-gray-500 rounded-xl font-medium text-xs hover:bg-gray-50 transition-colors">
                    Hủy giao dịch
                </button>
            </div>
        </div>
    </div>

    <div id="admin-pending-modal" class="hidden fixed inset-0 bg-gray-900/60 flex items-center justify-center z-[60] p-4 backdrop-blur-sm">
        <div class="bg-white rounded-2xl max-w-sm w-full shadow-2xl p-6 text-center transition-all duration-200">
            <div class="w-14 h-14 bg-amber-50 rounded-full flex items-center justify-center mx-auto mb-4 text-amber-500">
                <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            </div>
            <h3 class="text-lg font-bold text-gray-900 mb-2">Hệ thống ghi nhận</h3>
            <p class="text-sm text-gray-600 font-medium leading-relaxed mb-6">Chờ Admin xác nhận giao dịch của bạn nhé!</p>
            <button onclick="closeAdminPendingModal();" class="w-full btn-primary py-3">Đồng ý</button>
        </div>
    </div>

    <script>
        let selectedAmount = 0;

        function openDepositModal() {
            selectedAmount = 0;
            document.getElementById('custom-amount-input').value = "";
            resetPresetButtons();
            toggleSubmitButton(false);
            document.getElementById('deposit-modal').classList.remove('hidden');
        }

        function closeDepositModal() {
            document.getElementById('deposit-modal').classList.add('hidden');
        }

        function selectPresetValue(buttonElement, value) {
            resetPresetButtons();
            document.getElementById('custom-amount-input').value = ""; 
            selectedAmount = value;
            
            buttonElement.className = "preset-btn py-3 border border-[#5c3cf5] !bg-[#5c3cf5] !text-white rounded-xl font-bold text-sm transition-all shadow-md shadow-indigo-600/20";
            toggleSubmitButton(true);
        }

        document.getElementById('custom-amount-input').addEventListener('input', function(e) {
            let rawValue = e.target.value.replace(/\D/g, '');
            
            if (rawValue !== "") {
                resetPresetButtons(); 
                
                let numericValue = parseInt(rawValue, 10);
                selectedAmount = numericValue;
                
                e.target.value = numericValue.toLocaleString('vi-VN');
                
                if (selectedAmount >= 10000) {
                    toggleSubmitButton(true);
                } else {
                    toggleSubmitButton(false);
                }
            } else {
                selectedAmount = 0;
                e.target.value = "";
                toggleSubmitButton(false);
            }
        });

        function toggleSubmitButton(isEnabled) {
            const btn = document.getElementById('btn-submit-deposit');
            if (isEnabled) {
                btn.disabled = false;
                btn.className = "px-5 py-2 bg-[#5c3cf5] text-white font-semibold rounded-xl text-sm hover:bg-indigo-700 transition-all cursor-pointer";
            } else {
                btn.disabled = true;
                btn.className = "px-5 py-2 bg-gray-200 text-gray-400 rounded-xl font-semibold text-sm cursor-not-allowed transition-all";
            }
        }

        function resetPresetButtons() {
            document.querySelectorAll('.preset-btn').forEach(btn => {
                btn.className = "preset-btn py-3 border border-gray-700 rounded-xl font-bold text-sm text-gray-300 bg-gray-800 hover:border-[#5c3cf5] transition-all";
            });
        }

        function submitDeposit() {
            if (selectedAmount < 10000) return;
            
            closeDepositModal(); 
            document.getElementById('qr-amount-display').innerText = selectedAmount.toLocaleString('vi-VN') + 'đ';
            document.getElementById('formDepositAmount').value = selectedAmount; 
            
            const memo = `AI_Study_Hub_Nap_\${selectedAmount}_Coin`;
            document.getElementById('qr-memo-text').innerText = memo;
            
            const bankID = "tpb";               
            const accountNo = "00003424948";    
            const template = "qr_only";          
            const encodedMemo = encodeURIComponent(memo); 
            
            const vietQRApiUrl = `https://img.vietqr.io/image/\${bankID}-\${accountNo}-\${template}.png?amount=\${selectedAmount}&addInfo=\${encodedMemo}&accountName=PHAM%20NHAT%20MINH`;
            
            document.getElementById('qr-image-src').src = vietQRApiUrl;
            document.getElementById('qr-modal').classList.remove('hidden'); 
        }

        function closeQRModal() {
            document.getElementById('qr-modal').classList.add('hidden');
            selectedAmount = 0;
        }

        function closeAdminPendingModal() {
            document.getElementById('admin-pending-modal').classList.add('hidden');
            const cleanUrl = window.location.protocol + "//" + window.location.host + window.location.pathname;
            window.history.replaceState({ path: cleanUrl }, '', cleanUrl);
        }

        // Bắt tham số từ TransactionController trả về nếu form submit thành công
        document.addEventListener("DOMContentLoaded", function() {
            const urlParams = new URLSearchParams(window.location.search);
            if (urlParams.get('transactionSuccess') === '1') {
                document.getElementById('admin-pending-modal').classList.remove('hidden');
            }
        });
    </script>
</body>
</html>