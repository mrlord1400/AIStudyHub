<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AI Study Hub - Đăng nhập / Đăng ký</title>
    
    <script src="https://cdn.tailwindcss.com"></script>

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    
    <style>
      :root {
        --background: #ffffff;
        --foreground: #252525;
        --input-background: #f3f3f5;
        --brand-purple: #5c3cf5; 
        --brand-gradient: linear-gradient(135deg, #4f22ffd1 0%, #7c3aed 100%);
      }

      body {
        font-family: 'Inter', sans-serif;
        background-color: #f8fafc;
        margin: 0;
        padding: 0;
      }
      
      .bg-brand-gradient {
        background: var(--brand-gradient);
      }
    </style>
</head>

<body>
    <div class="flex flex-col md:flex-row min-h-screen w-full">
        
        <div class="w-full md:w-1/2 flex flex-col justify-between items-center p-8 bg-white min-h-screen">
            
            <div class="hidden md:block"></div>

            <div class="w-full max-w-md my-auto">
                
                <div class="text-center mb-8">
                    <div class="inline-flex items-center justify-center w-20 h-20 bg-[#5c3cf5] text-white rounded-[24px] mb-4 shadow-lg shadow-indigo-100">
                        <svg class="w-12 h-12" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" viewBox="0 0 24 24">
                            <path d="M12 3.5L2 8.5L12 13.5L22 8.5L12 3.5Z" />
                            <path d="M6 11V15.5C6 18 8.5 20 12 20C15.5 20 18 18 18 15.5V11" />
                        </svg>
                    </div>
                    <h1 class="text-3xl font-bold text-gray-900 tracking-tight">AI Study Hub</h1>
                    <p id="form-subtitle" class="text-sm text-gray-500 mt-1">Hệ thống Quản lý Tài liệu Học tập AI</p>
                </div>

                <div class="flex p-1 bg-gray-100 rounded-xl mb-6 select-none">
                    <button id="tab-login" onclick="switchTab('login')" class="flex-1 py-2.5 text-sm font-semibold rounded-lg bg-white text-gray-900 shadow-sm transition-all">Đăng nhập</button>
                    <button id="tab-register" onclick="switchTab('register')" class="flex-1 py-2.5 text-sm font-medium rounded-lg text-gray-500 hover:text-gray-900 transition-all">Đăng ký</button>
                </div>

                <%-- Server Notification Messages --%>
                <% 
                    String error = request.getParameter("error");
                    String register = request.getParameter("register");
                    if ("invalid_credentials".equals(error)) { 
                %>
                    <div class="mb-4 p-3 bg-red-50 text-red-600 text-sm text-center rounded-lg border border-red-100">Sai email hoặc mật khẩu. Vui lòng thử lại!</div>
                <% } else if ("register_failed".equals(error)) { %>
                    <div class="mb-4 p-3 bg-red-50 text-red-600 text-sm text-center rounded-lg border border-red-100">Đăng ký thất bại. Email có thể đã tồn tại!</div>
                <% } else if ("success".equals(register)) { %>
                    <div class="mb-4 p-3 bg-green-50 text-green-600 text-sm text-center rounded-lg border border-green-100">Đăng ký thành công! Vui lòng đăng nhập.</div>
                <% } else if ("account_deleted".equals(request.getParameter("account_deleted"))) { %>
                    <div class="mb-4 p-3 bg-blue-50 text-blue-600 text-sm text-center rounded-lg border border-blue-100">Tài khoản của bạn đã được xóa.</div>
                <% } %>

                <%-- Main Form submitting to MainController --%>
                <form id="auth-form" action="MainController" method="POST" class="space-y-4">
                    
                    <%-- Hidden Inputs for Controller Routing --%>
                    <input type="hidden" name="action" id="form-action" value="login">

                    <%-- Username Input (Hidden by default in Login mode) --%>
                    <div id="username-block" class="hidden">
                        <label class="block text-sm font-medium text-gray-700 mb-2">Tên người dùng (Username)</label>
                        <div class="relative">
                            <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path></svg>
                            </span>
                            <input type="text" name="username" id="input-username" placeholder="Nhập tên đăng nhập" class="w-full pl-10 pr-4 py-3 bg-[#f3f3f5] border border-transparent rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition-all text-sm">
                        </div>
                    </div>

                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-2">Email</label>
                        <div class="relative">
                            <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"></path></svg>
                            </span>
                            <input type="email" name="email" id="input-email" placeholder="your.email@fpt.edu.vn" class="w-full pl-10 pr-4 py-3 bg-[#f3f3f5] border border-transparent rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition-all text-sm" required>
                        </div>
                    </div>

                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-2">Mật khẩu</label>
                        <div class="relative">
                            <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path></svg>
                            </span>
                            <input type="password" name="password" id="input-password" placeholder="••••••••" class="w-full pl-10 pr-12 py-3 bg-[#f3f3f5] border border-transparent rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition-all text-sm" required>
                            <button type="button" onclick="const p = document.getElementById('input-password'); p.type = p.type === 'password' ? 'text' : 'password';" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path><path stroke-linecap="round" stroke-linejoin="round" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path></svg>
                            </button>
                        </div>
                    </div>

                    <div id="remember-row" class="flex items-center justify-between text-sm py-1">
                        <label class="flex items-center cursor-pointer select-none">
                            <input type="checkbox" name="remember" class="w-4 h-4 mr-2 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500">
                            <span class="text-gray-600 font-medium text-xs">Ghi nhớ đăng nhập</span>
                        </label>
                        <a href="#" class="text-xs font-semibold text-[#5c3cf5] hover:underline">Quên mật khẩu?</a>
                    </div>

                    <button type="submit" id="main-submit-btn" class="w-full bg-[#5c3cf5] text-white py-3 rounded-xl font-semibold hover:bg-indigo-700 transition-colors shadow-md shadow-indigo-100 text-sm">
                        Đăng nhập
                    </button>
                </form>

                <div id="social-login-block" class="space-y-4">
                    <button id="btn-guest" type="button" class="w-full flex items-center justify-center space-x-2 border border-gray-200 text-gray-700 py-3 rounded-xl hover:bg-gray-50 transition-colors mt-3 font-medium text-sm">
                        <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path></svg>
                        <span>Tiếp tục với tư cách Khách</span>
                    </button>

                    <div class="mt-6">
                        <div class="relative">
                            <div class="absolute inset-0 flex items-center"><div class="w-full border-t border-gray-200"></div></div>
                        </div>
                    </div>
                </div>

            </div>

            <p class="text-center text-xs text-gray-400 mt-6 font-medium tracking-wide">
                Dự án SWP391 - Đại học FPT
            </p>
        </div>

        <div class="hidden md:flex w-full md:w-1/2 bg-brand-gradient items-center justify-center p-16 text-white min-h-screen">
            <div class="max-w-lg w-full">
                <h2 class="text-4xl font-bold mb-4 leading-tight tracking-tight">Chào mừng đến với AI Study Hub</h2>
                <p class="text-lg mb-10 text-purple-100 font-light leading-relaxed">Nền tảng quản lý và chia sẻ tài liệu học tập thông minh với sức mạnh của AI</p>
                
                <div class="space-y-6">
                    <div class="flex items-start">
                        <div class="w-11 h-11 bg-white/15 rounded-xl flex items-center justify-center mr-4 flex-shrink-0 backdrop-blur-sm">
                            <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
                        </div>
                        <div>
                            <h3 class="font-semibold text-base mb-0.5">Quản lý tài liệu thông minh</h3>
                            <p class="text-sm text-purple-100/80">Tổ chức và quản lý tài liệu học tập một cách hiệu quả</p>
                        </div>
                    </div>
                    
                    <div class="flex items-start">
                        <div class="w-11 h-11 bg-white/15 rounded-xl flex items-center justify-center mr-4 flex-shrink-0 backdrop-blur-sm">
                            <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"></path></svg>
                        </div>
                        <div>
                            <h3 class="font-semibold text-base mb-0.5">AI Chatbot hỗ trợ học tập</h3>
                            <p class="text-sm text-purple-100/80">Trợ lý AI giúp bạn giải đáp thắc mắc và học tập hiệu quả hơn</p>
                        </div>
                    </div>
                    
                    <div class="flex items-start">
                        <div class="w-11 h-11 bg-white/15 rounded-xl flex items-center justify-center mr-4 flex-shrink-0 backdrop-blur-sm">
                            <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"></path></svg>
                        </div>
                        <div>
                            <h3 class="font-semibold text-base mb-0.5">Thư viện phong phú</h3>
                            <p class="text-sm text-purple-100/80">Truy cập hàng ngàn tài liệu học tập chất lượng cao</p>
                        </div>
                    </div>
                </div>

            </div>
        </div>

    </div>

    <script>
        let currentActiveMode = "login"; 

        const actionInput = document.getElementById('form-action');
        const submitBtn = document.getElementById('main-submit-btn');
        const subtitle = document.getElementById('form-subtitle');
        const rememberRow = document.getElementById('remember-row');
        const socialBlock = document.getElementById('social-login-block');
        
        // Các DOM element mới cho Username
        const usernameBlock = document.getElementById('username-block');
        const usernameInput = document.getElementById('input-username');

        // Handles UI tab switching and hidden action parameter updates
        function switchTab(mode) {
            currentActiveMode = mode;
            actionInput.value = mode; // Changes value to 'login' or 'register' for MainController
            
            const tabLogin = document.getElementById('tab-login');
            const tabRegister = document.getElementById('tab-register');

            if (mode === "login") {
                tabLogin.className = "flex-1 py-2.5 text-sm font-semibold rounded-lg bg-white text-gray-900 shadow-sm transition-all";
                tabRegister.className = "flex-1 py-2.5 text-sm font-medium rounded-lg text-gray-500 hover:text-gray-900 transition-all";
                
                subtitle.innerText = "Hệ thống Quản lý Tài liệu Học tập AI";
                submitBtn.innerText = "Đăng nhập";
                
                rememberRow.classList.remove('hidden');
                socialBlock.classList.remove('hidden');
                
                // Ẩn field Username và loại bỏ thuộc tính bắt buộc nhập
                usernameBlock.classList.add('hidden');
                usernameInput.removeAttribute('required');
                
            } else {
                tabLogin.className = "flex-1 py-2.5 text-sm font-medium rounded-lg text-gray-500 hover:text-gray-900 transition-all";
                tabRegister.className = "flex-1 py-2.5 text-sm font-semibold rounded-lg bg-white text-gray-900 shadow-sm transition-all";
                
                subtitle.innerText = "Tạo tài khoản học tập miễn phí ngay hôm nay";
                submitBtn.innerText = "Đăng ký tài khoản mới";
                
                rememberRow.classList.add('hidden');
                socialBlock.classList.add('hidden');
                
                // Hiển thị field Username và thêm thuộc tính bắt buộc nhập
                usernameBlock.classList.remove('hidden');
                usernameInput.setAttribute('required', 'required');
            }
        }

        // Guest login redirect
        const guestButtonElement = document.getElementById('btn-guest');
        if (guestButtonElement) {
            guestButtonElement.addEventListener('click', function() {
                window.location.href = "MainController?action=guest"; 
            });
        }
    </script>
</body>
</html>