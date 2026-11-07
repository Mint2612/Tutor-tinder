package com.tutormatch.controller;

import com.tutormatch.dto.profile.*;
import com.tutormatch.mapper.TutorMapper;
import com.tutormatch.model.Account;
import com.tutormatch.service.AvatarService;
import com.tutormatch.service.TutorService;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class TutorController {
  private final TutorService tutors;
  private final AvatarService avatars;
  private final TutorMapper mapper;

  public TutorController(TutorService tutors, AvatarService avatars, TutorMapper mapper) {
    this.tutors = tutors;
    this.avatars = avatars;
    this.mapper = mapper;
  }

  @GetMapping("/tutor/profile")
  public TutorResponse own(@AuthenticationPrincipal Account u) {
    return mapper.ownerResponse(tutors.own(u.id()));
  }

  @PutMapping("/tutor/profile")
  public TutorResponse save(
      @Valid @RequestBody TutorRequest r, @AuthenticationPrincipal Account u) {
    return mapper.ownerResponse(tutors.save(u.id(), r));
  }

  @PostMapping("/tutor/photo")
  public TutorResponse uploadPhoto(
      @AuthenticationPrincipal Account u, @RequestParam("file") MultipartFile file)
      throws IOException {
    return mapper.ownerResponse(avatars.replace(u.id(), file.getBytes()));
  }

  @DeleteMapping("/tutor/photo")
  public TutorResponse removePhoto(@AuthenticationPrincipal Account u) {
    return mapper.ownerResponse(avatars.remove(u.id()));
  }

  @GetMapping("/tutors/{id}")
  public TutorResponse details(@PathVariable String id) {
    return mapper.publicResponse(tutors.publicTutor(id));
  }
}
