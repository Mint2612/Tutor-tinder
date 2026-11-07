package com.tutormatch.integration.delivery;

import com.tutormatch.exception.BusinessException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** SMTP email delivery. Local Compose uses Mailpit; production uses the configured SMTP server. */
@Component
public class DeliveryGateway {
  private final JavaMailSender mail;
  private final String from, publicUrl;

  public DeliveryGateway(
      JavaMailSender mail,
      @Value("${MAIL_FROM:noreply@tutormatch.local}") String from,
      @Value("${APP_PUBLIC_URL:http://localhost:3000}") String publicUrl) {
    this.mail = mail;
    this.from = from;
    this.publicUrl = publicUrl.replaceAll("/+$", "");
  }

  public void send(String purpose, String destination, String code, String challengeId) {
    boolean reset = purpose.equals("RESET");
    String action = reset ? "Đặt lại mật khẩu" : "Xác minh email";
    String link =
        publicUrl
            + "/#/"
            + (reset ? "reset" : "verify")
            + "?challengeId="
            + encode(challengeId)
            + "&code="
            + encode(code);
    var message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(destination);
    message.setSubject("TutorMatch — " + action);
    message.setText(
        action
            + " cho tài khoản TutorMatch của bạn:\n\n"
            + link
            + "\n\nMã định danh yêu cầu: "
            + challengeId
            + "\nMã xác thực: "
            + code
            + "\n\n"
            + "Liên kết và mã có hiệu lực 10 phút. Nếu không yêu cầu thao tác này, bạn có thể bỏ"
            + " qua email.");
    try {
      mail.send(message);
    } catch (org.springframework.mail.MailException e) {
      throw new BusinessException(503, "Không thể gửi email. Vui lòng thử lại sau.");
    }
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
