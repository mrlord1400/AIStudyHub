<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>

<t:AdminLayout title="Cấu hình Hệ thống" activeMenu="system-config">
    
    <div class="max-w-6xl mx-auto">
        <div class="mb-6">
            <h2 class="text-xl font-bold text-gray-800">Cấu hình các gói dịch vụ (Tiers)</h2>
            <p class="text-sm text-gray-500 mt-1">Điều chỉnh giá tiền, dung lượng lưu trữ và giới hạn AI cho từng nhóm người dùng.</p>
        </div>

        <c:if test="${not empty successMessage}">
            <div class="mb-6 p-4 bg-green-50 border border-green-200 text-green-700 rounded-xl flex items-center space-x-2">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"></path></svg>
                <span class="font-medium text-sm">${successMessage}</span>
            </div>
        </c:if>
        
        <c:if test="${not empty errorMessage}">
            <div class="mb-6 p-4 bg-red-50 border border-red-200 text-red-600 rounded-xl flex items-center space-x-2">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"></path></svg>
                <span class="font-medium text-sm">${errorMessage}</span>
            </div>
        </c:if>

        <form action="${pageContext.request.contextPath}/admin/system-config" method="POST">
            <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
                
                <c:choose>
                    <c:when test="${not empty subList}">
                        <c:forEach var="sub" items="${subList}">
                            <div class="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden relative">
                                <div class="bg-gray-50 px-6 py-4 border-b border-gray-200 flex justify-between items-center">
                                    <h3 class="text-lg font-bold text-gray-900 uppercase">${sub.tierName}</h3>
                                    <c:if test="${sub.tierName eq 'Premium'}">
                                        <span class="bg-amber-100 text-amber-700 text-xs px-2 py-1 rounded-md font-bold">PRO</span>
                                    </c:if>
                                </div>
                                
                                <div class="p-6 space-y-5">
                                    <input type="hidden" name="tierId" value="${sub.tierId}" />
                                    
                                    <div>
                                        <label class="block text-sm font-semibold text-gray-700 mb-1">Giá (VND)</label>
                                        <div class="relative">
                                            <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                                <span class="text-gray-500 sm:text-sm">$</span>
                                            </div>
                                            <input type="number" step="0.01" min="0" name="price_${sub.tierId}" value="${sub.price}" 
                                                   class="w-full pl-7 pr-4 py-2.5 rounded-xl border border-gray-300 focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none transition-all text-sm" 
                                                   <c:if test="${sub.tierName eq 'Guest' or sub.tierName eq 'Free'}">readonly class="bg-gray-100"</c:if> />
                                        </div>
                                        <c:if test="${sub.tierName eq 'Guest' or sub.tierName eq 'Free'}">
                                            <p class="text-xs text-gray-400 mt-1">Gói này mặc định miễn phí.</p>
                                        </c:if>
                                    </div>

                                    <div>
                                        <label class="block text-sm font-semibold text-gray-700 mb-1">Upload File Tối đa (MB)</label>
                                        <input type="number" min="0" name="maxStorage_${sub.tierId}" value="${sub.maxStorageMb}" required
                                               class="w-full px-4 py-2.5 rounded-xl border border-gray-300 focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none transition-all text-sm" />
                                    </div>

                                    <div>
                                        <label class="block text-sm font-semibold text-gray-700 mb-1">Tổng kho lưu trữ (MB)</label>
                                        <input type="number" min="0" name="totalStorage_${sub.tierId}" value="${sub.totalStorageMb}" required
                                               class="w-full px-4 py-2.5 rounded-xl border border-gray-300 focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none transition-all text-sm" />
                                        <p class="text-[11px] text-gray-400 mt-1">Gợi ý: 5GB = 5120MB, 50GB = 51200MB</p>
                                    </div>

                                    <div>
                                        <label class="block text-sm font-semibold text-gray-700 mb-1">Số lần hỏi AI / Ngày</label>
                                        <input type="number" min="0" name="aiLimit_${sub.tierId}" value="${sub.aiPromptLimitPerDay}" required
                                               class="w-full px-4 py-2.5 rounded-xl border border-gray-300 focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none transition-all text-sm" />
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <div class="col-span-3 text-center py-10 text-gray-500">
                            Chưa có dữ liệu. Hãy đảm bảo Database của ông đã có dữ liệu trong bảng subscriptions.
                        </div>
                    </c:otherwise>
                </c:choose>
                
            </div>

            <div class="mt-8 flex justify-end">
                <button type="submit" class="flex items-center space-x-2 px-6 py-3 bg-red-600 text-white rounded-xl font-semibold hover:bg-red-700 transition-colors shadow-sm">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4"></path></svg>
                    <span>Lưu cấu hình hệ thống</span>
                </button>
            </div>
        </form>
    </div>

</t:AdminLayout>