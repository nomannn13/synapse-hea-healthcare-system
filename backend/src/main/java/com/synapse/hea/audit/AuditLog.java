package com.synapse.hea.audit;

import com.synapse.hea.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(
  name = "audit_logs",
  indexes = @Index(
    name = "idx_audit_actor_time",
    columnList = "actor_id,created_at"
  )
)
public class AuditLog extends BaseEntity {

  @Column(name = "actor_id")
  private UUID actorId;

  @Column(nullable = false, length = 30)
  private String action;

  @Column(nullable = false, length = 80)
  private String entityType;

  @Column(name = "entity_id")
  private UUID entityId;

  @Column(length = 1000)
  private String details;

  protected AuditLog() {}

  public AuditLog(
    UUID actorId,
    String action,
    String entityType,
    UUID entityId,
    String details
  ) {
    this.actorId = actorId;
    this.action = action;
    this.entityType = entityType;
    this.entityId = entityId;
    this.details = details;
  }

  public UUID getActorId() {
    return actorId;
  }

  public String getAction() {
    return action;
  }

  public String getEntityType() {
    return entityType;
  }

  public UUID getEntityId() {
    return entityId;
  }

  public String getDetails() {
    return details;
  }
}
