package com.appointment.payment.repository;

import com.appointment.payment.model.Payment;
import com.appointment.payment.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByProviderPaymentId(String providerPaymentId);
    
    Optional<Payment> findByPaymentLinkId(String paymentLinkId);
    
    List<Payment> findByBookingId(Long bookingId);
    
    List<Payment> findByPatientId(Long patientId);
    
    List<Payment> findByStatus(PaymentStatus status);
    
    List<Payment> findByBookingIdAndStatus(Long bookingId, PaymentStatus status);
}

