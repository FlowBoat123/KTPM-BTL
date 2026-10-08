package com.dental.controller;

import com.dental.dto.*;
import com.dental.service.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PublicController {
  private final CatalogService catalog;
  private final ScheduleService schedule;
  private final AppointmentService appointments;

  public PublicController(
      CatalogService catalog, ScheduleService schedule, AppointmentService appointments) {
    this.catalog = catalog;
    this.schedule = schedule;
    this.appointments = appointments;
  }

  @GetMapping("/clinic")
  public Responses.ClinicView clinic() {
    return catalog.clinic();
  }

  @GetMapping("/services")
  public List<Responses.ServiceView> services() {
    return catalog.services();
  }

  @GetMapping("/doctors")
  public List<Responses.DoctorView> doctors() {
    return catalog.doctors();
  }

  @GetMapping("/doctors/{id}")
  public Responses.DoctorView doctor(@PathVariable Long id) {
    return catalog.doctor(id);
  }

  @GetMapping("/doctors/{id}/available-slots")
  public Responses.AvailableSlots slots(
      @PathVariable Long id,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) Long serviceId) {
    return schedule.available(id, date, serviceId);
  }

  @PostMapping("/appointments")
  @ResponseStatus(HttpStatus.CREATED)
  public Responses.AppointmentView book(@Valid @RequestBody Requests.Booking r) {
    return appointments.book(r);
  }

  @GetMapping("/appointments/{code}")
  public Responses.AppointmentView lookup(@PathVariable String code) {
    return appointments.lookup(code);
  }

  @DeleteMapping("/appointments/{code}")
  public Responses.AppointmentView cancel(@PathVariable String code) {
    return appointments.cancel(code);
  }
}
