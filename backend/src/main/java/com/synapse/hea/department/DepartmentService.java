package com.synapse.hea.department;

import com.synapse.hea.audit.AuditService;
import com.synapse.hea.common.exception.*;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentService {
  private final DepartmentRepository departments;
  private final AuditService audit;

  public DepartmentService(DepartmentRepository departments, AuditService audit) {
    this.departments = departments;
    this.audit = audit;
  }

  @Transactional(readOnly = true)
  @Cacheable("departments")
  public List<View> list() {
    return departments.findByActiveTrueOrderByNameAsc().stream().map(this::view).toList();
  }

  @Transactional
  @CacheEvict(value = "departments", allEntries = true)
  public View create(UUID actorId, CreateRequest q) {
    if (departments.existsByCodeIgnoreCase(q.code())) throw new ConflictException("Department code already exists");
    if (departments.existsByNameIgnoreCase(q.name())) throw new ConflictException("Department name already exists");
    Department saved = departments.save(new Department(q.code().trim().toUpperCase(), q.name().trim(), q.description()));
    audit.record(actorId, "CREATE", "Department", saved.getId(), saved.getCode());
    return view(saved);
  }

  @Transactional
  @CacheEvict(value = "departments", allEntries = true)
  public View update(UUID actorId, UUID id, UpdateRequest q) {
    Department d = departments.findById(id).orElseThrow(() -> new NotFoundException("Department not found"));
    if (departments.existsByNameIgnoreCaseAndIdNot(q.name(), id))
      throw new ConflictException("Department name already exists");
    d.update(q.name(), q.description(), q.active());
    audit.record(actorId, "UPDATE", "Department", id, "Department details updated");
    return view(d);
  }

  private View view(Department d) {
    return new View(d.getId(), d.getCode(), d.getName(), d.getDescription(), d.isActive());
  }

  public record CreateRequest(
    @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Za-z0-9_-]+") String code,
    @NotBlank @Size(max = 120) String name,
    @Size(max = 500) String description
  ) {}
  public record UpdateRequest(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 500) String description,
    boolean active
  ) {}
  public record View(UUID id, String code, String name, String description, boolean active) {}
}
