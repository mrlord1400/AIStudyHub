package Controller;

import Model.DAO.ReportDAO;
import Model.DAO.DocumentDAO;
import Model.DAO.ReportReasonDAO;
import Model.DTO.Report;
import Model.DTO.Document;
import Model.DTO.ReportReason;

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
                int documentId = Integer.parseInt(request.getParameter("documentId"));
                Document document = docDao.findById(documentId);
                List<Report> documentReports = reportDao.getReportList(documentId);

                request.setAttribute("document", document);
                request.setAttribute("documentReports", documentReports);
                request.getRequestDispatcher("/admin_document_reports.jsp").forward(request, response);

            } else if ("adminUpdateReportStatus".equals(action)) {
                int reportId = Integer.parseInt(request.getParameter("reportId"));
                int documentId = Integer.parseInt(request.getParameter("documentId"));
                String newStatus = request.getParameter("status");

                List<Report> allReps = reportDao.getReportList(documentId);
                for (Report r : allReps) {
                    if (r.getReportId() == reportId) {
                        r.setStatus(newStatus);
                        r.setAdminId(adminId); 
                        r.setResolvedAt(java.time.LocalDateTime.now()); 
                        reportDao.updateReport(r); 
                        break;
                    }
                }
                response.sendRedirect(request.getContextPath() + "/MainController?action=adminReportList&documentId=" + documentId);

            } else if ("adminDeleteReport".equals(action)) {
                // Xóa bỏ lượt report rác từ user tố cáo xàm và HỒI ĐIỂM
                int reportId = Integer.parseInt(request.getParameter("reportId"));
                int documentId = Integer.parseInt(request.getParameter("documentId"));

                // Lấy thông tin report trước khi xóa để lấy mã lỗi (reason code)
                Report targetReport = null;
                List<Report> reps = reportDao.getReportList(documentId);
                for (Report r : reps) {
                    if (r.getReportId() == reportId) {
                        targetReport = r;
                        break;
                    }
                }

                if (targetReport != null) {
                    ReportReasonDAO reasonDao = new ReportReasonDAO();
                    ReportReason reason = reasonDao.getReasonByCode(targetReport.getReasonCode());
                    double scoreToDeduct = (reason != null) ? reason.getBaseScore() : 0.0;
                    double autoFlagThreshold = (reason != null) ? reason.getAutoFlagThreshold() : 999.0;

                    // Thực hiện xóa report
                    boolean isDeleted = reportDao.deleteReport(targetReport);
                    
                    if (isDeleted) {
                        // Cập nhật và trừ điểm lại cho Document
                        Document doc = docDao.findById(documentId);
                        if (doc != null) {
                            double newScore = doc.getTotalReportScore() - scoreToDeduct;
                            if (newScore < 0) {
                                newScore = 0.0;
                            }
                            doc.setTotalReportScore(newScore);

                            // Cập nhật lại cờ vi phạm (Flag)
                            if (newScore >= autoFlagThreshold && newScore > 0) {
                                doc.setIsFlagged(true);
                            } else {
                                doc.setIsFlagged(false);
                            }

                            // Lưu điểm số mới xuống Database
                            docDao.updateReportMetrics(doc.getDocumentId(), doc.getTotalReportScore(), doc.isFlagged());
                        }
                    }
                }

                response.sendRedirect(request.getContextPath() + "/MainController?action=adminReportList&documentId=" + documentId);

            } else if ("adminDeleteDocument".equals(action)) {
                int documentId = Integer.parseInt(request.getParameter("documentId"));
                boolean isDeleted = docDao.deleteDocumentAndDependencies(documentId);

                if (isDeleted) {
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