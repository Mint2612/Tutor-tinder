package com.tutormatch.controller;

import com.tutormatch.dto.auth.AccountProfile;
import com.tutormatch.dto.auth.AuthRequests;
import com.tutormatch.model.Account;
import com.tutormatch.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Name and contact phone of the signed-in user. Email and role are never editable here. */
@RestController
@RequestMapping("/api/account")
public class AccountController {
  private final AuthService auth;

  public AccountController(AuthService auth) {
    this.auth = auth;
  }

  @GetMapping
  public AccountProfile get(@AuthenticationPrincipal Account account) {
    return auth.profile(account.id());
  }

  @PutMapping
  public AccountProfile update(
      @AuthenticationPrincipal Account account, @Valid @RequestBody AuthRequests.ProfileUpdate r) {
    return auth.updateProfile(account.id(), r.name(), r.phone());
  }
}
