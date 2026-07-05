package com.synapse.hea.admin;

import com.synapse.hea.common.api.PageResponse;
import com.synapse.hea.common.security.CurrentUser;
import com.synapse.hea.user.Role;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

  private final AdminService s;
  private final CurrentUser c;

  public AdminController(AdminService s, CurrentUser c) {
    this.s = s;
    this.c = c;
  }

  @GetMapping("/users")
  public PageResponse<AdminService.UserView> users(
    @RequestParam(required = false) Role role,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return s.users(role, page, size);
  }

  @PatchMapping("/users/{id}/status")
  public AdminService.UserView status(
    Authentication a,
    @PathVariable UUID id,
    @Valid @RequestBody AdminService.StatusRequest q
  ) {
    return s.status(c.id(a), id, q);
  }

  @GetMapping("/resources")
  public PageResponse<AdminService.ResourceView> resources(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return s.resources(page, size);
  }

  @PostMapping("/resources")
  public AdminService.ResourceView resource(
    Authentication a,
    @Valid @RequestBody AdminService.CreateResource q
  ) {
    return s.createResource(c.id(a), q);
  }

  @PatchMapping("/resources/{id}")
  public AdminService.ResourceView resource(
    Authentication a,
    @PathVariable UUID id,
    @Valid @RequestBody AdminService.UpdateResource q
  ) {
    return s.updateResource(c.id(a), id, q);
  }

  @GetMapping("/audit-logs")
  public PageResponse<AdminService.AuditView> audits(
    @RequestParam(required = false) String action,
    @RequestParam(required = false) String entityType,
    @RequestParam(required = false) UUID actorId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "50") int size
  ) {
    return s.audits(action, entityType, actorId, page, size);
  }

  @GetMapping("/reports/summary")
  public AdminService.SummaryReport report() {
    return s.summary();
  }
}
