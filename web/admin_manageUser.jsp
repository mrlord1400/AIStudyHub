<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="Model.User" %>
<%@ page import="java.util.List" %>
<%
    // 1. Ensure user is logged in AND is an Admin
    HttpSession userSession = request.getSession(false);
    if (userSession == null || userSession.getAttribute("userId") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    String currentUserRole = (String) userSession.getAttribute("role");
    if (!"ADMIN".equalsIgnoreCase(currentUserRole)) {
        // Kick non-admins back to their dashboard
        response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp");
        return;
    }

    String currentUsername = (String) userSession.getAttribute("username");

    // Grab the user list passed from AdminController
    List<User> userList = (List<User>) request.getAttribute("user_list");
%>
<!DOCTYPE html>
<html lang="vi" class="dark">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Quản lý người dùng - AI Study Hub Admin</title>

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
                    @apply flex items-center justify-center space-x-2 px-5 py-2.5 bg-[#5c3cf5] text-white rounded-xl font-semibold hover:bg-indigo-700 transition-colors text-sm shadow-sm cursor-pointer;
                }
                .btn-secondary {
                    @apply flex items-center justify-center space-x-2 px-5 py-2.5 bg-white border border-gray-200 text-gray-700 rounded-xl font-semibold hover:bg-gray-50 transition-colors text-sm shadow-sm cursor-pointer dark:bg-gray-800 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700;
                }
                .form-input {
                    @apply w-full px-4 py-2.5 rounded-xl border border-gray-200 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all bg-gray-50 focus:bg-white text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-white dark:focus:bg-gray-800;
                }
                .form-label {
                    @apply block text-sm font-medium text-gray-700 mb-1.5 dark:text-gray-300;
                }
            }
        </style>
    </head>
    <body class="page-body">

        <aside class="sidebar">
            <div class="space-y-6 w-full">
                <a href="<%= request.getContextPath()%>/AdminController?action=dashboard" class="flex items-center space-x-3 px-2 py-1 transition-opacity hover:opacity-80 block w-full">
                    <div class="w-9 h-9 bg-red-600 rounded-xl flex items-center justify-center text-white shadow-sm">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path></svg>
                    </div>
                    <span class="font-bold text-gray-900 text-base tracking-tight dark:text-white">Admin Panel</span>
                </a>

                <nav class="space-y-1 w-full">
                    <a href="<%= request.getContextPath()%>/AdminController?action=listDashboard" class="nav-link">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/></svg>
                        <span>Dashboard</span>
                    </a>
                    <a href="<%= request.getContextPath()%>/AdminController?action=listUsers" class="nav-link-active">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path></svg>
                        <span>Quản lý người dùng</span>
                    </a>
                    <a href="<%= request.getContextPath()%>/AdminController?action=listTransactions" class="nav-link">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/></svg>
                        <span>Quản lý giao dịch</span>
                    </a>
                </nav>
            </div>

            <div class="pt-4 border-t border-gray-100 dark:border-gray-700">
                <a href="<%= request.getContextPath()%>/admin_profile.jsp" class="flex items-center space-x-3 px-2 py-2 mb-2 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors cursor-pointer group">
                    <div class="w-8 h-8 rounded-full bg-red-100 flex items-center justify-center text-red-700 font-bold text-xs uppercase group-hover:bg-red-200 transition-colors"><%= currentUsername != null ? currentUsername.substring(0, 1) : "A"%></div>
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
        </aside>

        <main class="flex-1 p-8 overflow-y-auto h-screen relative">
            <div class="flex justify-between items-center mb-6">
                <h1 class="text-2xl font-bold text-gray-900 tracking-tight dark:text-white">Danh sách người dùng</h1>
                <button onclick="openCreateModal()" class="btn-primary">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4"></path></svg>
                    <span>Thêm tài khoản</span>
                </button>
            </div>

            <div class="bg-white border border-gray-100 rounded-2xl shadow-sm overflow-hidden dark:bg-gray-800 dark:border-gray-700">
                <div class="overflow-x-auto">
                    <table class="w-full text-left text-sm text-gray-600 dark:text-gray-300">
                        <thead class="bg-gray-50 text-xs text-gray-500 uppercase tracking-wider dark:bg-gray-900/50 dark:text-gray-400 border-b border-gray-100 dark:border-gray-700">
                            <tr>
                                <th class="px-6 py-4 font-semibold">ID</th>
                                <th class="px-6 py-4 font-semibold">Người dùng</th>
                                <th class="px-6 py-4 font-semibold">Ví / Tier</th>
                                <th class="px-6 py-4 font-semibold">Phân quyền</th>
                                <th class="px-6 py-4 font-semibold">Trạng thái</th>
                                <th class="px-6 py-4 font-semibold text-right">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-gray-100 dark:divide-gray-700">
                            <% if (userList != null && !userList.isEmpty()) {
                                for (User u : userList) {%>
                            <tr class="hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
                                <td class="px-6 py-4 font-medium">#<%= u.getUserId()%></td>
                                <td class="px-6 py-4">
                                    <p class="font-bold text-gray-900 dark:text-white"><%= u.getUsername()%></p>
                                    <p class="text-xs text-gray-500"><%= u.getEmail() != null ? u.getEmail() : "N/A"%></p>
                                </td>
                                <td class="px-6 py-4">
                                    <p class="font-semibold text-amber-600 dark:text-amber-400"><%= String.format("%,d", u.getBalance())%> Coin</p>
                                    <p class="text-[11px] text-gray-400 uppercase tracking-wide">Tier: <%= u.getTierId()%></p>
                                </td>
                                <td class="px-6 py-4">
                                    <% if ("ADMIN".equals(u.getRole())) { %>
                                    <span class="px-2.5 py-1 text-[11px] font-bold bg-red-100 text-red-700 rounded-lg dark:bg-red-900/30 dark:text-red-400">ADMIN</span>
                                    <% } else {%>
                                    <span class="px-2.5 py-1 text-[11px] font-bold bg-indigo-100 text-indigo-700 rounded-lg dark:bg-indigo-900/30 dark:text-indigo-400"><%= u.getRole()%></span>
                                    <% } %>
                                </td>
                                <td class="px-6 py-4">
                                    <% if ("ACTIVE".equals(u.getStatus())) { %>
                                    <span class="flex items-center space-x-1.5 text-emerald-600 dark:text-emerald-400 font-medium text-xs">
                                        <span class="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse"></span>
                                        <span>Hoạt động</span>
                                    </span>
                                    <% } else if ("SUSPENDED".equals(u.getStatus())) { %>
                                    <span class="flex items-center space-x-1.5 text-amber-500 font-medium text-xs">
                                        <span class="w-1.5 h-1.5 bg-amber-500 rounded-full"></span>
                                        <span>Tạm khóa</span>
                                    </span>
                                    <% } else { %>
                                    <span class="flex items-center space-x-1.5 text-rose-500 font-medium text-xs">
                                        <span class="w-1.5 h-1.5 bg-rose-500 rounded-full"></span>
                                        <span>Banned</span>
                                    </span>
                                    <% }%>
                                </td>
                                <td class="px-6 py-4 text-right space-x-2">
                                    <button onclick="openEditModal('<%= u.getUserId()%>', '<%= u.getUsername()%>', '<%= u.getEmail() != null ? u.getEmail() : ""%>', '<%= u.getBalance()%>', '<%= u.getRole()%>', '<%= u.getTierId()%>', '<%= u.getStatus()%>')" class="p-2 text-gray-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors dark:hover:bg-gray-700 inline-flex" title="Chỉnh sửa">
                                        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path></svg>
                                    </button>

                                    <form action="<%= request.getContextPath()%>/AdminController" method="POST" class="inline-block" onsubmit="return confirm('CẢNH BÁO: Hành động này sẽ xóa vĩnh viễn người dùng này cùng toàn bộ tài liệu và lịch sử chat của họ. Bạn có chắc chắn?');">
                                        <input type="hidden" name="action" value="deleteUser" />
                                        <input type="hidden" name="user_id" value="<%= u.getUserId()%>" />
                                        <button type="submit" class="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors dark:hover:bg-gray-700 inline-flex" title="Xóa tài khoản">
                                            <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                                        </button>
                                    </form>
                                </td>
                            </tr>
                            <%  }
                        } else { %>
                            <tr>
                                <td colspan="6" class="px-6 py-8 text-center text-gray-500">Chưa có dữ liệu người dùng.</td>
                            </tr>
                            <% }%>
                        </tbody>
                    </table>
                </div>
            </div>
        </main>

        <div id="createUserModal" class="fixed inset-0 z-50 hidden bg-gray-900/60 backdrop-blur-sm flex justify-center items-center">
            <div class="bg-white w-full max-w-md rounded-2xl p-6 shadow-2xl dark:bg-gray-800">
                <h2 class="text-xl font-bold text-gray-900 mb-6 dark:text-white">Thêm người dùng mới</h2>
                <form action="<%= request.getContextPath()%>/AdminController" method="POST" class="space-y-4">
                    <input type="hidden" name="action" value="createUser" />

                    <div>
                        <label class="form-label">Tên đăng nhập</label>
                        <input type="text" name="username" required class="form-input" placeholder="Nhập username..." />
                    </div>
                    <div>
                        <label class="form-label">Email</label>
                        <input type="email" name="email" required class="form-input" placeholder="example@mail.com" />
                    </div>
                    <div>
                        <label class="form-label">Mật khẩu</label>
                        <input type="password" name="password" required class="form-input" placeholder="••••••••" />
                    </div>
                    <div>
                        <label class="form-label">Loại tài khoản</label>
                        <select name="role" class="form-input cursor-pointer">
                            <option value="STUDENT">Premium (VIP)</option>
                            <option value="ADMIN">Admin (Quản trị viên)</option>
                        </select>
                    </div>

                    <div class="flex justify-end space-x-3 pt-4">
                        <button type="button" onclick="closeCreateModal()" class="btn-secondary">Hủy</button>
                        <button type="submit" class="btn-primary">Tạo tài khoản</button>
                    </div>
                </form>
            </div>
        </div>

        <div id="editUserModal" class="fixed inset-0 z-50 hidden bg-gray-900/60 backdrop-blur-sm flex justify-center items-center">
            <div class="bg-white w-full max-w-lg rounded-2xl p-6 shadow-2xl dark:bg-gray-800">
                <h2 class="text-xl font-bold text-gray-900 mb-6 dark:text-white">Cập nhật thông tin</h2>
                <form action="<%= request.getContextPath()%>/AdminController" method="POST" class="space-y-4">
                    <input type="hidden" name="action" value="updateUser" />
                    <input type="hidden" name="user_id" id="editUserId" />

                    <div class="grid grid-cols-2 gap-4">
                        <div>
                            <label class="form-label">Tên đăng nhập</label>
                            <input type="text" name="newUsername" id="editUsername" required class="form-input" />
                        </div>
                        <div>
                            <label class="form-label">Email</label>
                            <input type="email" name="newEmail" id="editEmail" required class="form-input" />
                        </div>
                    </div>

                    <div class="grid grid-cols-2 gap-4">
                        <div>
                            <label class="form-label">Số dư Coin</label>
                            <input type="number" name="newBalance" id="editBalance" required class="form-input" min="0" />
                        </div>
                        <div>
                            <label class="form-label">Cấp bậc (Tier ID)</label>
                            <select name="newTierId" id="editTier" class="form-input cursor-pointer">
                                <option value="1">1 - Free</option>
                                <option value="2">2 - Premium</option>
                            </select>
                        </div>
                    </div>

                    <div class="grid grid-cols-2 gap-4">
                        <div>
                            <label class="form-label">Phân quyền (Role)</label>
                            <select name="newRole" id="editRole" class="form-input cursor-pointer">
                                <option value="GUEST">GUEST</option>
                                <option value="STUDENT">STUDENT</option>
                                <option value="ADMIN">ADMIN</option>
                            </select>
                        </div>
                        <div>
                            <label class="form-label">Trạng thái</label>
                            <select name="newStatus" id="editStatus" class="form-input cursor-pointer">
                                <option value="ACTIVE">Hoạt động (ACTIVE)</option>
                                <option value="SUSPENDED">Tạm khóa (SUSPENDED)</option>
                                <option value="BANNED">Cấm (BANNED)</option>
                            </select>
                        </div>
                    </div>

                    <div class="flex justify-end space-x-3 pt-4 border-t border-gray-100 dark:border-gray-700 mt-6">
                        <button type="button" onclick="closeEditModal()" class="btn-secondary">Hủy</button>
                        <button type="submit" class="btn-primary">Lưu thay đổi</button>
                    </div>
                </form>
            </div>
        </div>

        <script>
            function openCreateModal() {
                document.getElementById('createUserModal').classList.remove('hidden');
            }

            function closeCreateModal() {
                document.getElementById('createUserModal').classList.add('hidden');
            }

            function openEditModal(id, username, email, balance, role, tier, status) {
                document.getElementById('editUserId').value = id;
                document.getElementById('editUsername').value = username;
                document.getElementById('editEmail').value = email;
                document.getElementById('editBalance').value = balance;
                document.getElementById('editRole').value = role;
                document.getElementById('editTier').value = tier;
                document.getElementById('editStatus').value = status;
                document.getElementById('editUserModal').classList.remove('hidden');
            }

            function closeEditModal() {
                document.getElementById('editUserModal').classList.add('hidden');
            }
        </script>
    </body>
</html>