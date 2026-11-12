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
  private final MatchingService matching;
  private final MatchingRepository pairs;

  public SwipeService(
      StudentService students,
      TutorService tutors,
      TutorRepository tutorRepository,
      SwipeRepository swipes,
      EligibilityPolicy eligibility, MatchingService matching, MatchingRepository pairs) {
    this.students = students;
    this.tutors = tutors;
    this.tutorRepository = tutorRepository;
    this.swipes = swipes;
    this.eligibility = eligibility;
    this.matching = matching;
    this.pairs = pairs;
  }

  @Transactional
  public SwipeResponse save(String parent, LessonRequests.Swipe r) {
    var result = matching.act(new com.tutormatch.model.Account(parent,"","","PARENT"),
        new com.tutormatch.dto.recommendation.MatchingDtos.Action(r.studentId(),r.tutorId(),r.interested()));
    return new SwipeResponse(r.interested(), result.message());
  }

  public List<TutorProfile> interests(String student, String parent) {
    var s = students.owned(student, parent);
    var ids = swipes.interestedTutorIds(student);
    var active = pairs.forProfile(true,student).stream()
        .filter(p -> "LIKE".equals(p.parentChoice()) && (p.status().equals("MATCHED")
            || (p.status().equals("OPEN") && p.expiresAt()!=null && p.expiresAt().isAfter(java.time.Instant.now()))))
        .map(MatchingRepository.State::tutorId).collect(java.util.stream.Collectors.toSet());
    return tutorRepository.findPublicByIds(ids).stream()
        .filter(t -> active.contains(t.id()))
        .filter(t -> eligibility.eligible(s, t))
        .sorted(Comparator.comparingInt(t -> ids.indexOf(t.id())))
        .toList();
  }

  @Transactional
  public void remove(String student, String tutor, String parent) {
    students.owned(student, parent);
    tutors.lock(tutor);
    var pair=pairs.lockPair(student,tutor);
    require(pair.status().equals("OPEN") && "LIKE".equals(pair.parentChoice()),409,"Cặp đã đóng hoặc đã Match.");
    pairs.choose(true,pair,false,false,java.time.Instant.now());
    require(swipes.removeInterest(student, tutor) == 1, 404, "Không tìm thấy lựa chọn quan tâm");
  }
}
