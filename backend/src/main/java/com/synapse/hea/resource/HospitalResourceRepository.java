package com.synapse.hea.resource;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalResourceRepository
  extends JpaRepository<HospitalResource, UUID>
{
  Page<HospitalResource> findAllByOrderByNameAsc(Pageable p);
  long countByStatus(HospitalResource.Status status);
}
