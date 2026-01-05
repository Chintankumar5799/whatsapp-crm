package com.appointment.payment.service;

import com.appointment.payment.dto.PaymentLinkRequest;
import com.appointment.payment.dto.PaymentLinkResponse;
import com.appointment.payment.dto.WebhookPayload;
import com.appointment.payment.model.Invoice;
import com.appointment.payment.model.Payment;
import com.appointment.payment.model.PaymentStatus;
import com.appointment.payment.repository.InvoiceRepository;
import com.appointment.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentProvider paymentProvider;
    private final InvoiceService invoiceService;
    
    @Value("${payment.default-provider:RAZORPAY}")
    private String defaultProvider;
    
    public PaymentLinkResponse createPaymentLink(PaymentLinkRequest request) {
        log.info("Creating payment link for booking: {}", request.getBookingId());
        
        try {
            // Check if payment already exists
            List<Payment> existingPayments = paymentRepository.findByBookingIdAndStatus(
                    request.getBookingId(), PaymentStatus.SUCCESS);
            if (!existingPayments.isEmpty()) {
                throw new IllegalStateException("Payment already completed for this booking");
            }
            
            log.info("Using payment provider: {}", paymentProvider.getProviderName());
            PaymentLinkResponse response = paymentProvider.createPaymentLink(request);
            
            // Save payment record
            Payment payment = Payment.builder()
                    .bookingId(request.getBookingId())
                    .patientId(request.getPatientId())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .status(PaymentStatus.PENDING)
                    .provider(defaultProvider)
                    .paymentLinkId(response.getPaymentLinkId())
                    .paymentLink(response.getPaymentLink())
                    .providerOrderId(response.getProviderOrderId())
                    .description(request.getDescription())
                    .build();
            
            paymentRepository.save(payment);
            
            log.info("Payment link created: {} for booking: {}", response.getPaymentLinkId(), request.getBookingId());
            return response;
        } catch (Exception e) {
            log.error("Error creating payment link for booking {}", request.getBookingId(), e);
            throw e;
        }
    }
    
    @Transactional
    public void processWebhook(WebhookPayload webhookPayload) {
        log.info("Processing webhook for payment: {}", webhookPayload.getProviderPaymentId());
        
        Optional<Payment> paymentOpt = Optional.empty();
        
        // Try to find by payment link ID first
        if (webhookPayload.getPaymentLinkId() != null) {
            paymentOpt = paymentRepository.findByPaymentLinkId(webhookPayload.getPaymentLinkId());
        }
        
        // If not found, try by provider payment ID
        if (paymentOpt.isEmpty() && webhookPayload.getProviderPaymentId() != null) {
            paymentOpt = paymentRepository.findByProviderPaymentId(webhookPayload.getProviderPaymentId());
        }
        
        if (paymentOpt.isEmpty()) {
            log.warn("Payment not found for webhook: {}", webhookPayload);
            return;
        }
        
        Payment payment = paymentOpt.get();
        
        // Update payment status
        PaymentStatus newStatus = mapWebhookStatus(webhookPayload.getStatus());
        payment.setStatus(newStatus);
        payment.setProviderPaymentId(webhookPayload.getProviderPaymentId());
        
        if (newStatus == PaymentStatus.SUCCESS && webhookPayload.getPaidAt() != null) {
            payment.setPaidAt(webhookPayload.getPaidAt());
        }
        
        if (webhookPayload.getPaymentMethod() != null) {
            try {
                payment.setPaymentMethod(com.appointment.payment.model.PaymentMethod.valueOf(
                        webhookPayload.getPaymentMethod().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Unknown payment method: {}", webhookPayload.getPaymentMethod());
            }
        }
        
        paymentRepository.save(payment);
        
        // Generate invoice if payment is successful
        if (newStatus == PaymentStatus.SUCCESS) {
            generateInvoice(payment);
        }
        
        log.info("Payment webhook processed successfully. Payment ID: {}, Status: {}", 
                payment.getId(), newStatus);
    }
    
    private PaymentStatus mapWebhookStatus(String status) {
        return switch (status.toUpperCase()) {
            case "SUCCESS", "PAID", "CAPTURED" -> PaymentStatus.SUCCESS;
            case "FAILED", "FAILURE" -> PaymentStatus.FAILED;
            case "CANCELLED", "CANCELED" -> PaymentStatus.CANCELLED;
            case "REFUNDED" -> PaymentStatus.REFUNDED;
            default -> PaymentStatus.PENDING;
        };
    }
    
    private void generateInvoice(Payment payment) {
        try {
            // Check if invoice already exists
            List<Invoice> existingInvoices = invoiceRepository.findByPaymentId(payment.getId());
            if (!existingInvoices.isEmpty()) {
                log.info("Invoice already exists for payment: {}", payment.getId());
                return;
            }
            
            invoiceService.generateInvoice(payment);
            log.info("Invoice generated for payment: {}", payment.getId());
        } catch (Exception e) {
            log.error("Error generating invoice for payment: {}", payment.getId(), e);
        }
    }
    
    public Optional<Payment> getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId);
    }
    
    public List<Payment> getPaymentsByBooking(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }
    
    public List<Payment> getPaymentsByPatient(Long patientId) {
        return paymentRepository.findByPatientId(patientId);
    }
    
    public Optional<Invoice> getInvoice(Long invoiceId) {
        return invoiceRepository.findById(invoiceId);
    }
    
    public List<Invoice> getInvoicesByBooking(Long bookingId) {
        return invoiceRepository.findByBookingId(bookingId);
    }
    
    public byte[] getInvoicePdf(Long invoiceId) throws IOException {
        return invoiceService.getInvoicePdf(invoiceId);
    }
}

