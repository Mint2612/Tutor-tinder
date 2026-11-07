package com.tutormatch.dto.verification;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class VerificationRequests {
  private VerificationRequests() {}

  public record Decision(@NotNull Boolean approve, @Size(max = 1000) String reason) {}

  public record Visibility(@NotNull Boolean visible) {}
}
