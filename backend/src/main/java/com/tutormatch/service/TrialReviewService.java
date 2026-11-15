package com.tutormatch.service;

import static com.tutormatch.exception.BusinessException.require;
import com.tutormatch.dto.lesson.TrialReviewDtos.*;
import com.tutormatch.model.Account;
import com.tutormatch.exception.BusinessException;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrialReviewService {
  private final JdbcTemplate db;
  public TrialReviewService(JdbcTemplate db) { this.db=db; }
  private void parent(Account u) {
    require("PARENT".equals(u.role()),403,"Chỉ phụ huynh có thể đánh giá buổi học.");
  }
  public List<Item> list(Account u) {
    parent(u);
    return db.query("SELECT q.id,s.nickname,t.display_name,q.chosen_at,q.mode,r.rating,r.comment,r.created_at "
        + "FROM trial_schedules q JOIN student_profiles s ON s.id=q.student_id "
        + "JOIN users u ON u.id=s.parent_id JOIN tutor_profiles t ON t.id=q.tutor_id "
        + "LEFT JOIN trial_reviews r ON r.trial_id=q.id WHERE s.parent_id=? AND u.deleted_at IS NULL "
        + "AND s.deleted_at IS NULL AND t.deleted_at IS NULL AND q.status='CONFIRMED' "
        + "AND q.chosen_at<=CURRENT_TIMESTAMP ORDER BY q.chosen_at DESC,q.id",
        (r,n) -> new Item(r.getString("id"),r.getString("nickname"),r.getString("display_name"),
            r.getTimestamp("chosen_at").toInstant(),r.getString("mode"),(Integer)r.getObject("rating"),
            r.getString("comment"),r.getTimestamp("created_at")==null ? null : r.getTimestamp("created_at").toInstant()),u.id());
  }
  @Transactional
  public void submit(Account u,String trialId,Submit request) {
    parent(u);
    require(request.rating()>=1 && request.rating()<=5 && request.comment()!=null && request.comment().length()<=2000,
        400,"Chọn từ 1 đến 5 sao và nhận xét tối đa 2.000 ký tự.");
    require(request.attended(),400,"Vui lòng xác nhận buổi học đã diễn ra.");
    var rows=db.queryForList("SELECT q.status,q.chosen_at FROM trial_schedules q "
        + "JOIN student_profiles s ON s.id=q.student_id JOIN users u ON u.id=s.parent_id "
        + "JOIN tutor_profiles t ON t.id=q.tutor_id WHERE q.id=? AND s.parent_id=? "
        + "AND u.deleted_at IS NULL AND s.deleted_at IS NULL AND t.deleted_at IS NULL FOR UPDATE",trialId,u.id());
    if(rows.isEmpty()) throw new BusinessException(404,"Không tìm thấy buổi học thử của bạn.");
    var trial=rows.getFirst();
    var chosen=(Timestamp)trial.get("chosen_at");
    require("CONFIRMED".equals(trial.get("status")) && chosen!=null && !chosen.toInstant().isAfter(Instant.now()),
        409,"Chỉ đánh giá buổi học đã chốt lịch và đã diễn ra.");
    require(db.queryForObject("SELECT COUNT(*) FROM trial_reviews WHERE trial_id=?",Integer.class,trialId)==0,
        409,"Buổi học này đã được đánh giá.");
    db.update("INSERT INTO trial_reviews(id,trial_id,rating,comment) VALUES(?,?,?,?)",
        UUID.randomUUID().toString(),trialId,request.rating(),request.comment().trim());
  }
}
