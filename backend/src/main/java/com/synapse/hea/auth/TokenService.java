package com.synapse.hea.auth;

import com.synapse.hea.common.exception.ForbiddenOperationException;
import com.synapse.hea.user.User;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {

  private final JwtEncoder encoder;
  private final RefreshTokenRepository refreshTokens;
  private final long accessMinutes;
  private final long refreshDays;
  private final SecureRandom random = new SecureRandom();

  public TokenService(
    JwtEncoder e,
    RefreshTokenRepository r,
    @Value("${app.security.access-token-minutes}") long am,
    @Value("${app.security.refresh-token-days}") long rd
  ) {
    encoder = e;
    refreshTokens = r;
    accessMinutes = am;
    refreshDays = rd;
  }

  public AccessToken access(User user) {
    Instant now = Instant.now();
    JwtClaimsSet claims = JwtClaimsSet.builder()
      .issuer("synapse-hea")
      .issuedAt(now)
      .expiresAt(now.plus(Duration.ofMinutes(accessMinutes)))
      .subject(user.getId().toString())
      .claim("email", user.getEmail())
      .claim("name", user.getDisplayName())
      .claim("roles", List.of(user.getRole().name()))
      .build();
    String value = encoder
      .encode(
        JwtEncoderParameters.from(
          JwsHeader.with(
            org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256
          ).build(),
          claims
        )
      )
      .getTokenValue();
    return new AccessToken(value, now.plus(Duration.ofMinutes(accessMinutes)));
  }

  @Transactional
  public RefreshIssue issueRefresh(User user) {
    String raw = randomToken();
    String hash = hash(raw);
    refreshTokens.save(
      new RefreshToken(
        user,
        hash,
        Instant.now().plus(Duration.ofDays(refreshDays))
      )
    );
    return new RefreshIssue(
      raw,
      Instant.now().plus(Duration.ofDays(refreshDays))
    );
  }

  @Transactional
  public Rotation rotate(String raw) {
    String oldHash = hash(raw);
    RefreshToken old = refreshTokens
      .findByTokenHash(oldHash)
      .orElseThrow(() ->
        new ForbiddenOperationException("Invalid refresh token")
      );
    if (!old.active()) throw new ForbiddenOperationException(
      "Refresh token expired or revoked"
    );
    String replacement = randomToken();
    String replacementHash = hash(replacement);
    old.revoke(replacementHash);
    refreshTokens.save(
      new RefreshToken(
        old.getUser(),
        replacementHash,
        Instant.now().plus(Duration.ofDays(refreshDays))
      )
    );
    return new Rotation(
      old.getUser(),
      access(old.getUser()),
      replacement,
      Instant.now().plus(Duration.ofDays(refreshDays))
    );
  }

  @Transactional
  public void revoke(String raw) {
    if (raw == null || raw.isBlank()) return;
    refreshTokens
      .findByTokenHash(hash(raw))
      .filter(RefreshToken::active)
      .ifPresent(t -> t.revoke(null));
  }

  private String randomToken() {
    byte[] bytes = new byte[48];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hash(String raw) {
    try {
      byte[] out = MessageDigest.getInstance("SHA-256").digest(
        raw.getBytes(StandardCharsets.UTF_8)
      );
      return HexFormat.of().formatHex(out);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public record AccessToken(String value, Instant expiresAt) {}

  public record RefreshIssue(String raw, Instant expiresAt) {}

  public record Rotation(
    User user,
    AccessToken access,
    String refreshRaw,
    Instant refreshExpiresAt
  ) {}
}
