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

  public LessonService(
      StudentService students,
      TutorService tutors,
      EligibilityPolicy eligibility,
      LessonRepository lessons) {
    this.students = students;
    this.tutors = tutors;
    this.eligibility = eligibility;
    this.lessons = lessons;
  }

  @Transactional
  public LessonResult request(Account u, LessonRequests.Create r) {
    require(u.role().equals("PARENT"), 403, "Chỉ phụ huynh có thể gửi yêu cầu");
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
