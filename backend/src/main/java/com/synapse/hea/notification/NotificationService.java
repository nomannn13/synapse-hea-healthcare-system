package com.synapse.hea.notification;

import com.synapse.hea.auth.MailGateway;
import com.synapse.hea.common.exception.ForbiddenOperationException;
import com.synapse.hea.common.exception.NotFoundException;
import com.synapse.hea.user.User;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

  private final NotificationRepository repo;
  private final NotificationStreamService streams;
  private final MailGateway mail;

  public NotificationService(
    NotificationRepository repo,
    NotificationStreamService streams,
    MailGateway mail
  ) {
    this.repo = repo;
    this.streams = streams;
    this.mail = mail;
  }

  @Transactional
  public NotificationView create(
    User user,
    String title,
    String message,
    String type
  ) {
    Notification n = repo.save(new Notification(user, title, message, type));
    NotificationView v = view(n);
    streams.publish(user.getId(), v);
    mail.sendNotification(user.getEmail(), user.getDisplayName(), title, message);
    return v;
  }

  @Transactional(readOnly = true)
  public java.util.List<NotificationView> list(UUID userId, int limit) {
    return repo
      .findByUserIdOrderByCreatedAtDesc(
        userId,
        PageRequest.of(0, Math.min(limit, 100))
      )
      .map(this::view)
      .getContent();
  }

  @Transactional
  public NotificationView markRead(UUID userId, UUID id) {
    Notification n = repo
      .findById(id)
      .orElseThrow(() -> new NotFoundException("Notification not found"));
    if (
      !n.getUser().getId().equals(userId)
    ) throw new ForbiddenOperationException("Not your notification");
    n.markRead();
    return view(n);
  }

  public long unread(UUID userId) {
    return repo.countByUserIdAndReadAtIsNull(userId);
  }

  public NotificationView view(Notification n) {
    return new NotificationView(
      n.getId(),
      n.getTitle(),
      n.getMessage(),
      n.getType(),
      n.getCreatedAt(),
      n.getReadAt()
    );
  }

  public record NotificationView(
    UUID id,
    String title,
    String message,
    String type,
    java.time.Instant createdAt,
    java.time.Instant readAt
  ) {}
}
