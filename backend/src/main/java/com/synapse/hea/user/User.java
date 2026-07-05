package com.synapse.hea.user;

import com.synapse.hea.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
  name = "users",
  indexes = @Index(name = "idx_users_role_status", columnList = "role,status")
)
public class User extends BaseEntity {

  @Column(nullable = false, unique = true, length = 190)
  private String email;

  @Column(nullable = false, length = 100)
  private String passwordHash;

  @Column(nullable = false, length = 80)
  private String firstName;

  @Column(nullable = false, length = 80)
  private String lastName;

  @Column(length = 30)
  private String phone;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserStatus status = UserStatus.ACTIVE;

  protected User() {}

  public User(
    String email,
    String passwordHash,
    String firstName,
    String lastName,
    String phone,
    Role role
  ) {
    this.email = email.toLowerCase().trim();
    this.passwordHash = passwordHash;
    this.firstName = firstName.trim();
    this.lastName = lastName.trim();
    this.phone = phone;
    this.role = role;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getPhone() {
    return phone;
  }

  public Role getRole() {
    return role;
  }

  public UserStatus getStatus() {
    return status;
  }

  public String getDisplayName() {
    return firstName + " " + lastName;
  }

  public void updateProfile(String firstName, String lastName, String phone) {
    this.firstName = firstName.trim();
    this.lastName = lastName.trim();
    this.phone = phone;
  }

  public void changePassword(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public void changeStatus(UserStatus status) {
    this.status = status;
  }
}
