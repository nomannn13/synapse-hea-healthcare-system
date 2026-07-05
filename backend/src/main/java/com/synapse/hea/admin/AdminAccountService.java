package com.synapse.hea.admin;

import com.synapse.hea.audit.AuditService;
import com.synapse.hea.common.exception.*;
import com.synapse.hea.department.*;
import com.synapse.hea.doctor.*;
import com.synapse.hea.user.*;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccountService {

  private final UserRepository users;
  private final DoctorProfileRepository doctors;
  private final DepartmentRepository departments;
  private final PasswordEncoder passwords;
  private final AuditService audit;

  public AdminAccountService(
    UserRepository u,
    DoctorProfileRepository d,
    DepartmentRepository dep,
    PasswordEncoder p,
    AuditService a
  ) {
    users = u;
    doctors = d;
    departments = dep;
    passwords = p;
    audit = a;
  }

  @Transactional
  public DoctorView createDoctor(UUID actor, CreateDoctor q) {
    if (users.existsByEmailIgnoreCase(q.email())) throw new ConflictException(
      "Email already registered"
    );
    Department dep = departments
      .findById(q.departmentId())
      .orElseThrow(() -> new NotFoundException("Department not found"));
    User u = users.save(
      new User(
        q.email(),
        passwords.encode(q.password()),
        q.firstName(),
        q.lastName(),
        q.phone(),
        Role.DOCTOR
      )
    );
    DoctorProfile d = doctors.save(
      new DoctorProfile(u, q.licenseNumber(), q.specialization(), dep)
    );
    d.updateProfessional(
      q.specialization(),
      dep,
      q.biography(),
      q.consultationMinutes()
    );
    audit.record(
      actor,
      "CREATE",
      "DoctorProfile",
      d.getId(),
      "Doctor account created"
    );
    return new DoctorView(
      d.getId(),
      u.getId(),
      u.getEmail(),
      u.getDisplayName(),
      d.getLicenseNumber(),
      d.getSpecialization(),
      dep.getName()
    );
  }

  public record CreateDoctor(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 10, max = 72) String password,
    @NotBlank @Size(max = 80) String firstName,
    @NotBlank @Size(max = 80) String lastName,
    @Size(max = 30) String phone,
    @NotBlank @Size(max = 80) String licenseNumber,
    @NotBlank @Size(max = 120) String specialization,
    @NotNull UUID departmentId,
    @Size(max = 1500) String biography,
    @Min(10) @Max(180) int consultationMinutes
  ) {}

  public record DoctorView(
    UUID doctorId,
    UUID userId,
    String email,
    String name,
    String licenseNumber,
    String specialization,
    String department
  ) {}
}
