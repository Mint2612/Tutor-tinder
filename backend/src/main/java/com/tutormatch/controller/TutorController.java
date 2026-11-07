package com.tutormatch.controller;

import com.tutormatch.dto.profile.*;
import com.tutormatch.mapper.TutorMapper;
import com.tutormatch.model.Account;
import com.tutormatch.service.TutorService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TutorController {
  private final TutorService tutors;
  private final TutorMapper mapper;

  public TutorController(TutorService tutors, TutorMapper mapper) {
    this.tutors = tutors;
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

  @GetMapping("/tutors/{id}")
  public TutorResponse details(@PathVariable String id) {
    return mapper.publicResponse(tutors.publicTutor(id));
  }
}
