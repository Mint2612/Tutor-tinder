package com.tutormatch.repository;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SwipeRepository {
  private final JdbcTemplate db;

  public SwipeRepository(JdbcTemplate db) {
    this.db = db;
  }

  public void save(String student, String tutor, boolean interested) {
    db.update(
        "INSERT INTO swipe_interactions(student_id,tutor_id,interested) VALUES(?,?,?) ON DUPLICATE"
            + " KEY UPDATE interested=VALUES(interested),created_at=CURRENT_TIMESTAMP",
        student,
        tutor,
        interested);
  }

  public List<String> interestedTutorIds(String student) {
    return db.queryForList(
        "SELECT tutor_id FROM swipe_interactions WHERE student_id=? AND interested=TRUE ORDER BY"
            + " created_at DESC,tutor_id",
        String.class,
        student);
  }

  public int removeInterest(String student, String tutor) {
    return db.update(
        "DELETE FROM swipe_interactions WHERE student_id=? AND tutor_id=? AND interested=TRUE",
        student,
        tutor);
  }
}
