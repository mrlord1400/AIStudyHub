package Utils;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailUtil {

    private static final String SENDER_EMAIL = "aistudyhub.noreply@gmail.com";
    private static final String SENDER_PASSWORD = "hfumixnlwrjntlld";
    private static final String SENDER_DISPLAY_NAME = "AI Study Hub";

    public static boolean sendOTP(String toEmail, String otp) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, SENDER_DISPLAY_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            
            // Set tiêu đề Email chuẩn UTF-8
            message.setSubject("Mã xác thực đổi mật khẩu - AI Study Hub", "UTF-8");

            // Truyền thẳng nội dung HTML (Rất nhẹ và an toàn cho SMTP)
            message.setContent(buildOtpEmailHtml(otp), "text/html; charset=UTF-8");

            Transport.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Template email OTP dạng HTML
     */
    private static String buildOtpEmailHtml(String otp) {
        // Thay vì dùng <svg> bị Gmail chặn, ta dùng link ảnh PNG Mũ Cử Nhân Trắng tĩnh
        String logoUrl = "https://i.postimg.cc/nhWz4dLJ/web-app-manifest-512x512.png";

        return "<!DOCTYPE html>"
            + "<html>"
            + "<head><meta charset=\"UTF-8\"></head>"
            + "<body style=\"margin:0; padding:0; background-color:#f4f4f7; font-family:'Segoe UI', Arial, Helvetica, sans-serif;\">"
            + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#f4f4f7; padding:32px 16px;\">"
            + "<tr><td align=\"center\">"
            + "<table role=\"presentation\" width=\"480\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:480px; width:100%; background-color:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 4px 18px rgba(92,60,245,0.12);\">"

            // Phần Header màu Gradient Tím + Logo
            + "<tr><td style=\"background:linear-gradient(135deg, #5c3cf5 0%, #7c3aed 100%); padding:36px 32px; text-align:center;\">"
            + "<div style=\"display:inline-block; width:56px; height:56px; background-color:rgba(255,255,255,0.18); border-radius:16px; text-align:center; line-height:62px;\">"
            + "<img src=\"" + logoUrl + "\" width=\"34\" height=\"34\" style=\"vertical-align:middle; border:none;\" alt=\"AI Study Hub Logo\" />"
            + "</div>"
            + "<div style=\"color:#ffffff; font-size:22px; font-weight:700; letter-spacing:0.3px; margin-top:12px;\">AI Study Hub</div>"
            + "</td></tr>"

            // Nội dung chính
            + "<tr><td style=\"padding:36px 32px 8px 32px;\">"
            + "<h1 style=\"margin:0 0 8px 0; font-size:20px; color:#111827; font-weight:700;\">Mã xác thực của bạn</h1>"
            + "<p style=\"margin:0 0 24px 0; font-size:14px; line-height:22px; color:#6b7280;\">"
            + "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản AI Study Hub của bạn. "
            + "Vui lòng nhập mã bên dưới để tiếp tục:"
            + "</p>"
            + "</td></tr>"

            // Khối OTP
            + "<tr><td style=\"padding:0 32px;\">"
            + "<div style=\"background-color:#f5f3ff; border:1px solid #ddd6fe; border-radius:12px; padding:20px; text-align:center;\">"
            + "<span style=\"font-size:34px; font-weight:700; letter-spacing:10px; color:#5c3cf5;\">" + otp + "</span>"
            + "</div>"
            + "</td></tr>"

            // Lưu ý thời gian
            + "<tr><td style=\"padding:16px 32px 0 32px;\">"
            + "<p style=\"margin:0; font-size:13px; color:#9ca3af; text-align:center;\">Mã có hiệu lực trong <strong style=\"color:#6b7280;\">2 phút</strong> kể từ thời điểm gửi.</p>"
            + "</td></tr>"

            // Dòng gạch ngang
            + "<tr><td style=\"padding:28px 32px 0 32px;\">"
            + "<hr style=\"border:none; border-top:1px solid #eef0f4; margin:0;\">"
            + "</td></tr>"

            // Lưu ý bảo mật
            + "<tr><td style=\"padding:20px 32px 32px 32px;\">"
            + "<p style=\"margin:0; font-size:12.5px; line-height:20px; color:#9ca3af;\">"
            + "Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này. "
            + "Tài khoản của bạn vẫn an toàn và không cần thực hiện thêm thao tác nào. "
            + "Không chia sẻ mã này cho bất kỳ ai, kể cả nhân viên AI Study Hub."
            + "</p>"
            + "</td></tr>"

            // Footer
            + "<tr><td style=\"background-color:#fafafa; padding:20px 32px; text-align:center;\">"
            + "<p style=\"margin:0; font-size:12px; color:#b0b3ba;\">Dự án SWP391 &middot; Đại học FPT</p>"
            + "</td></tr>"

            + "</table>"
            + "</td></tr>"
            + "</table>"
            + "</body>"
            + "</html>";
    }
}