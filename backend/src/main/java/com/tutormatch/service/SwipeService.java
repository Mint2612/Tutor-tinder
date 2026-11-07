package com.tutormatch.service;

import static com.tutormatch.exception.BusinessException.require;

import com.tutormatch.dto.lesson.*;
import com.tutormatch.model.TutorProfile;
import com.tutormatch.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SwipeService {
  private final StudentService students;
  private final TutorService tutors;
  private final TutorRepository tutorRepository;
  private final SwipeRepository swipes;
  private final EligibilityPolicy eligibility;

  public SwipeService(
      StudentService students,
      TutorService tutors,
      TutorRepository tutorRepository,
      SwipeRepository swipes,
      EligibilityPolicy eligibility) {
    this.students = students;
    this.tutors = tutors;
    this.tutorRepository = tutorRepository;
    this.swipes = swipes;
    this.eligibility = eligibility;
  }

  @Transactional
  public SwipeResponse save(String parent, LessonRequests.Swipe r) {
    var s = students.owned(r.studentId(), parent);
    var t = tutors.publicTutor(r.tutorId());
    require(eligibility.eligible(s, t), 400, "Gia sư không đáp ứng điều kiện bắt buộc");
    swipes.save(s.id(), t.id(), r.interested());
    return new SwipeResponse(r.interested(), "Đã lưu lựa chọn. Gia sư chưa xác nhận kết nối.");
  }

  public List<TutorProfile> interests(String student, String parent) {
    var s = students.owned(student, parent);
    var ids = swipes.interestedTutorIds(student);
    return tutorRepository.findPublicByIds(ids).stream()
        .filter(t -> eligibility.eligible(s, t))
        .sorted(Comparator.comparingInt(t -> ids.indexOf(t.id())))
        .toList();
  }

  @Transactional
  public void remove(String student, String tutor, String parent) {
    students.owned(student, parent);
    require(swipes.removeInterest(student, tutor) == 1, 404, "Không tìm thấy lựa chọn quan tâm");
  }
}
