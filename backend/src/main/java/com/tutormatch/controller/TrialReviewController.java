package com.tutormatch.controller;

import com.tutormatch.dto.lesson.TrialReviewDtos.*;
import com.tutormatch.dto.MessageResponse;
import com.tutormatch.model.Account;
import com.tutormatch.service.TrialReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trial-reviews")
public class TrialReviewController {
  private final TrialReviewService reviews;
  public TrialReviewController(TrialReviewService reviews) { this.reviews=reviews; }
  @GetMapping public List<Item> list(@AuthenticationPrincipal Account u) { return reviews.list(u); }
  @PostMapping("/{trialId}")
  public MessageResponse submit(@AuthenticationPrincipal Account u,@PathVariable String trialId,@Valid @RequestBody Submit body) {
    reviews.submit(u,trialId,body);
    return new MessageResponse("Đã gửi đánh giá buổi học thử.");
  }
}
