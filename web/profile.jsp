<%@page import="Model.DAO.UserDAO"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="Model.DTO.User" %>
<%
    HttpSession userSession = request.getSession(false);
    if (userSession == null || userSession.getAttribute("userId") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    // Safety net: If accessed directly, route back to the controller to fetch data
    User currentUser = (User) request.getAttribute("currentUser");
    if (currentUser == null) {
        response.sendRedirect(request.getContextPath() + "/MainController?action=profile");
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

    // Status Parameters
    String error = request.getParameter("error");
    String updateSuccess = request.getParameter("updateSuccess");
%>
<!DOCTYPE html>
<html lang="vi" class="dark">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Hồ sơ của tôi - AI Study Hub</title>

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
            html.dark .form-card {
                background-color: #1f2937;
                border-color: #374151;
            }
            html.dark .form-input {
                background-color: #374151;
                border-color: #4b5563;
                color: #ffffff;
            }
            html.dark .form-input:focus {
                background-color: #1f2937;
            }
            html.dark .tab-btn-inactive {
                color: #9ca3af;
            }
            html.dark .tab-btn-inactive:hover {
                color: #f3f4f6;
                background-color: #374151;
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
                    @apply flex items-center space-x-3 px-2 py-2 rounded-xl bg-gray-50 dark:bg-gray-700 transition-colors cursor-pointer;
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
                    @apply bg-white border border-gray-100 rounded-2xl p-7 shadow-sm max-w-3xl mb-6 transition-colors duration-200;
                }
                .form-label {
                    @apply block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2;
                }
                .form-input {
                    @apply w-full px-4 py-3 rounded-xl border border-gray-200 outline-none transition-all bg-gray-50 focus:bg-white text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500;
                }
                .btn-primary {
                    @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-[#5c3cf5] text-white rounded-xl font-semibold hover:bg-indigo-700 transition-colors text-sm shadow-sm shadow-indigo-100 cursor-pointer;
                }
                .btn-secondary {
                    @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-white border border-gray-200 text-gray-700 rounded-xl font-semibold hover:bg-gray-50 transition-colors text-sm shadow-sm cursor-pointer dark:bg-gray-800 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-700;
                }
                .btn-danger {
                    @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-red-500 text-white rounded-xl font-semibold hover:bg-red-600 transition-colors text-sm shadow-sm shadow-red-100 cursor-pointer;
                }

                .tab-btn-active {
                    @apply px-5 py-2.5 bg-indigo-600 text-white font-semibold text-sm rounded-xl shadow-sm transition-all;
                }
                .tab-btn-inactive {
                    @apply px-5 py-2.5 text-gray-600 hover:bg-gray-100 font-medium text-sm rounded-xl transition-all;
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
                    <a href="FileExplore.jsp" class="nav-link">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
                        <span>Khám phá tài liệu</span>
                    </a>
                    <a href="<%= request.getContextPath()%>/MainController?action=chatMain" class="nav-link">
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
                    <a href="#" class="user-profile-link">
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
                    <a href="<%= request.getContextPath()%>/MainController?action=logout" class="logout-btn">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" x2="9" y1="12" y2="12"/></svg>
                        <span>Đăng xuất</span>
                    </a>
                </div>
            </div>
        </aside>

        <main class="main-content">
            <div class="header-container max-w-3xl">
                <h1 class="page-title dark:text-white">Hồ sơ cá nhân</h1>
                <!-- Button quay lại Dashboard ở Header -->
                <a href="<%= request.getContextPath()%>/user_dashboard.jsp" class="btn-secondary !px-4 !py-2 flex items-center space-x-1.5">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path></svg>
                    <span>Dashboard</span>
                </a>
            </div>

            <% if ("wrong_password".equals(error)) { %>
            <div class="max-w-3xl mb-6 p-4 bg-red-50 border border-red-200 text-red-600 rounded-xl text-sm font-medium dark:bg-red-950/40 dark:border-red-900 dark:text-red-400">Mật khẩu hiện tại không chính xác. Không thể lưu thay đổi.</div>
            <% } else if ("update_failed".equals(error)) { %>
            <div class="max-w-3xl mb-6 p-4 bg-red-50 border border-red-200 text-red-600 rounded-xl text-sm font-medium dark:bg-red-950/40 dark:border-red-900 dark:text-red-400">Lỗi hệ thống: Không thể cập nhật thông tin.</div>
            <% } else if ("1".equals(updateSuccess)) { %>
            <div class="max-w-3xl mb-6 p-4 bg-green-50 border border-green-200 text-green-700 rounded-xl text-sm font-medium dark:bg-green-950/40 dark:border-green-900 dark:text-green-400">Cập nhật thông tin thành công!</div>
            <% }%>

            <!-- Hệ thống Tab điều hướng -->
            <div class="flex space-x-2 max-w-3xl mb-6 bg-gray-100 dark:bg-gray-800 p-1.5 rounded-2xl w-fit">
                <button type="button" id="tab-info-btn" onclick="switchTab('info')" class="tab-btn-active">Thông tin cá nhân</button>
                <button type="button" id="tab-security-btn" onclick="switchTab('security')" class="tab-btn-inactive">Bảo mật & Mật khẩu</button>
            </div>

            <form action="<%= request.getContextPath()%>/MainController" method="POST">
                <input type="hidden" name="action" value="updateProfile" />

                <!-- TAB 1: THÔNG TIN CÁ NHÂN -->
                <div id="tab-info-content" class="form-card">
                    <div class="mb-6 pb-6 border-b border-gray-100 dark:border-gray-700 flex items-start space-x-4">
                        <div class="p-3 bg-indigo-50 dark:bg-indigo-950/50 rounded-xl text-indigo-600 dark:text-indigo-400">
                            <svg class="w-8 h-8" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path></svg>
                        </div>
                        <div>
                            <h2 class="text-lg font-bold text-gray-900 dark:text-white">Thông tin cơ bản</h2>
                            <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">Cập nhật tên hiển thị công khai và địa chỉ email hệ thống của bạn.</p>
                        </div>
                    </div>

                    <div class="grid grid-cols-1 sm:grid-cols-2 gap-5 mb-6">
                        <div>
                            <label class="form-label">Tên hiển thị</label>
                            <input type="text" name="username" value="<%= currentUser.getUsername()%>" required class="form-input" />
                        </div>
                        <div>
                            <label class="form-label">Email</label>
                            <input type="email" name="email" value="<%= currentUser.getEmail()%>" required class="form-input" />
                        </div>
                    </div>

                    <div class="p-4 bg-gray-50 dark:bg-gray-800/50 rounded-xl border border-gray-200 dark:border-gray-700">
                        <label class="form-label">Mật khẩu hiện tại <span class="text-red-500">*</span></label>
                        <input type="password" name="currentPassword" required class="form-input bg-white dark:bg-gray-700" placeholder="Bắt buộc nhập mật khẩu hiện tại để xác nhận lưu thay đổi" />
                    </div>

                    <div class="mt-6 flex justify-end space-x-3">
                        <a href="<%= request.getContextPath()%>/user_dashboard.jsp" class="btn-secondary">Hủy bỏ</a>
                        <button type="submit" class="btn-primary">Lưu thông tin</button>
                    </div>
                </div>

                <!-- TAB 2: BẢO MẬT & MẬT KHẨU -->
                <div id="tab-security-content" class="form-card hidden">
                    <div class="mb-6 pb-6 border-b border-gray-100 dark:border-gray-700 flex items-start space-x-4">
                        <div class="p-3 bg-indigo-50 dark:bg-indigo-950/50 rounded-xl text-indigo-600 dark:text-indigo-400">
                            <svg class="w-8 h-8" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 15v2m-6 4h12a2 2 0 0 0 2-2v-6a2 2 0 0 0-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path></svg>
                        </div>
                        <div>
                            <h2 class="text-lg font-bold text-gray-900 dark:text-white">Đổi mật khẩu tài khoản</h2>
                            <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">Đảm bảo tài khoản sử dụng mật khẩu mạnh để bảo mật an toàn dữ liệu học tập.</p>
                        </div>
                    </div>

                    <div class="space-y-5">
                        <div>
                            <label class="form-label text-indigo-600 dark:text-indigo-400">Mật khẩu mới</label>
                            <input type="password" name="newPassword" class="form-input" placeholder="Nhập mật khẩu mới muốn thay đổi" />
                        </div>

                        <div class="p-4 bg-gray-50 dark:bg-gray-800/50 rounded-xl border border-gray-200 dark:border-gray-700">
                            <label class="form-label">Mật khẩu hiện tại <span class="text-red-500">*</span></label>
                            <input type="password" name="currentPassword" disabled class="form-input bg-white dark:bg-gray-700" placeholder="Bắt buộc nhập mật khẩu hiện tại để xác nhận đổi" />
                        </div>
                    </div>

                    <div class="mt-6 flex justify-end space-x-3">
                        <a href="<%= request.getContextPath()%>/user_dashboard.jsp" class="btn-secondary">Hủy bỏ</a>
                        <button type="submit" class="btn-primary">Cập nhật mật khẩu</button>
                    </div>
                </div>
            </form>

            <!-- KHU VỰC NGUY HIỂM (Chỉ hiện khi ở tab bảo mật) -->
            <div id="danger-zone-content" class="form-card border-red-200 bg-red-50/30 dark:border-red-900/50 dark:bg-red-950/10 hidden max-w-3xl">
                <div class="flex flex-col sm:flex-row items-start justify-between gap-4">
                    <div>
                        <h2 class="text-lg font-bold text-red-600 dark:text-red-400">Khu vực nguy hiểm</h2>
                        <p class="text-sm text-red-500 dark:text-red-400/80 mt-1 max-w-xl">Xóa tài khoản của bạn sẽ xóa vĩnh viễn mọi dữ liệu, tài liệu, và lịch sử trò chuyện. Hành động này không thể hoàn tác.</p>
                    </div>
                    <form action="<%= request.getContextPath()%>/MainController" method="POST" onsubmit="return confirm('CẢNH BÁO: Bạn có chắc chắn muốn xóa vĩnh viễn tài khoản này không? Dữ liệu không thể phục hồi.');">
                        <input type="hidden" name="action" value="deleteAccount" />
                        <button type="submit" class="btn-danger w-full sm:w-auto">Xóa tài khoản</button>
                    </form>
                </div>
            </div>

        </main>

        <script>
            function switchTab(tab) {
                const infoBtn = document.getElementById('tab-info-btn');
                const securityBtn = document.getElementById('tab-security-btn');
                const infoContent = document.getElementById('tab-info-content');
                const securityContent = document.getElementById('tab-security-content');
                const dangerZone = document.getElementById('danger-zone-content');

                // Lấy chính xác 2 thẻ input
                const infoCurrentPass = infoContent.querySelector('input[name="currentPassword"]');
                const securityCurrentPass = securityContent.querySelector('input[name="currentPassword"]');

                if (tab === 'info') {
                    // Đổi giao diện
                    infoBtn.className = "tab-btn-active";
                    securityBtn.className = "tab-btn-inactive";

                    infoContent.classList.remove('hidden');
                    securityContent.classList.add('hidden');
                    dangerZone.classList.add('hidden');

                    // BẬT input của tab Info, TẮT input của tab Security
                    infoCurrentPass.required = true;
                    infoCurrentPass.disabled = false;

                    securityCurrentPass.required = false;
                    securityCurrentPass.disabled = true; // Trình duyệt sẽ bỏ qua thẻ này khi submit

                } else if (tab === 'security') {
                    // Đổi giao diện
                    infoBtn.className = "tab-btn-inactive";
                    securityBtn.className = "tab-btn-active";

                    infoContent.classList.add('hidden');
                    securityContent.classList.remove('hidden');
                    dangerZone.classList.remove('hidden');

                    // TẮT input của tab Info, BẬT input của tab Security
                    infoCurrentPass.required = false;
                    infoCurrentPass.disabled = true; // Trình duyệt sẽ bỏ qua thẻ này khi submit

                    securityCurrentPass.required = true;
                    securityCurrentPass.disabled = false;
                }
            }
        </script>
    </body>
</html>