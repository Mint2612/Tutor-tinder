package com.tutormatch.controller;

import com.tutormatch.dto.IdResponse;
import com.tutormatch.dto.StatusResponse;
import com.tutormatch.dto.profile.TutorResponse;
import com.tutormatch.dto.verification.*;
import com.tutormatch.mapper.TutorMapper;
import com.tutormatch.model.Account;
import com.tutormatch.service.*;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class VerificationController {
  private final VerificationService verification;
  private final TutorService tutors;
  private final TutorMapper mapper;

  public VerificationController(
      VerificationService verification, TutorService tutors, TutorMapper mapper) {
    this.verification = verification;
    this.tutors = tutors;
    this.mapper = mapper;
  }

  @GetMapping("/tutor/documents")
  public List<DocumentResponse> documents(@AuthenticationPrincipal Account u) {
    return verification.documents(u.id());
  }

  @PostMapping("/tutor/documents")
  public IdResponse upload(
      @AuthenticationPrincipal Account u, @RequestParam("file") MultipartFile file)
      throws IOException {
    return new IdResponse(
        verification.upload(
            u.id(), file.getOriginalFilename(), file.getContentType(), file.getBytes()));
  }

  @DeleteMapping("/tutor/documents/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(@PathVariable String id, @AuthenticationPrincipal Account u) {
    verification.remove(u.id(), id);
  }

  @PostMapping("/tutor/submit")
  public TutorResponse submit(@AuthenticationPrincipal Account u) {
    return mapper.ownerResponse(verification.submit(u.id()));
  }

  @GetMapping("/admin/tutors")
  public List<TutorResponse> queue() {
    return tutors.all().stream().map(mapper::ownerResponse).toList();
  }

  @GetMapping("/admin/tutors/{id}")
  public TutorResponse review(@PathVariable String id) {
    return verification.review(id);
  }

  @GetMapping("/admin/documents/{id}")
  public ResponseEntity<byte[]> read(@PathVariable String id, @AuthenticationPrincipal Account u) {
    var d = verification.read(id, u.id());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(d.contentType()))
        .header("Content-Disposition", "attachment; filename=\"evidence\"")
        .header("Cache-Control", "no-store")
        .header("X-Content-Type-Options", "nosniff")
        .body(d.content());
  }

  @PostMapping("/admin/tutors/{id}/decision")
  public StatusResponse decide(
      @PathVariable String id,
      @AuthenticationPrincipal Account u,
      @Valid @RequestBody VerificationRequests.Decision d) {
    return new StatusResponse(verification.decide(id, u.id(), d));
  }

  @PutMapping("/admin/tutors/{id}/visibility")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void visibility(
      @PathVariable String id, @Valid @RequestBody VerificationRequests.Visibility v) {
    verification.visibility(id, v.visible());
  }
}
