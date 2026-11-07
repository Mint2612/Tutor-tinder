package com.tutormatch.service;

import com.tutormatch.dto.recommendation.RecommendationResponse;
import com.tutormatch.integration.ai.LlmRanker;
import com.tutormatch.mapper.RecommendationMapper;
import com.tutormatch.model.TutorProfile;
import com.tutormatch.repository.TutorRepository;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {
  private final StudentService students;
  private final TutorRepository tutors;
  private final EligibilityPolicy eligibility;
  private final LlmRanker ai;
  private final RecommendationMapper mapper;
  private final Semaphore aiSlots = new Semaphore(8);
  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  public RecommendationService(
      StudentService students,
      TutorRepository tutors,
      EligibilityPolicy eligibility,
      LlmRanker ai,
      RecommendationMapper mapper) {
    this.students = students;
    this.tutors = tutors;
    this.eligibility = eligibility;
    this.ai = ai;
    this.mapper = mapper;
  }

  @PreDestroy
  void shutdown() {
    executor.shutdownNow();
  }

  public List<RecommendationResponse> recommend(String studentId, String parent) {
    var student = students.owned(studentId, parent);
    var candidates =
        tutors.candidates(student).stream()
            .filter(t -> eligibility.eligible(student, t))
            .sorted(
                Comparator.<TutorProfile>comparingInt(t -> eligibility.score(student, t))
                    .reversed()
                    .thenComparing(TutorProfile::id))
            .limit(20)
            .toList();
    if (candidates.isEmpty()) return List.of();
    var aliases = new LinkedHashMap<String, TutorProfile>();
    var anonymous = new ArrayList<Map<String, Object>>();
    for (int i = 0; i < candidates.size(); i++) {
      String alias = "candidate-" + i;
      var tutor = candidates.get(i);
      aliases.put(alias, tutor);
      anonymous.add(mapper.anonymousTutor(alias, tutor));
    }
    if (aiSlots.tryAcquire()) {
      Future<List<Map<String, Object>>> future =
          executor.submit(
              () -> {
                try {
                  return ai.rank(mapper.anonymousStudent(student), anonymous);
                } finally {
                  aiSlots.release();
                }
              });
      try {
        return mapper.validate(future.get(9, TimeUnit.SECONDS), aliases);
      } catch (Exception e) {
        future.cancel(true);
        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        LoggerFactory.getLogger(getClass())
            .info("Recommendation fallback: {}", e.getClass().getSimpleName());
      }
    }
    return candidates.stream()
        .limit(5)
        .map(t -> mapper.rules(student, t, eligibility.score(student, t)))
        .toList();
  }
}
