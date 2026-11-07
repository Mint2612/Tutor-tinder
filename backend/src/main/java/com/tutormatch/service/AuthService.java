package com.tutormatch.service;

import static com.tutormatch.exception.BusinessException.require;

import com.tutormatch.dto.auth.AuthRequests;
import com.tutormatch.exception.BusinessException;
import com.tutormatch.integration.delivery.DeliveryGateway;
import com.tutormatch.model.*;
import com.tutormatch.repository.AuthRepository;
import com.tutormatch.security.Tokens;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  public static final long SESSION_SECONDS = 28800;
  private static final String INVALID_LOGIN =
      "Thông tin đăng nhập không hợp lệ hoặc tài khoản chưa được kích hoạt";
  private static final String DUPLICATE_PARENT =
      "Email này đã có tài khoản. Hãy đăng nhập hoặc dùng “Quên mật khẩu”.";
  private final AuthRepository users;
  private final PasswordEncoder encoder;
  private final DeliveryGateway delivery;
  private final ChallengeService challenges;
  private final String dummy;

  public AuthService(
      AuthRepository users,
      PasswordEncoder encoder,
      DeliveryGateway delivery,
      ChallengeService challenges) {
    this.users = users;
    this.encoder = encoder;
    this.delivery = delivery;
    this.challenges = challenges;
    dummy = encoder.encode("unmatchable-dummy-password");
  }

  public record SignedIn(Account account, String token) {}

  public void throttle(String bucket, int limit) {
    require(
        users.recordAttempt(Tokens.hash(bucket)) <= limit,
        429,
        "Quá nhiều lần thử. Vui lòng chờ 15 phút.");
  }

  @Transactional
  public void register(AuthRequests.Registration r) {
    passwordLength(r.password());
    require(Set.of("PARENT", "TUTOR").contains(r.role()), 400, "Vai trò không hợp lệ");
    String email = canonical(r.email());
    boolean parent = "PARENT".equals(r.role());
    if (users.findByEmail(email).isPresent()) {
      // A parent is active immediately, so there is no email step to hide behind: tell them to sign
      // in. Tutors keep the generic response because their activation still goes through email.
      if (parent) throw new BusinessException(409, DUPLICATE_PARENT);
      return;
    }
    String id = UUID.randomUUID().toString();
    // Only tutors verify their email (verification level 1). Parents need no verification; the
    // email_verified column doubles as the "account is active" flag, so they are created active.
    users.create(
        id, email, r.name().trim(), r.role(), encoder.encode(r.password()), r.phone(), parent);
    if (!parent) challenge(id, "EMAIL", email);
  }

  private void challenge(String user, String purpose, String destination) {
    String code = Tokens.random(), id = UUID.randomUUID().toString();
    users.replaceChallenge(id, user, purpose, Tokens.hash(code), Instant.now().plusSeconds(600));
    delivery.send(purpose, destination, code, id);
  }

  @Transactional
  public void resend(String email) {
    users
        .findByEmail(canonical(email))
        .filter(u -> !u.emailVerified())
        .ifPresent(u -> challenge(u.account().id(), "EMAIL", u.account().email()));
  }

  // No outer transaction: committing the guess counter must not hold a second pool connection.
  public void verify(String id, String code) {
    users.incrementChallengeAttempts(id);
    challenges.verifyEmail(id, code);
  }

  @Transactional
  public SignedIn login(String email, String password) {
    var optional = users.lockByEmail(canonical(email));
    String stored = optional.map(UserCredentials::passwordHash).orElse(dummy);
    boolean valid =
        password.getBytes(StandardCharsets.UTF_8).length <= 72 && encoder.matches(password, stored);
    require(valid && optional.isPresent() && optional.get().emailVerified(), 401, INVALID_LOGIN);
    var user = optional.orElseThrow();
    String token = Tokens.random();
    users.createSession(
        Tokens.hash(token), user.account().id(), Instant.now().plusSeconds(SESSION_SECONDS));
    return new SignedIn(user.account(), token);
  }

  public com.tutormatch.dto.auth.AccountProfile profile(String id) {
    return users
        .profile(id)
        .orElseThrow(() -> new BusinessException(404, "Không tìm thấy tài khoản"));
  }

  @Transactional
  public com.tutormatch.dto.auth.AccountProfile updateProfile(String id, String name, String phone) {
    users.updateProfile(id, name.trim(), phone);
    return profile(id);
  }

  public Optional<Account> authenticate(String token) {
    return users.sessionAccount(Tokens.hash(token));
  }

  public void logout(String token) {
    if (token != null) users.deleteSession(Tokens.hash(token));
  }

  @Transactional
  public void forgot(String email) {
    users
        .findByEmail(canonical(email))
        .ifPresent(u -> challenge(u.account().id(), "RESET", u.account().email()));
  }

  public void reset(String id, String code, String password) {
    passwordLength(password);
    users.incrementChallengeAttempts(id);
    challenges.resetPassword(id, code, password);
  }

  @Transactional
  public void provisionAdmin(String email, String password) {
    if (users.findByEmail(canonical(email)).isEmpty())
      users.create(
          UUID.randomUUID().toString(),
          canonical(email),
          "Quản trị viên",
          "ADMIN",
          encoder.encode(password),
          null,
          true);
  }

  private void passwordLength(String password) {
    require(
        password.getBytes(StandardCharsets.UTF_8).length <= 72,
        400,
        "Mật khẩu tối đa 72 byte UTF-8");
  }

  private String canonical(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
