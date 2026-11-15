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
@SpringBootTest
@AutoConfigureMockMvc
class Sprint5AcceptanceIntegrationTest {
  @Autowired JdbcTemplate db;
  @Autowired MockMvc mvc;
  @Autowired MatchingService matching;
  @Autowired ConnectionService connections;
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

  @Test void detailRequiresMutualMatchOwnershipAndHidesPrivateData() throws Exception {
    mvc.perform(get(path()).with(as(parent))).andExpect(status().isNotFound());
    matching.act(parent, new Action(studentId, tutorId, true));
    mvc.perform(get(path()).with(as(parent))).andExpect(status().isNotFound());
    matching.act(tutor, new Action(studentId, tutorId, true));
    mvc.perform(get(path()).with(as(account("PARENT")))).andExpect(status().isNotFound());
    mvc.perform(get(path()).with(as(account("TUTOR")))).andExpect(status().isNotFound());
    mvc.perform(get(path()).with(as(account("ADMIN")))).andExpect(status().isForbidden());
    db.update("UPDATE tutor_profiles SET background='legacy-contact@example.test 0901234567' WHERE id=?",tutorId);
    var body = mvc.perform(get(path()).with(as(parent))).andExpect(status().isOk())
        .andExpect(jsonPath("$.connected").value(false)).andExpect(jsonPath("$.contact").isEmpty())
        .andExpect(jsonPath("$.tutor.documents").doesNotExist())
        .andExpect(jsonPath("$.tutor.legal_name").doesNotExist()).andReturn().getResponse().getContentAsString();
    assertFalse(body.contains(tutor.email()));
    assertFalse(body.contains("legacy-contact@example.test"));
    assertFalse(body.contains("0901234567"));
    db.update("UPDATE student_profiles SET needs='PRIVATE SCHOOL email@example.test 0901234567' WHERE id=?", studentId);
    body = mvc.perform(get(path()).with(as(tutor))).andExpect(status().isOk())
        .andExpect(jsonPath("$.student.grade").value(6))
        .andExpect(jsonPath("$.student.needs").value(""))
        .andReturn().getResponse().getContentAsString();
    for (var secret : List.of(parent.email(), "PRIVATE CHILD", "PRIVATE NAME", "PRIVATE SCHOOL", "0901234567"))
      assertFalse(body.contains(secret), secret);
  }

  @Test void bothMustConfirmBeforeCrossContactAppearsAndRetriesCannotChangeDecision() throws Exception {
    match();
    decide(parent, true).andExpect(status().isOk()).andExpect(jsonPath("$.contact").isEmpty());
    decide(parent, true).andExpect(status().isOk()).andExpect(jsonPath("$.connected").value(false));
    decide(parent, false).andExpect(status().isConflict());
    mvc.perform(get(path()).with(as(tutor))).andExpect(jsonPath("$.contact").isEmpty());
    decide(tutor, true).andExpect(status().isOk()).andExpect(jsonPath("$.connected").value(true))
        .andExpect(jsonPath("$.contact.email").value(parent.email()))
        .andExpect(jsonPath("$.contact.phone").value("0901234567"));
    mvc.perform(get(path()).with(as(parent))).andExpect(status().isOk())
        .andExpect(jsonPath("$.contact.email").value(tutor.email()))
        .andExpect(jsonPath("$.contact.phone").value("0912345678"));
    expire();
    mvc.perform(get(path()).with(as(parent))).andExpect(status().isOk())
        .andExpect(jsonPath("$.connected").value(true));
  }

  @Test void confirmationRequiresCsrfAndCorrectOwner() throws Exception {
    match();
    mvc.perform(post("/api/matching/confirm").with(as(parent)).contentType("application/json")
        .content(json.writeValueAsString(Map.of("studentId", studentId, "tutorId", tutorId, "confirm", true))))
        .andExpect(status().isForbidden());
    decide(account("PARENT"), true).andExpect(status().isNotFound());
    decide(account("TUTOR"), true).andExpect(status().isNotFound());
    decide(account("ADMIN"), true).andExpect(status().isForbidden());
  }

  @Test void declineClosesPairAndNeverDisclosesContacts() throws Exception {
    match();
    decide(parent, true).andExpect(status().isOk());
    decide(tutor, false).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SKIPPED"))
        .andExpect(jsonPath("$.contact").isEmpty());
    decide(tutor, true).andExpect(status().isConflict());
    mvc.perform(get(path()).with(as(parent))).andExpect(status().isNotFound());
  }

  @Test void declineNotifiesOtherSideWithNeutralClosure() throws Exception {
    match(); clearMatchNotices();
    decide(tutor, false).andExpect(status().isOk());
    assertFalse(matching.notifications(parent).isEmpty(), "S5-02: other side must receive closure notification");
  }

  @Test void expiredDetailPersistsClosedStatusAndNotifiesOtherSide() throws Exception {
    match(); clearMatchNotices(); expire();
    mvc.perform(get(path()).with(as(parent))).andExpect(status().isNotFound());
    assertEquals("EXPIRED", db.queryForObject("SELECT status FROM matching_pairs WHERE student_id=? AND tutor_id=?",
        String.class, studentId, tutorId), "S5-02: expiry must persist after accessing detail");
    assertFalse(matching.notifications(tutor).isEmpty(), "S5-02: expiry must notify other side");
  }

  @Test void expiredConfirmationRejectsAndPersistsExpiryWithoutContact() throws Exception {
    match(); expire();
    decide(parent, true).andExpect(status().isConflict());
    assertEquals("EXPIRED", db.queryForObject("SELECT status FROM matching_pairs WHERE student_id=? AND tutor_id=?",
        String.class, studentId, tutorId), "S5-02: rejected expired confirmation must retain expiry");
  }

  @Test void unconnectedPairCannotCreateTrialRequest() throws Exception {
    match();
    mvc.perform(post("/api/lessons").with(as(parent)).with(csrf()).contentType("application/json")
        .content(json.writeValueAsString(Map.of("studentId", studentId, "tutorId", tutorId,
            "proposedAt", Instant.now().plusSeconds(86400).toString(), "message", "Học thử"))))
        .andExpect(status().is4xxClientError());
  }

  @Test void legacyTutorDetailCannotBypassMutualMatchRequirement() throws Exception {
    mvc.perform(get("/api/tutors/" + tutorId).with(as(parent)))
        .andExpect(status().is4xxClientError());
  }

  @Test void expiryViaFeedClosesPairAndNotifiesOtherSide() {
    match(); clearMatchNotices(); expire();
    matching.parentFeed(studentId, parent.id());
    assertEquals("EXPIRED", db.queryForObject("SELECT status FROM matching_pairs WHERE student_id=? AND tutor_id=?",
        String.class, studentId, tutorId));
    assertFalse(matching.notifications(tutor).isEmpty(), "S5-02: feed-triggered expiry must notify other side");
  }

  @Test void concurrentConfirmationsShareContactsAndNotifyBothExactlyOnce() throws Exception {
    match(); clearMatchNotices();
    var start=new java.util.concurrent.CountDownLatch(1);
    try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var tasks=new ArrayList<java.util.concurrent.Future<?>>();
      for(var who:List.of(parent,tutor)) tasks.add(pool.submit(() -> {
        start.await();
        return connections.confirm(who,new com.tutormatch.dto.recommendation.ConnectionDtos.Decision(studentId,tutorId,true));
      }));
      start.countDown();
      for(var task:tasks) task.get(20,java.util.concurrent.TimeUnit.SECONDS);
    }
    assertTrue(connections.detail(parent,studentId,tutorId).connected());
    assertTrue(connections.detail(tutor,studentId,tutorId).connected());
    assertEquals(1,matching.notifications(parent).size());
    assertEquals(1,matching.notifications(tutor).size());
  }

  @Test void concurrentOppositeDecisionsCannotOverwriteTheFirstDecision() throws Exception {
    match(); var start=new java.util.concurrent.CountDownLatch(1);
    try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var tasks=new ArrayList<java.util.concurrent.Future<Boolean>>();
      for(boolean yes:List.of(true,false)) tasks.add(pool.submit(() -> {
        start.await();
        try { connections.confirm(parent,new com.tutormatch.dto.recommendation.ConnectionDtos.Decision(studentId,tutorId,yes)); return true; }
        catch(com.tutormatch.exception.BusinessException e) { assertEquals(409,e.status()); return false; }
      }));
      start.countDown(); int successes=0;
      for(var task:tasks) if(task.get(20,java.util.concurrent.TimeUnit.SECONDS)) successes++;
      assertEquals(1,successes);
    }
  }
}
