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
      String parentConfirm, String tutorConfirm, Instant confirmExpiresAt, Instant firstLikedAt) {}
  private final JdbcTemplate db;
  public MatchingRepository(JdbcTemplate db) { this.db = db; }
  private State state(java.sql.ResultSet r, int index) throws java.sql.SQLException {
    var expires = r.getTimestamp("expires_at");
    var matched = r.getTimestamp("matched_at");
    var confirmBy = r.getTimestamp("confirm_expires_at");
    var liked = r.getTimestamp("first_liked_at");
    return new State(r.getString("student_id"), r.getString("tutor_id"),
        r.getString("parent_choice"), r.getString("tutor_choice"), r.getString("status"),
        expires == null ? null : expires.toInstant(), matched == null ? null : matched.toInstant(),
        r.getString("parent_confirm"), r.getString("tutor_confirm"),
        confirmBy == null ? null : confirmBy.toInstant(), liked == null ? null : liked.toInstant());
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
    var expired = db.queryForList("SELECT student_id,tutor_id FROM matching_pairs WHERE " + column
        + "=? AND status='EXPIRED' AND confirm_expires_at IS NOT NULL", profile);
    for (var p : expired) notifyEvent((String)p.get("student_id"), (String)p.get("tutor_id"),
        "CONNECTION_EXPIRED", "Cặp đã đóng do hết thời gian xác nhận kết nối.", null);
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
    if (!yes) notifyEvent(student, tutor, "CONNECTION_CLOSED", "Cặp đã đóng. Bạn có thể tiếp tục khám phá hồ sơ phù hợp khác.", parent ? "TUTOR" : "PARENT");
  }
  public void notifyEvent(String student, String tutor, String event, String message, String role) {
    if (role == null || role.equals("PARENT")) db.update(
        "INSERT IGNORE INTO matching_notifications(id,user_id,student_id,tutor_id,event_key,message) "
        + "SELECT UUID(),parent_id,?,?,?,? FROM student_profiles WHERE id=?",
        student,tutor,event,message,student);
    if (role == null || role.equals("TUTOR")) db.update(
        "INSERT IGNORE INTO matching_notifications(id,user_id,student_id,tutor_id,event_key,message) "
        + "SELECT UUID(),user_id,?,?,?,? FROM tutor_profiles WHERE id=?",
        student,tutor,event,message,tutor);
  }
  public boolean hasMatchedTutor(String parent, String tutor) {
    return db.queryForObject("SELECT COUNT(*) FROM matching_pairs p JOIN student_profiles s ON s.id=p.student_id "
        + "WHERE s.parent_id=? AND s.deleted_at IS NULL AND p.tutor_id=? AND p.status='MATCHED' "
        + "AND (p.confirm_expires_at>CURRENT_TIMESTAMP OR (p.parent_confirm='CONFIRM' AND p.tutor_confirm='CONFIRM'))",
        Integer.class,parent,tutor)>0;
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
  public void notifyMatch(String student, String tutor) {
    db.update("INSERT IGNORE INTO matching_notifications(id,user_id,student_id,tutor_id,message) "
        + "SELECT UUID(),parent_id,?,?,'Hai bên đã cùng OK. Bạn có một Match mới!' FROM student_profiles WHERE id=?",
        student,tutor,student);
    db.update("INSERT IGNORE INTO matching_notifications(id,user_id,student_id,tutor_id,message) "
        + "SELECT UUID(),user_id,?,?,'Hai bên đã cùng OK. Bạn có một Match mới!' FROM tutor_profiles WHERE id=?",
        student,tutor,tutor);
  }
  public List<Notification> notifications(String user) {
    return db.query("SELECT id,student_id,tutor_id,message,created_at,event_key FROM matching_notifications WHERE user_id=? AND read_at IS NULL ORDER BY created_at DESC,id LIMIT 100",
        (r,n) -> new Notification(r.getString("id"),r.getString("student_id"),r.getString("tutor_id"),r.getString("message"),r.getTimestamp("created_at").toInstant(),r.getString("event_key")),user);
  }
  public int readNotification(String id, String user) {
    return db.update("UPDATE matching_notifications SET read_at=CURRENT_TIMESTAMP WHERE id=? AND user_id=?",id,user);
  }
}
