package com.synapse.hea.urgent;

import com.synapse.hea.common.model.BaseEntity;
import com.synapse.hea.patient.PatientProfile;
import com.synapse.hea.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "urgent_cases")
public class UrgentCase extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "patient_id", nullable = false)
  private PatientProfile patient;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Priority priority;

  private Integer heartRate;
  private Integer oxygenSaturation;
  private Integer systolicPressure;
  private Double temperature;
  private Integer symptomSeverity;

  @Column(nullable = false, length = 1000)
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.OPEN;

  protected UrgentCase() {}

  public UrgentCase(
    PatientProfile p,
    User u,
    Priority pr,
    Integer hr,
    Integer oxy,
    Integer sys,
    Double temp,
    Integer sev,
    String reason
  ) {
    patient = p;
    createdBy = u;
    priority = pr;
    heartRate = hr;
    oxygenSaturation = oxy;
    systolicPressure = sys;
    temperature = temp;
    symptomSeverity = sev;
    this.reason = reason;
  }

  public PatientProfile getPatient() {
    return patient;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public Priority getPriority() {
    return priority;
  }

  public Integer getHeartRate() {
    return heartRate;
  }

  public Integer getOxygenSaturation() {
    return oxygenSaturation;
  }

  public Integer getSystolicPressure() {
    return systolicPressure;
  }

  public Double getTemperature() {
    return temperature;
  }

  public Integer getSymptomSeverity() {
    return symptomSeverity;
  }

  public String getReason() {
    return reason;
  }

  public Status getStatus() {
    return status;
  }

  public enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
  }

  public enum Status {
    OPEN,
    ACKNOWLEDGED,
    CLOSED,
  }
}
