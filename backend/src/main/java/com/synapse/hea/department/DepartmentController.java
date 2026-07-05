package com.synapse.hea.department;

import com.synapse.hea.common.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {
  private final DepartmentService service;
  private final CurrentUser current;

  public DepartmentController(DepartmentService service, CurrentUser current) {
    this.service = service;
    this.current = current;
  }

  @GetMapping
  public List<DepartmentService.View> list() { return service.list(); }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public DepartmentService.View create(Authentication a, @Valid @RequestBody DepartmentService.CreateRequest q) {
    return service.create(current.id(a), q);
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public DepartmentService.View update(Authentication a, @PathVariable UUID id,
      @Valid @RequestBody DepartmentService.UpdateRequest q) {
    return service.update(current.id(a), id, q);
  }
}
