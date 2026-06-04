<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // Ensure logged-in users are redirected to the full version of the explorer
    HttpSession userSession = request.getSession(false);
    if (userSession != null && userSession.getAttribute("role") != null) {
        String role = (String) userSession.getAttribute("role");
        if (!"GUEST".equalsIgnoreCase(role)) {
            response.sendRedirect(request.getContextPath() + "/file_explorer.jsp");
            return;
        }
    }
%>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Khám phá tài liệu (Khách) - AI Study Hub</title>

        <script src="https://cdn.tailwindcss.com"></script>
        <link rel="preconnect" href="https://fonts.googleapis.com">
        <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">

        <style type="text/tailwindcss">
            @layer components {
                /* General Layout */
                .page-body {
                    @apply bg-white min-h-screen w-full text-gray-800 p-8 font-sans;
                }

                /* Header Section */
                .header-container {
                    @apply flex justify-between items-center mb-6;
                }
                .page-title {
                    @apply text-4xl font-bold text-gray-900 tracking-tight;
                }
                .notification-btn {
                    @apply relative p-1.5 text-gray-500 hover:text-gray-700 cursor-pointer rounded-full transition-colors;
                }
                .notification-badge {
                    @apply absolute top-1 right-1 w-4 h-4 bg-red-500 text-white font-bold text-[9px] flex items-center justify-center rounded-full scale-90 border border-white;
                }

                /* Subtitle Section */
                .subtitle-container {
                    @apply mb-4;
                }
                .section-title {
                    @apply text-2xl font-bold text-gray-900 mb-1.5 tracking-tight;
                }
                .section-desc {
                    @apply text-sm text-gray-500 font-medium;
                }

                /* Guest Banner */
                .guest-banner {
                    @apply mb-6 bg-amber-50/60 border border-amber-200 rounded-xl p-4;
                }
                .guest-banner-content {
                    @apply flex items-start;
                }
                .guest-banner-icon {
                    @apply w-4 h-4 text-amber-600 mt-0.5 mr-3 flex-shrink-0;
                }
                .guest-banner-title {
                    @apply text-sm font-bold text-amber-900 mb-0.5;
                }
                .guest-banner-text {
                    @apply text-xs text-amber-700/90 font-medium leading-relaxed;
                }
                .guest-banner-link {
                    @apply inline-block text-xs font-bold text-amber-800 underline hover:text-amber-900 mt-2;
                }

                /* Search Section */
                .search-container {
                    @apply mb-5;
                }
                .search-wrapper {
                    @apply relative;
                }
                .search-icon {
                    @apply absolute left-3 top-1/2 -translate-y-1/2 text-gray-400;
                }
                .search-input {
                    @apply w-full pl-9 pr-4 py-2.5 border border-gray-200 rounded-xl text-sm text-gray-700 focus:outline-none focus:border-gray-300 shadow-sm;
                }

                /* Categories Filter */
                .categories-wrapper {
                    @apply mb-6 flex gap-2 overflow-x-auto pb-1;
                }
                .cat-btn {
                    @apply px-4 py-1.5 border rounded-full text-xs font-semibold whitespace-nowrap transition-all;
                }
                .cat-btn-active {
                    @apply bg-gray-900 text-white border-gray-900 shadow-sm;
                }
                .cat-btn-inactive {
                    @apply bg-white text-gray-700 border-gray-200 hover:bg-gray-50;
                }

                /* Stats Grid */
                .stats-grid {
                    @apply grid grid-cols-1 md:grid-cols-3 gap-4 mb-6;
                }
                .stat-card-blue {
                    @apply bg-blue-600 rounded-xl p-4 text-white shadow-sm flex items-center justify-between;
                }
                .stat-card-purple {
                    @apply bg-purple-600 rounded-xl p-4 text-white shadow-sm flex items-center justify-between;
                }
                .stat-card-green {
                    @apply bg-green-600 rounded-xl p-4 text-white shadow-sm flex items-center justify-between;
                }
                .stat-subtitle {
                    @apply text-xs font-medium opacity-90;
                }
                .stat-value {
                    @apply text-2xl font-bold mt-1.5 tracking-tight;
                }
                .stat-icon-box {
                    @apply w-12 h-12 bg-white/10 rounded-xl flex items-center justify-center;
                }

                /* Document Grid & Cards */
                .docs-grid {
                    @apply grid grid-cols-1 lg:grid-cols-2 gap-5;
                }
                .doc-card {
                    @apply bg-white border border-gray-100 rounded-2xl p-5 shadow-sm flex flex-col justify-between;
                }
                .doc-header {
                    @apply flex items-start justify-between mb-2;
                }
                .doc-title {
                    @apply font-bold text-[15px] text-gray-900 leading-snug;
                }
                .doc-desc {
                    @apply text-xs text-gray-400 font-medium line-clamp-2 leading-relaxed mb-3;
                }
                .doc-tags {
                    @apply flex flex-wrap gap-1.5 mb-4;
                }
                .doc-tag {
                    @apply px-2 py-0.5 text-[11px] bg-indigo-50 text-indigo-600 rounded font-medium;
                }

                /* Document Author Details */
                .author-box {
                    @apply flex items-center mb-3 bg-gray-50/70 p-2 rounded-xl border border-gray-50;
                }
                .author-avatar {
                    @apply w-7 h-7 rounded-full mr-2 border bg-white;
                }
                .author-info {
                    @apply flex-1 min-w-0;
                }
                .author-name {
                    @apply text-xs font-bold text-gray-800 truncate;
                }
                .author-date {
                    @apply text-[10px] text-gray-400 mt-0.5;
                }
                .category-badge {
                    @apply px-2 py-0.5 bg-white border text-gray-500 text-[10px] font-bold rounded-md;
                }

                /* Document Footer Details */
                .doc-footer {
                    @apply flex items-center justify-between pt-3 border-t border-gray-100;
                }
                .doc-stats {
                    @apply flex items-center space-x-3 text-[11px] font-semibold text-gray-400;
                }
                .rating-text {
                    @apply text-gray-700 font-bold;
                }
                .btn-download-disabled {
                    @apply flex items-center space-x-1.5 px-4 py-2 bg-[#cbd5e1] text-[#64748b] rounded-xl cursor-not-allowed text-xs font-bold shadow-sm;
                }

                /* Utilities & Floating Buttons */
                .load-more-container {
                    @apply mt-8 text-center;
                }
                .btn-secondary {
                    @apply px-5 py-2.5 border border-gray-200 rounded-xl bg-white text-gray-700 font-semibold text-sm hover:bg-gray-50 transition-colors shadow-sm;
                }
                .fab-container {
                    @apply fixed bottom-4 right-4 z-50;
                }
                .fab-btn {
                    @apply w-9 h-9 bg-gray-800 text-white rounded-full flex items-center justify-center hover:bg-slate-700 shadow-md font-bold text-sm;
                }
            }

            /* Custom Scrollbar */
            body {
                font-family: 'Inter', sans-serif;
            }
            ::-webkit-scrollbar {
                width: 6px;
                height: 6px;
            }
            ::-webkit-scrollbar-thumb {
                background: #cbd5e1;
                border-radius: 4px;
            }
        </style>
    </head>
    <body class="page-body">

        <div class="header-container">
            <h1 class="page-title">Khám phá tài liệu</h1>
            <div class="flex items-center space-x-2">
                <a href="<%= request.getContextPath()%>/MainController?action=logout" class="flex items-center space-x-1.5 px-3 py-2 text-sm font-medium text-gray-500 hover:text-red-500 hover:bg-red-50 rounded-xl transition-colors cursor-pointer">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                    <polyline points="16 17 21 12 16 7"/>
                    <line x1="21" x2="9" y1="12" y2="12"/>
                    </svg>
                    <span class="font-bold hidden sm:inline">Đăng xuất</span>
                </a>

                <div class="notification-btn">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"></path></svg>
                    <span class="notification-badge">3</span>
                </div>
            </div>
        </div>

        <div class="subtitle-container">
            <p class="section-desc">Tìm kiếm và xem tài liệu được chia sẻ bởi cộng đồng</p>
        </div>

        <div class="guest-banner">
            <div class="guest-banner-content">
                <svg class="guest-banner-icon" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
                </svg>
                <div>
                    <h3 class="guest-banner-title">Bạn đang dùng tài khoản Khách</h3>
                    <p class="guest-banner-text">
                        Đăng ký tài khoản để mở khóa các tính năng: tải xuống tài liệu, upload tài liệu, sử dụng AI Chatbot và nhiều hơn nữa!
                    </p>
                </div>
            </div>
        </div>

        <div class="search-container">
            <div class="search-wrapper">
                <span class="search-icon">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
                </span>
                <input type="text" id="search-input" placeholder="Tìm kiếm tài liệu, tác giả, môn học..." class="search-input">
            </div>
        </div>

        <div id="categories-container" class="categories-wrapper"></div>

        <div class="stats-grid">
            <div class="stat-card-blue">
                <div><p class="stat-subtitle text-blue-100">Tổng tài liệu</p><p class="stat-value">12,456</p></div>
                <div class="stat-icon-box"><svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"></path></svg></div>
            </div>
            <div class="stat-card-purple">
                <div><p class="stat-subtitle text-purple-100">Người đóng góp</p><p class="stat-value">3,842</p></div>
                <div class="stat-icon-box"><svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path></svg></div>
            </div>
            <div class="stat-card-green">
                <div><p class="stat-subtitle text-green-100">Lượt tải xuống</p><p class="stat-value">156K+</p></div>
                <div class="stat-icon-box"><svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg></div>
            </div>
        </div>

        <div id="documents-grid" class="docs-grid"></div>

        <div class="load-more-container">
            <button class="btn-secondary">Xem thêm tài liệu</button>
        </div>

        <div class="fab-container">
            <button class="fab-btn">?</button>
        </div>

        <script>
            let searchQuery = "";
            let selectedCategory = "Tất cả";

            const mockSharedDocs = [
                {id: "1", title: "Tổng hợp kiến thức SWP391 - Học kỳ Fall 2024", author: "Nguyễn Minh Tuấn", authorAvatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=1", category: "SWP391", downloads: 1245, rating: 4.8, reviews: 89, uploadDate: "2024-05-10", size: "15.2 MB", description: "Tài liệu tổng hợp đầy đủ các kiến thức quan trọng cho môn SWP391", tags: ["Software Engineering", "Project Management", "Agile"]},
                {id: "2", title: "PRJ301 - Java Web Complete Guide", author: "Trần Thu Hà", authorAvatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=2", category: "PRJ301", downloads: 2103, rating: 4.9, reviews: 156, uploadDate: "2024-05-08", size: "22.8 MB", description: "Hướng dẫn chi tiết về Java Web từ cơ bản đến nâng cao", tags: ["Java", "Servlet", "JSP", "MVC"]},
                {id: "3", title: "Database Design Patterns - Best Practices", author: "Phạm Văn Khoa", authorAvatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=3", category: "DBI202", downloads: 876, rating: 4.7, reviews: 64, uploadDate: "2024-05-12", size: "8.4 MB", description: "Các mẫu thiết kế database phổ biến và cách áp dụng", tags: ["Database", "SQL", "Design Patterns"]},
                {id: "4", title: "React & Tailwind - Modern Web Development", author: "Lê Hoàng Anh", authorAvatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=4", category: "Frontend", downloads: 1567, rating: 4.9, reviews: 123, uploadDate: "2024-05-15", size: "18.6 MB", description: "Xây dựng ứng dụng web hiện đại với React và Tailwind CSS", tags: ["React", "Tailwind CSS", "Frontend"]},
                {id: "5", title: "Algorithms & Data Structures Cheat Sheet", author: "Vũ Thị Mai", authorAvatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=5", category: "DSA", downloads: 3245, rating: 5.0, reviews: 234, uploadDate: "2024-05-05", size: "5.2 MB", description: "Tổng hợp các thuật toán và cấu trúc dữ liệu quan trọng", tags: ["Algorithms", "Data Structures", "Interview Prep"]},
                {id: "6", title: "English Communication for IT Professionals", author: "Đỗ Thanh Tùng", authorAvatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=6", category: "English", downloads: 654, rating: 4.6, reviews: 45, uploadDate: "2024-05-14", size: "12.1 MB", description: "Kỹ năng giao tiếp tiếng Anh chuyên ngành IT", tags: ["English", "Communication", "IT"]}
            ];

            const categories = ["Tất cả", "SWP391", "PRJ301", "DBI202", "Frontend", "DSA", "English"];

            function renderCategories() {
                const container = document.getElementById('categories-container');
                container.innerHTML = categories.map(cat => {
                    const isActive = cat === selectedCategory;
                    const activeClass = isActive ? "cat-btn-active" : "cat-btn-inactive";
                    return `<button onclick="selectCategory('${cat}')" class="cat-btn ${activeClass}">${cat}</button>`;
                }).join('');
            }

            function selectCategory(category) {
                selectedCategory = category;
                renderCategories();
                renderDocuments();
            }

            function renderDocuments() {
                const grid = document.getElementById('documents-grid');
                const filteredDocs = mockSharedDocs.filter(doc => {
                    const matchesSearch = doc.title.toLowerCase().includes(searchQuery.toLowerCase()) || doc.author.toLowerCase().includes(searchQuery.toLowerCase());
                    const matchesCategory = selectedCategory === "Tất cả" || doc.category === selectedCategory;
                    return matchesSearch && matchesCategory;
                });

                grid.innerHTML = filteredDocs.map(doc => {
                    const tagsHTML = doc.tags.map(t => `<span class="doc-tag">${t}</span>`).join('');
                    return `
                    <div class="doc-card">
                        <div>
                            <div class="doc-header">
                                <h3 class="doc-title">${doc.title}</h3>
                            </div>
                            <p class="doc-desc">${doc.description}</p>
                            <div class="doc-tags">${tagsHTML}</div>
                        </div>
                        <div>
                            <div class="author-box">
                                <img src="${doc.authorAvatar}" class="author-avatar">
                                <div class="author-info">
                                    <p class="author-name">${doc.author}</p>
                                    <p class="author-date">${doc.uploadDate}</p>
                                </div>
                                <span class="category-badge">${doc.category}</span>
                            </div>
                            <div class="doc-footer">
                                <div class="doc-stats">
                                    <div class="flex items-center"><span class="rating-text">${doc.rating}</span></div>
                                    <div><span>${doc.downloads.toLocaleString()} tải</span></div>
                                    <div><span>${doc.size}</span></div>
                                </div>
                                <button disabled class="btn-download-disabled">
                                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path></svg>
                                    <span>Tải xuống</span>
                                </button>
                            </div>
                        </div>
                    </div>`;
                }).join('');
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
