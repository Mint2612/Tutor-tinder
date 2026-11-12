package com.tutormatch.service;

import static com.tutormatch.exception.BusinessException.require;
import static com.tutormatch.service.ProfileValidation.*;

import com.tutormatch.dto.profile.TutorRequest;
import com.tutormatch.model.TutorProfile;
import com.tutormatch.repository.TutorRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TutorService {
  private final TutorRepository tutors;

  public TutorService(TutorRepository tutors) {
    this.tutors = tutors;
  }

  public TutorProfile own(String user) {
    return tutors
        .findByUser(user)
        .orElseThrow(
            () -> new com.tutormatch.exception.BusinessException(404, "Bạn chưa tạo hồ sơ gia sư"));
  }

  public TutorProfile lockOwn(String user) {
    return tutors
        .lockByUser(user)
        .orElseThrow(
            () -> new com.tutormatch.exception.BusinessException(404, "Bạn chưa tạo hồ sơ gia sư"));
  }

  public TutorProfile lock(String id) {
    return tutors
        .lockById(id)
        .orElseThrow(
            () -> new com.tutormatch.exception.BusinessException(404, "Không tìm thấy gia sư"));
  }

  public TutorProfile find(String id) {
    return tutors
        .findById(id)
        .orElseThrow(
            () -> new com.tutormatch.exception.BusinessException(404, "Không tìm thấy gia sư"));
  }

  public TutorProfile publicTutor(String id) {
    return tutors
        .findPublic(id)
        .orElseThrow(
            () ->
                new com.tutormatch.exception.BusinessException(
                    404, "Gia sư chưa sẵn sàng hiển thị"));
  }

  public List<TutorProfile> all() {
    return tutors.findAll();
  }

  @Transactional
  public TutorProfile save(String user, TutorRequest t) {
    require(SUBJECTS.containsAll(t.subjects()), 400, "Môn học không hợp lệ");
    province(t.province());
    distinct(t.subjects());
    distinct(t.grades());
    distinct(t.modes());
    availability(t.availability());
    // t.photoUrl() is accepted for compatibility but ignored: the photo is uploaded through
    // AvatarService, so a profile save can neither set an arbitrary external image nor wipe it.
    require(tutors.lockActiveOwner(user),404,"Tài khoản gia sư không còn hoạt động.");
    var old = tutors.lockForSave(user);
    String id = old.map(TutorProfile::id).orElseGet(() -> UUID.randomUUID().toString());
    tutors.save(id, user, t, old.isEmpty());
    return own(user);
  }
}
