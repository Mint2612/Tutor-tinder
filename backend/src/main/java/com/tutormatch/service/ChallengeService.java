package com.tutormatch.service;

import static com.tutormatch.exception.BusinessException.require;

import com.tutormatch.exception.BusinessException;
import com.tutormatch.model.AuthChallenge;
import com.tutormatch.repository.AuthRepository;
import com.tutormatch.security.Tokens;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Atomic challenge consumption, entered after the attempt counter has committed. */
@Service
public class ChallengeService {
  private final AuthRepository users;
  private final PasswordEncoder encoder;

  public ChallengeService(AuthRepository users, PasswordEncoder encoder) {
    this.users = users;
    this.encoder = encoder;
  }

  @Transactional
  public void verifyEmail(String id, String code) {
    var challenge =
        users
            .activeChallenge(id, "EMAIL")
            .orElseThrow(
                () -> new BusinessException(400, "Mã xác minh không hợp lệ hoặc đã hết hạn"));
    require(matches(challenge, code), 400, "Mã xác minh không hợp lệ hoặc đã hết hạn");
    users.verifyEmail(challenge.userId());
    users.deleteChallenge(id);
  }

  @Transactional
  public void resetPassword(String id, String code, String password) {
    var challenge =
        users
            .activeChallenge(id, "RESET")
            .orElseThrow(() -> new BusinessException(400, "Mã đặt lại không hợp lệ hoặc hết hạn"));
    require(matches(challenge, code), 400, "Mã đặt lại không hợp lệ hoặc hết hạn");
    users.changePassword(challenge.userId(), encoder.encode(password));
  }

  private boolean matches(AuthChallenge challenge, String code) {
    return MessageDigest.isEqual(
        challenge.secretHash().getBytes(StandardCharsets.UTF_8),
        Tokens.hash(code).getBytes(StandardCharsets.UTF_8));
  }
}
