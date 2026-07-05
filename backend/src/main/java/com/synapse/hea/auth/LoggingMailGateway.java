package com.synapse.hea.auth;

import org.slf4j.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingMailGateway implements MailGateway {
  private static final Logger log = LoggerFactory.getLogger(LoggingMailGateway.class);

  public void sendPasswordReset(String email, String name, String token) {
    log.info("DEV PASSWORD RESET for {} <{}>: token={}", name, email, token);
  }

  public void sendNotification(String email, String name, String title, String message) {
    log.info("DEV EMAIL to {} <{}>: {} — {}", name, email, title, message);
  }
}
