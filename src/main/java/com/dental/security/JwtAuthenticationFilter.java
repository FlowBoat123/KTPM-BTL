package com.dental.security;

import com.dental.exception.ApiError;
import com.dental.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final AuthService auth;
  private final ObjectMapper mapper;

  public JwtAuthenticationFilter(AuthService auth, ObjectMapper mapper) {
    this.auth = auth;
    this.mapper = mapper;
  }

  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String header = req.getHeader("Authorization");
    if (header != null) {
      try {
        if (!header.startsWith("Bearer ")) throw new JwtException("Invalid authorization header");
        CurrentUser user = auth.authenticate(header.substring(7));
        var authentication =
            new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      } catch (JwtException | IllegalArgumentException e) {
        SecurityContextHolder.clearContext();
        res.setStatus(401);
        res.setContentType("application/json");
        mapper.writeValue(
            res.getOutputStream(), ApiError.of(401, "Invalid, expired or revoked token"));
        return;
      }
    }
    chain.doFilter(req, res);
  }
}
