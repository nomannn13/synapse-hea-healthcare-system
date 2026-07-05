package com.synapse.hea.admin;

import com.synapse.hea.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/doctors")
public class AdminAccountController {

  private final AdminAccountService s;
  private final CurrentUser c;

  public AdminAccountController(AdminAccountService s, CurrentUser c) {
    this.s = s;
    this.c = c;
  }

  @PostMapping
  public AdminAccountService.DoctorView create(
    Authentication a,
    @Valid @RequestBody AdminAccountService.CreateDoctor q
  ) {
    return s.createDoctor(c.id(a), q);
  }
}
