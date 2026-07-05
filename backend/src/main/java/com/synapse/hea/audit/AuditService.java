package com.synapse.hea.audit;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

  private final AuditLogRepository repo;

  public AuditService(AuditLogRepository repo) {
    this.repo = repo;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(
    UUID actor,
    String action,
    String entity,
    UUID entityId,
    String details
  ) {
    repo.save(new AuditLog(actor, action, entity, entityId, details));
  }
}
