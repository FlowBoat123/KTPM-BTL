package com.dental.controller;

import com.dental.dto.*;
import com.dental.security.CurrentUser;
import com.dental.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Xác thực tài khoản và quản lý phiên đăng nhập (JWT)")
public class AuthController {
  private final AuthService auth;

  public AuthController(AuthService auth) {
    this.auth = auth;
  }

  @PostMapping("/login")
  @Operation(summary = "Đăng nhập", description = "Xác thực bằng username và password, trả về Bearer JWT token")
  public Responses.Login login(@Valid @RequestBody Requests.Login r) {
    return auth.login(r);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Đăng xuất", description = "Thu hồi token JWT hiện tại của phiên đăng nhập")
  @SecurityRequirement(name = "BearerAuth")
  public void logout(@AuthenticationPrincipal CurrentUser user) {
    auth.logout(user);
  }

  @GetMapping("/me")
  @Operation(summary = "Thông tin tài khoản hiện tại", description = "Lấy thông tin tài khoản và vai trò của token đang sử dụng")
  @SecurityRequirement(name = "BearerAuth")
  public Responses.User me(@AuthenticationPrincipal CurrentUser user) {
    return auth.me(user);
  }
}
