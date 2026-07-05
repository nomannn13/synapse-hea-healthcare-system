package com.synapse.hea.appointment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository
  extends JpaRepository<Appointment, UUID>
{
  @Query(
    """
    select (count(a) > 0) from Appointment a
    where a.doctor.id=:doctorId and a.status not in (com.synapse.hea.appointment.AppointmentStatus.CANCELLED, com.synapse.hea.appointment.AppointmentStatus.NO_SHOW)
    and a.startAt < :endAt and a.endAt > :startAt and (:excludeId is null or a.id <> :excludeId)
    """
  )
  boolean hasDoctorConflict(
    @Param("doctorId") UUID doctorId,
    @Param("startAt") Instant startAt,
    @Param("endAt") Instant endAt,
    @Param("excludeId") UUID excludeId
  );

  @Query(
    """
    select (count(a) > 0) from Appointment a
    where a.patient.id=:patientId and a.status not in (com.synapse.hea.appointment.AppointmentStatus.CANCELLED, com.synapse.hea.appointment.AppointmentStatus.NO_SHOW)
    and a.startAt < :endAt and a.endAt > :startAt and (:excludeId is null or a.id <> :excludeId)
    """
  )
  boolean hasPatientConflict(
    @Param("patientId") UUID patientId,
    @Param("startAt") Instant startAt,
    @Param("endAt") Instant endAt,
    @Param("excludeId") UUID excludeId
  );

  Page<Appointment> findByPatientIdOrderByStartAtDesc(
    UUID patientId,
    Pageable pageable
  );
  Page<Appointment> findByDoctorIdOrderByStartAtDesc(
    UUID doctorId,
    Pageable pageable
  );
  List<Appointment> findTop5ByPatientIdAndStartAtAfterAndStatusNotOrderByStartAtAsc(
    UUID patientId,
    Instant now,
    AppointmentStatus status
  );
  List<Appointment> findTop5ByDoctorIdAndStartAtAfterAndStatusNotOrderByStartAtAsc(
    UUID doctorId,
    Instant now,
    AppointmentStatus status
  );
  long countByStatus(AppointmentStatus status);
  long countByPatientId(UUID patientId);
  long countByDoctorIdAndStartAtAfter(UUID doctorId, Instant startAt);
}
