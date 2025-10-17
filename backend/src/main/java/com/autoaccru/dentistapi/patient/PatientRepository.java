package com.autoaccru.dentistapi.patient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    // Return only the latest record for each phone number (used for the main table view)
    @Query(value = """
    SELECT * FROM (
      SELECT p.*,
             ROW_NUMBER() OVER (
               PARTITION BY REGEXP_REPLACE(p.phone_number, '[^0-9]', '')
               ORDER BY p.id DESC
             ) rn
      FROM patients p
    ) x
    WHERE x.rn = 1
    ORDER BY x.id DESC
    """, nativeQuery = true)
    List<Patient> findLatestUnique();

    // Check if a normalized phone number already exists in the DB
    @Query(value = """
    SELECT EXISTS(
      SELECT 1 FROM patients
      WHERE REGEXP_REPLACE(phone_number, '[^0-9]', '') = :norm
    )
    """, nativeQuery = true)
    boolean existsByNormPhone(@Param("norm") String norm);

    // Count how many times this patient has visited (based on phone number)
    @Query(value = """
    SELECT COUNT(*) FROM patients
    WHERE REGEXP_REPLACE(phone_number, '[^0-9]', '') = :norm
    """, nativeQuery = true)
    int countVisits(@Param("norm") String norm);
}
