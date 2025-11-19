package com.appointment.payment.repository;

import com.appointment.payment.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    
    List<Invoice> findByBookingId(Long bookingId);
    
    List<Invoice> findByPatientId(Long patientId);
    
    List<Invoice> findByPaymentId(Long paymentId);
}

