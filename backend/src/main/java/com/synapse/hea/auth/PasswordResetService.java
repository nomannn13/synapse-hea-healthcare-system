package com.synapse.hea.auth;

import com.synapse.hea.audit.AuditService;
import com.synapse.hea.common.exception.ForbiddenOperationException;
import com.synapse.hea.user.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

  private final UserRepository users;
  private final PasswordResetTokenRepository tokens;
  private final PasswordEncoder passwords;
  private final MailGateway mail;
  private final AuditService audit;
  private final SecureRandom random = new SecureRandom();

  public PasswordResetService(
    UserRepository u,
    PasswordResetTokenRepository t,
    PasswordEncoder p,
    MailGateway m,
    AuditService a
  ) {
    users = u;
    tokens = t;
    passwords = p;
    mail = m;
    audit = a;
  }

  @Transactional
  public void request(String email) {
    users.findByEmailIgnoreCase(email).ifPresent(u -> {
      String raw = random();
      tokens.save(
        new PasswordResetToken(
          u,
          hash(raw),
          Instant.now().plus(Duration.ofMinutes(20))
        )
      );
      mail.sendPasswordReset(u.getEmail(), u.getDisplayName(), raw);
      audit.record(
        u.getId(),
        "REQUEST",
        "PasswordReset",
        null,
        "Password reset requested"
      );
    });
  }

  @Transactional
  public void confirm(String raw, String newPassword) {
    PasswordResetToken t = tokens
      .findByTokenHash(hash(raw))
      .orElseThrow(() ->
        new ForbiddenOperationException("Reset token is invalid")
      );
    if (!t.active()) throw new ForbiddenOperationException(
      "Reset token is expired or already used"
    );
    t.getUser().changePassword(passwords.encode(newPassword));
    t.consume();
    audit.record(
      t.getUser().getId(),
      "UPDATE",
      "User",
      t.getUser().getId(),
      "Password reset completed"
    );
  }

  private String random() {
    byte[] b = new byte[32];
    random.nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  private String hash(String s) {
    try {
      return HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(
          s.getBytes(StandardCharsets.UTF_8)
        )
      );
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
