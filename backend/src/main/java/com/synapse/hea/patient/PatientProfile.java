package com.synapse.hea.patient;

import com.synapse.hea.common.model.BaseEntity;
import com.synapse.hea.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "patient_profiles")
public class PatientProfile extends BaseEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  private LocalDate dateOfBirth;

  @Column(length = 10)
  private String bloodGroup;

  @Column(length = 1000)
  private String allergies;

  @Column(length = 120)
  private String emergencyContact;

  @Column(length = 500)
  private String address;

  protected PatientProfile() {}

  public PatientProfile(User user, LocalDate dateOfBirth) {
    this.user = user;
    this.dateOfBirth = dateOfBirth;
  }

  public User getUser() {
    return user;
  }

  public LocalDate getDateOfBirth() {
    return dateOfBirth;
  }

  public String getBloodGroup() {
    return bloodGroup;
  }

  public String getAllergies() {
    return allergies;
  }

  public String getEmergencyContact() {
    return emergencyContact;
  }

  public String getAddress() {
    return address;
  }

  public void update(
    LocalDate dateOfBirth,
    String bloodGroup,
    String allergies,
    String emergencyContact,
    String address
  ) {
    this.dateOfBirth = dateOfBirth;
    this.bloodGroup = bloodGroup;
    this.allergies = allergies;
    this.emergencyContact = emergencyContact;
    this.address = address;
  }
}
