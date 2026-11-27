package Controller;

import Model.DTO.Subscription;
import Model.DAO.SubscriptionDAO;
import Utils.Access;
import Utils.Validation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "SystemConfigController", urlPatterns = {"/admin/system-config"})
public class SystemConfigController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // AuthFilter đã chặn trước; đây là lớp phòng thủ thứ hai.
        if (!Access.isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        SubscriptionDAO dao = new SubscriptionDAO();
        List<Subscription> subList = dao.getAllSubscriptions();

        request.setAttribute("subList", subList);
        request.getRequestDispatcher("/admin_system_config.jsp").forward(request, response);
    }

    // Trần hợp lý cho từng trường (cột DB là INT / DECIMAL(10,2))
    private static final int MAX_STORAGE_MB = 10_000_000;
    private static final int MAX_AI_LIMIT = 100_000;
    private static final double MAX_PRICE = 99_999_999;

    /** @return danh sách gói đã kiểm tra hợp lệ, hoặc null nếu BẤT KỲ dòng nào không hợp lệ. */
    private List<Subscription> parseAndValidate(HttpServletRequest request, List<Subscription> existing) {
        String[] tierIds = request.getParameterValues("tierId");
        if (tierIds == null || tierIds.length == 0) {
            return null;
        }
        Set<Integer> knownTiers = new HashSet<Integer>();
        for (Subscription sub : existing) {
            knownTiers.add(sub.getTierId());
        }

        List<Subscription> result = new ArrayList<Subscription>();
        Set<Integer> seen = new HashSet<Integer>();
        for (String idStr : tierIds) {
            Integer tierId = Validation.parseIntInRange(idStr, 1, Integer.MAX_VALUE);
            if (tierId == null || !knownTiers.contains(tierId) || !seen.add(tierId)) {
                return null;
            }
            Integer maxStorage = Validation.parseIntInRange(request.getParameter("maxStorage_" + tierId), 0, MAX_STORAGE_MB);
            Integer aiLimit = Validation.parseIntInRange(request.getParameter("aiLimit_" + tierId), 0, MAX_AI_LIMIT);
            Double price = Validation.parseDoubleInRange(request.getParameter("price_" + tierId), 0, MAX_PRICE);
            Integer totalStorage = Validation.parseIntInRange(request.getParameter("totalStorage_" + tierId), 0, MAX_STORAGE_MB);
            if (maxStorage == null || aiLimit == null || price == null || totalStorage == null) {
                return null;
            }
            result.add(new Subscription(tierId, "", maxStorage, aiLimit, price, totalStorage));
        }
        return result;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!Access.isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        request.setCharacterEncoding("UTF-8");
        SubscriptionDAO dao = new SubscriptionDAO();

        // Kiểm tra TẤT CẢ các dòng trước, rồi mới ghi trong một transaction (F-11)
        List<Subscription> toSave = parseAndValidate(request, dao.getAllSubscriptions());
        if (toSave == null) {
            request.setAttribute("errorMessage", "Dữ liệu cấu hình không hợp lệ. Không có thay đổi nào được lưu.");
        } else if (dao.updateSubscriptions(toSave)) {
            request.setAttribute("successMessage", "Đã cập nhật cấu hình hệ thống thành công!");
        } else {
            request.setAttribute("errorMessage", "Có lỗi xảy ra khi lưu cấu hình!");
        }

        List<Subscription> subList = dao.getAllSubscriptions();
        request.setAttribute("subList", subList);
        request.getRequestDispatcher("/admin_system_config.jsp").forward(request, response);
    }
}