package com.tutormatch.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {
  public record Account(String id, String email, String name, String role) {}

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
  SecurityFilterChain security(HttpSecurity http, JdbcTemplate jdbc) throws Exception {
    var csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
    http.csrf(
            c ->
                c.csrfTokenRepository(csrf)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
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
            new OncePerRequestFilter() {
              protected void doFilterInternal(
                  HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                  throws ServletException, IOException {
                if (req.getCookies() != null)
                  for (var cookie : req.getCookies())
                    if (cookie.getName().equals("TM_SESSION")) {
                      var users =
                          jdbc.query(
                              "SELECT u.id,u.email,u.full_name,u.role FROM auth_sessions s JOIN"
                                  + " users u ON u.id=s.user_id WHERE s.token_hash=? AND"
                                  + " s.expires_at>CURRENT_TIMESTAMP AND u.email_verified=TRUE AND"
                                  + " u.phone_verified=TRUE",
                              (rs, n) ->
                                  new Account(
                                      rs.getString(1),
                                      rs.getString(2),
                                      rs.getString(3),
                                      rs.getString(4)),
                              AuthService.hash(cookie.getValue()));
                      if (!users.isEmpty()) {
                        var u = users.getFirst();
                        SecurityContextHolder.getContext()
                            .setAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                    u,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + u.role()))));
                      }
                    }
                res.setHeader("Cache-Control", "no-store");
                chain.doFilter(req, res);
              }
            },
            UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  static void error(HttpServletResponse r, int status, String message) throws IOException {
    r.setStatus(status);
    r.setContentType("application/json;charset=UTF-8");
    r.getWriter().write("{\"message\":\"" + message + "\"}");
  }
}
