package com.synapse.hea.billing;

import com.synapse.hea.appointment.Appointment;
import com.synapse.hea.appointment.AppointmentRepository;
import com.synapse.hea.audit.AuditService;
import com.synapse.hea.common.api.PageResponse;
import com.synapse.hea.common.exception.*;
import com.synapse.hea.notification.NotificationService;
import com.synapse.hea.patient.*;
import com.synapse.hea.pdf.PdfDocumentFactory;
import com.synapse.hea.user.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {
  private final InvoiceRepository invoices;
  private final PatientProfileRepository patients;
  private final AppointmentRepository appointments;
  private final UserRepository users;
  private final NotificationService notifications;
  private final AuditService audit;

  public BillingService(InvoiceRepository invoices, PatientProfileRepository patients,
                        AppointmentRepository appointments, UserRepository users,
                        NotificationService notifications, AuditService audit) {
    this.invoices = invoices;
    this.patients = patients;
    this.appointments = appointments;
    this.users = users;
    this.notifications = notifications;
    this.audit = audit;
  }

  @Transactional
  public View create(UUID actorId, CreateRequest q) {
    requireAdmin(actorId);
    PatientProfile patient = patients.findById(q.patientId())
      .orElseThrow(() -> new NotFoundException("Patient not found"));
    Appointment appointment = q.appointmentId() == null ? null : appointments.findById(q.appointmentId())
      .orElseThrow(() -> new NotFoundException("Appointment not found"));
    if (appointment != null && !appointment.getPatient().getId().equals(patient.getId()))
      throw new ConflictException("Appointment belongs to a different patient");
    Invoice invoice = new Invoice(nextNumber(), patient, appointment, q.currency(), q.dueDate(), q.notes());
    q.items().forEach(item -> invoice.addItem(item.description(), item.quantity(), item.unitPrice()));
    invoice.calculate(q.taxPercent(), q.discountAmount());
    Invoice saved = invoices.save(invoice);
    audit.record(actorId, "CREATE", "Invoice", saved.getId(), saved.getInvoiceNumber());
    return view(saved);
  }

  @Transactional(readOnly = true)
  public PageResponse<View> list(UUID actorId, int page, int size) {
    User actor = users.findById(actorId).orElseThrow();
    var pageable = PageRequest.of(page, Math.min(size, 100));
    if (actor.getRole() == Role.PATIENT) {
      UUID patientId = patients.findByUserId(actorId).orElseThrow().getId();
      return PageResponse.from(invoices.findByPatientIdOrderByCreatedAtDesc(patientId, pageable).map(this::view));
    }
    if (actor.getRole() != Role.ADMIN) throw new ForbiddenOperationException("Billing access requires admin role");
    return PageResponse.from(invoices.findAll(pageable).map(this::view));
  }

  @Transactional(readOnly = true)
  public View get(UUID actorId, UUID id) { return view(authorized(actorId, id)); }

  @Transactional
  public View transition(UUID actorId, UUID id, StatusRequest q) {
    requireAdmin(actorId);
    Invoice invoice = invoices.findById(id).orElseThrow(() -> new NotFoundException("Invoice not found"));
    try {
      switch (q.status()) {
        case ISSUED -> invoice.issue();
        case PAID -> invoice.markPaid();
        case CANCELLED -> invoice.cancel();
        default -> throw new ConflictException("Unsupported status transition");
      }
    } catch (IllegalStateException ex) {
      throw new ConflictException(ex.getMessage());
    }
    notifications.create(invoice.getPatient().getUser(), "Invoice " + q.status().name().toLowerCase(),
      invoice.getInvoiceNumber() + " · " + invoice.getCurrency() + " " + invoice.getTotalAmount(), "BILLING");
    audit.record(actorId, "UPDATE", "Invoice", id, "Status changed to " + q.status());
    return view(invoice);
  }

  @Transactional(readOnly = true)
  public byte[] pdf(UUID actorId, UUID id) {
    Invoice i = authorized(actorId, id);
    List<String> lines = new ArrayList<>();
    lines.add("Invoice number: " + i.getInvoiceNumber());
    lines.add("Status: " + i.getStatus());
    lines.add("Patient: " + i.getPatient().getUser().getDisplayName());
    lines.add("Email: " + i.getPatient().getUser().getEmail());
    lines.add("Created: " + DateTimeFormatter.ISO_LOCAL_DATE.format(i.getCreatedAt().atZone(ZoneOffset.UTC)));
    lines.add("Due date: " + Objects.toString(i.getDueDate(), "Not set"));
    lines.add(" ");
    lines.add("ITEMS");
    for (InvoiceItem item : i.getItems()) {
      lines.add(item.getDescription() + " | " + item.getQuantity() + " x " + item.getUnitPrice() + " = " + item.getLineTotal());
    }
    lines.add(" ");
    lines.add("Subtotal: " + i.getCurrency() + " " + i.getSubtotal());
    lines.add("Discount: " + i.getCurrency() + " " + i.getDiscountAmount());
    lines.add("Tax: " + i.getCurrency() + " " + i.getTaxAmount());
    lines.add("TOTAL: " + i.getCurrency() + " " + i.getTotalAmount());
    if (i.getNotes() != null) lines.add("Notes: " + i.getNotes());
    return PdfDocumentFactory.textDocument("Synapse HEA Invoice", lines);
  }

  private Invoice authorized(UUID actorId, UUID id) {
    User actor = users.findById(actorId).orElseThrow();
    Invoice invoice = invoices.findById(id).orElseThrow(() -> new NotFoundException("Invoice not found"));
    if (actor.getRole() == Role.ADMIN) return invoice;
    if (actor.getRole() == Role.PATIENT && invoice.getPatient().getUser().getId().equals(actorId)) return invoice;
    throw new ForbiddenOperationException("You cannot access this invoice");
  }

  private void requireAdmin(UUID actorId) {
    User actor = users.findById(actorId).orElseThrow();
    if (actor.getRole() != Role.ADMIN) throw new ForbiddenOperationException("Admin role required");
  }

  private String nextNumber() {
    return "HEA-" + LocalDate.now(ZoneOffset.UTC).getYear() + "-" +
      UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
  }

  private View view(Invoice i) {
    return new View(i.getId(), i.getInvoiceNumber(), i.getPatient().getId(),
      i.getPatient().getUser().getDisplayName(), i.getAppointment() == null ? null : i.getAppointment().getId(),
      i.getStatus(), i.getSubtotal(), i.getTaxAmount(), i.getDiscountAmount(), i.getTotalAmount(),
      i.getCurrency(), i.getDueDate(), i.getIssuedAt(), i.getPaidAt(), i.getNotes(), i.getCreatedAt(),
      i.getItems().stream().map(x -> new ItemView(x.getId(), x.getDescription(), x.getQuantity(), x.getUnitPrice(), x.getLineTotal())).toList());
  }

  public record ItemRequest(
    @NotBlank @Size(max = 180) String description,
    @NotNull @DecimalMin("0.01") BigDecimal quantity,
    @NotNull @DecimalMin("0.00") BigDecimal unitPrice
  ) {}

  public record CreateRequest(
    @NotNull UUID patientId,
    UUID appointmentId,
    @Size(min = 3, max = 3) String currency,
    @FutureOrPresent LocalDate dueDate,
    @NotEmpty @Size(max = 50) List<@Valid ItemRequest> items,
    @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal taxPercent,
    @DecimalMin("0.00") BigDecimal discountAmount,
    @Size(max = 1000) String notes
  ) {}

  public record StatusRequest(@NotNull InvoiceStatus status) {}
  public record ItemView(UUID id, String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}
  public record View(UUID id, String invoiceNumber, UUID patientId, String patientName, UUID appointmentId,
    InvoiceStatus status, BigDecimal subtotal, BigDecimal taxAmount, BigDecimal discountAmount,
    BigDecimal totalAmount, String currency, LocalDate dueDate, java.time.Instant issuedAt,
    java.time.Instant paidAt, String notes, java.time.Instant createdAt, List<ItemView> items) {}
}
