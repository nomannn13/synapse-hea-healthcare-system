package com.synapse.hea.medical;

import com.synapse.hea.common.api.PageResponse;
import com.synapse.hea.common.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/medical-records")
public class MedicalRecordController {

  private final MedicalRecordService service;
  private final CurrentUser current;

  public MedicalRecordController(MedicalRecordService s, CurrentUser c) {
    service = s;
    current = c;
  }

  @PostMapping
  public MedicalRecordService.View create(
    Authentication a,
    @Valid @RequestBody MedicalRecordService.CreateRequest q
  ) {
    return service.create(current.id(a), q);
  }

  @GetMapping
  public PageResponse<MedicalRecordService.View> list(
    Authentication a,
    @RequestParam(required = false) UUID patientId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return service.mine(current.id(a), patientId, page, size);
  }

  @PatchMapping("/{id}")
  public MedicalRecordService.View update(
    Authentication a,
    @PathVariable UUID id,
    @Valid @RequestBody MedicalRecordService.UpdateRequest q
  ) {
    return service.update(current.id(a), id, q);
  }
}
