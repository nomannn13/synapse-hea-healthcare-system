package com.synapse.hea.notification;

import com.synapse.hea.common.security.CurrentUser;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

  private final NotificationService service;
  private final NotificationStreamService streams;
  private final CurrentUser current;

  public NotificationController(
    NotificationService service,
    NotificationStreamService streams,
    CurrentUser current
  ) {
    this.service = service;
    this.streams = streams;
    this.current = current;
  }

  @GetMapping
  public List<NotificationService.NotificationView> list(
    Authentication a,
    @RequestParam(defaultValue = "30") int limit
  ) {
    return service.list(current.id(a), limit);
  }

  @GetMapping("/unread-count")
  public Map<String, Long> unread(Authentication a) {
    return Map.of("count", service.unread(current.id(a)));
  }

  @PatchMapping("/{id}/read")
  public NotificationService.NotificationView read(
    Authentication a,
    @PathVariable UUID id
  ) {
    return service.markRead(current.id(a), id);
  }

  @GetMapping(path = "/stream", produces = "text/event-stream")
  public SseEmitter stream(Authentication a) {
    return streams.subscribe(current.id(a));
  }
}
