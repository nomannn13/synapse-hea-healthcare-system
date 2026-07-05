package com.synapse.hea.document;

import com.synapse.hea.common.api.PageResponse;
import com.synapse.hea.common.security.CurrentUser;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
public class ClinicalDocumentController {
  private final ClinicalDocumentService service;
  private final CurrentUser current;

  public ClinicalDocumentController(ClinicalDocumentService service, CurrentUser current) {
    this.service = service;
    this.current = current;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ClinicalDocumentService.View upload(Authentication a,
      @RequestParam(required = false) UUID patientId,
      @RequestParam DocumentCategory category,
      @RequestParam(required = false) String notes,
      @RequestPart("file") MultipartFile file) {
    return service.upload(current.id(a), patientId, category, notes, file);
  }

  @GetMapping
  public PageResponse<ClinicalDocumentService.View> list(Authentication a,
      @RequestParam(required = false) UUID patientId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.list(current.id(a), patientId, page, size);
  }

  @GetMapping("/{id}/download")
  public ResponseEntity<org.springframework.core.io.Resource> download(Authentication a, @PathVariable UUID id) {
    ClinicalDocumentService.Download d = service.download(current.id(a), id);
    MediaType type;
    try { type = MediaType.parseMediaType(d.contentType()); }
    catch (InvalidMediaTypeException ex) { type = MediaType.APPLICATION_OCTET_STREAM; }
    return ResponseEntity.ok()
      .contentType(type)
      .contentLength(d.sizeBytes())
      .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(d.originalName()).build().toString())
      .body(d.resource());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(Authentication a, @PathVariable UUID id) {
    service.delete(current.id(a), id);
  }
}
