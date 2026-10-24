package com.dental.controller;

import com.dental.dto.Requests;
import com.dental.dto.Responses;
import com.dental.security.CurrentUser;
import com.dental.service.AppointmentService;
import com.dental.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor")
@Tag(name = "Doctor", description = "Các API dành cho Bác sĩ (xem lịch, đổi trạng thái, cập nhật hồ sơ cá nhân)")
@SecurityRequirement(name = "BearerAuth")
public class DoctorController {
  private final DoctorService doctors;
  private final AppointmentService appointments;

  public DoctorController(DoctorService doctors, AppointmentService appointments) {
    this.doctors = doctors;
    this.appointments = appointments;
  }

  @GetMapping("/appointments")
  @Operation(summary = "Xem lịch hẹn của bác sĩ đang đăng nhập", description = "Lấy danh sách các lịch hẹn được phân công cho bác sĩ này")
  public List<Responses.AppointmentView> appointments(@AuthenticationPrincipal CurrentUser user) {
    return appointments.mine(user.accountId());
  }

  @GetMapping("/appointments/{id}")
  @Operation(summary = "Xem chi tiết 1 lịch hẹn của bác sĩ", description = "Xem chi tiết ca khám nếu thuộc quyền phụ trách của bác sĩ này")
  public Responses.AppointmentView appointment(
      @AuthenticationPrincipal CurrentUser user, @PathVariable Long id) {
    return appointments.mineDetail(user.accountId(), id);
  }

  @PatchMapping("/appointments/{id}/status")
  @Operation(summary = "Bác sĩ đổi trạng thái lịch hẹn", description = "Bác sĩ cập nhật trạng thái (CONFIRMED, COMPLETED hoặc CANCELLED)")
  public Responses.AppointmentView status(
      @AuthenticationPrincipal CurrentUser user,
      @PathVariable Long id,
      @Valid @RequestBody Requests.StatusChange r) {
    return appointments.changeMine(user.accountId(), id, r.status());
  }

  @GetMapping("/profile")
  @Operation(summary = "Xem hồ sơ bác sĩ", description = "Lấy thông tin hồ sơ của bác sĩ đang đăng nhập")
  public Responses.DoctorView profile(@AuthenticationPrincipal CurrentUser user) {
    return doctors.profile(user.accountId());
  }

  @PutMapping("/profile")
  @Operation(summary = "Cập nhật hồ sơ bác sĩ", description = "Cập nhật thông tin cá nhân của bác sĩ đang đăng nhập")
  public Responses.DoctorView update(
      @AuthenticationPrincipal CurrentUser user, @Valid @RequestBody Requests.DoctorUpdate r) {
    return doctors.updateProfile(user.accountId(), r);
  }
}
