package com.tutormatch.controller;

import com.tutormatch.dto.StatusResponse;
import com.tutormatch.dto.lesson.*;
import com.tutormatch.model.Account;
import com.tutormatch.service.LessonService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {
  private final LessonService lessons;

  public LessonController(LessonService lessons) {
    this.lessons = lessons;
  }

  @PostMapping
  public LessonResult request(
      @AuthenticationPrincipal Account u, @Valid @RequestBody LessonRequests.Create r) {
    return lessons.request(u, r);
  }

  @PutMapping("/{id}")
  public StatusResponse respond(
      @PathVariable String id,
      @AuthenticationPrincipal Account u,
      @Valid @RequestBody LessonRequests.Respond r) {
    lessons.respond(u, id, r);
    return new StatusResponse(r.status());
  }

  @GetMapping
  public List<LessonResponse> list(@AuthenticationPrincipal Account u) {
    return lessons.list(u);
  }
}
