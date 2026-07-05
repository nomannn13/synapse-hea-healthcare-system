package com.synapse.hea.notification;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class NotificationStreamService {

  private final ConcurrentHashMap<
    UUID,
    java.util.concurrent.CopyOnWriteArrayList<SseEmitter>
  > emitters = new ConcurrentHashMap<>();

  public SseEmitter subscribe(UUID userId) {
    SseEmitter e = new SseEmitter(30 * 60 * 1000L);
    emitters
      .computeIfAbsent(userId, k ->
        new java.util.concurrent.CopyOnWriteArrayList<>()
      )
      .add(e);
    Runnable cleanup = () ->
      emitters
        .getOrDefault(userId, new java.util.concurrent.CopyOnWriteArrayList<>())
        .remove(e);
    e.onCompletion(cleanup);
    e.onTimeout(cleanup);
    e.onError(x -> cleanup.run());
    return e;
  }

  public void publish(UUID userId, Object payload) {
    for (SseEmitter e : emitters.getOrDefault(
      userId,
      new java.util.concurrent.CopyOnWriteArrayList<>()
    )) {
      try {
        e.send(SseEmitter.event().name("notification").data(payload));
      } catch (IOException ex) {
        e.complete();
      }
    }
  }
}
