package com.tutormatch.auth;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  final AuthService auth;
  final boolean secure;

  public AuthController(AuthService auth, @Value("${COOKIE_SECURE:true}") boolean secure) {
    this.auth = auth;
    this.secure = secure;
  }

  public record Registration(
      @Email @NotBlank @Size(max = 255) String email,
      @NotBlank @Size(min = 12, max = 72) String password,
      @NotBlank @Size(max = 120) String name,
      @Pattern(regexp = "\\+[1-9][0-9]{7,14}") @NotNull String phone,
      @NotNull String role) {}

  public record Login(@Email @NotBlank String email, @NotBlank @Size(max = 72) String password) {}

  public record EmailInput(@Email @NotBlank String email) {}

  public record Code(
      @NotBlank @Size(max = 36) String challengeId, @NotBlank @Size(max = 100) String code) {}

  public record Reset(
      @NotBlank String challengeId,
      @NotBlank String code,
      @Size(min = 12, max = 72) @NotNull String password) {}

  @GetMapping("/csrf")
  public Map<String, String> csrf(CsrfToken csrf) {
    return Map.of("token", csrf.getToken());
  }

  @PostMapping("/register")
  public Map<String, String> register(@Valid @RequestBody Registration r, HttpServletRequest q) {
    auth.throttle("register:" + q.getRemoteAddr(), 8);
    auth.register(r);
    return generic();
  }

  @PostMapping("/resend")
  public Map<String, String> resend(@Valid @RequestBody EmailInput r, HttpServletRequest q) {
    auth.throttle("resend:" + q.getRemoteAddr(), 5);
    auth.resend(r.email());
    return generic();
  }

  @PostMapping("/verify")
  public Map<String, String> verify(@Valid @RequestBody Code r, HttpServletRequest q) {
    auth.throttle("verify:" + q.getRemoteAddr(), 30);
    auth.verify(r.challengeId(), r.code());
    return Map.of(
        "message", "Đã xác minh. Hãy xác minh cả email và điện thoại trước khi đăng nhập.");
  }

  @PostMapping("/forgot")
  public Map<String, String> forgot(@Valid @RequestBody EmailInput r, HttpServletRequest q) {
    auth.throttle("forgot:" + q.getRemoteAddr(), 5);
    auth.forgot(r.email());
    return generic();
  }

  @PostMapping("/reset")
  public Map<String, String> reset(@Valid @RequestBody Reset r, HttpServletRequest q) {
    auth.throttle("reset:" + q.getRemoteAddr(), 10);
    auth.reset(r.challengeId(), r.code(), r.password());
    return Map.of("message", "Mật khẩu đã được thay đổi");
  }

  @PostMapping("/login")
  public SecurityConfig.Account login(
      @Valid @RequestBody Login r, HttpServletRequest q, HttpServletResponse response) {
    auth.throttle("login:" + q.getRemoteAddr(), 20);
    auth.throttle("account:" + r.email().toLowerCase(Locale.ROOT), 10);
    var u = auth.login(r.email(), r.password());
    String token = AuthService.token();
    auth.jdbc()
        .update(
            "INSERT INTO auth_sessions VALUES(?,?,?)",
            AuthService.hash(token),
            u.get("id"),
            Timestamp.from(Instant.now().plusSeconds(28800)));
    cookie(response, token, 28800);
    return new SecurityConfig.Account(
        u.get("id").toString(),
        u.get("email").toString(),
        u.get("full_name").toString(),
        u.get("role").toString());
  }

  @GetMapping("/me")
  public SecurityConfig.Account me(@AuthenticationPrincipal SecurityConfig.Account u) {
    return u;
  }

  @PostMapping("/logout")
  public Map<String, String> logout(HttpServletRequest q, HttpServletResponse r) {
    if (q.getCookies() != null)
      for (var c : q.getCookies())
        if (c.getName().equals("TM_SESSION"))
          auth.jdbc()
              .update(
                  "DELETE FROM auth_sessions WHERE token_hash=?", AuthService.hash(c.getValue()));
    cookie(r, "", 0);
    return Map.of("message", "Đã đăng xuất");
  }

  void cookie(HttpServletResponse r, String token, long seconds) {
    r.addHeader(
        HttpHeaders.SET_COOKIE,
        ResponseCookie.from("TM_SESSION", token)
            .httpOnly(true)
            .secure(secure)
            .sameSite("Lax")
            .path("/")
            .maxAge(seconds)
            .build()
            .toString());
  }

  Map<String, String> generic() {
    return Map.of(
        "message",
        "Nếu thông tin hợp lệ, hướng dẫn đã được gửi. Kiểm tra email và tin nhắn điện thoại của"
            + " bạn.");
  }
}
