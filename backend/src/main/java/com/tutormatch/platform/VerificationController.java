package com.tutormatch.platform;

import static com.tutormatch.platform.ApiErrors.require;

import com.tutormatch.auth.SecurityConfig.Account;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class VerificationController {
  final ProfileService profiles;

  public VerificationController(ProfileService profiles) {
    this.profiles = profiles;
  }

  @GetMapping("/tutor/documents")
  public Object documents(@AuthenticationPrincipal Account u) {
    var t = profiles.ownTutor(u.id());
    return profiles
        .jdbc()
        .queryForList(
            "SELECT id,filename,content_type,revision,created_at FROM tutor_documents WHERE"
                + " tutor_id=? ORDER BY created_at DESC",
            t.get("id"));
  }

  @PostMapping("/tutor/documents")
  @Transactional
  public Object upload(@AuthenticationPrincipal Account u, @RequestParam("file") MultipartFile file)
      throws java.io.IOException {
    profiles
        .jdbc()
        .queryForList("SELECT id FROM tutor_profiles WHERE user_id=? FOR UPDATE", u.id());
    var t = profiles.ownTutor(u.id());
    Object id = t.get("id");
    require(
        t.get("workflow_state").equals("DRAFT") || t.get("workflow_state").equals("REJECTED"),
        409,
        "Chỉ có thể thêm giấy tờ khi hồ sơ ở bản nháp hoặc bị từ chối");
    require(!file.isEmpty() && file.getSize() <= 5 * 1024 * 1024, 400, "Giấy tờ tối đa 5 MB");
    require(
        profiles
                .jdbc()
                .queryForObject(
                    "SELECT COUNT(*) FROM tutor_documents WHERE tutor_id=?", Integer.class, id)
            < 5,
        400,
        "Tối đa 5 giấy tờ. Liên hệ quản trị viên nếu cần thay thế.");
    byte[] b = file.getBytes();
    String type = file.getContentType();
    boolean pdf =
        "application/pdf".equals(type)
            && b.length > 4
            && b[0] == '%'
            && b[1] == 'P'
            && b[2] == 'D'
            && b[3] == 'F';
    boolean png =
        "image/png".equals(type)
            && b.length > 8
            && b[0] == (byte) 137
            && b[1] == 'P'
            && b[2] == 'N'
            && b[3] == 'G';
    boolean jpg =
        "image/jpeg".equals(type)
            && b.length > 3
            && b[0] == (byte) 255
            && b[1] == (byte) 216
            && b[2] == (byte) 255;
    require(pdf || png || jpg, 400, "Chỉ nhận PDF, PNG hoặc JPEG hợp lệ");
    String document = UUID.randomUUID().toString();
    String filename =
        Optional.ofNullable(file.getOriginalFilename())
            .orElse("document")
            .replaceAll("[^\\p{L}\\p{N}._ -]", "_");
    filename = filename.substring(0, Math.min(200, filename.length()));
    profiles
        .jdbc()
        .update(
            "INSERT INTO tutor_documents(id,tutor_id,filename,content_type,content,revision)"
                + " VALUES(?,?,?,?,?,?)",
            document,
            id,
            filename,
            type,
            b,
            t.get("revision"));
    return Map.of("id", document);
  }

  @DeleteMapping("/tutor/documents/{id}")
  @Transactional
  public void remove(@AuthenticationPrincipal Account u, @PathVariable String id) {
    profiles
        .jdbc()
        .queryForList("SELECT id FROM tutor_profiles WHERE user_id=? FOR UPDATE", u.id());
    var t = profiles.ownTutor(u.id());
    require(
        Set.of("DRAFT", "REJECTED").contains(t.get("workflow_state")),
        409,
        "Hồ sơ đang được xử lý");
    require(
        profiles
                .jdbc()
                .update("DELETE FROM tutor_documents WHERE id=? AND tutor_id=?", id, t.get("id"))
            == 1,
        404,
        "Không tìm thấy giấy tờ");
  }

  @PostMapping("/tutor/submit")
  @Transactional
  public Object submit(@AuthenticationPrincipal Account u) {
    profiles
        .jdbc()
        .queryForList("SELECT id FROM tutor_profiles WHERE user_id=? FOR UPDATE", u.id());
    var t = profiles.ownTutor(u.id());
    Object id = t.get("id");
    require(
        Set.of("DRAFT", "REJECTED").contains(t.get("workflow_state")),
        409,
        "Hồ sơ không ở trạng thái có thể gửi");
    require(
        profiles
                .jdbc()
                .queryForObject(
                    "SELECT COUNT(*) FROM tutor_documents WHERE tutor_id=?", Integer.class, id)
            > 0,
        400,
        "Cần gửi bằng chứng danh tính");
    require(!t.get("display_name").toString().isBlank(), 400, "Cần hoàn thiện hồ sơ trước khi gửi");
    profiles
        .jdbc()
        .update(
            "UPDATE tutor_profiles SET"
                + " workflow_state='PENDING_REVIEW',verification_status='PENDING',rejection_reason=NULL,submitted_at=CURRENT_TIMESTAMP,submission_id=?"
                + " WHERE id=?",
            UUID.randomUUID().toString(),
            id);
    return profiles.ownTutor(u.id());
  }

  @GetMapping("/admin/tutors")
  public Object queue() {
    return profiles
        .jdbc()
        .queryForList(
            "SELECT id,display_name,workflow_state,rejection_reason,visible,revision FROM"
                + " tutor_profiles ORDER BY workflow_state,display_name");
  }

  @GetMapping("/admin/tutors/{id}")
  public Object review(@PathVariable String id) {
    var rows = profiles.jdbc().queryForList("SELECT * FROM tutor_profiles WHERE id=?", id);
    require(!rows.isEmpty(), 404, "Không tìm thấy gia sư");
    var t = profiles.tutorView(rows.getFirst());
    t.put(
        "documents",
        profiles
            .jdbc()
            .queryForList(
                "SELECT id,filename,content_type,revision,created_at FROM tutor_documents WHERE"
                    + " tutor_id=?",
                id));
    return t;
  }

  @GetMapping("/admin/documents/{id}")
  @Transactional
  public ResponseEntity<byte[]> read(@PathVariable String id, @AuthenticationPrincipal Account u) {
    var rows = profiles.jdbc().queryForList("SELECT * FROM tutor_documents WHERE id=?", id);
    require(!rows.isEmpty(), 404, "Không tìm thấy giấy tờ");
    var d = rows.getFirst();
    profiles
        .jdbc()
        .update(
            "INSERT INTO document_reviews(document_id,admin_id,submission_id) SELECT"
                + " ?,?,submission_id FROM tutor_profiles WHERE id=? ON DUPLICATE KEY UPDATE"
                + " reviewed_at=CURRENT_TIMESTAMP,submission_id=VALUES(submission_id)",
            id,
            u.id(),
            d.get("tutor_id"));
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(d.get("content_type").toString()))
        .header("Content-Disposition", "attachment; filename=\"evidence\"")
        .header("Cache-Control", "no-store")
        .header("X-Content-Type-Options", "nosniff")
        .body((byte[]) d.get("content"));
  }

  public record Decision(boolean approve, @Size(max = 1000) String reason) {}

  @PostMapping("/admin/tutors/{id}/decision")
  @Transactional
  public Object decide(
      @PathVariable String id, @AuthenticationPrincipal Account u, @Valid @RequestBody Decision d) {
    var rows =
        profiles.jdbc().queryForList("SELECT * FROM tutor_profiles WHERE id=? FOR UPDATE", id);
    require(!rows.isEmpty(), 404, "Không tìm thấy gia sư");
    require(
        rows.getFirst().get("workflow_state").equals("PENDING_REVIEW"),
        409,
        "Hồ sơ không chờ duyệt");
    if (d.approve())
      require(
          profiles
                  .jdbc()
                  .queryForObject(
                      "SELECT COUNT(*) FROM tutor_documents d JOIN document_reviews r ON"
                          + " r.document_id=d.id JOIN tutor_profiles t ON t.id=d.tutor_id WHERE"
                          + " d.tutor_id=? AND r.admin_id=? AND r.submission_id=t.submission_id",
                      Integer.class,
                      id,
                      u.id())
              > 0,
          400,
          "Cần mở và kiểm tra bằng chứng danh tính trước khi duyệt");
    else require(d.reason() != null && !d.reason().isBlank(), 400, "Cần nhập lý do từ chối");
    String status = d.approve() ? "APPROVED" : "REJECTED";
    profiles
        .jdbc()
        .update(
            "UPDATE tutor_profiles SET"
                + " workflow_state=?,verification_status=?,rejection_reason=?,verified_at=IF(?='APPROVED',CURRENT_TIMESTAMP,NULL)"
                + " WHERE id=?",
            status,
            status,
            d.approve() ? null : d.reason(),
            status,
            id);
    profiles
        .jdbc()
        .update(
            "INSERT INTO verification_audit(id,tutor_id,admin_id,decision,reason)"
                + " VALUES(?,?,?,?,?)",
            UUID.randomUUID().toString(),
            id,
            u.id(),
            status,
            d.reason());
    return Map.of("status", status);
  }

  public record Visibility(boolean visible) {}

  @PutMapping("/admin/tutors/{id}/visibility")
  public void visibility(@PathVariable String id, @RequestBody Visibility v) {
    require(
        profiles.jdbc().update("UPDATE tutor_profiles SET visible=? WHERE id=?", v.visible(), id)
            == 1,
        404,
        "Không tìm thấy gia sư");
  }
}
