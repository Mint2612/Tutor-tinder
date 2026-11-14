package com.tutormatch.dto.recommendation;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

/** Explicit previews: student/parent identity and raw narratives never enter this API. */
public final class MatchingDtos {
  private MatchingDtos() {}
  public record Action(@NotBlank @Size(max=36) String studentId,
      @NotBlank @Size(max=36) String tutorId, @NotNull Boolean interested) {}
  public record TutorPreview(String displayName, String photoUrl, List<String> subjects, List<Integer> grades,
      List<String> modes, String province, int hourlyRate, int experienceYears,
      List<String> availability, String verificationBadge) {}
  public record StudentPreview(int grade, List<String> subjects, String mode, String province,
      int budget, List<String> availability, List<String> learningPreferences, String needsSummary) {}
  public record Card(String studentId, String tutorId, String tag, String level,
      List<String> strengths, List<String> concerns, String explanation,
      TutorPreview tutor, StudentPreview student, Instant expiresAt, boolean canLike, String status,
      int ruleScore, Instant activityAt) {}
  public record Pair(String studentId, String tutorId, String status, Instant matchedAt) {}
  public record Feed(List<Card> cards, List<Pair> matches, List<Card> savedCards, boolean paused) {}
  public record Result(String status, boolean matched, String message) {}
  public record Notification(String id, String studentId, String tutorId, String message,
      Instant createdAt, String eventKey) {}
}
