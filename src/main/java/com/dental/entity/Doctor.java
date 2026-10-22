package com.dental.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "doctors")
public class Doctor {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false, unique = true)
  private Account account;

  @Column(nullable = false)
  private String fullName;

  @Column(nullable = false)
  private String specialization;

  private String phone;

  private String email;

  private String description;

  public Long getId() {
    return id;
  }

  public Account getAccount() {
    return account;
  }

  public void setAccount(Account value) {
    this.account = value;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String value) {
    this.fullName = value;
  }

  public String getSpecialization() {
    return specialization;
  }

  public void setSpecialization(String value) {
    this.specialization = value;
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

  public String getDescription() {
    return description;
  }

  public void setDescription(String value) {
    this.description = value;
  }
}
