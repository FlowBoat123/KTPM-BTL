package com.dental.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "clinics")
public class Clinic {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String address;

  @Column(nullable = false)
  private String phone;

  private String email;

  @Column(nullable = false)
  private LocalTime openingTime;

  @Column(nullable = false)
  private LocalTime closingTime;

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String value) {
    this.name = value;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String value) {
    this.address = value;
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

  public LocalTime getOpeningTime() {
    return openingTime;
  }

  public void setOpeningTime(LocalTime value) {
    this.openingTime = value;
  }

  public LocalTime getClosingTime() {
    return closingTime;
  }

  public void setClosingTime(LocalTime value) {
    this.closingTime = value;
  }
}
