package com.synapse.hea.user;

import com.synapse.hea.audit.AuditService;
import com.synapse.hea.common.exception.*;
import com.synapse.hea.department.*;
import com.synapse.hea.doctor.*;
import com.synapse.hea.patient.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

  private final UserRepository users;
  private final PatientProfileRepository patients;
  private final DoctorProfileRepository doctors;
  private final DepartmentRepository departments;
  private final AuditService audit;

  public ProfileService(
    UserRepository u,
    PatientProfileRepository p,
    DoctorProfileRepository d,
    DepartmentRepository dep,
    AuditService a
  ) {
    users = u;
    patients = p;
    doctors = d;
    departments = dep;
    audit = a;
  }

  @Transactional(readOnly = true)
  public ProfileView get(UUID id) {
    User u = users
      .findById(id)
      .orElseThrow(() -> new NotFoundException("User not found"));
    PatientData patient = null;
    DoctorData doctor = null;
    if (u.getRole() == Role.PATIENT) {
      PatientProfile p = patients.findByUserId(id).orElseThrow();
      patient = new PatientData(
        p.getDateOfBirth(),
        p.getBloodGroup(),
        p.getAllergies(),
        p.getEmergencyContact(),
        p.getAddress()
      );
    }
    if (u.getRole() == Role.DOCTOR) {
      DoctorProfile d = doctors.findByUserId(id).orElseThrow();
      doctor = new DoctorData(
        d.getLicenseNumber(),
        d.getSpecialization(),
        d.getDepartment().getId(),
        d.getDepartment().getName(),
        d.getBiography(),
        d.getConsultationMinutes()
      );
    }
    return new ProfileView(
      u.getId(),
      u.getEmail(),
      u.getFirstName(),
      u.getLastName(),
      u.getPhone(),
      u.getRole(),
      patient,
      doctor
    );
  }

  @Transactional
  public ProfileView update(UUID id, UpdateRequest q) {
    User u = users
      .findById(id)
      .orElseThrow(() -> new NotFoundException("User not found"));
    u.updateProfile(q.firstName(), q.lastName(), q.phone());
    if (u.getRole() == Role.PATIENT) {
      PatientProfile p = patients.findByUserId(id).orElseThrow();
      p.update(
        q.dateOfBirth(),
        q.bloodGroup(),
        q.allergies(),
        q.emergencyContact(),
        q.address()
      );
    }
    audit.record(id, "UPDATE", "User", id, "Profile updated");
    return get(id);
  }

  @Transactional
  public ProfileView updateDoctor(UUID id, DoctorUpdate q) {
    DoctorProfile d = doctors
      .findByUserId(id)
      .orElseThrow(() ->
        new ForbiddenOperationException("Doctor profile required")
      );
    Department dep = departments
      .findById(q.departmentId())
      .orElseThrow(() -> new NotFoundException("Department not found"));
    d.updateProfessional(
      q.specialization(),
      dep,
      q.biography(),
      q.consultationMinutes()
    );
    audit.record(
      id,
      "UPDATE",
      "DoctorProfile",
      d.getId(),
      "Professional profile updated"
    );
    return get(id);
  }

  public record UpdateRequest(
    @NotBlank @Size(max = 80) String firstName,
    @NotBlank @Size(max = 80) String lastName,
    @Size(max = 30) String phone,
    @Past LocalDate dateOfBirth,
    @Size(max = 10) String bloodGroup,
    @Size(max = 1000) String allergies,
    @Size(max = 120) String emergencyContact,
    @Size(max = 500) String address
  ) {}

  public record DoctorUpdate(
    @NotBlank @Size(max = 120) String specialization,
    @NotNull UUID departmentId,
    @Size(max = 1500) String biography,
    @Min(10) @Max(180) int consultationMinutes
  ) {}

  public record PatientData(
    LocalDate dateOfBirth,
    String bloodGroup,
    String allergies,
    String emergencyContact,
    String address
  ) {}

  public record DoctorData(
    String licenseNumber,
    String specialization,
    UUID departmentId,
    String department,
    String biography,
    int consultationMinutes
  ) {}

  public record ProfileView(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String phone,
    Role role,
    PatientData patient,
    DoctorData doctor
  ) {}
}
