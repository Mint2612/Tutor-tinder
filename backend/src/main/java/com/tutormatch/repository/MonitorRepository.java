package com.tutormatch.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Aggregate counts for the admin monitor. Read-only; no row-level personal data. */
@Repository
public class MonitorRepository {
  /** Vietnam is UTC+7 without daylight saving; day buckets follow the users' calendar. */
  private static final String LOCAL_DAY = "DATE(CONVERT_TZ(%s,'+00:00','+07:00'))";
  private final JdbcTemplate db;

  public MonitorRepository(JdbcTemplate db) {
    this.db = db;
  }

  private long count(String sql, Object... args) {
    Long value = db.queryForObject(sql, Long.class, args);
    return value == null ? 0 : value;
  }

  private Map<String, Long> grouped(String sql) {
    var out = new HashMap<String, Long>();
    db.query(sql, rs -> {
      out.put(rs.getString(1), rs.getLong(2));
    });
    return out;
  }

  public Map<String, Long> accountsByRole() {
    return grouped("SELECT role, COUNT(*) FROM users WHERE deleted_at IS NULL GROUP BY role");
  }

  public long newAccountsSince(int days) {
    return count(
        "SELECT COUNT(*) FROM users WHERE deleted_at IS NULL AND created_at >= TIMESTAMPADD(DAY,?,UTC_TIMESTAMP())",
        -days);
  }

  public long activeSessions() {
    return count("SELECT COUNT(*) FROM auth_sessions WHERE expires_at > CURRENT_TIMESTAMP");
  }

  public Map<String, Long> tutorsByState() {
    return grouped(
        "SELECT workflow_state, COUNT(*) FROM tutor_profiles WHERE deleted_at IS NULL GROUP BY workflow_state");
  }

  public long hiddenApprovedTutors() {
    return count(
        "SELECT COUNT(*) FROM tutor_profiles WHERE deleted_at IS NULL AND workflow_state='APPROVED' AND visible=FALSE");
  }

  public long pausedApprovedTutors() {
    return count(
        "SELECT COUNT(*) FROM tutor_profiles WHERE deleted_at IS NULL AND workflow_state='APPROVED' AND accepting_students=FALSE");
  }

  public Instant oldestPending() {
    Timestamp t =
        db.queryForObject(
            "SELECT MIN(submitted_at) FROM tutor_profiles WHERE deleted_at IS NULL AND workflow_state='PENDING_REVIEW'",
            Timestamp.class);
    return t == null ? null : t.toInstant();
  }

  public record Waiting(String id, String displayName, Instant submittedAt) {}

  public List<Waiting> waitingTutors(int limit) {
    return db.query(
        "SELECT id, display_name, submitted_at FROM tutor_profiles WHERE deleted_at IS NULL"
            + " AND workflow_state='PENDING_REVIEW' ORDER BY submitted_at, id LIMIT ?",
        (rs, n) ->
            new Waiting(
                rs.getString(1),
                rs.getString(2),
                rs.getTimestamp(3) == null ? null : rs.getTimestamp(3).toInstant()),
        limit);
  }

  public long students() {
    return count("SELECT COUNT(*) FROM student_profiles WHERE deleted_at IS NULL");
  }

  public long hiddenStudents() {
    return count("SELECT COUNT(*) FROM student_profiles WHERE deleted_at IS NULL AND visible=FALSE");
  }

  public Map<String, Long> pairsByStatus() {
    return grouped("SELECT status, COUNT(*) FROM matching_pairs GROUP BY status");
  }

  public long waitingPairs() {
    return count(
        "SELECT COUNT(*) FROM matching_pairs WHERE status='OPEN' AND expires_at > CURRENT_TIMESTAMP"
            + " AND (parent_choice='LIKE' OR tutor_choice='LIKE')");
  }

  public long connectedPairs() {
    return count(
        "SELECT COUNT(*) FROM matching_pairs WHERE status='MATCHED' AND parent_confirm='CONFIRM' AND tutor_confirm='CONFIRM'");
  }

  public long awaitingConfirmation() {
    return count(
        "SELECT COUNT(*) FROM matching_pairs WHERE status='MATCHED' AND confirm_expires_at > CURRENT_TIMESTAMP"
            + " AND NOT (COALESCE(parent_confirm,'')='CONFIRM' AND COALESCE(tutor_confirm,'')='CONFIRM')");
  }

  public long confirmationExpired() {
    return count(
        "SELECT COUNT(*) FROM matching_pairs WHERE status='MATCHED' AND confirm_expires_at <= CURRENT_TIMESTAMP"
            + " AND NOT (COALESCE(parent_confirm,'')='CONFIRM' AND COALESCE(tutor_confirm,'')='CONFIRM')");
  }

  public Map<String, Long> trialsByStatus() {
    return grouped("SELECT status, COUNT(*) FROM trial_schedules GROUP BY status");
  }

  public long reviews() {
    return count("SELECT COUNT(*) FROM trial_reviews");
  }

  public Double averageRating() {
    return db.queryForObject("SELECT AVG(rating) FROM trial_reviews", Double.class);
  }

  public long unreadNotifications() {
    return count("SELECT COUNT(*) FROM matching_notifications WHERE read_at IS NULL");
  }

  /**
   * Events per local day over the last {@code days} days. {@code table}, {@code column} and {@code
   * where} are fixed strings from {@link com.tutormatch.service.MonitorService}, never user input.
   */
  public Map<LocalDate, Long> perDay(String table, String column, String where, int days) {
    var out = new HashMap<LocalDate, Long>();
    String day = LOCAL_DAY.formatted(column);
    db.query(
        "SELECT "
            + day
            + ", COUNT(*) FROM "
            + table
            + " WHERE "
            + column
            + " >= TIMESTAMPADD(DAY,?,UTC_TIMESTAMP())"
            + (where == null ? "" : " AND " + where)
            + " GROUP BY "
            + day,
        rs -> {
          out.put(rs.getDate(1).toLocalDate(), rs.getLong(2));
        },
        -(days + 1));
    return out;
  }

  public boolean ping() {
    try {
      return Integer.valueOf(1).equals(db.queryForObject("SELECT 1", Integer.class));
    } catch (RuntimeException e) {
      return false;
    }
  }
}
