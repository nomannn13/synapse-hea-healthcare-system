package com.synapse.hea.auth;

import com.synapse.hea.common.security.CurrentUser;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private static final String COOKIE = "hea_refresh";
  private final AuthService service;
  private final PasswordResetService reset;
  private final CurrentUser current;
  private final boolean secure;

  public AuthController(
    AuthService s,
    PasswordResetService reset,
    CurrentUser c,
    @Value("${app.security.secure-cookies}") boolean secure
  ) {
    service = s;
    this.reset = reset;
    current = c;
    this.secure = secure;
  }

  @PostMapping("/register")
  public ResponseEntity<SessionResponse> register(
    @Valid @RequestBody AuthService.RegisterRequest q
  ) {
    return response(service.register(q));
  }

  @PostMapping("/login")
  public ResponseEntity<SessionResponse> login(
    @Valid @RequestBody AuthService.LoginRequest q
  ) {
    return response(service.login(q));
  }

  @PostMapping("/refresh")
  public ResponseEntity<SessionResponse> refresh(
    @CookieValue(name = COOKIE, required = false) String refresh
  ) {
    if (
      refresh == null
    ) throw new com.synapse.hea.common.exception.ForbiddenOperationException(
      "Refresh cookie missing"
    );
    return response(service.refresh(refresh));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
    Authentication a,
    @CookieValue(name = COOKIE, required = false) String refresh
  ) {
    service.logout(current.id(a), refresh);
    return ResponseEntity.noContent()
      .header(HttpHeaders.SET_COOKIE, expiredCookie().toString())
      .build();
  }

  @GetMapping("/me")
  public AuthService.UserView me(Authentication a) {
    return service.me(current.id(a));
  }

  @PostMapping("/change-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void change(
    Authentication a,
    @Valid @RequestBody AuthService.ChangePasswordRequest q
  ) {
    service.changePassword(current.id(a), q);
  }

  @PostMapping("/password-reset/request")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void requestReset(@Valid @RequestBody ResetRequest q) {
    reset.request(q.email());
  }

  @PostMapping("/password-reset/confirm")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void confirmReset(@Valid @RequestBody ResetConfirm q) {
    reset.confirm(q.token(), q.newPassword());
  }

  private ResponseEntity<SessionResponse> response(AuthService.Session s) {
    ResponseCookie cookie = ResponseCookie.from(COOKIE, s.refresh().raw())
      .httpOnly(true)
      .secure(secure)
      .sameSite("Strict")
      .path("/api/v1/auth")
      .maxAge(
        Duration.between(java.time.Instant.now(), s.refresh().expiresAt())
      )
      .build();
    return ResponseEntity.ok()
      .header(HttpHeaders.SET_COOKIE, cookie.toString())
      .body(
        new SessionResponse(
          s.user(),
          s.access().value(),
          s.access().expiresAt()
        )
      );
  }

  private ResponseCookie expiredCookie() {
    return ResponseCookie.from(COOKIE, "")
      .httpOnly(true)
      .secure(secure)
      .sameSite("Strict")
      .path("/api/v1/auth")
      .maxAge(Duration.ZERO)
      .build();
  }

  public record SessionResponse(
    AuthService.UserView user,
    String accessToken,
    java.time.Instant accessTokenExpiresAt
  ) {}

  public record ResetRequest(
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Email
    String email
  ) {}

  public record ResetConfirm(
    @jakarta.validation.constraints.NotBlank String token,
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(min = 10, max = 72)
    String newPassword
  ) {}
}
