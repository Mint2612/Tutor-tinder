package com.tutormatch.dto.lesson;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class TrialDtos {
  private TrialDtos() {}
  public record Propose(@NotBlank @Size(max=36) String studentId,
      @NotBlank @Size(max=36) String tutorId,
      @NotNull @Size(min=1,max=3) List<@NotNull Instant> slots,
      @Pattern(regexp="ONLINE|IN_PERSON") String mode) {
    public Propose(String studentId,String tutorId,List<Instant> slots) { this(studentId,tutorId,slots,null); }
  }
  public record Respond(@NotBlank @Pattern(regexp="ACCEPT|COUNTER|CANCEL") String action,
      @Min(1) int version, Instant slot, @Size(max=3) List<@NotNull Instant> slots,
      @Size(max=500) String reason) {}
  public record Trial(String id, String studentId, String tutorId, String proposerRole,
      List<Instant> slots, String status, int version, Instant chosenAt, Instant expiresAt,
      String cancellationReason, String mode) {}
}
