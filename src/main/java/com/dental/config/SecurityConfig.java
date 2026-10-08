package com.dental.config;

import com.dental.exception.ApiError;
import com.dental.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public FilterRegistrationBean<JwtAuthenticationFilter> jwtRegistration(
      JwtAuthenticationFilter filter) {
    var registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  public SecurityFilterChain security(
      HttpSecurity http, JwtAuthenticationFilter filter, ObjectMapper mapper) throws Exception {
    return http.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/error")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/appointments")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/clinic",
                        "/api/services",
                        "/api/doctors",
                        "/api/doctors/*",
                        "/api/doctors/*/available-slots",
                        "/api/appointments/*")
                    .permitAll()
                    .requestMatchers(HttpMethod.DELETE, "/api/appointments/*")
                    .permitAll()
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/doctor/**")
                    .hasRole("DOCTOR")
                    .requestMatchers("/api/auth/me", "/api/auth/logout")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, ex) -> {
                          res.setStatus(401);
                          res.setContentType("application/json");
                          mapper.writeValue(
                              res.getOutputStream(), ApiError.of(401, "Authentication required"));
                        })
                    .accessDeniedHandler(
                        (req, res, ex) -> {
                          res.setStatus(403);
                          res.setContentType("application/json");
                          mapper.writeValue(
                              res.getOutputStream(), ApiError.of(403, "Access denied"));
                        }))
        .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
