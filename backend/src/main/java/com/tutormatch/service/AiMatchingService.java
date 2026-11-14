package com.tutormatch.service;

import static com.tutormatch.exception.BusinessException.require;

import com.tutormatch.dto.recommendation.RecommendationResponse;
import com.tutormatch.integration.ai.TextEmbedder;
import com.tutormatch.mapper.RecommendationMapper;
import com.tutormatch.mapper.TutorMapper;
import com.tutormatch.matching.CosineSimilarity;
import com.tutormatch.model.StudentProfile;
import com.tutormatch.model.TutorProfile;
import com.tutormatch.repository.TutorRepository;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Bộ lọc cứng (tối đa 100) → embedding + cosine → top 5. Không dùng @Transactional: có gọi mạng. */
@Service
public class AiMatchingService {
  static final int POOL = 100;
  static final int TOP = 5;
  private static final Logger LOG = LoggerFactory.getLogger(AiMatchingService.class);
  private final StudentService students;
  private final TutorRepository tutors;
  private final EligibilityPolicy eligibility;
  private final TextEmbedder embedder;
  private final TutorMapper tutorMapper;
  private final RecommendationMapper mapper;

  public AiMatchingService(
      StudentService students,
      TutorRepository tutors,
      EligibilityPolicy eligibility,
      TextEmbedder embedder,
      TutorMapper tutorMapper,
      RecommendationMapper mapper) {
    this.students = students;
    this.tutors = tutors;
    this.eligibility = eligibility;
    this.embedder = embedder;
    this.tutorMapper = tutorMapper;
    this.mapper = mapper;
  }

  public List<RecommendationResponse> suggest(String studentId, String parent) {
    var student = students.owned(studentId, parent);
    require(student.consentAt() != null, 400, "Cần đồng ý lưu hồ sơ trước khi dùng gợi ý AI.");
    require(student.visible(), 400, "Hồ sơ đang tạm ẩn.");
    // Bước 1: bộ lọc cứng, rồi giữ 100 gia sư có điểm luật cao nhất.
    var pool =
        tutors.candidates(student).stream()
            .filter(t -> eligibility.eligible(student, t))
            .sorted(
                Comparator.<TutorProfile>comparingInt(t -> eligibility.score(student, t))
                    .reversed()
                    .thenComparing(TutorProfile::id))
            .limit(POOL)
            .toList();
    if (pool.isEmpty()) return List.of();
    try {
      return rank(student, pool);
    } catch (Exception e) {
      LOG.warn("AI ranking fallback: {}", e.getClass().getSimpleName());
      return pool.stream()
          .limit(TOP)
          .map(t -> mapper.rules(student, t, eligibility.score(student, t)))
          .toList();
    }
  }

  private List<RecommendationResponse> rank(StudentProfile student, List<TutorProfile> pool)
      throws Exception {
    String model = embedder.model();
    var vectors =
        new HashMap<>(tutors.embeddings(pool.stream().map(TutorProfile::id).toList(), model));
    // Bước 2a: chỉ gia sư chưa có vector mới gọi API (một lần rồi lưu).
    var missing = pool.stream().filter(t -> !vectors.containsKey(t.id())).toList();
    if (!missing.isEmpty()) {
      var created = embedder.embed(missing.stream().map(this::tutorText).toList(), false);
      for (int i = 0; i < missing.size(); i++) {
        var t = missing.get(i);
        vectors.put(t.id(), created.get(i));
        tutors.saveEmbedding(t.id(), t.revision(), created.get(i), model);
      }
    }
    // Bước 2b: vector phía người tìm, rồi so cosine ngay trên server.
    double[] query = embedder.embed(List.of(studentText(student)), true).get(0);
    var scores = new HashMap<String, Double>();
    for (var t : pool) scores.put(t.id(), CosineSimilarity.calculate(query, vectors.get(t.id())));
    var top =
        pool.stream()
            .sorted(
                Comparator.<TutorProfile>comparingDouble(t -> scores.get(t.id()))
                    .reversed()
                    .thenComparing(TutorProfile::id))
            .limit(TOP)
            .toList();
    var out = new ArrayList<RecommendationResponse>();
    for (int i = 0; i < top.size(); i++) out.add(response(top.get(i), i));
    return out;
  }

  /** Nhãn theo thứ hạng, không hiển thị điểm hay phần trăm (xem README mục 5). */
  private RecommendationResponse response(TutorProfile t, int rank) {
    return new RecommendationResponse(
        tutorMapper.publicResponse(t),
        rank < 2 ? "STRONG" : "GOOD",
        List.of(
            "Đáp ứng môn học, lớp, lịch và ngân sách",
            "Mô tả cách dạy gần với mô tả nhu cầu của con"),
        List.of("Cần học thử để đánh giá cách dạy có phù hợp với con"),
        "Gợi ý bằng AI dựa trên độ tương đồng giữa mô tả nhu cầu và cách dạy; chỉ mang tính tham khảo.",
        "AI");
  }

  /** Ẩn danh: không tên, tỉnh, liên hệ. Mô tả tự do chỉ gửi khi không chứa SĐT/email. */
  private String studentText(StudentProfile s) {
    var prefs =
        s.survey().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> MatchingService.LEARNING.get(e.getValue()))
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.joining(", "));
    String needs = ContactInfo.find(s.needs()).isPresent() ? "" : s.needs();
    return "Con cần gia sư như thế nào? Học sinh lớp "
        + s.grade()
        + ", cần học "
        + String.join(", ", s.subjects())
        + ". Cách học phù hợp: "
        + prefs
        + ". "
        + needs;
  }

  private String tutorText(TutorProfile t) {
    // Không dùng achievements (tự khai, không được dùng để xếp hạng).
    return "Gia sư dạy "
        + String.join(", ", t.subjects())
        + " cho lớp "
        + t.grades().stream().map(String::valueOf).collect(Collectors.joining(", "))
        + ", "
        + t.experience()
        + " năm kinh nghiệm. Phong cách: "
        + t.style()
        + ". Phương pháp: "
        + t.methodology()
        + ". Học vấn: "
        + t.background();
  }
}