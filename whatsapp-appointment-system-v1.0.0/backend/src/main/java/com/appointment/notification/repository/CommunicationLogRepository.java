package com.appointment.notification.repository;

import com.appointment.notification.model.CommunicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunicationLogRepository extends JpaRepository<CommunicationLog, Long> {
    List<CommunicationLog> findByClinicIdOrderBySentAtDesc(Long clinicId);
    List<CommunicationLog> findByRecipientPhoneOrderBySentAtDesc(String recipientPhone);
    List<CommunicationLog> findByBookingIdOrderBySentAtDesc(Long bookingId);
}

