package com.dental.repository;

import com.dental.entity.*;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DentalServiceRepository extends JpaRepository<DentalService, Long> {
  List<DentalService> findByActiveTrueOrderByIdAsc();
}
