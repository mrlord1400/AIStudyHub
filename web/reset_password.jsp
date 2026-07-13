<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String targetEmail = request.getParameter("email");
    if (targetEmail == null || targetEmail.trim().isEmpty()) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    HttpSession userSession = request.getSession(false);
    Boolean canReset = (userSession != null) ? (Boolean) userSession.getAttribute("ALLOW_RESET_" + targetEmail) : null;
    
    if (canReset == null || !canReset) {
        response.sendRedirect(request.getContextPath() + "/login.jsp?error=unauthorized");
        return;
    }
%>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AI Study Hub - Đặt lại mật khẩu</title>

    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = { darkMode: 'class' };
    </script>

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">

    <style type="text/tailwindcss">
        html.dark body { background-color: #111827; color: #f3f4f6; }
        html.dark .form-container { background-color: #111827; }
        html.dark .input-field { background-color: #374151; border-color: #4b5563; color: #ffffff; }
        html.dark .input-field:focus { background-color: #1f2937; border-color: #5c3cf5; }
        .bg-brand-gradient { background: linear-gradient(135deg, #4f22ffd1 0%, #7c3aed 100%); }
    </style>
</head>

<body class="flex min-h-screen w-full text-gray-800 dark:text-gray-100 bg-[#f8fafc]">
    <div class="flex flex-col md:flex-row min-h-screen w-full">

        <div class="w-full md:w-1/2 flex flex-col justify-between items-center p-8 bg-white dark:bg-gray-900 min-h-screen form-container">
            <div class="hidden md:block"></div>
            <div class="w-full max-w-md my-auto">
                <div class="text-center mb-8">
                    <div class="inline-flex items-center justify-center w-20 h-20 bg-green-500 text-white rounded-[24px] mb-4 shadow-lg shadow-green-500/20">
                        <svg class="w-10 h-10" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                        </svg>
                    </div>
                    <h1 class="text-3xl font-bold text-gray-900 dark:text-white tracking-tight">Xác thực thành công</h1>
                    <p class="text-sm text-gray-500 dark:text-gray-400 mt-2">
                        Tạo mật khẩu mới cho tài khoản <br>
                        <strong class="text-gray-800 dark:text-gray-200"><%= targetEmail %></strong>
                    </p>
                </div>
                
                <%
                    String errorMsg = request.getParameter("error");
                    if ("weak_password".equals(errorMsg)) {
                %>
                <div class="mb-4 p-3 bg-red-50 dark:bg-red-950/40 text-red-600 dark:text-red-400 text-sm text-center rounded-lg border border-red-100 dark:border-red-900/50">Mật khẩu không đạt yêu cầu bảo mật! (Cần 8 ký tự, có số, chữ hoa, ký tự đặc biệt).</div>
                <% } %>
                <div id="reset-alert" class="hidden mb-4 p-3 text-sm text-center rounded-lg border"></div>

                <form id="reset-form" action="MainController" method="POST" class="space-y-4">
                    <input type="hidden" name="action" value="processResetPassword">
                    <input type="hidden" name="email" value="<%= targetEmail %>">

                    <div>
                        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Mật khẩu mới</label>
                        <div class="relative">
                            <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 dark:text-gray-500">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path></svg>
                            </span>
                            <input type="password" name="newPassword" id="new-password" placeholder="Tối thiểu 8 ký tự, có số & chữ hoa" 
                                   class="input-field w-full pl-10 pr-12 py-3 bg-[#f3f3f5] border border-transparent rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all text-sm" required>
                            <button type="button" onclick="togglePassword('new-password')" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-300">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path><path stroke-linecap="round" stroke-linejoin="round" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path></svg>
                            </button>
                        </div>
                    </div>

                    <div>
                        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Xác nhận mật khẩu mới</label>
                        <div class="relative">
                            <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 dark:text-gray-500">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"></path></svg>
                            </span>
                            <input type="password" id="confirm-password" placeholder="Nhập lại mật khẩu mới" 
                                   class="input-field w-full pl-10 pr-12 py-3 bg-[#f3f3f5] border border-transparent rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all text-sm" required>
                            <button type="button" onclick="togglePassword('confirm-password')" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-300">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path><path stroke-linecap="round" stroke-linejoin="round" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path></svg>
                            </button>
                        </div>
                    </div>

                    <button type="submit" id="btn-submit" class="w-full bg-green-500 text-white py-3 rounded-xl font-semibold hover:bg-green-600 transition-colors shadow-md shadow-green-500/20 text-sm mt-4 cursor-pointer">
                        Lưu mật khẩu và Đăng nhập
                    </button>
                </form>
            </div>
            <p class="text-center text-xs text-gray-400 dark:text-gray-500 mt-6 font-medium tracking-wide">Dự án SWP391 - Đại học FPT</p>
        </div>

        <div class="hidden md:flex w-full md:w-1/2 bg-brand-gradient items-center justify-center p-16 text-white min-h-screen">
            <div class="max-w-lg w-full">
                <h2 class="text-4xl font-bold mb-4 leading-tight tracking-tight">Tuyệt vời!</h2>
                <p class="text-lg mb-10 text-purple-100 font-light leading-relaxed">Bạn đã xác thực thành công. Vui lòng tạo một mật khẩu mới đủ mạnh để bảo vệ tài khoản của bạn.</p>

                <div class="space-y-6">
                    <div class="flex items-start">
                        <div class="w-11 h-11 bg-white/15 rounded-xl flex items-center justify-center mr-4 flex-shrink-0 backdrop-blur-sm">
                            <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"></path></svg>
                        </div>
                        <div>
                            <h3 class="font-semibold text-base mb-0.5">Lời khuyên bảo mật</h3>
                            <p class="text-sm text-purple-100/80">Sử dụng ít nhất 8 ký tự. Phải có chữ cái in hoa, chữ số, và ký tự đặc biệt (!@#$%...).</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

    </div>

    <script>
        function togglePassword(inputId) {
            const input = document.getElementById(inputId);
            if (input.type === 'password') {
                input.type = 'text';
            } else {
                input.type = 'password';
            }
        }

        document.getElementById('reset-form').addEventListener('submit', function(e) {
            const pwd = document.getElementById('new-password').value;
            const confirmPwd = document.getElementById('confirm-password').value;
            const alertBox = document.getElementById('reset-alert');

            // BR-15, BR-16, BR-17, BR-17b Validation
            const hasUppercase = /[A-Z]/.test(pwd);
            const hasNumber = /[0-9]/.test(pwd);
            const hasSpecial = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(pwd);

            if (pwd !== confirmPwd) {
                e.preventDefault(); 
                alertBox.classList.remove('hidden', 'bg-green-50', 'text-green-600', 'border-green-100');
                alertBox.classList.add('bg-red-50', 'dark:bg-red-950/40', 'text-red-600', 'dark:text-red-400', 'border-red-100', 'dark:border-red-900/50');
                alertBox.innerText = 'Mật khẩu xác nhận không trùng khớp!';
            } else if (pwd.length < 8 || !hasUppercase || !hasNumber || !hasSpecial) {
                e.preventDefault();
                alertBox.classList.remove('hidden', 'bg-green-50', 'text-green-600', 'border-green-100');
                alertBox.classList.add('bg-red-50', 'dark:bg-red-950/40', 'text-red-600', 'dark:text-red-400', 'border-red-100', 'dark:border-red-900/50');
                alertBox.innerText = 'Mật khẩu phải từ 8 ký tự, có chữ hoa, chữ số & ký tự đặc biệt!';
            }
        });
    </script>
</body>
</html>