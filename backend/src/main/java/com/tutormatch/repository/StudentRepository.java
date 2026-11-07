package com.tutormatch.repository;

import static com.tutormatch.mapper.JsonFields.json;

import com.tutormatch.dto.profile.StudentRequest;
import com.tutormatch.mapper.StudentMapper;
import com.tutormatch.model.StudentProfile;
import com.tutormatch.model.TutorProfile;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StudentRepository {
  private final JdbcTemplate db;
  private final StudentMapper mapper;

  public StudentRepository(JdbcTemplate db, StudentMapper mapper) {
    this.db = db;
    this.mapper = mapper;
  }

  public Optional<StudentProfile> findOwned(String id, String parent) {
    return findOwned(id, parent, false);
  }

  public boolean lockActiveParent(String parent) {
    return !db.queryForList("SELECT id FROM users WHERE id=? AND role='PARENT' AND deleted_at IS NULL FOR UPDATE",parent).isEmpty();
  }

  public Optional<StudentProfile> lockOwned(String id, String parent) {
    return findOwned(id, parent, true);
  }

  private Optional<StudentProfile> findOwned(String id, String parent, boolean lock) {
    return db
        .queryForList(
            "SELECT * FROM student_profiles WHERE id=? AND parent_id=? AND deleted_at IS NULL"
                + (lock ? " FOR UPDATE" : ""),
            id,
            parent)
        .stream()
        .findFirst()
        .map(this::hydrate);
  }

  public List<StudentProfile> findByParent(String parent) {
    return db
        .queryForList(
            "SELECT * FROM student_profiles WHERE parent_id=? AND deleted_at IS NULL ORDER BY nickname,id", parent)
        .stream()
        .map(this::hydrate)
        .toList();
  }

  public Optional<StudentProfile> findById(String id) {
    return db.queryForList("SELECT * FROM student_profiles WHERE id=? AND deleted_at IS NULL", id).stream()
        .findFirst().map(this::hydrate);
  }

  public Optional<StudentProfile> lockById(String id) {
    return db.queryForList("SELECT * FROM student_profiles WHERE id=? AND deleted_at IS NULL FOR UPDATE",id)
        .stream().findFirst().map(this::hydrate);
  }

  public List<StudentProfile> candidates(TutorProfile tutor) {
    var named = new org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate(db);
    var rows = named.queryForList("SELECT s.* FROM student_profiles s JOIN users u ON u.id=s.parent_id "
        + "WHERE u.email_verified=TRUE AND u.deleted_at IS NULL AND s.deleted_at IS NULL AND s.grade IN (:grades) AND s.mode IN (:modes) "
        + "AND s.max_hourly_rate>=:rate AND (s.mode='ONLINE' OR LOWER(s.province)=LOWER(:province)) "
        + "AND NOT EXISTS(SELECT 1 FROM student_subjects ss WHERE ss.student_id=s.id AND ss.subject NOT IN (:subjects)) ORDER BY s.id",
        Map.of("grades",tutor.grades(),"modes",tutor.modes(),"rate",tutor.hourlyRate(),
            "province",tutor.province(),"subjects",tutor.subjects()));
    if (rows.isEmpty()) return List.of();
    var subjects = new HashMap<String,List<String>>();
    named.queryForList("SELECT student_id,subject FROM student_subjects WHERE student_id IN (:ids) ORDER BY subject",
        Map.of("ids",rows.stream().map(r -> r.get("id").toString()).toList()))
        .forEach(r -> subjects.computeIfAbsent(r.get("student_id").toString(), key -> new ArrayList<>()).add(r.get("subject").toString()));
    return rows.stream().map(r -> mapper.fromRow(r,subjects.getOrDefault(r.get("id").toString(),List.of()))).toList();
  }

  private StudentProfile hydrate(Map<String, Object> row) {
    return mapper.fromRow(
        row,
        db.queryForList(
            "SELECT subject FROM student_subjects WHERE student_id=? ORDER BY subject",
            String.class,
            row.get("id")));
  }

  public void insert(String id, String parent, StudentRequest s) {
    db.update(
        "INSERT INTO"
            + " student_profiles(id,parent_id,nickname,grade,province,mode,max_hourly_rate,availability,survey,needs,consent_at)"
            + " VALUES(?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)",
        id,
        parent,
        s.nickname().trim(),
        s.grade(),
        s.province().trim(),
        s.mode(),
        s.budget(),
        json(s.availability()),
        json(s.survey()),
        s.needs().trim());
    subjects(id, s.subjects());
  }

  public void update(String id, String parent, StudentRequest s) {
    db.update(
        "UPDATE student_profiles SET"
            + " nickname=?,grade=?,province=?,mode=?,max_hourly_rate=?,availability=?,survey=?,needs=?,consent_at=CURRENT_TIMESTAMP"
            + " WHERE id=? AND parent_id=? AND deleted_at IS NULL",
        s.nickname().trim(),
        s.grade(),
        s.province().trim(),
        s.mode(),
        s.budget(),
        json(s.availability()),
        json(s.survey()),
        s.needs().trim(),
        id,
        parent);
    db.update("DELETE FROM student_subjects WHERE student_id=?", id);
    subjects(id, s.subjects());
  }

  private void subjects(String id, List<String> subjects) {
    for (String subject : subjects)
      db.update("INSERT INTO student_subjects VALUES(?,?)", id, subject);
  }
}
