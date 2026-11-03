package com.tutormatch.platform;

import static com.tutormatch.platform.ApiErrors.require;

import com.tutormatch.auth.SecurityConfig.Account;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LessonController {
  final ProfileService profiles;
  final RecommendationService recommendations;

  public LessonController(ProfileService profiles, RecommendationService recommendations) {
    this.profiles = profiles;
    this.recommendations = recommendations;
  }

  public record Swipe(@NotBlank String studentId, @NotBlank String tutorId, boolean interested) {}

  @PostMapping("/swipes")
  public Object swipe(@AuthenticationPrincipal Account u, @Valid @RequestBody Swipe s) {
    var student = profiles.ownedStudent(s.studentId(), u.id());
    require(
        recommendations.eligible(student, profiles.publicTutor(s.tutorId())),
        400,
        "Gia sư không đáp ứng điều kiện bắt buộc");
    profiles
        .jdbc()
        .update(
            "INSERT INTO swipe_interactions(student_id,tutor_id,interested) VALUES(?,?,?) ON"
                + " DUPLICATE KEY UPDATE"
                + " interested=VALUES(interested),created_at=CURRENT_TIMESTAMP",
            s.studentId(),
            s.tutorId(),
            s.interested());
    return Map.of(
        "interested", s.interested(), "message", "Đã lưu lựa chọn. Gia sư chưa xác nhận kết nối.");
  }

  public record Request(
      @NotBlank String studentId,
      @NotBlank String tutorId,
      @NotNull Instant proposedAt,
      @NotNull @Size(max = 2000) String message) {}

  @PostMapping("/lessons")
  @Transactional
  public Object request(@AuthenticationPrincipal Account u, @Valid @RequestBody Request r) {
    require(u.role().equals("PARENT"), 403, "Chỉ phụ huynh có thể gửi yêu cầu");
    // Serialize submissions per student before the unique pending-pair insert.
    profiles.jdbc().queryForList("SELECT id FROM student_profiles WHERE id=? AND parent_id=? FOR UPDATE",r.studentId(),u.id());
    var s = profiles.ownedStudent(r.studentId(), u.id());
    var tutor = profiles.publicTutor(r.tutorId());
    require(recommendations.eligible(s, tutor), 400, "Gia sư không đáp ứng điều kiện bắt buộc");
    require(
        r.proposedAt().isAfter(Instant.now().plusSeconds(3600))
            && r.proposedAt().isBefore(Instant.now().plusSeconds(90 * 86400)),
        400,
        "Chọn thời gian sau ít nhất một giờ và trong 90 ngày");
    profiles.jdbc().update("UPDATE lesson_requests SET status='EXPIRED' WHERE student_id=? AND tutor_id=? AND status='PENDING' AND expires_at<=CURRENT_TIMESTAMP",r.studentId(),r.tutorId());
    String id = UUID.randomUUID().toString();
    Instant expires = Instant.now().plusSeconds(7 * 86400);
    if (r.proposedAt().isBefore(expires)) expires = r.proposedAt();
    profiles
        .jdbc()
        .update(
            "INSERT INTO lesson_requests(id,student_id,tutor_id,proposed_at,message,expires_at)"
                + " VALUES(?,?,?,?,?,?)",
            id,
            r.studentId(),
            r.tutorId(),
            Timestamp.from(r.proposedAt()),
            r.message(),
            Timestamp.from(expires));
    return Map.of("id", id, "status", "PENDING");
  }

  public record Response(@Pattern(regexp = "ACCEPTED|REJECTED") @NotNull String status) {}

  @PutMapping("/lessons/{id}")
  @Transactional
  public Object respond(
      @AuthenticationPrincipal Account u, @PathVariable String id, @Valid @RequestBody Response r) {
    require(u.role().equals("TUTOR"), 403, "Chỉ gia sư có thể phản hồi");
    int n =
        profiles
            .jdbc()
            .update(
                "UPDATE lesson_requests l JOIN tutor_profiles t ON t.id=l.tutor_id SET"
                    + " l.status=?,l.responded_at=CURRENT_TIMESTAMP WHERE l.id=? AND t.user_id=?"
                    + " AND l.status='PENDING' AND l.expires_at>CURRENT_TIMESTAMP",
                r.status(),
                id,
                u.id());
    require(n == 1, 409, "Yêu cầu không tồn tại, không thuộc bạn hoặc đã kết thúc");
    return Map.of("status", r.status());
  }

  @GetMapping("/lessons")
  public Object list(@AuthenticationPrincipal Account u) {
    require(Set.of("PARENT", "TUTOR").contains(u.role()), 403, "Vai trò không hợp lệ");
    expire();
    String condition = u.role().equals("PARENT") ? "s.parent_id=?" : "t.user_id=?";
    return profiles
        .jdbc()
        .queryForList(
            "SELECT"
                + " l.id,l.student_id,l.tutor_id,l.status,l.proposed_at,l.expires_at,l.message,l.created_at,s.nickname,t.display_name,CASE"
                + " WHEN l.status='ACCEPTED' THEN pu.email ELSE NULL END AS parent_email,CASE WHEN"
                + " l.status='ACCEPTED' THEN pu.phone ELSE NULL END AS parent_phone,CASE WHEN"
                + " l.status='ACCEPTED' THEN tu.email ELSE NULL END AS tutor_email,CASE WHEN"
                + " l.status='ACCEPTED' THEN tu.phone ELSE NULL END AS tutor_phone FROM"
                + " lesson_requests l JOIN student_profiles s ON s.id=l.student_id JOIN"
                + " tutor_profiles t ON t.id=l.tutor_id JOIN users pu ON pu.id=s.parent_id JOIN"
                + " users tu ON tu.id=t.user_id WHERE "
                + condition
                + " ORDER BY l.created_at DESC",
            u.id());
  }

  void expire() {
    profiles
        .jdbc()
        .update(
            "UPDATE lesson_requests SET status='EXPIRED' WHERE status='PENDING' AND"
                + " expires_at<=CURRENT_TIMESTAMP");
  }
}
