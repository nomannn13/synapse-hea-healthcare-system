package com.synapse.hea.department;

import com.synapse.hea.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "departments")
public class Department extends BaseEntity {

  @Column(nullable = false, unique = true, length = 30)
  private String code;

  @Column(nullable = false, unique = true, length = 120)
  private String name;

  @Column(length = 500)
  private String description;

  private boolean active = true;

  protected Department() {}

  public Department(String code, String name, String description) {
    this.code = code;
    this.name = name;
    this.description = description;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public boolean isActive() {
    return active;
  }

  public void update(String name, String description, boolean active) {
    this.name = name.trim();
    this.description = description;
    this.active = active;
  }
}
