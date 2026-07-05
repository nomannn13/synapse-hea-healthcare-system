package com.synapse.hea.dashboard;

import com.synapse.hea.common.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

  private final DashboardService s;
  private final CurrentUser c;

  public DashboardController(DashboardService s, CurrentUser c) {
    this.s = s;
    this.c = c;
  }

  @GetMapping
  public Object dashboard(Authentication a) {
    return s.get(c.id(a));
  }
}
