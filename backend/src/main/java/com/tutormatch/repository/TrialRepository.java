package com.tutormatch.repository;

import com.tutormatch.dto.lesson.TrialDtos.Trial;
import com.tutormatch.model.Account;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class TrialRepository {
  private final JdbcTemplate db;
  private final JsonMapper json = JsonMapper.builder().build();
  public TrialRepository(JdbcTemplate db) { this.db=db; }
  private Trial map(ResultSet r, int n) throws SQLException {
    var slots = new ArrayList<Instant>();
    for (var slot : json.readTree(r.getString("slots"))) slots.add(Instant.parse(slot.asText()));
    var chosen = r.getTimestamp("chosen_at");
    return new Trial(r.getString("id"),r.getString("student_id"),r.getString("tutor_id"),
        r.getString("proposer_role"),slots,r.getString("status"),r.getInt("version"),
        chosen==null ? null : chosen.toInstant(),r.getTimestamp("expires_at").toInstant(),
        r.getString("cancellation_reason"),r.getString("mode"));
  }
  public Optional<Trial> find(String id) {
    return db.query("SELECT * FROM trial_schedules WHERE id=?",this::map,id).stream().findFirst();
  }
  public Optional<Trial> lock(String id) {
    return db.query("SELECT * FROM trial_schedules WHERE id=? FOR UPDATE",this::map,id).stream().findFirst();
  }
  public List<Trial> pair(String student,String tutor) {
    return db.query("SELECT * FROM trial_schedules WHERE student_id=? AND tutor_id=? ORDER BY created_at DESC,id FOR UPDATE",this::map,student,tutor);
  }
  public boolean legacyActive(String student,String tutor) {
    return !db.queryForList("SELECT id FROM lesson_requests WHERE student_id=? AND tutor_id=? AND "
        + "(status='ACCEPTED' OR (status='PENDING' AND expires_at>CURRENT_TIMESTAMP)) FOR UPDATE",student,tutor).isEmpty();
  }
  public List<Trial> list(Account user) {
    return db.query("SELECT q.* FROM trial_schedules q JOIN student_profiles s ON s.id=q.student_id "
        + "JOIN tutor_profiles t ON t.id=q.tutor_id WHERE "
        + (user.role().equals("PARENT") ? "s.parent_id=?" : "t.user_id=?")
        + " AND s.deleted_at IS NULL AND t.deleted_at IS NULL ORDER BY q.created_at DESC,q.id",this::map,user.id());
  }
  public void create(String id,String student,String tutor,String role,List<Instant> slots,Instant expires,String mode) {
    db.update("INSERT INTO trial_schedules(id,student_id,tutor_id,proposer_role,slots,expires_at,mode) VALUES(?,?,?,?,?,?,?)",
        id,student,tutor,role,json.writeValueAsString(slots.stream().map(Instant::toString).toList()),Timestamp.from(expires),mode);
  }
  public void counter(String id,String role,List<Instant> slots,Instant expires) {
    db.update("UPDATE trial_schedules SET proposer_role=?,slots=?,expires_at=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",
        role,json.writeValueAsString(slots.stream().map(Instant::toString).toList()),Timestamp.from(expires),id);
  }
  public void accept(String id,Instant slot) {
    db.update("UPDATE trial_schedules SET status='CONFIRMED',chosen_at=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",Timestamp.from(slot),id);
  }
  public void close(String id,String status,String reason) {
    db.update("UPDATE trial_schedules SET status=?,cancellation_reason=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",status,reason,id);
  }
}
