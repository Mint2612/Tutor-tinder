package com.tutormatch.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tutormatch.dto.profile.*;
import com.tutormatch.dto.recommendation.MatchingDtos.Action;
import com.tutormatch.dto.lesson.LessonRequests;
import com.tutormatch.exception.BusinessException;
import com.tutormatch.integration.storage.FileStorage;
import com.tutormatch.integration.delivery.DeliveryGateway;
import com.tutormatch.model.Account;
import com.tutormatch.service.*;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest @AutoConfigureMockMvc
class ProfileManagementIntegrationTest {
  @Autowired JdbcTemplate db; @Autowired MockMvc mvc;
  @Autowired StudentService students; @Autowired TutorService tutors;
  @Autowired MatchingService matching; @Autowired ProfileManagementService management;
  @Autowired AuthService auth; @Autowired PasswordEncoder encoder;
  @Autowired VerificationService verification; @Autowired LessonService lessons;
  @MockitoBean FileStorage storage; @MockitoBean DeliveryGateway delivery;
  final JsonMapper json=JsonMapper.builder().build();
  static final String PASSWORD="management-test-password-2026";
  static String hash;
  Account parent,tutor,other,admin; String studentId,tutorId;
  Account account(String role) {
    String id=UUID.randomUUID().toString();
    db.update("INSERT INTO users(id,email,full_name,role,password_hash,phone,email_verified) VALUES(?,?,?,?,?,'+84900000001',TRUE)",
        id,id+"@example.test","Hồ sơ TEST",role,hash);
    return new Account(id,id+"@example.test","Hồ sơ TEST",role);
  }
  TutorRequest request() {
    return new TutorRequest("Gia sư kiểm thử","",List.of("Toán"),List.of(6),List.of("ONLINE"),
        "Hà Nội",150000,"Hướng dẫn từng bước","Kiên nhẫn","Đại học",3,List.of("1_EVENING"),null);
  }
  String student() {
    return students.save(null,parent.id(),new StudentRequest("Con kiểm thử",6,List.of("Toán"),"Hà Nội",
        "ONLINE",200000,List.of("1_EVENING"),"Củng cố kiến thức",Map.of("goal","foundation","learning","practice",
            "difficulty","understanding","pace","step_by_step","support","feedback","lesson","examples"),true)).id();
  }
  org.springframework.test.web.servlet.request.RequestPostProcessor as(Account a) {
    return authentication(new UsernamePasswordAuthenticationToken(a,null,List.of(new SimpleGrantedAuthority("ROLE_"+a.role()))));
  }
  @BeforeEach void setup() {
    if(hash==null) hash=encoder.encode(PASSWORD);
    db.update("UPDATE tutor_profiles SET visible=FALSE");
    parent=account("PARENT"); tutor=account("TUTOR"); other=account("PARENT"); admin=account("ADMIN");
    studentId=student(); tutorId=tutors.save(tutor.id(),request()).id();
    db.update("UPDATE tutor_profiles SET workflow_state='APPROVED',verification_status='APPROVED',visible=TRUE WHERE id=?",tutorId);
  }
  void matched() {
    matching.act(parent,new Action(studentId,tutorId,true));
    matching.act(tutor,new Action(studentId,tutorId,true));
  }
  @Test void hidingChildBlocksBothDirectionsAndRestoringKeepsDecisions() throws Exception {
    mvc.perform(put("/api/students/"+studentId+"/visibility").with(as(other)).with(csrf()).contentType("application/json").content("{\"visible\":false}"))
        .andExpect(status().isNotFound());
    management.studentVisibility(studentId,parent,false);
    assertFalse(students.owned(studentId,parent.id()).visible());
    assertTrue(matching.parentFeed(studentId,parent.id()).paused());
    assertTrue(matching.parentFeed(studentId,parent.id()).cards().isEmpty());
    assertFalse(matching.tutorFeed(tutor.id()).cards().stream().anyMatch(c -> c.studentId().equals(studentId)));
    assertThrows(BusinessException.class,() -> matching.act(tutor,new Action(studentId,tutorId,true)));
    management.studentVisibility(studentId,parent,true);
    assertFalse(matching.parentFeed(studentId,parent.id()).cards().isEmpty());
    matched(); management.studentVisibility(studentId,parent,false);
    assertEquals(1,matching.parentFeed(studentId,parent.id()).savedCards().size());
    assertTrue(matching.tutorFeed(tutor.id()).savedCards().stream().anyMatch(c -> c.studentId().equals(studentId)));
    management.studentVisibility(studentId,parent,true);
  }
  @Test void childDeletionScrubsDataClosesPairsAndKeepsLessonHistoryWithoutContacts() throws Exception {
    String sibling=student(); matched();
    db.update("UPDATE matching_pairs SET parent_confirm='CONFIRM',tutor_confirm='CONFIRM' WHERE student_id=? AND tutor_id=?",studentId,tutorId);
    var lesson=lessons.request(parent,new LessonRequests.Create(studentId,tutorId,Instant.now().plusSeconds(86400),"Học thử"));
    lessons.respond(tutor,lesson.id(),new LessonRequests.Respond("ACCEPTED"));
    mvc.perform(delete("/api/students/"+studentId).with(as(parent)).with(csrf()).contentType("application/json").content("{\"confirmation\":\"DELETE\"}"))
        .andExpect(status().isNoContent());
    assertEquals(List.of(sibling),students.list(parent.id()).stream().map(s -> s.id()).toList());
    assertThrows(BusinessException.class,() -> students.owned(studentId,parent.id()));
    assertEquals("Hồ sơ đã xóa",db.queryForObject("SELECT nickname FROM student_profiles WHERE id=?",String.class,studentId));
    assertEquals("SKIPPED",db.queryForObject("SELECT status FROM matching_pairs WHERE student_id=? AND tutor_id=?",String.class,studentId,tutorId));
    assertTrue(matching.notifications(tutor).isEmpty());
    mvc.perform(get("/api/lessons").with(as(tutor))).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].parent_email").isEmpty()).andExpect(jsonPath("$[0].parent_phone").isEmpty());
    mvc.perform(put("/api/students/"+studentId+"/visibility").with(as(parent)).with(csrf()).contentType("application/json").content("{\"visible\":true}"))
        .andExpect(status().isNotFound());
    assertTrue(matching.tutorFeed(tutor.id()).cards().stream().noneMatch(c -> c.studentId().equals(studentId)));
  }
  @Test void tutorPauseIsIndependentFromAdminBlockAndPreservesApprovalAndMatch() throws Exception {
    matched(); String fresh=student();
    mvc.perform(put("/api/tutor/availability").with(as(tutor)).with(csrf()).contentType("application/json").content("{\"acceptingStudents\":false}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.accepting_students").value(false)).andExpect(jsonPath("$.workflow_state").value("APPROVED"));
    assertTrue(matching.parentFeed(fresh,parent.id()).cards().isEmpty());
    assertTrue(matching.tutorFeed(tutor.id()).paused());
    assertEquals(1,matching.parentFeed(studentId,parent.id()).savedCards().size());
    db.update("UPDATE tutor_profiles SET visible=FALSE WHERE id=?",tutorId);
    management.availability(tutor,true);
    assertFalse(tutors.own(tutor.id()).visible());
    assertTrue(matching.parentFeed(fresh,parent.id()).cards().isEmpty());
    db.update("UPDATE tutor_profiles SET visible=TRUE WHERE id=?",tutorId);
    assertFalse(matching.parentFeed(fresh,parent.id()).cards().isEmpty());
    mvc.perform(put("/api/tutor/availability").with(as(parent)).with(csrf()).contentType("application/json").content("{\"acceptingStudents\":false}"))
        .andExpect(status().isForbidden());
  }
  @Test void deletingTutorProfileCleansFilesAndRecreationRequiresReviewWithoutResettingDecisions() throws Exception {
    matched();
    db.update("UPDATE matching_pairs SET parent_confirm='CONFIRM',tutor_confirm='CONFIRM' WHERE student_id=? AND tutor_id=?",studentId,tutorId);
    var lesson=lessons.request(parent,new LessonRequests.Create(studentId,tutorId,Instant.now().plusSeconds(86400),"Học thử"));
    lessons.respond(tutor,lesson.id(),new LessonRequests.Respond("ACCEPTED"));
    verification.upload(tutor.id(),"test.pdf","application/pdf","%PDF-1.4\nTEST\n%%EOF".getBytes());
    db.update("UPDATE tutor_profiles SET photo_url='/api/media/avatars/lifecycle-test.jpg',visible=FALSE WHERE id=?",tutorId);
    assertThrows(BusinessException.class,() -> management.deleteTutorProfile(tutor,"wrong"));
    management.deleteTutorProfile(tutor,PASSWORD);
    assertThrows(BusinessException.class,() -> tutors.own(tutor.id()));
    assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM tutor_documents WHERE tutor_id=?",Integer.class,tutorId));
    verify(storage).delete(eq(FileStorage.EVIDENCE),anyString());
    verify(storage).delete(FileStorage.AVATARS,"lifecycle-test.jpg");
    var restored=tutors.save(tutor.id(),request());
    assertEquals(tutorId,restored.id()); assertEquals("DRAFT",restored.workflowState());
    assertFalse(restored.visible()); assertTrue(restored.acceptingStudents());
    assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM matching_daily_usage WHERE actor='TUTOR' AND profile_id=?",Integer.class,tutorId));
    assertEquals("SKIPPED",db.queryForObject("SELECT status FROM matching_pairs WHERE student_id=? AND tutor_id=?",String.class,studentId,tutorId));
    mvc.perform(get("/api/lessons").with(as(parent))).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].tutor_email").isEmpty()).andExpect(jsonPath("$[0].tutor_phone").isEmpty());
  }
  @Test void accountDeletionRequiresPasswordAndConfirmationAndRevokesEverySession() throws Exception {
    matched();
    var first=auth.login(parent.email(),PASSWORD); var second=auth.login(parent.email(),PASSWORD);
    mvc.perform(delete("/api/account").cookie(new Cookie("TM_SESSION",first.token())).with(csrf()).contentType("application/json")
        .content(json.writeValueAsString(Map.of("password","wrong","confirmation","DELETE"))))
        .andExpect(status().isBadRequest());
    assertTrue(auth.authenticate(first.token()).isPresent());
    mvc.perform(delete("/api/account").cookie(new Cookie("TM_SESSION",first.token())).with(csrf()).contentType("application/json")
        .content(json.writeValueAsString(Map.of("password",PASSWORD))))
        .andExpect(status().isBadRequest());
    mvc.perform(delete("/api/account").cookie(new Cookie("TM_SESSION",first.token())).with(csrf()).contentType("application/json")
        .content(json.writeValueAsString(Map.of("password",PASSWORD,"confirmation","DELETE"))))
        .andExpect(status().isNoContent()).andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.containsString("Max-Age=0")));
    assertTrue(auth.authenticate(first.token()).isEmpty()); assertTrue(auth.authenticate(second.token()).isEmpty());
    assertThrows(BusinessException.class,() -> auth.login(parent.email(),PASSWORD));
    assertEquals("Tài khoản đã xóa",db.queryForObject("SELECT full_name FROM users WHERE id=?",String.class,parent.id()));
    assertTrue(students.list(parent.id()).isEmpty()); assertTrue(matching.notifications(tutor).isEmpty());
    assertThrows(BusinessException.class,() -> students.save(null,parent.id(),new StudentRequest("Con",6,List.of("Toán"),"Hà Nội","ONLINE",200000,List.of("1_EVENING"),"Học tập",Map.of(),true)));
  }
  @Test void tutorAccountDeletionRemovesDiscoveryAndCannotBeRestoredThroughStaleProfileSave() {
    var session=auth.login(tutor.email(),PASSWORD);
    management.deleteAccount(tutor,PASSWORD);
    assertTrue(auth.authenticate(session.token()).isEmpty());
    assertTrue(matching.parentFeed(studentId,parent.id()).cards().isEmpty());
    assertThrows(BusinessException.class,() -> tutors.save(tutor.id(),request()));
    assertThrows(BusinessException.class,() -> management.deleteAccount(admin,PASSWORD));
    assertTrue(auth.login(admin.email(),PASSWORD).account().role().equals("ADMIN"));
  }
  @Test void deleteEndpointsEnforceRoleOwnershipCsrfAndConfirmation() throws Exception {
    String body=json.writeValueAsString(Map.of("password",PASSWORD,"confirmation","DELETE"));
    mvc.perform(delete("/api/students/"+studentId).with(as(other)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isNotFound());
    mvc.perform(delete("/api/students/"+studentId).with(as(parent)).contentType("application/json").content(body)).andExpect(status().isForbidden());
    mvc.perform(delete("/api/students/"+studentId).with(as(parent)).with(csrf()).contentType("application/json").content("{}")) .andExpect(status().isBadRequest());
    mvc.perform(delete("/api/tutor/profile").with(as(parent)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
    mvc.perform(delete("/api/account").with(as(admin)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
  }
  @Test void accountDeletionRacingWithLikeCannotLeaveActivePairs() throws Exception {
    var start=new CountDownLatch(1);
    try(var executor=Executors.newFixedThreadPool(2)) {
      var a=executor.submit(() -> { start.await(); management.deleteAccount(parent,PASSWORD); return true; });
      var b=executor.submit(() -> { start.await(); try { matching.act(tutor,new Action(studentId,tutorId,true)); } catch(BusinessException expected) {} return true; });
      start.countDown(); assertTrue(a.get(20,TimeUnit.SECONDS)); assertTrue(b.get(20,TimeUnit.SECONDS));
    }
    assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM matching_pairs WHERE student_id=? AND status IN ('OPEN','MATCHED')",Integer.class,studentId));
    assertTrue(matching.tutorFeed(tutor.id()).cards().stream().noneMatch(c -> c.studentId().equals(studentId)));
  }
}
