package com.synapse.hea.prescription;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {
  Page<Prescription> findByPatientIdOrderByCreatedAtDesc(UUID patientId, Pageable pageable);
  Page<Prescription> findByDoctorIdOrderByCreatedAtDesc(UUID doctorId, Pageable pageable);
  long countByPatientId(UUID patientId);
  long countByDoctorId(UUID doctorId);
}
