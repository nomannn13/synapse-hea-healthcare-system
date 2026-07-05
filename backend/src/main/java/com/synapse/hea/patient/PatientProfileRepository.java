package com.synapse.hea.patient;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientProfileRepository
  extends JpaRepository<PatientProfile, UUID>
{
  Optional<PatientProfile> findByUserId(UUID userId);

  @Query(
    "select p from PatientProfile p join p.user u where :query is null or lower(u.firstName) like lower(concat('%',:query,'%')) or lower(u.lastName) like lower(concat('%',:query,'%')) or lower(u.email) like lower(concat('%',:query,'%'))"
  )
  Page<PatientProfile> search(@Param("query") String query, Pageable pageable);
}
