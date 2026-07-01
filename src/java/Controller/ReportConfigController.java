package Controller;

import Model.DAO.ReportReasonDAO;
import Model.DTO.ReportReasonConfig;
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
                List<ReportReasonConfig> reasonList = reasonDAO.getAllReasons();
                request.setAttribute("reasonList", reasonList);
                request.getRequestDispatcher("admin_report_config.jsp").forward(request, response);
                break;

            case "reportConfigDelete":
                String codeToDelete = request.getParameter("reasonCode");
                if (codeToDelete != null) {
                    reasonDAO.deleteReason(codeToDelete);
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
            // Thay thế dấu phẩy thành dấu chấm đề phòng người dùng nhập sai format
            baseScore = Double.parseDouble(request.getParameter("baseScore").replace(",", "."));
            autoFlagThreshold = Double.parseDouble(request.getParameter("autoFlagThreshold").replace(",", "."));
        } catch (NumberFormatException | NullPointerException e) {
            System.err.println("[ReportConfigController] Lỗi parse số thập phân: " + e.getMessage());
        }
        String description = request.getParameter("description");

        ReportReasonConfig config = new ReportReasonConfig(reasonCode, severityLevel, baseScore, autoFlagThreshold, description);

        if ("reportConfigAdd".equals(action)) {
            reasonDAO.addReason(config);
        } else if ("reportConfigUpdate".equals(action)) {
            reasonDAO.updateReason(config);
        }

        // Xử lý xong chuyển hướng về lại danh sách
        response.sendRedirect(request.getContextPath() + "/MainController?action=reportConfigList");
    }
}