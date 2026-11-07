package com.tutormatch.service;

import static com.tutormatch.exception.BusinessException.require;
import static com.tutormatch.service.ProfileValidation.*;

import com.tutormatch.dto.profile.StudentRequest;
import com.tutormatch.model.StudentProfile;
import com.tutormatch.repository.StudentRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentService {
  private final StudentRepository students;

  public StudentService(StudentRepository students) {
    this.students = students;
  }

  public List<StudentProfile> list(String parent) {
    return students.findByParent(parent);
  }

  public StudentProfile owned(String id, String parent) {
    return students
        .findOwned(id, parent)
        .orElseThrow(
            () ->
                new com.tutormatch.exception.BusinessException(
                    404, "Không tìm thấy hồ sơ học sinh"));
  }

  public StudentProfile lockOwned(String id, String parent) {
    return students
        .lockOwned(id, parent)
        .orElseThrow(
            () ->
                new com.tutormatch.exception.BusinessException(
                    404, "Không tìm thấy hồ sơ học sinh"));
  }

  @Transactional
  public StudentProfile save(String id, String parent, StudentRequest s) {
    require(students.lockActiveParent(parent),404,"Tài khoản phụ huynh không còn hoạt động.");
    require(s.consent(), 400, "Cần sự đồng ý của phụ huynh trước khi lưu thông tin học sinh");
    require(SUBJECTS.containsAll(s.subjects()), 400, "Môn học không hợp lệ");
    province(s.province());
    distinct(s.subjects());
    availability(s.availability());
    noContactInfo(s.needs(), "Mục tiêu, khó khăn và điều bạn muốn gia sư hiểu");
    if (id == null) {
      id = UUID.randomUUID().toString();
      students.insert(id, parent, s);
    } else {
      lockOwned(id, parent);
      students.update(id, parent, s);
    }
    return owned(id, parent);
  }
}
