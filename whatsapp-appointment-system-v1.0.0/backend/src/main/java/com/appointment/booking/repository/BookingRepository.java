package com.appointment.booking.repository;

import com.appointment.booking.model.Booking;
import com.appointment.booking.model.Booking.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingNumber(String bookingNumber);
    
    List<Booking> findByDoctorId(Long doctorId);
    
    List<Booking> findByPatientId(Long patientId);
    
    List<Booking> findByStatus(BookingStatus status);
    
    List<Booking> findByDoctorIdAndStatus(Long doctorId, BookingStatus status);
    
    @Query("SELECT b FROM Booking b WHERE b.doctorId = :doctorId AND b.bookingDate = :date AND b.status IN :statuses")
    List<Booking> findByDoctorIdAndDateAndStatusIn(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("statuses") List<BookingStatus> statuses
    );
    
    @Query("SELECT b FROM Booking b WHERE b.doctorId = :doctorId AND b.bookingDate >= :startDate AND b.bookingDate <= :endDate")
    List<Booking> findByDoctorIdAndDateRange(
            @Param("doctorId") Long doctorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}

