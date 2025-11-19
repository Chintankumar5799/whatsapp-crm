package com.appointment.booking.repository;

import com.appointment.booking.model.Availability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    List<Availability> findByDoctorId(Long doctorId);
    
    List<Availability> findByDoctorIdAndIsActiveTrue(Long doctorId);
    
    List<Availability> findByDoctorIdAndDayOfWeek(Long doctorId, DayOfWeek dayOfWeek);
}

