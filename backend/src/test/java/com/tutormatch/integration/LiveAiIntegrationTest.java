package com.tutormatch.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tutormatch.dto.profile.*;
import com.tutormatch.dto.recommendation.MatchingDtos.Action;
import com.tutormatch.model.Account;
import com.tutormatch.service.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;

/** Acceptance checks against S5-01/02/03; failures expose unfinished requirements. */
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="RUN_LIVE_AI",matches="true")
@SpringBootTest
@AutoConfigureMockMvc
class LiveAiIntegrationTest {
  @Autowired JdbcTemplate db;
  @Autowired MockMvc mvc;
  @Autowired AiMatchingService ai;
  @Autowired MatchingService matching;
  @Autowired StudentService students;
  @Autowired TutorService tutors;
  final JsonMapper json = JsonMapper.builder().build();
  Account parent, tutor;
  String studentId, tutorId;

  Account account(String role) {
    String id = UUID.randomUUID().toString();
    String email = id + "@example.test";
    db.update("INSERT INTO users(id,email,full_name,role,email_verified,phone) VALUES(?,?,?,?,TRUE,?)",
        id, email, "PRIVATE NAME", role, role.equals("PARENT") ? "0901234567" : "0912345678");
    return new Account(id, email, "PRIVATE NAME", role);
  }
  RequestPostProcessor as(Account a) {
    return authentication(new UsernamePasswordAuthenticationToken(a, null,
        List.of(new SimpleGrantedAuthority("ROLE_" + a.role()))));
  }
  @BeforeEach void setup() {
    db.update("UPDATE tutor_profiles SET visible=FALSE");
    db.update("UPDATE users SET email_verified=FALSE WHERE role='PARENT'");
    parent = account("PARENT");
    tutor = account("TUTOR");
    studentId = students.save(null, parent.id(), new StudentRequest("PRIVATE CHILD", 6,
        List.of("Toán"), "Hà Nội", "ONLINE", 200000, List.of("1_EVENING"), "Củng cố nền tảng",
        Map.of("goal", "foundation", "learning", "practice", "difficulty", "understanding",
            "pace", "step_by_step", "support", "feedback", "lesson", "examples"), true)).id();
    tutorId = tutors.save(tutor.id(), new TutorRequest("Gia sư kiểm thử", "", List.of("Toán"),
        List.of(6), List.of("ONLINE"), "Hà Nội", 150000, "Kiên nhẫn", "Thực hành", "Đại học",
        4, List.of("1_EVENING"), null)).id();
    db.update("UPDATE tutor_profiles SET workflow_state='APPROVED',verification_status='APPROVED',visible=TRUE WHERE id=?", tutorId);
  }
  void match() {
    matching.act(parent, new Action(studentId, tutorId, true));
    matching.act(tutor, new Action(studentId, tutorId, true));
  }
  String path() { return "/api/matching/pairs/" + studentId + "/" + tutorId; }
  org.springframework.test.web.servlet.ResultActions decide(Account who, boolean yes) throws Exception {
    return mvc.perform(post("/api/matching/confirm").with(as(who)).with(csrf())
        .contentType("application/json").content(json.writeValueAsString(
            Map.of("studentId", studentId, "tutorId", tutorId, "confirm", yes))));
  }
  void expire() {
    db.update("UPDATE matching_pairs SET confirm_expires_at=TIMESTAMPADD(SECOND,-1,CURRENT_TIMESTAMP) WHERE student_id=? AND tutor_id=?", studentId, tutorId);
  }
  void clearMatchNotices() {
    db.update("DELETE FROM matching_notifications WHERE student_id=? AND tutor_id=?", studentId, tutorId);
  }

  @Test void realProviderRanksAndSharesTheValueAcrossBothSides() {
    var a=ai.parent(studentId,parent.id()).getFirst();
    assertTrue(a.explanation().startsWith("AI"),"Configured live provider must successfully rank, not just fall back");
    var b=ai.tutor(tutor.id()).getFirst();
    assertEquals(a.level(),b.level()); assertEquals(a.ruleScore(),b.ruleScore());
  }
}
