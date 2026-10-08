package com.dental.dto;

import com.dental.entity.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class Responses {
  private Responses() {}

  public record User(Long id, String username, Role role) {}

  public record Login(String accessToken, String tokenType, Instant expiresAt, User user) {}

  public record DoctorView(
      Long id,
      String fullName,
      String specialization,
      String phone,
      String email,
      String description,
      boolean active) {}

  public record ClinicView(
      Long id,
      String name,
      String address,
      String phone,
      String email,
      LocalTime openingTime,
      LocalTime closingTime) {}

  public record ServiceView(
      Long id, String name, String description, int durationMinutes, BigDecimal price) {}

  public record AppointmentView(
      Long id,
      String code,
      String patientName,
      String phone,
      String email,
      Long serviceId,
      String serviceName,
      LocalDate preferredDate,
      LocalTime preferredTime,
      String note,
      Long doctorId,
      String doctorName,
      LocalDate appointmentDate,
      LocalTime startTime,
      LocalTime endTime,
      AppointmentStatus status,
      Instant createdAt) {}

  public record AvailableSlots(
      Long doctorId, LocalDate date, int durationMinutes, List<LocalTime> slots) {}
}
