package com.dental.service;

import com.dental.dto.*;
import com.dental.entity.*;
import com.dental.exception.ApiException;
import com.dental.repository.*;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DoctorService {
  private final DoctorRepository doctors;
  private final AccountRepository accounts;
  private final AppointmentRepository appointments;
  private final PasswordEncoder passwords;

  public DoctorService(
      DoctorRepository doctors,
      AccountRepository accounts,
      AppointmentRepository appointments,
      PasswordEncoder passwords) {
    this.doctors = doctors;
    this.accounts = accounts;
    this.appointments = appointments;
    this.passwords = passwords;
  }

  public Doctor byAccount(Long id) {
    return doctors
        .findByAccountId(id)
        .orElseThrow(() -> ApiException.notFound("Doctor profile not found"));
  }

  public Doctor byId(Long id) {
    return doctors.findById(id).orElseThrow(() -> ApiException.notFound("Doctor not found"));
  }

  public List<Responses.DoctorView> all() {
    return doctors.findAll().stream().map(ViewMapper::doctor).toList();
  }

  public Responses.DoctorView get(Long id) {
    return ViewMapper.doctor(byId(id));
  }

  public Responses.DoctorView profile(Long account) {
    return ViewMapper.doctor(byAccount(account));
  }

  @Transactional
  public Responses.DoctorView create(Requests.DoctorCreate r) {
    if (accounts.findByUsername(r.username()).isPresent())
      throw ApiException.conflict("Username already exists");
    if (r.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw ApiException.invalid("Password must not exceed 72 UTF-8 bytes");
    Account account = new Account();
    account.setUsername(r.username());
    account.setPasswordHash(passwords.encode(r.password()));
    account.setRole(Role.DOCTOR);
    account.setActive(true);
    Doctor d = new Doctor();
    d.setAccount(accounts.save(account));
    apply(
        d,
        new Requests.DoctorUpdate(
            r.fullName(), r.specialization(), r.phone(), r.email(), r.description()));
    return ViewMapper.doctor(doctors.saveAndFlush(d));
  }

  @Transactional
  public Responses.DoctorView update(Long id, Requests.DoctorUpdate r) {
    Doctor d = byId(id);
    apply(d, r);
    return ViewMapper.doctor(doctors.save(d));
  }

  @Transactional
  public Responses.DoctorView updateProfile(Long account, Requests.DoctorUpdate r) {
    Doctor d = byAccount(account);
    apply(d, r);
    return ViewMapper.doctor(doctors.save(d));
  }

  @Transactional
  public void disable(Long id) {
    Doctor d = byId(id);
    if (appointments.existsByDoctorIdAndStatusIn(id, ScheduleService.OPEN))
      throw ApiException.conflict("Reassign or cancel open appointments before disabling doctor");
    d.getAccount().setActive(false);
    accounts.save(d.getAccount());
  }

  private void apply(Doctor d, Requests.DoctorUpdate r) {
    d.setFullName(r.fullName().trim());
    d.setSpecialization(r.specialization().trim());
    d.setPhone(r.phone());
    d.setEmail(r.email());
    d.setDescription(r.description());
  }
}
