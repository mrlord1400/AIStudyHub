package Controller;

import Model.DAO.ReportReasonDAO;
import Model.DTO.ReportReason; // Thay đổi import sang Object của team
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "ReportConfigController", urlPatterns = {"/admin/report-config", "/ReportConfigController"})
public class ReportConfigController extends HttpServlet {

    private final ReportReasonDAO reasonDAO = new ReportReasonDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null) {
            action = "reportConfigList";
        }

        switch (action) {
            case "reportConfigList":
                // Đổi thành getAllReportReason() theo code của team
                List<ReportReason> reasonList = reasonDAO.getAllReportReason();
                request.setAttribute("reasonList", reasonList);
                request.getRequestDispatcher("admin_report_config.jsp").forward(request, response);
                break;

            case "reportConfigDelete":
                String codeToDelete = request.getParameter("reasonCode");
                if (codeToDelete != null) {
                    // Hàm xóa của team yêu cầu truyền vào nguyên 1 object, nên mình khởi tạo và set ID cho nó
                    ReportReason deleteTarget = new ReportReason();
                    deleteTarget.setReasonCode(codeToDelete);
                    reasonDAO.deleteReportReason(deleteTarget);
                }
                response.sendRedirect(request.getContextPath() + "/MainController?action=reportConfigList");
                break;
                
            default:
                response.sendRedirect(request.getContextPath() + "/MainController?action=reportConfigList");
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        String reasonCode = request.getParameter("reasonCode");
        String severityLevel = request.getParameter("severityLevel");
        double baseScore = 0;
        double autoFlagThreshold = 0;
        
        try {
            baseScore = Double.parseDouble(request.getParameter("baseScore").replace(",", "."));
            autoFlagThreshold = Double.parseDouble(request.getParameter("autoFlagThreshold").replace(",", "."));
        } catch (NumberFormatException | NullPointerException e) {
            System.err.println("[ReportConfigController] Lỗi parse số thập phân: " + e.getMessage());
        }
        String description = request.getParameter("description");

        // Dùng Object ReportReason của team để đóng gói dữ liệu
        ReportReason config = new ReportReason();
        config.setReasonCode(reasonCode);
        config.setSeverityLevel(severityLevel);
        config.setBaseScore(baseScore);
        config.setAutoFlagThreshold(autoFlagThreshold);
        config.setDescription(description);

        if ("reportConfigAdd".equals(action)) {
            reasonDAO.createReportReason(config); // Đổi thành createReportReason() của team
        } else if ("reportConfigUpdate".equals(action)) {
            reasonDAO.updateReportReason(config); // Đổi thành updateReportReason() của team
        }

        response.sendRedirect(request.getContextPath() + "/MainController?action=reportConfigList");
    }
}