package com.tutormatch.controller;

import com.tutormatch.dto.MessageResponse;
import com.tutormatch.dto.auth.AuthRequests.*;
import com.tutormatch.dto.auth.CsrfResponse;
import com.tutormatch.model.Account;
import com.tutormatch.service.AuthService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService auth;
  private final boolean secure;

  public AuthController(AuthService auth, @Value("${COOKIE_SECURE:true}") boolean secure) {
    this.auth = auth;
    this.secure = secure;
  }

  @GetMapping("/csrf")
  public CsrfResponse csrf(CsrfToken token) {
    return new CsrfResponse(token.getToken());
  }

  @PostMapping("/register")
  public MessageResponse register(@Valid @RequestBody Registration r, HttpServletRequest q) {
    auth.throttle("register:" + q.getRemoteAddr(), 8);
    auth.register(r);
    // Parents are active at once (no email step); tutors are told to check their inbox.
    return "PARENT".equals(r.role())
        ? new MessageResponse("Đã tạo tài khoản. Bạn có thể đăng nhập ngay.")
        : generic();
  }

  @PostMapping("/resend")
  public MessageResponse resend(@Valid @RequestBody EmailInput r, HttpServletRequest q) {
    auth.throttle("resend:" + q.getRemoteAddr(), 5);
    auth.resend(r.email());
    return generic();
  }

  @PostMapping("/verify")
  public MessageResponse verify(@Valid @RequestBody Code r, HttpServletRequest q) {
    auth.throttle("verify:" + q.getRemoteAddr(), 30);
    auth.verify(r.challengeId(), r.code());
    return new MessageResponse("Đã xác minh email. Bạn có thể đăng nhập.");
  }

  @PostMapping("/forgot")
  public MessageResponse forgot(@Valid @RequestBody EmailInput r, HttpServletRequest q) {
    auth.throttle("forgot:" + q.getRemoteAddr(), 5);
    auth.forgot(r.email());
    return generic();
  }

  @PostMapping("/reset")
  public MessageResponse reset(@Valid @RequestBody Reset r, HttpServletRequest q) {
    auth.throttle("reset:" + q.getRemoteAddr(), 10);
    auth.reset(r.challengeId(), r.code(), r.password());
    return new MessageResponse("Mật khẩu đã được thay đổi");
  }

  @PostMapping("/login")
  public Account login(
      @Valid @RequestBody Login r, HttpServletRequest q, HttpServletResponse response) {
    auth.throttle("login:" + q.getRemoteAddr(), 20);
    auth.throttle("account:" + r.email().trim().toLowerCase(Locale.ROOT), 10);
    var login = auth.login(r.email(), r.password());
    cookie(response, login.token(), AuthService.SESSION_SECONDS);
    return login.account();
  }

  @GetMapping("/me")
  public Account me(@AuthenticationPrincipal Account account) {
    return account;
  }

  @PostMapping("/logout")
  public MessageResponse logout(HttpServletRequest q, HttpServletResponse response) {
    if (q.getCookies() != null)
      for (var c : q.getCookies()) if (c.getName().equals("TM_SESSION")) auth.logout(c.getValue());
    cookie(response, "", 0);
    return new MessageResponse("Đã đăng xuất");
  }

  private void cookie(HttpServletResponse response, String token, long seconds) {
    response.addHeader(
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

  private MessageResponse generic() {
    return new MessageResponse(
        "Nếu thông tin hợp lệ, hướng dẫn đã được gửi. Vui lòng kiểm tra email của bạn.");
  }
}
