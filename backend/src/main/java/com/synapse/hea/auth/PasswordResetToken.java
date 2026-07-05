package com.synapse.hea.auth;

import com.synapse.hea.common.model.BaseEntity;
import com.synapse.hea.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
  name = "password_reset_tokens",
  indexes = @Index(
    name = "idx_password_reset_hash",
    columnList = "token_hash",
    unique = true
  )
)
public class PasswordResetToken extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "consumed_at")
  private Instant consumedAt;

  protected PasswordResetToken() {}

  public PasswordResetToken(User u, String h, Instant e) {
    user = u;
    tokenHash = h;
    expiresAt = e;
  }

  public User getUser() {
    return user;
  }

  public boolean active() {
    return consumedAt == null && expiresAt.isAfter(Instant.now());
  }

  public void consume() {
    consumedAt = Instant.now();
  }
}
