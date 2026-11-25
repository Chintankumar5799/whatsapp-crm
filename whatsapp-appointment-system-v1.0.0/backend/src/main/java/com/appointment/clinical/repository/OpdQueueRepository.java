package com.appointment.clinical.repository;

import com.appointment.clinical.model.OpdQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OpdQueueRepository extends JpaRepository<OpdQueue, Long> {
    List<OpdQueue> findByDoctorIdAndQueueDateOrderByQueueNumberAsc(Long doctorId, LocalDate queueDate);
    List<OpdQueue> findByClinicIdAndQueueDateOrderByQueueNumberAsc(Long clinicId, LocalDate queueDate);
    List<OpdQueue> findByPatientId(Long patientId);
    Optional<OpdQueue> findByBookingId(Long bookingId);

    @Query("SELECT COALESCE(MAX(q.queueNumber), 0) FROM OpdQueue q WHERE q.doctorId = :doctorId AND q.queueDate = :queueDate")
    Integer findMaxQueueNumber(@Param("doctorId") Long doctorId, @Param("queueDate") LocalDate queueDate);
}

