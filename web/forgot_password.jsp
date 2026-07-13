<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="vi" class="dark">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>AI Study Hub - Quên mật khẩu</title>
        <script src="https://cdn.tailwindcss.com"></script>
        <script>
            tailwind.config = {darkMode: 'class'};
        </script>
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
        <style type="text/tailwindcss">
            html.dark body { background-color: #111827; color: #f3f4f6; }
            html.dark .form-container { background-color: #111827; }
            html.dark .input-field { background-color: #374151; border-color: #4b5563; color: #ffffff; }
            html.dark .input-field:focus { background-color: #1f2937; border-color: #5c3cf5; }
            .bg-brand-gradient { background: linear-gradient(135deg, #4f22ffd1 0%, #7c3aed 100%); }
            .otp-box { width: 3rem; height: 3.5rem; text-align: center; font-size: 1.5rem; font-weight: 600; }
        </style>
    </head>

    <body class="flex min-h-screen w-full text-gray-800 dark:text-gray-100 bg-[#f8fafc]">
        <div class="flex flex-col md:flex-row min-h-screen w-full">
            <div class="w-full md:w-1/2 flex flex-col justify-between items-center p-8 bg-white dark:bg-gray-900 min-h-screen form-container">
                <div class="hidden md:block"></div>

                <div class="w-full max-w-md my-auto">
                    <div class="text-center mb-8">
                        <div class="inline-flex items-center justify-center w-20 h-20 bg-[#5c3cf5] text-white rounded-[24px] mb-4 shadow-lg shadow-indigo-600/20">
                            <svg class="w-12 h-12" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" viewBox="0 0 24 24">
                            <path d="M12 3.5L2 8.5L12 13.5L22 8.5L12 3.5Z" />
                            <path d="M6 11V15.5C6 18 8.5 20 12 20C15.5 20 18 18 18 15.5V11" />
                            </svg>
                        </div>
                        <h1 class="text-3xl font-bold text-gray-900 dark:text-white tracking-tight">AI Study Hub</h1>
                        <p id="step-subtitle" class="text-sm text-gray-500 dark:text-gray-400 mt-1">Khôi phục mật khẩu của bạn</p>
                    </div>

                    <div id="step-email">
                        <div id="email-alert" class="hidden mb-4 p-3 text-sm text-center rounded-lg border"></div>
                        <div class="mb-4">
                            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Email tài khoản</label>
                            <input type="email" id="input-email" placeholder="your.email@fpt.edu.vn" class="input-field w-full px-4 py-3 bg-[#f3f3f5] rounded-xl text-sm">
                        </div>
                        <button id="btn-send-otp" type="button" class="w-full bg-[#5c3cf5] text-white py-3 rounded-xl font-semibold hover:bg-indigo-700 shadow-md">Gửi mã xác nhận</button>
                        <a href="login.jsp" class="block text-center text-xs font-semibold text-[#5c3cf5] dark:text-indigo-400 hover:underline mt-6">← Quay lại đăng nhập</a>
                    </div>

                    <div id="step-otp" class="hidden">
                        <p class="text-sm text-gray-500 dark:text-gray-400 text-center mb-6">
                            Nếu email này tồn tại trong hệ thống, mã xác nhận 6 số đã được gửi đến <br>
                            <span id="otp-target-email" class="font-semibold text-gray-800 dark:text-gray-200"></span>
                        </p>
                        <div id="otp-alert" class="hidden mb-4 p-3 text-sm text-center rounded-lg border"></div>
                        <div class="flex justify-center gap-2 mb-4">
                            <input type="text" maxlength="1" class="otp-digit otp-box input-field bg-[#f3f3f5] rounded-xl focus:ring-2 focus:ring-indigo-500">
                            <input type="text" maxlength="1" class="otp-digit otp-box input-field bg-[#f3f3f5] rounded-xl focus:ring-2 focus:ring-indigo-500">
                            <input type="text" maxlength="1" class="otp-digit otp-box input-field bg-[#f3f3f5] rounded-xl focus:ring-2 focus:ring-indigo-500">
                            <input type="text" maxlength="1" class="otp-digit otp-box input-field bg-[#f3f3f5] rounded-xl focus:ring-2 focus:ring-indigo-500">
                            <input type="text" maxlength="1" class="otp-digit otp-box input-field bg-[#f3f3f5] rounded-xl focus:ring-2 focus:ring-indigo-500">
                            <input type="text" maxlength="1" class="otp-digit otp-box input-field bg-[#f3f3f5] rounded-xl focus:ring-2 focus:ring-indigo-500">
                        </div>
                        <p class="text-center text-sm text-gray-500 dark:text-gray-400 mb-6">Mã hết hạn sau <span id="countdown" class="font-semibold text-[#5c3cf5]">02:00</span></p>
                        <button id="btn-verify-otp" type="button" class="w-full bg-[#5c3cf5] text-white py-3 rounded-xl font-semibold hover:bg-indigo-700 shadow-md">Xác nhận</button>
                        <button id="btn-resend-otp" type="button" class="w-full text-[#5c3cf5] dark:text-indigo-400 py-3 rounded-xl font-semibold text-sm hover:underline mt-2">Gửi lại mã</button>
                        <a href="login.jsp" class="block text-center text-xs font-semibold text-gray-400 hover:underline mt-4">← Quay lại đăng nhập</a>
                    </div>

                    <div id="step-locked" class="hidden text-center">
                        <div class="inline-flex items-center justify-center w-16 h-16 bg-red-50 text-red-500 rounded-full mb-4">
                            <svg class="w-8 h-8" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path></svg>
                        </div>
                        <p id="locked-message" class="text-sm text-gray-600 mb-6">Tính năng tạm khóa do nhập sai quá 3 lần. Vui lòng thử lại sau 12 tiếng.</p>
                        <a href="login.jsp" class="block text-center text-xs font-semibold text-[#5c3cf5] hover:underline">← Quay lại đăng nhập</a>
                    </div>
                </div>
                <p class="text-center text-xs text-gray-400 mt-6 font-medium">Dự án SWP391 - Đại học FPT</p>
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
                    </div>

                </div>
            </div>

        </div>

        <script>
            const stepEmail = document.getElementById('step-email');
            const stepOtp = document.getElementById('step-otp');
            const stepLocked = document.getElementById('step-locked');
            const subtitle = document.getElementById('step-subtitle');

            const inputEmail = document.getElementById('input-email');
            const emailAlert = document.getElementById('email-alert');
            const btnSendOtp = document.getElementById('btn-send-otp');

            const otpAlert = document.getElementById('otp-alert');
            const otpDigits = document.querySelectorAll('.otp-digit');
            const otpTargetEmail = document.getElementById('otp-target-email');
            const countdownEl = document.getElementById('countdown');
            const btnVerifyOtp = document.getElementById('btn-verify-otp');
            const btnResendOtp = document.getElementById('btn-resend-otp');
            const lockedMessage = document.getElementById('locked-message');

            let currentEmail = '';
            let countdownTimer = null;
            let secondsLeft = 120;

            function showAlert(el, message, type) {
                el.classList.remove('hidden', 'bg-red-50', 'text-red-600', 'bg-green-50', 'text-green-600');
                if (type === 'success') {
                    el.classList.add('bg-green-50', 'text-green-600');
                } else {
                    el.classList.add('bg-red-50', 'text-red-600');
                }
                el.innerText = message;
                el.classList.remove('hidden');
            }

            function goToLockedStep(message) {
                clearInterval(countdownTimer);
                stepEmail.classList.add('hidden');
                stepOtp.classList.add('hidden');
                stepLocked.classList.remove('hidden');
                subtitle.innerText = 'Tính năng tạm thời bị khóa';
                if (message)
                    lockedMessage.innerText = message;
            }

            function startCountdown() {
                secondsLeft = 120;
                clearInterval(countdownTimer);
                btnVerifyOtp.disabled = false;
                btnVerifyOtp.classList.remove('opacity-50', 'cursor-not-allowed');

                countdownTimer = setInterval(() => {
                    secondsLeft--;
                    const m = String(Math.floor(secondsLeft / 60)).padStart(2, '0');
                    const s = String(secondsLeft % 60).padStart(2, '0');
                    countdownEl.innerText = m + ':' + s;

                    if (secondsLeft <= 0) {
                        clearInterval(countdownTimer);
                        countdownEl.innerText = 'Hết hạn';
                        btnVerifyOtp.disabled = true;
                        btnVerifyOtp.classList.add('opacity-50', 'cursor-not-allowed');
                        showAlert(otpAlert, 'Mã OTP đã hết hạn. Vui lòng bấm "Gửi lại mã".', 'error');
                    }
                }, 1000);
            }

            async function callForgotPasswordApi(action, extraParams) {
                const params = new URLSearchParams({action: action, email: currentEmail, ...extraParams});
                const res = await fetch('<%= request.getContextPath()%>/ForgotPasswordController', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                    body: params.toString()
                });
                if (!res.ok)
                    throw new Error("HTTP Status: " + res.status);
                return res.json();
            }

            btnSendOtp.addEventListener('click', async () => {
                const email = inputEmail.value.trim();
                if (!email) {
                    showAlert(emailAlert, 'Vui lòng nhập email.', 'error');
                    return;
                }

                currentEmail = email;
                btnSendOtp.disabled = true;
                btnSendOtp.innerText = 'Đang gửi...';

                try {
                    const data = await callForgotPasswordApi('sendOTP', {});
                    if (data.status === 'success') {
                        otpTargetEmail.innerText = currentEmail;
                        stepEmail.classList.add('hidden');
                        stepOtp.classList.remove('hidden');
                        subtitle.innerText = 'Nhập mã xác nhận được gửi qua Email';
                        otpDigits.forEach(d => d.value = '');
                        otpDigits[0].focus();
                        startCountdown();
                    } else if (data.locked) {
                        goToLockedStep(data.message);
                    } else {
                        showAlert(emailAlert, data.message || 'Có lỗi xảy ra, vui lòng thử lại.', 'error');
                    }
                } catch (e) {
                    showAlert(emailAlert, 'Không thể kết nối máy chủ. Vui lòng kiểm tra lại!', 'error');
                } finally {
                    btnSendOtp.disabled = false;
                    btnSendOtp.innerText = 'Gửi mã xác nhận';
                }
            });

            btnResendOtp.addEventListener('click', async () => {
                btnResendOtp.disabled = true;
                btnResendOtp.innerText = 'Đang gửi lại...';
                try {
                    const data = await callForgotPasswordApi('sendOTP', {});
                    if (data.status === 'success') {
                        otpDigits.forEach(d => d.value = '');
                        otpDigits[0].focus();
                        otpAlert.classList.add('hidden');
                        startCountdown();
                    } else if (data.locked) {
                        goToLockedStep(data.message);
                    } else {
                        showAlert(otpAlert, data.message, 'error');
                    }
                } catch (e) {
                    showAlert(otpAlert, 'Không thể kết nối máy chủ.', 'error');
                } finally {
                    btnResendOtp.disabled = false;
                    btnResendOtp.innerText = 'Gửi lại mã';
                }
            });

            btnVerifyOtp.addEventListener('click', async () => {
                const otp = Array.from(otpDigits).map(d => d.value).join('');
                if (otp.length !== 6) {
                    showAlert(otpAlert, 'Vui lòng nhập đủ 6 số.', 'error');
                    return;
                }

                btnVerifyOtp.disabled = true;
                btnVerifyOtp.innerText = 'Đang xác nhận...';

                try {
                    const data = await callForgotPasswordApi('verifyOTP', {otp: otp});
                    if (data.status === 'success') {
                        clearInterval(countdownTimer);
                        window.location.href = 'reset_password.jsp?email=' + encodeURIComponent(currentEmail);
                    } else if (data.locked) {
                        goToLockedStep(data.message);
                    } else {
                        showAlert(otpAlert, data.message || 'Mã OTP không đúng.', 'error');
                        otpDigits.forEach(d => d.value = '');
                        otpDigits[0].focus();
                    }
                } catch (e) {
                    showAlert(otpAlert, 'Không thể kết nối máy chủ.', 'error');
                } finally {
                    btnVerifyOtp.disabled = false;
                    btnVerifyOtp.innerText = 'Xác nhận';
                }
            });

            otpDigits.forEach((input, idx) => {
                input.addEventListener('input', () => {
                    input.value = input.value.replace(/[^0-9]/g, '');
                    if (input.value && idx < otpDigits.length - 1)
                        otpDigits[idx + 1].focus();
                });
                input.addEventListener('keydown', (e) => {
                    if (e.key === 'Backspace' && !input.value && idx > 0)
                        otpDigits[idx - 1].focus();
                });
                input.addEventListener('paste', (e) => {
                    e.preventDefault();
                    const pasted = (e.clipboardData || window.clipboardData).getData('text').replace(/[^0-9]/g, '');
                    pasted.split('').slice(0, otpDigits.length).forEach((ch, i) => {
                        otpDigits[i].value = ch;
                    });
                    const nextEmpty = Array.from(otpDigits).findIndex(d => !d.value);
                    (nextEmpty === -1 ? otpDigits[otpDigits.length - 1] : otpDigits[nextEmpty]).focus();
                });
            });

            document.getElementById('input-email').addEventListener('keydown', (e) => {
                if (e.key === 'Enter')
                    btnSendOtp.click();
            });
        </script>
    </body>
</html>