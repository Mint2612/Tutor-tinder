package com.tutormatch.controller;

import com.tutormatch.dto.profile.*;
import com.tutormatch.mapper.StudentMapper;
import com.tutormatch.mapper.TutorMapper;
import com.tutormatch.model.Account;
import com.tutormatch.service.ProfileManagementService;
import com.tutormatch.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ProfileManagementController {
  private final ProfileManagementService management;
  private final StudentMapper students;
  private final TutorMapper tutors;
  private final AuthService auth;
  public ProfileManagementController(ProfileManagementService management,StudentMapper students,TutorMapper tutors,AuthService auth) {
    this.management=management; this.students=students; this.tutors=tutors;
    this.auth=auth;
  }
  @PutMapping("/students/{id}/visibility")
  public StudentResponse visibility(@PathVariable String id,@AuthenticationPrincipal Account user,
      @Valid @RequestBody ManagementRequests.Visibility request) {
    return students.response(management.studentVisibility(id,user,request.visible()));
  }
  @DeleteMapping("/students/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteStudent(@PathVariable String id,@AuthenticationPrincipal Account user,
      @Valid @RequestBody ManagementRequests.Delete request) {
    management.deleteStudent(id,user);
  }
  @PutMapping("/tutor/availability")
  public TutorResponse availability(@AuthenticationPrincipal Account user,
      @Valid @RequestBody ManagementRequests.Availability request) {
    return tutors.ownerResponse(management.availability(user,request.acceptingStudents()));
  }
  @DeleteMapping("/tutor/profile") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteTutor(@AuthenticationPrincipal Account user,
      @Valid @RequestBody ManagementRequests.DeleteAccount request) {
    auth.throttle("delete-profile:"+user.id(),5);
    management.deleteTutorProfile(user,request.password());
  }
}
