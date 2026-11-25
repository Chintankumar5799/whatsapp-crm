package com.appointment.clinical.repository;

import com.appointment.clinical.model.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {
    List<FollowUp> findByClinicIdAndDueDateAndStatus(Long clinicId, LocalDate dueDate, FollowUp.FollowUpStatus status);
    List<FollowUp> findByPatientId(Long patientId);

    @Query("SELECT f FROM FollowUp f WHERE f.clinicId = :clinicId AND f.dueDate < :today AND f.status = 'PENDING'")
    List<FollowUp> findOverdueFollowUps(@Param("clinicId") Long clinicId, @Param("today") LocalDate today);

    @Query("SELECT f FROM FollowUp f WHERE f.clinicId = :clinicId AND f.dueDate BETWEEN :startDate AND :endDate ORDER BY f.dueDate ASC")
    List<FollowUp> findUpcomingFollowUps(@Param("clinicId") Long clinicId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}

