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

/** Real MariaDB acceptance for trial scheduling and concurrent replies. */
@SpringBootTest
@AutoConfigureMockMvc
class TrialSchedulingIntegrationTest {
  @Autowired JdbcTemplate db;
  @Autowired MockMvc mvc;
  @Autowired TrialService trials;
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

  void connected() throws Exception {
    match(); decide(parent,true).andExpect(status().isOk()); decide(tutor,true).andExpect(status().isOk());
  }
  java.util.List<java.time.Instant> futureSlots() {
    return List.of(Instant.now().plusSeconds(172800).truncatedTo(java.time.temporal.ChronoUnit.SECONDS),
        Instant.now().plusSeconds(259200).truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
  }
  com.tutormatch.dto.lesson.TrialDtos.Trial propose(Account who) {
    return trials.propose(who,new com.tutormatch.dto.lesson.TrialDtos.Propose(studentId,tutorId,futureSlots()));
  }
  com.tutormatch.dto.lesson.TrialDtos.Respond response(String action,int version,Instant slot,List<Instant> slots,String reason) {
    return new com.tutormatch.dto.lesson.TrialDtos.Respond(action,version,slot,slots,reason);
  }
  @Test void eitherSideCanProposeCounterAndConfirmAndBothGetNotices() throws Exception {
    connected(); clearMatchNotices();
    var first=propose(tutor);
    assertEquals("ONLINE",first.mode());
    assertEquals(2,first.slots().size());
    assertThrows(com.tutormatch.exception.BusinessException.class,() -> propose(parent));
    assertThrows(com.tutormatch.exception.BusinessException.class,() -> trials.respond(tutor,first.id(),response("ACCEPT",1,first.slots().getFirst(),null,null)));
    var revised=trials.respond(parent,first.id(),response("COUNTER",1,null,List.of(futureSlots().getLast()),null));
    assertEquals(2,revised.version()); assertEquals("PARENT",revised.proposerRole());
    assertThrows(com.tutormatch.exception.BusinessException.class,() -> trials.respond(tutor,first.id(),response("ACCEPT",1,revised.slots().getFirst(),null,null)));
    var confirmed=trials.respond(tutor,first.id(),response("ACCEPT",2,revised.slots().getFirst(),null,null));
    assertEquals("CONFIRMED",confirmed.status());
    assertEquals(revised.slots().getFirst(),confirmed.chosenAt());
    assertFalse(matching.notifications(parent).isEmpty()); assertFalse(matching.notifications(tutor).isEmpty());
    assertThrows(com.tutormatch.exception.BusinessException.class,() -> trials.respond(tutor,first.id(),response("ACCEPT",3,revised.slots().getFirst(),null,null)));
  }
  @Test void cancellationRequiresReasonAndAllowsANewTrial() throws Exception {
    connected(); var t=propose(parent);
    assertThrows(com.tutormatch.exception.BusinessException.class,() -> trials.respond(parent,t.id(),response("CANCEL",1,null,null," ")));
    var closed=trials.respond(tutor,t.id(),response("CANCEL",1,null,null,"??i k? ho?ch"));
    assertEquals("CANCELLED",closed.status()); assertEquals("??i k? ho?ch",closed.cancellationReason());
    assertNotEquals(t.id(),propose(tutor).id());
    assertThrows(com.tutormatch.exception.BusinessException.class,() -> trials.respond(parent,t.id(),response("CANCEL",2,null,null,"L?p l?i")));
  }
  @Test void expiryPersistsOnAccessAndOnRejectedReply() throws Exception {
    connected(); var t=propose(parent);
    db.update("UPDATE trial_schedules SET expires_at=TIMESTAMPADD(SECOND,-1,CURRENT_TIMESTAMP) WHERE id=?",t.id());
    assertThrows(com.tutormatch.exception.BusinessException.class,() -> trials.respond(tutor,t.id(),response("ACCEPT",1,t.slots().getFirst(),null,null)));
    assertEquals("EXPIRED",trials.pair(parent,studentId,tutorId).getFirst().status());
    var next=propose(tutor);
    db.update("UPDATE trial_schedules SET expires_at=TIMESTAMPADD(SECOND,-1,CURRENT_TIMESTAMP) WHERE id=?",next.id());
    assertTrue(trials.list(parent).stream().allMatch(x -> x.status().equals("EXPIRED")));
  }
  @Test void onlyConnectedParticipantsCanScheduleAndCsrfIsRequired() throws Exception {
    match();
    assertThrows(com.tutormatch.exception.BusinessException.class,() -> propose(parent));
    decide(parent,true); decide(tutor,true);
    var t=propose(parent);
    assertThrows(com.tutormatch.exception.BusinessException.class,() -> trials.pair(account("PARENT"),studentId,tutorId));
    assertThrows(com.tutormatch.exception.BusinessException.class,() -> trials.respond(account("TUTOR"),t.id(),response("CANCEL",1,null,null,"Kh?ng thu?c t?i")));
    mvc.perform(post("/api/trials").with(as(parent)).contentType("application/json").content(json.writeValueAsString(Map.of("studentId",studentId,"tutorId",tutorId,"slots",futureSlots().stream().map(Instant::toString).toList())))).andExpect(status().isForbidden());
  }
  @Test void invalidTimesAndOutOfProposalAcceptanceAreRejected() throws Exception {
    connected();
    assertThrows(com.tutormatch.exception.BusinessException.class,() -> trials.propose(parent,
        new com.tutormatch.dto.lesson.TrialDtos.Propose(studentId,tutorId,futureSlots(),"IN_PERSON")));
    for(var values : List.of(List.<Instant>of(),List.of(Instant.now()),List.of(futureSlots().getFirst(),futureSlots().getFirst()),List.of(Instant.now().plusSeconds(100L*86400)))) {
      assertThrows(com.tutormatch.exception.BusinessException.class,() -> trials.propose(parent,new com.tutormatch.dto.lesson.TrialDtos.Propose(studentId,tutorId,values)));
    }
    var t=propose(parent);
    assertThrows(com.tutormatch.exception.BusinessException.class,() -> trials.respond(tutor,t.id(),response("ACCEPT",1,Instant.now().plusSeconds(200000),null,null)));
  }
  @Test void simultaneousProposalsCreateOnlyOneActiveSchedule() throws Exception {
    connected(); var start=new java.util.concurrent.CountDownLatch(1);
    try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var tasks=new ArrayList<java.util.concurrent.Future<Boolean>>();
      for(var who : List.of(parent,tutor)) tasks.add(pool.submit(() -> {
        start.await(); try { propose(who); return true; } catch(com.tutormatch.exception.BusinessException e) { assertEquals(409,e.status()); return false; }
      }));
      start.countDown(); int successes=0;
      for(var task:tasks) if(task.get(20,java.util.concurrent.TimeUnit.SECONDS)) successes++;
      assertEquals(1,successes);
      assertEquals(1,trials.pair(parent,studentId,tutorId).size());
    }
  }
  @Test void simultaneousDuplicateAcceptanceCommitsOnce() throws Exception {
    connected(); var t=propose(parent); var start=new java.util.concurrent.CountDownLatch(1);
    try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var tasks=new ArrayList<java.util.concurrent.Future<Boolean>>();
      for(int i=0;i<2;i++) tasks.add(pool.submit(() -> {
        start.await(); try { trials.respond(tutor,t.id(),response("ACCEPT",1,t.slots().getFirst(),null,null)); return true; }
        catch(com.tutormatch.exception.BusinessException e) { assertEquals(409,e.status()); return false; }
      }));
      start.countDown(); int successes=0;
      for(var task:tasks) if(task.get(20,java.util.concurrent.TimeUnit.SECONDS)) successes++;
      assertEquals(1,successes); assertEquals("CONFIRMED",trials.pair(parent,studentId,tutorId).getFirst().status());
    }
  }
}
