package com.tutormatch.repository;

import com.tutormatch.dto.recommendation.MatchingDtos.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MatchingRepository {
  public record State(String studentId, String tutorId, String parentChoice, String tutorChoice,
      String status, Instant expiresAt, Instant matchedAt,
      String parentConfirm, String tutorConfirm, Instant confirmExpiresAt) {}
  private final JdbcTemplate db;
  public MatchingRepository(JdbcTemplate db) { this.db = db; }
  private State state(java.sql.ResultSet r, int index) throws java.sql.SQLException {
    var expires = r.getTimestamp("expires_at");
    var matched = r.getTimestamp("matched_at");
    var confirmBy = r.getTimestamp("confirm_expires_at");
    return new State(r.getString("student_id"), r.getString("tutor_id"),
        r.getString("parent_choice"), r.getString("tutor_choice"), r.getString("status"),
        expires == null ? null : expires.toInstant(), matched == null ? null : matched.toInstant(),
        r.getString("parent_confirm"), r.getString("tutor_confirm"),
        confirmBy == null ? null : confirmBy.toInstant());
  }
  public List<State> forProfile(boolean parent, String profile) {
    return db.query("SELECT * FROM matching_pairs WHERE " + (parent ? "student_id" : "tutor_id") + "=?",
        this::state, profile);
  }
  public void expire(boolean parent, String profile, Instant now) {
    String column = parent ? "student_id" : "tutor_id";
    db.update("UPDATE matching_pairs SET status='EXPIRED' WHERE " + column
        + "=? AND status='OPEN' AND expires_at<=?", profile, Timestamp.from(now));
    db.update("UPDATE matching_pairs SET status='EXPIRED' WHERE " + column
        + "=? AND status='MATCHED' AND confirm_expires_at IS NOT NULL AND confirm_expires_at<=?"
        + " AND NOT (parent_confirm<=>'CONFIRM' AND tutor_confirm<=>'CONFIRM')",
        profile, Timestamp.from(now));
  }
  public State lockPair(String student, String tutor) {
    db.update("INSERT INTO matching_pairs(student_id,tutor_id) VALUES(?,?) ON DUPLICATE KEY UPDATE student_id=VALUES(student_id)", student,tutor);
    return db.query("SELECT * FROM matching_pairs WHERE student_id=? AND tutor_id=? FOR UPDATE",
        this::state, student,tutor).getFirst();
  }
  public Optional<State> findPair(String student, String tutor, boolean lock) {
    return db.query("SELECT * FROM matching_pairs WHERE student_id=? AND tutor_id=?"
        + (lock ? " FOR UPDATE" : ""), this::state, student, tutor).stream().findFirst();
  }
  public void openConfirmation(String student, String tutor, Instant at) {
    db.update("UPDATE matching_pairs SET confirm_expires_at=? WHERE student_id=? AND tutor_id=?",
        Timestamp.from(at.plus(Duration.ofDays(3))), student, tutor);
  }
  public void confirm(boolean parent, String student, String tutor, boolean yes) {
    db.update("UPDATE matching_pairs SET " + (parent ? "parent_confirm" : "tutor_confirm")
        + "=? WHERE student_id=? AND tutor_id=?", yes ? "CONFIRM" : "DECLINE", student, tutor);
    if (!yes) db.update("UPDATE matching_pairs SET status='SKIPPED' WHERE student_id=? AND tutor_id=?",
        student, tutor);
  }
  public Map<String, Object> contacts(String student, String tutor) {
    return db.queryForMap("SELECT pu.email AS parent_email, pu.phone AS parent_phone,"
        + " tu.email AS tutor_email, tu.phone AS tutor_phone FROM student_profiles s"
        + " JOIN users pu ON pu.id=s.parent_id JOIN tutor_profiles t ON t.id=?"
        + " JOIN users tu ON tu.id=t.user_id WHERE s.id=?", tutor, student);
  }
  public void choose(boolean parent, State pair, boolean like, boolean matched, Instant now) {
    String column = parent ? "parent_choice" : "tutor_choice";
    db.update("UPDATE matching_pairs SET " + column + "=?, status=?, "
        + "first_liked_at=COALESCE(first_liked_at,?),expires_at=COALESCE(expires_at,?),matched_at=? "
        + "WHERE student_id=? AND tutor_id=?", like ? "LIKE" : "SKIP",
        !like ? "SKIPPED" : matched ? "MATCHED" : "OPEN", like ? Timestamp.from(now) : null,
        like ? Timestamp.from(now.plus(Duration.ofDays(7))) : null,
        matched ? Timestamp.from(now) : null, pair.studentId(),pair.tutorId());
  }
  public int[] usage(String actor, String profile, LocalDate day) {
    var rows = db.queryForList("SELECT total_used,incoming_used FROM matching_daily_usage WHERE actor=? AND profile_id=? AND usage_day=?",
        actor,profile,java.sql.Date.valueOf(day));
    return rows.isEmpty() ? new int[] {0,0} : new int[] {
        ((Number)rows.getFirst().get("total_used")).intValue(),
        ((Number)rows.getFirst().get("incoming_used")).intValue()};
  }
  public boolean consume(String actor, String profile, LocalDate day, boolean incoming) {
    // Acquire an exclusive lock immediately on an existing counter. INSERT IGNORE would take
    // shared locks that can deadlock when simultaneous requests all upgrade them for UPDATE.
    db.update("INSERT INTO matching_daily_usage(actor,profile_id,usage_day) VALUES(?,?,?) ON DUPLICATE KEY UPDATE total_used=total_used",
        actor,profile,java.sql.Date.valueOf(day));
    return db.update("UPDATE matching_daily_usage SET total_used=total_used+1,incoming_used=incoming_used+? "
        + "WHERE actor=? AND profile_id=? AND usage_day=? AND total_used<10 AND (?=FALSE OR incoming_used<3)",
        incoming ? 1 : 0,actor,profile,java.sql.Date.valueOf(day),incoming)==1;
  }
  public void notifyMatch(String student, String tutor) {
    db.update("INSERT IGNORE INTO matching_notifications(id,user_id,student_id,tutor_id,message) "
        + "SELECT UUID(),parent_id,?,?,'Hai bên đã cùng OK. Bạn có một Match mới!' FROM student_profiles WHERE id=?",
        student,tutor,student);
    db.update("INSERT IGNORE INTO matching_notifications(id,user_id,student_id,tutor_id,message) "
        + "SELECT UUID(),user_id,?,?,'Hai bên đã cùng OK. Bạn có một Match mới!' FROM tutor_profiles WHERE id=?",
        student,tutor,tutor);
  }
  public List<Notification> notifications(String user) {
    return db.query("SELECT id,student_id,tutor_id,message,created_at FROM matching_notifications WHERE user_id=? AND read_at IS NULL ORDER BY created_at DESC,id LIMIT 100",
        (r,n) -> new Notification(r.getString("id"),r.getString("student_id"),r.getString("tutor_id"),r.getString("message"),r.getTimestamp("created_at").toInstant()),user);
  }
  public int readNotification(String id, String user) {
    return db.update("UPDATE matching_notifications SET read_at=CURRENT_TIMESTAMP WHERE id=? AND user_id=?",id,user);
  }
}