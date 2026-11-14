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
class AiMatchingIntegrationTest {
  @Autowired JdbcTemplate db;
  @Autowired MockMvc mvc;
  @Autowired AiMatchingService ai;
  @org.springframework.test.context.bean.override.mockito.MockitoBean com.tutormatch.integration.ai.TextEmbedder embedder;
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

  @Test void providerReceivesOnlyAllowedFieldsAndBothFeedsShareTheSameValue() throws Exception {
    org.mockito.Mockito.when(embedder.embed(org.mockito.ArgumentMatchers.anyList(),org.mockito.ArgumentMatchers.eq(false)))
        .thenAnswer(inv -> {
          List<String> texts=inv.getArgument(0);
          for(var text:texts) for(var forbidden:List.of("PRIVATE",parent.email(),tutor.email(),"0901234567","H? N?i","example.test")) assertFalse(text.contains(forbidden),text);
          return texts.stream().map(x -> new double[]{1,0}).toList();
        });
    var a=ai.parent(studentId,parent.id()).getFirst();
    assertTrue(a.explanation().startsWith("AI"));
    var b=ai.tutor(tutor.id()).getFirst();
    assertEquals(a.level(),b.level()); assertEquals(a.ruleScore(),b.ruleScore());
    org.mockito.Mockito.verify(embedder,org.mockito.Mockito.times(1)).embed(org.mockito.ArgumentMatchers.anyList(),org.mockito.ArgumentMatchers.eq(false));
    assertEquals(a.level(),matching.parentFeed(studentId,parent.id()).cards().getFirst().level());
    assertEquals(a.level(),matching.tutorFeed(tutor.id()).cards().getFirst().level());
  }
  @Test void invalidProviderVectorsFallBackWithoutSavingScores() throws Exception {
    org.mockito.Mockito.when(embedder.embed(org.mockito.ArgumentMatchers.anyList(),org.mockito.ArgumentMatchers.anyBoolean()))
        .thenReturn(List.of(new double[]{Double.NaN,0},new double[]{1,0}));
    var a=ai.parent(studentId,parent.id()).getFirst();
    assertFalse(a.explanation().startsWith("AI"));
    assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM pair_ai_scores WHERE student_id=?",Integer.class,studentId));
  }
  @Test void aiListReturnsTenEligibleProfilesFromTwentyCandidates() throws Exception {
    for(int i=0;i<24;i++) {
      var owner=account("TUTOR");
      String id=UUID.randomUUID().toString();
      db.update("INSERT INTO tutor_profiles(id,user_id,province,hourly_rate,teaching_description,availability,survey,workflow_state,verification_status,display_name) SELECT ?,?,province,hourly_rate,teaching_description,availability,survey,'APPROVED','APPROVED',? FROM tutor_profiles WHERE id=?",id,owner.id(),"Gia sư demo "+i,tutorId);
      db.update("INSERT INTO tutor_subjects(tutor_id,subject) SELECT ?,subject FROM tutor_subjects WHERE tutor_id=?",id,tutorId);
      db.update("INSERT INTO tutor_grades(tutor_id,grade) SELECT ?,grade FROM tutor_grades WHERE tutor_id=?",id,tutorId);
      db.update("INSERT INTO tutor_modes(tutor_id,mode) SELECT ?,mode FROM tutor_modes WHERE tutor_id=?",id,tutorId);
    }
    org.mockito.Mockito.when(embedder.embed(org.mockito.ArgumentMatchers.anyList(),org.mockito.ArgumentMatchers.eq(false)))
        .thenAnswer(inv -> ((List<?>)inv.getArgument(0)).stream().map(x -> new double[]{1,0}).toList());
    var cards=ai.parent(studentId,parent.id());
    assertEquals(10,cards.size());
    assertEquals(10,cards.stream().map(c -> c.tutorId()).distinct().count());
    assertTrue(cards.stream().allMatch(c -> c.explanation().startsWith("AI") && c.canLike()));
  }
  @Test void featuredTutorIsListedFirstInDiscoveryAndAiSuggestions() throws Exception {
    var owner=account("TUTOR");
    String featured=UUID.randomUUID().toString();
    // Worse rule score than the regular tutor: no budget headroom bonus and no experience.
    db.update("INSERT INTO tutor_profiles(id,user_id,province,hourly_rate,teaching_description,availability,survey,workflow_state,verification_status,display_name,experience_years) SELECT ?,?,province,hourly_rate+30000,teaching_description,availability,survey,'APPROVED','APPROVED','Gia sư nổi bật',0 FROM tutor_profiles WHERE id=?",featured,owner.id(),tutorId);
    db.update("INSERT INTO tutor_subjects(tutor_id,subject) SELECT ?,subject FROM tutor_subjects WHERE tutor_id=?",featured,tutorId);
    db.update("INSERT INTO tutor_grades(tutor_id,grade) SELECT ?,grade FROM tutor_grades WHERE tutor_id=?",featured,tutorId);
    db.update("INSERT INTO tutor_modes(tutor_id,mode) SELECT ?,mode FROM tutor_modes WHERE tutor_id=?",featured,tutorId);
    org.mockito.Mockito.when(embedder.embed(org.mockito.ArgumentMatchers.anyList(),org.mockito.ArgumentMatchers.eq(false)))
        .thenAnswer(inv -> ((List<?>)inv.getArgument(0)).stream().map(x -> new double[]{1,0}).toList());
    assertEquals(tutorId,matching.parentFeed(studentId,parent.id()).cards().getFirst().tutorId());
    // AI scores tie (identical vectors), so only the featured flag decides the AI order below.
    db.update("UPDATE tutor_profiles SET featured=TRUE WHERE id=?",featured);
    assertEquals(featured,matching.parentFeed(studentId,parent.id()).cards().getFirst().tutorId());
    assertEquals(featured,ai.parent(studentId,parent.id()).getFirst().tutorId());
    // Featuring only reorders: an ineligible featured tutor never appears.
    db.update("UPDATE tutor_profiles SET hourly_rate=900000 WHERE id=?",featured);
    assertEquals(tutorId,matching.parentFeed(studentId,parent.id()).cards().getFirst().tutorId());
  }
  @Test void savedListIsSortedNewestFirstByMatchOrFirstOkTime() throws Exception {
    var owner=account("TUTOR");
    String second=UUID.randomUUID().toString();
    db.update("INSERT INTO tutor_profiles(id,user_id,province,hourly_rate,teaching_description,availability,survey,workflow_state,verification_status,display_name) SELECT ?,?,province,hourly_rate,teaching_description,availability,survey,'APPROVED','APPROVED','Gia sư thứ hai' FROM tutor_profiles WHERE id=?",second,owner.id(),tutorId);
    db.update("INSERT INTO tutor_subjects(tutor_id,subject) SELECT ?,subject FROM tutor_subjects WHERE tutor_id=?",second,tutorId);
    db.update("INSERT INTO tutor_grades(tutor_id,grade) SELECT ?,grade FROM tutor_grades WHERE tutor_id=?",second,tutorId);
    db.update("INSERT INTO tutor_modes(tutor_id,mode) SELECT ?,mode FROM tutor_modes WHERE tutor_id=?",second,tutorId);
    matching.act(parent,new Action(studentId,tutorId,true));
    matching.act(parent,new Action(studentId,second,true));
    db.update("UPDATE matching_pairs SET first_liked_at=TIMESTAMPADD(HOUR,-3,UTC_TIMESTAMP(6)) WHERE student_id=? AND tutor_id=?",studentId,tutorId);
    db.update("UPDATE matching_pairs SET first_liked_at=TIMESTAMPADD(HOUR,-1,UTC_TIMESTAMP(6)) WHERE student_id=? AND tutor_id=?",studentId,second);
    var saved=matching.parentFeed(studentId,parent.id()).savedCards();
    assertEquals(List.of(second,tutorId),saved.stream().map(c -> c.tutorId()).toList());
    assertNotNull(saved.getFirst().activityAt());
    // The older pair becomes a Match now, so it is the newest activity.
    matching.act(tutor,new Action(studentId,tutorId,true));
    saved=matching.parentFeed(studentId,parent.id()).savedCards();
    assertEquals(List.of(tutorId,second),saved.stream().map(c -> c.tutorId()).toList());
    assertEquals("MATCHED",saved.getFirst().status());
  }
  @Test void timeoutFallsBackBeforeTenSeconds() throws Exception {
    org.mockito.Mockito.when(embedder.embed(org.mockito.ArgumentMatchers.anyList(),org.mockito.ArgumentMatchers.anyBoolean()))
        .thenAnswer(inv -> { Thread.sleep(30000); return List.of(); });
    assertTimeoutPreemptively(java.time.Duration.ofSeconds(10),() -> {
      var a=ai.parent(studentId,parent.id()).getFirst(); assertFalse(a.explanation().startsWith("AI"));
    });
  }
  @Test void hiddenAndIncompatibleProfilesNeverReachProvider() throws Exception {
    db.update("UPDATE tutor_profiles SET hourly_rate=900000 WHERE id=?",tutorId);
    assertTrue(ai.parent(studentId,parent.id()).isEmpty());
    org.mockito.Mockito.verifyNoInteractions(embedder);
  }
}
