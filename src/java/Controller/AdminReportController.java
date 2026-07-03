package Controller;

import Model.DAO.ReportDAO;
import Model.DAO.DocumentDAO;
import Model.DTO.Report;
import Model.DTO.Document;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "AdminReportController", urlPatterns = {"/AdminReportController"})
public class AdminReportController extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        // 1. Kiểm tra bảo mật Admin tối cao
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }
        
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equalsIgnoreCase(role)) {
            response.sendRedirect(request.getContextPath() + "/user_dashboard.jsp");
            return;
        }

        int adminId = (int) session.getAttribute("userId"); // ID của admin đang đăng nhập
        String action = request.getParameter("action");

        ReportDAO reportDao = new ReportDAO();
        DocumentDAO docDao = new DocumentDAO();

        try {
            if ("adminReportList".equals(action)) {
                // Lấy ID tài liệu cần xem danh sách tố cáo
                int documentId = Integer.parseInt(request.getParameter("documentId"));
                
                // Lấy thông tin chi tiết tài liệu và danh sách report liên quan
                Document document = docDao.findById(documentId);
                List<Report> documentReports = reportDao.getReportList(documentId);

                // Đóng gói dữ liệu chuyển sang giao diện JSP
                request.setAttribute("document", document);
                request.setAttribute("documentReports", documentReports);
                request.getRequestDispatcher("/admin_document_reports.jsp").forward(request, response);

            } else if ("adminUpdateReportStatus".equals(action)) {
                // Đổi trạng thái xử lý của report (PENDING -> REVIEWED)
                int reportId = Integer.parseInt(request.getParameter("reportId"));
                int documentId = Integer.parseInt(request.getParameter("documentId"));
                String newStatus = request.getParameter("status");

                // Vì hàm update của team yêu cầu truyền nguyên Object, ta tìm lại record cũ để set dữ liệu mới
                List<Report> allReps = reportDao.getReportList(documentId);
                for (Report r : allReps) {
                    if (r.getReportId() == reportId) {
                        r.setStatus(newStatus);
                        r.setAdminId(adminId); // Ghi nhận Admin thực hiện giải quyết
                        r.setResolvedAt(java.time.LocalDateTime.now()); // Lưu vết thời gian giải quyết
                        reportDao.updateReport(r); // Gọi hàm update an toàn của team
                        break;
                    }
                }
                response.sendRedirect(request.getContextPath() + "/MainController?action=adminReportList&documentId=" + documentId);

            } else if ("adminDeleteReport".equals(action)) {
                // Xóa bỏ lượt report rác từ user tố cáo xàm
                int reportId = Integer.parseInt(request.getParameter("reportId"));
                int documentId = Integer.parseInt(request.getParameter("documentId"));

                Report reportToDelete = new Report();
                reportToDelete.setReportId(reportId);
                reportDao.deleteReport(reportToDelete);

                response.sendRedirect(request.getContextPath() + "/MainController?action=adminReportList&documentId=" + documentId);

            } else if ("adminDeleteDocument".equals(action)) {
                // Lệnh hạt nhân: Xóa tài liệu vi phạm nặng
                int documentId = Integer.parseInt(request.getParameter("documentId"));
                
                // Gọi hàm dọn dẹp liên hoàn bằng Transaction mà ta đã viết ở DocumentDAO
                boolean isDeleted = docDao.deleteDocumentAndDependencies(documentId);

                if (isDeleted) {
                    // Xóa sạch xong thì đá về trang dashboard quản lý chung của admin
                    response.sendRedirect(request.getContextPath() + "/MainController?action=listDashboard");
                } else {
                    response.sendRedirect(request.getContextPath() + "/MainController?action=adminReportList&documentId=" + documentId + "&error=delete_failed");
                }
            }
        } catch (Exception e) {
            System.err.println("[AdminReportController Error] " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/MainController?action=listDashboard&error=system_error");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}