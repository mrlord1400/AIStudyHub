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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        SubscriptionDAO dao = new SubscriptionDAO();
        List<Subscription> subList = dao.getAllSubscriptions();
        
        request.setAttribute("subList", subList);
        request.getRequestDispatcher("/admin_system_config.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        SubscriptionDAO dao = new SubscriptionDAO();

        try {
            String[] tierIds = request.getParameterValues("tierId");
            
            if (tierIds != null) {
                for (String idStr : tierIds) {
                    int tierId = Integer.parseInt(idStr);
                    int maxStorage = Integer.parseInt(request.getParameter("maxStorage_" + tierId));
                    int aiLimit = Integer.parseInt(request.getParameter("aiLimit_" + tierId));
                    double price = Double.parseDouble(request.getParameter("price_" + tierId));
                    // THÊM DÒNG NÀY: Nhận giá trị dung lượng kho tổng từ Form
                    int totalStorage = Integer.parseInt(request.getParameter("totalStorage_" + tierId));
                    
                    // CẬP NHẬT CONSTRUCTOR: Truyền thêm totalStorage vào cuối
                    Subscription sub = new Subscription(tierId, "", maxStorage, aiLimit, price, totalStorage);
                    dao.updateSubscription(sub);
                }
            }
            
            request.setAttribute("successMessage", "Đã cập nhật cấu hình hệ thống thành công!");
            
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "Có lỗi xảy ra khi lưu cấu hình!");
        }
        
        List<Subscription> subList = dao.getAllSubscriptions();
        request.setAttribute("subList", subList);
        request.getRequestDispatcher("/admin_system_config.jsp").forward(request, response);
    }
}