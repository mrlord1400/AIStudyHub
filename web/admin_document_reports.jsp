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
        <title>Chi Tiết Báo Cáo Tài Liệu - AI Study Hub Admin</title>

        <script src="https://cdn.tailwindcss.com"></script>
        <script>
            tailwind.config = {darkMode: 'class'}
        </script>
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">

        <style type="text/tailwindcss">
            @layer components {
                .page-body { @apply flex min-h-screen w-full text-gray-800 bg-[#f8f9fa] font-sans dark:bg-gray-900 dark:text-gray-100; }
                .sidebar { @apply w-64 bg-white border-r border-gray-100 flex flex-col justify-between p-4 flex-shrink-0 min-h-screen shadow-sm dark:bg-gray-800 dark:border-gray-700; }
                .nav-link { @apply flex items-center space-x-3 px-4 py-2.5 text-gray-600 hover:bg-gray-50 rounded-xl font-medium text-sm transition-all w-full text-left dark:text-gray-300 dark:hover:bg-gray-700; }
                .btn-danger-big { @apply px-6 py-3 bg-red-600 text-white rounded-xl font-bold text-sm shadow-md hover:bg-red-700 hover:shadow-lg transition-all flex items-center gap-2 uppercase tracking-wide cursor-pointer; }
                .btn-success { @apply px-3 py-1.5 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors text-xs font-bold shadow-sm cursor-pointer; }
                .btn-secondary { @apply px-3 py-1.5 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors text-xs font-bold shadow-sm dark:bg-gray-700 dark:text-gray-200 dark:hover:bg-gray-600 cursor-pointer; }
            }
        </style>
    </head>
    <body class="page-body">

        <aside class="sidebar">
            <div class="space-y-6 w-full">
                <a href="${pageContext.request.contextPath}/MainController?action=listDashboard" class="flex items-center space-x-3 px-2 py-1 transition-opacity hover:opacity-80 block w-full">
                    <div class="w-9 h-9 bg-red-600 rounded-xl flex items-center justify-center text-white shadow-sm">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path></svg>
                    </div>
                    <span class="font-bold text-gray-900 text-base tracking-tight dark:text-white">Admin Panel</span>
                </a>

                <nav class="space-y-1 w-full">
                    <a href="${pageContext.request.contextPath}/MainController?action=listDashboard" class="nav-link">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M11 15l-3-3m0 0l3-3m-3 3h8M3 12a9 9 0 1118 0 9 9 0 01-18 0z"></path></svg>
                        <span>Quay lại Dashboard</span>
                    </a>
                </nav>
            </div>
            <div class="pt-4 border-t border-gray-100 dark:border-gray-700"></div>
        </aside>

        <main class="flex-1 p-8 overflow-y-auto h-screen relative">
            <div class="max-w-6xl mx-auto">
                
                <c:if test="${param.error eq 'delete_failed'}">
                    <div class="mb-6 p-4 bg-red-50 border border-red-200 text-red-600 rounded-xl flex items-center space-x-2 dark:bg-red-950/40 dark:border-red-900 dark:text-red-400">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                        <span class="font-medium text-sm">Lỗi: Không thể xóa tài liệu này, vui lòng thử lại!</span>
                    </div>
                </c:if>

                <div class="flex flex-col md:flex-row justify-between items-start md:items-center mb-8 bg-white p-6 rounded-2xl shadow-sm border border-gray-200 dark:bg-gray-800 dark:border-gray-700 gap-4">
                    <div>
                        <h2 class="text-2xl font-bold text-gray-900 tracking-tight dark:text-white">
                            Hồ Sơ Xét Xử Tài Liệu ID: #${document.documentId}
                        </h2>
                        <div class="text-sm mt-3 flex flex-wrap gap-3">
                            <span class="bg-gray-100 text-gray-700 px-3 py-1 rounded-md font-semibold dark:bg-gray-700 dark:text-gray-300">Tổng điểm phạt: <span class="text-red-500">${document.totalReportScore}</span></span>
                            
                            <c:if test="${document.isFlagged}">
                                <span class="bg-red-100 text-red-700 px-3 py-1 rounded-md font-bold flex items-center gap-1 dark:bg-red-900/30 dark:text-red-400">
                                    <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path></svg>
                                    ĐANG BỊ CẮM CỜ VI PHẠM
                                </span>
                            </c:if>
                        </div>
                    </div>
                    
                    <form action="${pageContext.request.contextPath}/MainController" method="POST" 
                          onsubmit="return confirm('🚨 CẢNH BÁO NGUY HIỂM 🚨\n\nHành động này sẽ XÓA VĨNH VIỄN tài liệu #${document.documentId} cùng với toàn bộ Text, Bookmark và lịch sử Báo cáo liên quan khỏi CSDL.\n\nÔng có chắc chắn muốn xóa không?');">
                        <input type="hidden" name="action" value="adminDeleteDocument">
                        <input type="hidden" name="documentId" value="${document.documentId}">
                        <button type="submit" class="btn-danger-big">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                            TIÊU HỦY TÀI LIỆU
                        </button>
                    </form>
                </div>

                <div class="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden dark:bg-gray-800 dark:border-gray-700">
                    <div class="bg-gray-50 px-6 py-4 border-b border-gray-200 dark:bg-gray-900/50 dark:border-gray-700">
                        <h3 class="text-lg font-bold text-gray-900 dark:text-white">Chi tiết các lượt Báo cáo từ cộng đồng</h3>
                    </div>
                    <div class="overflow-x-auto">
                        <table class="w-full text-left border-collapse">
                            <thead>
                                <tr class="bg-gray-50 border-b border-gray-200 text-gray-500 text-xs uppercase tracking-wider dark:bg-gray-900/50 dark:border-gray-700 dark:text-gray-400">
                                    <th class="p-4 font-semibold">Ngày Report</th>
                                    <th class="p-4 font-semibold">User ID</th>
                                    <th class="p-4 font-semibold text-red-500">Mã Vi Phạm</th>
                                    <th class="p-4 font-semibold">Lý do chi tiết</th>
                                    <th class="p-4 font-semibold text-center">Trạng Thái</th>
                                    <th class="p-4 font-semibold text-center">Xử Lý</th>
                                </tr>
                            </thead>
                            <tbody class="text-sm divide-y divide-gray-100 dark:divide-gray-700">
                                <c:forEach items="${documentReports}" var="rep">
                                    <tr class="hover:bg-gray-50 transition-colors dark:hover:bg-gray-700/50">
                                        <td class="p-4 text-gray-500 font-medium whitespace-nowrap">${rep.createdAt}</td>
                                        <td class="p-4 font-bold text-indigo-600 dark:text-indigo-400">#${rep.reporterId}</td>
                                        <td class="p-4 font-bold text-red-600 dark:text-red-400">${rep.reasonCode}</td>
                                        <td class="p-4 text-gray-600 dark:text-gray-300 max-w-xs break-words">${rep.details}</td>
                                        <td class="p-4 text-center">
                                            <span class="px-2 py-1 text-[10px] font-bold uppercase rounded-md 
                                                  ${rep.status eq 'PENDING' ? 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400' : 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'}">
                                                ${rep.status}
                                            </span>
                                        </td>
                                        <td class="p-4 text-center space-x-2 flex items-center justify-center">
                                            
                                            <c:if test="${rep.status eq 'PENDING'}">
                                                <form action="${pageContext.request.contextPath}/MainController" method="POST" class="inline">
                                                    <input type="hidden" name="action" value="adminUpdateReportStatus">
                                                    <input type="hidden" name="documentId" value="${document.documentId}">
                                                    <input type="hidden" name="reportId" value="${rep.reportId}">
                                                    <input type="hidden" name="status" value="REVIEWED">
                                                    <button type="submit" class="btn-success">Duyệt</button>
                                                </form>
                                            </c:if>

                                            <c:if test="${rep.status ne 'PENDING'}">
                                                <span class="text-xs text-green-500 font-semibold">✓ Đã duyệt</span>
                                            </c:if>
                                            
                                            <form action="${pageContext.request.contextPath}/MainController" method="POST" class="inline" onsubmit="return confirm('Bạn có chắc chắn muốn xóa bỏ báo cáo này không? Điểm phạt sẽ được tự động hoàn lại cho tài liệu.');">
                                                <input type="hidden" name="action" value="adminDeleteReport">
                                                <input type="hidden" name="documentId" value="${document.documentId}">
                                                <input type="hidden" name="reportId" value="${rep.reportId}">
                                                <button type="submit" class="btn-secondary ml-2">Xóa</button>
                                            </form>
                                            
                                        </td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty documentReports}">
                                    <tr>
                                        <td colspan="6" class="p-8 text-center text-gray-500 dark:text-gray-400 font-medium">
                                            Chưa có bất kỳ báo cáo nào cho tài liệu này!
                                        </td>
                                    </tr>
                                </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>

            </div>
        </main>
    </body>
</html>