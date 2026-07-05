package com.synapse.hea.urgent;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UrgentCaseRepository extends JpaRepository<UrgentCase, UUID> {
  Page<UrgentCase> findAllByOrderByCreatedAtDesc(Pageable p);
  long countByStatus(UrgentCase.Status status);
}
