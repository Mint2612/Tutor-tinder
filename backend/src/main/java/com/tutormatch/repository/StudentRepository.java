package com.tutormatch.repository;

import static com.tutormatch.mapper.JsonFields.json;

import com.tutormatch.dto.profile.StudentRequest;
import com.tutormatch.mapper.StudentMapper;
import com.tutormatch.model.StudentProfile;
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

  public Optional<StudentProfile> lockOwned(String id, String parent) {
    return findOwned(id, parent, true);
  }

  private Optional<StudentProfile> findOwned(String id, String parent, boolean lock) {
    return db
        .queryForList(
            "SELECT * FROM student_profiles WHERE id=? AND parent_id=?"
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
            "SELECT * FROM student_profiles WHERE parent_id=? ORDER BY nickname,id", parent)
        .stream()
        .map(this::hydrate)
        .toList();
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
            + " WHERE id=? AND parent_id=?",
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
