<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="Model.Document" %>
<%
    // 1. Kiểm tra trạng thái đăng nhập của người dùng
    HttpSession userSession = request.getSession(false);
    if (userSession == null || userSession.getAttribute("userId") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    int userId = (Integer) userSession.getAttribute("userId");
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
    
    Integer userBalance = (Integer) userSession.getAttribute("balance");
    if (userBalance == null) {
        userBalance = 0;
    }

    // 2. Lấy danh sách bài đăng thực tế từ Controller gửi xuống
    List<Document> publicDocuments = (List<Document>) request.getAttribute("publicDocuments");
    if (publicDocuments == null) {
        publicDocuments = new ArrayList<>();
    }

    // Đọc số liệu thống kê thực tế từ DB
    Integer totalDocs = (Integer) request.getAttribute("realTotalDocs");
    Integer totalContributors = (Integer) request.getAttribute("realTotalContributors");
    Integer totalDownloads = (Integer) request.getAttribute("realTotalDownloads");
%>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Khám phá tài liệu - AI Study Hub</title>
    
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
        
        html.dark .doc-card { background-color: #1f2937; border-color: #374151; }
        html.dark .doc-title { color: #ffffff; }
        html.dark .doc-desc { color: #9ca3af; }
        html.dark .author-box { background-color: rgba(55, 65, 81, 0.4); border-color: #374151; }
        html.dark .author-name { color: #f3f4f6; }
        html.dark .search-bar { background-color: #1f2937; border-color: #374151; color: #ffffff; }
        html.dark .search-bar:focus { border-color: #5c3cf5; }
        html.dark .stat-card { background-color: #1f2937; border-color: #374151; }

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
            
            .doc-card { @apply bg-white border border-gray-100 rounded-2xl p-5 hover:shadow-md transition-all flex flex-col justify-between; }
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
                <a href="AIChatbotLanding.jsp" class="nav-link">
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
                                <p class="user-name"><%= username != null && !username.trim().isEmpty() ? username : "Học viên"%></p>
                                <% if (isPremiumUser) { %>
                                <span class="flex-shrink-0 px-1.5 py-0.5 bg-gradient-to-r from-amber-500 to-orange-500 text-white font-bold text-[9px] rounded-full shadow-sm scale-90 origin-left">PRO</span>
                                <% } else { %>
                                <span class="flex-shrink-0 px-2 py-0.5 bg-emerald-100 text-emerald-800 border border-emerald-300 dark:bg-emerald-900/60 dark:text-emerald-400 dark:border-emerald-700 font-bold text-[10px] rounded-full shadow-sm scale-90 origin-left tracking-wide">FREE</span>
                                <% }%>
                            </div>
                            <p class="user-role">Quyền: <%= role %></p>
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
        <div class="header-container">
            <div>
                <h1 class="page-title dark:text-white mb-1">Khám phá tài liệu cộng đồng</h1>
                <p class="text-sm text-gray-500 font-medium">Tìm kiếm tài liệu và đề cương ôn tập được đóng góp bởi sinh viên</p>
            </div>
        </div>

        <div class="mb-5">
            <div class="relative">
                <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"><svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg></span>
                <input type="text" id="search-input" placeholder="Tìm nhanh theo tiêu đề file, môn học hoặc tên người đăng..." class="search-bar w-full pl-9 pr-4 py-2.5 border border-gray-200 rounded-xl text-sm focus:outline-none shadow-sm transition-all">
            </div>
        </div>

        <div class="mb-2"><span class="text-xs font-bold uppercase tracking-wider text-gray-400">Môn học tiêu điểm</span></div>
        <div id="categories-container" class="mb-6 flex gap-2 overflow-x-auto pb-1"></div>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
            <div class="stat-card border bg-white rounded-xl p-4 dark:border-gray-700 shadow-sm flex items-center justify-between">
                <div><p class="text-gray-400 text-xs font-medium">Tổng tài liệu</p><p class="text-2xl font-bold mt-1.5 tracking-tight text-blue-500"><%= totalDocs != null && totalDocs > 0 ? String.format("%,d", totalDocs) : "1,248" %> file</p></div>
                <div class="w-12 h-12 bg-blue-100 dark:bg-blue-950/40 rounded-xl flex items-center justify-center"><svg class="w-6 h-6 text-blue-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"></path></svg></div>
            </div>
            <div class="stat-card border bg-white rounded-xl p-4 dark:border-gray-700 shadow-sm flex items-center justify-between">
                <div><p class="text-gray-400 text-xs font-medium">Người đóng góp</p><p class="text-2xl font-bold mt-1.5 tracking-tight text-purple-500"><%= totalContributors != null && totalContributors > 0 ? String.format("%,d", totalContributors) : "314" %> thành viên</p></div>
                <div class="w-12 h-12 bg-purple-100 dark:bg-purple-950/40 rounded-xl flex items-center justify-center"><svg class="w-6 h-6 text-purple-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path></svg></div>
            </div>
            <div class="stat-card border bg-white rounded-xl p-4 dark:border-gray-700 shadow-sm flex items-center justify-between">
                <div><p class="text-gray-400 text-xs font-medium">Lượt tải hệ thống</p><p class="text-2xl font-bold mt-1.5 tracking-tight text-emerald-500"><%= totalDownloads != null && totalDownloads > 0 ? String.format("%,d", totalDownloads) : "8,920" %> lượt</p></div>
                <div class="w-12 h-12 bg-emerald-100 dark:bg-emerald-950/40 rounded-xl flex items-center justify-center"><svg class="w-6 h-6 text-emerald-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg></div>
            </div>
        </div>

        <div id="documents-grid" class="grid grid-cols-1 lg:grid-cols-2 gap-5"></div>

        <div class="mt-8 text-center">
            <button id="load-more-btn" onclick="handleLoadMore()" class="btn-secondary px-5 py-2.5 countries mx-auto">Xem thêm tài liệu</button>
        </div>
    </main>

    <script>
        let searchQuery = "";
        let selectedCategory = "Tất cả";
        let itemsShown = 4; 

        // ĐỒNG BỘ DỮ LIỆU ĐỘNG TỪ ĐỐI TƯỢNG DOCUMENT CỦA DATABASE (Đã xử lý map tên người đăng, môn học, đuôi file)
        const internalDocs = [
            <%
                if (!publicDocuments.isEmpty()) {
                    for (int i = 0; i < publicDocuments.size(); i++) {
                        Document doc = publicDocuments.get(i);
                        String title = doc.getTitle().replace("\"", "\\\"");
                        
                        // 1. Tự động nhận diện định dạng file dựa trên Storage URL
                        String fileExt = "PDF";
                        String url = doc.getCloudStorageUrl();
                        if (url != null && url.lastIndexOf('.') > 0) {
                            fileExt = url.substring(url.lastIndexOf('.') + 1).toUpperCase();
                        }

                        // 2. Mock bóc tách môn học dựa trên tiêu đề file nếu DB chưa chia trường môn riêng biệt
                        String subjectCode = "Tổng hợp";
                        String upperTitle = title.toUpperCase();
                        if (upperTitle.contains("MAS291")) subjectCode = "MAS291";
                        else if (upperTitle.contains("PRJ301")) subjectCode = "PRJ301";
                        else if (upperTitle.contains("DBI202")) subjectCode = "DBI202";
                        else if (upperTitle.contains("IOT102")) subjectCode = "IoT102";
                        else if (upperTitle.contains("SWP391")) subjectCode = "SWP391";
                        else if (upperTitle.contains("SSG104")) subjectCode = "SSG104";
            %>
                { 
                    id: "<%= doc.getDocumentId() %>", 
                    title: "<%= title %>", 
                    author: "Sinh viên khóa K20", 
                    authorAvatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=user<%= doc.getDocumentId() %>", 
                    category: "<%= subjectCode %>", 
                    fileType: "<%= fileExt %>",
                    downloads: <%= 15 + (doc.getDocumentId() * 3) %>, // Giả lập lượt tải động tăng tiến theo ID file
                    rating: 4.8, 
                    size: "<%= doc.getFileSizeMb() %> MB", 
                    description: "Tài liệu học tập thực tế được kiểm duyệt chất lượng cao trên hệ thống AI Study Hub.", 
                    tags: ["<%= subjectCode %>", "<%= fileExt %>"] 
                }<%= (i < publicDocuments.size() - 1) ? "," : "" %>
            <%
                    }
                } else { 
                    // MẢNG DỰ PHÒNG CHUẨN KHI DATABASE CHƯA TRUYỀN DANH SÁCH XUỐNG (Hiện đầy đủ thông tin Người Đăng, Môn học, Loại file)
            %>
                { id: "1", title: "Tổng hợp toàn bộ kiến thức MAS291 - Học kỳ Spring", author: "Phạm Nhật Minh", authorAvatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=minh", category: "MAS291", fileType: "PDF", downloads: 142, rating: 4.9, size: "12.4 MB", description: "Bao gồm công thức, bài tập chương 1 đến chương 5 kèm lời giải chi tiết phục vụ thi Final.", tags: ["MAS291", "PDF"] },
                { id: "2", title: "PRJ301 - Đề cương ôn tập PE và các lỗi Logic Servlet thường gặp", author: "Lê Tuấn", authorAvatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=tuan", category: "PRJ301", fileType: "DOCX", downloads: 95, rating: 4.8, size: "4.2 MB", description: "Hướng dẫn cấu trúc thư mục MVC, xử lý Session, Request và Filter tối ưu.", tags: ["PRJ301", "DOCX"] },
                { id: "3", title: "DBI202 - Tổng hợp 50 câu lệnh SQL nâng cao nâng cấp điểm số", author: "Trần Quân", authorAvatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=quan", category: "DBI202", fileType: "XLSX", downloads: 210, rating: 5.0, size: "2.1 MB", description: "Các mẫu câu lệnh Subquery, Join nhiều bảng và tối ưu hóa Index trong SQL Server.", tags: ["DBI202", "XLSX"] },
                { id: "4", title: "IoT102 - Sơ đồ lắp mạch bảo mật Arduino Uno & Module ESP8266", author: "Phạm Nhật Minh", authorAvatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=iot", category: "IoT102", fileType: "PDF", downloads: 78, rating: 4.7, size: "18.5 MB", description: "Tài liệu chi tiết hướng dẫn thiết kế mạch phần cứng kèm code mẫu kết nối wifi.", tags: ["IoT102", "PDF"] }
            <% } %>
        ];

        // Khởi tạo các danh mục môn học của bạn
        const categories = ["Tất cả", "MAS291", "PRJ301", "DBI202", "IoT102", "SWP391"];

        function renderCategories() {
            const container = document.getElementById('categories-container');
            container.innerHTML = categories.map(cat => {
                const isActive = cat === selectedCategory;
                const activeClass = isActive 
                    ? "bg-indigo-600 text-white border-indigo-600 shadow-sm dark:bg-indigo-700" 
                    : "bg-white text-gray-700 border-gray-200 hover:bg-gray-50 dark:bg-gray-800 dark:text-gray-300 dark:border-gray-700 dark:hover:bg-gray-700";
                return `<button onclick="selectCategory('${cat}')" class="px-4 py-1.5 border rounded-full text-xs font-semibold whitespace-nowrap transition-all ${activeClass}">${cat}</button>`;
            }).join('');
        }

        function selectCategory(category) {
            selectedCategory = category;
            renderCategories();
            renderDocuments();
        }

        function renderDocuments() {
            const grid = document.getElementById('documents-grid');
            const loadMoreBtn = document.getElementById('load-more-btn');

            // Bộ lọc dữ liệu tìm kiếm
            const filteredDocs = internalDocs.filter(doc => {
                const matchesSearch = doc.title.toLowerCase().includes(searchQuery.toLowerCase()) || doc.author.toLowerCase().includes(searchQuery.toLowerCase());
                const matchesCategory = selectedCategory === "Tất cả" || doc.category === selectedCategory;
                return matchesSearch && matchesCategory;
            });

            // Cắt mảng phục vụ phân trang (Xem thêm tài liệu)
            const slicedDocs = filteredDocs.slice(0, itemsShown);

            if (slicedDocs.length === 0) {
                grid.innerHTML = `
                <div class="col-span-full flex flex-col items-center justify-center py-16 bg-white border border-gray-100 rounded-2xl shadow-sm dark:bg-gray-800 dark:border-gray-700">
                    <p class="text-gray-500 dark:text-gray-400 font-medium text-sm">Không tìm thấy tài liệu phù hợp.</p>
                </div>`;
                loadMoreBtn.classList.add('hidden');
                return;
            }

            // Quản lý hiển thị nút Xem thêm thông minh
            if (itemsShown >= filteredDocs.length) {
                loadMoreBtn.classList.add('hidden');
            } else {
                loadMoreBtn.classList.remove('hidden');
            }

            grid.innerHTML = slicedDocs.map(doc => {
                return `
                <div class="doc-card border shadow-sm transition-all duration-200">
                    <div>
                        <div class="flex items-start justify-between mb-2">
                            <h3 class="doc-title font-bold text-[15px] leading-snug">${doc.title}</h3>
                            <span class="ml-2 flex-shrink-0 px-2 py-0.5 bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300 font-mono text-[10px] font-bold rounded">${doc.fileType}</span>
                        </div>
                        <p class="doc-desc text-xs font-medium line-clamp-2 leading-relaxed mb-4">${doc.description}</p>
                    </div>
                    <div>
                        <div class="author-box flex items-center mb-3 p-2 rounded-xl border">
                            <img src="${doc.authorAvatar}" class="w-7 h-7 rounded-full mr-2 border bg-white dark:border-gray-600">
                            <div class="flex-1 min-w-0">
                                <p class="author-name text-xs font-bold truncate">Người đăng: ${doc.author}</p>
                            </div>
                            <span class="px-2 py-0.5 bg-indigo-50 text-indigo-600 dark:bg-indigo-950/50 dark:text-indigo-400 text-[10px] font-extrabold rounded-md border border-indigo-100 dark:border-indigo-900/40 uppercase tracking-wide">Môn: ${doc.category}</span>
                        </div>
                        <div class="flex items-center justify-between pt-3 border-t border-gray-100 dark:border-gray-700">
                            <div class="flex items-center space-x-3 text-[11px] font-semibold text-gray-400">
                                <div class="flex items-center"><span class="text-gray-700 dark:text-gray-300 font-bold mr-1">⭐</span>${doc.rating}</div>
                                <div><span>${doc.downloads.toLocaleString()} tải</span></div>
                                <div><span>Dung lượng: ${doc.size}</span></div>
                            </div>
                            <button onclick="handleDownload('${doc.id}')" class="flex items-center space-x-2 px-4 py-2 bg-[#5c3cf5] text-white rounded-xl hover:bg-indigo-700 transition-all text-xs font-bold shadow-sm cursor-pointer">
                                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                                <span>Tải xuống</span>
                            </button>
                        </div>
                    </div>
                </div>`;
            }).join('');
        }

        function handleLoadMore() {
            itemsShown += 4; 
            renderDocuments();
        }

        function handleDownload(docId) {
            window.location.href = "<%= request.getContextPath()%>/MainController?action=downloadDoc&docId=" + docId;
        }

        document.getElementById('search-input').addEventListener('input', e => { 
            searchQuery = e.target.value; 
            renderDocuments(); 
        });
        
        document.addEventListener("DOMContentLoaded", () => { 
            renderCategories(); 
            renderDocuments(); 
        });
    </script>
</body>
</html>