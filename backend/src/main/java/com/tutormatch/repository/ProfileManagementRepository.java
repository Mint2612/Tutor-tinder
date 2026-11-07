package com.tutormatch.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Account/profile lifecycle writes. Tombstones preserve references from historical lessons. */
@Repository
public class ProfileManagementRepository {
  private final JdbcTemplate db;
  public ProfileManagementRepository(JdbcTemplate db) { this.db=db; }

  public Optional<String> lockPassword(String user) {
    return db.queryForList("SELECT password_hash FROM users WHERE id=? AND deleted_at IS NULL FOR UPDATE",
        String.class,user).stream().findFirst();
  }
  public int studentVisibility(String student,String parent,boolean visible) {
    return db.update("UPDATE student_profiles SET visible=? WHERE id=? AND parent_id=? AND deleted_at IS NULL",
        visible,student,parent);
  }
  public int tutorAvailability(String tutor,String user,boolean accepting) {
    // The admin's visible/approval flags are deliberately not writable here.
    return db.update("UPDATE tutor_profiles SET accepting_students=? WHERE id=? AND user_id=? AND deleted_at IS NULL",
        accepting,tutor,user);
  }
  public void closeStudent(String student) {
    closePairs("student_id",student);
    db.update("DELETE FROM student_subjects WHERE student_id=?",student);
    db.update("UPDATE student_profiles SET visible=FALSE,deleted_at=CURRENT_TIMESTAMP,nickname='Hồ sơ đã xóa',"
        + "needs='',survey='{}',availability='[]',province='',grade=1,max_hourly_rate=0,embedding=NULL,embedding_model=NULL WHERE id=? AND deleted_at IS NULL",student);
  }
  public void closeTutor(String tutor) {
    closePairs("tutor_id",tutor);
    db.update("DELETE FROM tutor_subjects WHERE tutor_id=?",tutor);
    db.update("DELETE FROM tutor_grades WHERE tutor_id=?",tutor);
    db.update("DELETE FROM tutor_modes WHERE tutor_id=?",tutor);
    db.update("UPDATE tutor_profiles SET accepting_students=FALSE,deleted_at=CURRENT_TIMESTAMP,"
        + "display_name='Hồ sơ đã xóa',legal_name='',province='',hourly_rate=0,experience_years=0,teaching_description='',background='',"
        + "teaching_style='',achievements=NULL,photo_url='',availability='[]',survey='{}',embedding=NULL,embedding_model=NULL,"
        + "workflow_state='DRAFT',verification_status='PENDING',verified_at=NULL,rejection_reason=NULL,submission_id=NULL,submitted_at=NULL,revision=revision+1 WHERE id=?",tutor);
    db.update("UPDATE verification_audit SET reason=NULL WHERE tutor_id=?",tutor);
  }
  private void closePairs(String column,String id) {
    db.update("UPDATE matching_pairs SET status='SKIPPED' WHERE "+column+"=? AND status IN ('OPEN','MATCHED')",id);
    db.update("DELETE FROM matching_notifications WHERE "+column+"=?",id);
    db.update("UPDATE lesson_requests SET status='REJECTED',responded_at=CURRENT_TIMESTAMP WHERE "+column+"=? AND status='PENDING'",id);
    db.update("UPDATE lesson_requests SET contact_revoked=TRUE,message='' WHERE "+column+"=?",id);
  }
  public void deleteAccount(String user) {
    db.update("DELETE FROM auth_sessions WHERE user_id=?",user);
    db.update("DELETE FROM auth_challenges WHERE user_id=?",user);
    db.update("DELETE FROM matching_notifications WHERE user_id=?",user);
    db.update("UPDATE users SET deleted_at=CURRENT_TIMESTAMP,email=CONCAT('deleted-',id,'@deleted.invalid'),"
        + "full_name='Tài khoản đã xóa',phone=NULL,password_hash=NULL,email_verified=FALSE,phone_verified=FALSE WHERE id=? AND deleted_at IS NULL",user);
  }
}
