package com.tutormatch.service;

import static com.tutormatch.exception.BusinessException.require;

import com.tutormatch.dto.lesson.*;
import com.tutormatch.model.Account;
import com.tutormatch.repository.LessonRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LessonService {
  private final StudentService students;
  private final TutorService tutors;
  private final EligibilityPolicy eligibility;
  private final LessonRepository lessons;
  private final ConnectionService connections;
  private final com.tutormatch.repository.TrialRepository trials;

  public LessonService(
      StudentService students,
      TutorService tutors,
      EligibilityPolicy eligibility,
      LessonRepository lessons, ConnectionService connections, com.tutormatch.repository.TrialRepository trials) {
    this.students = students;
    this.tutors = tutors;
    this.eligibility = eligibility;
    this.lessons = lessons;
    this.connections = connections;
    this.trials = trials;
  }

  @Transactional
  public LessonResult request(Account u, LessonRequests.Create r) {
    require(u.role().equals("PARENT"), 403, "Chỉ phụ huynh có thể gửi yêu cầu");
    connections.requireConnected(u,r.studentId(),r.tutorId());
    require(!trials.legacyActive(r.studentId(),r.tutorId()),409,"Cặp đã có lịch học thử đang hoạt động.");
    require(trials.pair(r.studentId(),r.tutorId()).stream().noneMatch(t -> Set.of("PENDING","CONFIRMED").contains(t.status())),409,"Cặp đã có lịch học thử đang hoạt động.");
    var s = students.lockOwned(r.studentId(), u.id());
    var t = tutors.publicTutor(r.tutorId());
    require(eligibility.eligible(s, t), 400, "Gia sư không đáp ứng điều kiện bắt buộc");
    Instant now = Instant.now();
    require(
        r.proposedAt().isAfter(now.plusSeconds(3600))
            && r.proposedAt().isBefore(now.plusSeconds(90L * 86400)),
        400,
        "Chọn thời gian sau ít nhất một giờ và trong 90 ngày");
    lessons.expirePair(s.id(), t.id());
    String id = UUID.randomUUID().toString();
    Instant expires = now.plusSeconds(7L * 86400);
    if (r.proposedAt().isBefore(expires)) expires = r.proposedAt();
    lessons.create(id, r, expires);
    return new LessonResult(id, "PENDING");
  }

  @Transactional
  public void respond(Account u, String id, LessonRequests.Respond r) {
    require(u.role().equals("TUTOR"), 403, "Chỉ gia sư có thể phản hồi");
    var pair=lessons.ownedPair(id,u.id()).orElseThrow(() -> new com.tutormatch.exception.BusinessException(409,"Không tìm thấy yêu cầu học thử của bạn."));
    connections.requireConnected(u,(String)pair.get("student_id"),(String)pair.get("tutor_id"));
    require(
        lessons.respond(id, u.id(), r.status()) == 1,
        409,
        "Yêu cầu không tồn tại, không thuộc bạn hoặc đã kết thúc");
  }

  @Transactional
  public List<LessonResponse> list(Account u) {
    require(Set.of("PARENT", "TUTOR").contains(u.role()), 403, "Vai trò không hợp lệ");
    return lessons.list(u);
  }
}
