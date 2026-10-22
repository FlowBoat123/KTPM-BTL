package com.dental.service;

import com.dental.dto.*;
import com.dental.entity.*;
import com.dental.exception.ApiException;
import com.dental.repository.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogService {
  private final ClinicRepository clinics;
  private final DentalServiceRepository services;
  private final DoctorRepository doctors;

  public CatalogService(
      ClinicRepository clinics, DentalServiceRepository services, DoctorRepository doctors) {
    this.clinics = clinics;
    this.services = services;
    this.doctors = doctors;
  }

  public Clinic clinicEntity() {
    return clinics
        .findFirstByOrderByIdAsc()
        .orElseThrow(() -> ApiException.notFound("Clinic not configured"));
  }

  public Responses.ClinicView clinic() {
    return ViewMapper.clinic(clinicEntity());
  }

  public List<Responses.ServiceView> services() {
    return services.findByActiveTrueOrderByIdAsc().stream().map(ViewMapper::service).toList();
  }

  public List<Responses.DoctorView> doctors() {
    return doctors.findByAccountActiveTrueOrderByIdAsc().stream().map(ViewMapper::doctor).toList();
  }

  public Doctor activeDoctor(Long id) {
    Doctor d = doctors.findById(id).orElseThrow(() -> ApiException.notFound("Doctor not found"));
    if (!d.getAccount().getActive()) throw ApiException.notFound("Doctor not found");
    return d;
  }

  public Responses.DoctorView doctor(Long id) {
    return ViewMapper.doctor(activeDoctor(id));
  }

  public DentalService activeService(Long id) {
    return services
        .findById(id)
        .filter(DentalService::getActive)
        .orElseThrow(() -> ApiException.notFound("Service not found"));
  }
}
