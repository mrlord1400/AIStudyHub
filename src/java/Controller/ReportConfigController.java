package Controller;

import Model.DAO.ReportReasonDAO;
import Model.DTO.ReportReason; // Thay đổi import sang Object của team
import Utils.Access;
import Utils.Validation;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "ReportConfigController", urlPatterns = {"/admin/report-config", "/ReportConfigController"})
public class ReportConfigController extends HttpServlet {

    private final ReportReasonDAO reasonDAO = new ReportReasonDAO();

    // Mã lý do: chữ/số/gạch dưới/gạch ngang, tối đa 50 ký tự (cột reason_code NVARCHAR(50))
    private static final Pattern REASON_CODE = Pattern.compile("^[A-Za-z0-9_\\-]{1,50}$");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // AuthFilter đã chặn trước; đây là lớp phòng thủ thứ hai.
        if (!Access.isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
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
                // Xóa làm thay đổi dữ liệu nên chỉ nhận POST (Conv §4)
                response.setHeader("Allow", "POST");
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                break;

            default:
                response.sendRedirect(request.getContextPath() + "/MainController?action=reportConfigList");
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!Access.isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        String reasonCode = request.getParameter("reasonCode");
        String invalidRedirect = request.getContextPath() + "/MainController?action=reportConfigList&error=invalid_input";

        if (!Validation.isOneOf(action, "reportConfigAdd", "reportConfigUpdate", "reportConfigDelete")
                || reasonCode == null || !REASON_CODE.matcher(reasonCode).matches()) {
            response.sendRedirect(invalidRedirect);
            return;
        }

        if ("reportConfigDelete".equals(action)) {
            ReportReason deleteTarget = new ReportReason();
            deleteTarget.setReasonCode(reasonCode);
            reasonDAO.deleteReportReason(deleteTarget);
            response.sendRedirect(request.getContextPath() + "/MainController?action=reportConfigList");
            return;
        }

        String severityLevel = request.getParameter("severityLevel");
        // DB: DECIMAL(5,2), tối đa 999.99
        Double baseScore = Validation.parseDoubleInRange(request.getParameter("baseScore"), 0, 999.99);
        Double autoFlagThreshold = Validation.parseDoubleInRange(request.getParameter("autoFlagThreshold"), 0, 999.99);
        String description = request.getParameter("description");
        if (!Validation.isOneOf(severityLevel, "LOW", "MEDIUM", "HIGH", "CRITICAL")
                || baseScore == null || autoFlagThreshold == null
                || (description != null && description.length() > 255)) {
            response.sendRedirect(invalidRedirect);
            return;
        }

        // Dùng Object ReportReason của team để đóng gói dữ liệu
        ReportReason config = new ReportReason();
        config.setReasonCode(reasonCode);
        config.setSeverityLevel(severityLevel);
        config.setBaseScore(baseScore);
        config.setAutoFlagThreshold(autoFlagThreshold);
        config.setDescription(description);

        if ("reportConfigAdd".equals(action)) {
            reasonDAO.createReportReason(config); // Đổi thành createReportReason() của team
        } else {
            reasonDAO.updateReportReason(config); // Đổi thành updateReportReason() của team
        }

        response.sendRedirect(request.getContextPath() + "/MainController?action=reportConfigList");
    }
}