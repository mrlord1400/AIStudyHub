/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controller;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "MainController", urlPatterns = {"/MainController"})
public class MainController extends HttpServlet {

    // Define routing destinations here for easy management
    private static final String LOGIN_PAGE = "login.jsp";
    private static final String AUTH_CONTROLLER = "AuthController";
    private static final String DOCUMENT_CONTROLLER = "DocumentController";
    private static final String USER_CONTROLLER = "UserController";
    private static final String FOLDER_CONTROLLER = "FolderController";
    private static final String TRANSACTION_CONTROLLER = "TransactionController";
    private static final String ADMIN_CONTROLLER = "AdminController";

    // --- Future Controllers (Uncomment when you build them) ---
    // private static final String DOCUMENT_CONTROLLER = "DocumentController";
    // private static final String CHAT_CONTROLLER = "ChatController";
    /**
     * Processes requests for both HTTP GET and POST methods.
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        String action = request.getParameter("action");
        String url = LOGIN_PAGE; // Default route if action is missing

        try {
            if (action != null) {
                switch (action) {
                    case "login":
                    case "register":
                    case "logout":
                    case "update":
                    case "delete":
                        url = AUTH_CONTROLLER;
                        break;
                    case "guest":
                        url = "guest_dashboard.jsp";
                        break;
                    case "editDoc":
                    case "deleteDoc":
                    case "updateDoc":
                    case "viewDoc":      
                    case "downloadDoc":
                        url = DOCUMENT_CONTROLLER;
                        break;
                    case "profile":
                    case "updateProfile":
                    case "deleteAccount":
                        url = USER_CONTROLLER;
                        break;
                    case "createFolder":
                    case "editFolder":
                    case "deleteFolder":
                    case "viewFolder":
                    case "updateFolder":
                        url = FOLDER_CONTROLLER;
                        break;
                    case "createTransaction":
                    case "listTransactions":
                    case "adminListTransactions":
                    case "adminUpdateTransaction":
                    case "buyPremium":
                        url = TRANSACTION_CONTROLLER;
                        break;
                    case "listUsers":
                    case "createUser":
                    case "updateUser":
                    case "deleteUser":
                    case "listDashboard":
                        url = ADMIN_CONTROLLER;
                        break;
                    case "wallet":
                        url = "CreditWallet.jsp";
                        break;
                    default:
                        // If an unknown action is sent, redirect to login as a failsafe
                        url = LOGIN_PAGE;
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("[MainController Error] " + e.getMessage());
        } finally {
            // Forward the request to the designated controller or JSP
            request.getRequestDispatcher(url).forward(request, response);
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

    @Override
    public String getServletInfo() {
        return "Main Router for AI Study Hub";
    }
}
