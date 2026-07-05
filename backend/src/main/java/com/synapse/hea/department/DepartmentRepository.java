package com.synapse.hea.department;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
  List<Department> findByActiveTrueOrderByNameAsc();
  boolean existsByCodeIgnoreCase(String code);
  boolean existsByNameIgnoreCase(String name);
  boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
  Optional<Department> findByCodeIgnoreCase(String code);
}
