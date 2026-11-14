package com.tutormatch.service;

import static com.tutormatch.exception.BusinessException.require;

import com.tutormatch.dto.recommendation.ConnectionDtos.*;
import com.tutormatch.exception.BusinessException;
import com.tutormatch.mapper.TutorMapper;
import com.tutormatch.model.*;
import com.tutormatch.repository.*;
import com.tutormatch.repository.MatchingRepository.State;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConnectionService {
  private final MatchingRepository matching;
  private final StudentRepository studentRepository;
  private final TutorRepository tutorRepository;
  private final StudentService students;
  private final TutorService tutors;
  private final TutorMapper tutorMapper;

  public ConnectionService(MatchingRepository matching, StudentRepository studentRepository,
      TutorRepository tutorRepository, StudentService students, TutorService tutors,
      TutorMapper tutorMapper) {
    this.matching = matching;
    this.studentRepository = studentRepository;
    this.tutorRepository = tutorRepository;
    this.students = students;
    this.tutors = tutors;
    this.tutorMapper = tutorMapper;
  }

  private static BusinessException notFound() {
    return new BusinessException(404, "Không tìm thấy cặp phù hợp.");
  }

  private record Ctx(StudentProfile s, TutorProfile t, boolean parent) {}

  /** Cùng thứ tự khóa với MatchingService.act: tài khoản gia sư → gia sư → phụ huynh → học sinh → cặp. */
  private Ctx open(Account user, String studentId, String tutorId, boolean lock) {
    require(Set.of("PARENT", "TUTOR").contains(user.role()), 403,
        "Chỉ phụ huynh và gia sư sử dụng matching.");
    boolean parent = user.role().equals("PARENT");
    var found = parent ? tutors.find(tutorId) : tutors.own(user.id());
    require(parent || found.id().equals(tutorId), 404, "Không tìm thấy cặp phù hợp.");
    TutorProfile t = found;
    if (lock) {
      require(tutorRepository.lockActiveOwner(found.userId()), 404, "Hồ sơ không còn hoạt động.");
      t = tutors.lock(found.id());
    }
    var s = parent ? students.owned(studentId, user.id())
        : studentRepository.findById(studentId).orElseThrow(ConnectionService::notFound);
    if (lock) {
      require(studentRepository.lockActiveParent(s.parentId()), 404, "Hồ sơ không còn hoạt động.");
      s = studentRepository.lockById(s.id()).orElseThrow(ConnectionService::notFound);
    }
    return new Ctx(s, t, parent);
  }

  @Transactional
  public Detail detail(Account user, String studentId, String tutorId) {
    var c = open(user, studentId, tutorId, false);
    matching.expire(c.parent(), c.parent() ? c.s().id() : c.t().id(), Instant.now());
    var pair = matching.findPair(c.s().id(), c.t().id(), false).orElseThrow(ConnectionService::notFound);
    require(pair.status().equals("MATCHED"), 404, "Cặp không còn hiệu lực.");
    return build(c, pair);
  }

  @Transactional
  public Detail confirm(Account user, Decision d) {
    var c = open(user, d.studentId(), d.tutorId(), true);
    matching.findPair(c.s().id(), c.t().id(), true).orElseThrow(ConnectionService::notFound);
    matching.expire(c.parent(), c.parent() ? c.s().id() : c.t().id(), Instant.now());
    var pair = matching.findPair(c.s().id(), c.t().id(), false).orElseThrow(ConnectionService::notFound);
    String own = c.parent() ? pair.parentConfirm() : pair.tutorConfirm();
    String wanted = d.confirm() ? "CONFIRM" : "DECLINE";
    if (own != null) { // gửi lại cùng quyết định là idempotent; đổi ý thì từ chối
      require(own.equals(wanted), 409, "Quyết định đã được lưu và không thể đổi.");
      return build(c, pair);
    }
    require(pair.status().equals("MATCHED"), 409, "Cặp đã đóng hoặc hết hạn.");
    if (d.confirm()) tutors.publicTutor(c.t().id()); // gia sư bị ẩn/không còn duyệt thì không xác nhận được
    matching.confirm(c.parent(), c.s().id(), c.t().id(), d.confirm());
    return build(c, matching.findPair(c.s().id(), c.t().id(), false).orElseThrow(ConnectionService::notFound));
  }

  private Detail build(Ctx c, State pair) {
    boolean connected = "CONFIRM".equals(pair.parentConfirm()) && "CONFIRM".equals(pair.tutorConfirm());
    String mine = c.parent() ? pair.parentConfirm() : pair.tutorConfirm();
    Contact contact = null;
    if (connected) {
      var m = matching.contacts(c.s().id(), c.t().id());
      contact = c.parent()
          ? new Contact((String) m.get("tutor_email"), (String) m.get("tutor_phone"))
          : new Contact((String) m.get("parent_email"), (String) m.get("parent_phone"));
    }
    var tutor = c.parent() ? tutorMapper.publicResponse(c.t()) : null;
    StudentDetail student = null;
    if (!c.parent()) {
      var s = c.s();
      var prefs = s.survey().entrySet().stream().sorted(Map.Entry.comparingByKey())
          .map(e -> MatchingService.LEARNING.get(e.getValue())).filter(Objects::nonNull)
          .distinct().toList();
      // Hồ sơ cũ có thể còn SĐT/email trong mô tả: không bao giờ chuyển cho gia sư.
      String needs = ContactInfo.find(s.needs()).isPresent() ? "" : s.needs();
      student = new StudentDetail(s.grade(), s.subjects(), s.mode(), s.province(), s.budget(),
          s.availability(), prefs, needs);
    }
    return new Detail(c.s().id(), c.t().id(), pair.status(), mine, connected,
        pair.confirmExpiresAt(), tutor, student, contact);
  }
}