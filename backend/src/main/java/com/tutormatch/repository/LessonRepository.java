package com.tutormatch.repository;

import com.tutormatch.dto.lesson.*;
import com.tutormatch.mapper.LessonMapper;
import com.tutormatch.model.Account;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LessonRepository {
  private final JdbcTemplate db;
  private final LessonMapper mapper;

  public LessonRepository(JdbcTemplate db, LessonMapper mapper) {
    this.db = db;
    this.mapper = mapper;
  }

  public void expirePair(String student, String tutor) {
    db.update(
        "UPDATE lesson_requests SET status='EXPIRED' WHERE student_id=? AND tutor_id=? AND"
            + " status='PENDING' AND expires_at<=CURRENT_TIMESTAMP",
        student,
        tutor);
  }

  public void create(String id, LessonRequests.Create r, Instant expires) {
    db.update(
        "INSERT INTO lesson_requests(id,student_id,tutor_id,proposed_at,message,expires_at)"
            + " VALUES(?,?,?,?,?,?)",
        id,
        r.studentId(),
        r.tutorId(),
        Timestamp.from(r.proposedAt()),
        r.message().trim(),
        Timestamp.from(expires));
  }

  public int respond(String id, String tutorUser, String status) {
    return db.update(
        "UPDATE lesson_requests l JOIN tutor_profiles t ON t.id=l.tutor_id JOIN student_profiles s ON s.id=l.student_id SET"
            + " l.status=?,l.responded_at=CURRENT_TIMESTAMP WHERE l.id=? AND t.user_id=? AND"
            + " l.status='PENDING' AND s.deleted_at IS NULL AND t.deleted_at IS NULL AND l.expires_at>CURRENT_TIMESTAMP",
        status,
        id,
        tutorUser);
  }
  public java.util.Optional<java.util.Map<String,Object>> ownedPair(String id,String tutorUser) {
    return db.queryForList("SELECT l.student_id,l.tutor_id FROM lesson_requests l JOIN tutor_profiles t ON t.id=l.tutor_id WHERE l.id=? AND t.user_id=?",id,tutorUser).stream().findFirst();
  }

  public List<LessonResponse> list(Account u) {
    String owner = u.role().equals("PARENT") ? "s.parent_id=?" : "t.user_id=?";
    db.update(
        "UPDATE lesson_requests l JOIN student_profiles s ON s.id=l.student_id JOIN tutor_profiles"
            + " t ON t.id=l.tutor_id SET l.status='EXPIRED' WHERE "
            + owner
            + " AND l.status='PENDING' AND l.expires_at<=CURRENT_TIMESTAMP",
        u.id());
    return db
        .queryForList(
            "SELECT"
                + " l.id,l.student_id,l.tutor_id,l.status,l.proposed_at,l.expires_at,l.message,l.created_at,s.nickname,t.display_name,CASE"
                + " WHEN l.status='ACCEPTED' AND EXISTS(SELECT 1 FROM matching_pairs mp WHERE mp.student_id=l.student_id AND mp.tutor_id=l.tutor_id AND mp.status='MATCHED' AND mp.parent_confirm='CONFIRM' AND mp.tutor_confirm='CONFIRM') AND l.contact_revoked=FALSE AND s.deleted_at IS NULL AND t.deleted_at IS NULL AND pu.deleted_at IS NULL AND tu.deleted_at IS NULL THEN pu.email ELSE NULL END AS parent_email,CASE WHEN"
                + " l.status='ACCEPTED' AND EXISTS(SELECT 1 FROM matching_pairs mp WHERE mp.student_id=l.student_id AND mp.tutor_id=l.tutor_id AND mp.status='MATCHED' AND mp.parent_confirm='CONFIRM' AND mp.tutor_confirm='CONFIRM') AND l.contact_revoked=FALSE AND s.deleted_at IS NULL AND t.deleted_at IS NULL AND pu.deleted_at IS NULL AND tu.deleted_at IS NULL THEN pu.phone ELSE NULL END AS parent_phone,CASE WHEN"
                + " l.status='ACCEPTED' AND EXISTS(SELECT 1 FROM matching_pairs mp WHERE mp.student_id=l.student_id AND mp.tutor_id=l.tutor_id AND mp.status='MATCHED' AND mp.parent_confirm='CONFIRM' AND mp.tutor_confirm='CONFIRM') AND l.contact_revoked=FALSE AND s.deleted_at IS NULL AND t.deleted_at IS NULL AND pu.deleted_at IS NULL AND tu.deleted_at IS NULL THEN tu.email ELSE NULL END AS tutor_email,CASE WHEN"
                + " l.status='ACCEPTED' AND EXISTS(SELECT 1 FROM matching_pairs mp WHERE mp.student_id=l.student_id AND mp.tutor_id=l.tutor_id AND mp.status='MATCHED' AND mp.parent_confirm='CONFIRM' AND mp.tutor_confirm='CONFIRM') AND l.contact_revoked=FALSE AND s.deleted_at IS NULL AND t.deleted_at IS NULL AND pu.deleted_at IS NULL AND tu.deleted_at IS NULL THEN tu.phone ELSE NULL END AS tutor_phone FROM"
                + " lesson_requests l JOIN student_profiles s ON s.id=l.student_id JOIN"
                + " tutor_profiles t ON t.id=l.tutor_id JOIN users pu ON pu.id=s.parent_id JOIN"
                + " users tu ON tu.id=t.user_id WHERE "
                + owner
                + " ORDER BY l.created_at DESC,l.id",
            u.id())
        .stream()
        .map(mapper::fromRow)
        .toList();
  }
}
