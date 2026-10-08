package com.dental.dto;

import com.dental.entity.AppointmentStatus;
import jakarta.validation.constraints.*;
import java.time.*;

public final class Requests {
  private Requests() {}

  public record Login(
      @NotBlank @Size(max = 80) String username, @NotBlank @Size(max = 72) String password) {}

  public record Booking(
      @NotBlank @Size(max = 120) String patientName,
      @NotBlank @Size(max = 20) @Pattern(regexp = "[+]?[0-9][0-9 ()-]{6,18}[0-9]") String phone,
      @Email @Size(max = 160) String email,
      @NotNull @Positive Long serviceId,
      @NotNull LocalDate preferredDate,
      @NotNull LocalTime preferredTime,
      @Size(max = 1000) String note) {}

  public record Assignment(
      @NotNull @Positive Long doctorId,
      @NotNull LocalDate appointmentDate,
      @NotNull LocalTime startTime) {}

  public record StatusChange(@NotNull AppointmentStatus status) {}

  public record DoctorCreate(
      @NotBlank @Pattern(regexp = "[a-zA-Z0-9_.-]{3,80}") String username,
      @NotBlank @Size(min = 8, max = 72) String password,
      @NotBlank @Size(max = 120) String fullName,
      @NotBlank @Size(max = 120) String specialization,
      @Size(max = 20) @Pattern(regexp = "[+]?[0-9][0-9 ()-]{6,18}[0-9]") String phone,
      @Email @Size(max = 160) String email,
      @Size(max = 1000) String description) {}

  public record DoctorUpdate(
      @NotBlank @Size(max = 120) String fullName,
      @NotBlank @Size(max = 120) String specialization,
      @Size(max = 20) @Pattern(regexp = "[+]?[0-9][0-9 ()-]{6,18}[0-9]") String phone,
      @Email @Size(max = 160) String email,
      @Size(max = 1000) String description) {}
}
