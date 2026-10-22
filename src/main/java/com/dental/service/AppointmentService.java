package com.dental.service;

import com.dental.dto.*;
import com.dental.entity.*;
import com.dental.exception.ApiException;
import com.dental.repository.AppointmentRepository;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AppointmentService {
  private final AppointmentRepository appointments;
  private final CatalogService catalog;
  private final DoctorService doctors;
  private final ScheduleService schedule;
  private final Clock clock;

  public AppointmentService(
      AppointmentRepository appointments,
      CatalogService catalog,
      DoctorService doctors,
      ScheduleService schedule,
      Clock clock) {
    this.appointments = appointments;
    this.catalog = catalog;
    this.doctors = doctors;
    this.schedule = schedule;
    this.clock = clock;
  }

  private Appointment byId(Long id) {
    return appointments
        .findById(id)
        .orElseThrow(() -> ApiException.notFound("Appointment not found"));
  }

  private Appointment byCode(String code) {
    return appointments
        .findByCode(code)
        .orElseThrow(() -> ApiException.notFound("Appointment not found"));
  }

  @Transactional
  public Responses.AppointmentView book(Requests.Booking r) {
    DentalService service = catalog.activeService(r.serviceId());
    schedule.validateSlot(r.preferredDate(), r.preferredTime(), service.getDurationMinutes());
    Appointment a = new Appointment();
    a.setCode(UUID.randomUUID().toString());
    a.setPatientName(r.patientName().trim());
    a.setPhone(r.phone());
    a.setEmail(r.email());
    a.setService(service);
    a.setPreferredDate(r.preferredDate());
    a.setPreferredTime(r.preferredTime());
    a.setNote(r.note());
    a.setStatus(AppointmentStatus.PENDING);
    a.setCreatedAt(clock.instant());
    return ViewMapper.appointment(appointments.saveAndFlush(a));
  }

  public Responses.AppointmentView lookup(String code) {
    return ViewMapper.appointment(byCode(code));
  }

  public Responses.AppointmentView get(Long id) {
    return ViewMapper.appointment(byId(id));
  }

  public List<Responses.AppointmentView> all() {
    return appointments.findAllByOrderByPreferredDateAscPreferredTimeAsc().stream()
        .map(ViewMapper::appointment)
        .toList();
  }

  public List<Responses.AppointmentView> forDoctor(Long id) {
    doctors.byId(id);
    return appointments.findByDoctorIdOrderByAppointmentDateAscStartTimeAsc(id).stream()
        .map(ViewMapper::appointment)
        .toList();
  }

  public List<Responses.AppointmentView> mine(Long account) {
    return forDoctor(doctors.byAccount(account).getId());
  }

  private Appointment owned(Long account, Long id) {
    Appointment a = byId(id);
    if (a.getDoctor() == null || !a.getDoctor().getAccount().getId().equals(account))
      throw new ApiException(HttpStatus.FORBIDDEN, "Appointment is not assigned to this doctor");
    return a;
  }

  public Responses.AppointmentView mineDetail(Long account, Long id) {
    return ViewMapper.appointment(owned(account, id));
  }

  @Transactional
  public Responses.AppointmentView assign(Long id, Requests.Assignment r) {
    Appointment a = byId(id);
    if (a.getStatus() == AppointmentStatus.COMPLETED
        || a.getStatus() == AppointmentStatus.CANCELLED)
      throw ApiException.conflict("Terminal appointments cannot be reassigned");
    Doctor doctor = catalog.activeDoctor(r.doctorId());
    LocalTime end =
        schedule.validateSlot(
            r.appointmentDate(), r.startTime(), a.getService().getDurationMinutes());
    schedule.ensureFree(doctor.getId(), r.appointmentDate(), r.startTime(), end, id);
    a.setDoctor(doctor);
    a.setAppointmentDate(r.appointmentDate());
    a.setStartTime(r.startTime());
    a.setEndTime(end);
    a.setStatus(AppointmentStatus.ASSIGNED);
    return ViewMapper.appointment(appointments.saveAndFlush(a));
  }

  @Transactional
  public Responses.AppointmentView change(Long id, AppointmentStatus status) {
    return transition(byId(id), status);
  }

  @Transactional
  public Responses.AppointmentView changeMine(Long account, Long id, AppointmentStatus status) {
    return transition(owned(account, id), status);
  }

  @Transactional
  public Responses.AppointmentView cancel(String code) {
    Appointment a = byCode(code);
    LocalDate date = a.getAppointmentDate() == null ? a.getPreferredDate() : a.getAppointmentDate();
    LocalTime time = a.getStartTime() == null ? a.getPreferredTime() : a.getStartTime();
    if (!LocalDateTime.of(date, time).isAfter(LocalDateTime.now(clock)))
      throw ApiException.conflict("Cannot cancel an appointment after its start time");
    return transition(a, AppointmentStatus.CANCELLED);
  }

  private Responses.AppointmentView transition(Appointment a, AppointmentStatus next) {
    Set<AppointmentStatus> allowed =
        switch (a.getStatus()) {
          case PENDING -> Set.of(AppointmentStatus.CANCELLED);
          case ASSIGNED ->
              Set.of(
                  AppointmentStatus.CONFIRMED,
                  AppointmentStatus.COMPLETED,
                  AppointmentStatus.CANCELLED);
          case CONFIRMED -> Set.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED);
          default -> Set.of();
        };
    if (!allowed.contains(next))
      throw ApiException.conflict("Invalid status transition: " + a.getStatus() + " -> " + next);
    a.setStatus(next);
    return ViewMapper.appointment(appointments.saveAndFlush(a));
  }
}
