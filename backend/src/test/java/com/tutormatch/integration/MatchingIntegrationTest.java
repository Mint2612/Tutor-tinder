package com.tutormatch.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tutormatch.dto.profile.*;
import com.tutormatch.dto.recommendation.MatchingDtos.*;
import com.tutormatch.exception.BusinessException;
import com.tutormatch.model.Account;
import com.tutormatch.service.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class MatchingIntegrationTest {
  @Autowired JdbcTemplate db;
  @Autowired MockMvc mvc;
  @Autowired MatchingService matching;
  @Autowired StudentService students;
  @Autowired TutorService tutors;
  final JsonMapper json=JsonMapper.builder().build();
  Account parent,tutor;
  String studentId,tutorId;
  static final Map<String,String> SURVEY=Map.of("goal","foundation","learning","practice",
      "difficulty","understanding","pace","step_by_step","support","feedback","lesson","examples");

  Account account(String role) {
    String id=UUID.randomUUID().toString();
    db.update("INSERT INTO users(id,email,full_name,role,email_verified) VALUES(?,?,?,?,TRUE)",
        id,id+"@example.test","PRIVATE NAME",role);
    return new Account(id,id+"@example.test","PRIVATE NAME",role);
  }
  String student(Account owner) {
    var s=students.save(null,owner.id(),new StudentRequest("PRIVATE CHILD",6,List.of("Toán"),"Hà Nội",
        "ONLINE",200000,List.of("1_EVENING"),"Củng cố nền tảng học tập",SURVEY,true));
    // Simulate an older stored narrative: the new intake rejects contact data, and matching must
    // also keep historical identity/contact details out of anonymous previews.
    db.update("UPDATE student_profiles SET needs=? WHERE id=?", "PRIVATE SCHOOL email@example.test 0901234567",s.id());
    return s.id();
  }
  String teaching(Account owner) {
    var t=tutors.save(owner.id(),new TutorRequest("Gia sư", "",List.of("Toán"),List.of(6),
        List.of("ONLINE","IN_PERSON"),"Hà Nội",150000,"Kiên nhẫn","Thực hành","Đại học",4,List.of("1_EVENING"),null));
    db.update("UPDATE tutor_profiles SET workflow_state='APPROVED',verification_status='APPROVED',visible=TRUE WHERE id=?",t.id());
    return t.id();
  }
  org.springframework.test.web.servlet.request.RequestPostProcessor as(Account a) {
    return authentication(new UsernamePasswordAuthenticationToken(a,null,List.of(new SimpleGrantedAuthority("ROLE_"+a.role()))));
  }
  Result choose(Account a,String s,String t,boolean like) { return matching.act(a,new Action(s,t,like)); }
  Card incomingTutorCard(String student,String owner,String tutor) {
    return matching.parentFeed(student,owner).cards().stream().filter(c -> c.tutorId().equals(tutor)).findFirst().orElseThrow();
  }
  @BeforeEach void setup() {
    db.update("UPDATE tutor_profiles SET visible=FALSE");
    db.update("UPDATE users SET email_verified=FALSE WHERE role='PARENT'");
    parent=account("PARENT"); tutor=account("TUTOR");
    studentId=student(parent); tutorId=teaching(tutor);
  }

  @Test void twoWayFeedsAreAnonymousAndUseSharedRules() throws Exception {
    var p=incomingTutorCard(studentId,parent.id(),tutorId);
    var t=matching.tutorFeed(tutor.id()).cards().stream().filter(c -> c.studentId().equals(studentId)).findFirst().orElseThrow();
    assertEquals(p.level(),t.level()); assertEquals("SUGGESTED",p.tag());
    String preview=json.writeValueAsString(t);
    for(var secret:List.of("PRIVATE CHILD","PRIVATE SCHOOL","PRIVATE NAME","0901234567","email@example.test",parent.email(),parent.id())) assertFalse(preview.contains(secret),secret);
    assertNull(t.tutor()); assertNull(p.student());
    mvc.perform(get("/api/students/"+studentId+"/matching").with(as(tutor))).andExpect(status().isForbidden());
    mvc.perform(get("/api/students/"+studentId+"/matching").with(as(account("PARENT")))).andExpect(status().isNotFound());
    mvc.perform(get("/api/tutor/matching").with(as(parent))).andExpect(status().isForbidden());
    mvc.perform(post("/api/matching/actions").with(as(parent)).contentType("application/json")
        .content(json.writeValueAsString(new Action(studentId,tutorId,true)))).andExpect(status().isForbidden());
    assertThrows(BusinessException.class,() -> choose(account("ADMIN"),studentId,tutorId,true));
    assertThrows(BusinessException.class,() -> choose(account("TUTOR"),studentId,tutorId,true));
  }

  @Test void tutorLikesFirstThenParentMatchesAndBothReceivePersistentNotifications() {
    assertFalse(choose(tutor,studentId,tutorId,true).matched());
    var first=matching.parentFeed(studentId,parent.id()).cards().getFirst();
    assertEquals(tutorId,first.tutorId()); assertEquals("INTERESTED_IN_YOU",first.tag());
    assertTrue(choose(parent,studentId,tutorId,true).matched());
    assertEquals(1,matching.parentFeed(studentId,parent.id()).matches().size());
    assertTrue(matching.tutorFeed(tutor.id()).matches().stream().anyMatch(p -> p.studentId().equals(studentId)));
    assertEquals(1,matching.notifications(parent).size()); assertEquals(1,matching.notifications(tutor).size());
    assertTrue(choose(parent,studentId,tutorId,true).matched());
    assertEquals(1,matching.notifications(parent).size());
    assertEquals(9,matching.parentFeed(studentId,parent.id()).limits().totalRemaining());
    assertEquals(2,matching.parentFeed(studentId,parent.id()).limits().incomingRemaining());
    var note=matching.notifications(parent).getFirst();
    assertThrows(BusinessException.class,() -> matching.readNotification(note.id(),tutor));
    matching.readNotification(note.id(),parent); assertTrue(matching.notifications(parent).isEmpty());
    assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM lesson_requests WHERE student_id=?",Integer.class,studentId));
  }

  @Test void parentLikesFirstAndSkipClosesForBothWithoutNotification() {
    choose(parent,studentId,tutorId,true);
    assertEquals("INTERESTED_IN_YOU",matching.tutorFeed(tutor.id()).cards().getFirst().tag());
    choose(tutor,studentId,tutorId,false);
    assertTrue(matching.parentFeed(studentId,parent.id()).cards().isEmpty());
    assertTrue(matching.parentFeed(studentId,parent.id()).savedCards().isEmpty());
    assertFalse(matching.tutorFeed(tutor.id()).cards().stream().anyMatch(c -> c.studentId().equals(studentId)));
    assertTrue(matching.notifications(parent).isEmpty()); assertTrue(matching.notifications(tutor).isEmpty());
    assertThrows(BusinessException.class,() -> choose(parent,studentId,tutorId,true));
  }

  @Test void incomingQuotaKeepsCardAndStillAllowsSkipAndIsPerStudent() {
    List<String> ids=new ArrayList<>(); List<Account> owners=new ArrayList<>();
    for(int i=0;i<4;i++) { var owner=account("TUTOR"); owners.add(owner); var id=teaching(owner); ids.add(id); choose(owner,studentId,id,true); }
    for(int i=0;i<3;i++) choose(parent,studentId,ids.get(i),true);
    var card=incomingTutorCard(studentId,parent.id(),ids.get(3));
    assertFalse(card.canLike());
    assertEquals(429,assertThrows(BusinessException.class,() -> choose(parent,studentId,ids.get(3),true)).status());
    assertEquals(7,matching.parentFeed(studentId,parent.id()).limits().totalRemaining());
    assertFalse(incomingTutorCard(studentId,parent.id(),ids.get(3)).canLike());
    choose(parent,studentId,ids.get(3),false);
    String sibling=student(parent);
    assertEquals(10,matching.parentFeed(sibling,parent.id()).limits().totalRemaining());
    assertEquals(3,matching.parentFeed(sibling,parent.id()).limits().incomingRemaining());
  }

  @Test void tutorIncomingQuotaAndDailyResetAreEnforced() {
    var ids=new ArrayList<String>();
    for(int i=0;i<4;i++) { var p=account("PARENT"); var s=student(p); ids.add(s); choose(p,s,tutorId,true); }
    for(int i=0;i<3;i++) choose(tutor,ids.get(i),tutorId,true);
    assertEquals(429,assertThrows(BusinessException.class,() -> choose(tutor,ids.get(3),tutorId,true)).status());
    assertFalse(matching.tutorFeed(tutor.id()).cards().stream().filter(c -> c.studentId().equals(ids.get(3))).findFirst().orElseThrow().canLike());
    db.update("UPDATE matching_daily_usage SET usage_day=DATE_SUB(usage_day,INTERVAL 1 DAY) WHERE actor='TUTOR' AND profile_id=?",tutorId);
    assertEquals(10,matching.tutorFeed(tutor.id()).limits().totalRemaining());
    assertTrue(choose(tutor,ids.get(3),tutorId,true).matched());
  }

  @Test void concurrentLikesCannotExceedDailyTotalAndRetriesDoNotDoubleCount() throws Exception {
    var ids=new ArrayList<String>();
    for(int i=0;i<12;i++) ids.add(student(account("PARENT")));
    var start=new CountDownLatch(1);
    try(var executor=Executors.newFixedThreadPool(12)) {
      var tasks=new ArrayList<Future<Integer>>();
      for(var s:ids) tasks.add(executor.submit(() -> { start.await(); try { choose(tutor,s,tutorId,true); return 200; } catch(BusinessException e) { return e.status(); } }));
      start.countDown();
      int ok=0,limited=0;
      for(var task:tasks) { int status=task.get(30,TimeUnit.SECONDS); if(status==200) ok++; else if(status==429) limited++; else fail("Unexpected "+status); }
      assertEquals(10,ok); assertEquals(2,limited);
    }
    assertEquals(0,matching.tutorFeed(tutor.id()).limits().totalRemaining());
    assertTrue(matching.tutorFeed(tutor.id()).cards().stream().allMatch(c -> !c.canLike()));
    String liked=db.queryForObject("SELECT student_id FROM matching_pairs WHERE tutor_id=? AND tutor_choice='LIKE' LIMIT 1",String.class,tutorId);
    choose(tutor,liked,tutorId,true);
    assertEquals(10,db.queryForObject("SELECT total_used FROM matching_daily_usage WHERE actor='TUTOR' AND profile_id=?",Integer.class,tutorId));
    String skipped=matching.tutorFeed(tutor.id()).cards().getFirst().studentId();
    choose(tutor,skipped,tutorId,false);
  }

  @Test void expiryClosesBothFeedsAndLateActionsCannotMatch() {
    choose(parent,studentId,tutorId,true);
    db.update("UPDATE matching_pairs SET expires_at=TIMESTAMPADD(SECOND,-1,CURRENT_TIMESTAMP) WHERE student_id=? AND tutor_id=?",studentId,tutorId);
    assertThrows(BusinessException.class,() -> choose(tutor,studentId,tutorId,true));
    assertFalse(matching.tutorFeed(tutor.id()).cards().stream().anyMatch(c -> c.studentId().equals(studentId)));
    assertTrue(matching.parentFeed(studentId,parent.id()).savedCards().isEmpty());
    assertEquals("EXPIRED",db.queryForObject("SELECT status FROM matching_pairs WHERE student_id=? AND tutor_id=?",String.class,studentId,tutorId));
  }

  @Test void concurrentParentLikesRespectTotalQuotaAndEachStudentIsIndependent() throws Exception {
    var ids=new ArrayList<String>();
    for(int i=0;i<11;i++) ids.add(teaching(account("TUTOR")));
    var start=new CountDownLatch(1);
    try(var executor=Executors.newFixedThreadPool(11)) {
      var tasks=new ArrayList<Future<Integer>>();
      for(var t:ids) tasks.add(executor.submit(() -> { start.await(); try { choose(parent,studentId,t,true); return 200; } catch(BusinessException e) { return e.status(); } }));
      start.countDown(); int ok=0,limited=0;
      for(var task:tasks) { int status=task.get(30,TimeUnit.SECONDS); if(status==200) ok++; else if(status==429) limited++; else fail("Unexpected "+status); }
      assertEquals(10,ok); assertEquals(1,limited);
    }
    assertEquals(0,matching.parentFeed(studentId,parent.id()).limits().totalRemaining());
    assertEquals(10,matching.parentFeed(student(parent),parent.id()).limits().totalRemaining());
  }

  @Test void simultaneousReciprocalLikesCreateOneMatchAndExactlyTwoNotices() throws Exception {
    var start=new CountDownLatch(1);
    try(var executor=Executors.newFixedThreadPool(2)) {
      var a=executor.submit(() -> { start.await(); return choose(parent,studentId,tutorId,true); });
      var b=executor.submit(() -> { start.await(); return choose(tutor,studentId,tutorId,true); });
      start.countDown();
      int matched=(a.get(15,TimeUnit.SECONDS).matched()?1:0)+(b.get(15,TimeUnit.SECONDS).matched()?1:0);
      assertEquals(1,matched);
    }
    assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM matching_notifications WHERE student_id=? AND tutor_id=?",Integer.class,studentId,tutorId));
    assertEquals(1,matching.parentFeed(studentId,parent.id()).matches().size());
  }

  @Test void incompleteSurveyUnapprovedHiddenAndMandatoryConstraintsNeverEnterFeeds() {
    db.update("UPDATE student_profiles SET survey='{}' WHERE id=?",studentId);
    assertThrows(BusinessException.class,() -> matching.parentFeed(studentId,parent.id()));
    assertFalse(matching.tutorFeed(tutor.id()).cards().stream().anyMatch(c -> c.studentId().equals(studentId)));
    db.update("UPDATE student_profiles SET survey=? WHERE id=?",json.writeValueAsString(SURVEY),studentId);
    db.update("UPDATE tutor_profiles SET visible=FALSE WHERE id=?",tutorId);
    assertTrue(matching.parentFeed(studentId,parent.id()).cards().isEmpty());
    assertThrows(BusinessException.class,() -> matching.tutorFeed(tutor.id()));
    db.update("UPDATE tutor_profiles SET visible=TRUE,workflow_state='PENDING_REVIEW' WHERE id=?",tutorId);
    assertTrue(matching.parentFeed(studentId,parent.id()).cards().isEmpty());
    db.update("UPDATE tutor_profiles SET workflow_state='APPROVED' WHERE id=?",tutorId);
    db.update("UPDATE users SET email_verified=FALSE WHERE id=?",tutor.id());
    assertTrue(matching.parentFeed(studentId,parent.id()).cards().isEmpty());
    db.update("UPDATE users SET email_verified=TRUE WHERE id=?",tutor.id());
    db.update("UPDATE student_profiles SET mode='IN_PERSON',province='Huế' WHERE id=?",studentId);
    assertTrue(matching.parentFeed(studentId,parent.id()).cards().isEmpty());
    db.update("UPDATE student_profiles SET mode='ONLINE' WHERE id=?",studentId);
    assertFalse(matching.parentFeed(studentId,parent.id()).cards().isEmpty());
    db.update("UPDATE student_profiles SET max_hourly_rate=1000 WHERE id=?",studentId);
    assertTrue(matching.parentFeed(studentId,parent.id()).cards().isEmpty());
  }
}
