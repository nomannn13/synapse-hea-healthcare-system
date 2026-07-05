package com.synapse.hea.document;

import com.synapse.hea.common.model.BaseEntity;
import com.synapse.hea.patient.PatientProfile;
import com.synapse.hea.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "clinical_documents", indexes = @Index(name = "idx_document_patient_created", columnList = "patient_id,created_at"))
public class ClinicalDocument extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "patient_id", nullable = false)
  private PatientProfile patient;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "uploaded_by", nullable = false)
  private User uploadedBy;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private DocumentCategory category;

  @Column(name = "original_name", nullable = false, length = 255)
  private String originalName;

  @Column(name = "stored_name", nullable = false, unique = true, length = 120)
  private String storedName;

  @Column(name = "content_type", nullable = false, length = 120)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(length = 700)
  private String notes;

  protected ClinicalDocument() {}

  public ClinicalDocument(PatientProfile patient, User uploadedBy, DocumentCategory category,
      String originalName, String storedName, String contentType, long sizeBytes, String notes) {
    this.patient = patient;
    this.uploadedBy = uploadedBy;
    this.category = category;
    this.originalName = originalName;
    this.storedName = storedName;
    this.contentType = contentType;
    this.sizeBytes = sizeBytes;
    this.notes = notes;
  }

  public PatientProfile getPatient() { return patient; }
  public User getUploadedBy() { return uploadedBy; }
  public DocumentCategory getCategory() { return category; }
  public String getOriginalName() { return originalName; }
  public String getStoredName() { return storedName; }
  public String getContentType() { return contentType; }
  public long getSizeBytes() { return sizeBytes; }
  public String getNotes() { return notes; }
}
