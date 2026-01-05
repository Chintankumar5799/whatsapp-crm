package com.appointment.booking.repository;

import com.appointment.booking.model.PendingAppointmentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PendingAppointmentRequestRepository extends JpaRepository<PendingAppointmentRequest, Long> {
    @Query("SELECT r FROM PendingAppointmentRequest r WHERE r.doctorPhone = (SELECT u.phone FROM User u WHERE u.id = :doctorId)")
    List<PendingAppointmentRequest> findByDoctorId(@Param("doctorId") Long doctorId);
}
