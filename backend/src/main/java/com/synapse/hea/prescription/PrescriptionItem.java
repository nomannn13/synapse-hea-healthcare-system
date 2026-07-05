package com.synapse.hea.prescription;

import com.synapse.hea.common.model.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "prescription_items")
public class PrescriptionItem extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prescription_id", nullable = false)
  private Prescription prescription;

  @Column(name = "medication_name", nullable = false, length = 180)
  private String medicationName;

  @Column(nullable = false, length = 120)
  private String dosage;

  @Column(nullable = false, length = 120)
  private String frequency;

  @Column(name = "duration_days")
  private Integer durationDays;

  @Column(length = 500)
  private String instructions;

  protected PrescriptionItem() {}

  PrescriptionItem(Prescription prescription, String medicationName, String dosage,
                   String frequency, Integer durationDays, String instructions) {
    this.prescription = prescription;
    this.medicationName = medicationName.trim();
    this.dosage = dosage.trim();
    this.frequency = frequency.trim();
    this.durationDays = durationDays;
    this.instructions = instructions;
  }

  public String getMedicationName() { return medicationName; }
  public String getDosage() { return dosage; }
  public String getFrequency() { return frequency; }
  public Integer getDurationDays() { return durationDays; }
  public String getInstructions() { return instructions; }
}
