package com.synapse.hea.notification;

import com.synapse.hea.common.model.BaseEntity;
import com.synapse.hea.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
  name = "notifications",
  indexes = @Index(
    name = "idx_notification_user_read",
    columnList = "user_id,read_at"
  )
)
public class Notification extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, length = 140)
  private String title;

  @Column(nullable = false, length = 1000)
  private String message;

  @Column(nullable = false, length = 40)
  private String type;

  @Column(name = "read_at")
  private Instant readAt;

  protected Notification() {}

  public Notification(User user, String title, String message, String type) {
    this.user = user;
    this.title = title;
    this.message = message;
    this.type = type;
  }

  public User getUser() {
    return user;
  }

  public String getTitle() {
    return title;
  }

  public String getMessage() {
    return message;
  }

  public String getType() {
    return type;
  }

  public Instant getReadAt() {
    return readAt;
  }

  public void markRead() {
    if (readAt == null) readAt = Instant.now();
  }
}
