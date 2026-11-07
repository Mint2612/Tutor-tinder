package com.tutormatch.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tutormatch.dto.auth.AuthRequests;
import com.tutormatch.dto.profile.StudentRequest;
import com.tutormatch.dto.profile.TutorRequest;
import com.tutormatch.integration.ai.LlmRanker;
import com.tutormatch.integration.delivery.DeliveryGateway;
import com.tutormatch.integration.storage.FileStorage;
import com.tutormatch.model.Account;
import com.tutormatch.security.Tokens;
import com.tutormatch.service.RecommendationService;
import com.tutormatch.service.StudentService;
import com.tutormatch.service.TutorService;
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
  @Autowired StudentService students;
  @Autowired TutorService tutors;
  @Autowired RecommendationService recommendations;
  @Autowired PasswordEncoder encoder;
  @MockitoBean DeliveryGateway delivery;
  @MockitoBean LlmRanker llm;
  @MockitoBean FileStorage storage;
  static final JsonMapper JSON = JsonMapper.builder().build();
  Account parent, other, tutor, admin;
  String tutorId, studentId;
  final Map<String, List<String>> sent = new ConcurrentHashMap<>();
  /** In-memory stand-in for the object store, keyed "bucket/key". */
  final Map<String, FileStorage.StoredFile> objects = new ConcurrentHashMap<>();

  @BeforeEach
  void setup() throws Exception {
    db.update("DELETE FROM auth_throttles");
    doAnswer(
            inv -> {
              objects.put(
                  inv.getArgument(0) + "/" + inv.getArgument(1),
                  new FileStorage.StoredFile(inv.getArgument(2), inv.getArgument(3)));
              return null;
            })
        .when(storage)
        .put(anyString(), anyString(), any(byte[].class), anyString());
    when(storage.get(anyString(), anyString()))
        .thenAnswer(
            inv ->
                Optional.ofNullable(objects.get(inv.getArgument(0) + "/" + inv.getArgument(1))));
    doAnswer(
            inv -> {
              objects.remove(inv.getArgument(0) + "/" + inv.getArgument(1));
              return null;
            })
        .when(storage)
        .delete(anyString(), anyString());
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
    studentId = students.save(null, parent.id(), student(true)).id();
    tutorId = tutors.save(tutor.id(), teaching()).id();
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

  StudentRequest student(boolean consent) {
    return new StudentRequest(
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

  TutorRequest teaching() {
    return new TutorRequest(
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
        List.of("1_EVENING"),
        null);
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
  void tutorActivationRequiresEmailOnlyAndSessionsExpire() throws Exception {
    String email = UUID.randomUUID() + "@example.test";
    var registration =
        new AuthRequests.Registration(
            email, "secure-password-123", "Gia sư", "+84909876543", "TUTOR");
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType("application/json")
                .content(body(registration)))
        .andExpect(status().isOk());
    assertNotNull(sent.get("EMAIL"));
    assertFalse(sent.containsKey("SMS"));
    String login = body(Map.of("email", email, "password", "secure-password-123"));
    mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content(login))
        .andExpect(status().isUnauthorized());
    verifyCode("EMAIL");
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
        .andExpect(jsonPath("$.role").value("TUTOR"));
    db.update(
        "UPDATE auth_sessions SET expires_at=TIMESTAMPADD(HOUR,-1,CURRENT_TIMESTAMP) WHERE"
            + " token_hash=?",
        Tokens.hash(cookie.getValue()));
    mvc.perform(get("/api/auth/me").cookie(cookie)).andExpect(status().isUnauthorized());
  }

  @Test
  void parentNeedsNoVerificationAndCanSignInRightAfterRegistering() throws Exception {
    String email = UUID.randomUUID() + "@example.test";
    var registration =
        new AuthRequests.Registration(
            email, "secure-password-123", "Phụ huynh", "+84909876543", "PARENT");
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType("application/json")
                .content(body(registration)))
        .andExpect(status().isOk());
    assertTrue(sent.isEmpty(), "a parent registration must not send any verification message");
    String login = body(Map.of("email", email, "password", "secure-password-123"));
    var result =
        mvc.perform(
                post("/api/auth/login").with(csrf()).contentType("application/json").content(login))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("PARENT"))
            .andReturn();
    var cookie = result.getResponse().getCookie("TM_SESSION");
    assertNotNull(cookie);
    mvc.perform(get("/api/auth/me").cookie(cookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("PARENT"));
    // The account is usable for parent work straight away.
    mvc.perform(get("/api/students").cookie(cookie))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  void duplicateParentEmailIsRejectedWithGuidanceAndLeavesTheAccountUntouched() throws Exception {
    String email = UUID.randomUUID() + "@example.test";
    String register =
        body(
            new AuthRequests.Registration(
                email, "original-password-123", "Phụ huynh", "+84909876543", "PARENT"));
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType("application/json")
                .content(register))
        .andExpect(status().isOk());
    String again =
        body(
            new AuthRequests.Registration(
                email, "attacker-password-123", "Người khác", "+84900000000", "PARENT"));
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType("application/json")
                .content(again))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("đăng nhập")));
    // A tutor registration for an existing email keeps the generic, non-revealing answer.
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType("application/json")
                .content(
                    body(
                        new AuthRequests.Registration(
                            email, "attacker-password-123", "Gia sư", "+84900000000", "TUTOR"))))
        .andExpect(status().isOk());
    assertTrue(sent.isEmpty(), "no message may be sent for a duplicate registration");
    mvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType("application/json")
                .content(body(Map.of("email", email, "password", "attacker-password-123"))))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType("application/json")
                .content(body(Map.of("email", email, "password", "original-password-123"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("PARENT"));
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
                        new AuthRequests.Registration(
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
    // Editing information keeps the approval: only the evidence is reviewed.
    tutors.save(tutor.id(), teaching());
    mvc.perform(get("/api/tutors/" + tutorId).with(as(parent))).andExpect(status().isOk());
    // New evidence withdraws it until the tutor submits again and an admin reads it.
    mvc.perform(
            multipart("/api/tutor/documents")
                .file(
                    new MockMultipartFile(
                        "file", "second.pdf", "application/pdf", "%PDF-1.4 second".getBytes()))
                .with(as(tutor))
                .with(csrf()))
        .andExpect(status().isOk());
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
        Tokens.hash("fixture-session"),
        parent.id());
    mvc.perform(
            post("/api/auth/forgot")
                .with(csrf())
                .contentType("application/json")
                .content(body(Map.of("email", parent.email()))))
        .andExpect(status().isOk());
    var code = sent.get("RESET");
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
          assertEquals("RULES", result.getFirst().source());
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

  @Test
  void emailOnlyTutorIsEligibleAndPublicDtosDoNotLeakOwnerMetadata() throws Exception {
    approveFixture();
    db.update("UPDATE users SET phone_verified=FALSE WHERE id=?", tutor.id());
    db.update("UPDATE tutor_profiles SET visible=FALSE WHERE id<>?", tutorId);
    mvc.perform(get("/api/students/" + studentId + "/recommendations").with(as(parent)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].tutor.id").value(tutorId));
    mvc.perform(get("/api/tutors/" + tutorId).with(as(parent)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user_id").doesNotExist())
        .andExpect(jsonPath("$.submission_id").doesNotExist())
        .andExpect(jsonPath("$.revision").doesNotExist())
        .andExpect(jsonPath("$.documents").doesNotExist())
        .andExpect(jsonPath("$.audit").doesNotExist());
    mvc.perform(get("/api/tutor/profile").with(as(tutor)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user_id").value(tutor.id()));
  }

  @Test
  void savedInterestsCanBeReopenedAndRemovedOnlyByTheirOwner() throws Exception {
    approveFixture();
    mvc.perform(
            post("/api/swipes")
                .with(as(parent))
                .with(csrf())
                .contentType("application/json")
                .content(
                    body(Map.of("studentId", studentId, "tutorId", tutorId, "interested", true))))
        .andExpect(status().isOk());
    String path = "/api/students/" + studentId + "/interests";
    mvc.perform(get(path).with(as(parent)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(tutorId));
    mvc.perform(get(path).with(as(other))).andExpect(status().isNotFound());
    db.update("UPDATE tutor_profiles SET visible=FALSE WHERE id=?", tutorId);
    mvc.perform(get(path).with(as(parent)))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
    db.update("UPDATE tutor_profiles SET visible=TRUE WHERE id=?", tutorId);
    mvc.perform(delete(path + "/" + tutorId).with(as(other)).with(csrf()))
        .andExpect(status().isNotFound());
    mvc.perform(delete(path + "/" + tutorId).with(as(parent)).with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(get(path).with(as(parent)))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  void resetChallengeLocksAfterRepeatedInvalidGuesses() throws Exception {
    mvc.perform(
            post("/api/auth/forgot")
                .with(csrf())
                .contentType("application/json")
                .content(body(Map.of("email", parent.email()))))
        .andExpect(status().isOk());
    var challenge = sent.get("RESET");
    assertNotNull(challenge);
    for (int i = 0; i < 5; i++)
      mvc.perform(
              post("/api/auth/reset")
                  .with(csrf())
                  .contentType("application/json")
                  .content(
                      body(
                          Map.of(
                              "challengeId",
                              challenge.get(1),
                              "code",
                              "wrong",
                              "password",
                              "changed-password-123"))))
          .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/auth/reset")
                .with(csrf())
                .contentType("application/json")
                .content(
                    body(
                        Map.of(
                            "challengeId",
                            challenge.get(1),
                            "code",
                            challenge.get(0),
                            "password",
                            "changed-password-123"))))
        .andExpect(status().isBadRequest());
    assertEquals(
        6,
        db.queryForObject(
            "SELECT attempts FROM auth_challenges WHERE id=?", Integer.class, challenge.get(1)));
  }

  @Test
  void nullScheduleEntriesAreRejectedBeforePersistence() throws Exception {
    var request = JSON.readValue(body(teaching()), Map.class);
    request.put("availability", Arrays.asList((String) null));
    mvc.perform(
            put("/api/tutor/profile")
                .with(as(tutor))
                .with(csrf())
                .contentType("application/json")
                .content(body(request)))
        .andExpect(status().isBadRequest());
    assertEquals("DRAFT", tutors.own(tutor.id()).workflowState());
  }

  @Test
  void concurrentEmailVerificationDoesNotExhaustTheConnectionPool() throws Exception {
    var challenges = new ArrayList<String>();
    for (int i = 0; i < 12; i++) {
      String user = UUID.randomUUID().toString(), challenge = UUID.randomUUID().toString();
      db.update(
          "INSERT INTO users(id,email,full_name,role) VALUES(?,?,?,'PARENT')",
          user,
          user + "@example.test",
          "Parallel fixture");
      db.update(
          "INSERT INTO auth_challenges(id,user_id,purpose,secret_hash,expires_at)"
              + " VALUES(?,?,'EMAIL',?,TIMESTAMPADD(MINUTE,10,CURRENT_TIMESTAMP))",
          challenge,
          user,
          Tokens.hash("parallel-code"));
      challenges.add(challenge);
    }
    assertTimeoutPreemptively(
        java.time.Duration.ofSeconds(10),
        () -> {
          var start = new java.util.concurrent.CountDownLatch(1);
          try (var executor = java.util.concurrent.Executors.newFixedThreadPool(12)) {
            var results =
                challenges.stream()
                    .map(
                        id ->
                            executor.submit(
                                () -> {
                                  start.await();
                                  return mvc.perform(
                                          post("/api/auth/verify")
                                              .with(csrf())
                                              .contentType("application/json")
                                              .content(
                                                  body(
                                                      Map.of(
                                                          "challengeId",
                                                          id,
                                                          "code",
                                                          "parallel-code"))))
                                      .andReturn()
                                      .getResponse()
                                      .getStatus();
                                }))
                    .toList();
            start.countDown();
            for (var result : results)
              assertEquals(200, result.get(5, java.util.concurrent.TimeUnit.SECONDS));
          }
        });
  }

  /** Grey keeps even the 30-megapixel decompression-bomb fixture at 30 MB of heap. */
  byte[] png(int width, int height) throws Exception {
    var image =
        new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_BYTE_GRAY);
    var out = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(image, "png", out);
    return out.toByteArray();
  }

  RequestBuilder photo(byte[] bytes, String type) {
    return multipart("/api/tutor/photo")
        .file(new MockMultipartFile("file", "avatar", type, bytes))
        .with(as(tutor))
        .with(csrf());
  }

  @Test
  void avatarIsStoredAsNormalisedJpegServedByTheApiAndKeepsAnApprovedProfileApproved()
      throws Exception {
    approveFixture();
    assertEquals("APPROVED", tutors.own(tutor.id()).workflowState());
    String url =
        JSON.readTree(
                mvc.perform(photo(png(1200, 800), "image/png"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.workflow_state").value("APPROVED"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .path("photo_url")
            .asText();
    assertTrue(url.startsWith("/api/media/avatars/") && url.endsWith(".jpg"), url);
    String name = url.substring("/api/media/avatars/".length());
    var stored = objects.get("avatars/" + name);
    assertNotNull(stored, "the photo must be written to the avatars bucket");
    assertEquals("image/jpeg", stored.contentType());
    var image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(stored.content()));
    assertEquals(512, image.getWidth());
    assertEquals(341, image.getHeight());
    // Signed-in users can fetch it; anonymous callers cannot.
    mvc.perform(get(url)).andExpect(status().isUnauthorized());
    mvc.perform(get(url).with(as(parent)))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/jpeg"))
        .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("immutable")))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    mvc.perform(get("/api/media/avatars/not-a-uuid.jpg").with(as(parent)))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/media/avatars/00000000-0000-0000-0000-000000000000.jpg").with(as(parent)))
        .andExpect(status().isNotFound());

    // Replacing the photo discards the old object; removing it clears the reference.
    String second =
        JSON.readTree(
                mvc.perform(photo(png(300, 300), "image/png"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .path("photo_url")
            .asText();
    assertNotEquals(url, second);
    assertFalse(objects.containsKey("avatars/" + name), "the replaced photo must be deleted");
    assertEquals(1, objects.keySet().stream().filter(k -> k.startsWith("avatars/")).count());
    mvc.perform(delete("/api/tutor/photo").with(as(tutor)).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.photo_url").value(""));
    assertTrue(objects.keySet().stream().noneMatch(k -> k.startsWith("avatars/")));
  }

  @Test
  void avatarUploadRejectsNonImagesOversizedFilesDecompressionBombsAndOtherRoles()
      throws Exception {
    mvc.perform(photo("GIF89a-not-allowed".getBytes(), "image/jpeg")).andExpect(status().isBadRequest());
    mvc.perform(photo("plain text".getBytes(), "image/png")).andExpect(status().isBadRequest());
    byte[] truncated = png(200, 200);
    mvc.perform(photo(Arrays.copyOf(truncated, 40), "image/png")).andExpect(status().isBadRequest());
    byte[] big = new byte[2 * 1024 * 1024 + 1];
    big[0] = (byte) 0xFF;
    big[1] = (byte) 0xD8;
    big[2] = (byte) 0xFF;
    mvc.perform(photo(big, "image/jpeg")).andExpect(status().isBadRequest());
    // 30 megapixels compress to a tiny PNG; it must be refused before it is decoded.
    mvc.perform(photo(png(6000, 5000), "image/png")).andExpect(status().isBadRequest());
    mvc.perform(
            multipart("/api/tutor/photo")
                .file(new MockMultipartFile("file", "a.png", "image/png", png(10, 10)))
                .with(as(parent))
                .with(csrf()))
        .andExpect(status().isForbidden());
    assertTrue(objects.isEmpty(), "rejected uploads must leave nothing in storage");
    assertEquals("", tutors.own(tutor.id()).photoUrl());
  }

  @Test
  void profileSaveCanNeitherSetAnExternalPhotoNorWipeTheUploadedOne() throws Exception {
    mvc.perform(photo(png(100, 100), "image/png")).andExpect(status().isOk());
    String uploaded = tutors.own(tutor.id()).photoUrl();
    var request = JSON.readValue(body(teaching()), Map.class);
    request.put("photoUrl", "https://tracker.example/pixel.png");
    mvc.perform(
            put("/api/tutor/profile")
                .with(as(tutor))
                .with(csrf())
                .contentType("application/json")
                .content(body(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.photo_url").value(uploaded));
    request.put("photoUrl", "");
    mvc.perform(
            put("/api/tutor/profile")
                .with(as(tutor))
                .with(csrf())
                .contentType("application/json")
                .content(body(request)))
        .andExpect(status().isOk());
    assertEquals(uploaded, tutors.own(tutor.id()).photoUrl());
  }

  @Test
  void achievementsAreSelfDeclaredLimitedAndShownToParentsOnceApproved() throws Exception {
    var request = JSON.readValue(body(teaching()), Map.class);
    request.put("achievements", "Giải Nhì Toán cấp tỉnh, GPA 3.6/4");
    mvc.perform(
            put("/api/tutor/profile")
                .with(as(tutor))
                .with(csrf())
                .contentType("application/json")
                .content(body(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.achievements").value("Giải Nhì Toán cấp tỉnh, GPA 3.6/4"));
    approveFixture();
    mvc.perform(get("/api/tutors/" + tutorId).with(as(parent)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.achievements").value("Giải Nhì Toán cấp tỉnh, GPA 3.6/4"));
    request.put("achievements", "x".repeat(1001));
    mvc.perform(
            put("/api/tutor/profile")
                .with(as(tutor))
                .with(csrf())
                .contentType("application/json")
                .content(body(request)))
        .andExpect(status().isBadRequest());
    request.put("achievements", "   ");
    mvc.perform(
            put("/api/tutor/profile")
                .with(as(tutor))
                .with(csrf())
                .contentType("application/json")
                .content(body(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.achievements").doesNotExist());
  }

  @Test
  void identityEvidenceLivesInPrivateStorageAndLegacyDatabaseFilesStillOpen() throws Exception {
    byte[] pdf = "%PDF-1.4 evidence fixture".getBytes();
    String doc =
        JSON.readTree(
                mvc.perform(
                        multipart("/api/tutor/documents")
                            .file(new MockMultipartFile("file", "id.pdf", "application/pdf", pdf))
                            .with(as(tutor))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .path("id")
            .asText();
    String key = db.queryForObject("SELECT storage_key FROM tutor_documents WHERE id=?", String.class, doc);
    assertNotNull(key);
    assertNull(db.queryForObject("SELECT content FROM tutor_documents WHERE id=?", byte[].class, doc));
    assertArrayEquals(pdf, objects.get("evidence/" + key).content());
    mvc.perform(get("/api/admin/documents/" + doc).with(as(admin)))
        .andExpect(status().isOk())
        .andExpect(content().bytes(pdf));
    mvc.perform(get("/api/admin/documents/" + doc).with(as(parent))).andExpect(status().isForbidden());
    // A file stored before object storage existed is still read from the database.
    String legacy = UUID.randomUUID().toString();
    db.update(
        "INSERT INTO tutor_documents(id,tutor_id,filename,content_type,content,revision)"
            + " VALUES(?,?,'old.pdf','application/pdf',?,0)",
        legacy,
        tutorId,
        "%PDF-1.4 legacy".getBytes());
    mvc.perform(get("/api/admin/documents/" + legacy).with(as(admin)))
        .andExpect(status().isOk())
        .andExpect(content().bytes("%PDF-1.4 legacy".getBytes()));
    // Removing the document deletes its stored object once the database change commits.
    mvc.perform(delete("/api/tutor/documents/" + doc).with(as(tutor)).with(csrf()))
        .andExpect(status().isNoContent());
    assertFalse(objects.containsKey("evidence/" + key));
  }

  @Test
  void profileEditsAndPhotoChangesNeverSendAnApprovedTutorBackToReview() throws Exception {
    approveFixture();
    var request = JSON.readValue(body(teaching()), Map.class);
    request.put("displayName", "Tên mới sau khi duyệt");
    request.put("hourlyRate", 120000);
    mvc.perform(
            put("/api/tutor/profile")
                .with(as(tutor))
                .with(csrf())
                .contentType("application/json")
                .content(body(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.workflow_state").value("APPROVED"))
        .andExpect(jsonPath("$.verification_status").value("APPROVED"));
    mvc.perform(get("/api/tutors/" + tutorId).with(as(parent)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.display_name").value("Tên mới sau khi duyệt"))
        .andExpect(jsonPath("$.hourly_rate").value(120000));
    mvc.perform(photo(png(80, 80), "image/png")).andExpect(status().isOk());
    mvc.perform(delete("/api/tutor/photo").with(as(tutor)).with(csrf())).andExpect(status().isOk());
    assertEquals("APPROVED", tutors.own(tutor.id()).workflowState());
    mvc.perform(get("/api/tutors/" + tutorId).with(as(parent))).andExpect(status().isOk());
  }

  @Test
  void evidenceChangesWithdrawApprovalButARejectedProfileStaysRejected() throws Exception {
    approveFixture();
    String first = uploadEvidence("a.pdf");
    assertEquals("DRAFT", tutors.own(tutor.id()).workflowState(), "new evidence withdraws approval");
    mvc.perform(post("/api/tutor/submit").with(as(tutor)).with(csrf())).andExpect(status().isOk());
    assertEquals("PENDING_REVIEW", tutors.own(tutor.id()).workflowState());
    mvc.perform(delete("/api/tutor/documents/" + first).with(as(tutor)).with(csrf()))
        .andExpect(status().isNoContent());
    assertEquals("DRAFT", tutors.own(tutor.id()).workflowState(), "removing evidence withdraws a pending submission");
    mvc.perform(post("/api/tutor/submit").with(as(tutor)).with(csrf())).andExpect(status().isBadRequest());
    String second = uploadEvidence("b.pdf");
    mvc.perform(post("/api/tutor/submit").with(as(tutor)).with(csrf())).andExpect(status().isOk());
    mvc.perform(get("/api/admin/documents/" + second).with(as(admin))).andExpect(status().isOk());
    mvc.perform(
            post("/api/admin/tutors/" + tutorId + "/decision")
                .with(as(admin))
                .with(csrf())
                .contentType("application/json")
                .content(body(Map.of("approve", false, "reason", "Ảnh giấy tờ bị mờ"))))
        .andExpect(status().isOk());
    uploadEvidence("c.pdf");
    var rejected = tutors.own(tutor.id());
    assertEquals("REJECTED", rejected.workflowState(), "a rejection stays until the tutor resubmits");
    assertEquals("Ảnh giấy tờ bị mờ", rejected.rejectionReason());
    mvc.perform(post("/api/tutor/submit").with(as(tutor)).with(csrf())).andExpect(status().isOk());
    assertEquals("PENDING_REVIEW", tutors.own(tutor.id()).workflowState());
  }

  String uploadEvidence(String name) throws Exception {
    return JSON.readTree(
            mvc.perform(
                    multipart("/api/tutor/documents")
                        .file(new MockMultipartFile("file", name, "application/pdf", ("%PDF-1.4 " + name).getBytes()))
                        .with(as(tutor))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString())
        .path("id")
        .asText();
  }

  @Test
  void tutorAvailabilityAcceptsExactRangesAndRejectsBrokenOnes() throws Exception {
    java.util.function.Function<List<String>, org.springframework.test.web.servlet.ResultActions> save =
        slots -> {
          try {
            var request = JSON.readValue(body(teaching()), Map.class);
            request.put("availability", slots);
            return mvc.perform(
                put("/api/tutor/profile")
                    .with(as(tutor))
                    .with(csrf())
                    .contentType("application/json")
                    .content(body(request)));
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        };
    save.apply(List.of("1_18:00-20:30", "3_09:00-11:00", "3_14:00-15:00"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.availability[0]").value("1_18:00-20:30"))
        .andExpect(jsonPath("$.availability.length()").value(3));
    // Adjacent ranges and a coarse block next to an exact range are fine.
    save.apply(List.of("2_18:00-19:00", "2_19:00-20:00")).andExpect(status().isOk());
    save.apply(List.of("4_EVENING", "4_19:00-20:00")).andExpect(status().isOk());
    for (List<String> bad :
        List.of(
            List.of("1_20:00-18:00"),
            List.of("1_18:00-18:00"),
            List.of("1_18:00-18:15"),
            List.of("1_18:00-20:00", "1_19:00-21:00"),
            List.of("1_25:00-26:00"),
            List.of("1_18:00-20:00", "1_18:00-20:00"),
            List.of("8_09:00-10:00"),
            List.of("1_9:00-10:00"),
            List.<String>of()))
      save.apply(bad).andExpect(status().isBadRequest());
    assertTrue(tutors.own(tutor.id()).availability().contains("4_19:00-20:00"));
  }

  @Test
  void accountNameAndPhoneCanBeUpdatedButNotEmailOrRole() throws Exception {
    mvc.perform(get("/api/account").with(as(parent)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(parent.email()))
        .andExpect(jsonPath("$.phone").value("+84901234567"));
    mvc.perform(
            put("/api/account")
                .with(as(parent))
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"name\":\"Tên mới\",\"phone\":\"+84911111111\",\"email\":\"x@y.test\",\"role\":\"ADMIN\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Tên mới"))
        .andExpect(jsonPath("$.phone").value("+84911111111"))
        .andExpect(jsonPath("$.email").value(parent.email()))
        .andExpect(jsonPath("$.role").value("PARENT"));
    for (String bad :
        List.of(
            "{\"name\":\"\",\"phone\":\"+84911111111\"}",
            "{\"name\":\"Tên\",\"phone\":\"0911111111\"}",
            "{\"name\":\"Tên\"}")) {
      mvc.perform(
              put("/api/account")
                  .with(as(parent))
                  .with(csrf())
                  .contentType("application/json")
                  .content(bad))
          .andExpect(status().isBadRequest());
    }
    mvc.perform(get("/api/account")).andExpect(status().isUnauthorized());
    // Nobody else's account changes.
    mvc.perform(get("/api/account").with(as(other)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.phone").value("+84901234567"));
  }
}
