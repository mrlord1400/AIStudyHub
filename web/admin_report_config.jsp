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
        <title>Cấu hình Báo Cáo - AI Study Hub Admin</title>

        <script src="https://cdn.tailwindcss.com"></script>
        <script>
            tailwind.config = {darkMode: 'class'}
        </script>
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">

        <style type="text/tailwindcss">
            @layer components {
                .page-body {
                    @apply flex min-h-screen w-full text-gray-800 bg-[#f8f9fa] font-sans dark:bg-gray-900 dark:text-gray-100;
                }
                .sidebar {
                    @apply w-64 bg-white border-r border-gray-100 flex flex-col justify-between p-4 flex-shrink-0 min-h-screen shadow-sm dark:bg-gray-800 dark:border-gray-700;
                }
                .nav-link {
                    @apply flex items-center space-x-3 px-4 py-2.5 text-gray-600 hover:bg-gray-50 rounded-xl font-medium text-sm transition-all w-full text-left dark:text-gray-300 dark:hover:bg-gray-700;
                }
                .nav-link-active {
                    @apply flex items-center space-x-3 px-4 py-2.5 bg-indigo-50 text-indigo-600 rounded-xl font-semibold text-sm transition-colors w-full text-left dark:bg-indigo-900/50 dark:text-indigo-400;
                }
                .btn-primary {
                    @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-red-600 text-white rounded-xl font-semibold hover:bg-red-700 transition-colors text-sm shadow-sm cursor-pointer;
                }
                .btn-secondary {
                    @apply flex items-center justify-center space-x-2 px-4 py-2.5 bg-gray-200 text-gray-700 rounded-xl font-semibold hover:bg-gray-300 transition-colors text-sm shadow-sm cursor-pointer dark:bg-gray-700 dark:text-gray-200 dark:hover:bg-gray-600;
                }
                .btn-danger {
                    @apply px-3 py-1.5 bg-red-100 text-red-600 rounded-lg hover:bg-red-200 transition-colors text-xs font-bold dark:bg-red-900/30 dark:text-red-400 dark:hover:bg-red-900/50;
                }
                .btn-edit {
                    @apply px-3 py-1.5 bg-indigo-100 text-indigo-600 rounded-lg hover:bg-indigo-200 transition-colors text-xs font-bold dark:bg-indigo-900/30 dark:text-indigo-400 dark:hover:bg-indigo-900/50;
                }
                .form-input {
                    @apply w-full px-4 py-2.5 rounded-xl border border-gray-300 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-white;
                }
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
                    <a href="<%= request.getContextPath()%>/MainController?action=adminListTransactions" class="nav-link">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/></svg>
                        <span>Quản lý giao dịch</span>
                    </a>
                    <a href="<%= request.getContextPath()%>/admin/system-config" class="nav-link">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path></svg>
                        <span>Cấu hình hệ thống</span>
                    </a>
                    <a href="<%= request.getContextPath()%>/MainController?action=reportConfigList" class="nav-link-active">               <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path></svg>
                        <span>Cấu hình Báo cáo</span>
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
            <!--<div class="pt-4 border-t border-gray-100 dark:border-gray-700"></div>-->
        </aside>

        <main class="flex-1 p-8 overflow-y-auto h-screen relative">
            <div class="max-w-6xl mx-auto">
                <div class="mb-6">
                    <h2 class="text-2xl font-bold text-gray-900 tracking-tight dark:text-white">Cấu hình Quy tắc Báo cáo (Report Config)</h2>
                    <p class="text-sm text-gray-500 mt-1 dark:text-gray-400">Thiết lập các lý do báo cáo, mức độ nghiêm trọng và điểm trừ áp dụng cho tài liệu.</p>
                </div>

                <div class="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden mb-8 dark:bg-gray-800 dark:border-gray-700">
                    <div class="bg-gray-50 px-6 py-4 border-b border-gray-200 dark:bg-gray-900/50 dark:border-gray-700">
                        <h3 id="formTitle" class="text-lg font-bold text-gray-900 dark:text-white">Thêm Lý Do Mới</h3>
                    </div>
                    <div class="p-6">
                        <form id="reasonForm" action="${pageContext.request.contextPath}/MainController" method="POST">
                            <input type="hidden" id="formAction" name="action" value="reportConfigAdd">

                            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">

                                <div>
                                    <label class="block text-sm font-semibold text-gray-700 mb-1 dark:text-gray-300">Mã Lý Do (Mã khóa chính viết liền)</label>
                                    <input type="text" id="reasonCode" name="reasonCode" placeholder="VD: MALWARE, SPAM..." required class="form-input" />
                                    <p id="codeWarning" class="text-xs text-amber-500 mt-1 hidden">* Không thể thay đổi mã lý do khi đang chỉnh sửa.</p>
                                </div>

                                <div>
                                    <label class="block text-sm font-semibold text-gray-700 mb-1 dark:text-gray-300">Mức Độ Nghiêm Trọng</label>
                                    <select id="severityLevel" name="severityLevel" class="form-input">
                                        <option value="LOW">LOW (Thấp)</option>
                                        <option value="MEDIUM">MEDIUM (Trung bình)</option>
                                        <option value="HIGH">HIGH (Cao)</option>
                                        <option value="CRITICAL">CRITICAL (Nghiêm trọng)</option>
                                    </select>
                                </div>

                                <div>
                                    <label class="block text-sm font-semibold text-gray-700 mb-1 dark:text-gray-300">Điểm Phạt Cơ Sở (Mỗi lượt report)</label>
                                    <input type="number" step="0.1" id="baseScore" name="baseScore" value="1.0" required class="form-input" />
                                </div>

                                <div>
                                    <label class="block text-sm font-semibold text-gray-700 mb-1 dark:text-gray-300">Ngưỡng Tự Động Cắm Cờ</label>
                                    <input type="number" step="0.1" id="autoFlagThreshold" name="autoFlagThreshold" value="5.0" required class="form-input" />
                                </div>

                                <div class="md:col-span-2">
                                    <label class="block text-sm font-semibold text-gray-700 mb-1 dark:text-gray-300">Mô tả chi tiết</label>
                                    <input type="text" id="description" name="description" placeholder="Mô tả lý do này cho Admin dễ quản lý..." class="form-input" />
                                </div>

                            </div>

                            <div class="mt-6 flex justify-end space-x-3">
                                <button type="button" id="btnCancelEdit" class="btn-secondary hidden" onclick="cancelEditMode()">
                                    <span>Hủy Sửa</span>
                                </button>

                                <button type="submit" id="btnSubmitForm" class="btn-primary">
                                    <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4"></path></svg>
                                    <span id="btnSubmitText">Thêm Lý Do</span>
                                </button>
                            </div>
                        </form>
                    </div>
                </div>

                <div class="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden dark:bg-gray-800 dark:border-gray-700">
                    <div class="bg-gray-50 px-6 py-4 border-b border-gray-200 dark:bg-gray-900/50 dark:border-gray-700">
                        <h3 class="text-lg font-bold text-gray-900 dark:text-white">Danh sách các Quy tắc đang áp dụng</h3>
                    </div>
                    <div class="overflow-x-auto">
                        <table class="w-full text-left border-collapse">
                            <thead>
                                <tr class="bg-gray-50 border-b border-gray-200 text-gray-500 text-xs uppercase tracking-wider dark:bg-gray-900/50 dark:border-gray-700 dark:text-gray-400">
                                    <th class="p-4 font-semibold">Mã Lý Do</th>
                                    <th class="p-4 font-semibold">Mức Độ</th>
                                    <th class="p-4 font-semibold">Điểm Phạt</th>
                                    <th class="p-4 font-semibold">Ngưỡng</th>
                                    <th class="p-4 font-semibold">Mô Tả</th>
                                    <th class="p-4 font-semibold text-center">Thao Tác</th>
                                </tr>
                            </thead>
                            <tbody class="text-sm divide-y divide-gray-100 dark:divide-gray-700">
                                <c:forEach items="${reasonList}" var="reason">
                                    <tr class="hover:bg-gray-50 transition-colors dark:hover:bg-gray-700/50">
                                        <td class="p-4 font-bold text-gray-900 dark:text-white">${reason.reasonCode}</td>
                                        <td class="p-4">
                                            <span class="px-2 py-1 text-[10px] font-bold uppercase rounded-md 
                                                  ${reason.severityLevel eq 'CRITICAL' ? 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400' : 
                                                    reason.severityLevel eq 'HIGH' ? 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400' : 
                                                    reason.severityLevel eq 'MEDIUM' ? 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400' : 
                                                    'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'}">
                                                      ${reason.severityLevel}
                                                  </span>
                                            </td>
                                            <td class="p-4 text-gray-600 dark:text-gray-300 font-semibold">${reason.baseScore}</td>
                                            <td class="p-4 text-gray-600 dark:text-gray-300">${reason.autoFlagThreshold}</td>
                                            <td class="p-4 text-gray-500 dark:text-gray-400">${reason.description}</td>
                                            <td class="p-4 text-center space-x-2 flex items-center justify-center min-h-[50px]">
                                                <button type="button" class="btn-edit" 
                                                        onclick="enterEditMode('${reason.reasonCode}', '${reason.severityLevel}', '${reason.baseScore}', '${reason.autoFlagThreshold}', '${reason.description}')">
                                                    Sửa
                                                </button>

                                                <a href="${pageContext.request.contextPath}/MainController?action=reportConfigDelete&reasonCode=${reason.reasonCode}" 
                                                   class="btn-danger" 
                                                   onclick="return confirm('Bạn có chắc muốn xóa lý do [${reason.reasonCode}] không?');">
                                                    Xóa
                                                </a>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    <c:if test="${empty reasonList}">
                                        <tr>
                                            <td colspan="6" class="p-8 text-center text-gray-500 dark:text-gray-400">
                                                Chưa có quy tắc báo cáo nào trong hệ thống.
                                            </td>
                                        </tr>
                                    </c:if>
                                </tbody>
                            </table>
                        </div>
                    </div>

                </div>
            </main>

            <script>
                function enterEditMode(code, severity, baseScore, threshold, desc) {
                    // 1. Thay đổi tiêu đề form và nút bấm submit
                    document.getElementById('formTitle').innerText = "Chỉnh Sửa Quy Tắc Báo Cáo: [" + code + "]";
                    document.getElementById('btnSubmitText').innerText = "Cập Nhật Thay Đổi";

                    // 2. Thay đổi giá trị ẩn Action để gửi về doPost nhận đúng lệnh update
                    document.getElementById('formAction').value = "reportConfigUpdate";

                    // 3. Đổ ngược dữ liệu vào các ô Input
                    document.getElementById('reasonCode').value = code;
                    document.getElementById('reasonCode').readOnly = true; // Khóa trường mã lại không cho sửa
                    document.getElementById('reasonCode').classList.add('bg-gray-100', 'dark:bg-gray-800', 'cursor-not-allowed');
                    document.getElementById('codeWarning').classList.remove('hidden');

                    document.getElementById('severityLevel').value = severity;
                    document.getElementById('baseScore').value = baseScore;
                    document.getElementById('autoFlagThreshold').value = threshold;
                    document.getElementById('description').value = desc;

                    // 4. Hiện nút Hủy sửa
                    document.getElementById('btnCancelEdit').classList.remove('hidden');

                    // 5. Cuộn màn hình nhẹ lên trên để admin tập trung nhìn vào Form
                    window.scrollTo({top: 0, behavior: 'smooth'});
                }

                function cancelEditMode() {
                    // Trả Form về trạng thái Thêm Mới ban đầu
                    document.getElementById('formTitle').innerText = "Thêm Lý Do Mới";
                    document.getElementById('btnSubmitText').innerText = "Thêm Lý Do";
                    document.getElementById('formAction').value = "reportConfigAdd";

                    // Reset form và mở khóa ô mã lý do
                    document.getElementById('reasonForm').reset();
                    document.getElementById('reasonCode').readOnly = false;
                    document.getElementById('reasonCode').classList.remove('bg-gray-100', 'dark:bg-gray-800', 'cursor-not-allowed');
                    document.getElementById('codeWarning').classList.add('hidden');

                    // Ẩn nút hủy sửa
                    document.getElementById('btnCancelEdit').classList.add('hidden');
                }
            </script>
        </body>
    </html>