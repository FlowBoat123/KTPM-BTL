package com.dental.controller;

import com.dental.dto.*;
import com.dental.service.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final DoctorService doctors;
  private final AppointmentService appointments;

  public AdminController(DoctorService doctors, AppointmentService appointments) {
    this.doctors = doctors;
    this.appointments = appointments;
  }

  @GetMapping("/appointments")
  public List<Responses.AppointmentView> appointments() {
    return appointments.all();
  }

  @GetMapping("/appointments/{id}")
  public Responses.AppointmentView appointment(@PathVariable Long id) {
    return appointments.get(id);
  }

  @PatchMapping("/appointments/{id}/assign")
  public Responses.AppointmentView assign(
      @PathVariable Long id, @Valid @RequestBody Requests.Assignment r) {
    return appointments.assign(id, r);
  }

  @PatchMapping("/appointments/{id}/status")
  public Responses.AppointmentView status(
      @PathVariable Long id, @Valid @RequestBody Requests.StatusChange r) {
    return appointments.change(id, r.status());
  }

  @GetMapping("/doctors")
  public List<Responses.DoctorView> doctors() {
    return doctors.all();
  }

  @PostMapping("/doctors")
  @ResponseStatus(HttpStatus.CREATED)
  public Responses.DoctorView create(@Valid @RequestBody Requests.DoctorCreate r) {
    return doctors.create(r);
  }

  @GetMapping("/doctors/{id}")
  public Responses.DoctorView doctor(@PathVariable Long id) {
    return doctors.get(id);
  }

  @PutMapping("/doctors/{id}")
  public Responses.DoctorView update(
      @PathVariable Long id, @Valid @RequestBody Requests.DoctorUpdate r) {
    return doctors.update(id, r);
  }

  @DeleteMapping("/doctors/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void disable(@PathVariable Long id) {
    doctors.disable(id);
  }

  @GetMapping("/doctors/{id}/appointments")
  public List<Responses.AppointmentView> doctorAppointments(@PathVariable Long id) {
    return appointments.forDoctor(id);
  }
}
