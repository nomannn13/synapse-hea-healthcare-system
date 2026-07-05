package com.synapse.hea.appointment;

import static com.synapse.hea.appointment.AppointmentDtos.*;

import com.synapse.hea.audit.AuditService;
import com.synapse.hea.common.api.PageResponse;
import com.synapse.hea.common.exception.ConflictException;
import com.synapse.hea.common.exception.ForbiddenOperationException;
import com.synapse.hea.common.exception.NotFoundException;
import com.synapse.hea.doctor.AvailabilitySlotRepository;
import com.synapse.hea.doctor.DoctorProfile;
import com.synapse.hea.doctor.DoctorProfileRepository;
import com.synapse.hea.notification.NotificationService;
import com.synapse.hea.patient.PatientProfile;
import com.synapse.hea.patient.PatientProfileRepository;
import com.synapse.hea.user.Role;
import com.synapse.hea.user.User;
import com.synapse.hea.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {

  private final AppointmentRepository appointments;
  private final DoctorProfileRepository doctors;
  private final PatientProfileRepository patients;
  private final AvailabilitySlotRepository slots;
  private final UserRepository users;
  private final NotificationService notifications;
  private final AuditService audit;

  public AppointmentService(
    AppointmentRepository appointments,
    DoctorProfileRepository doctors,
    PatientProfileRepository patients,
    AvailabilitySlotRepository slots,
    UserRepository users,
    NotificationService notifications,
    AuditService audit
  ) {
    this.appointments = appointments;
    this.doctors = doctors;
    this.patients = patients;
    this.slots = slots;
    this.users = users;
    this.notifications = notifications;
    this.audit = audit;
  }

  @Transactional
  public View create(UUID userId, CreateRequest request) {
    PatientProfile patient = patients
      .findByUserId(userId)
      .orElseThrow(() ->
        new ForbiddenOperationException("Patient profile required")
      );
    DoctorProfile doctor = doctors
      .lockById(request.doctorId())
      .orElseThrow(() -> new NotFoundException("Doctor not found"));
    Instant endAt = request
      .startAt()
      .plus(Duration.ofMinutes(doctor.getConsultationMinutes()));
    validateSlot(doctor, patient, request.startAt(), endAt, null);
    Appointment saved = appointments.save(
      new Appointment(
        patient,
        doctor,
        request.startAt(),
        endAt,
        request.reason()
      )
    );
    notifications.create(
      patient.getUser(),
      "Appointment requested",
      "Your appointment with " +
        doctor.getUser().getDisplayName() +
        " was created.",
      "APPOINTMENT"
    );
    notifications.create(
      doctor.getUser(),
      "New appointment",
      patient.getUser().getDisplayName() + " requested an appointment.",
      "APPOINTMENT"
    );
    audit.record(
      userId,
      "CREATE",
      "Appointment",
      saved.getId(),
      "Appointment booked"
    );
    return toView(saved);
  }

  @Transactional
  public View reschedule(
    UUID userId,
    UUID appointmentId,
    RescheduleRequest request
  ) {
    Appointment appointment = ownedByPatient(userId, appointmentId);
    if (
      appointment.getStatus() == AppointmentStatus.CANCELLED ||
      appointment.getStatus() == AppointmentStatus.COMPLETED
    ) {
      throw new ConflictException(
        "This appointment can no longer be rescheduled"
      );
    }
    DoctorProfile doctor = doctors
      .lockById(appointment.getDoctor().getId())
      .orElseThrow();
    Instant endAt = request
      .startAt()
      .plus(Duration.ofMinutes(doctor.getConsultationMinutes()));
    validateSlot(
      doctor,
      appointment.getPatient(),
      request.startAt(),
      endAt,
      appointmentId
    );
    appointment.reschedule(request.startAt(), endAt);
    notifications.create(
      doctor.getUser(),
      "Appointment rescheduled",
      "An appointment was moved to " + request.startAt(),
      "APPOINTMENT"
    );
    audit.record(
      userId,
      "UPDATE",
      "Appointment",
      appointmentId,
      "Appointment rescheduled"
    );
    return toView(appointment);
  }

  @Transactional
  public View cancel(UUID userId, UUID appointmentId, CancelRequest request) {
    Appointment appointment = appointments
      .findById(appointmentId)
      .orElseThrow(() -> new NotFoundException("Appointment not found"));
    User actor = users.findById(userId).orElseThrow();
    boolean allowed =
      actor.getRole() == Role.ADMIN ||
      appointment.getPatient().getUser().getId().equals(userId) ||
      appointment.getDoctor().getUser().getId().equals(userId);
    if (!allowed) throw new ForbiddenOperationException(
      "You cannot cancel this appointment"
    );
    if (
      appointment.getStatus() == AppointmentStatus.CANCELLED ||
      appointment.getStatus() == AppointmentStatus.COMPLETED
    ) throw new ConflictException("Appointment is already closed");
    appointment.cancel(request.reason());
    notifications.create(
      appointment.getPatient().getUser(),
      "Appointment cancelled",
      request.reason(),
      "APPOINTMENT"
    );
    notifications.create(
      appointment.getDoctor().getUser(),
      "Appointment cancelled",
      request.reason(),
      "APPOINTMENT"
    );
    audit.record(
      userId,
      "UPDATE",
      "Appointment",
      appointmentId,
      "Appointment cancelled"
    );
    return toView(appointment);
  }

  @Transactional
  public View updateStatus(
    UUID userId,
    UUID appointmentId,
    StatusRequest request
  ) {
    Appointment appointment = appointments
      .findById(appointmentId)
      .orElseThrow(() -> new NotFoundException("Appointment not found"));
    User actor = users.findById(userId).orElseThrow();
    if (
      actor.getRole() != Role.ADMIN &&
      !appointment.getDoctor().getUser().getId().equals(userId)
    ) throw new ForbiddenOperationException(
      "Only the assigned doctor or an admin can update status"
    );
    appointment.updateStatus(request.status(), request.notes());
    notifications.create(
      appointment.getPatient().getUser(),
      "Appointment updated",
      "Status: " + request.status(),
      "APPOINTMENT"
    );
    audit.record(
      userId,
      "UPDATE",
      "Appointment",
      appointmentId,
      "Status changed to " + request.status()
    );
    return toView(appointment);
  }

  @Transactional(readOnly = true)
  public PageResponse<View> mine(UUID userId, int page, int size) {
    User user = users.findById(userId).orElseThrow();
    var pageable = PageRequest.of(page, Math.min(size, 50));
    if (user.getRole() == Role.PATIENT) {
      UUID patientId = patients.findByUserId(userId).orElseThrow().getId();
      return PageResponse.from(
        appointments
          .findByPatientIdOrderByStartAtDesc(patientId, pageable)
          .map(this::toView)
      );
    }
    if (user.getRole() == Role.DOCTOR) {
      UUID doctorId = doctors.findByUserId(userId).orElseThrow().getId();
      return PageResponse.from(
        appointments
          .findByDoctorIdOrderByStartAtDesc(doctorId, pageable)
          .map(this::toView)
      );
    }
    return PageResponse.from(appointments.findAll(pageable).map(this::toView));
  }

  private Appointment ownedByPatient(UUID userId, UUID id) {
    Appointment a = appointments
      .findById(id)
      .orElseThrow(() -> new NotFoundException("Appointment not found"));
    if (
      !a.getPatient().getUser().getId().equals(userId)
    ) throw new ForbiddenOperationException("Not your appointment");
    return a;
  }

  private void validateSlot(
    DoctorProfile doctor,
    PatientProfile patient,
    Instant startAt,
    Instant endAt,
    UUID excludeId
  ) {
    if (
      startAt.isBefore(Instant.now().plusSeconds(300))
    ) throw new ConflictException(
      "Appointment must be at least five minutes in the future"
    );
    boolean hasConfiguredSlots = !slots
      .findWindow(
        doctor.getId(),
        startAt.minus(Duration.ofDays(1)),
        endAt.plus(Duration.ofDays(1))
      )
      .isEmpty();
    if (
      hasConfiguredSlots &&
      !slots.existsByDoctorIdAndActiveTrueAndStartAtLessThanAndEndAtGreaterThan(
        doctor.getId(),
        endAt.plusNanos(1),
        startAt.minusNanos(1)
      )
    ) throw new ConflictException("Doctor is not available in this period");
    if (
      appointments.hasDoctorConflict(doctor.getId(), startAt, endAt, excludeId)
    ) throw new ConflictException(
      "Doctor already has an overlapping appointment"
    );
    if (
      appointments.hasPatientConflict(
        patient.getId(),
        startAt,
        endAt,
        excludeId
      )
    ) throw new ConflictException(
      "You already have an overlapping appointment"
    );
  }

  public View toView(Appointment a) {
    return new View(
      a.getId(),
      a.getPatient().getId(),
      a.getPatient().getUser().getDisplayName(),
      a.getDoctor().getId(),
      a.getDoctor().getUser().getDisplayName(),
      a.getDepartment().getName(),
      a.getStartAt(),
      a.getEndAt(),
      a.getStatus(),
      a.getReason(),
      a.getNotes(),
      a.getCancellationReason()
    );
  }
}
