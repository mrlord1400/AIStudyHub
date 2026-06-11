<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="Model.User" %>
<%@ page import="java.util.List" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%
    // 1. Kiểm tra đăng nhập và quyền Admin
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

    // 2. Lấy danh sách người dùng được nạp từ AdminController
    List<User> userList = (List<User>) request.getAttribute("user_list");
    
    // 3. Tính toán số lượng người dùng thực tế
    int totalUsers = 0;
    if (userList != null) {
        totalUsers = userList.size();
    }

    // Lấy dữ liệu thống kê giao dịch
    Integer totalTransactions = (Integer) request.getAttribute("totalTransactionAmount");
    if (totalTransactions == null) {
        totalTransactions = 0;
    }

    // =========================================================================
    // ĐƯA DỮ LIỆU VÀO BIẾN JSP ĐỂ DÙNG BÊN TRONG <t:AdminLayout> (TRÁNH LỖI)
    // =========================================================================
    pageContext.setAttribute("displayName", currentUsername != null ? currentUsername : "Admin");
    pageContext.setAttribute("strTotalUsers", String.format("%,d", totalUsers));
    pageContext.setAttribute("strTotalTrans", String.format("%,d", totalTransactions));
    pageContext.setAttribute("isDataMissing", userList == null);
%>

<t:AdminLayout title="Tổng quan hệ thống" activeMenu="dashboard">

    <div class="mb-8">
        <p class="text-gray-500 text-sm mt-0.5">Chào mừng quay trở lại, ${displayName}!</p>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div class="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm flex items-center justify-between">
            <div>
                <p class="text-xs font-medium text-gray-500 uppercase tracking-wider mb-1">Tổng người dùng</p>
                <h3 class="text-2xl font-bold text-gray-900">${strTotalUsers}</h3>
            </div>
            <div class="w-12 h-12 bg-indigo-100 text-indigo-600 rounded-2xl flex items-center justify-center">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 00-3-3.87"></path><path d="M16 3.13a4 4 0 010 7.75"></path></svg>
            </div>
        </div>

        <div class="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm flex items-center justify-between">
            <div>
                <p class="text-xs font-medium text-gray-500 uppercase tracking-wider mb-1">Tổng giao dịch</p>
                <h3 class="text-2xl font-bold text-gray-900">${strTotalTrans}</h3>
            </div>
            <div class="w-12 h-12 bg-emerald-100 text-emerald-600 rounded-2xl flex items-center justify-center">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/></svg>
            </div>
        </div>

        <div class="bg-indigo-600 p-6 rounded-2xl shadow-sm flex items-center justify-between border-none">
            <div>
                <p class="text-white/80 text-xs font-medium uppercase tracking-wider mb-1">Hạ tầng hệ thống</p>
                <h3 class="text-xl font-bold text-white">Hoạt động ổn định</h3>
            </div>
            <div class="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center text-white">
                <svg class="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/></svg>
            </div>
        </div>
    </div>

    <div class="bg-white p-7 rounded-2xl border border-gray-100 shadow-sm">
        <h2 class="text-lg font-bold text-gray-900 mb-2">Hệ thống phân tích AI Study Hub</h2>
        <p class="text-gray-500 text-sm leading-relaxed">
            Đây là khu vực hiển thị các thông số tổng quan lõi của ứng dụng. 
            Sử dụng menu thanh điều hướng bên trái để quản lý chi tiết danh sách tài khoản người dùng, cấu hình hệ thống, cấp bậc phân quyền hoặc kiểm duyệt các trạng thái giao dịch nạp tiền.
        </p>
    </div>

    <script>
        // Kiểm tra điều kiện nạp dữ liệu trực tiếp bằng biến EL
        if (${isDataMissing}) {
            window.location.href = "${pageContext.request.contextPath}/MainController?action=listDashboard";
        }
    </script>

</t:AdminLayout>