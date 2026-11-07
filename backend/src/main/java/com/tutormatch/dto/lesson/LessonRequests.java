package com.tutormatch.dto.lesson;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class LessonRequests {
  private LessonRequests() {}

  public record Swipe(
      @NotBlank @Size(max = 36) String studentId,
      @NotBlank @Size(max = 36) String tutorId,
      @NotNull Boolean interested) {}

  public record Create(
      @NotBlank @Size(max = 36) String studentId,
      @NotBlank @Size(max = 36) String tutorId,
      @NotNull Instant proposedAt,
      @NotNull @Size(max = 2000) String message) {}

  public record Respond(@NotNull @Pattern(regexp = "ACCEPTED|REJECTED") String status) {}
}
