package com.tutormatch.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.tutormatch.dto.auth.AuthRequests.Registration;
import java.util.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.tutormatch.model.Account;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PhoneFormatIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;
  @Test void profileAcceptsLocalAndInternationalAndPreservesChosenFormat() throws Exception {
    var id=UUID.randomUUID().toString();
    var email=id+"@example.test";
    db.update("INSERT INTO users(id,email,full_name,role,email_verified) VALUES(?,?,'Phone demo','PARENT',TRUE)",id,email);
    var auth=new UsernamePasswordAuthenticationToken(new Account(id,email,"Phone demo","PARENT"),null,List.of(new SimpleGrantedAuthority("ROLE_PARENT")));
    for(var phone:List.of("0901234567","+84901234567","+14155552671")) {
      mvc.perform(put("/api/account").with(authentication(auth)).with(csrf()).contentType("application/json")
          .content("{\"name\":\"Phone demo\",\"phone\":\""+phone+"\"}"))
          .andExpect(status().isOk()).andExpect(jsonPath("$.phone").value(phone));
      assertEquals(phone,db.queryForObject("SELECT phone FROM users WHERE id=?",String.class,id));
    }
    for(var phone:List.of("901234567","090123","0123456789","not-a-phone"))
      mvc.perform(put("/api/account").with(authentication(auth)).with(csrf()).contentType("application/json")
          .content("{\"name\":\"Phone demo\",\"phone\":\""+phone+"\"}"))
          .andExpect(status().isBadRequest());
  }
  @Test void registrationAcceptsBothVietnameseFormats() {
    try(var factory=Validation.buildDefaultValidatorFactory()) {
      var validator=factory.getValidator();
      for(var phone:List.of("0901234567","+84901234567"))
        assertTrue(validator.validate(new Registration("phone@example.test","PasswordDemo!2026","Demo",phone,"PARENT")).isEmpty());
      assertFalse(validator.validate(new Registration("phone@example.test","PasswordDemo!2026","Demo","090123","PARENT")).isEmpty());
    }
  }
}
