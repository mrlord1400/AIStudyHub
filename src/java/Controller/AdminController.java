package Controller;

import Model.AdminDAO;
import Model.User;
import Model.UserDAO;
import Utils.PasswordUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import java.io.IOException;
import java.util.List;

@WebServlet("/AdminController")
public class AdminController extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        AdminDAO dao = new AdminDAO();

        if ("listUsers".equals(action)) {

            List<User> users = dao.getAllUsers();

            request.setAttribute("user_list", users);

            request.getRequestDispatcher(
                    "/admin_manageUser.jsp")
                    .forward(request, response);
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        AdminDAO adminDAO = new AdminDAO();
        UserDAO userDAO = new UserDAO();

        switch (action) {

            case "createUser":

                String username
                        = request.getParameter("username");

                String email
                        = request.getParameter("email");

                String password
                        = request.getParameter("password");

                // JSP should send:
                // free / premium / admin
                String accountType
                        = request.getParameter("role");

                String role = "STUDENT";
                int tierId = 2;

                switch (accountType.toLowerCase()) {

                    case "free":
                        role = "STUDENT";
                        tierId = 2;
                        break;

                    case "premium":
                        role = "STUDENT";
                        tierId = 3;
                        break;

                    case "admin":
                        role = "ADMIN";
                        tierId = 2;
                        break;
                }

                User newUser = new User();

                newUser.setUsername(username);
                newUser.setEmail(email);

                // HASH PASSWORD
                newUser.setPasswordHash(
                        PasswordUtil.hashPassword(password));

                newUser.setRole(role);
                newUser.setTierId(tierId);
                newUser.setStatus("ACTIVE");

                adminDAO.createUser(newUser);

                break;

            case "updateUser":
                int userId = Integer.parseInt(request.getParameter("user_id"));
                String newUsername = request.getParameter("newUsername");
                String newEmail = request.getParameter("newEmail");
                String newRole = request.getParameter("newRole");
                String newStatus = request.getParameter("newStatus");
                int newBalance = Integer.parseInt(request.getParameter("newBalance"));
                int newTierId = Integer.parseInt(request.getParameter("newTierId"));

                User user = userDAO.getUserById(userId);
                if (user != null) {
                    user.setUsername(newUsername);
                    user.setEmail(newEmail);
                    user.setRole(newRole);
                    user.setStatus(newStatus);
                    user.setBalance(newBalance);
                    user.setTierId(newTierId);
                    userDAO.updateUser(user);
                }
                break;

            case "deleteUser":

                adminDAO.deleteUser(
                        Integer.parseInt(
                                request.getParameter(
                                        "user_id")));

                break;
        }

        response.sendRedirect(
                request.getContextPath()
                + "/AdminController?action=listUsers");
    }
}
