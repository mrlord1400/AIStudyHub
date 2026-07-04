package Utils;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailUtil {
    // Lưu ý: Gmail yêu cầu App Password
    private static final String SENDER_EMAIL = "aistudyhub.noreply@gmail.com"; 
    private static final String SENDER_PASSWORD = "hfumixnlwrjntlld"; 

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
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Ma xac thuc OTP - AI Study Hub");
            message.setContent("Ma OTP cua ban la: <strong>" + otp + "</strong>", "text/html; charset=UTF-8");
            
            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace(); // Xem kỹ lỗi ở cửa sổ Output/Console
            return false;
        }
    }
}