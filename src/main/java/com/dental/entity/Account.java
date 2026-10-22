package com.dental.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "accounts")
public class Account {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Column(nullable = false)
  private boolean active;

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String value) {
    this.username = value;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String value) {
    this.passwordHash = value;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role value) {
    this.role = value;
  }

  public boolean getActive() {
    return active;
  }

  public void setActive(boolean value) {
    this.active = value;
  }
}
