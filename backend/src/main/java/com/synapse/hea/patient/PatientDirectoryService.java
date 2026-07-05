package com.synapse.hea.patient;

import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientDirectoryService {

  private final PatientProfileRepository patients;

  public PatientDirectoryService(PatientProfileRepository p) {
    patients = p;
  }

  @Transactional(readOnly = true)
  public com.synapse.hea.common.api.PageResponse<View> search(
    String query,
    int page,
    int size
  ) {
    String q = query == null || query.isBlank() ? null : query.trim();
    return com.synapse.hea.common.api.PageResponse.from(
      patients
        .search(q, PageRequest.of(page, Math.min(size, 50)))
        .map(p ->
          new View(
            p.getId(),
            p.getUser().getDisplayName(),
            p.getUser().getEmail(),
            p.getUser().getPhone(),
            p.getDateOfBirth(),
            p.getBloodGroup()
          )
        )
    );
  }

  public record View(
    UUID id,
    String name,
    String email,
    String phone,
    java.time.LocalDate dateOfBirth,
    String bloodGroup
  ) {}
}
