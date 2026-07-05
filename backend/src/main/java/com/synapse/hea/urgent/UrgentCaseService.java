package com.synapse.hea.urgent;

import com.synapse.hea.audit.AuditService;
import com.synapse.hea.common.api.PageResponse;
import com.synapse.hea.common.exception.*;
import com.synapse.hea.notification.NotificationService;
import com.synapse.hea.patient.*;
import com.synapse.hea.user.*;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrgentCaseService {

  private final UrgentCaseRepository cases;
  private final PatientProfileRepository patients;
  private final UserRepository users;
  private final NotificationService notifications;
  private final AuditService audit;

  public UrgentCaseService(
    UrgentCaseRepository c,
    PatientProfileRepository p,
    UserRepository u,
    NotificationService n,
    AuditService a
  ) {
    cases = c;
    patients = p;
    users = u;
    notifications = n;
    audit = a;
  }

  @Transactional
  public View create(UUID actorId, CreateRequest q) {
    User actor = users.findById(actorId).orElseThrow();
    if (actor.getRole() == Role.PATIENT) throw new ForbiddenOperationException(
      "Medical staff access required"
    );
    PatientProfile p = patients
      .findById(q.patientId())
      .orElseThrow(() -> new NotFoundException("Patient not found"));
    UrgentCase.Priority pr = priority(q);
    UrgentCase saved = cases.save(
      new UrgentCase(
        p,
        actor,
        pr,
        q.heartRate(),
        q.oxygenSaturation(),
        q.systolicPressure(),
        q.temperature(),
        q.symptomSeverity(),
        q.reason()
      )
    );
    notifications.create(
      p.getUser(),
      "Urgent case recorded",
      "Priority: " + pr + ". Contact hospital staff for instructions.",
      "URGENT"
    );
    audit.record(
      actorId,
      "CREATE",
      "UrgentCase",
      saved.getId(),
      "Priority " + pr
    );
    return view(saved);
  }

  @Transactional(readOnly = true)
  public PageResponse<View> list(int page, int size) {
    return PageResponse.from(
      cases
        .findAllByOrderByCreatedAtDesc(PageRequest.of(page, Math.min(size, 50)))
        .map(this::view)
    );
  }

  UrgentCase.Priority priority(CreateRequest q) {
    if (
      val(q.oxygenSaturation(), 100) < 92
    ) return UrgentCase.Priority.CRITICAL;
    if (
      val(q.systolicPressure(), 120) < 90 ||
      val(q.heartRate(), 80) > 130 ||
      val(q.heartRate(), 80) < 45
    ) return UrgentCase.Priority.HIGH;
    if (
      val(q.symptomSeverity(), 0) >= 8 || val(q.temperature(), 37d) >= 39.5
    ) return UrgentCase.Priority.HIGH;
    if (val(q.symptomSeverity(), 0) >= 5) return UrgentCase.Priority.MEDIUM;
    return UrgentCase.Priority.LOW;
  }

  private int val(Integer v, int d) {
    return v == null ? d : v;
  }

  private double val(Double v, double d) {
    return v == null ? d : v;
  }

  private View view(UrgentCase c) {
    return new View(
      c.getId(),
      c.getPatient().getId(),
      c.getPatient().getUser().getDisplayName(),
      c.getPriority(),
      c.getHeartRate(),
      c.getOxygenSaturation(),
      c.getSystolicPressure(),
      c.getTemperature(),
      c.getSymptomSeverity(),
      c.getReason(),
      c.getStatus(),
      c.getCreatedAt()
    );
  }

  public record CreateRequest(
    @NotNull UUID patientId,
    @Min(20) @Max(250) Integer heartRate,
    @Min(50) @Max(100) Integer oxygenSaturation,
    @Min(40) @Max(260) Integer systolicPressure,
    @DecimalMin("30.0") @DecimalMax("45.0") Double temperature,
    @Min(0) @Max(10) Integer symptomSeverity,
    @NotBlank @Size(max = 1000) String reason
  ) {}

  public record View(
    UUID id,
    UUID patientId,
    String patientName,
    UrgentCase.Priority priority,
    Integer heartRate,
    Integer oxygenSaturation,
    Integer systolicPressure,
    Double temperature,
    Integer symptomSeverity,
    String reason,
    UrgentCase.Status status,
    java.time.Instant createdAt
  ) {}
}
