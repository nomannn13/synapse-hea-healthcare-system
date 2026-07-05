package com.synapse.hea.doctor;

import com.synapse.hea.common.model.BaseEntity;
import com.synapse.hea.department.Department;
import com.synapse.hea.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "doctor_profiles")
public class DoctorProfile extends BaseEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(nullable = false, unique = true, length = 80)
  private String licenseNumber;

  @Column(nullable = false, length = 120)
  private String specialization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "department_id", nullable = false)
  private Department department;

  @Column(length = 1500)
  private String biography;

  @Column(nullable = false)
  private int consultationMinutes = 30;

  private boolean active = true;

  protected DoctorProfile() {}

  public DoctorProfile(
    User user,
    String licenseNumber,
    String specialization,
    Department department
  ) {
    this.user = user;
    this.licenseNumber = licenseNumber;
    this.specialization = specialization;
    this.department = department;
  }

  public User getUser() {
    return user;
  }

  public String getLicenseNumber() {
    return licenseNumber;
  }

  public String getSpecialization() {
    return specialization;
  }

  public Department getDepartment() {
    return department;
  }

  public String getBiography() {
    return biography;
  }

  public int getConsultationMinutes() {
    return consultationMinutes;
  }

  public boolean isActive() {
    return active;
  }

  public void updateProfessional(
    String specialization,
    Department department,
    String biography,
    int minutes
  ) {
    this.specialization = specialization;
    this.department = department;
    this.biography = biography;
    this.consultationMinutes = minutes;
  }
}
