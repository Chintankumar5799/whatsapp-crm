package com.appointment.payment.example;

import com.appointment.payment.dto.PaymentLinkRequest;
import com.appointment.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Example usage of the Payment Service
 * This demonstrates how to integrate payment functionality into your booking flow
 */
@Component
@RequiredArgsConstructor
public class PaymentUsageExample {
    
    private final PaymentService paymentService;
    
    /**
     * Example: Create a payment link after booking confirmation
     */
    public void createPaymentLinkForBooking(Long bookingId, Long patientId, String patientName, 
                                           String patientEmail, String patientPhone) {
        
        PaymentLinkRequest request = PaymentLinkRequest.builder()
                .bookingId(bookingId)
                .patientId(patientId)
                .amount(new BigDecimal("500.00")) // Consultation fee
                .currency("INR")
                .description("Consultation fee for appointment")
                .customerName(patientName)
                .customerEmail(patientEmail)
                .customerPhone(patientPhone)
                .callbackUrl("https://yourapp.com/payment/callback")
                .redirectUrl("https://yourapp.com/payment/success")
                .build();
        
        // Create payment link
        var response = paymentService.createPaymentLink(request);
        
        // Send payment link to patient via WhatsApp
        // whatsAppService.sendPaymentLink(patientPhone, response.getPaymentLink());
        
        System.out.println("Payment link created: " + response.getPaymentLink());
    }
    
    /**
     * Example: Check payment status for a booking
     */
    public void checkPaymentStatus(Long bookingId) {
        var payments = paymentService.getPaymentsByBooking(bookingId);
        
        payments.forEach(payment -> {
            System.out.println("Payment ID: " + payment.getId());
            System.out.println("Status: " + payment.getStatus());
            System.out.println("Amount: " + payment.getAmount());
            
            if (payment.getStatus().name().equals("SUCCESS")) {
                // Payment successful - get invoice
                var invoices = paymentService.getInvoicesByBooking(bookingId);
                invoices.forEach(invoice -> {
                    System.out.println("Invoice Number: " + invoice.getInvoiceNumber());
                    System.out.println("Total Amount: " + invoice.getTotalAmount());
                });
            }
        });
    }
}

