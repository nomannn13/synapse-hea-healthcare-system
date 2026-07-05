package com.synapse.hea.admin;

import com.synapse.hea.appointment.*;
import com.synapse.hea.billing.*;
import com.synapse.hea.prescription.*;
import com.synapse.hea.document.*;
import com.synapse.hea.audit.*;
import com.synapse.hea.common.api.PageResponse;
import com.synapse.hea.common.exception.NotFoundException;
import com.synapse.hea.department.*;
import com.synapse.hea.resource.*;
import com.synapse.hea.urgent.*;
import com.synapse.hea.user.*;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

  private final UserRepository users;
  private final HospitalResourceRepository resources;
  private final DepartmentRepository departments;
  private final AuditLogRepository audits;
  private final AppointmentRepository appointments;
  private final UrgentCaseRepository urgent;
  private final AuditService audit;
  private final InvoiceRepository invoices;
  private final PrescriptionRepository prescriptions;
  private final ClinicalDocumentRepository documents;

  public AdminService(
    UserRepository u,
    HospitalResourceRepository r,
    DepartmentRepository d,
    AuditLogRepository a,
    AppointmentRepository ap,
    UrgentCaseRepository ur,
    AuditService au,
    InvoiceRepository i,
    PrescriptionRepository pr,
    ClinicalDocumentRepository cd
  ) {
    users = u;
    resources = r;
    departments = d;
    audits = a;
    appointments = ap;
    urgent = ur;
    audit = au;
    invoices = i;
    prescriptions = pr;
    documents = cd;
  }

  @Transactional(readOnly = true)
  public PageResponse<UserView> users(Role role, int page, int size) {
    var p = PageRequest.of(page, Math.min(size, 100));
    return PageResponse.from(
      (role == null ? users.findAll(p) : users.findByRole(role, p)).map(
        this::view
      )
    );
  }

  @Transactional
  public UserView status(UUID actor, UUID id, StatusRequest q) {
    User u = users
      .findById(id)
      .orElseThrow(() -> new NotFoundException("User not found"));
    u.changeStatus(q.status());
    audit.record(
      actor,
      "UPDATE",
      "User",
      id,
      "Status changed to " + q.status()
    );
    return view(u);
  }

  @Transactional
  public ResourceView createResource(UUID actor, CreateResource q) {
    Department d = departments
      .findById(q.departmentId())
      .orElseThrow(() -> new NotFoundException("Department not found"));
    HospitalResource r = resources.save(
      new HospitalResource(d, q.type(), q.code(), q.name(), q.notes())
    );
    audit.record(actor, "CREATE", "HospitalResource", r.getId(), r.getCode());
    return resourceView(r);
  }

  @Transactional
  public ResourceView updateResource(UUID actor, UUID id, UpdateResource q) {
    HospitalResource r = resources
      .findById(id)
      .orElseThrow(() -> new NotFoundException("Resource not found"));
    r.update(q.status(), q.notes());
    audit.record(
      actor,
      "UPDATE",
      "HospitalResource",
      id,
      "Status " + q.status()
    );
    return resourceView(r);
  }

  @Transactional(readOnly = true)
  public PageResponse<ResourceView> resources(int page, int size) {
    return PageResponse.from(
      resources
        .findAllByOrderByNameAsc(PageRequest.of(page, Math.min(size, 100)))
        .map(this::resourceView)
    );
  }

  @Transactional(readOnly = true)
  public PageResponse<AuditView> audits(String action, String entityType, UUID actorId, int page, int size) {
    String normalizedAction = action == null || action.isBlank() ? null : action.trim().toUpperCase();
    String normalizedEntity = entityType == null || entityType.isBlank() ? null : entityType.trim();
    return PageResponse.from(
      audits
        .search(normalizedAction, normalizedEntity, actorId,
          PageRequest.of(page, Math.min(size, 100))
        )
        .map(a ->
          new AuditView(
            a.getId(),
            a.getActorId(),
            a.getAction(),
            a.getEntityType(),
            a.getEntityId(),
            a.getDetails(),
            a.getCreatedAt()
          )
        )
    );
  }

  @Transactional(readOnly = true)
  public SummaryReport summary() {
    return new SummaryReport(
      users.count(),
      users.countByRole(Role.PATIENT),
      users.countByRole(Role.DOCTOR),
      appointments.count(),
      appointments.countByStatus(AppointmentStatus.CONFIRMED),
      appointments.countByStatus(AppointmentStatus.COMPLETED),
      urgent.countByStatus(UrgentCase.Status.OPEN),
      resources.count(),
      resources.countByStatus(HospitalResource.Status.AVAILABLE),
      invoices.count(),
      invoices.countByStatus(InvoiceStatus.ISSUED) + invoices.countByStatus(InvoiceStatus.OVERDUE),
      invoices.totalPaidRevenue(),
      prescriptions.count(),
      documents.count()
    );
  }

  private UserView view(User u) {
    return new UserView(
      u.getId(),
      u.getEmail(),
      u.getFirstName(),
      u.getLastName(),
      u.getPhone(),
      u.getRole(),
      u.getStatus(),
      u.getCreatedAt()
    );
  }

  private ResourceView resourceView(HospitalResource r) {
    return new ResourceView(
      r.getId(),
      r.getDepartment().getId(),
      r.getDepartment().getName(),
      r.getType(),
      r.getCode(),
      r.getName(),
      r.getStatus(),
      r.getNotes()
    );
  }

  public record StatusRequest(@NotNull UserStatus status) {}

  public record CreateResource(
    @NotNull UUID departmentId,
    @NotNull HospitalResource.Type type,
    @NotBlank @Size(max = 60) String code,
    @NotBlank @Size(max = 140) String name,
    @Size(max = 1000) String notes
  ) {}

  public record UpdateResource(
    @NotNull HospitalResource.Status status,
    @Size(max = 1000) String notes
  ) {}

  public record UserView(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String phone,
    Role role,
    UserStatus status,
    java.time.Instant createdAt
  ) {}

  public record ResourceView(
    UUID id,
    UUID departmentId,
    String department,
    HospitalResource.Type type,
    String code,
    String name,
    HospitalResource.Status status,
    String notes
  ) {}

  public record AuditView(
    UUID id,
    UUID actorId,
    String action,
    String entityType,
    UUID entityId,
    String details,
    java.time.Instant createdAt
  ) {}

  public record SummaryReport(
    long users,
    long patients,
    long doctors,
    long appointments,
    long confirmedAppointments,
    long completedAppointments,
    long openUrgentCases,
    long resources,
    long availableResources,
    long invoices,
    long unpaidInvoices,
    java.math.BigDecimal paidRevenue,
    long prescriptions,
    long clinicalDocuments
  ) {}
}
