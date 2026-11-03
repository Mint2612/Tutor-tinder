package com.tutormatch.platform;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tutormatch.auth.*;
import com.tutormatch.auth.SecurityConfig.Account;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class MvpIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;
  @Autowired ProfileService profiles;
  @Autowired RecommendationService recommendations;
  @Autowired PasswordEncoder encoder;
  @MockitoBean DeliveryGateway delivery;
  @MockitoBean LlmRanker llm;
  static final JsonMapper JSON = JsonMapper.builder().build();
  Account parent, other, tutor, admin;
  String tutorId, studentId;
  final Map<String, List<String>> sent = new ConcurrentHashMap<>();

  @BeforeEach
  void setup() throws Exception {
    db.update("DELETE FROM auth_throttles");
    doAnswer(
            inv -> {
              sent.put(inv.getArgument(0), List.of(inv.getArgument(2), inv.getArgument(3)));
              return null;
            })
        .when(delivery)
        .send(anyString(), anyString(), anyString(), anyString());
    when(llm.rank(anyMap(), anyList())).thenThrow(new IllegalStateException("Provider offline"));
    parent = account("PARENT");
    other = account("PARENT");
    tutor = account("TUTOR");
    admin = account("ADMIN");
    studentId = profiles.saveStudent(null, parent.id(), student(true)).get("id").toString();
    tutorId = profiles.saveTutor(tutor.id(), teaching()).get("id").toString();
  }

  Account account(String role) {
    String id = UUID.randomUUID().toString(), email = id + "@example.test";
    db.update(
        "INSERT INTO"
            + " users(id,email,full_name,role,password_hash,phone,email_verified,phone_verified)"
            + " VALUES(?,?,?,?,?,'+84901234567',TRUE,TRUE)",
        id,
        email,
        "Fixture " + role,
        role,
        encoder.encode("fixture-password-123"));
    return new Account(id, email, "Fixture", role);
  }

  ProfileData.Student student(boolean consent) {
    return new ProfileData.Student(
        "Con",
        6,
        List.of("Toán"),
        "Hà Nội",
        "ONLINE",
        200000,
        List.of("1_EVENING"),
        "Củng cố kiến thức",
        Map.of("learning", "practice"),
        consent);
  }

  ProfileData.Tutor teaching() {
    return new ProfileData.Tutor(
        "Gia sư kiểm thử",
        "",
        List.of("Toán"),
        List.of(6),
        List.of("ONLINE"),
        "Hà Nội",
        150000,
        "Kiên nhẫn, thực hành từng bước",
        "Kiên nhẫn",
        "Đại học, giảng dạy",
        4,
        List.of("1_EVENING"));
  }

  org.springframework.test.web.servlet.request.RequestPostProcessor as(Account u) {
    return authentication(
        new UsernamePasswordAuthenticationToken(
            u, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.role()))));
  }

  String body(Object o) {
    return JSON.writeValueAsString(o);
  }

  void approveFixture() {
    db.update(
        "UPDATE tutor_profiles SET workflow_state='APPROVED',verification_status='APPROVED' WHERE"
            + " id=?",
        tutorId);
  }

  @Test
  void authenticationRequiresBothChannelsAndSessionsExpire() throws Exception {
    String email = UUID.randomUUID() + "@example.test";
    var registration =
        new AuthController.Registration(
            email, "secure-password-123", "Phụ huynh", "+84909876543", "PARENT");
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType("application/json")
                .content(body(registration)))
        .andExpect(status().isOk());
    assertNotNull(sent.get("EMAIL"));
    assertNotNull(sent.get("SMS"));
    String login = body(Map.of("email", email, "password", "secure-password-123"));
    mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content(login))
        .andExpect(status().isUnauthorized());
    verifyCode("EMAIL");
    mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content(login))
        .andExpect(status().isUnauthorized());
    verifyCode("SMS");
    var result =
        mvc.perform(
                post("/api/auth/login").with(csrf()).contentType("application/json").content(login))
            .andExpect(status().isOk())
            .andReturn();
    var cookie = result.getResponse().getCookie("TM_SESSION");
    assertNotNull(cookie);
    assertTrue(cookie.isHttpOnly());
    mvc.perform(get("/api/auth/me").cookie(cookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("PARENT"));
    db.update(
        "UPDATE auth_sessions SET expires_at=TIMESTAMPADD(HOUR,-1,CURRENT_TIMESTAMP) WHERE"
            + " token_hash=?",
        AuthService.hash(cookie.getValue()));
    mvc.perform(get("/api/auth/me").cookie(cookie)).andExpect(status().isUnauthorized());
  }

  void verifyCode(String channel) throws Exception {
    var code = sent.get(channel);
    mvc.perform(
            post("/api/auth/verify")
                .with(csrf())
                .contentType("application/json")
                .content(body(Map.of("challengeId", code.get(1), "code", code.get(0)))))
        .andExpect(status().isOk());
  }

  @Test
  void rolesCsrfOwnershipConsentAndAdminRegistrationAreEnforced() throws Exception {
    mvc.perform(get("/api/students")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/students").with(as(tutor))).andExpect(status().isForbidden());
    mvc.perform(
            post("/api/students")
                .with(as(parent))
                .contentType("application/json")
                .content(body(student(true))))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/students")
                .with(as(parent))
                .with(csrf())
                .contentType("application/json")
                .content(body(student(false))))
        .andExpect(status().isBadRequest());
    mvc.perform(
            put("/api/students/" + studentId)
                .with(as(other))
                .with(csrf())
                .contentType("application/json")
                .content(body(student(true))))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/admin/tutors").with(as(parent))).andExpect(status().isForbidden());
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType("application/json")
                .content(
                    body(
                        new AuthController.Registration(
                            "admin@example.test",
                            "safe-password-123",
                            "Bad admin",
                            "+84901234567",
                            "ADMIN"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void evidenceIsPrivateAndMustBeReadAfterSubmissionBeforeApproval() throws Exception {
    mvc.perform(post("/api/tutor/submit").with(as(tutor)).with(csrf()))
        .andExpect(status().isBadRequest());
    var file =
        new MockMultipartFile(
            "file", "identity.pdf", "application/pdf", "%PDF-1.4 test fixture".getBytes());
    String response =
        mvc.perform(multipart("/api/tutor/documents").file(file).with(as(tutor)).with(csrf()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String doc = JSON.readTree(response).path("id").asText();
    mvc.perform(get("/api/admin/documents/" + doc).with(as(parent)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/documents/" + doc).with(as(tutor)))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/tutor/submit").with(as(tutor)).with(csrf())).andExpect(status().isOk());
    var approve =
        post("/api/admin/tutors/" + tutorId + "/decision")
            .with(as(admin))
            .with(csrf())
            .contentType("application/json")
            .content(body(Map.of("approve", true)));
    mvc.perform(approve).andExpect(status().isBadRequest());
    mvc.perform(get("/api/admin/documents/" + doc).with(as(admin)))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"));
    mvc.perform(approve).andExpect(status().isOk());
    mvc.perform(get("/api/tutors/" + tutorId).with(as(parent))).andExpect(status().isOk());
    profiles.saveTutor(tutor.id(), teaching());
    mvc.perform(get("/api/tutors/" + tutorId).with(as(parent))).andExpect(status().isNotFound());
    mvc.perform(post("/api/tutor/submit").with(as(tutor)).with(csrf())).andExpect(status().isOk());
    mvc.perform(approve).andExpect(status().isBadRequest());
  }

  @Test
  void fallbackFiltersAndTrialLifecycleProtectContactsAndDuplicates() throws Exception {
    // Isolate candidate selection from other fixtures without deleting their audit records.
    db.update("UPDATE tutor_profiles SET visible=FALSE");
    db.update("UPDATE tutor_profiles SET visible=TRUE WHERE id=?", tutorId);
    mvc.perform(get("/api/students/" + studentId + "/recommendations").with(as(parent)))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
    approveFixture();
    mvc.perform(get("/api/students/" + studentId + "/recommendations").with(as(parent)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].source").value("RULES"))
        .andExpect(jsonPath("$[0].tutor.id").value(tutorId));
    mvc.perform(get("/api/students/" + studentId + "/recommendations").with(as(other)))
        .andExpect(status().isNotFound());
    String payload =
        body(
            Map.of(
                "studentId",
                studentId,
                "tutorId",
                tutorId,
                "proposedAt",
                Instant.now().plusSeconds(86400).toString(),
                "message",
                "Xin học thử"));
    String response =
        mvc.perform(
                post("/api/lessons")
                    .with(as(parent))
                    .with(csrf())
                    .contentType("application/json")
                    .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = JSON.readTree(response).path("id").asText();
    mvc.perform(
            post("/api/lessons")
                .with(as(parent))
                .with(csrf())
                .contentType("application/json")
                .content(payload))
        .andExpect(status().isConflict());
    mvc.perform(get("/api/lessons").with(as(parent)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].tutor_email").isEmpty());
    mvc.perform(
            put("/api/lessons/" + id)
                .with(as(other))
                .with(csrf())
                .contentType("application/json")
                .content("{\"status\":\"ACCEPTED\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/lessons/" + id)
                .with(as(tutor))
                .with(csrf())
                .contentType("application/json")
                .content("{\"status\":\"ACCEPTED\"}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/lessons").with(as(parent)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].tutor_email").value(tutor.email()));
    mvc.perform(
            put("/api/lessons/" + id)
                .with(as(tutor))
                .with(csrf())
                .contentType("application/json")
                .content("{\"status\":\"REJECTED\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void resetRevokesSessionsAndInvalidCredentialsShareMessage() throws Exception {
    var bad =
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .contentType("application/json")
                    .content(body(Map.of("email", parent.email(), "password", "wrong"))))
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var absent =
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .contentType("application/json")
                    .content(body(Map.of("email", "missing@example.test", "password", "wrong"))))
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertEquals(bad, absent);
    db.update(
        "INSERT INTO auth_sessions VALUES(?,?,TIMESTAMPADD(HOUR,1,CURRENT_TIMESTAMP))",
        AuthService.hash("fixture-session"),
        parent.id());
    mvc.perform(
            post("/api/auth/forgot")
                .with(csrf())
                .contentType("application/json")
                .content(body(Map.of("email", parent.email()))))
        .andExpect(status().isOk());
    var code = sent.get("EMAIL");
    mvc.perform(
            post("/api/auth/reset")
                .with(csrf())
                .contentType("application/json")
                .content(
                    body(
                        Map.of(
                            "challengeId",
                            code.get(1),
                            "code",
                            code.get(0),
                            "password",
                            "changed-password-123"))))
        .andExpect(status().isOk());
    assertEquals(
        0,
        db.queryForObject(
            "SELECT COUNT(*) FROM auth_sessions WHERE user_id=?", Integer.class, parent.id()));
    mvc.perform(
            post("/api/auth/reset")
                .with(csrf())
                .contentType("application/json")
                .content(
                    body(
                        Map.of(
                            "challengeId",
                            code.get(1),
                            "code",
                            code.get(0),
                            "password",
                            "changed-password-123"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void expiredRequestsDoNotExposeContactsAndCannotBeAccepted() throws Exception {
    approveFixture();
    String id = UUID.randomUUID().toString();
    db.update(
        "INSERT INTO lesson_requests(id,student_id,tutor_id,proposed_at,message,expires_at)"
            + " VALUES(?,?,?,TIMESTAMPADD(DAY,1,CURRENT_TIMESTAMP),'',TIMESTAMPADD(SECOND,-1,CURRENT_TIMESTAMP))",
        id,
        studentId,
        tutorId);
    mvc.perform(
            put("/api/lessons/" + id)
                .with(as(tutor))
                .with(csrf())
                .contentType("application/json")
                .content("{\"status\":\"ACCEPTED\"}"))
        .andExpect(status().isConflict());
    mvc.perform(get("/api/lessons").with(as(parent)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("EXPIRED"))
        .andExpect(jsonPath("$[0].tutor_phone").isEmpty());
  }

  @Test
  void llmDeadlineReturnsFallbackBeforeTenSeconds() throws Exception {
    approveFixture();
    db.update("UPDATE tutor_profiles SET visible=FALSE WHERE id<>?", tutorId);
    doAnswer(
            inv -> {
              Thread.sleep(30000);
              return List.of();
            })
        .when(llm)
        .rank(anyMap(), anyList());
    assertTimeoutPreemptively(
        java.time.Duration.ofSeconds(10),
        () -> {
          var result = recommendations.recommend(studentId, parent.id());
          assertFalse(result.isEmpty());
          assertEquals("RULES", result.getFirst().get("source"));
        });
  }

  @Test
  void swipeInterestIsSeparateFromTrialAcceptance() throws Exception {
    approveFixture();
    mvc.perform(
            post("/api/swipes")
                .with(as(parent))
                .with(csrf())
                .contentType("application/json")
                .content(
                    body(Map.of("studentId", studentId, "tutorId", tutorId, "interested", true))))
        .andExpect(status().isOk());
    assertEquals(
        1,
        db.queryForObject(
            "SELECT COUNT(*) FROM swipe_interactions WHERE student_id=? AND tutor_id=? AND"
                + " interested=TRUE",
            Integer.class,
            studentId,
            tutorId));
    assertEquals(
        0,
        db.queryForObject(
            "SELECT COUNT(*) FROM lesson_requests WHERE student_id=? AND tutor_id=?",
            Integer.class,
            studentId,
            tutorId));
    mvc.perform(get("/api/lessons").with(as(parent)))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  void successfulAiRankingUsesPseudonymsAndSafeLearningAttributes() throws Exception {
    approveFixture();
    db.update("UPDATE tutor_profiles SET visible=FALSE WHERE id<>?", tutorId);
    doAnswer(
            inv -> {
              Map<?, ?> student = inv.getArgument(0);
              List<?> candidates = inv.getArgument(1);
              assertFalse(student.containsKey("nickname"));
              assertFalse(student.containsKey("needs"));
              assertFalse(student.containsKey("province"));
              assertFalse(body(candidates).contains(tutorId));
              assertFalse(body(candidates).contains(tutor.email()));
              return List.of(
                  Map.of(
                      "tutorId",
                      "candidate-0",
                      "level",
                      "GOOD",
                      "strengths",
                      List.of("Hướng dẫn từng bước"),
                      "concerns",
                      List.of("Cần học thử"),
                      "explanation",
                      "Cách dạy thực hành phù hợp với sở thích học tập."));
            })
        .when(llm)
        .rank(anyMap(), anyList());
    mvc.perform(get("/api/students/" + studentId + "/recommendations").with(as(parent)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].source").value("AI"))
        .andExpect(jsonPath("$[0].tutor.id").value(tutorId))
        .andExpect(jsonPath("$[0].tutor.user_id").doesNotExist());
  }

  @Test
  void concurrentPendingRequestsArePreventedByDatabaseConstraint() throws Exception {
    approveFixture();
    String payload =
        body(
            Map.of(
                "studentId",
                studentId,
                "tutorId",
                tutorId,
                "proposedAt",
                Instant.now().plusSeconds(86400).toString(),
                "message",
                ""));
    var start = new java.util.concurrent.CountDownLatch(1);
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<Integer> request =
          () -> {
            start.await();
            return mvc.perform(
                    post("/api/lessons")
                        .with(as(parent))
                        .with(csrf())
                        .contentType("application/json")
                        .content(payload))
                .andReturn()
                .getResponse()
                .getStatus();
          };
      var a = executor.submit(request);
      var b = executor.submit(request);
      start.countDown();
      assertEquals(Set.of(200, 409), Set.of(a.get(), b.get()));
    }
    assertEquals(
        1,
        db.queryForObject(
            "SELECT COUNT(*) FROM lesson_requests WHERE student_id=? AND tutor_id=? AND"
                + " status='PENDING'",
            Integer.class,
            studentId,
            tutorId));
  }
}
