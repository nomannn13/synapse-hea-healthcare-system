package com.synapse.hea.doctor;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DoctorProfileRepository
  extends JpaRepository<DoctorProfile, UUID>
{
  Optional<DoctorProfile> findByUserId(UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from DoctorProfile d where d.id = :id")
  Optional<DoctorProfile> lockById(@Param("id") UUID id);

  @Query(
    """
    select d from DoctorProfile d join d.user u join d.department dep
    where d.active = true and (:query is null or lower(u.firstName) like lower(concat('%',:query,'%'))
      or lower(u.lastName) like lower(concat('%',:query,'%'))
      or lower(d.specialization) like lower(concat('%',:query,'%')))
      and (:departmentId is null or dep.id = :departmentId)
    """
  )
  Page<DoctorProfile> search(
    @Param("query") String query,
    @Param("departmentId") UUID departmentId,
    Pageable pageable
  );
}
