package com.appointment.booking.repository;

import com.appointment.booking.model.AvailabilityException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AvailabilityExceptionRepository extends JpaRepository<AvailabilityException, Long> {
    List<AvailabilityException> findByDoctorId(Long doctorId);
    
    Optional<AvailabilityException> findByDoctorIdAndExceptionDate(Long doctorId, LocalDate exceptionDate);
    
    List<AvailabilityException> findByDoctorIdAndExceptionDateBetween(
            Long doctorId, LocalDate startDate, LocalDate endDate
    );
}

