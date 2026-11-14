package com.tutormatch.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tutormatch.model.Account;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AdminMonitorIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;

  private UsernamePasswordAuthenticationToken as(String role) {
    var id = UUID.randomUUID().toString();
    var email = id + "@example.test";
    db.update(
        "INSERT INTO users(id,email,full_name,role,email_verified) VALUES(?,?,'Monitor demo',?,TRUE)",
        id, email, role);
    return new UsernamePasswordAuthenticationToken(
        new Account(id, email, "Monitor demo", role), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
  }

  @Test
  void adminSeesAggregateSnapshotWithSevenQuietOrBusyDays() throws Exception {
    mvc.perform(get("/api/admin/monitor").with(authentication(as("ADMIN"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accounts.admins").isNumber())
        .andExpect(jsonPath("$.accounts.parents").isNumber())
        .andExpect(jsonPath("$.tutors.pendingReview").isNumber())
        .andExpect(jsonPath("$.matching.connected").isNumber())
        .andExpect(jsonPath("$.trials.pending").isNumber())
        .andExpect(jsonPath("$.activity.length()").value(7))
        .andExpect(jsonPath("$.activity[6].newAccounts").isNumber())
        .andExpect(jsonPath("$.server.database").value(true))
        // Counts only: no contact data or family identifiers.
        .andExpect(jsonPath("$..email").doesNotExist())
        .andExpect(jsonPath("$..phone").doesNotExist());
  }

  @Test
  void onlyAdminsMayReadTheMonitor() throws Exception {
    mvc.perform(get("/api/admin/monitor")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/admin/monitor").with(authentication(as("PARENT")))).andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/monitor").with(authentication(as("TUTOR")))).andExpect(status().isForbidden());
  }
}
