package com.tutormatch.auth;

import static com.tutormatch.platform.ApiErrors.require;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final JdbcTemplate db;
  private final PasswordEncoder encoder;

  public JdbcTemplate jdbc() {
    return db;
  }

  public PasswordEncoder passwords() {
    return encoder;
  }

  final DeliveryGateway delivery;
  private final String dummy;

  public AuthService(JdbcTemplate db, PasswordEncoder encoder, DeliveryGateway delivery) {
    this.db = db;
    this.encoder = encoder;
    this.delivery = delivery;
    dummy = encoder.encode("unmatchable-dummy-password");
  }

  public static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static String token() {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public void throttle(String bucket, int limit) {
    String key = hash(bucket);
    db.update(
        "INSERT INTO auth_throttles(bucket,attempts,window_start) VALUES(?,1,CURRENT_TIMESTAMP) ON"
            + " DUPLICATE KEY UPDATE"
            + " attempts=IF(window_start<TIMESTAMPADD(MINUTE,-15,CURRENT_TIMESTAMP),1,attempts+1),window_start=IF(window_start<TIMESTAMPADD(MINUTE,-15,CURRENT_TIMESTAMP),CURRENT_TIMESTAMP,window_start)",
        key);
    require(
        db.queryForObject("SELECT attempts FROM auth_throttles WHERE bucket=?", Integer.class, key)
            <= limit,
        429,
        "Quá nhiều lần thử. Vui lòng chờ 15 phút.");
  }

  @Transactional
  public void register(AuthController.Registration r) {
    require(
        r.password().getBytes(StandardCharsets.UTF_8).length <= 72,
        400,
        "Mật khẩu tối đa 72 byte UTF-8");
    require(Set.of("PARENT", "TUTOR").contains(r.role()), 400, "Vai trò không hợp lệ");
    String email = r.email().trim().toLowerCase(Locale.ROOT);
    if (!db.queryForList("SELECT id FROM users WHERE email=?", email).isEmpty()) return;
    String id = UUID.randomUUID().toString();
    db.update(
        "INSERT INTO users(id,email,full_name,role,password_hash,phone) VALUES(?,?,?,?,?,?)",
        id,
        email,
        r.name(),
        r.role(),
        encoder.encode(r.password()),
        r.phone());
    challenge(id, "EMAIL", email);
    challenge(id, "PHONE", r.phone());
  }

  void challenge(String user, String purpose, String destination) {
    db.update("DELETE FROM auth_challenges WHERE user_id=? AND purpose=?", user, purpose);
    String code = token(), id = UUID.randomUUID().toString();
    db.update(
        "INSERT INTO auth_challenges(id,user_id,purpose,secret_hash,expires_at) VALUES(?,?,?,?,?)",
        id,
        user,
        purpose,
        hash(code),
        Timestamp.from(Instant.now().plusSeconds(600)));
    delivery.send(purpose.equals("PHONE") ? "SMS" : "EMAIL", destination, code, id);
  }

  @Transactional
  public void resend(String email) {
    var rows =
        db.queryForList("SELECT * FROM users WHERE email=?", email.trim().toLowerCase(Locale.ROOT));
    if (rows.isEmpty()) return;
    var u = rows.getFirst();
    String id = u.get("id").toString();
    if (db.queryForObject("SELECT email_verified FROM users WHERE id=?", Integer.class, id) == 0)
      challenge(id, "EMAIL", u.get("email").toString());
    if (db.queryForObject("SELECT phone_verified FROM users WHERE id=?", Integer.class, id) == 0)
      challenge(id, "PHONE", u.get("phone").toString());
  }

  public void verify(String id, String code) {
    // Increment persists even when validation fails, so guessing cannot roll back the attempt
    // counter.
    db.update("UPDATE auth_challenges SET attempts=attempts+1 WHERE id=?", id);
    var rows =
        db.queryForList(
            "SELECT * FROM auth_challenges WHERE id=? AND attempts<=5 AND"
                + " expires_at>CURRENT_TIMESTAMP AND purpose IN ('EMAIL','PHONE')",
            id);
    require(
        !rows.isEmpty()
            && MessageDigest.isEqual(
                rows.getFirst().get("secret_hash").toString().getBytes(StandardCharsets.UTF_8),
                hash(code).getBytes(StandardCharsets.UTF_8)),
        400,
        "Mã xác minh không hợp lệ hoặc đã hết hạn");
    var row = rows.getFirst();
    String column = row.get("purpose").equals("EMAIL") ? "email_verified" : "phone_verified";
    db.update("UPDATE users SET " + column + "=TRUE WHERE id=?", row.get("user_id"));
    db.update("DELETE FROM auth_challenges WHERE id=?", id);
  }

  public Map<String, Object> login(String email, String password) {
    var rows =
        db.queryForList("SELECT * FROM users WHERE email=?", email.trim().toLowerCase(Locale.ROOT));
    String stored =
        rows.isEmpty() || rows.getFirst().get("password_hash") == null
            ? dummy
            : rows.getFirst().get("password_hash").toString();
    boolean valid =
        password.getBytes(StandardCharsets.UTF_8).length <= 72 && encoder.matches(password, stored);
    require(
        valid && !rows.isEmpty(),
        401,
        "Thông tin đăng nhập không hợp lệ hoặc tài khoản chưa được kích hoạt");
    var u = rows.getFirst();
    String id = u.get("id").toString();
    require(
        db.queryForObject(
                "SELECT email_verified AND phone_verified FROM users WHERE id=?", Integer.class, id)
            == 1,
        401,
        "Thông tin đăng nhập không hợp lệ hoặc tài khoản chưa được kích hoạt");
    return u;
  }

  @Transactional
  public void forgot(String email) {
    var rows =
        db.queryForList(
            "SELECT id,email FROM users WHERE email=?", email.trim().toLowerCase(Locale.ROOT));
    if (!rows.isEmpty())
      challenge(
          rows.getFirst().get("id").toString(), "RESET", rows.getFirst().get("email").toString());
  }

  @Transactional
  public void reset(String id, String code, String password) {
    require(
        password.getBytes(StandardCharsets.UTF_8).length <= 72,
        400,
        "Mật khẩu tối đa 72 byte UTF-8");
    var rows =
        db.queryForList(
            "SELECT * FROM auth_challenges WHERE id=? AND purpose='RESET' AND secret_hash=? AND"
                + " expires_at>CURRENT_TIMESTAMP FOR UPDATE",
            id,
            hash(code));
    require(!rows.isEmpty(), 400, "Mã đặt lại không hợp lệ hoặc hết hạn");
    Object user = rows.getFirst().get("user_id");
    db.update("UPDATE users SET password_hash=? WHERE id=?", encoder.encode(password), user);
    db.update("DELETE FROM auth_sessions WHERE user_id=?", user);
    db.update("DELETE FROM auth_challenges WHERE user_id=?", user);
  }
}
