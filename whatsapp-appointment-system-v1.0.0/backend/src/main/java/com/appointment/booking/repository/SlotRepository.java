package com.appointment.booking.repository;

import com.appointment.booking.model.Slot;
import com.appointment.booking.model.Slot.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlotRepository extends JpaRepository<Slot, Long> {
    List<Slot> findByDoctorIdAndSlotDate(Long doctorId, LocalDate slotDate);
    
    List<Slot> findByDoctorIdAndSlotDateAndStatus(Long doctorId, LocalDate slotDate, SlotStatus status);
    
    @Query("SELECT s FROM Slot s WHERE s.doctorId = :doctorId AND s.slotDate = :date " +
           "AND s.startTime >= :startTime AND s.endTime <= :endTime AND s.status = :status")
    List<Slot> findAvailableSlots(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("status") SlotStatus status
    );
    
    Optional<Slot> findByDoctorIdAndSlotDateAndStartTimeAndEndTime(
            Long doctorId, LocalDate slotDate, LocalTime startTime, LocalTime endTime
    );
}

