package com.tutormatch.dto.profile;

import jakarta.validation.constraints.*;

public final class ManagementRequests {
  private ManagementRequests() {}
  public record Visibility(@NotNull Boolean visible) {}
  public record Availability(@NotNull Boolean acceptingStudents) {}
  public record Delete(@NotBlank @Pattern(regexp="DELETE") String confirmation) {}
  public record DeleteAccount(@NotBlank @Size(max=72) String password,
      @NotBlank @Pattern(regexp="DELETE") String confirmation) {}
}
