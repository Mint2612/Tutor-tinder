package com.tutormatch.controller;

import com.tutormatch.dto.recommendation.MatchingDtos.*;
import com.tutormatch.model.Account;
import com.tutormatch.service.MatchingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MatchingController {
  private final MatchingService matching;
  public MatchingController(MatchingService matching) { this.matching=matching; }
  @GetMapping("/students/{id}/matching")
  public Feed parent(@PathVariable String id,@AuthenticationPrincipal Account user) {
    return matching.parentFeed(id,user.id());
  }
  @GetMapping("/tutor/matching")
  public Feed tutor(@AuthenticationPrincipal Account user) { return matching.tutorFeed(user.id()); }
  @PostMapping("/matching/actions")
  public Result act(@AuthenticationPrincipal Account user,@Valid @RequestBody Action action) {
    return matching.act(user,action);
  }
  @GetMapping("/matching/notifications")
  public List<Notification> notifications(@AuthenticationPrincipal Account user) {
    return matching.notifications(user);
  }
  @PostMapping("/matching/notifications/{id}/read")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void read(@PathVariable String id,@AuthenticationPrincipal Account user) {
    matching.readNotification(id,user);
  }
}
