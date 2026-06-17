package Controller;

import Model.Transaction;
import Model.TransactionDAO;
import Model.User;
import Model.UserDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "TransactionController", urlPatterns = {"/TransactionController"})
public class TransactionController extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");

        try {
            switch (action) {

                case "createTransaction":
                    handleCreateTransaction(request, response);
                    break;

                case "listTransactions":
                    handleListTransactions(request, response);
                    break;

                case "adminListTransactions":
                    handleAdminListTransactions(request, response);
                    break;

                case "adminUpdateTransaction":
                    handleUpdateTransactionStatus(request, response);
                    break;
                case "buyPremium":
                    handleBuyPremium(request, response);
                    break;
                default:
                    response.sendRedirect(request.getContextPath() + "/login.jsp");
                    break;
            }
        } catch (Exception e) {
            System.err.println("[TransactionController Error] " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=system_error");
        }
    }

    /**
     * User tạo giao dịch mới. Nhận từ JSP: amount, type (DEPOSIT / WITHDRAW)
     * Status mặc định = PENDING
     */
    private void handleCreateTransaction(HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        int userId = (int) session.getAttribute("userId");

        String amountStr = request.getParameter("amount");
        String type = request.getParameter("type");

        // Validate input
        if (amountStr == null || type == null || amountStr.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath()
                    + "/CreditWallet.jsp?error=invalid_input");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                response.sendRedirect(request.getContextPath()
                        + "/CreditWallet.jsp?error=invalid_amount");
                return;
            }
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath()
                    + "/CreditWallet.jsp?error=invalid_amount");
            return;
        }

        Transaction t = new Transaction();
        t.setUserId(userId);
        t.setAmount(amount);
        t.setType(type);

        TransactionDAO dao = new TransactionDAO();
        boolean success = dao.createTransaction(t);

        if (success) {
            response.sendRedirect(request.getContextPath()
                    + "/MainController?action=listTransactions&transactionSuccess=1");
        } else {
            response.sendRedirect(request.getContextPath()
                    + "/CreditWallet.jsp?error=create_failed");
        }
    }

    /**
     * User xem danh sách giao dịch của mình.
     */
    private void handleListTransactions(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        int userId = (int) session.getAttribute("userId");

        TransactionDAO dao = new TransactionDAO();
        List<Transaction> transactions = dao.getTransactionsByUserId(userId);

        request.setAttribute("transactions", transactions);
        request.getRequestDispatcher("/CreditWallet.jsp").forward(request, response);
    }

    /**
     * Admin xem tất cả giao dịch.
     */
    private void handleAdminListTransactions(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || !"ADMIN".equals(session.getAttribute("role"))) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        TransactionDAO dao = new TransactionDAO();
        List<Transaction> transactions = dao.getAllTransactions();

        request.setAttribute("transaction_list", transactions);
        request.getRequestDispatcher("/admin_manageTrans.jsp").forward(request, response);
    }

    /**
     * Admin cập nhật trạng thái giao dịch. Nếu approve (SUCCESS): - DEPOSIT →
     * cộng balance cho user - WITHDRAW → trừ balance cho user Nếu cancel
     * (CANCELLED): - Không thay đổi balance
     */
    private void handleUpdateTransactionStatus(HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);
        if (session == null || !"ADMIN".equals(session.getAttribute("role"))) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        String transactionIdStr = request.getParameter("transactionId");
        String newStatus = request.getParameter("newStatus");

        if (transactionIdStr == null || newStatus == null) {
            response.sendRedirect(request.getContextPath()
                    + "/MainController?action=adminListTransactions&error=invalid_input");
            return;
        }

        int transactionId = Integer.parseInt(transactionIdStr);

        TransactionDAO transactionDAO = new TransactionDAO();
        UserDAO userDAO = new UserDAO();

        // Lấy thông tin giao dịch hiện tại
        Transaction t = transactionDAO.getTransactionById(transactionId);

        if (t == null) {
            response.sendRedirect(request.getContextPath()
                    + "/MainController?action=adminListTransactions&error=not_found");
            return;
        }

        // Chỉ cho phép thay đổi khi giao dịch chưa hoàn thành
        if ("SUCCESS".equals(t.getStatus()) || "CANCELLED".equals(t.getStatus())) {
            response.sendRedirect(request.getContextPath()
                    + "/MainController?action=adminListTransactions&error=already_completed");
            return;
        }

        // Cập nhật trạng thái
        boolean statusUpdated = transactionDAO.updateTransactionStatus(transactionId, newStatus);

        if (statusUpdated && "SUCCESS".equals(newStatus)) {
            // Cập nhật balance cho user
            if ("DEPOSIT".equals(t.getType())) {
                userDAO.updateBalance(t.getUserId(), (int) t.getAmount());
            } else if ("WITHDRAW".equals(t.getType())) {
                userDAO.updateBalance(t.getUserId(), (int) -t.getAmount());
            }
        }

        response.sendRedirect(request.getContextPath()
                + "/MainController?action=adminListTransactions&updateSuccess=1");
    }

    private void handleBuyPremium(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        int userId = (int) session.getAttribute("userId");
        int premiumCost = 99000;

        UserDAO userDAO = new UserDAO();
        TransactionDAO transactionDAO = new TransactionDAO();

        // 1. Fetch current user to verify they have enough balance
        User currentUser = userDAO.getUserById(userId);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        if (currentUser.getBalance() < premiumCost) {
            // Redirect with an error if they don't have enough credits
            response.sendRedirect(request.getContextPath() + "/CreditWallet.jsp?error=insufficient_balance");
            return;
        }

        // 2. Create the Transaction record
        Transaction t = new Transaction();
        t.setUserId(userId);
        t.setAmount(-99000);     // Set to exactly -99000 as requested
        t.setType("WITHDRAW");   // Set type to withdraw
        t.setStatus("SUCCESS");  // Automatically set to SUCCESS since it's an instant purchase

        boolean txSuccess = transactionDAO.createTransaction(t);

        if (txSuccess) {
            // 3. Atomically deduct the balance using the safe updateBalance method
            boolean balanceUpdated = userDAO.updateBalance(userId, -99000);

            if (balanceUpdated) {
                // 4. Update the user's tier
                // IMPORTANT: We re-fetch the user here to get the newly updated balance from the DB. 
                // If we used `currentUser`, the `updateUser()` method would overwrite the database 
                // with the old balance still stored in Java memory!
                User updatedUser = userDAO.getUserById(userId);
                updatedUser.setTierId(3);
                userDAO.updateUser(updatedUser);

                // Update the session attribute so the UI reflects the new tier immediately
                session.setAttribute("tierId", 3);

                // Redirect to a success page or dashboard
                response.sendRedirect(request.getContextPath() + "/MainController?action=listTransactions&premiumSuccess=1");
            } else {
                response.sendRedirect(request.getContextPath() + "/CreditWallet.jsp?error=balance_update_failed");
            }
        } else {
            response.sendRedirect(request.getContextPath() + "/CreditWallet.jsp?error=transaction_failed");
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
