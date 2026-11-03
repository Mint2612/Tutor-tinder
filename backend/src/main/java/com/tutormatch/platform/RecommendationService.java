package com.tutormatch.platform;

import static com.tutormatch.platform.ProfileData.*;

import java.util.*;
import java.util.concurrent.*;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {
  final ProfileService profiles;
  final LlmRanker ai;
  private final Semaphore aiSlots = new Semaphore(8);
  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  @jakarta.annotation.PreDestroy
  void shutdown() {
    executor.shutdownNow();
  }

  public RecommendationService(ProfileService profiles, LlmRanker ai) {
    this.profiles = profiles;
    this.ai = ai;
  }

  @SuppressWarnings("unchecked")
  public boolean eligible(Map<String, Object> s, Map<String, Object> t) {
    return "APPROVED".equals(t.get("workflow_state"))
        && "APPROVED".equals(t.get("verification_status"))
        && ((Number) t.get("hourly_rate")).intValue()
            <= ((Number) s.get("max_hourly_rate")).intValue()
        && ((List<Number>) t.get("grades"))
            .stream().anyMatch(g -> g.intValue() == ((Number) s.get("grade")).intValue())
        && ((List<String>) t.get("modes")).contains(s.get("mode"))
        && (s.get("mode").equals("ONLINE")
            || s.get("province").toString().equalsIgnoreCase(t.get("province").toString()))
        && ((List<String>) t.get("subjects")).containsAll((List<String>) s.get("subjects"))
        && !Collections.disjoint(
            (List<String>) s.get("availability"), (List<String>) t.get("availability"));
  }

  public List<Map<String, Object>> recommend(String studentId, String parent) {
    var s = profiles.ownedStudent(studentId, parent);
    // SQL prefilters bound expensive relation hydration; remaining hard constraints are checked
    // before top-20.
    var all =
        profiles
            .jdbc()
            .queryForList(
                """
                SELECT t.* FROM tutor_profiles t JOIN users u ON u.id=t.user_id
                WHERE t.workflow_state='APPROVED' AND t.verification_status='APPROVED' AND t.visible=TRUE
                AND u.email_verified=TRUE AND u.phone_verified=TRUE AND t.hourly_rate<=?
                AND EXISTS(SELECT 1 FROM tutor_grades g WHERE g.tutor_id=t.id AND g.grade=?)
                AND EXISTS(SELECT 1 FROM tutor_modes m WHERE m.tutor_id=t.id AND m.mode=?)
                AND (?='ONLINE' OR LOWER(t.province)=LOWER(?))
                AND NOT EXISTS(SELECT 1 FROM swipe_interactions w WHERE w.student_id=? AND w.tutor_id=t.id)
                ORDER BY t.experience_years DESC,t.hourly_rate,t.id
                """,
                s.get("max_hourly_rate"),
                s.get("grade"),
                s.get("mode"),
                s.get("mode"),
                s.get("province"),
                studentId);
    var candidates =
        all.stream()
            .map(profiles::tutorView)
            .filter(t -> eligible(s, t))
            .sorted(
                Comparator.<Map<String, Object>>comparingInt(t -> score(s, t))
                    .reversed()
                    .thenComparing(t -> t.get("id").toString()))
            .limit(20)
            .toList();
    if (candidates.isEmpty()) return List.of();
    var pseudonyms = new LinkedHashMap<String, Map<String, Object>>();
    var anonymous = new ArrayList<Map<String, Object>>();
    for (int i = 0; i < candidates.size(); i++) {
      var t = candidates.get(i);
      String alias = "candidate-" + i;
      pseudonyms.put(alias, t);
      anonymous.add(
          Map.of(
              "tutorId",
              alias,
              "experienceYears",
              t.get("experience_years"),
              "teachingApproaches",
              approaches(t.get("teaching_style").toString() + " " + t.get("teaching_description")),
              "subjects",
              ((List<?>) t.get("subjects")).stream().filter(SUBJECTS::contains).toList()));
    }
    // Raw needs, nickname, location, schedule and free-text survey answers never leave the backend.
    var student =
        Map.<String, Object>of(
            "grade", s.get("grade"), "preferences", safePreferences(s.get("survey")));
    boolean acquired = aiSlots.tryAcquire();
    if (acquired) {
      Future<List<Map<String, Object>>> future =
          executor.submit(
              () -> {
                try {
                  return ai.rank(student, anonymous);
                } finally {
                  aiSlots.release();
                }
              });
      try {
        return validate(future.get(9, TimeUnit.SECONDS), pseudonyms);
      } catch (Exception e) {
        future.cancel(true);
        LoggerFactory.getLogger(getClass())
            .info("Recommendation fallback: {}", e.getClass().getSimpleName());
      }
    }
    return candidates.stream()
        .limit(5)
        .map(
            t -> {
              var result = new LinkedHashMap<String, Object>();
              result.put("tutor", publicView(t));
              result.put("level", score(s, t) >= 20 ? "STRONG" : "GOOD");
              result.put(
                  "strengths",
                  List.of(
                      "Đáp ứng môn học, lớp, lịch và ngân sách",
                      "Có kinh nghiệm giảng dạy " + t.get("experience_years") + " năm"));
              result.put(
                  "concerns", List.of("Cần học thử để đánh giá cách dạy có phù hợp với con"));
              result.put(
                  "explanation",
                  "Gợi ý dựa trên các điều kiện học tập và kinh nghiệm; mức độ phù hợp chỉ mang"
                      + " tính tham khảo.");
              result.put("source", "RULES");
              return (Map<String, Object>) result;
            })
        .toList();
  }

  @SuppressWarnings("unchecked")
  int score(Map<String, Object> s, Map<String, Object> t) {
    int overlap = 0;
    for (var slot : (List<String>) s.get("availability"))
      if (((List<String>) t.get("availability")).contains(slot)) overlap++;
    return Math.min(15, ((Number) t.get("experience_years")).intValue())
        + overlap * 3
        + (((Number) t.get("hourly_rate")).intValue()
                <= ((Number) s.get("max_hourly_rate")).intValue() * 0.8
            ? 3
            : 0);
  }

  static final Set<String> SAFE_VALUES =
      Set.of(
          "visual",
          "practice",
          "discussion",
          "step_by_step",
          "patient",
          "structured",
          "interactive",
          "confidence",
          "foundation",
          "exam",
          "homework",
          "attention",
          "motivation",
          "understanding",
          "independent",
          "support",
          "short",
          "long",
          "balanced",
          "feedback",
          "examples",
          "review",
          "challenge");

  Object safePreferences(Object survey) {
    if (!(survey instanceof Map<?, ?> m)) return List.of();
    return m.values().stream()
        .filter(v -> v instanceof String && SAFE_VALUES.contains(v))
        .sorted(Comparator.comparing(Object::toString))
        .toList();
  }

  List<String> approaches(String text) {
    String s = text.toLowerCase(Locale.ROOT);
    var tags = new ArrayList<String>();
    if (s.contains("kiên nhẫn") || s.contains("patient")) tags.add("patient");
    if (s.contains("thực hành") || s.contains("practice")) tags.add("practice");
    if (s.contains("từng bước") || s.contains("step")) tags.add("step_by_step");
    if (s.contains("trực quan") || s.contains("visual")) tags.add("visual");
    if (s.contains("tương tác") || s.contains("interactive")) tags.add("interactive");
    return tags;
  }

  Map<String, Object> publicView(Map<String, Object> t) {
    var v = new LinkedHashMap<>(t);
    v.remove("user_id");
    v.remove("rejection_reason");
    return v;
  }

  List<Map<String, Object>> validate(
      List<Map<String, Object>> rankings, Map<String, Map<String, Object>> candidates) {
    if (rankings == null || rankings.isEmpty() || rankings.size() > 5)
      throw new IllegalArgumentException("Invalid ranking count");
    var seen = new HashSet<String>();
    var result = new ArrayList<Map<String, Object>>();
    for (var r : rankings) {
      String id = (String) r.get("tutorId");
      if (!candidates.containsKey(id)
          || !seen.add(id)
          || !Set.of("STRONG", "GOOD", "POSSIBLE").contains(r.get("level")))
        throw new IllegalArgumentException("Invalid ranking");
      checkText(r.get("explanation"), 1500);
      for (String field : List.of("strengths", "concerns")) {
        if (!(r.get(field) instanceof List<?> values) || values.size() > 5)
          throw new IllegalArgumentException("Invalid explanations");
        for (Object text : values) checkText(text, 400);
      }
      result.add(
          Map.of(
              "tutor",
              publicView(candidates.get(id)),
              "level",
              r.get("level"),
              "strengths",
              r.get("strengths"),
              "concerns",
              r.get("concerns"),
              "explanation",
              r.get("explanation"),
              "source",
              "AI"));
    }
    return result;
  }

  void checkText(Object text, int max) {
    if (!(text instanceof String s)
        || s.isBlank()
        || s.length() > max
        || s.matches("(?s).*\\d\\s*[%％].*"))
      throw new IllegalArgumentException("Invalid explanation");
  }
}
