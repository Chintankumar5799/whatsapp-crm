package com.appointment.booking.repository;

import com.appointment.booking.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByClinicIdAndPatientUid(Long clinicId, String patientUid);
    List<Patient> findByClinicIdAndPhone(Long clinicId, String phone);

    @Query("SELECT p FROM Patient p WHERE p.clinicId = :clinicId AND p.isMerged = false AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "p.phone LIKE CONCAT('%', :query, '%') OR " +
           "LOWER(p.patientUid) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Patient> searchPatients(@Param("clinicId") Long clinicId, @Param("query") String query);

    @Query("SELECT COUNT(p) FROM Patient p WHERE p.clinicId = :clinicId")
    long countByClinicId(@Param("clinicId") Long clinicId);
}
