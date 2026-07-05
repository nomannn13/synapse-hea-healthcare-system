package com.synapse.hea.doctor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvailabilitySlotRepository
  extends JpaRepository<AvailabilitySlot, UUID>
{
  @Query(
    "select s from AvailabilitySlot s where s.doctor.id=:doctorId and s.active=true and s.startAt < :to and s.endAt > :from order by s.startAt"
  )
  List<AvailabilitySlot> findWindow(
    @Param("doctorId") UUID doctorId,
    @Param("from") Instant from,
    @Param("to") Instant to
  );

  boolean existsByDoctorIdAndActiveTrueAndStartAtLessThanAndEndAtGreaterThan(
    UUID doctorId,
    Instant endAt,
    Instant startAt
  );

  List<AvailabilitySlot> findByDoctorIdAndActiveTrueOrderByStartAtAsc(UUID doctorId);
}
