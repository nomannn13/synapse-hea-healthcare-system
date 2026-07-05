package com.synapse.hea.prescription;

import com.synapse.hea.appointment.Appointment;
import com.synapse.hea.common.model.BaseEntity;
import com.synapse.hea.doctor.DoctorProfile;
import com.synapse.hea.patient.PatientProfile;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prescriptions", indexes = @Index(name = "idx_prescription_patient_issued", columnList = "patient_id,issued_at"))
public class Prescription extends BaseEntity {
  @Column(name = "prescription_number", nullable = false, unique = true, length = 40)
  private String prescriptionNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "patient_id", nullable = false)
  private PatientProfile patient;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "doctor_id", nullable = false)
  private DoctorProfile doctor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "appointment_id")
  private Appointment appointment;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PrescriptionStatus status = PrescriptionStatus.DRAFT;

  @Column(length = 1000)
  private String notes;

  @Column(name = "issued_at")
  private Instant issuedAt;

  @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("createdAt asc")
  private List<PrescriptionItem> items = new ArrayList<>();

  protected Prescription() {}

  public Prescription(String number, PatientProfile patient, DoctorProfile doctor,
                      Appointment appointment, String notes) {
    prescriptionNumber = number;
    this.patient = patient;
    this.doctor = doctor;
    this.appointment = appointment;
    this.notes = notes;
  }

  public void addItem(String medicationName, String dosage, String frequency,
                      Integer durationDays, String instructions) {
    items.add(new PrescriptionItem(this, medicationName, dosage, frequency, durationDays, instructions));
  }

  public void issue() {
    if (status != PrescriptionStatus.DRAFT) throw new IllegalStateException("Only draft prescriptions can be issued");
    status = PrescriptionStatus.ISSUED;
    issuedAt = Instant.now();
  }

  public void cancel() {
    if (status == PrescriptionStatus.CANCELLED) throw new IllegalStateException("Prescription is already cancelled");
    status = PrescriptionStatus.CANCELLED;
  }

  public String getPrescriptionNumber() { return prescriptionNumber; }
  public PatientProfile getPatient() { return patient; }
  public DoctorProfile getDoctor() { return doctor; }
  public Appointment getAppointment() { return appointment; }
  public PrescriptionStatus getStatus() { return status; }
  public String getNotes() { return notes; }
  public Instant getIssuedAt() { return issuedAt; }
  public List<PrescriptionItem> getItems() { return List.copyOf(items); }
}
