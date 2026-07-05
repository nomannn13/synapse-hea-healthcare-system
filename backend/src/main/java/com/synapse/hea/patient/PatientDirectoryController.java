package com.synapse.hea.patient;

import com.synapse.hea.common.api.PageResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientDirectoryController {

  private final PatientDirectoryService s;

  public PatientDirectoryController(PatientDirectoryService s) {
    this.s = s;
  }

  @GetMapping
  public PageResponse<PatientDirectoryService.View> search(
    @RequestParam(required = false) String query,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return s.search(query, page, size);
  }
}
