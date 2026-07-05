package com.synapse.hea.auth;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class SmtpMailGateway implements MailGateway {
  private static final Logger log = LoggerFactory.getLogger(SmtpMailGateway.class);
  private final JavaMailSender sender;
  private final String from;
  private final String frontendUrl;

  public SmtpMailGateway(JavaMailSender sender,
      @Value("${app.mail.from:no-reply@synapse.local}") String from,
      @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl) {
    this.sender = sender;
    this.from = from;
    this.frontendUrl = frontendUrl;
  }

  @Override
  public void sendPasswordReset(String email, String name, String token) {
    send(email, "Synapse HEA password reset",
      "Hello " + name + ",\n\nUse this reset token: " + token +
      "\n\nOpen " + frontendUrl + "/login to continue.\n\nIf you did not request this, ignore this email.");
  }

  @Override
  public void sendNotification(String email, String name, String title, String message) {
    send(email, "Synapse HEA · " + title,
      "Hello " + name + ",\n\n" + message + "\n\nOpen " + frontendUrl + " to view details.");
  }

  private void send(String to, String subject, String body) {
    try {
      SimpleMailMessage mail = new SimpleMailMessage();
      mail.setFrom(from);
      mail.setTo(to);
      mail.setSubject(subject);
      mail.setText(body);
      sender.send(mail);
    } catch (MailException ex) {
      log.error("Email delivery failed for {}: {}", to, ex.getMessage());
    }
  }
}
