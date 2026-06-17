package Controller;

import Model.DTO.Subscription;
import Model.DAO.SubscriptionDAO;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "SystemConfigController", urlPatterns = {"/admin/system-config"})
public class SystemConfigController extends HttpServlet {

    // doGet: Khi Admin bấm vào menu "Quản lý hệ thống", tải dữ liệu từ DB lên và đẩy sang giao diện JSP
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        SubscriptionDAO dao = new SubscriptionDAO();
        List<Subscription> subList = dao.getAllSubscriptions();
        
        // Gửi danh sách qua cho JSP hiển thị
        request.setAttribute("subList", subList);
        
        // Chuyển hướng sang trang giao diện
        request.getRequestDispatcher("/admin_system_config.jsp").forward(request, response);
    }

    // doPost: Khi Admin sửa thông số và bấm "Lưu thay đổi"
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        SubscriptionDAO dao = new SubscriptionDAO();

        try {
            // Lấy danh sách các ID được gửi lên từ Form
            String[] tierIds = request.getParameterValues("tierId");
            
            if (tierIds != null) {
                // Duyệt qua từng ID để cập nhật thông số tương ứng
                for (String idStr : tierIds) {
                    int tierId = Integer.parseInt(idStr);
                    int maxStorage = Integer.parseInt(request.getParameter("maxStorage_" + tierId));
                    int aiLimit = Integer.parseInt(request.getParameter("aiLimit_" + tierId));
                    double price = Double.parseDouble(request.getParameter("price_" + tierId));
                    
                    // Đóng gói vào Object và gọi DAO cập nhật
                    Subscription sub = new Subscription(tierId, "", maxStorage, aiLimit, price);
                    dao.updateSubscription(sub);
                }
            }
            
            // Cập nhật xong thì báo thành công và load lại trang
            request.setAttribute("successMessage", "Đã cập nhật cấu hình hệ thống thành công!");
            
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "Có lỗi xảy ra khi lưu cấu hình!");
        }
        
        // Lấy lại danh sách mới nhất để hiển thị lại
        List<Subscription> subList = dao.getAllSubscriptions();
        request.setAttribute("subList", subList);
        request.getRequestDispatcher("/admin_system_config.jsp").forward(request, response);
    }
}