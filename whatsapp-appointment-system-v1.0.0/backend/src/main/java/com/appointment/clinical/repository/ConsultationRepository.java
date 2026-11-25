package com.appointment.clinical.repository;

import com.appointment.clinical.model.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    List<Consultation> findByPatientIdOrderByFinalizedAtDesc(Long patientId);
    List<Consultation> findByDoctorIdOrderByFinalizedAtDesc(Long doctorId);
    List<Consultation> findByClinicIdOrderByCreatedAtDesc(Long clinicId);
    Optional<Consultation> findByBookingId(Long bookingId);

    @Query("SELECT c FROM Consultation c WHERE c.patientId = :patientId AND c.isFinalized = true ORDER BY c.finalizedAt DESC LIMIT 1")
    Optional<Consultation> findLatestCompletedByPatientId(@Param("patientId") Long patientId);
}

