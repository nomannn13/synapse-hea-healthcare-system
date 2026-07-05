package com.synapse.hea.medical;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalRecordRepository
  extends JpaRepository<MedicalRecord, UUID>
{
  Page<MedicalRecord> findByPatientIdOrderByRecordedAtDesc(
    UUID patientId,
    Pageable p
  );
  long countByPatientId(UUID patientId);
}
