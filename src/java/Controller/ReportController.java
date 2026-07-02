package Controller;

import Model.DAO.ReportDAO;
import Model.DAO.ReportReasonDAO;
import Model.DAO.DocumentDAO;
import Model.DTO.Report;
import Model.DTO.ReportReason;
import Model.DTO.Document;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "ReportController", urlPatterns = {"/ReportController"})
public class ReportController extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // 1. Kiểm tra trạng thái đăng nhập của tài khoản (Security Guard)
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        int userId = (int) session.getAttribute("userId");
        String action = request.getParameter("action");

        // Cơ chế Failsafe: Mặc định chuyển hướng nếu không chỉ định rõ action
        if (action == null || action.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/FileExplore.jsp");
            return;
        }

        ReportDAO reportDao = new ReportDAO();
        ReportReasonDAO reasonDao = new ReportReasonDAO();
        DocumentDAO docDao = new DocumentDAO();

        try {
            if ("report".equals(action)) {
                // Đọc documentId từ request tham số gửi lên
                int documentId = Integer.parseInt(request.getParameter("documentId"));

                // Kiểm tra tính hợp lệ (Chưa có báo cáo nào ở trạng thái PENDING)
                boolean isValid = reportDao.checkValid(userId, documentId);

                if (isValid) {
                    // Chuyển hướng nội bộ sang trang tạo mới kèm thông tin
                    request.setAttribute("userId", userId);
                    request.setAttribute("documentId", documentId);
                    request.getRequestDispatcher("/report_create.jsp").forward(request, response);
                } else {
                    // Lấy báo cáo PENDING hiện tại đang có sẵn
                    Report existingReport = reportDao.getReportByUserAndDoc(userId, documentId);

                    // Thiết lập thuộc tính lên Request Scope và forward qua màn hình chỉnh sửa
                    request.setAttribute("report", existingReport);
                    request.getRequestDispatcher("/report_edit.jsp").forward(request, response);
                }

            } else if ("createReport".equals(action)) {
                // 1. Thu thập dữ liệu từ biểu mẫu gửi lên
                int documentId = Integer.parseInt(request.getParameter("documentId"));
                String reasonCode = request.getParameter("reasonCode");
                String details = request.getParameter("details");

                // Tạo đối tượng Report mới (mặc định ban đầu là PENDING)
                Report newReport = new Report();
                newReport.setDocumentId(documentId);
                newReport.setReporterId(userId);
                newReport.setReasonCode(reasonCode);
                newReport.setDetails(details);
                newReport.setStatus("PENDING");

                // 2. Lưu trữ bản ghi báo cáo xuống Database
                boolean isCreated = reportDao.createReport(newReport);

                if (isCreated) {
                    // 3. Lấy cấu hình cấu trúc điểm vi phạm dựa theo reasonCode
                    ReportReason reasonConfig = reasonDao.getReasonByCode(reasonCode);
                    double reasonBaseScore = reasonConfig.getBaseScore();
                    double autoFlagThreshold = reasonConfig.getAutoFlagThreshold();

                    // 4. Tìm kiếm thực thể tài liệu (Document) liên quan để tính điểm tích lũy
                    Document doc = docDao.findById(documentId);

                    if (doc != null) {
                        double currentTotalScore = doc.getTotalReportScore();
                        double updatedScore = currentTotalScore + reasonBaseScore;
                        doc.setTotalReportScore(updatedScore);
                        
                        // 🔥 ĐÃ FIX LỖI Ở ĐÂY: Dùng updatedScore (điểm sau khi cộng) và thêm dấu =
                        // 5. Tính toán & cập nhật trạng thái cờ cảnh báo (Flag Check)
                        if (updatedScore >= autoFlagThreshold) {
                            doc.setIsFlagged(true);
                        } else {
                            doc.setIsFlagged(false);
                        }
                        
                        boolean isDocUpdated = docDao.updateReportMetrics(doc.getDocumentId(), doc.getTotalReportScore(), doc.isFlagged());
                        System.out.println("[ReportController] Cập nhật điểm tài liệu thành công? " + isDocUpdated
                                + " | Điểm mới: " + doc.getTotalReportScore() + " | Bị ẩn/cắm cờ: " + doc.isFlagged());
                    }

                    // 6. Đóng gói Report hiện tại vào Request Scope và forward/redirect về màn hình quản lý file
                    request.setAttribute("report", newReport);
                    request.getRequestDispatcher("/FileExplore.jsp").forward(request, response);
                } else {
                    response.sendRedirect(request.getContextPath() + "/FileExplore.jsp?error=create_report_failed");
                }
            } else if ("updateReport".equals(action)) {
                // 🔥 CASE HANDING: CẬP NHẬT BÁO CÁO ĐANG PENDING
                int reportId = Integer.parseInt(request.getParameter("reportId"));
                int documentId = Integer.parseInt(request.getParameter("documentId"));
                String newReasonCode = request.getParameter("reasonCode");
                String newDetails = request.getParameter("details");

                // 1. Lấy thông tin báo cáo cũ từ DB trước khi cập nhật để tính toán lại điểm
                Report oldReport = reportDao.getReportByUserAndDoc(userId, documentId);

                if (oldReport != null && oldReport.getReportId() == reportId) {
                    String oldReasonCode = oldReport.getReasonCode();

                    // Chuẩn bị đối tượng cập nhật
                    oldReport.setReasonCode(newReasonCode);
                    oldReport.setDetails(newDetails);

                    // 2. Thực thi cập nhật bảng document_reports
                    boolean isUpdated = reportDao.updateReport(oldReport);

                    if (isUpdated) {
                        // 3. Tính toán lại Metrics điểm phạt của Document
                        Document doc = docDao.findById(documentId);
                        if (doc != null) {
                            // Lấy điểm cấu hình cũ và mới
                            ReportReason oldReasonConfig = reasonDao.getReasonByCode(oldReasonCode);
                            ReportReason newReasonConfig = reasonDao.getReasonByCode(newReasonCode);

                            double oldScoreWeight = (oldReasonConfig != null) ? oldReasonConfig.getBaseScore() : 0.0;
                            double newScoreWeight = (newReasonConfig != null) ? newReasonConfig.getBaseScore() : 0.0;
                            double autoFlagThreshold = (newReasonConfig != null) ? newReasonConfig.getAutoFlagThreshold() : 0.0;

                            // Công thức hoàn trả điểm cũ và áp cấu trúc điểm phạt mới
                            double recalculatedScore = doc.getTotalReportScore() - oldScoreWeight + newScoreWeight;
                            if (recalculatedScore < 0) {
                                recalculatedScore = 0.0; // Failsafe hạ sàn điểm
                            }
                            doc.setTotalReportScore(recalculatedScore);

                            // Kiểm tra lại ngưỡng cắm cờ sau khi hoàn điểm
                            if (recalculatedScore >= autoFlagThreshold) {
                                doc.setIsFlagged(true);
                            } else {
                                doc.setIsFlagged(false);
                            }

                            // 4. Lưu trực tiếp điểm số đã tính toán lại xuống DB
                            docDao.updateReportMetrics(doc.getDocumentId(), doc.getTotalReportScore(), doc.isFlagged());
                        }

                        // Đóng gói trả về view thành công
                        request.setAttribute("report", oldReport);
                        request.getRequestDispatcher("/FileExplore.jsp?success=report_updated").forward(request, response);
                        return;
                    }
                }
                response.sendRedirect(request.getContextPath() + "/FileExplore.jsp?error=update_report_failed");
            }

        } catch (Exception e) {
            System.err.println("[ReportController Error] " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/FileExplore.jsp?error=system_error");
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