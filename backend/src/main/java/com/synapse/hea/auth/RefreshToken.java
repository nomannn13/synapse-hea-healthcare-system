package com.synapse.hea.auth;

import com.synapse.hea.common.model.BaseEntity;
import com.synapse.hea.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
  name = "refresh_tokens",
  indexes = {
    @Index(name = "idx_refresh_hash", columnList = "token_hash", unique = true),
    @Index(name = "idx_refresh_user", columnList = "user_id"),
  }
)
public class RefreshToken extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "replaced_by_hash", length = 64)
  private String replacedByHash;

  protected RefreshToken() {}

  public RefreshToken(User user, String hash, Instant expires) {
    this.user = user;
    tokenHash = hash;
    expiresAt = expires;
  }

  public User getUser() {
    return user;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public boolean active() {
    return revokedAt == null && expiresAt.isAfter(Instant.now());
  }

  public void revoke(String replacement) {
    revokedAt = Instant.now();
    replacedByHash = replacement;
  }
}
