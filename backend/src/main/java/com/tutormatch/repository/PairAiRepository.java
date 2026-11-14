package com.tutormatch.repository;

import com.tutormatch.mapper.RecommendationMapper;
import com.tutormatch.model.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** One validated compatibility value per pair, shared by both feeds. */
@Repository
public class PairAiRepository {
  public record Score(double score,String level) {}
  private final JdbcTemplate db;
  private final RecommendationMapper mapper;
  private final String model;
  private final JsonMapper json=JsonMapper.builder().build();
  public PairAiRepository(JdbcTemplate db,RecommendationMapper mapper,
      @org.springframework.beans.factory.annotation.Value("${GEMINI_EMBED_MODEL:gemini-embedding-001}") String model) {
    this.db=db; this.mapper=mapper; this.model=model;
  }
  public String fingerprint(StudentProfile s,TutorProfile t) {
    try {
      String data=json.writeValueAsString(List.of(model,"learning-v1",new TreeMap<>(mapper.anonymousStudent(s)),
          new TreeMap<>(mapper.anonymousTutor("pair",t))));
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) { throw new IllegalStateException(e); }
  }
  public Optional<Score> get(StudentProfile s,TutorProfile t) {
    return db.query("SELECT score,level FROM pair_ai_scores WHERE student_id=? AND tutor_id=? AND fingerprint=?",
        (r,n) -> new Score(r.getDouble("score"),r.getString("level")),s.id(),t.id(),fingerprint(s,t)).stream().findFirst();
  }
  public Score save(StudentProfile s,TutorProfile t,double score) {
    String level=score>=0.80 ? "STRONG" : score>=0.60 ? "GOOD" : "POSSIBLE";
    // Concurrent requests with identical inputs retain the first shared value.
    db.update("INSERT INTO pair_ai_scores(student_id,tutor_id,fingerprint,score,level) VALUES(?,?,?,?,?) "
        + "ON DUPLICATE KEY UPDATE score=IF(fingerprint=VALUES(fingerprint),score,VALUES(score)),"
        + "level=IF(fingerprint=VALUES(fingerprint),level,VALUES(level)),fingerprint=VALUES(fingerprint)",
        s.id(),t.id(),fingerprint(s,t),score,level);
    return get(s,t).orElseThrow();
  }
}
