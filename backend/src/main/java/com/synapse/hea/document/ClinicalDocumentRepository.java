package com.synapse.hea.document;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalDocumentRepository extends JpaRepository<ClinicalDocument, UUID> {
  Page<ClinicalDocument> findByPatientIdOrderByCreatedAtDesc(UUID patientId, Pageable pageable);
  long countByPatientId(UUID patientId);
}
