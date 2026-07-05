package com.synapse.hea.medical;

import com.synapse.hea.appointment.Appointment;
import com.synapse.hea.common.model.BaseEntity;
import com.synapse.hea.doctor.DoctorProfile;
import com.synapse.hea.patient.PatientProfile;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
  name = "medical_records",
  indexes = @Index(
    name = "idx_medical_patient_time",
    columnList = "patient_id,recorded_at"
  )
)
public class MedicalRecord extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "patient_id", nullable = false)
  private PatientProfile patient;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "doctor_id", nullable = false)
  private DoctorProfile doctor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "appointment_id")
  private Appointment appointment;

  @Column(nullable = false, length = 500)
  private String diagnosis;

  @Column(length = 1500)
  private String treatment;

  @Column(length = 1500)
  private String prescription;

  @Column(length = 3000)
  private String clinicalNotes;

  @Column(name = "recorded_at", nullable = false)
  private Instant recordedAt = Instant.now();

  protected MedicalRecord() {}

  public MedicalRecord(
    PatientProfile p,
    DoctorProfile d,
    Appointment a,
    String diagnosis,
    String treatment,
    String prescription,
    String notes
  ) {
    patient = p;
    doctor = d;
    appointment = a;
    this.diagnosis = diagnosis;
    this.treatment = treatment;
    this.prescription = prescription;
    clinicalNotes = notes;
  }

  public PatientProfile getPatient() {
    return patient;
  }

  public DoctorProfile getDoctor() {
    return doctor;
  }

  public Appointment getAppointment() {
    return appointment;
  }

  public String getDiagnosis() {
    return diagnosis;
  }

  public String getTreatment() {
    return treatment;
  }

  public String getPrescription() {
    return prescription;
  }

  public String getClinicalNotes() {
    return clinicalNotes;
  }

  public Instant getRecordedAt() {
    return recordedAt;
  }

  public void update(
    String diagnosis,
    String treatment,
    String prescription,
    String notes
  ) {
    this.diagnosis = diagnosis;
    this.treatment = treatment;
    this.prescription = prescription;
    this.clinicalNotes = notes;
  }
}
