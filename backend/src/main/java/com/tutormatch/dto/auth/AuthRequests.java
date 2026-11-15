package com.tutormatch.dto.auth;

import jakarta.validation.constraints.*;

public final class AuthRequests {
  private static final String PHONE = "(?:0[35789][0-9]{8}|\\+[1-9][0-9]{7,14})";
  private AuthRequests() {}

  public record Registration(
      @Email @NotBlank @Size(max = 255) String email,
      @NotBlank @Size(min = 12, max = 72) String password,
      @NotBlank @Size(max = 120) String name,
      @NotNull @Pattern(regexp = PHONE, message="Nhập số điện thoại dạng 0901234567 hoặc +84901234567") String phone,
      @NotNull String role) {}

  public record ProfileUpdate(
      @NotBlank @Size(max = 120) String name,
      @NotNull @Pattern(regexp = PHONE, message="Nhập số điện thoại dạng 0901234567 hoặc +84901234567") String phone) {}

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
