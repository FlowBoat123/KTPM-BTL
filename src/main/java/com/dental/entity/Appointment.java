package com.dental.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "appointments")
public class Appointment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  private String patientName;

  @Column(nullable = false)
  private String phone;

  private String email;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "service_id", nullable = false)
  private DentalService service;

  @Column(nullable = false)
  private LocalDate preferredDate;

  @Column(nullable = false)
  private LocalTime preferredTime;

  private String note;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "doctor_id")
  private Doctor doctor;

  private LocalDate appointmentDate;

  private LocalTime startTime;

  private LocalTime endTime;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AppointmentStatus status;

  @Column(nullable = false)
  private Instant createdAt;

  @Version private Long version;

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String value) {
    this.code = value;
  }

  public String getPatientName() {
    return patientName;
  }

  public void setPatientName(String value) {
    this.patientName = value;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String value) {
    this.phone = value;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String value) {
    this.email = value;
  }

  public DentalService getService() {
    return service;
  }

  public void setService(DentalService value) {
    this.service = value;
  }

  public LocalDate getPreferredDate() {
    return preferredDate;
  }

  public void setPreferredDate(LocalDate value) {
    this.preferredDate = value;
  }

  public LocalTime getPreferredTime() {
    return preferredTime;
  }

  public void setPreferredTime(LocalTime value) {
    this.preferredTime = value;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String value) {
    this.note = value;
  }

  public Doctor getDoctor() {
    return doctor;
  }

  public void setDoctor(Doctor value) {
    this.doctor = value;
  }

  public LocalDate getAppointmentDate() {
    return appointmentDate;
  }

  public void setAppointmentDate(LocalDate value) {
    this.appointmentDate = value;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalTime value) {
    this.startTime = value;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalTime value) {
    this.endTime = value;
  }

  public AppointmentStatus getStatus() {
    return status;
  }

  public void setStatus(AppointmentStatus value) {
    this.status = value;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant value) {
    this.createdAt = value;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long value) {
    this.version = value;
  }
}
