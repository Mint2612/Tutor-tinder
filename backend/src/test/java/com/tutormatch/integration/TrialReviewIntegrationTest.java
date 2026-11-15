package com.tutormatch.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.tutormatch.dto.lesson.TrialReviewDtos.Submit;
import com.tutormatch.exception.BusinessException;
import com.tutormatch.model.Account;
import com.tutormatch.service.TrialReviewService;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class TrialReviewIntegrationTest {
  @Autowired JdbcTemplate db;
  @Autowired TrialReviewService reviews;
  @Autowired MockMvc mvc;
  Account parent,tutor,other;
  String student,teaching,trial;
  Account account(String role) {
    String id=UUID.randomUUID().toString();
    String email=id+"@example.test";
    db.update("INSERT INTO users(id,email,full_name,role,email_verified) VALUES(?,?,?,?,TRUE)",id,email,"Demo "+role,role);
    return new Account(id,email,"Demo "+role,role);
  }
  RequestPostProcessor as(Account a) {
    return authentication(new UsernamePasswordAuthenticationToken(a,null,List.of(new SimpleGrantedAuthority("ROLE_"+a.role()))));
  }
  @BeforeEach void setup() {
    parent=account("PARENT"); tutor=account("TUTOR"); other=account("PARENT");
    student=UUID.randomUUID().toString(); teaching=UUID.randomUUID().toString(); trial=UUID.randomUUID().toString();
    db.update("INSERT INTO student_profiles(id,parent_id,nickname,grade,province,mode,max_hourly_rate,availability,survey,needs,consent_at) VALUES(?,?,'Demo student',6,'Demo','ONLINE',200000,'[]','{}','Demo',CURRENT_TIMESTAMP)",student,parent.id());
    db.update("INSERT INTO tutor_profiles(id,user_id,province,hourly_rate,teaching_description,availability,survey,display_name) VALUES(?,?,'Demo',150000,'Demo','[]','{}','Demo tutor')",teaching,tutor.id());
    db.update("INSERT INTO matching_pairs(student_id,tutor_id,status,parent_choice,tutor_choice,parent_confirm,tutor_confirm) VALUES(?,?,'MATCHED','LIKE','LIKE','CONFIRM','CONFIRM')",student,teaching);
    db.update("INSERT INTO trial_schedules(id,student_id,tutor_id,proposer_role,slots,status,chosen_at,expires_at) VALUES(?,?,?,'PARENT','[]','CONFIRMED',TIMESTAMPADD(DAY,-1,CURRENT_TIMESTAMP),CURRENT_TIMESTAMP)",trial,student,teaching);
  }
  @Test void ownerCanSubmitAndReadSavedReview() throws Exception {
    mvc.perform(get("/api/trial-reviews").with(as(parent))).andExpect(status().isOk()).andExpect(jsonPath("$[0].rating").isEmpty());
    mvc.perform(post("/api/trial-reviews/"+trial).with(as(parent)).with(csrf()).contentType("application/json")
        .content("{\"rating\":5,\"comment\":\"Helpful lesson\",\"attended\":true}")).andExpect(status().isOk());
    mvc.perform(get("/api/trial-reviews").with(as(parent))).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].rating").value(5)).andExpect(jsonPath("$[0].comment").value("Helpful lesson"));
  }
  @Test void ownershipRoleAndAuthenticationAreRequired() throws Exception {
    assertTrue(reviews.list(other).isEmpty());
    assertEquals(404,assertThrows(BusinessException.class,()->reviews.submit(other,trial,new Submit(5,"",true))).status());
    mvc.perform(get("/api/trial-reviews").with(as(tutor))).andExpect(status().isForbidden());
    mvc.perform(get("/api/trial-reviews")).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/trial-reviews/"+trial).with(as(parent)).contentType("application/json")
        .content("{\"rating\":5,\"comment\":\"\",\"attended\":true}")).andExpect(status().isForbidden());
  }
  @Test void futureCancelledAndPendingTrialsCannotBeReviewed() {
    for(String state:List.of("PENDING","CANCELLED","EXPIRED")) {
      db.update("UPDATE trial_schedules SET status=? WHERE id=?",state,trial);
      assertTrue(reviews.list(parent).isEmpty());
      assertEquals(409,assertThrows(BusinessException.class,()->reviews.submit(parent,trial,new Submit(5,"",true))).status());
    }
    db.update("UPDATE trial_schedules SET status='CONFIRMED',chosen_at=TIMESTAMPADD(DAY,1,CURRENT_TIMESTAMP) WHERE id=?",trial);
    assertTrue(reviews.list(parent).isEmpty());
    assertEquals(409,assertThrows(BusinessException.class,()->reviews.submit(parent,trial,new Submit(5,"",true))).status());
  }
  @Test void invalidRatingAndMissingAttendanceAreRejected() throws Exception {
    for(String body:List.of("{\"rating\":0,\"comment\":\"\",\"attended\":true}",
        "{\"rating\":6,\"comment\":\"\",\"attended\":true}","{\"rating\":5,\"comment\":\"\",\"attended\":false}"))
      mvc.perform(post("/api/trial-reviews/"+trial).with(as(parent)).with(csrf()).contentType("application/json").content(body))
          .andExpect(status().isBadRequest());
    assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM trial_reviews WHERE trial_id=?",Integer.class,trial));
  }
  @Test void concurrentSubmissionsCreateExactlyOneReview() throws Exception {
    var start=new CountDownLatch(1);
    try(var pool=Executors.newFixedThreadPool(2)) {
      var tasks=new ArrayList<Future<Integer>>();
      for(int i=0;i<2;i++) tasks.add(pool.submit(()->{start.await();try {reviews.submit(parent,trial,new Submit(4,"Good",true));return 200;}catch(BusinessException e){return e.status();}}));
      start.countDown();
      var statuses=new ArrayList<Integer>();for(var task:tasks)statuses.add(task.get(15,TimeUnit.SECONDS));
      Collections.sort(statuses);assertEquals(List.of(200,409),statuses);
    }
    assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM trial_reviews WHERE trial_id=?",Integer.class,trial));
  }
}
