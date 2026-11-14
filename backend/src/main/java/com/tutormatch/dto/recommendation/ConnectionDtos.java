package com.tutormatch.dto.recommendation;

import com.tutormatch.dto.profile.TutorResponse;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class ConnectionDtos {
  private ConnectionDtos() {}
  public record Decision(@NotBlank @Size(max = 36) String studentId,
      @NotBlank @Size(max = 36) String tutorId, @NotNull Boolean confirm) {}
  public record StudentDetail(int grade, List<String> subjects, String mode, String province,
      int budget, List<String> availability, List<String> learningPreferences, String needs) {}
  public record Contact(String email, String phone) {}
  /** Phụ huynh nhận tutor, gia sư nhận student; contact chỉ có khi cả hai đã xác nhận. */
  public record Detail(String studentId, String tutorId, String status, String myDecision,
      boolean connected, Instant expiresAt, TutorResponse tutor, StudentDetail student,
      Contact contact) {}
}