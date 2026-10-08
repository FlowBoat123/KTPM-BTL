package com.dental.dto;

import com.dental.entity.*;

public final class ViewMapper {
  private ViewMapper() {}

  public static Responses.User user(Account a) {
    return new Responses.User(a.getId(), a.getUsername(), a.getRole());
  }

  public static Responses.DoctorView doctor(Doctor d) {
    return new Responses.DoctorView(
        d.getId(),
        d.getFullName(),
        d.getSpecialization(),
        d.getPhone(),
        d.getEmail(),
        d.getDescription(),
        d.getAccount().getActive());
  }

  public static Responses.ServiceView service(DentalService s) {
    return new Responses.ServiceView(
        s.getId(), s.getName(), s.getDescription(), s.getDurationMinutes(), s.getPrice());
  }

  public static Responses.ClinicView clinic(Clinic c) {
    return new Responses.ClinicView(
        c.getId(),
        c.getName(),
        c.getAddress(),
        c.getPhone(),
        c.getEmail(),
        c.getOpeningTime(),
        c.getClosingTime());
  }

  public static Responses.AppointmentView appointment(Appointment a) {
    Doctor d = a.getDoctor();
    return new Responses.AppointmentView(
        a.getId(),
        a.getCode(),
        a.getPatientName(),
        a.getPhone(),
        a.getEmail(),
        a.getService().getId(),
        a.getService().getName(),
        a.getPreferredDate(),
        a.getPreferredTime(),
        a.getNote(),
        d == null ? null : d.getId(),
        d == null ? null : d.getFullName(),
        a.getAppointmentDate(),
        a.getStartTime(),
        a.getEndTime(),
        a.getStatus(),
        a.getCreatedAt());
  }
}
