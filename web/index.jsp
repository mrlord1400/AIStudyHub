<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // 1. Read the user's role securely from the Java session
    String role = (String) session.getAttribute("role");
    
    // 2. Determine the target destination based on the role
    String targetUrl = "login.jsp"; // Default to login if not authenticated

    if (role != null) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            targetUrl = "admin_dashboard.jsp";
        } else if ("STUDENT".equalsIgnoreCase(role)) {
            targetUrl = "user_dashboard.jsp";
        } else if ("GUEST".equalsIgnoreCase(role)) {
            targetUrl = "user_dashboard.jsp"; // Adjust this if you create a specific guest.jsp
        }
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AI Study Hub - Đang chuyển hướng...</title>
    
    <meta http-equiv="refresh" content="1;url=<%= request.getContextPath() %>/<%= targetUrl %>">
    
    <style>
        /* Giao diện màn hình chờ tải trang đơn giản trong lúc xử lý điều hướng */
        body {
            margin: 0;
            padding: 0;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            background-color: #f8fafc;
            font-family: 'Inter', sans-serif;
            color: #4b5563;
        }
        .loader {
            text-align: center;
        }
        .spinner {
            width: 40px;
            height: 40px;
            border: 4px solid #ececf0;
            border-top: 4px solid #5c3cf5;
            border-radius: 50%;
            animation: spin 1s linear infinite;
            margin: 0 auto 12px;
        }
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
    </style>
</head>
<body>

    <div class="loader">
        <div class="spinner"></div>
        <p>Đang tải hệ thống, vui lòng chờ...</p>
    </div>

    <script>
        // Use the server-computed target URL to redirect the user.
        // A slight delay (400ms) is added so the nice loading animation is actually visible.
        setTimeout(function() {
            window.location.href = "<%= request.getContextPath() %>/<%= targetUrl %>";
        }, 400);
    </script>
</body>
</html>