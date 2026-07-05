package com.synapse.hea.prescription;

import com.synapse.hea.common.api.PageResponse;
import com.synapse.hea.common.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/prescriptions")
public class PrescriptionController {
  private final PrescriptionService service;
  private final CurrentUser current;

  public PrescriptionController(PrescriptionService service, CurrentUser current) {
    this.service = service;
    this.current = current;
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
  public PrescriptionService.View create(Authentication a,
      @Valid @RequestBody PrescriptionService.CreateRequest q) {
    return service.create(current.id(a), q);
  }

  @GetMapping
  public PageResponse<PrescriptionService.View> list(Authentication a,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.list(current.id(a), page, size);
  }

  @GetMapping("/{id}")
  public PrescriptionService.View get(Authentication a, @PathVariable UUID id) {
    return service.get(current.id(a), id);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
  public PrescriptionService.View status(Authentication a, @PathVariable UUID id,
      @Valid @RequestBody PrescriptionService.StatusRequest q) {
    return service.transition(current.id(a), id, q);
  }

  @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> pdf(Authentication a, @PathVariable UUID id) {
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=prescription-" + id + ".pdf")
      .body(service.pdf(current.id(a), id));
  }
}
