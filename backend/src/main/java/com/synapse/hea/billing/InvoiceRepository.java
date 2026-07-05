package com.synapse.hea.billing;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
  Page<Invoice> findByPatientIdOrderByCreatedAtDesc(UUID patientId, Pageable pageable);
  long countByPatientId(UUID patientId);
  long countByStatus(InvoiceStatus status);

  @Query("select sum(i.totalAmount) from Invoice i where i.status = com.synapse.hea.billing.InvoiceStatus.PAID")
  BigDecimal totalPaidRevenueValue();

  default BigDecimal totalPaidRevenue() {
    return Optional.ofNullable(totalPaidRevenueValue()).orElse(BigDecimal.ZERO);
  }
}
