package com.tutormatch.platform;

import static com.tutormatch.platform.ApiErrors.require;
import static com.tutormatch.platform.ProfileData.*;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
  private final JdbcTemplate db;

  public JdbcTemplate jdbc() {
    return db;
  }

  public ProfileService(JdbcTemplate db) {
    this.db = db;
  }

  public Map<String, Object> ownedStudent(String id, String parent) {
    var rows =
        db.queryForList("SELECT * FROM student_profiles WHERE id=? AND parent_id=?", id, parent);
    require(!rows.isEmpty(), 404, "Không tìm thấy hồ sơ học sinh");
    return studentView(rows.getFirst());
  }

  public Map<String, Object> studentView(Map<String, Object> r) {
    var result = new LinkedHashMap<>(r);
    result.put(
        "subjects",
        db.queryForList(
            "SELECT subject FROM student_subjects WHERE student_id=? ORDER BY subject",
            String.class,
            r.get("id")));
    result.put("availability", parse(r.get("availability")));
    result.put("survey", parse(r.get("survey")));
    result.remove("embedding");
    result.remove("embedding_model");
    return result;
  }

  public List<Map<String, Object>> students(String parent) {
    return db.queryForList("SELECT * FROM student_profiles WHERE parent_id=?", parent).stream()
        .map(this::studentView)
        .toList();
  }

  @Transactional
  public Map<String, Object> saveStudent(String id, String parent, Student s) {
    require(s.consent(), 400, "Cần sự đồng ý của phụ huynh trước khi lưu thông tin học sinh");
    require(SUBJECTS.containsAll(s.subjects()), 400, "Môn học không hợp lệ");
    distinct(s.subjects());
    distinct(s.availability());
    if (id == null) {
      id = UUID.randomUUID().toString();
      db.update(
          "INSERT INTO"
              + " student_profiles(id,parent_id,nickname,grade,province,mode,max_hourly_rate,availability,survey,needs,consent_at)"
              + " VALUES(?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)",
          id,
          parent,
          s.nickname(),
          s.grade(),
          s.province(),
          s.mode(),
          s.budget(),
          json(s.availability()),
          json(s.survey()),
          s.needs());
    } else {
      ownedStudent(id, parent);
      db.update(
          "UPDATE student_profiles SET"
              + " nickname=?,grade=?,province=?,mode=?,max_hourly_rate=?,availability=?,survey=?,needs=?,consent_at=CURRENT_TIMESTAMP"
              + " WHERE id=? AND parent_id=?",
          s.nickname(),
          s.grade(),
          s.province(),
          s.mode(),
          s.budget(),
          json(s.availability()),
          json(s.survey()),
          s.needs(),
          id,
          parent);
      db.update("DELETE FROM student_subjects WHERE student_id=?", id);
    }
    for (String subject : s.subjects())
      db.update("INSERT INTO student_subjects VALUES(?,?)", id, subject);
    return ownedStudent(id, parent);
  }

  public Map<String, Object> ownTutor(String user) {
    var rows = db.queryForList("SELECT * FROM tutor_profiles WHERE user_id=?", user);
    require(!rows.isEmpty(), 404, "Bạn chưa tạo hồ sơ gia sư");
    return tutorView(rows.getFirst());
  }

  public Map<String, Object> tutorView(Map<String, Object> r) {
    var result = new LinkedHashMap<>(r);
    Object id = r.get("id");
    result.put(
        "subjects",
        db.queryForList(
            "SELECT subject FROM tutor_subjects WHERE tutor_id=? ORDER BY subject",
            String.class,
            id));
    result.put(
        "grades",
        db.queryForList(
            "SELECT grade FROM tutor_grades WHERE tutor_id=? ORDER BY grade", Integer.class, id));
    result.put(
        "modes",
        db.queryForList("SELECT mode FROM tutor_modes WHERE tutor_id=?", String.class, id));
    result.put("availability", parse(r.get("availability")));
    result.remove("embedding");
    result.remove("embedding_model");
    result.remove("survey");
    return result;
  }

  public Map<String, Object> publicTutor(String id) {
    var rows =
        db.queryForList(
            "SELECT t.* FROM tutor_profiles t JOIN users u ON u.id=t.user_id WHERE t.id=? AND"
                + " t.workflow_state='APPROVED' AND t.verification_status='APPROVED' AND"
                + " t.visible=TRUE AND u.email_verified=TRUE AND u.phone_verified=TRUE",
            id);
    require(!rows.isEmpty(), 404, "Gia sư chưa sẵn sàng hiển thị");
    var view = tutorView(rows.getFirst());
    view.remove("user_id");
    view.remove("rejection_reason");
    return view;
  }

  @Transactional
  public Map<String, Object> saveTutor(String user, Tutor t) {
    require(SUBJECTS.containsAll(t.subjects()), 400, "Môn học không hợp lệ");
    distinct(t.subjects());
    distinct(t.grades());
    distinct(t.modes());
    distinct(t.availability());
    require(
        t.photoUrl().isBlank() || t.photoUrl().matches("https://[^\\s]+"),
        400,
        "Ảnh cần dùng đường dẫn HTTPS");
    var rows = db.queryForList("SELECT id FROM tutor_profiles WHERE user_id=? FOR UPDATE", user);
    String id =
        rows.isEmpty() ? UUID.randomUUID().toString() : rows.getFirst().get("id").toString();
    if (rows.isEmpty())
      db.update(
          "INSERT INTO"
              + " tutor_profiles(id,user_id,province,hourly_rate,teaching_description,availability,survey)"
              + " VALUES(?,?,?,?,?,?,'{}')",
          id,
          user,
          t.province(),
          t.hourlyRate(),
          t.methodology(),
          json(t.availability()));
    db.update(
        "UPDATE tutor_profiles SET"
            + " display_name=?,photo_url=?,province=?,hourly_rate=?,teaching_description=?,background=?,experience_years=?,teaching_style=?,availability=?,workflow_state='DRAFT',verification_status='PENDING',verified_at=NULL,rejection_reason=NULL,revision=revision+1"
            + " WHERE id=?",
        t.displayName(),
        t.photoUrl(),
        t.province(),
        t.hourlyRate(),
        t.methodology(),
        t.background(),
        t.experience(),
        t.style(),
        json(t.availability()),
        id);
    db.update("DELETE FROM tutor_subjects WHERE tutor_id=?", id);
    db.update("DELETE FROM tutor_grades WHERE tutor_id=?", id);
    db.update("DELETE FROM tutor_modes WHERE tutor_id=?", id);
    for (var s : t.subjects()) db.update("INSERT INTO tutor_subjects VALUES(?,?)", id, s);
    for (var g : t.grades()) db.update("INSERT INTO tutor_grades VALUES(?,?)", id, g);
    for (var m : t.modes()) db.update("INSERT INTO tutor_modes VALUES(?,?)", id, m);
    return ownTutor(user);
  }
}
