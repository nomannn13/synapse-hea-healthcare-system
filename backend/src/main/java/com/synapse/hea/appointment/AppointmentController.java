package com.synapse.hea.appointment;

import static com.synapse.hea.appointment.AppointmentDtos.*;

import com.synapse.hea.common.api.PageResponse;
import com.synapse.hea.common.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

  private final AppointmentService service;
  private final CurrentUser current;

  public AppointmentController(
    AppointmentService service,
    CurrentUser current
  ) {
    this.service = service;
    this.current = current;
  }

  @PostMapping
  public View create(
    Authentication auth,
    @Valid @RequestBody CreateRequest request
  ) {
    return service.create(current.id(auth), request);
  }

  @GetMapping
  public PageResponse<View> mine(
    Authentication auth,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return service.mine(current.id(auth), page, size);
  }

  @PatchMapping("/{id}/reschedule")
  public View reschedule(
    Authentication auth,
    @PathVariable UUID id,
    @Valid @RequestBody RescheduleRequest request
  ) {
    return service.reschedule(current.id(auth), id, request);
  }

  @PatchMapping("/{id}/cancel")
  public View cancel(
    Authentication auth,
    @PathVariable UUID id,
    @Valid @RequestBody CancelRequest request
  ) {
    return service.cancel(current.id(auth), id, request);
  }

  @PatchMapping("/{id}/status")
  public View status(
    Authentication auth,
    @PathVariable UUID id,
    @Valid @RequestBody StatusRequest request
  ) {
    return service.updateStatus(current.id(auth), id, request);
  }
}
