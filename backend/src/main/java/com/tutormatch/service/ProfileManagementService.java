package com.tutormatch.service;

import static com.tutormatch.exception.BusinessException.require;
import com.tutormatch.dto.profile.*;
import com.tutormatch.model.Account;
import com.tutormatch.model.StudentProfile;
import com.tutormatch.model.TutorProfile;
import com.tutormatch.repository.ProfileManagementRepository;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileManagementService {
  private final ProfileManagementRepository management;
  private final StudentService students;
  private final TutorService tutors;
  private final VerificationService verification;
  private final AvatarService avatars;
  private final PasswordEncoder passwords;
  public ProfileManagementService(ProfileManagementRepository management,StudentService students,
      TutorService tutors,VerificationService verification,AvatarService avatars,PasswordEncoder passwords) {
    this.management=management; this.students=students; this.tutors=tutors;
    this.verification=verification; this.avatars=avatars; this.passwords=passwords;
  }
  private void password(Account user,String password) {
    var hash=management.lockPassword(user.id());
    require(hash.isPresent() && passwords.matches(password,hash.orElse("")),400,"Mật khẩu hiện tại không đúng.");
  }
  @Transactional
  public StudentProfile studentVisibility(String id,Account user,boolean visible) {
    require(user.role().equals("PARENT"),403,"Chỉ phụ huynh quản lý hồ sơ con.");
    students.lockOwned(id,user.id());
    require(management.studentVisibility(id,user.id(),visible)==1,404,"Không tìm thấy hồ sơ học sinh.");
    return students.owned(id,user.id());
  }
  @Transactional
  public void deleteStudent(String id,Account user) {
    require(user.role().equals("PARENT"),403,"Chỉ phụ huynh quản lý hồ sơ con.");
    students.lockOwned(id,user.id());
    management.closeStudent(id);
  }
  @Transactional
  public TutorProfile availability(Account user,boolean accepting) {
    require(user.role().equals("TUTOR"),403,"Chỉ gia sư quản lý trạng thái nhận học sinh.");
    var tutor=tutors.lockOwn(user.id());
    require(management.tutorAvailability(tutor.id(),user.id(),accepting)==1,404,"Không tìm thấy hồ sơ gia sư.");
    return tutors.own(user.id());
  }
  @Transactional
  public void deleteTutorProfile(Account user,String password) {
    require(user.role().equals("TUTOR"),403,"Chỉ gia sư có thể xóa hồ sơ giảng dạy của mình.");
    password(user,password);
    var tutor=tutors.lockOwn(user.id());
    removeTutor(user,tutor);
  }
  private void removeTutor(Account user,TutorProfile tutor) {
    for(var document:verification.documents(user.id())) verification.remove(user.id(),document.id());
    avatars.remove(user.id());
    management.closeTutor(tutor.id());
  }
  @Transactional
  public void deleteAccount(Account user,String password) {
    require(Set.of("PARENT","TUTOR").contains(user.role()),403,"Tài khoản quản trị không thể tự xóa tại đây.");
    // Account locks precede profile locks, matching login/profile edits and matching mutations.
    password(user,password);
    TutorProfile tutor=null;
    if(user.role().equals("TUTOR")) {
      try { tutor=tutors.lockOwn(user.id()); }
      catch(com.tutormatch.exception.BusinessException e) { if(e.status()!=404) throw e; }
    }
    if(tutor!=null) removeTutor(user,tutor);
    if(user.role().equals("PARENT")) {
      for(var student:students.list(user.id())) {
        students.lockOwned(student.id(),user.id());
        management.closeStudent(student.id());
      }
    }
    management.deleteAccount(user.id());
  }
}
