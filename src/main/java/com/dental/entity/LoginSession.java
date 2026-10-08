package com.dental.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "login_sessions")
public class LoginSession {
  @Id private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(nullable = false)
  private Instant expiresAt;

  public LoginSession() {}

  public LoginSession(String id, Account account, Instant expiresAt) {
    this.id = id;
    this.account = account;
    this.expiresAt = expiresAt;
  }

  public String getId() {
    return id;
  }

  public Account getAccount() {
    return account;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }
}
