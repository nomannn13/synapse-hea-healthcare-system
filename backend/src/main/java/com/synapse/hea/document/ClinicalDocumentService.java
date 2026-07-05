package com.synapse.hea.document;

import com.synapse.hea.audit.AuditService;
import com.synapse.hea.common.api.PageResponse;
import com.synapse.hea.common.exception.*;
import com.synapse.hea.notification.NotificationService;
import com.synapse.hea.patient.*;
import com.synapse.hea.user.*;
import java.nio.file.Paths;
import java.util.*;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ClinicalDocumentService {
  private static final Set<String> ALLOWED_TYPES = Set.of(
    "application/pdf", "image/jpeg", "image/png", "text/plain"
  );

  private final ClinicalDocumentRepository documents;
  private final PatientProfileRepository patients;
  private final UserRepository users;
  private final FileStorageService storage;
  private final NotificationService notifications;
  private final AuditService audit;

  public ClinicalDocumentService(ClinicalDocumentRepository documents, PatientProfileRepository patients,
      UserRepository users, FileStorageService storage, NotificationService notifications, AuditService audit) {
    this.documents = documents;
    this.patients = patients;
    this.users = users;
    this.storage = storage;
    this.notifications = notifications;
    this.audit = audit;
  }

  @Transactional
  public View upload(UUID actorId, UUID requestedPatientId, DocumentCategory category,
      String notes, MultipartFile file) {
    User actor = users.findById(actorId).orElseThrow();
    if (file == null || file.isEmpty()) throw new ConflictException("Document file is required");
    if (notes != null && notes.length() > 700) throw new ConflictException("Document notes must be 700 characters or fewer");
    if (file.getSize() > 10 * 1024 * 1024L) throw new ConflictException("Document must be 10 MB or smaller");
    String contentType = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");
    if (!ALLOWED_TYPES.contains(contentType)) throw new ConflictException("Allowed formats: PDF, JPEG, PNG, TXT");

    PatientProfile patient;
    if (actor.getRole() == Role.PATIENT) {
      patient = patients.findByUserId(actorId).orElseThrow();
      if (requestedPatientId != null && !requestedPatientId.equals(patient.getId()))
        throw new ForbiddenOperationException("Patients can upload only to their own record");
    } else if (actor.getRole() == Role.DOCTOR || actor.getRole() == Role.ADMIN) {
      if (requestedPatientId == null) throw new ConflictException("patientId is required for staff uploads");
      patient = patients.findById(requestedPatientId).orElseThrow(() -> new NotFoundException("Patient not found"));
    } else throw new ForbiddenOperationException("Document upload is not permitted");

    String original = safeOriginalName(file.getOriginalFilename());
    String extension = extension(original);
    String stored = UUID.randomUUID() + extension;
    storage.store(stored, file);
    ClinicalDocument saved;
    try {
      saved = documents.save(new ClinicalDocument(patient, actor, category, original, stored,
        contentType, file.getSize(), notes));
    } catch (RuntimeException ex) {
      storage.delete(stored);
      throw ex;
    }
    if (!patient.getUser().getId().equals(actorId)) {
      notifications.create(patient.getUser(), "New clinical document",
        original + " was added to your medical file.", "DOCUMENT");
    }
    audit.record(actorId, "CREATE", "ClinicalDocument", saved.getId(), original);
    return view(saved);
  }

  @Transactional(readOnly = true)
  public PageResponse<View> list(UUID actorId, UUID patientId, int page, int size) {
    User actor = users.findById(actorId).orElseThrow();
    UUID target;
    if (actor.getRole() == Role.PATIENT) target = patients.findByUserId(actorId).orElseThrow().getId();
    else if (actor.getRole() == Role.DOCTOR || actor.getRole() == Role.ADMIN) {
      if (patientId == null) throw new ConflictException("patientId is required for staff");
      target = patientId;
    } else throw new ForbiddenOperationException("Document access is not permitted");
    return PageResponse.from(documents.findByPatientIdOrderByCreatedAtDesc(target,
      PageRequest.of(page, Math.min(size, 100))).map(this::view));
  }

  @Transactional(readOnly = true)
  public Download download(UUID actorId, UUID id) {
    ClinicalDocument d = authorized(actorId, id);
    return new Download(storage.load(d.getStoredName()), d.getOriginalName(), d.getContentType(), d.getSizeBytes());
  }

  @Transactional
  public void delete(UUID actorId, UUID id) {
    ClinicalDocument d = authorized(actorId, id);
    User actor = users.findById(actorId).orElseThrow();
    if (actor.getRole() != Role.ADMIN && !d.getUploadedBy().getId().equals(actorId))
      throw new ForbiddenOperationException("Only the uploader or admin can delete this document");
    documents.delete(d);
    storage.delete(d.getStoredName());
    audit.record(actorId, "DELETE", "ClinicalDocument", id, d.getOriginalName());
  }

  private ClinicalDocument authorized(UUID actorId, UUID id) {
    User actor = users.findById(actorId).orElseThrow();
    ClinicalDocument d = documents.findById(id).orElseThrow(() -> new NotFoundException("Document not found"));
    if (actor.getRole() == Role.ADMIN || actor.getRole() == Role.DOCTOR ||
        d.getPatient().getUser().getId().equals(actorId)) return d;
    throw new ForbiddenOperationException("You cannot access this document");
  }

  private String safeOriginalName(String raw) {
    String name = raw == null || raw.isBlank() ? "document" : Paths.get(raw).getFileName().toString();
    return name.replaceAll("[^A-Za-z0-9._ -]", "_").substring(0, Math.min(name.length(), 255));
  }

  private String extension(String name) {
    int index = name.lastIndexOf('.');
    return index < 0 ? "" : name.substring(index).toLowerCase(Locale.ROOT);
  }

  private View view(ClinicalDocument d) {
    return new View(d.getId(), d.getPatient().getId(), d.getPatient().getUser().getDisplayName(),
      d.getUploadedBy().getId(), d.getUploadedBy().getDisplayName(), d.getCategory(), d.getOriginalName(),
      d.getContentType(), d.getSizeBytes(), d.getNotes(), d.getCreatedAt());
  }

  public record View(UUID id, UUID patientId, String patientName, UUID uploadedById,
    String uploadedByName, DocumentCategory category, String originalName, String contentType,
    long sizeBytes, String notes, java.time.Instant createdAt) {}
  public record Download(Resource resource, String originalName, String contentType, long sizeBytes) {}
}
