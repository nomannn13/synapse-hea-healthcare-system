package com.synapse.hea.resource;

import com.synapse.hea.common.model.BaseEntity;
import com.synapse.hea.department.Department;
import jakarta.persistence.*;

@Entity
@Table(
  name = "hospital_resources",
  indexes = @Index(
    name = "idx_resource_dept_type_status",
    columnList = "department_id,type,status"
  )
)
public class HospitalResource extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "department_id", nullable = false)
  private Department department;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private Type type;

  @Column(nullable = false, unique = true, length = 60)
  private String code;

  @Column(nullable = false, length = 140)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private Status status = Status.AVAILABLE;

  @Column(length = 1000)
  private String notes;

  protected HospitalResource() {}

  public HospitalResource(
    Department d,
    Type t,
    String code,
    String name,
    String notes
  ) {
    department = d;
    type = t;
    this.code = code;
    this.name = name;
    this.notes = notes;
  }

  public Department getDepartment() {
    return department;
  }

  public Type getType() {
    return type;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public Status getStatus() {
    return status;
  }

  public String getNotes() {
    return notes;
  }

  public void update(Status status, String notes) {
    this.status = status;
    this.notes = notes;
  }

  public enum Type {
    BED,
    ROOM,
    EQUIPMENT,
  }

  public enum Status {
    AVAILABLE,
    IN_USE,
    MAINTENANCE,
    UNAVAILABLE,
  }
}
