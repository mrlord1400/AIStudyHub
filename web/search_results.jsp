<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="Model.DTO.Document" %>
<%@ page import="Model.DTO.Folder" %>
<%@ page import="Utils.LinkUtil" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%!
    // Escape nhẹ để tránh XSS khi in trực tiếp title/folderName ra HTML
    public String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
%>
<%
    HttpSession userSession = request.getSession(false);
    if (userSession == null || userSession.getAttribute("userId") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    String keyword = (String) request.getAttribute("keyword");
    if (keyword == null) {
        keyword = "";
    }

    List<Document> foundDocuments = (List<Document>) request.getAttribute("foundDocuments");
    List<Folder> foundFolders = (List<Folder>) request.getAttribute("foundFolders");

    int totalResults = (foundDocuments != null ? foundDocuments.size() : 0)
            + (foundFolders != null ? foundFolders.size() : 0);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
%>
<!DOCTYPE html>
<html lang="vi" class="dark">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Kết quả tìm kiếm: <%= esc(keyword)%> - AI Study Hub</title>
        <script src="https://cdn.tailwindcss.com"></script>
        <script>tailwind.config = {darkMode: 'class'}</script>
        <link rel="preconnect" href="https://fonts.googleapis.com">
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
        <style>
            body {
                font-family: 'Inter', sans-serif;
            }
            html.dark body {
                background-color: #111827;
                color: #f3f4f6;
            }
            html.dark .result-card {
                background-color: #1f2937;
                border-color: #374151;
            }
            html.dark .result-title {
                color: #f3f4f6;
            }
            html.dark .search-bar {
                background-color: #1f2937;
                border-color: #374151;
                color: #fff;
            }
        </style>
    </head>
    <body class="bg-[#f8f9fa] min-h-screen">
        <div class="max-w-5xl mx-auto px-6 py-8">

            <div class="flex items-center gap-3 mb-6">
                <a href="<%= request.getContextPath()%>/FolderController?action=viewFolder"
                   class="p-2.5 bg-white border border-gray-200 rounded-xl hover:bg-gray-50 text-gray-600 transition-colors dark:bg-gray-800 dark:border-gray-700 dark:text-gray-300">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path></svg>
                </a>
                <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Kết quả tìm kiếm</h1>
            </div>

            <form action="<%= request.getContextPath()%>/SearchController" method="GET" class="mb-8">
                <div class="relative">
                    <svg class="w-4 h-4 text-gray-400 absolute left-4 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><circle cx="11" cy="11" r="8"/><path stroke-linecap="round" d="m21 21-4.3-4.3"/></svg>
                    <input type="text" name="keyword" value="<%= esc(keyword)%>"
                           placeholder="Tìm kiếm tài liệu, thư mục..."
                           class="search-bar w-full pl-11 pr-4 py-3 rounded-xl border border-gray-200 focus:ring-2 focus:ring-indigo-500 outline-none text-sm bg-white" />
                </div>
            </form>

            <p class="text-sm text-gray-500 dark:text-gray-400 mb-6">
                <% if (keyword.isEmpty()) { %>
                Nhập từ khóa để bắt đầu tìm kiếm.
                <% } else {%>
                Tìm thấy <b><%= totalResults%></b> kết quả cho "<b><%= esc(keyword)%></b>"
                <% } %>
            </p>

            <% if (!keyword.isEmpty() && totalResults == 0) { %>
            <div class="flex flex-col items-center justify-center py-16 bg-white border border-gray-100 rounded-2xl shadow-sm dark:bg-gray-800 dark:border-gray-700">
                <div class="w-16 h-16 bg-gray-50 dark:bg-gray-700 rounded-full flex items-center justify-center text-gray-400 mb-4">
                    <svg class="w-8 h-8" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><circle cx="11" cy="11" r="8"/><path stroke-linecap="round" d="m21 21-4.3-4.3"/></svg>
                </div>
                <p class="text-gray-500 dark:text-gray-400 font-medium text-sm">Không tìm thấy tài liệu hoặc thư mục nào phù hợp.</p>
            </div>
            <% } %>

            <% if (foundFolders != null && !foundFolders.isEmpty()) {%>
            <h2 class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-3">Thư mục (<%= foundFolders.size()%>)</h2>
            <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4 mb-8">
                <% for (Folder f : foundFolders) {
                        String url = LinkUtil.getFolderUrl(request.getContextPath(), f.getFolderId());
                        String dateStr = f.getCreatedAt() != null ? f.getCreatedAt().format(formatter) : "N/A";
                %>
                <a href="<%= url%>" class="result-card flex items-center gap-3 bg-white border border-gray-100 rounded-2xl p-4 hover:shadow-md transition-all dark:border-gray-700">
                    <svg class="w-8 h-8 text-amber-500 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20"><path d="M2 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z"></path></svg>
                    <div class="min-w-0">
                        <p class="result-title font-semibold text-gray-800 text-sm truncate"><%= esc(f.getFolderName())%></p>
                        <p class="text-[11px] text-gray-400"><%= dateStr%></p>
                    </div>
                </a>
                <% } %>
            </div>
            <% } %>

            <% if (foundDocuments != null && !foundDocuments.isEmpty()) {%>
            <h2 class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-3">Tài liệu (<%= foundDocuments.size()%>)</h2>
            <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
                <% for (Document doc : foundDocuments) {
                        String url = LinkUtil.getFolderUrl(request.getContextPath(), doc.getFolderId());
                        String dateStr = doc.getCreatedAt() != null ? doc.getCreatedAt().format(formatter) : "N/A";
                %>
                <a href="<%= url%>" class="result-card flex items-center gap-3 bg-white border border-gray-100 rounded-2xl p-4 hover:shadow-md transition-all dark:border-gray-700">
                    <svg class="w-8 h-8 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
                    <div class="min-w-0">
                        <p class="result-title font-semibold text-gray-800 text-sm truncate"><%= esc(doc.getTitle())%></p>
                        <p class="text-[11px] text-gray-400"><%= String.format("%.2f", doc.getFileSizeMb())%> MB • <%= dateStr%></p>
                    </div>
                </a>
                <% } %>
            </div>
            <% }%>

        </div>
    </body>
</html>