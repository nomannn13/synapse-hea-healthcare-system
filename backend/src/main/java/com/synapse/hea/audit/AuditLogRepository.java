package com.synapse.hea.audit;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
  Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

  @Query("""
    select a from AuditLog a
    where (:action is null or a.action = :action)
      and (:entityType is null or lower(a.entityType) = lower(:entityType))
      and (:actorId is null or a.actorId = :actorId)
    order by a.createdAt desc
  """)
  Page<AuditLog> search(@Param("action") String action, @Param("entityType") String entityType,
    @Param("actorId") UUID actorId, Pageable pageable);
}
