package com.synapse.hea.doctor;

import com.synapse.hea.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
  name = "availability_slots",
  indexes = @Index(
    name = "idx_slot_doctor_time",
    columnList = "doctor_id,start_at,end_at"
  )
)
public class AvailabilitySlot extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "doctor_id", nullable = false)
  private DoctorProfile doctor;

  @Column(name = "start_at", nullable = false)
  private Instant startAt;

  @Column(name = "end_at", nullable = false)
  private Instant endAt;

  private boolean active = true;

  protected AvailabilitySlot() {}

  public AvailabilitySlot(
    DoctorProfile doctor,
    Instant startAt,
    Instant endAt
  ) {
    this.doctor = doctor;
    this.startAt = startAt;
    this.endAt = endAt;
  }

  public DoctorProfile getDoctor() {
    return doctor;
  }

  public Instant getStartAt() {
    return startAt;
  }

  public Instant getEndAt() {
    return endAt;
  }

  public boolean isActive() {
    return active;
  }

  public void deactivate() {
    this.active = false;
  }
}
