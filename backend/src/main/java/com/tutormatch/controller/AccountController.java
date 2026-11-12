package com.tutormatch.controller;

import com.tutormatch.dto.auth.AccountProfile;
import com.tutormatch.dto.auth.AuthRequests;
import com.tutormatch.model.Account;
import com.tutormatch.service.AuthService;
import com.tutormatch.service.ProfileManagementService;
import com.tutormatch.dto.profile.ManagementRequests;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
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
  private final ProfileManagementService management;
  private final boolean secure;

  public AccountController(AuthService auth,ProfileManagementService management,
      @Value("${COOKIE_SECURE:true}") boolean secure) {
    this.auth = auth;
    this.management=management;
    this.secure=secure;
  }

  @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@AuthenticationPrincipal Account account,
      @Valid @RequestBody ManagementRequests.DeleteAccount request,HttpServletResponse response) {
    auth.throttle("delete-account:"+account.id(),5);
    management.deleteAccount(account,request.password());
    response.addHeader("Set-Cookie",ResponseCookie.from("TM_SESSION","").path("/")
        .httpOnly(true).secure(secure).sameSite("Lax").maxAge(0).build().toString());
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
