package Controller;

import Model.DTO.Document;
import Model.DAO.DocumentDAO;
import Model.DTO.Folder;
import Model.DAO.FolderDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "SearchController", urlPatterns = {"/SearchController"})
public class SearchController extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");

        String keyword = request.getParameter("keyword");
        keyword = (keyword == null) ? "" : keyword.trim();

        List<Document> foundDocuments = null;
        List<Folder> foundFolders = null;

        if (!keyword.isEmpty()) {
            DocumentDAO docDao = new DocumentDAO();
            FolderDAO folderDao = new FolderDAO();

            foundDocuments = docDao.searchDocumentsByUserId(userId, keyword);
            foundFolders = folderDao.searchFoldersByUserId(userId, keyword);
        }

        request.setAttribute("keyword", keyword);
        request.setAttribute("foundDocuments", foundDocuments);
        request.setAttribute("foundFolders", foundFolders);

        request.getRequestDispatcher("/search_results.jsp").forward(request, response);
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