package com.synapse.hea.appointment;

import com.synapse.hea.common.model.BaseEntity;
import com.synapse.hea.department.Department;
import com.synapse.hea.doctor.DoctorProfile;
import com.synapse.hea.patient.PatientProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
  name = "appointments",
  indexes = {
    @Index(
      name = "idx_appointment_doctor_time",
      columnList = "doctor_id,start_at,end_at,status"
    ),
    @Index(
      name = "idx_appointment_patient_time",
      columnList = "patient_id,start_at"
    ),
  }
)
public class Appointment extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "patient_id", nullable = false)
  private PatientProfile patient;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "doctor_id", nullable = false)
  private DoctorProfile doctor;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "department_id", nullable = false)
  private Department department;

  @Column(name = "start_at", nullable = false)
  private Instant startAt;

  @Column(name = "end_at", nullable = false)
  private Instant endAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AppointmentStatus status = AppointmentStatus.PENDING;

  @Column(nullable = false, length = 500)
  private String reason;

  @Column(length = 1000)
  private String notes;

  @Column(length = 500)
  private String cancellationReason;

  protected Appointment() {}

  public Appointment(
    PatientProfile patient,
    DoctorProfile doctor,
    Instant startAt,
    Instant endAt,
    String reason
  ) {
    this.patient = patient;
    this.doctor = doctor;
    this.department = doctor.getDepartment();
    this.startAt = startAt;
    this.endAt = endAt;
    this.reason = reason;
  }

  public PatientProfile getPatient() {
    return patient;
  }

  public DoctorProfile getDoctor() {
    return doctor;
  }

  public Department getDepartment() {
    return department;
  }

  public Instant getStartAt() {
    return startAt;
  }

  public Instant getEndAt() {
    return endAt;
  }

  public AppointmentStatus getStatus() {
    return status;
  }

  public String getReason() {
    return reason;
  }

  public String getNotes() {
    return notes;
  }

  public String getCancellationReason() {
    return cancellationReason;
  }

  public void reschedule(Instant startAt, Instant endAt) {
    this.startAt = startAt;
    this.endAt = endAt;
    this.status = AppointmentStatus.PENDING;
  }

  public void cancel(String reason) {
    this.status = AppointmentStatus.CANCELLED;
    this.cancellationReason = reason;
  }

  public void updateStatus(AppointmentStatus status, String notes) {
    this.status = status;
    this.notes = notes;
  }
}
