<%@page import="Model.DTO.User"%>
<%@page import="Model.DAO.UserDAO"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%
    // 1. Security Guard
    HttpSession userSession = request.getSession(false);
    if (userSession == null || userSession.getAttribute("userId") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    Integer userId = (Integer) userSession.getAttribute("userId");
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

    // Get User Balance
    UserDAO dao = new UserDAO();
    int userBalance = 0;
    if (userId != null) {
        User user = dao.getUserById(userId);
        if (user != null) {
            userBalance = user.getBalance();
        }
    }
    String tierNameDisplay = isPremiumUser ? "PREMIUM" : "CƠ BẢN";

    // 2. Data Retrieval from Controller
    String currentAction = request.getParameter("action");
    if (currentAction == null || currentAction.isEmpty()) {
        currentAction = "friendList";
    }

    List<User> friends = (List<User>) request.getAttribute("friends");
    List<User> pendingRequests = (List<User>) request.getAttribute("pendingRequests");
    List<User> blockedUsers = (List<User>) request.getAttribute("blockedUsers");

    User searchedUser = (User) request.getAttribute("searchedUser");
    String friendshipStatus = (String) request.getAttribute("friendshipStatus");
    String searchError = (String) request.getAttribute("searchError");

    // [THÊM MỚI] - Logic đếm số lượng thông minh: Ưu tiên đếm từ List có sẵn, nếu List null thì lấy Count do Controller gửi sang
    int friendCount = friends != null ? friends.size() : (request.getAttribute("friendCount") != null ? (Integer) request.getAttribute("friendCount") : 0);
    int pendingCount = pendingRequests != null ? pendingRequests.size() : (request.getAttribute("pendingCount") != null ? (Integer) request.getAttribute("pendingCount") : 0);
    int blockedCount = blockedUsers != null ? blockedUsers.size() : (request.getAttribute("blockedCount") != null ? (Integer) request.getAttribute("blockedCount") : 0);

%>
<!DOCTYPE html>
<html lang="vi" class="dark">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Quản lý bạn bè - AI Study Hub</title>
        <script src="https://cdn.tailwindcss.com"></script>
        <script>
            tailwind.config = {darkMode: 'class'}
        </script>
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
        <style type="text/tailwindcss">
            /* Import core styles from your dashboard to keep consistency */
            @layer components {
                .page-body {
                    @apply flex min-h-screen w-full text-gray-800 bg-[#f8f9fa] dark:bg-[#111827] font-sans transition-colors duration-200;
                }
                .sidebar {
                    @apply w-64 bg-white dark:bg-[#1f2937] border-r border-gray-100 dark:border-gray-800 flex flex-col justify-between p-4 flex-shrink-0 min-h-screen shadow-sm z-10 transition-colors duration-200;
                }
                .nav-link {
                    @apply flex items-center space-x-3 px-4 py-2.5 text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-800 rounded-xl font-medium text-sm transition-all w-full text-left;
                }
                .nav-link-active {
                    @apply flex items-center space-x-3 px-4 py-2.5 bg-indigo-50 dark:bg-indigo-900/40 text-indigo-600 dark:text-indigo-400 rounded-xl font-semibold text-sm transition-colors w-full text-left;
                }
                .btn-primary {
                    @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-[#5c3cf5] text-white rounded-xl font-semibold hover:bg-indigo-700 transition-colors text-sm shadow-sm cursor-pointer;
                }
                .btn-secondary {
                    @apply flex items-center justify-center space-x-2 px-6 py-2.5 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 text-gray-700 dark:text-gray-300 rounded-xl font-semibold hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors text-sm shadow-sm cursor-pointer;
                }

                /* User Card Styles */
                .user-card {
                    @apply bg-white dark:bg-gray-800 border border-gray-100 dark:border-gray-700 rounded-2xl p-5 hover:shadow-md transition-all flex flex-col items-center text-center relative;
                }
                .avatar-lg {
                    @apply w-16 h-16 rounded-full bg-gradient-to-tr from-indigo-500 to-purple-500 flex items-center justify-center text-white text-2xl font-bold shadow-md mb-3;
                }
                .tab-link {
                    @apply pb-4 px-2 text-sm font-semibold transition-colors border-b-2;
                }
                .tab-active {
                    @apply text-indigo-600 dark:text-indigo-400 border-indigo-600 dark:border-indigo-400;
                }
                .tab-inactive {
                    @apply text-gray-500 dark:text-gray-400 border-transparent hover:text-gray-700 dark:hover:text-gray-300 hover:border-gray-300 dark:hover:border-gray-600;
                }
            }
        </style>
    </head>
    <body class="page-body">

        <div id="toastContainer" class="fixed top-5 right-5 z-[200] flex flex-col gap-3 pointer-events-none"></div>

<aside class="sidebar">
            <div class="space-y-6 w-full">
                <div class="flex items-center space-x-3 px-2 py-1">
                    <div class="w-9 h-9 bg-indigo-600 rounded-xl flex items-center justify-center text-white shadow-sm shadow-indigo-600/20">
                        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21.42 10.922a1 1 0 0 0-.019-1.838L12.83 5.18a2 2 0 0 0-1.66 0L2.6 9.08a1 1 0 0 0 0 1.832l8.57 3.908a2 2 0 0 0 1.66 0z"/><path d="M6 18.8V13.5"/><path d="M18 13.5v5.3a2.1 2.1 0 0 1-2 2h-8a2.1 2.1 0 0 1-2-2v-5.3"/></svg>
                    </div>
                    <span class="font-bold text-gray-900 dark:text-white text-base tracking-tight">AI Study Hub</span>
                </div>

                <nav class="space-y-1 w-full">
                    <a href="<%= request.getContextPath()%>/FolderController?action=viewFolder" class="nav-link">
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
                <div class="w-full bg-gradient-to-br from-purple-500 to-indigo-600 text-white p-4 rounded-2xl shadow-md shadow-indigo-600/10 relative overflow-hidden">
                    <div class="flex justify-between items-center opacity-85">
                        <span class="text-xs font-medium tracking-wide">Số dư ví</span>
                    </div>
                    <div class="text-xl font-bold mt-2 tracking-tight"><%= String.format("%,d", userBalance)%> Coin</div>
                </div>

                <div class="pt-2 border-t border-gray-100 dark:border-gray-800 flex flex-col gap-1">
                    <div class="flex items-center justify-between w-full gap-1">
                        
                        <a href="<%= request.getContextPath()%>/MainController?action=profile" class="flex items-center space-x-3 px-2 py-2 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors flex-1 min-w-0">
                            <div class="w-8 h-8 rounded-full bg-indigo-100 flex items-center justify-center text-indigo-700 flex-shrink-0 font-bold text-xs uppercase"><%= username != null && !username.trim().isEmpty() ? username.trim().substring(0, 1) : "U"%></div>
                            
                            <div class="user-info truncate">
                                <!-- Đã bổ sung thẻ Tier PRO/FREE -->
                                <div class="flex items-center gap-1.5 min-w-0">
                                    <p class="text-sm font-bold text-gray-900 dark:text-white truncate"><%= username != null ? username : "Học viên"%></p>
                                    <% if (isPremiumUser) { %>
                                    <span class="flex-shrink-0 px-1.5 py-0.5 bg-gradient-to-r from-amber-500 to-orange-500 text-white font-bold text-[9px] rounded-full shadow-sm scale-90 origin-left">PRO</span>
                                    <% } else { %>
                                    <span class="flex-shrink-0 px-2 py-0.5 bg-emerald-100 text-emerald-800 border border-emerald-300 dark:bg-emerald-900/60 dark:text-emerald-400 dark:border-emerald-700 font-bold text-[10px] rounded-full shadow-sm scale-90 origin-left tracking-wide">FREE</span>
                                    <% }%>
                                </div>
                                <p class="text-[11px] text-gray-400 font-medium">Quyền: <%= role%></p>
                            </div>
                        </a>

                        <a href="<%= request.getContextPath()%>/FriendController?action=friendList" 
                           class="p-2 bg-indigo-50 text-indigo-600 dark:bg-indigo-950/40 dark:text-indigo-400 rounded-xl transition-colors flex-shrink-0 shadow-sm" 
                           title="Danh sách bạn bè">
                            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/>
                                <circle cx="9" cy="7" r="4"/>
                                <path d="M22 21v-2a4 4 0 0 0-3-3.87"/>
                                <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                            </svg>
                        </a>
                    </div>

                    <!-- Nút Đăng xuất đã được điều chỉnh class Tailwind để khớp màu với trang gốc -->
                    <a href="<%= request.getContextPath()%>/MainController?action=logout" class="w-full flex items-center space-x-2.5 px-2 py-2 rounded-xl text-sm font-medium text-gray-500 hover:text-red-500 hover:bg-red-50 dark:text-gray-400 dark:hover:bg-red-900/30 dark:hover:text-red-400 transition-colors text-left">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" x2="9" y1="12" y2="12"/></svg>
                        <span>Đăng xuất</span>
                    </a>
                </div>
            </div>
        </aside>

        <main class="flex-1 p-8 overflow-y-auto h-screen relative flex flex-col">
            <div class="flex justify-between items-center mb-8 flex-shrink-0">
                <h1 class="text-2xl font-bold text-gray-900 dark:text-white tracking-tight">Cộng đồng & Kết nối</h1>
                <button onclick="document.getElementById('addFriendModal').classList.remove('hidden')" class="btn-primary">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2.2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4"></path></svg>
                    <span>Thêm bạn bè</span>
                </button>
            </div>

            <!-- KHU VỰC TAB ĐÃ ĐƯỢC CHỈNH SỬA -->
            <div class="flex space-x-6 border-b border-gray-200 dark:border-gray-700 mb-8 flex-shrink-0">
                <a href="<%= request.getContextPath()%>/FriendController?action=friendList" class="tab-link <%= currentAction.equals("friendList") ? "tab-active" : "tab-inactive"%>">
                    Bạn bè của tôi <%= friendCount > 0 ? "(" + friendCount + ")" : "" %>
                </a>
                
                <a href="<%= request.getContextPath()%>/FriendController?action=pendingList" class="tab-link <%= currentAction.equals("pendingList") ? "tab-active" : "tab-inactive"%>">
                    Lời mời kết bạn 
                    <% if (pendingCount > 0) { %>
                        <span class="ml-1 px-1.5 py-0.5 bg-red-500 text-white rounded-full text-[11px] font-bold shadow-sm"><%= pendingCount %></span>
                    <% } %>
                </a>
                
                <a href="<%= request.getContextPath()%>/FriendController?action=blockedList" class="tab-link <%= currentAction.equals("blockedList") ? "tab-active" : "tab-inactive"%>">
                    Người dùng đã chặn <%= blockedCount > 0 ? "(" + blockedCount + ")" : "" %>
                </a>
            </div>
            <div class="mb-6 relative flex-shrink-0">
                <input type="text" id="searchInput" onkeyup="filterFriends()" placeholder="Tìm kiếm người dùng trong danh sách này..." class="w-full px-4 py-3 pl-11 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-sm text-gray-900 dark:text-white outline-none focus:ring-2 focus:ring-indigo-500 transition-all shadow-sm">
                <svg class="w-5 h-5 absolute left-4 top-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
            </div>
            <div class="flex-1 overflow-y-auto">
                <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">

                    <%-- TAB 1: DANH SÁCH BẠN BÈ --%>
                    <% if (currentAction.equals("friendList")) {
                            if (friends == null || friends.isEmpty()) { %>
                    <div class="col-span-full py-16 text-center">
                        <div class="w-16 h-16 bg-gray-100 dark:bg-gray-800 rounded-full flex items-center justify-center text-gray-400 mx-auto mb-4">
                            <svg class="w-8 h-8" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path></svg>
                        </div>
                        <p class="text-gray-500 dark:text-gray-400 text-sm">Bạn chưa có người bạn nào. Hãy bắt đầu tìm kiếm và kết nối nhé!</p>
                    </div>
                    <% } else {
                        for (User f : friends) {%>
                    <div class="user-card">
                        <div class="avatar-lg"><%= f.getUsername().substring(0, 1).toUpperCase()%></div>
                        <h3 class="font-bold text-gray-900 dark:text-white text-lg truncate w-full px-2"><%= f.getUsername()%></h3>
                        <p class="text-xs text-gray-500 dark:text-gray-400 mb-5 truncate w-full px-2"><%= f.getEmail()%></p>

                        <div class="flex space-x-2 w-full mt-auto">
                            <a href="<%= request.getContextPath()%>/FriendController?action=deleteFriendship&targetUserId=<%= f.getUserId()%>&returnPath=friendList" onclick="return confirm('Bạn có chắc chắn muốn hủy kết bạn?');" class="flex-1 py-2 bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 text-gray-700 dark:text-gray-200 text-sm font-semibold rounded-xl transition-colors">Hủy kết bạn</a>
                            <a href="<%= request.getContextPath()%>/FriendController?action=updateFriendshipStatus&status=BLOCKED&targetUserId=<%= f.getUserId()%>" onclick="return confirm('Xác nhận chặn người dùng này?');" class="flex-1 py-2 border border-red-200 dark:border-red-900/50 text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 text-sm font-semibold rounded-xl transition-colors">Chặn</a>
                        </div>
                    </div>
                    <%      }
                            }
                        } %>

                    <%-- TAB 2: LỜI MỜI KẾT BẠN --%>
                    <% if (currentAction.equals("pendingList")) {
                            if (pendingRequests == null || pendingRequests.isEmpty()) { %>
                    <div class="col-span-full py-16 text-center">
                        <p class="text-gray-500 dark:text-gray-400 text-sm">Không có lời mời kết bạn nào đang chờ xử lý.</p>
                    </div>
                    <% } else {
                        for (User p : pendingRequests) {%>
                    <div class="user-card">
                        <a href="<%= request.getContextPath()%>/FriendController?action=updateFriendshipStatus&status=BLOCKED&targetUserId=<%= p.getUserId()%>" onclick="return confirm('Xác nhận chặn người dùng này?');" class="absolute top-4 right-4 text-gray-400 hover:text-red-500 transition-colors bg-white dark:bg-gray-800 rounded-full" title="Chặn người dùng này">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"></line>
                            </svg>
                        </a>
                        <div class="avatar-lg"><%= p.getUsername().substring(0, 1).toUpperCase()%></div>
                        <h3 class="font-bold text-gray-900 dark:text-white text-lg truncate w-full px-2"><%= p.getUsername()%></h3>
                        <p class="text-xs text-gray-500 dark:text-gray-400 mb-5 truncate w-full px-2"><%= p.getEmail()%></p>

                        <div class="flex space-x-2 w-full mt-auto">
                            <a href="<%= request.getContextPath()%>/FriendController?action=updateFriendshipStatus&status=ACCEPTED&targetUserId=<%= p.getUserId()%>" class="flex-1 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold rounded-xl transition-colors">Chấp nhận</a>
                            <a href="<%= request.getContextPath()%>/FriendController?action=deleteFriendship&targetUserId=<%= p.getUserId()%>&returnPath=pendingList" class="flex-1 py-2 bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 text-gray-700 dark:text-gray-200 text-sm font-semibold rounded-xl transition-colors">Từ chối</a>
                        </div>
                    </div>
                    <%      }
                            }
                        } %>

                    <%-- TAB 3: ĐÃ CHẶN --%>
                    <% if (currentAction.equals("blockedList")) {
                            if (blockedUsers == null || blockedUsers.isEmpty()) { %>
                    <div class="col-span-full py-16 text-center">
                        <p class="text-gray-500 dark:text-gray-400 text-sm">Danh sách chặn của bạn đang trống.</p>
                    </div>
                    <% } else {
                        for (User b : blockedUsers) {%>
                    <div class="user-card opacity-70 hover:opacity-100 transition-opacity">
                        <div class="avatar-lg bg-gradient-to-tr from-gray-500 to-gray-700"><%= b.getUsername().substring(0, 1).toUpperCase()%></div>
                        <h3 class="font-bold text-gray-900 dark:text-white text-lg truncate w-full px-2 line-through"><%= b.getUsername()%></h3>
                        <p class="text-xs text-gray-500 dark:text-gray-400 mb-5 truncate w-full px-2"><%= b.getEmail()%></p>

                        <div class="flex space-x-2 w-full mt-auto">
                            <a href="<%= request.getContextPath()%>/FriendController?action=deleteFriendship&targetUserId=<%= b.getUserId()%>&returnPath=blockedList" class="w-full py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 text-sm font-semibold rounded-xl transition-colors">Bỏ chặn</a>
                        </div>
                    </div>
                    <%      }
                            }
                        }%>

                </div>
            </div>
        </main>

        <div id="addFriendModal" class="fixed inset-0 z-50 hidden bg-gray-900/60 backdrop-blur-sm flex justify-center items-center">
            <div class="bg-white dark:bg-gray-800 w-full max-w-md rounded-2xl p-6 shadow-2xl">
                <div class="flex justify-between items-center mb-4">
                    <h2 class="text-xl font-bold text-gray-900 dark:text-white">Thêm bạn bè</h2>
                    <button onclick="document.getElementById('addFriendModal').classList.add('hidden')" class="text-gray-400 hover:text-red-500">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"></path></svg>
                    </button>
                </div>

                <form action="<%= request.getContextPath()%>/FriendController" method="GET">
                    <input type="hidden" name="action" value="findUserByEmail" />
                    <p class="text-sm text-gray-500 dark:text-gray-400 mb-4">Nhập địa chỉ Email của người bạn muốn kết nối vào ô bên dưới.</p>
                    <input type="email" name="email" required class="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-gray-700 focus:ring-2 focus:ring-indigo-500 bg-gray-50 dark:bg-gray-900 text-sm dark:text-white mb-6 outline-none transition-all" placeholder="ví dụ: hocvien@fpt.edu.vn" />

                    <div class="flex justify-end space-x-3">
                        <button type="button" onclick="document.getElementById('addFriendModal').classList.add('hidden')" class="btn-secondary px-5 py-2">Hủy</button>
                        <button type="submit" class="btn-primary px-5 py-2">Tìm kiếm</button>
                    </div>
                </form>
            </div>
        </div>

        <% if (searchedUser != null || searchError != null) { %>
        <div id="searchResultModal" class="fixed inset-0 z-[60] bg-gray-900/60 backdrop-blur-sm flex justify-center items-center">
            <div class="bg-white dark:bg-gray-800 w-full max-w-md rounded-2xl p-6 shadow-2xl text-center relative">
                <button onclick="document.getElementById('searchResultModal').classList.add('hidden')" class="absolute top-4 right-4 text-gray-400 hover:text-red-500">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"></path></svg>
                </button>

                <h2 class="text-xl font-bold text-gray-900 dark:text-white mb-6 text-left">Kết quả tìm kiếm</h2>

                <% if (searchError != null) {%>
                <div class="p-4 bg-red-50 dark:bg-red-900/20 rounded-xl text-red-600 dark:text-red-400 text-sm font-medium mb-4">
                    <%= searchError%>
                </div>
                <button onclick="document.getElementById('searchResultModal').classList.add('hidden'); document.getElementById('addFriendModal').classList.remove('hidden');" class="btn-secondary w-full py-2">Thử lại</button>
                <% } else if (searchedUser != null) {%>
                <div class="flex flex-col items-center justify-center p-4 border border-gray-100 dark:border-gray-700 rounded-2xl mb-6 bg-gray-50 dark:bg-gray-900/50">
                    <div class="avatar-lg mb-3"><%= searchedUser.getUsername().substring(0, 1).toUpperCase()%></div>
                    <h3 class="font-bold text-gray-900 dark:text-white text-lg"><%= searchedUser.getUsername()%></h3>
                    <p class="text-xs text-gray-500 dark:text-gray-400 mb-2"><%= searchedUser.getEmail()%></p>
                    <span class="px-2 py-0.5 bg-indigo-100 text-indigo-700 dark:bg-indigo-900/60 dark:text-indigo-300 rounded text-[10px] font-bold uppercase"><%= searchedUser.getRole()%></span>
                </div>

                <% if ("NONE".equals(friendshipStatus)) {%>
                <form action="<%= request.getContextPath()%>/FriendController" method="POST">
                    <input type="hidden" name="action" value="createFriendship">
                    <input type="hidden" name="addresseeId" value="<%= searchedUser.getUserId()%>">
                    <button type="submit" class="btn-primary w-full py-3">Gửi lời mời kết bạn</button>
                </form>

                <% } else if ("SELF".equals(friendshipStatus)) { %>
                <button disabled class="w-full py-3 bg-gray-200 dark:bg-gray-700 text-gray-500 dark:text-gray-400 rounded-xl font-semibold text-sm cursor-not-allowed">Đây là tài khoản của bạn</button>

                <% } else if ("PENDING_SENT".equals(friendshipStatus)) { %>
                <button disabled class="w-full py-3 bg-amber-100 dark:bg-amber-900/40 text-amber-700 dark:text-amber-400 rounded-xl font-semibold text-sm cursor-not-allowed">Bạn đang chờ người này phản hồi</button>

                <% } else if ("PENDING_RECEIVED".equals(friendshipStatus)) {%>
                <p class="text-sm text-gray-500 dark:text-gray-400 mb-4">Người này đã gửi cho bạn một lời mời kết bạn.</p>
                <div class="flex flex-col space-y-2">
                    <a href="<%= request.getContextPath()%>/FriendController?action=updateFriendshipStatus&status=ACCEPTED&targetUserId=<%= searchedUser.getUserId()%>"
                       class="w-full py-3 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold rounded-xl transition-colors text-center">Chấp nhận</a>
                    <div class="flex space-x-2">
                        <a href="<%= request.getContextPath()%>/FriendController?action=deleteFriendship&targetUserId=<%= searchedUser.getUserId()%>&returnPath=pendingList"
                           class="flex-1 py-2 bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 text-gray-700 dark:text-gray-200 text-sm font-semibold rounded-xl transition-colors text-center">Từ chối</a>
                        <a href="<%= request.getContextPath()%>/FriendController?action=updateFriendshipStatus&status=BLOCKED&targetUserId=<%= searchedUser.getUserId()%>"
                           onclick="return confirm('Xác nhận chặn người dùng này?');"
                           class="flex-1 py-2 border border-red-200 dark:border-red-900/50 text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 text-sm font-semibold rounded-xl transition-colors text-center">Chặn</a>
                    </div>
                </div>

                <% } else if ("ACCEPTED".equals(friendshipStatus)) { %>
                <button disabled class="w-full py-3 bg-emerald-100 dark:bg-emerald-900/40 text-emerald-700 dark:text-emerald-400 rounded-xl font-semibold text-sm cursor-not-allowed">Đã là bạn bè</button>

                <% } else if ("BLOCKED_BY_ME".equals(friendshipStatus)) {%>
                <p class="text-sm text-red-500 font-medium mb-3">Bạn đã chặn người dùng này.</p>
                <a href="<%= request.getContextPath()%>/FriendController?action=deleteFriendship&targetUserId=<%= searchedUser.getUserId()%>&returnPath=blockedList"
                   class="w-full py-3 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 text-sm font-semibold rounded-xl transition-colors block text-center">Bỏ chặn</a>

                <% } else if ("BLOCKED_BY_THEM".equals(friendshipStatus)) { %>
                <button disabled class="w-full py-3 bg-red-100 dark:bg-red-900/40 text-red-700 dark:text-red-400 rounded-xl font-semibold text-sm cursor-not-allowed">Bạn đã bị người dùng này chặn</button>
                <% } %>
                <% } %>
            </div>
        </div>
        <% }%>

        <script>
            function showToast(message, type = 'success') {
                const container = document.getElementById('toastContainer');
                if (!container)
                    return;
                const toast = document.createElement('div');
                toast.className = `flex items-center space-x-3 px-5 py-3.5 bg-white dark:bg-gray-800 text-gray-800 dark:text-white rounded-2xl shadow-xl border border-gray-100 dark:border-gray-700 pointer-events-auto transition-all duration-300 translate-x-20 opacity-0 min-w-[280px] max-w-md`;


                let iconHtml = '';
                if (type === 'success') {
                    iconHtml = `<div class="p-1.5 bg-emerald-100 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400 rounded-xl"><svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"></path></svg></div>`;
                } else if (type === 'error') {
                    iconHtml = `<div class="p-1.5 bg-red-100 dark:bg-red-950/50 text-red-600 dark:text-red-400 rounded-xl"><svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"></path></svg></div>`;
                } else {
                    iconHtml = `<div class="p-1.5 bg-blue-100 dark:bg-blue-950/50 text-blue-600 dark:text-blue-400 rounded-xl"><svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg></div>`;
                }

                toast.innerHTML = iconHtml + '<span class="text-sm font-semibold tracking-tight">' + message + '</span>';
                container.appendChild(toast);

                setTimeout(() => toast.classList.remove('translate-x-20', 'opacity-0'), 50);
                setTimeout(() => {
                    toast.classList.add('opacity-0', 'translate-x-10');
                    setTimeout(() => toast.remove(), 300);
                }, 4000);
            }

            document.addEventListener("DOMContentLoaded", function () {
                const urlParams = new URLSearchParams(window.location.search);
                if (urlParams.has('success')) {
                    const s = urlParams.get('success');
                    if (s === 'request_sent')
                        showToast("Đã gửi lời mời kết bạn!", "success");
                    if (s === 'status_updated')
                        showToast("Đã cập nhật trạng thái thành công!", "success");
                    if (s === 'deleted')
                        showToast("Đã thực hiện thao tác xóa thành công!", "success");
                }
                if (urlParams.has('error')) {
                    const e = urlParams.get('error');
                    if (e === 'already_exists')
                        showToast("Quan hệ bạn bè đã tồn tại!", "error");
                    if (e === 'self_request')
                        showToast("Không thể tự kết bạn với chính mình!", "error");
                    if (e === 'update_failed')
                        showToast("Lỗi cập nhật trạng thái!", "error");
                    if (e === 'not_authorized')
                        showToast("Bạn không có quyền thực hiện thao tác này!", "error");
                }

                // Clean URL
                if (urlParams.has('success') || urlParams.has('error')) {
                    const cleanUrl = window.location.protocol + "//" + window.location.host + window.location.pathname + "?action=<%= currentAction%>";
                    window.history.replaceState({}, document.title, cleanUrl);
                }
            });
            // Hàm lọc danh sách hiển thị (Search)
            function filterFriends() {
                const input = document.getElementById('searchInput').value.toLowerCase();
                const cards = document.querySelectorAll('.user-card');
                cards.forEach(card => {
                    // Lấy tên người dùng từ thẻ h3 bên trong mỗi card
                    const username = card.querySelector('h3').innerText.toLowerCase();
                    if (username.includes(input)) {
                        card.style.display = 'flex'; // Hiển thị lại nếu khớp
                    } else {
                        card.style.display = 'none'; // Ẩn đi nếu không khớp
                    }
                });
            }
        </script>
    </body>
</html>