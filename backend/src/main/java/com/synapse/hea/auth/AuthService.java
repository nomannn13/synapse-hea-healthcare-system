package com.synapse.hea.auth;

import com.synapse.hea.audit.AuditService;
import com.synapse.hea.common.exception.*;
import com.synapse.hea.patient.*;
import com.synapse.hea.user.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository users;
  private final PatientProfileRepository patients;
  private final PasswordEncoder passwords;
  private final AuthenticationManager authentication;
  private final TokenService tokens;
  private final AuditService audit;

  public AuthService(
    UserRepository u,
    PatientProfileRepository p,
    PasswordEncoder pe,
    AuthenticationManager am,
    TokenService t,
    AuditService a
  ) {
    users = u;
    patients = p;
    passwords = pe;
    authentication = am;
    tokens = t;
    audit = a;
  }

  @Transactional
  public Session register(RegisterRequest q) {
    if (users.existsByEmailIgnoreCase(q.email())) throw new ConflictException(
      "Email already registered"
    );
    User user = users.save(
      new User(
        q.email(),
        passwords.encode(q.password()),
        q.firstName(),
        q.lastName(),
        q.phone(),
        Role.PATIENT
      )
    );
    patients.save(new PatientProfile(user, q.dateOfBirth()));
    audit.record(
      user.getId(),
      "REGISTER",
      "User",
      user.getId(),
      "Patient account created"
    );
    return session(user);
  }

  @Transactional(readOnly = true)
  public UserView me(UUID id) {
    return view(
      users
        .findById(id)
        .orElseThrow(() -> new NotFoundException("User not found"))
    );
  }

  public Session login(LoginRequest q) {
    authentication.authenticate(
      new UsernamePasswordAuthenticationToken(q.email(), q.password())
    );
    User user = users.findByEmailIgnoreCase(q.email()).orElseThrow();
    audit.record(
      user.getId(),
      "LOGIN",
      "User",
      user.getId(),
      "Successful login"
    );
    return session(user);
  }

  public Session refresh(String raw) {
    TokenService.Rotation r = tokens.rotate(raw);
    return new Session(
      view(r.user()),
      r.access(),
      new TokenService.RefreshIssue(r.refreshRaw(), r.refreshExpiresAt())
    );
  }

  public void logout(UUID actor, String raw) {
    tokens.revoke(raw);
    audit.record(actor, "LOGOUT", "User", actor, "Session revoked");
  }

  @Transactional
  public void changePassword(UUID id, ChangePasswordRequest q) {
    User u = users.findById(id).orElseThrow();
    if (
      !passwords.matches(q.currentPassword(), u.getPasswordHash())
    ) throw new ForbiddenOperationException("Current password is incorrect");
    u.changePassword(passwords.encode(q.newPassword()));
    audit.record(id, "UPDATE", "User", id, "Password changed");
  }

  private Session session(User u) {
    return new Session(view(u), tokens.access(u), tokens.issueRefresh(u));
  }

  private UserView view(User u) {
    return new UserView(
      u.getId(),
      u.getEmail(),
      u.getFirstName(),
      u.getLastName(),
      u.getPhone(),
      u.getRole(),
      u.getStatus()
    );
  }

  public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 10, max = 72) String password,
    @NotBlank @Size(max = 80) String firstName,
    @NotBlank @Size(max = 80) String lastName,
    @Size(max = 30) String phone,
    @Past LocalDate dateOfBirth
  ) {}

  public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
  ) {}

  public record ChangePasswordRequest(
    @NotBlank String currentPassword,
    @NotBlank @Size(min = 10, max = 72) String newPassword
  ) {}

  public record UserView(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String phone,
    Role role,
    UserStatus status
  ) {}

  public record Session(
    UserView user,
    TokenService.AccessToken access,
    TokenService.RefreshIssue refresh
  ) {}
}
