package com.tutormatch.controller;

import com.tutormatch.dto.lesson.*;
import com.tutormatch.dto.profile.TutorResponse;
import com.tutormatch.mapper.TutorMapper;
import com.tutormatch.model.Account;
import com.tutormatch.service.SwipeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SwipeController {
  private final SwipeService swipes;
  private final TutorMapper mapper;

  public SwipeController(SwipeService swipes, TutorMapper mapper) {
    this.swipes = swipes;
    this.mapper = mapper;
  }

  @PostMapping("/swipes")
  public SwipeResponse save(
      @AuthenticationPrincipal Account u, @Valid @RequestBody LessonRequests.Swipe r) {
    return swipes.save(u.id(), r);
  }

  @GetMapping("/students/{id}/interests")
  public List<TutorResponse> interests(
      @PathVariable String id, @AuthenticationPrincipal Account u) {
    return swipes.interests(id, u.id()).stream().map(mapper::previewResponse).toList();
  }

  @DeleteMapping("/students/{id}/interests/{tutorId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(
      @PathVariable String id, @PathVariable String tutorId, @AuthenticationPrincipal Account u) {
    swipes.remove(id, tutorId, u.id());
  }
}
