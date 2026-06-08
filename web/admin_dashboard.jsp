<%-- 
    Document   : admin_dashboard
    Created on : 29/05/2026, 11:23:30 AM
    Author     : Admin
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%-- Khai báo gọi thư mục chứa file Layout --%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%-- Gọi Layout và truyền tham số: Tiêu đề trang & Menu đang active --%>
<t:AdminLayout title="Tổng quan Dashboard" activeMenu="dashboard">
    
    <%-- MỌI THỨ BẠN VIẾT Ở ĐÂY SẼ HIỂN THỊ VÀO PHẦN MAIN CONTENT CỦA LAYOUT --%>
    
    <div class="space-y-6">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
            
            <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-100 flex items-center justify-between">
                <div>
                    <p class="text-sm font-medium text-gray-500 mb-1">Tổng số User</p>
                    <h3 class="text-2xl font-bold text-gray-900">1,248</h3>
                </div>
                <div class="w-12 h-12 bg-blue-50 rounded-full flex items-center justify-center text-blue-600">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 00-3-3.87"></path><path d="M16 3.13a4 4 0 010 7.75"></path></svg>
                </div>
            </div>

            <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-100 flex items-center justify-between">
                <div>
                    <p class="text-sm font-medium text-gray-500 mb-1">Tài liệu đã tải lên</p>
                    <h3 class="text-2xl font-bold text-gray-900">856</h3>
                </div>
                <div class="w-12 h-12 bg-green-50 rounded-full flex items-center justify-center text-green-600">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
                </div>
            </div>

            <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-100 flex items-center justify-between">
                <div>
                    <p class="text-sm font-medium text-gray-500 mb-1">Giao dịch hôm nay</p>
                    <h3 class="text-2xl font-bold text-gray-900">24</h3>
                </div>
                <div class="w-12 h-12 bg-purple-50 rounded-full flex items-center justify-center text-purple-600">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><line x1="12" y1="1" x2="12" y2="23"></line><path d="M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"></path></svg>
                </div>
            </div>
            
        </div>

        <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
            <h2 class="text-lg font-semibold text-gray-800 mb-4">Chào mừng trở lại, Admin!</h2>
            <p class="text-gray-600">Đây là khu vực hiển thị các thông số tổng quan của AI Study Hub. Bạn có thể chọn các chức năng quản lý chi tiết ở thanh menu bên trái.</p>
        </div>
    </div>

</t:AdminLayout>