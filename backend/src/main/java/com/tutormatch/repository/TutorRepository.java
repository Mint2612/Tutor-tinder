package com.tutormatch.repository;

import static com.tutormatch.mapper.JsonFields.json;

import com.tutormatch.dto.profile.TutorRequest;
import com.tutormatch.mapper.TutorMapper;
import com.tutormatch.model.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TutorRepository {
  private static final String PUBLIC =
      " t.workflow_state='APPROVED' AND t.verification_status='APPROVED' AND t.visible=TRUE AND"
          + " u.email_verified=TRUE ";
  private final JdbcTemplate db;
  private final NamedParameterJdbcTemplate named;
  private final TutorMapper mapper;

  public TutorRepository(JdbcTemplate db, TutorMapper mapper) {
    this.db = db;
    this.named = new NamedParameterJdbcTemplate(db);
    this.mapper = mapper;
  }

  public Optional<TutorProfile> findByUser(String user) {
    return hydrate(db.queryForList("SELECT * FROM tutor_profiles WHERE user_id=?", user)).stream()
        .findFirst();
  }

  public Optional<TutorProfile> lockByUser(String user) {
    return hydrate(db.queryForList("SELECT * FROM tutor_profiles WHERE user_id=? FOR UPDATE", user))
        .stream()
        .findFirst();
  }

  public Optional<TutorProfile> findById(String id) {
    return hydrate(db.queryForList("SELECT * FROM tutor_profiles WHERE id=?", id)).stream()
        .findFirst();
  }

  public Optional<TutorProfile> lockById(String id) {
    return hydrate(db.queryForList("SELECT * FROM tutor_profiles WHERE id=? FOR UPDATE", id))
        .stream()
        .findFirst();
  }

  public Optional<TutorProfile> findPublic(String id) {
    return hydrate(
            db.queryForList(
                "SELECT t.* FROM tutor_profiles t JOIN users u ON u.id=t.user_id WHERE t.id=? AND"
                    + PUBLIC,
                id))
        .stream()
        .findFirst();
  }

  public List<TutorProfile> findAll() {
    return hydrate(
        db.queryForList("SELECT * FROM tutor_profiles ORDER BY workflow_state,display_name,id"));
  }

  public List<TutorProfile> findPublicByIds(List<String> ids) {
    if (ids.isEmpty()) return List.of();
    return hydrate(
        named.queryForList(
            "SELECT t.* FROM tutor_profiles t JOIN users u ON u.id=t.user_id WHERE t.id IN (:ids)"
                + " AND"
                + PUBLIC,
            Map.of("ids", ids)));
  }

  public List<TutorProfile> candidates(StudentProfile s) {
    return hydrate(
        db.queryForList(
            "SELECT t.* FROM tutor_profiles t JOIN users u ON u.id=t.user_id WHERE"
                + PUBLIC
                + "AND t.hourly_rate<=? AND EXISTS(SELECT 1 FROM tutor_grades g WHERE"
                + " g.tutor_id=t.id AND g.grade=?) AND EXISTS(SELECT 1 FROM tutor_modes m WHERE"
                + " m.tutor_id=t.id AND m.mode=?) AND (?='ONLINE' OR LOWER(t.province)=LOWER(?))"
                + " AND NOT EXISTS(SELECT 1 FROM swipe_interactions w WHERE w.student_id=? AND"
                + " w.tutor_id=t.id) AND NOT EXISTS(SELECT 1 FROM matching_pairs p WHERE p.student_id=? AND p.tutor_id=t.id"
                + " AND (p.status<>'OPEN' OR p.parent_choice IS NOT NULL OR p.expires_at<=CURRENT_TIMESTAMP))"
                + " ORDER BY t.experience_years DESC,t.hourly_rate,t.id",
            s.budget(),
            s.grade(),
            s.mode(),
            s.mode(),
            s.province(),
            s.id(),s.id()));
  }

  /** Hydrate relations in three bounded queries rather than one query per tutor/relationship. */
  private List<TutorProfile> hydrate(List<Map<String, Object>> rows) {
    if (rows.isEmpty()) return List.of();
    var ids = rows.stream().map(r -> r.get("id").toString()).toList();
    var subjects = new HashMap<String, List<String>>();
    var grades = new HashMap<String, List<Integer>>();
    var modes = new HashMap<String, List<String>>();
    named
        .queryForList(
            "SELECT tutor_id,subject FROM tutor_subjects WHERE tutor_id IN (:ids) ORDER BY subject",
            Map.of("ids", ids))
        .forEach(
            r ->
                subjects
                    .computeIfAbsent(r.get("tutor_id").toString(), k -> new ArrayList<>())
                    .add(r.get("subject").toString()));
    named
        .queryForList(
            "SELECT tutor_id,grade FROM tutor_grades WHERE tutor_id IN (:ids) ORDER BY grade",
            Map.of("ids", ids))
        .forEach(
            r ->
                grades
                    .computeIfAbsent(r.get("tutor_id").toString(), k -> new ArrayList<>())
                    .add(((Number) r.get("grade")).intValue()));
    named
        .queryForList(
            "SELECT tutor_id,mode FROM tutor_modes WHERE tutor_id IN (:ids) ORDER BY mode",
            Map.of("ids", ids))
        .forEach(
            r ->
                modes
                    .computeIfAbsent(r.get("tutor_id").toString(), k -> new ArrayList<>())
                    .add(r.get("mode").toString()));
    return rows.stream()
        .map(
            r -> {
              String id = r.get("id").toString();
              return mapper.fromRow(
                  r,
                  subjects.getOrDefault(id, List.of()),
                  grades.getOrDefault(id, List.of()),
                  modes.getOrDefault(id, List.of()));
            })
        .toList();
  }

  public void save(String id, String user, TutorRequest t, boolean create) {
    if (create)
      db.update(
          "INSERT INTO"
              + " tutor_profiles(id,user_id,province,hourly_rate,teaching_description,availability,survey)"
              + " VALUES(?,?,?,?,?,?,'{}')",
          id,
          user,
          t.province().trim(),
          t.hourlyRate(),
          t.methodology().trim(),
          json(t.availability()));
    db.update(
        "UPDATE tutor_profiles SET"
            // photo_url is deliberately absent: the photo changes only through setPhoto().
            // Profile edits require another review before public display (see withdrawReview below).
            // The public name is the account's display name; the name typed here is the legal one.
            + " display_name=(SELECT full_name FROM users WHERE id=?),legal_name=?,province=?,hourly_rate=?,teaching_description=?,background=?,experience_years=?,teaching_style=?,availability=?,achievements=?,revision=revision+1"
            + " WHERE id=?",
        user,
        t.fullName().trim(),
        t.province().trim(),
        t.hourlyRate(),
        t.methodology().trim(),
        t.background().trim(),
        t.experience(),
        t.style().trim(),
        json(t.availability()),
        t.achievements() == null || t.achievements().isBlank() ? null : t.achievements().trim(),
        id);
    withdrawReview(id);
    db.update("DELETE FROM tutor_subjects WHERE tutor_id=?", id);
    db.update("DELETE FROM tutor_grades WHERE tutor_id=?", id);
    db.update("DELETE FROM tutor_modes WHERE tutor_id=?", id);
    for (var s : t.subjects()) db.update("INSERT INTO tutor_subjects VALUES(?,?)", id, s);
    for (var g : t.grades()) db.update("INSERT INTO tutor_grades VALUES(?,?)", id, g);
    for (var m : t.modes()) db.update("INSERT INTO tutor_modes VALUES(?,?)", id, m);
  }

  /** Photo changes require another review. */
  public void setPhoto(String id, String url) {
    db.update("UPDATE tutor_profiles SET photo_url=?,revision=revision+1 WHERE id=?", url, id);
    withdrawReview(id);
  }

  private void withdrawReview(String id) {
    db.update(
        "UPDATE tutor_profiles SET workflow_state='DRAFT',verification_status='PENDING',"
            + "verified_at=NULL,rejection_reason=NULL,submission_id=NULL,submitted_at=NULL"
            + " WHERE id=? AND workflow_state IN ('APPROVED','PENDING_REVIEW')", id);
  }
}
