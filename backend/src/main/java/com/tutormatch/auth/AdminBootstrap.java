package com.tutormatch.auth;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.stereotype.Component;

/** Explicit deployment-only provisioning; never available as a public registration role. */
@Component
public class AdminBootstrap implements ApplicationRunner {
  private final AuthService auth;
  private final String email, password;

  public AdminBootstrap(
      AuthService auth,
      @Value("${ADMIN_EMAIL:}") String email,
      @Value("${ADMIN_PASSWORD:}") String password) {
    this.auth = auth;
    this.email = email.trim().toLowerCase(java.util.Locale.ROOT);
    this.password = password;
  }

  public void run(ApplicationArguments args) {
    if (email.isBlank() && password.isBlank()) return;
    if (email.isBlank()
        || password.length() < 16
        || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new IllegalStateException(
          "Admin provisioning requires email and a 16–72 character password");
    if (auth.jdbc().queryForList("SELECT id FROM users WHERE email=?", email).isEmpty())
      auth.jdbc()
          .update(
              "INSERT INTO"
                  + " users(id,email,full_name,role,password_hash,email_verified,phone_verified)"
                  + " VALUES(?,?,?,'ADMIN',?,TRUE,TRUE)",
              UUID.randomUUID().toString(),
              email,
              "Quản trị viên",
              auth.passwords().encode(password));
  }
}
