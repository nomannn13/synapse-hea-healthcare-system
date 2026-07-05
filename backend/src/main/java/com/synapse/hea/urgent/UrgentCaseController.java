package com.synapse.hea.urgent;

import com.synapse.hea.common.api.PageResponse;
import com.synapse.hea.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/urgent-cases")
public class UrgentCaseController {

  private final UrgentCaseService s;
  private final CurrentUser c;

  public UrgentCaseController(UrgentCaseService s, CurrentUser c) {
    this.s = s;
    this.c = c;
  }

  @PostMapping
  public UrgentCaseService.View create(
    Authentication a,
    @Valid @RequestBody UrgentCaseService.CreateRequest q
  ) {
    return s.create(c.id(a), q);
  }

  @GetMapping
  public PageResponse<UrgentCaseService.View> list(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return s.list(page, size);
  }
}
