package com.tutormatch.repository;

import com.tutormatch.mapper.AccountMapper;
import com.tutormatch.model.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;

@Repository
public class AuthRepository {
  private final JdbcTemplate db;
  private final AccountMapper mapper;

  public AuthRepository(JdbcTemplate db, AccountMapper mapper) {
    this.db = db;
    this.mapper = mapper;
  }

  public Optional<UserCredentials> findByEmail(String email) {
    return db.queryForList("SELECT * FROM users WHERE email=?", email).stream()
        .findFirst()
        .map(mapper::credentials);
  }

  public Optional<UserCredentials> lockByEmail(String email) {
    return db.queryForList("SELECT * FROM users WHERE email=? FOR UPDATE", email).stream()
        .findFirst()
        .map(mapper::credentials);
  }

  public Optional<com.tutormatch.dto.auth.AccountProfile> profile(String id) {
    return db.queryForList("SELECT id,email,full_name,role,phone FROM users WHERE id=?", id).stream()
        .findFirst()
        .map(mapper::profile);
  }

  public void updateProfile(String id, String name, String phone) {
    db.update("UPDATE users SET full_name=?,phone=? WHERE id=?", name, phone, id);
  }

  public void create(
      String id,
      String email,
      String name,
      String role,
      String hash,
      String phone,
      boolean verified) {
    db.update(
        "INSERT INTO users(id,email,full_name,role,password_hash,phone,email_verified)"
            + " VALUES(?,?,?,?,?,?,?)",
        id,
        email,
        name,
        role,
        hash,
        phone,
        verified);
  }

  public void replaceChallenge(
      String id, String user, String purpose, String hash, Instant expires) {
    db.update("DELETE FROM auth_challenges WHERE user_id=? AND purpose=?", user, purpose);
    db.update(
        "INSERT INTO auth_challenges(id,user_id,purpose,secret_hash,expires_at) VALUES(?,?,?,?,?)",
        id,
        user,
        purpose,
        hash,
        Timestamp.from(expires));
  }

  /** Invalid guesses must remain counted even when the surrounding validation rolls back. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void incrementChallengeAttempts(String id) {
    db.update("UPDATE auth_challenges SET attempts=attempts+1 WHERE id=?", id);
  }

  public Optional<AuthChallenge> activeChallenge(String id, String purpose) {
    return db
        .queryForList(
            "SELECT id,user_id,purpose,secret_hash FROM auth_challenges WHERE id=? AND purpose=?"
                + " AND attempts<=5 AND expires_at>CURRENT_TIMESTAMP FOR UPDATE",
            id,
            purpose)
        .stream()
        .findFirst()
        .map(
            r ->
                new AuthChallenge(
                    r.get("id").toString(),
                    r.get("user_id").toString(),
                    r.get("purpose").toString(),
                    r.get("secret_hash").toString()));
  }

  public void verifyEmail(String user) {
    db.update("UPDATE users SET email_verified=TRUE WHERE id=?", user);
  }

  public void deleteChallenge(String id) {
    db.update("DELETE FROM auth_challenges WHERE id=?", id);
  }

  public void changePassword(String user, String password) {
    db.update("UPDATE users SET password_hash=? WHERE id=?", password, user);
    db.update("DELETE FROM auth_sessions WHERE user_id=?", user);
    db.update("DELETE FROM auth_challenges WHERE user_id=?", user);
  }

  public void createSession(String hash, String user, Instant expires) {
    db.update(
        "INSERT INTO auth_sessions(token_hash,user_id,expires_at) VALUES(?,?,?)",
        hash,
        user,
        Timestamp.from(expires));
  }

  public void deleteSession(String hash) {
    db.update("DELETE FROM auth_sessions WHERE token_hash=?", hash);
  }

  public Optional<Account> sessionAccount(String hash) {
    return db
        .queryForList(
            "SELECT u.id,u.email,u.full_name,u.role FROM auth_sessions s JOIN users u ON"
                + " u.id=s.user_id WHERE s.token_hash=? AND s.expires_at>CURRENT_TIMESTAMP AND"
                + " u.email_verified=TRUE",
            hash)
        .stream()
        .findFirst()
        .map(mapper::account);
  }

  public int recordAttempt(String bucket) {
    db.update(
        "INSERT INTO auth_throttles(bucket,attempts,window_start) VALUES(?,1,CURRENT_TIMESTAMP) ON"
            + " DUPLICATE KEY UPDATE"
            + " attempts=IF(window_start<TIMESTAMPADD(MINUTE,-15,CURRENT_TIMESTAMP),1,attempts+1),window_start=IF(window_start<TIMESTAMPADD(MINUTE,-15,CURRENT_TIMESTAMP),CURRENT_TIMESTAMP,window_start)",
        bucket);
    return db.queryForObject(
        "SELECT attempts FROM auth_throttles WHERE bucket=?", Integer.class, bucket);
  }
}
