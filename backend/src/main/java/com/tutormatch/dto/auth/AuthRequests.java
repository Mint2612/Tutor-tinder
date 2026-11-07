package com.tutormatch.dto.auth;

import jakarta.validation.constraints.*;

public final class AuthRequests {
  private AuthRequests() {}

  public record Registration(
      @Email @NotBlank @Size(max = 255) String email,
      @NotBlank @Size(min = 12, max = 72) String password,
      @NotBlank @Size(max = 120) String name,
      @NotNull @Pattern(regexp = "\\+[1-9][0-9]{7,14}") String phone,
      @NotNull String role) {}

  public record Login(
      @Email @NotBlank @Size(max = 255) String email, @NotBlank @Size(max = 72) String password) {}

  public record EmailInput(@Email @NotBlank @Size(max = 255) String email) {}

  public record Code(
      @NotBlank @Size(max = 36) String challengeId, @NotBlank @Size(max = 100) String code) {}

  public record Reset(
      @NotBlank @Size(max = 36) String challengeId,
      @NotBlank @Size(max = 100) String code,
      @NotNull @Size(min = 12, max = 72) String password) {}
}
