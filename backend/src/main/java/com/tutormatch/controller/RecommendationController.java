package com.tutormatch.controller;

import com.tutormatch.dto.recommendation.RecommendationResponse;
import com.tutormatch.model.Account;
import com.tutormatch.service.AiMatchingService;
import com.tutormatch.service.RecommendationService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students/{id}/recommendations")
public class RecommendationController {
  private final RecommendationService recommendations;
  private final AiMatchingService ai;

  public RecommendationController(RecommendationService recommendations, AiMatchingService ai) {
    this.recommendations = recommendations;
    this.ai = ai;
  }

  @GetMapping
  public List<RecommendationResponse> recommend(
      @PathVariable String id, @AuthenticationPrincipal Account u) {
    return recommendations.recommend(id, u.id());
  }

  /** 100 ứng viên đã lọc → top 5 bằng embedding. */
  @GetMapping("/ai")
  public List<RecommendationResponse> recommendWithAi(
      @PathVariable String id, @AuthenticationPrincipal Account u) {
    return ai.suggest(id, u.id());
  }
}