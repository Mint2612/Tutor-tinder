package com.tutormatch.service;

import com.tutormatch.dto.recommendation.MatchingDtos.Card;
import com.tutormatch.dto.recommendation.RecommendationResponse;
import com.tutormatch.integration.ai.TextEmbedder;
import com.tutormatch.mapper.RecommendationMapper;
import com.tutormatch.mapper.TutorMapper;
import com.tutormatch.matching.CosineSimilarity;
import com.tutormatch.model.*;
import com.tutormatch.repository.*;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/** At most twenty eligible cards, one bounded provider batch, shared per-pair compatibility. */
@Service
public class AiMatchingService {
  private final MatchingService matching;
  private final StudentRepository students;
  private final TutorRepository tutors;
  private final TextEmbedder embedder;
  private final PairAiRepository scores;
  private final RecommendationMapper mapper;
  private final TutorMapper tutorMapper;
  private final ExecutorService executor=Executors.newVirtualThreadPerTaskExecutor();
  private final Semaphore slots=new Semaphore(8);
  private final JsonMapper json=JsonMapper.builder().build();
  public AiMatchingService(MatchingService matching,StudentRepository students,TutorRepository tutors,
      TextEmbedder embedder,PairAiRepository scores,RecommendationMapper mapper,TutorMapper tutorMapper) {
    this.matching=matching; this.students=students; this.tutors=tutors; this.embedder=embedder;
    this.scores=scores; this.mapper=mapper; this.tutorMapper=tutorMapper;
  }
  @PreDestroy void shutdown() { executor.shutdownNow(); }
  /** Số hồ sơ tối đa trong danh sách gợi ý AI (lấy từ tối đa 20 ứng viên đủ điều kiện). */
  static final int SUGGESTION_LIMIT=10;
  private record Pair(StudentProfile s,TutorProfile t) {}
  public List<Card> parent(String student,String owner) {
    var cards=matching.parentFeed(student,owner).cards();
    rank(cards);
    return top(matching.parentFeed(student,owner).cards());
  }
  public List<Card> tutor(String owner) {
    var cards=matching.tutorFeed(owner).cards();
    rank(cards);
    return top(matching.tutorFeed(owner).cards());
  }
  private List<Card> top(List<Card> cards) {
    var featured=matching.featuredTutors();
    return cards.stream().sorted(Comparator.comparing((Card c) -> !featured.contains(c.tutorId()))
        .thenComparing(Comparator.comparingInt(Card::ruleScore).reversed())
        .thenComparing(Card::studentId).thenComparing(Card::tutorId)).limit(SUGGESTION_LIMIT).toList();
  }
  private void rank(List<Card> cards) {
    var pairs=new ArrayList<Pair>();
    for (var c : cards.stream().limit(20).toList()) {
      var s=students.findById(c.studentId());
      var t=tutors.findById(c.tutorId());
      if (s.isPresent() && t.isPresent() && scores.get(s.get(),t.get()).isEmpty()) pairs.add(new Pair(s.get(),t.get()));
    }
    if (pairs.isEmpty() || !slots.tryAcquire()) return;
    Future<List<Double>> task=executor.submit(() -> {
      try {
        var texts=new LinkedHashMap<String,Integer>();
        for (var p : pairs) {
          texts.computeIfAbsent(studentText(p.s()), k -> texts.size());
          texts.computeIfAbsent(tutorText(p.t()), k -> texts.size());
        }
        var vectors=embedder.embed(new ArrayList<>(texts.keySet()),false);
        if (vectors.size()!=texts.size()) throw new IllegalArgumentException("Invalid embedding count");
        int dimension=vectors.getFirst().length;
        for (var v : vectors) {
          if (v.length!=dimension || dimension==0 || dimension>4096) throw new IllegalArgumentException("Invalid embedding size");
          double norm=0;
          for (double x : v) { if (!Double.isFinite(x)) throw new IllegalArgumentException("Invalid embedding number"); norm+=x*x; }
          if (!Double.isFinite(norm) || norm==0) throw new IllegalArgumentException("Invalid embedding norm");
        }
        var values=new ArrayList<Double>();
        for (var p : pairs) {
          double value=CosineSimilarity.calculate(vectors.get(texts.get(studentText(p.s()))),vectors.get(texts.get(tutorText(p.t()))));
          if (!Double.isFinite(value)) throw new IllegalArgumentException("Invalid score");
          values.add(Math.max(-1,Math.min(1,value)));
        }
        return values;
      } finally { slots.release(); }
    });
    try {
      var values=task.get(8500,TimeUnit.MILLISECONDS);
      for (int i=0;i<pairs.size();i++) scores.save(pairs.get(i).s(),pairs.get(i).t(),values.get(i));
    } catch (Exception e) {
      task.cancel(true);
      if (e instanceof InterruptedException) Thread.currentThread().interrupt();
      org.slf4j.LoggerFactory.getLogger(getClass()).info("AI matching uses rules: {}",e.getClass().getSimpleName());
    }
  }
  private String studentText(StudentProfile s) { return json.writeValueAsString(new TreeMap<>(mapper.anonymousStudent(s))); }
  private String tutorText(TutorProfile t) { return json.writeValueAsString(new TreeMap<>(mapper.anonymousTutor("candidate",t))); }
  /** Compatibility route returns previews only; full detail requires Match. */
  public List<RecommendationResponse> suggest(String student,String owner) {
    return parent(student,owner).stream().map(c -> new RecommendationResponse(
        tutorMapper.previewResponse(tutors.findById(c.tutorId()).orElseThrow()),c.level(),c.strengths(),c.concerns(),c.explanation(),
        c.explanation().startsWith("AI") ? "AI" : "RULES")).toList();
  }
}
