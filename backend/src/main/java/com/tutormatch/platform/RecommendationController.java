package com.tutormatch.platform;

import com.tutormatch.auth.SecurityConfig.Account;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class RecommendationController {
  final RecommendationService service;

  public RecommendationController(RecommendationService service) {
    this.service = service;
  }

  @GetMapping("/api/students/{id}/recommendations")
  public Object recommend(@PathVariable String id, @AuthenticationPrincipal Account user) {
    return service.recommend(id, user.id());
  }
}
