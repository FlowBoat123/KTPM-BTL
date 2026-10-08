package com.dental.controller;

import com.dental.dto.*;
import com.dental.security.CurrentUser;
import com.dental.service.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctor")
public class DoctorController {
  private final DoctorService doctors;
  private final AppointmentService appointments;

  public DoctorController(DoctorService doctors, AppointmentService appointments) {
    this.doctors = doctors;
    this.appointments = appointments;
  }

  @GetMapping("/appointments")
  public List<Responses.AppointmentView> appointments(@AuthenticationPrincipal CurrentUser user) {
    return appointments.mine(user.accountId());
  }

  @GetMapping("/appointments/{id}")
  public Responses.AppointmentView appointment(
      @AuthenticationPrincipal CurrentUser user, @PathVariable Long id) {
    return appointments.mineDetail(user.accountId(), id);
  }

  @PatchMapping("/appointments/{id}/status")
  public Responses.AppointmentView status(
      @AuthenticationPrincipal CurrentUser user,
      @PathVariable Long id,
      @Valid @RequestBody Requests.StatusChange r) {
    return appointments.changeMine(user.accountId(), id, r.status());
  }

  @GetMapping("/profile")
  public Responses.DoctorView profile(@AuthenticationPrincipal CurrentUser user) {
    return doctors.profile(user.accountId());
  }

  @PutMapping("/profile")
  public Responses.DoctorView update(
      @AuthenticationPrincipal CurrentUser user, @Valid @RequestBody Requests.DoctorUpdate r) {
    return doctors.updateProfile(user.accountId(), r);
  }
}
