package com.synapse.hea.prescription;

import com.synapse.hea.appointment.*;
import com.synapse.hea.audit.AuditService;
import com.synapse.hea.common.api.PageResponse;
import com.synapse.hea.common.exception.*;
import com.synapse.hea.doctor.*;
import com.synapse.hea.notification.NotificationService;
import com.synapse.hea.patient.*;
import com.synapse.hea.pdf.PdfDocumentFactory;
import com.synapse.hea.user.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrescriptionService {
  private final PrescriptionRepository prescriptions;
  private final PatientProfileRepository patients;
  private final DoctorProfileRepository doctors;
  private final AppointmentRepository appointments;
  private final UserRepository users;
  private final NotificationService notifications;
  private final AuditService audit;

  public PrescriptionService(PrescriptionRepository prescriptions, PatientProfileRepository patients,
      DoctorProfileRepository doctors, AppointmentRepository appointments, UserRepository users,
      NotificationService notifications, AuditService audit) {
    this.prescriptions = prescriptions;
    this.patients = patients;
    this.doctors = doctors;
    this.appointments = appointments;
    this.users = users;
    this.notifications = notifications;
    this.audit = audit;
  }

  @Transactional
  public View create(UUID actorId, CreateRequest q) {
    User actor = users.findById(actorId).orElseThrow();
    if (actor.getRole() != Role.DOCTOR && actor.getRole() != Role.ADMIN)
      throw new ForbiddenOperationException("Only doctors or admins can create prescriptions");
    DoctorProfile doctor = actor.getRole() == Role.DOCTOR
      ? doctors.findByUserId(actorId).orElseThrow()
      : doctors.findById(q.doctorId()).orElseThrow(() -> new NotFoundException("Doctor not found"));
    PatientProfile patient = patients.findById(q.patientId())
      .orElseThrow(() -> new NotFoundException("Patient not found"));
    Appointment appointment = q.appointmentId() == null ? null : appointments.findById(q.appointmentId())
      .orElseThrow(() -> new NotFoundException("Appointment not found"));
    if (appointment != null && (!appointment.getPatient().getId().equals(patient.getId()) ||
        !appointment.getDoctor().getId().equals(doctor.getId())))
      throw new ConflictException("Appointment does not match patient and doctor");
    Prescription p = new Prescription(nextNumber(), patient, doctor, appointment, q.notes());
    q.items().forEach(item -> p.addItem(item.medicationName(), item.dosage(), item.frequency(),
      item.durationDays(), item.instructions()));
    Prescription saved = prescriptions.save(p);
    audit.record(actorId, "CREATE", "Prescription", saved.getId(), saved.getPrescriptionNumber());
    return view(saved);
  }

  @Transactional(readOnly = true)
  public PageResponse<View> list(UUID actorId, int page, int size) {
    User actor = users.findById(actorId).orElseThrow();
    var pageable = PageRequest.of(page, Math.min(size, 100));
    if (actor.getRole() == Role.PATIENT) {
      UUID patientId = patients.findByUserId(actorId).orElseThrow().getId();
      return PageResponse.from(prescriptions.findByPatientIdOrderByCreatedAtDesc(patientId, pageable).map(this::view));
    }
    if (actor.getRole() == Role.DOCTOR) {
      UUID doctorId = doctors.findByUserId(actorId).orElseThrow().getId();
      return PageResponse.from(prescriptions.findByDoctorIdOrderByCreatedAtDesc(doctorId, pageable).map(this::view));
    }
    return PageResponse.from(prescriptions.findAll(pageable).map(this::view));
  }

  @Transactional(readOnly = true)
  public View get(UUID actorId, UUID id) { return view(authorized(actorId, id)); }

  @Transactional
  public View transition(UUID actorId, UUID id, StatusRequest q) {
    Prescription p = prescriptions.findById(id).orElseThrow(() -> new NotFoundException("Prescription not found"));
    requireAuthorOrAdmin(actorId, p);
    try {
      if (q.status() == PrescriptionStatus.ISSUED) p.issue();
      else if (q.status() == PrescriptionStatus.CANCELLED) p.cancel();
      else throw new ConflictException("Unsupported status transition");
    } catch (IllegalStateException ex) {
      throw new ConflictException(ex.getMessage());
    }
    if (q.status() == PrescriptionStatus.ISSUED) {
      notifications.create(p.getPatient().getUser(), "Prescription issued",
        p.getPrescriptionNumber() + " is ready to view and download.", "PRESCRIPTION");
    }
    audit.record(actorId, "UPDATE", "Prescription", id, "Status changed to " + q.status());
    return view(p);
  }

  @Transactional(readOnly = true)
  public byte[] pdf(UUID actorId, UUID id) {
    Prescription p = authorized(actorId, id);
    if (p.getStatus() == PrescriptionStatus.DRAFT && !p.getDoctor().getUser().getId().equals(actorId) &&
        users.findById(actorId).orElseThrow().getRole() != Role.ADMIN)
      throw new ForbiddenOperationException("Draft prescriptions are private");
    List<String> lines = new ArrayList<>();
    lines.add("Prescription number: " + p.getPrescriptionNumber());
    lines.add("Status: " + p.getStatus());
    lines.add("Patient: " + p.getPatient().getUser().getDisplayName());
    lines.add("Doctor: " + p.getDoctor().getUser().getDisplayName());
    lines.add("Specialization: " + p.getDoctor().getSpecialization());
    lines.add("Issued: " + Objects.toString(p.getIssuedAt(), "Draft"));
    lines.add(" ");
    lines.add("MEDICATIONS");
    int number = 1;
    for (PrescriptionItem item : p.getItems()) {
      lines.add(number++ + ". " + item.getMedicationName() + " · " + item.getDosage() + " · " + item.getFrequency());
      lines.add("   Duration: " + Objects.toString(item.getDurationDays(), "As directed") + " days" +
        (item.getInstructions() == null ? "" : " · " + item.getInstructions()));
    }
    if (p.getNotes() != null) {
      lines.add(" ");
      lines.add("Notes: " + p.getNotes());
    }
    lines.add(" ");
    lines.add("This document records a prescription issued by an authorized HEA user. Verify clinical details with the treating professional.");
    return PdfDocumentFactory.textDocument("Synapse HEA Prescription", lines);
  }

  private Prescription authorized(UUID actorId, UUID id) {
    User actor = users.findById(actorId).orElseThrow();
    Prescription p = prescriptions.findById(id).orElseThrow(() -> new NotFoundException("Prescription not found"));
    boolean allowed = actor.getRole() == Role.ADMIN || p.getPatient().getUser().getId().equals(actorId) ||
      p.getDoctor().getUser().getId().equals(actorId);
    if (!allowed) throw new ForbiddenOperationException("You cannot access this prescription");
    return p;
  }

  private void requireAuthorOrAdmin(UUID actorId, Prescription p) {
    User actor = users.findById(actorId).orElseThrow();
    if (actor.getRole() != Role.ADMIN && !p.getDoctor().getUser().getId().equals(actorId))
      throw new ForbiddenOperationException("Only the authoring doctor or admin can update this prescription");
  }

  private String nextNumber() {
    return "RX-" + LocalDate.now(ZoneOffset.UTC).getYear() + "-" +
      UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
  }

  private View view(Prescription p) {
    return new View(p.getId(), p.getPrescriptionNumber(), p.getPatient().getId(),
      p.getPatient().getUser().getDisplayName(), p.getDoctor().getId(), p.getDoctor().getUser().getDisplayName(),
      p.getAppointment() == null ? null : p.getAppointment().getId(), p.getStatus(), p.getNotes(),
      p.getIssuedAt(), p.getCreatedAt(), p.getItems().stream().map(i -> new ItemView(i.getId(), i.getMedicationName(),
        i.getDosage(), i.getFrequency(), i.getDurationDays(), i.getInstructions())).toList());
  }

  public record ItemRequest(
    @NotBlank @Size(max = 180) String medicationName,
    @NotBlank @Size(max = 120) String dosage,
    @NotBlank @Size(max = 120) String frequency,
    @Min(1) @Max(3650) Integer durationDays,
    @Size(max = 500) String instructions
  ) {}
  public record CreateRequest(
    @NotNull UUID patientId,
    UUID doctorId,
    UUID appointmentId,
    @NotEmpty @Size(max = 30) List<@Valid ItemRequest> items,
    @Size(max = 1000) String notes
  ) {}
  public record StatusRequest(@NotNull PrescriptionStatus status) {}
  public record ItemView(UUID id, String medicationName, String dosage, String frequency,
    Integer durationDays, String instructions) {}
  public record View(UUID id, String prescriptionNumber, UUID patientId, String patientName,
    UUID doctorId, String doctorName, UUID appointmentId, PrescriptionStatus status, String notes,
    java.time.Instant issuedAt, java.time.Instant createdAt, List<ItemView> items) {}
}
