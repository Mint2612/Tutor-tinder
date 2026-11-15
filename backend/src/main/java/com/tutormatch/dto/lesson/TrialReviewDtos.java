package com.tutormatch.dto.lesson;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class TrialReviewDtos {
  private TrialReviewDtos() {}
  public record Submit(@Min(1) @Max(5) int rating,
      @NotNull @Size(max=2000) String comment,
      @AssertTrue(message="Vui lòng xác nhận buổi học đã diễn ra") boolean attended) {}
  public record Item(String trialId, String studentName, String tutorName,
      Instant lessonAt, String mode, Integer rating, String comment, Instant reviewedAt) {}
}
