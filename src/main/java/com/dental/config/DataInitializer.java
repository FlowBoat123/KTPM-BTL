package com.dental.config;

import com.dental.dto.Requests;
import com.dental.entity.*;
import com.dental.repository.*;
import com.dental.service.DoctorService;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements ApplicationRunner {
  private final ClinicRepository clinics;
  private final DentalServiceRepository services;
  private final AccountRepository accounts;
  private final DoctorService doctors;
  private final PasswordEncoder passwords;
  private final Environment env;
  private final String adminUsername, adminPassword;

  public DataInitializer(
      ClinicRepository clinics,
      DentalServiceRepository services,
      AccountRepository accounts,
      DoctorService doctors,
      PasswordEncoder passwords,
      Environment env,
      @Value("${app.bootstrap.username:}") String username,
      @Value("${app.bootstrap.password:}") String password) {
    this.clinics = clinics;
    this.services = services;
    this.accounts = accounts;
    this.doctors = doctors;
    this.passwords = passwords;
    this.env = env;
    this.adminUsername = username;
    this.adminPassword = password;
  }

  @Transactional
  public void run(ApplicationArguments args) {
    if (clinics.count() == 0) {
      Clinic c = new Clinic();
      c.setName("Phong kham nha khoa Demo");
      c.setAddress("Dia chi mau - cap nhat theo phong kham");
      c.setPhone("0900000000");
      c.setEmail("clinic@example.com");
      c.setOpeningTime(LocalTime.of(8, 0));
      c.setClosingTime(LocalTime.of(17, 0));
      clinics.save(c);
    }
    if (services.count() == 0) {
      service("Kham tong quat", 30, "100000");
      service("Lay cao rang", 60, "300000");
      service("Nho rang", 60, "500000");
    }
    boolean demo = env.acceptsProfiles(Profiles.of("demo"));
    String username = demo ? "admin" : adminUsername, password = demo ? "Admin123!" : adminPassword;
    if (!username.isBlank() && accounts.findByUsername(username).isEmpty()) {
      if (password.length() < 8
          || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
        throw new IllegalArgumentException(
            "Admin password must contain at least 8 characters and at most 72 UTF-8 bytes");
      Account a = new Account();
      a.setUsername(username);
      a.setPasswordHash(passwords.encode(password));
      a.setRole(Role.ADMIN);
      a.setActive(true);
      accounts.save(a);
    }
    if (demo && accounts.findByUsername("doctor1").isEmpty())
      doctors.create(
          new Requests.DoctorCreate(
              "doctor1",
              "Doctor123!",
              "Bac si Nguyen An",
              "Nha khoa tong quat",
              "0900000001",
              "doctor1@example.com",
              "Du lieu mau"));
    if (demo && accounts.findByUsername("doctor2").isEmpty())
      doctors.create(
          new Requests.DoctorCreate(
              "doctor2",
              "Doctor123!",
              "Bac si Tran Binh",
              "Phau thuat rang",
              "0900000002",
              "doctor2@example.com",
              "Du lieu mau"));
  }

  private void service(String name, int duration, String price) {
    DentalService s = new DentalService();
    s.setName(name);
    s.setDescription("Dich vu mau");
    s.setDurationMinutes(duration);
    s.setPrice(new BigDecimal(price));
    s.setActive(true);
    services.save(s);
  }
}
