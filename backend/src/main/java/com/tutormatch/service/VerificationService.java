package com.tutormatch.service;

import static com.tutormatch.exception.BusinessException.require;

import com.tutormatch.dto.profile.TutorResponse;
import com.tutormatch.dto.verification.*;
import com.tutormatch.integration.storage.FileStorage;
import com.tutormatch.mapper.TutorMapper;
import com.tutormatch.model.*;
import com.tutormatch.repository.VerificationRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerificationService {
  private final TutorService tutors;
  private final VerificationRepository verification;
  private final TutorMapper mapper;
  private final FileStorage storage;

  public VerificationService(
      TutorService tutors,
      VerificationRepository verification,
      TutorMapper mapper,
      FileStorage storage) {
    this.tutors = tutors;
    this.verification = verification;
    this.mapper = mapper;
    this.storage = storage;
  }

  public List<DocumentResponse> documents(String user) {
    return verification.documents(tutors.own(user).id());
  }

  @Transactional
  public String upload(String user, String originalName, String type, byte[] bytes) {
    var t = tutors.lockOwn(user);
    checkFile(bytes, type);
    require(
        verification.documentCount(t.id()) < 5,
        400,
        "Tối đa 5 giấy tờ. Hãy xóa giấy tờ cũ trước khi thay thế.");
    String filename =
        Optional.ofNullable(originalName).orElse("document").replaceAll("[^\\p{L}\\p{N}._ -]", "_");
    if (filename.isBlank()) filename = "document";
    filename = filename.substring(0, Math.min(200, filename.length()));
    String id = UUID.randomUUID().toString();
    reopenForEvidenceChange(t);
    // Identity evidence goes to the private bucket; the database keeps only metadata and the key.
    String key = t.id() + "/" + id;
    storage.put(FileStorage.EVIDENCE, key, bytes, type);
    try {
      verification.upload(id, t.id(), filename, type, key, t.revision());
    } catch (RuntimeException e) {
      storage.delete(FileStorage.EVIDENCE, key);
      throw e;
    }
    return id;
  }

  /**
   * Edits one of the tutor's own documents: a new display name, a replacement file, or both. A real
   * change counts as an evidence change (see {@link #reopenForEvidenceChange}); saving the same
   * name again changes nothing and leaves the current decision alone.
   */
  @Transactional
  public void update(String user, String document, String name, String type, byte[] bytes) {
    var t = tutors.lockOwn(user);
    require(name != null || bytes != null, 400, "Không có thay đổi để lưu");
    String current =
        verification
            .filename(document, t.id())
            .orElseThrow(
                () -> new com.tutormatch.exception.BusinessException(404, "Không tìm thấy giấy tờ"));
    String label = name == null ? null : label(name);
    if (bytes != null) checkFile(bytes, type);
    boolean renamed = label != null && !label.equals(current);
    if (bytes == null && !renamed) return;
    String oldKey = verification.storageKey(document, t.id()).orElse(null);
    String newKey = null;
    if (bytes != null) {
      newKey = t.id() + "/" + UUID.randomUUID();
      storage.put(FileStorage.EVIDENCE, newKey, bytes, type);
    }
    try {
      reopenForEvidenceChange(t);
      if (renamed) verification.rename(document, t.id(), label);
      if (bytes != null) verification.replace(document, t.id(), type, newKey, t.revision());
    } catch (RuntimeException e) {
      if (newKey != null) storage.delete(FileStorage.EVIDENCE, newKey);
      throw e;
    }
    if (newKey != null && oldKey != null)
      AfterCommit.run(() -> storage.delete(FileStorage.EVIDENCE, oldKey));
  }

  @Transactional
  public void remove(String user, String document) {
    var t = tutors.lockOwn(user);
    String key = verification.storageKey(document, t.id()).orElse(null);
    require(verification.remove(document, t.id()) == 1, 404, "Không tìm thấy giấy tờ");
    reopenForEvidenceChange(t);
    if (key != null) AfterCommit.run(() -> storage.delete(FileStorage.EVIDENCE, key));
  }

  @Transactional
  public TutorProfile submit(String user) {
    var t = tutors.lockOwn(user);
    editable(t);
    require(verification.documentCount(t.id()) > 0, 400, "Cần gửi bằng chứng danh tính");
    // The admin compares this name with the documents, so it has to be filled in before review.
    require(
        t.legalName() != null && !t.legalName().isBlank(),
        400,
        "Cần nhập họ và tên đúng như giấy tờ trước khi gửi duyệt");
    require(
        !t.displayName().isBlank()
            && !t.subjects().isEmpty()
            && !t.grades().isEmpty()
            && !t.modes().isEmpty()
            && !t.availability().isEmpty(),
        400,
        "Cần hoàn thiện hồ sơ trước khi gửi");
    verification.submit(t.id(), UUID.randomUUID().toString());
    return tutors.own(user);
  }

  public TutorResponse review(String id) {
    return mapper.adminResponse(
        tutors.find(id), verification.documents(id), verification.audit(id));
  }

  @Transactional
  public DocumentContent read(String id, String admin) {
    var d =
        verification
            .content(id)
            .orElseThrow(
                () ->
                    new com.tutormatch.exception.BusinessException(404, "Không tìm thấy giấy tờ"));
    tutors.lock(d.tutorId());
    // Reload after acquiring the tutor lock: a concurrent replacement must not count as read.
    d = verification.content(id).orElseThrow(
        () -> new com.tutormatch.exception.BusinessException(404, "Không tìm thấy giấy tờ"));
    byte[] bytes = d.content();
    if (bytes == null)
      bytes =
          storage
              .get(FileStorage.EVIDENCE, d.storageKey())
              .orElseThrow(
                  () -> new com.tutormatch.exception.BusinessException(404, "Không tìm thấy giấy tờ"))
              .content();
    // Only a file that was really handed to the admin counts as "opened" for the approval rule.
    verification.recordRead(id, admin, d.tutorId());
    return new DocumentContent(
        d.id(), d.tutorId(), d.filename(), d.contentType(), bytes, d.storageKey());
  }

  @Transactional
  public String decide(String id, String admin, VerificationRequests.Decision d) {
    var t = tutors.lock(id);
    require(t.workflowState().equals("PENDING_REVIEW"), 409, "Hồ sơ không chờ duyệt");
    if (d.approve())
      require(
          verification.reviewedCurrentSubmission(id, admin),
          400,
          "Cần mở và kiểm tra bằng chứng danh tính trước khi duyệt");
    else require(d.reason() != null && !d.reason().isBlank(), 400, "Cần nhập lý do từ chối");
    String status = d.approve() ? "APPROVED" : "REJECTED";
    verification.decide(id, admin, status, d.reason());
    return status;
  }

  public void visibility(String id, boolean visible) {
    require(verification.visibility(id, visible) == 1, 404, "Không tìm thấy gia sư");
  }

  /** Size limit plus a magic-byte check, so the declared type cannot be a lie. */
  private static void checkFile(byte[] bytes, String type) {
    require(bytes.length > 0 && bytes.length <= 5 * 1024 * 1024, 400, "Giấy tờ tối đa 5 MB");
    boolean pdf =
        "application/pdf".equals(type)
            && bytes.length > 4
            && bytes[0] == '%'
            && bytes[1] == 'P'
            && bytes[2] == 'D'
            && bytes[3] == 'F';
    boolean png =
        "image/png".equals(type)
            && bytes.length > 8
            && bytes[0] == (byte) 137
            && bytes[1] == 'P'
            && bytes[2] == 'N'
            && bytes[3] == 'G'
            && bytes[4] == 13
            && bytes[5] == 10
            && bytes[6] == 26
            && bytes[7] == 10;
    boolean jpg =
        "image/jpeg".equals(type)
            && bytes.length > 3
            && bytes[0] == (byte) 255
            && bytes[1] == (byte) 216
            && bytes[2] == (byte) 255;
    require(pdf || png || jpg, 400, "Chỉ nhận PDF, PNG hoặc JPEG hợp lệ");
  }

  /** A name the tutor typed for a document: free text, but one tidy line of at most 120 characters. */
  private static String label(String raw) {
    String s = raw.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").strip();
    require(!s.isEmpty(), 400, "Tên giấy tờ không được để trống");
    require(s.length() <= 120, 400, "Tên giấy tờ tối đa 120 ký tự");
    return s;
  }

  /** Submitting is only possible from a draft or after a rejection. */
  private void editable(TutorProfile t) {
    require(
        Set.of("DRAFT", "REJECTED").contains(t.workflowState()),
        409,
        "Chỉ có thể gửi duyệt khi hồ sơ ở bản nháp hoặc bị từ chối");
  }

  /**
   * Evidence is the only thing an admin reviews. Changing it while a submission is pending or the
   * profile is approved withdraws that decision until the evidence is submitted again; a rejected
   * profile stays rejected (with its reason) until the tutor resubmits.
   */
  private void reopenForEvidenceChange(TutorProfile t) {
    if (Set.of("APPROVED", "PENDING_REVIEW").contains(t.workflowState()))
      verification.reopen(t.id());
  }
}
