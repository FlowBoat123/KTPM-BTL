package com.dental.controller;

import com.dental.dto.*;
import com.dental.security.CurrentUser;
import com.dental.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService auth;

  public AuthController(AuthService auth) {
    this.auth = auth;
  }

  @PostMapping("/login")
  public Responses.Login login(@Valid @RequestBody Requests.Login r) {
    return auth.login(r);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@AuthenticationPrincipal CurrentUser user) {
    auth.logout(user);
  }

  @GetMapping("/me")
  public Responses.User me(@AuthenticationPrincipal CurrentUser user) {
    return auth.me(user);
  }
}
