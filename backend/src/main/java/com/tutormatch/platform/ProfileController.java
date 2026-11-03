package com.tutormatch.platform;

import com.tutormatch.auth.SecurityConfig.Account;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ProfileController {
  private final ProfileService profiles;

  public ProfileController(ProfileService profiles) {
    this.profiles = profiles;
  }

  @GetMapping("/students")
  public Object students(@AuthenticationPrincipal Account u) {
    return profiles.students(u.id());
  }

  @PostMapping("/students")
  public Object create(
      @AuthenticationPrincipal Account u, @Valid @RequestBody ProfileData.Student s) {
    return profiles.saveStudent(null, u.id(), s);
  }

  @PutMapping("/students/{id}")
  public Object edit(
      @AuthenticationPrincipal Account u,
      @PathVariable String id,
      @Valid @RequestBody ProfileData.Student s) {
    return profiles.saveStudent(id, u.id(), s);
  }

  @GetMapping("/tutor/profile")
  public Object own(@AuthenticationPrincipal Account u) {
    return profiles.ownTutor(u.id());
  }

  @PutMapping("/tutor/profile")
  public Object save(@AuthenticationPrincipal Account u, @Valid @RequestBody ProfileData.Tutor t) {
    return profiles.saveTutor(u.id(), t);
  }

  @GetMapping("/tutors/{id}")
  public Object details(@PathVariable String id) {
    return profiles.publicTutor(id);
  }
}
