package com.synapse.hea.medical;

import com.synapse.hea.appointment.Appointment;
import com.synapse.hea.appointment.AppointmentRepository;
import com.synapse.hea.audit.AuditService;
import com.synapse.hea.common.api.PageResponse;
import com.synapse.hea.common.exception.*;
import com.synapse.hea.doctor.*;
import com.synapse.hea.notification.NotificationService;
import com.synapse.hea.patient.*;
import com.synapse.hea.user.*;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicalRecordService {

  private final MedicalRecordRepository records;
  private final PatientProfileRepository patients;
  private final DoctorProfileRepository doctors;
  private final AppointmentRepository appointments;
  private final UserRepository users;
  private final NotificationService notifications;
  private final AuditService audit;

  public MedicalRecordService(
    MedicalRecordRepository r,
    PatientProfileRepository p,
    DoctorProfileRepository d,
    AppointmentRepository a,
    UserRepository u,
    NotificationService n,
    AuditService au
  ) {
    records = r;
    patients = p;
    doctors = d;
    appointments = a;
    users = u;
    notifications = n;
    audit = au;
  }

  @Transactional
  public View create(UUID actorId, CreateRequest q) {
    User actor = users.findById(actorId).orElseThrow();
    if (
      actor.getRole() != Role.DOCTOR && actor.getRole() != Role.ADMIN
    ) throw new ForbiddenOperationException("Medical staff access required");
    DoctorProfile doctor =
      actor.getRole() == Role.DOCTOR
        ? doctors.findByUserId(actorId).orElseThrow()
        : doctors
            .findById(q.doctorId())
            .orElseThrow(() -> new NotFoundException("Doctor not found"));
    PatientProfile patient = patients
      .findById(q.patientId())
      .orElseThrow(() -> new NotFoundException("Patient not found"));
    Appointment ap =
      q.appointmentId() == null
        ? null
        : appointments
            .findById(q.appointmentId())
            .orElseThrow(() -> new NotFoundException("Appointment not found"));
    MedicalRecord saved = records.save(
      new MedicalRecord(
        patient,
        doctor,
        ap,
        q.diagnosis(),
        q.treatment(),
        q.prescription(),
        q.clinicalNotes()
      )
    );
    notifications.create(
      patient.getUser(),
      "Medical record updated",
      "A new record is available in your history.",
      "MEDICAL_RECORD"
    );
    audit.record(
      actorId,
      "CREATE",
      "MedicalRecord",
      saved.getId(),
      "Medical record created"
    );
    return view(saved);
  }

  @Transactional(readOnly = true)
  public PageResponse<View> mine(
    UUID actorId,
    UUID patientId,
    int page,
    int size
  ) {
    User actor = users.findById(actorId).orElseThrow();
    UUID target;
    if (actor.getRole() == Role.PATIENT) target = patients
      .findByUserId(actorId)
      .orElseThrow()
      .getId();
    else if (patientId != null) target = patientId;
    else throw new ForbiddenOperationException(
      "patientId is required for staff"
    );
    return PageResponse.from(
      records
        .findByPatientIdOrderByRecordedAtDesc(
          target,
          PageRequest.of(page, Math.min(size, 50))
        )
        .map(this::view)
    );
  }

  @Transactional
  public View update(UUID actorId, UUID id, UpdateRequest q) {
    MedicalRecord r = records
      .findById(id)
      .orElseThrow(() -> new NotFoundException("Record not found"));
    User actor = users.findById(actorId).orElseThrow();
    if (
      actor.getRole() != Role.ADMIN &&
      !r.getDoctor().getUser().getId().equals(actorId)
    ) throw new ForbiddenOperationException(
      "Only the authoring doctor or admin can edit"
    );
    r.update(q.diagnosis(), q.treatment(), q.prescription(), q.clinicalNotes());
    audit.record(
      actorId,
      "UPDATE",
      "MedicalRecord",
      id,
      "Medical record updated"
    );
    return view(r);
  }

  private View view(MedicalRecord r) {
    return new View(
      r.getId(),
      r.getPatient().getId(),
      r.getPatient().getUser().getDisplayName(),
      r.getDoctor().getId(),
      r.getDoctor().getUser().getDisplayName(),
      r.getAppointment() == null ? null : r.getAppointment().getId(),
      r.getDiagnosis(),
      r.getTreatment(),
      r.getPrescription(),
      r.getClinicalNotes(),
      r.getRecordedAt()
    );
  }

  public record CreateRequest(
    @jakarta.validation.constraints.NotNull UUID patientId,
    UUID doctorId,
    UUID appointmentId,
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max = 500)
    String diagnosis,
    @jakarta.validation.constraints.Size(max = 1500) String treatment,
    @jakarta.validation.constraints.Size(max = 1500) String prescription,
    @jakarta.validation.constraints.Size(max = 3000) String clinicalNotes
  ) {}

  public record UpdateRequest(
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max = 500)
    String diagnosis,
    @jakarta.validation.constraints.Size(max = 1500) String treatment,
    @jakarta.validation.constraints.Size(max = 1500) String prescription,
    @jakarta.validation.constraints.Size(max = 3000) String clinicalNotes
  ) {}

  public record View(
    UUID id,
    UUID patientId,
    String patientName,
    UUID doctorId,
    String doctorName,
    UUID appointmentId,
    String diagnosis,
    String treatment,
    String prescription,
    String clinicalNotes,
    java.time.Instant recordedAt
  ) {}
}
