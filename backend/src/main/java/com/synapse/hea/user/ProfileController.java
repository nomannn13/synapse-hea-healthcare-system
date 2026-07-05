package com.synapse.hea.user;

import com.synapse.hea.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

  private final ProfileService s;
  private final CurrentUser c;

  public ProfileController(ProfileService s, CurrentUser c) {
    this.s = s;
    this.c = c;
  }

  @GetMapping("/me")
  public ProfileService.ProfileView me(Authentication a) {
    return s.get(c.id(a));
  }

  @PatchMapping("/me")
  public ProfileService.ProfileView update(
    Authentication a,
    @Valid @RequestBody ProfileService.UpdateRequest q
  ) {
    return s.update(c.id(a), q);
  }

  @PatchMapping("/me/doctor")
  public ProfileService.ProfileView doctor(
    Authentication a,
    @Valid @RequestBody ProfileService.DoctorUpdate q
  ) {
    return s.updateDoctor(c.id(a), q);
  }
}
