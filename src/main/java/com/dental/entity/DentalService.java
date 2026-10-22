package com.dental.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "dental_services")
public class DentalService {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  private String description;

  @Column(nullable = false)
  private int durationMinutes;

  @Column(nullable = false)
  private BigDecimal price;

  @Column(nullable = false)
  private boolean active;

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String value) {
    this.name = value;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String value) {
    this.description = value;
  }

  public int getDurationMinutes() {
    return durationMinutes;
  }

  public void setDurationMinutes(int value) {
    this.durationMinutes = value;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal value) {
    this.price = value;
  }

  public boolean getActive() {
    return active;
  }

  public void setActive(boolean value) {
    this.active = value;
  }
}
