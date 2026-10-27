package com.dental.repository;

import com.dental.entity.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
  Optional<Doctor> findByAccountId(Long accountId);

  List<Doctor> findByAccountActiveTrueOrderByIdAsc();

  // SQLite relies on its write serialization and overlap triggers; its dialect has no FOR UPDATE.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from Doctor d where d.id = :id")
  Optional<Doctor> findByIdWithLock(@Param("id") Long id);
}
