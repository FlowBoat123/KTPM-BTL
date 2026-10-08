package com.dental.service;

import com.dental.dto.Responses;
import com.dental.entity.*;
import com.dental.exception.ApiException;
import com.dental.repository.AppointmentRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleService {
  public static final List<AppointmentStatus> OCCUPIED =
      List.of(AppointmentStatus.ASSIGNED, AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED);
  public static final List<AppointmentStatus> OPEN =
      List.of(AppointmentStatus.ASSIGNED, AppointmentStatus.CONFIRMED);
  private final AppointmentRepository appointments;
  private final CatalogService catalog;
  private final Clock clock;

  public ScheduleService(AppointmentRepository appointments, CatalogService catalog, Clock clock) {
    this.appointments = appointments;
    this.catalog = catalog;
    this.clock = clock;
  }

  public LocalTime validateSlot(LocalDate date, LocalTime start, int duration) {
    if (start.getSecond() != 0 || start.getNano() != 0 || start.getMinute() % 30 != 0)
      throw ApiException.invalid("Start time must use a 30-minute grid, without seconds");
    if (!LocalDateTime.of(date, start).isAfter(LocalDateTime.now(clock)))
      throw ApiException.invalid("Appointment time must be in the future");
    Clinic clinic = catalog.clinicEntity();
    int endMinutes = start.getHour() * 60 + start.getMinute() + duration;
    if (duration <= 0 || endMinutes >= 24 * 60)
      throw ApiException.invalid("Invalid service duration");
    LocalTime end = LocalTime.of(endMinutes / 60, endMinutes % 60);
    if (start.isBefore(clinic.getOpeningTime()) || end.isAfter(clinic.getClosingTime()))
      throw ApiException.invalid("Appointment must fit within clinic opening hours");
    return end;
  }

  public void ensureFree(
      Long doctor, LocalDate date, LocalTime start, LocalTime end, Long excluded) {
    if (appointments.countOverlaps(doctor, date, start, end, OCCUPIED, excluded) > 0)
      throw ApiException.conflict("Doctor already has an overlapping appointment");
  }

  @Transactional(readOnly = true)
  public Responses.AvailableSlots available(Long doctor, LocalDate date, Long serviceId) {
    catalog.activeDoctor(doctor);
    int duration = serviceId == null ? 30 : catalog.activeService(serviceId).getDurationMinutes();
    Clinic clinic = catalog.clinicEntity();
    var bookings = appointments.findByDoctorIdAndAppointmentDateAndStatusIn(doctor, date, OCCUPIED);
    List<LocalTime> slots = new ArrayList<>();
    for (LocalTime start = clinic.getOpeningTime();
        !start.plusMinutes(duration).isAfter(clinic.getClosingTime())
            && start.plusMinutes(duration).isAfter(start);
        start = start.plusMinutes(30)) {
      LocalTime candidate = start, end = start.plusMinutes(duration);
      boolean future = LocalDateTime.of(date, start).isAfter(LocalDateTime.now(clock));
      boolean occupied =
          bookings.stream()
              .anyMatch(a -> a.getStartTime().isBefore(end) && a.getEndTime().isAfter(candidate));
      if (future && !occupied) slots.add(start);
    }
    return new Responses.AvailableSlots(doctor, date, duration, slots);
  }
}
