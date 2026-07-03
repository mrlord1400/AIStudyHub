<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List" %>
<%@ page import="Model.DTO.Document" %>
<%
    // Header chống cache
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    // Xử lý dữ liệu được đẩy từ Controller xuống
    List<Document> publicDocuments = (List<Document>) request.getAttribute("publicDocuments");
    Integer totalDocs = (Integer) request.getAttribute("realTotalDocs");
    Integer totalContributors = (Integer) request.getAttribute("realTotalContributors");
    Integer totalDownloads = (Integer) request.getAttribute("realTotalDownloads");
%>
<%!
    // Hàm escape JS để tránh lỗi nháy kép/đơn khi parse chuỗi
    private String escapeJs(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'")
                    .replace("\r\n", " ").replace("\n", " ").replace("\r", " ").replace("</", "<\\/");
    }
%>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Khám phá tài liệu (Khách) - AI Study Hub</title>
    
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
        html.dark .doc-card { background-color: #1f2937; border-color: #374151; }
        html.dark .doc-title { color: #ffffff; }
        html.dark .doc-desc { color: #9ca3af; }
        html.dark .author-box { background-color: rgba(55, 65, 81, 0.4); border-color: #374151; }
        html.dark .author-name { color: #f3f4f6; }
        html.dark .search-bar { background-color: #1f2937; border-color: #374151; color: #ffffff; }
        html.dark .stat-card { background-color: #1f2937; border-color: #374151; }

        @layer components {
            .page-body { @apply flex flex-col min-h-screen w-full text-gray-800 bg-[#f8f9fa] font-sans p-4 md:p-8; }
            .header-container { @apply flex justify-between items-center mb-6; }
            .page-title { @apply text-2xl font-bold text-gray-900 tracking-tight; }
            
            /* Banner Khách */
            .guest-banner { @apply mb-6 bg-amber-50/80 border border-amber-200 rounded-2xl p-4 shadow-sm; }
            .guest-banner-content { @apply flex items-start; }
            .guest-banner-icon { @apply w-5 h-5 text-amber-600 mt-0.5 mr-3 flex-shrink-0; }
            .guest-banner-title { @apply text-[15px] font-bold text-amber-900 mb-1; }
            .guest-banner-text { @apply text-sm text-amber-700/90 font-medium leading-relaxed; }
            .guest-banner-link { @apply inline-flex items-center text-sm font-bold text-amber-800 underline hover:text-amber-900 mt-2 transition-colors; }
            
            /* Style tương đồng FileExplore */
            .search-bar { @apply w-full pl-10 pr-14 py-3 border border-gray-200 rounded-xl text-sm focus:outline-none focus:border-indigo-500 shadow-sm transition-all; }
            .stat-card { @apply bg-white border border-gray-100 rounded-2xl p-5 shadow-sm flex items-center justify-between; }
            .doc-card { @apply bg-white border border-gray-100 rounded-2xl p-5 hover:shadow-md transition-all flex flex-col justify-between; }
            .btn-primary { @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-[#5c3cf5] text-white rounded-xl font-semibold hover:bg-indigo-700 transition-colors text-sm shadow-sm cursor-pointer; }
            .btn-secondary { @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-white border border-gray-200 text-gray-700 rounded-xl font-semibold hover:bg-gray-50 transition-colors text-sm shadow-sm cursor-pointer; }
        }
    </style>
</head>
<body class="page-body">

    <div class="max-w-7xl mx-auto w-full">
        
        <div class="header-container">
            <div>
                <h1 class="page-title dark:text-white mb-1">Khám phá tài liệu cộng đồng</h1>
                <p class="text-sm text-gray-500 font-medium">Tìm kiếm tài liệu và đề cương ôn tập được đóng góp bởi sinh viên</p>
            </div>
            <div>
                <a href="<%= request.getContextPath()%>/login.jsp" class="btn-primary bg-gradient-to-r from-indigo-600 to-purple-600">
                    <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1"></path></svg>
                    Đăng nhập / Đăng ký
                </a>
            </div>
        </div>

        <div class="guest-banner dark:bg-amber-900/20 dark:border-amber-700/50">
            <div class="guest-banner-content">
                <svg class="guest-banner-icon dark:text-amber-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
                </svg>
                <div>
                    <h3 class="guest-banner-title dark:text-amber-400">Bạn đang dùng tài khoản Khách</h3>
                    <p class="guest-banner-text dark:text-amber-200/80">
                        Đăng ký tài khoản để mở khóa các tính năng: tải xuống tài liệu, upload tài liệu, sử dụng AI Chatbot và nhiều hơn nữa!
                    </p>
                    <a href="<%= request.getContextPath()%>/login.jsp" class="guest-banner-link dark:text-amber-300">Đăng ký ngay →</a>
                </div>
            </div>
        </div>

        <div class="mb-6 relative">
            <span class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
            </span>
            <input type="text" id="guest-search" placeholder="Nhập từ khóa để tìm kiếm nhanh tài liệu..." class="search-bar">
        </div>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
            <div class="stat-card">
                <div><p class="text-gray-400 text-xs font-medium">Tổng tài liệu công khai</p><p class="text-2xl font-bold mt-1.5 text-blue-500"><%= totalDocs != null ? String.format("%,d", totalDocs) : "0"%></p></div>
                <div class="w-12 h-12 bg-blue-100 dark:bg-blue-950/40 rounded-xl flex items-center justify-center"><svg class="w-6 h-6 text-blue-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"></path></svg></div>
            </div>
            <div class="stat-card">
                <div><p class="text-gray-400 text-xs font-medium">Người đóng góp</p><p class="text-2xl font-bold mt-1.5 text-purple-500"><%= totalContributors != null ? String.format("%,d", totalContributors) : "0"%></p></div>
                <div class="w-12 h-12 bg-purple-100 dark:bg-purple-950/40 rounded-xl flex items-center justify-center"><svg class="w-6 h-6 text-purple-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path></svg></div>
            </div>
            <div class="stat-card">
                <div><p class="text-gray-400 text-xs font-medium">Lượt tải hệ thống</p><p class="text-2xl font-bold mt-1.5 text-emerald-500"><%= totalDownloads != null ? String.format("%,d", totalDownloads) : "0"%></p></div>
                <div class="w-12 h-12 bg-emerald-100 dark:bg-emerald-950/40 rounded-xl flex items-center justify-center"><svg class="w-6 h-6 text-emerald-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg></div>
            </div>
        </div>

        <div id="documents-grid" class="grid grid-cols-1 lg:grid-cols-2 gap-5 mb-8"></div>
        
        <div class="text-center">
            <button id="load-more-btn" onclick="handleLoadMore()" class="btn-secondary px-5 py-2.5 mx-auto hidden">Xem thêm tài liệu</button>
        </div>

    </div>

    <script>
        // Data động mồi từ Backend (Giống y hệt trang FileExplore)
        let guestDocs = [
        <%
            if (publicDocuments != null && !publicDocuments.isEmpty()) {
                for (int i = 0; i < publicDocuments.size(); i++) {
                    Document doc = publicDocuments.get(i);
                    // Bỏ qua các file bị Flag (cắm cờ) đối với khách
                    if(doc.isFlagged()) continue; 
                    
                    String title = escapeJs(doc.getTitle());
                    String fileExt = doc.getFileExtension() != null ? doc.getFileExtension().toUpperCase() : "FILE";
                    String authorName = escapeJs(
                            (doc.getAuthorUsername() != null && !doc.getAuthorUsername().trim().isEmpty())
                            ? doc.getAuthorUsername()
                            : "Người dùng #" + doc.getUserId()
                    );
        %>
            {
                id: "<%= doc.getDocumentId()%>",
                title: "<%= title%>",
                author: "<%= authorName%>",
                authorAvatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=user<%= doc.getUserId()%>",
                fileType: "<%= fileExt%>",
                downloads: <%= doc.getDownloadCount() != null ? doc.getDownloadCount() : 0%>,
                size: "<%= doc.getFileSizeMb()%> MB",
                description: "Tài liệu học tập được chia sẻ công khai bởi cộng đồng sinh viên FPT."
            }<%= (i < publicDocuments.size() - 1) ? "," : ""%>
        <%
                }
            }
        %>
        ];

        let itemsShown = 8;
        let currentSearch = "";

        // Hàm xử lý khi bấm Tải -> Hiện Confirm Dialog -> Về Login
        function handleGuestDownload() {
            const isConfirm = confirm("Bạn cần đăng nhập/ đăng kí để có thể tải file này.\n\nNhấn OK để chuyển đến trang đăng nhập.");
            if (isConfirm) {
                window.location.href = "<%= request.getContextPath()%>/login.jsp";
            }
        }

        function renderDocuments() {
            const grid = document.getElementById('documents-grid');
            const loadMoreBtn = document.getElementById('load-more-btn');

            // Lọc front-end cho thanh search
            const filteredDocs = guestDocs.filter(doc => 
                doc.title.toLowerCase().includes(currentSearch.toLowerCase()) ||
                doc.author.toLowerCase().includes(currentSearch.toLowerCase())
            );

            if (filteredDocs.length === 0) {
                grid.innerHTML = `
                <div class="col-span-full flex flex-col items-center justify-center py-16 bg-white border border-gray-100 rounded-2xl shadow-sm dark:bg-gray-800 dark:border-gray-700">
                    <p class="text-gray-500 dark:text-gray-400 font-medium text-sm">Chưa có tài liệu nào hoặc không tìm thấy tài liệu phù hợp.</p>
                </div>`;
                loadMoreBtn.classList.add('hidden');
                return;
            }

            const slicedDocs = filteredDocs.slice(0, itemsShown);

            if (itemsShown >= filteredDocs.length) {
                loadMoreBtn.classList.add('hidden');
            } else {
                loadMoreBtn.classList.remove('hidden');
            }

            grid.innerHTML = slicedDocs.map(doc => {
                return `
                <div class="doc-card border shadow-sm transition-all duration-200 relative">
                    <div>
                        <div class="flex items-start justify-between mb-2 pr-6">
                            <h3 class="doc-title font-bold text-[15px] leading-snug">\${doc.title}</h3>
                            <span class="ml-2 flex-shrink-0 px-2 py-0.5 bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300 font-mono text-[10px] font-bold rounded">\${doc.fileType}</span>
                        </div>
                        <p class="doc-desc text-xs font-medium line-clamp-2 leading-relaxed mb-4">\${doc.description}</p>
                    </div>
                    <div>
                        <div class="author-box flex items-center mb-3 p-2 rounded-xl border">
                            <img src="\${doc.authorAvatar}" class="w-7 h-7 rounded-full mr-2 border bg-white dark:border-gray-600">
                            <div class="flex-1 min-w-0">
                                <p class="author-name text-xs font-bold truncate">Người đăng: \${doc.author}</p>
                            </div>
                        </div>
                        <div class="flex items-center justify-between pt-3 border-t border-gray-100 dark:border-gray-700">
                            <div class="flex items-center space-x-4 text-[11px] font-semibold text-gray-400">
                                <div class="flex items-center" title="Số lượt tải">
                                    <svg class="w-3.5 h-3.5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
                                    </svg>\${doc.downloads}
                                </div>
                                <div><span>\${doc.size}</span></div>
                            </div>
                            
                            <div class="flex items-center space-x-2">
                                <a href="<%= request.getContextPath()%>/MainController?action=viewPublicPage&docId=\${doc.id}" target="_blank" class="flex items-center space-x-2 px-4 py-2 bg-white text-gray-700 border border-gray-200 rounded-xl hover:bg-gray-50 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-700 transition-all text-xs font-bold shadow-sm cursor-pointer">
                                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path><path stroke-linecap="round" stroke-linejoin="round" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path></svg>
                                    <span>Xem Trước</span>
                                </a>
                                <button onclick="handleGuestDownload()" class="flex items-center space-x-2 px-4 py-2 bg-[#5c3cf5] text-white rounded-xl hover:bg-indigo-700 transition-all text-xs font-bold shadow-sm cursor-pointer">
                                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                                    <span>Tải</span>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>`;
            }).join('');
        }

        function handleLoadMore() {
            itemsShown += 8;
            renderDocuments();
        }

        document.getElementById('guest-search').addEventListener('input', (e) => {
            currentSearch = e.target.value;
            itemsShown = 8; // Reset lại số lượng hiển thị khi search
            renderDocuments();
        });

        document.addEventListener("DOMContentLoaded", () => {
            renderDocuments();
        });
    </script>
</body>
</html>