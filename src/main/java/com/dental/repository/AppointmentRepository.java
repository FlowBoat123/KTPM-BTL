package com.dental.repository;

import com.dental.entity.*;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

  Optional<Appointment> findByCode(String code);

  List<Appointment> findAllByOrderByPreferredDateAscPreferredTimeAsc();

  List<Appointment> findByDoctorIdOrderByAppointmentDateAscStartTimeAsc(Long doctorId);

  List<Appointment> findByDoctorIdAndAppointmentDateAndStatusIn(
      Long doctorId, LocalDate date, Collection<AppointmentStatus> statuses);

  boolean existsByDoctorIdAndStatusIn(Long doctorId, Collection<AppointmentStatus> statuses);

  @Query(
      "select count(a) from Appointment a where a.doctor.id = :doctorId and a.appointmentDate ="
          + " :date and a.status in :statuses and a.id <> :excludedId and a.startTime < :end and"
          + " a.endTime > :start")
  long countOverlaps(
      Long doctorId,
      LocalDate date,
      LocalTime start,
      LocalTime end,
      Collection<AppointmentStatus> statuses,
      Long excludedId);
}
