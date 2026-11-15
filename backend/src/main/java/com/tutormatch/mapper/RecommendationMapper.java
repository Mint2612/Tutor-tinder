package com.tutormatch.mapper;

import com.tutormatch.dto.recommendation.RecommendationResponse;
import com.tutormatch.model.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class RecommendationMapper {
  private final TutorMapper tutors;

  public RecommendationMapper(TutorMapper tutors) {
    this.tutors = tutors;
  }

  public Map<String, Object> anonymousStudent(StudentProfile s) {
    return Map.of("grade", s.grade(), "preferences", safePreferences(s.survey()),
        "subjects", s.subjects().stream().filter(SUBJECTS::contains).sorted().toList());
  }

  public Map<String, Object> anonymousTutor(String alias, TutorProfile t) {
    return Map.of(
        "tutorId",
        alias,
        "experienceYears",
        t.experience(),
        "teachingApproaches",
        approaches(t.style() + " " + t.methodology()),
        "subjects",
        t.subjects().stream().filter(SUBJECTS::contains).sorted().toList());
  }

  private static final Set<String> SUBJECTS =
      Set.of("Toán", "Ngữ văn", "Tiếng Anh", "Vật lý", "Hóa học", "Sinh học", "Tin học");
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

  public Object safePreferences(Object survey) {
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

  public List<RecommendationResponse> validate(
      List<Map<String, Object>> rankings, Map<String, TutorProfile> candidates) {
    if (rankings == null || rankings.isEmpty() || rankings.size() > 5)
      throw new IllegalArgumentException("Invalid ranking count");
    var seen = new HashSet<String>();
    var results = new ArrayList<RecommendationResponse>();
    for (var r : rankings) {
      if (!(r.get("tutorId") instanceof String id)
          || !candidates.containsKey(id)
          || !seen.add(id)
          || !(r.get("level") instanceof String level)
          || !Set.of("STRONG", "GOOD", "POSSIBLE").contains(level))
        throw new IllegalArgumentException("Invalid ranking");
      String explanation = text(r.get("explanation"), 1500);
      var strengths = texts(r.get("strengths"));
      var concerns = texts(r.get("concerns"));
      results.add(
          new RecommendationResponse(
              tutors.previewResponse(candidates.get(id)),
              level,
              strengths,
              concerns,
              explanation,
              "AI"));
    }
    return List.copyOf(results);
  }

  private List<String> texts(Object value) {
    if (!(value instanceof List<?> list) || list.size() > 5)
      throw new IllegalArgumentException("Invalid explanations");
    return list.stream().map(t -> text(t, 400)).toList();
  }

  private String text(Object value, int max) {
    if (!(value instanceof String s)
        || s.isBlank()
        || s.length() > max
        || s.matches("(?s).*\\d\\s*[%％].*"))
      throw new IllegalArgumentException("Invalid explanation");
    return s;
  }

  public RecommendationResponse rules(StudentProfile s, TutorProfile t, int score) {
    return new RecommendationResponse(
        tutors.previewResponse(t),
        score >= 20 ? "STRONG" : "GOOD",
        List.of(
            "Đáp ứng môn học, lớp, lịch và ngân sách",
            "Có kinh nghiệm giảng dạy " + t.experience() + " năm"),
        List.of("Cần học thử để đánh giá cách dạy có phù hợp với con"),
        "Gợi ý dựa trên các điều kiện học tập và kinh nghiệm; mức độ phù hợp chỉ mang tính tham"
            + " khảo.",
        "RULES");
  }
}
