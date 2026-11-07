package com.tutormatch.security;

import com.tutormatch.service.AuthService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Registered only inside Spring Security, never as a second servlet filter. */
public class SessionAuthenticationFilter extends OncePerRequestFilter {
  private final AuthService auth;

  public SessionAuthenticationFilter(AuthService auth) {
    this.auth = auth;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest q, HttpServletResponse r, FilterChain chain)
      throws ServletException, IOException {
    if (q.getCookies() != null)
      for (var cookie : q.getCookies())
        if (cookie.getName().equals("TM_SESSION")) {
          auth.authenticate(cookie.getValue())
              .ifPresent(
                  u ->
                      SecurityContextHolder.getContext()
                          .setAuthentication(
                              new UsernamePasswordAuthenticationToken(
                                  u,
                                  null,
                                  List.of(new SimpleGrantedAuthority("ROLE_" + u.role())))));
          break;
        }
    r.setHeader("Cache-Control", "no-store");
    chain.doFilter(q, r);
  }
}
