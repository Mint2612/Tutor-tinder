package com.tutormatch.config;

import com.tutormatch.security.SessionAuthenticationFilter;
import com.tutormatch.service.AuthService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {

  @Bean
  org.springframework.security.core.userdetails.UserDetailsService noDefaultUsers() {
    return username -> {
      throw new org.springframework.security.core.userdetails.UsernameNotFoundException(
          "Use TutorMatch session authentication");
    };
  }

  @Bean
  PasswordEncoder passwords() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http, AuthService auth) throws Exception {
    var csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
    http.csrf(
            c ->
                c.csrfTokenRepository(csrf)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(HttpMethod.POST, "/api/auth/logout")
                    .permitAll()
                    .requestMatchers(
                        "/api/health",
                        "/actuator/health",
                        "/api/auth/csrf",
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/verify",
                        "/api/auth/resend",
                        "/api/auth/forgot",
                        "/api/auth/reset")
                    .permitAll()
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/tutor/**")
                    .hasRole("TUTOR")
                    .requestMatchers("/api/students/**", "/api/swipes")
                    .hasRole("PARENT")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint((q, r, x) -> error(r, 401, "Vui lòng đăng nhập"))
                    .accessDeniedHandler(
                        (q, r, x) ->
                            error(r, 403, "Không có quyền hoặc phiên biểu mẫu đã hết hạn")))
        .addFilterBefore(
            new SessionAuthenticationFilter(auth), UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  static void error(HttpServletResponse r, int status, String message) throws IOException {
    r.setStatus(status);
    r.setContentType("application/json;charset=UTF-8");
    r.getWriter().write("{\"message\":\"" + message + "\"}");
  }
}
