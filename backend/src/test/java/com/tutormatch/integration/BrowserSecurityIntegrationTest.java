package com.tutormatch.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tutormatch.mapper.JsonFields;
import com.tutormatch.security.Tokens;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/** Separate context: csrf() test helpers replace the filter repository in other tests. */
@SpringBootTest
@AutoConfigureMockMvc
class BrowserSecurityIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;
  @Autowired PasswordEncoder encoder;

  @Test
  void realCookieAndHeaderAllowLoginAndLogoutRevokesTheSession() throws Exception {
    String id = UUID.randomUUID().toString(), email = id + "@example.test";
    db.update("DELETE FROM auth_throttles");
    db.update(
        "INSERT INTO users(id,email,full_name,role,password_hash,email_verified,phone_verified)"
            + " VALUES(?,?,?,'PARENT',?,TRUE,TRUE)",
        id,
        email,
        "Browser fixture",
        encoder.encode("fixture-password-123"));
    var result = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
    var cookie = result.getResponse().getCookie("XSRF-TOKEN");
    assertNotNull(cookie);
    String token =
        JsonMapper.builder()
            .build()
            .readTree(result.getResponse().getContentAsString())
            .path("token")
            .asText();
    String credentials =
        JsonFields.json(Map.of("email", email, "password", "fixture-password-123"));
    mvc.perform(
            post("/api/auth/login")
                .cookie(cookie)
                .header("X-XSRF-TOKEN", "wrong")
                .contentType("application/json")
                .content(credentials))
        .andExpect(status().isForbidden());
    var login =
        mvc.perform(
                post("/api/auth/login")
                    .cookie(cookie)
                    .header("X-XSRF-TOKEN", token)
                    .contentType("application/json")
                    .content(credentials))
            .andExpect(status().isOk())
            .andExpect(cookie().httpOnly("TM_SESSION", true))
            .andExpect(cookie().maxAge("TM_SESSION", 28800))
            .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
            .andReturn();
    var session = login.getResponse().getCookie("TM_SESSION");
    assertNotNull(session);
    assertFalse(login.getResponse().getContentAsString().contains(session.getValue()));
    assertEquals(
        1,
        db.queryForObject(
            "SELECT COUNT(*) FROM auth_sessions WHERE token_hash=? AND user_id=?",
            Integer.class,
            Tokens.hash(session.getValue()),
            id));
    mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isOk());
    mvc.perform(post("/api/auth/logout").cookie(cookie, session).header("X-XSRF-TOKEN", token))
        .andExpect(status().isOk());
    mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isUnauthorized());
    assertEquals(
        0,
        db.queryForObject(
            "SELECT COUNT(*) FROM auth_sessions WHERE token_hash=?",
            Integer.class,
            Tokens.hash(session.getValue())));
  }

  @Test
  void expiredSessionCanLogoutAndClearCookieWhileCsrfRemainsRequired() throws Exception {
    String id = UUID.randomUUID().toString();
    String rawToken = "expired-session-" + UUID.randomUUID();
    db.update(
        "INSERT INTO users(id,email,full_name,role,email_verified) VALUES(?,?,?,'PARENT',TRUE)",
        id,
        id + "@example.test",
        "Expired session fixture");
    db.update(
        "INSERT INTO auth_sessions(token_hash,user_id,expires_at) VALUES(?,?,TIMESTAMPADD(SECOND,-1,CURRENT_TIMESTAMP))",
        Tokens.hash(rawToken),
        id);
    var session = new jakarta.servlet.http.Cookie("TM_SESSION", rawToken);
    mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/auth/logout").cookie(session)).andExpect(status().isForbidden());

    var csrf = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
    var cookie = csrf.getResponse().getCookie("XSRF-TOKEN");
    assertNotNull(cookie);
    String token = JsonMapper.builder().build()
        .readTree(csrf.getResponse().getContentAsString()).path("token").asText();
    mvc.perform(post("/api/auth/logout").cookie(session, cookie).header("X-XSRF-TOKEN", token))
        .andExpect(status().isOk())
        .andExpect(cookie().httpOnly("TM_SESSION", true))
        .andExpect(cookie().maxAge("TM_SESSION", 0));
    assertEquals(0, db.queryForObject(
        "SELECT COUNT(*) FROM auth_sessions WHERE token_hash=?", Integer.class, Tokens.hash(rawToken)));
    mvc.perform(post("/api/auth/logout").cookie(cookie).header("X-XSRF-TOKEN", token))
        .andExpect(status().isOk()).andExpect(cookie().maxAge("TM_SESSION", 0));
  }
}
