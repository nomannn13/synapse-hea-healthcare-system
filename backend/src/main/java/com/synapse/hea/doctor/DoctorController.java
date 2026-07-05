package com.synapse.hea.doctor;

import com.synapse.hea.common.api.PageResponse;
import com.synapse.hea.common.security.CurrentUser;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorController {

  private final DoctorService s;
  private final CurrentUser c;

  public DoctorController(DoctorService s, CurrentUser c) {
    this.s = s;
    this.c = c;
  }

  @GetMapping
  public PageResponse<DoctorService.DoctorView> list(
    @RequestParam(required = false) String query,
    @RequestParam(required = false) UUID departmentId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return s.search(query, departmentId, page, size);
  }

  @GetMapping("/{id}/availability")
  public List<DoctorService.SlotView> availability(
    @PathVariable UUID id,
    @RequestParam Instant from,
    @RequestParam Instant to
  ) {
    return s.availability(id, from, to);
  }

  @GetMapping("/me/availability")
  @PreAuthorize("hasRole('DOCTOR')")
  public List<DoctorService.SlotView> myAvailability(Authentication a) {
    return s.mySlots(c.id(a));
  }

  @PostMapping("/me/availability")
  @PreAuthorize("hasRole('DOCTOR')")
  public DoctorService.SlotView slot(
    Authentication a,
    @Valid @RequestBody DoctorService.SlotRequest q
  ) {
    return s.addSlot(c.id(a), q);
  }

  @DeleteMapping("/me/availability/{id}")
  @PreAuthorize("hasRole('DOCTOR')")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void deleteSlot(Authentication a, @PathVariable UUID id) {
    s.deactivateSlot(c.id(a), id);
  }
}
