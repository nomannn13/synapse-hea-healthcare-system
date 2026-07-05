package com.synapse.hea.doctor;

import com.synapse.hea.appointment.*;
import com.synapse.hea.audit.AuditService;
import com.synapse.hea.common.api.PageResponse;
import com.synapse.hea.common.exception.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorService {

  private final DoctorProfileRepository doctors;
  private final AvailabilitySlotRepository slots;
  private final AppointmentRepository appointments;
  private final AuditService audit;

  public DoctorService(
    DoctorProfileRepository d,
    AvailabilitySlotRepository s,
    AppointmentRepository a,
    AuditService au
  ) {
    doctors = d;
    slots = s;
    appointments = a;
    audit = au;
  }

  @Transactional(readOnly = true)
  public PageResponse<DoctorView> search(
    String query,
    UUID departmentId,
    int page,
    int size
  ) {
    String normalized = query == null || query.isBlank() ? null : query.trim();
    return PageResponse.from(
      doctors
        .search(
          normalized,
          departmentId,
          PageRequest.of(page, Math.min(size, 50))
        )
        .map(this::view)
    );
  }

  @Transactional(readOnly = true)
  public List<SlotView> availability(UUID doctorId, Instant from, Instant to) {
    DoctorProfile d = doctors
      .findById(doctorId)
      .orElseThrow(() -> new NotFoundException("Doctor not found"));
    List<AvailabilitySlot> configured = slots.findWindow(doctorId, from, to);
    List<SlotView> result = new ArrayList<>();
    if (configured.isEmpty()) {
      ZonedDateTime cursor = from
        .atZone(ZoneOffset.UTC)
        .withHour(9)
        .withMinute(0)
        .withSecond(0)
        .withNano(0);
      while (cursor.toInstant().isBefore(to) && result.size() < 300) {
        if (cursor.getDayOfWeek().getValue() <= 5) {
          for (
            int minute = 0;
            minute < 8 * 60;
            minute += d.getConsultationMinutes()
          ) {
            Instant start = cursor.plusMinutes(minute).toInstant(),
              end = start.plus(Duration.ofMinutes(d.getConsultationMinutes()));
            if (
              start.isAfter(Instant.now()) &&
              end.isBefore(to.plusSeconds(1)) &&
              !appointments.hasDoctorConflict(doctorId, start, end, null)
            ) result.add(new SlotView(null, start, end, true));
          }
        }
        cursor = cursor.plusDays(1);
      }
      return result;
    }
    for (AvailabilitySlot slot : configured) {
      for (
        Instant start = slot.getStartAt();
        !start
          .plus(Duration.ofMinutes(d.getConsultationMinutes()))
          .isAfter(slot.getEndAt());
        start = start.plus(Duration.ofMinutes(d.getConsultationMinutes()))
      ) {
        Instant end = start.plus(
          Duration.ofMinutes(d.getConsultationMinutes())
        );
        if (
          start.isAfter(Instant.now()) &&
          !appointments.hasDoctorConflict(doctorId, start, end, null)
        ) result.add(new SlotView(null, start, end, true));
      }
    }
    return result;
  }

  @Transactional
  public SlotView addSlot(UUID userId, SlotRequest q) {
    DoctorProfile d = doctors
      .findByUserId(userId)
      .orElseThrow(() ->
        new ForbiddenOperationException("Doctor profile required")
      );
    if (!q.endAt().isAfter(q.startAt())) throw new ConflictException(
      "End time must be after start time"
    );
    if (Duration.between(q.startAt(), q.endAt()).toDays() > 31)
      throw new ConflictException("Availability blocks cannot exceed 31 days");
    if (slots.existsByDoctorIdAndActiveTrueAndStartAtLessThanAndEndAtGreaterThan(
      d.getId(), q.endAt(), q.startAt())) {
      throw new ConflictException("Availability overlaps an existing schedule block");
    }
    AvailabilitySlot s = slots.save(
      new AvailabilitySlot(d, q.startAt(), q.endAt())
    );
    audit.record(
      userId,
      "CREATE",
      "AvailabilitySlot",
      s.getId(),
      "Doctor availability added"
    );
    return new SlotView(s.getId(), s.getStartAt(), s.getEndAt(), s.isActive());
  }

  @Transactional(readOnly = true)
  public List<SlotView> mySlots(UUID userId) {
    DoctorProfile d = doctors.findByUserId(userId)
      .orElseThrow(() -> new ForbiddenOperationException("Doctor profile required"));
    return slots.findByDoctorIdAndActiveTrueOrderByStartAtAsc(d.getId()).stream()
      .map(s -> new SlotView(s.getId(), s.getStartAt(), s.getEndAt(), s.isActive()))
      .toList();
  }

  @Transactional
  public void deactivateSlot(UUID userId, UUID slotId) {
    DoctorProfile d = doctors.findByUserId(userId)
      .orElseThrow(() -> new ForbiddenOperationException("Doctor profile required"));
    AvailabilitySlot slot = slots.findById(slotId)
      .orElseThrow(() -> new NotFoundException("Availability slot not found"));
    if (!slot.getDoctor().getId().equals(d.getId()))
      throw new ForbiddenOperationException("You cannot change another doctor schedule");
    slot.deactivate();
    audit.record(userId, "DELETE", "AvailabilitySlot", slotId, "Doctor availability removed");
  }

  private DoctorView view(DoctorProfile d) {
    return new DoctorView(
      d.getId(),
      d.getUser().getDisplayName(),
      d.getSpecialization(),
      d.getDepartment().getId(),
      d.getDepartment().getName(),
      d.getBiography(),
      d.getConsultationMinutes()
    );
  }

  public record DoctorView(
    UUID id,
    String name,
    String specialization,
    UUID departmentId,
    String department,
    String biography,
    int consultationMinutes
  ) {}

  public record SlotView(UUID id, Instant startAt, Instant endAt, boolean active) {}

  public record SlotRequest(
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Future
    Instant startAt,
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Future
    Instant endAt
  ) {}
}
