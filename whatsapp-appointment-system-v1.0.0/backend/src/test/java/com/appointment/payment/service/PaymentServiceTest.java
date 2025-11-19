package com.appointment.payment.service;

import com.appointment.payment.dto.PaymentLinkRequest;
import com.appointment.payment.dto.PaymentLinkResponse;
import com.appointment.payment.dto.WebhookPayload;
import com.appointment.payment.model.Payment;
import com.appointment.payment.model.PaymentStatus;
import com.appointment.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    
    @Mock
    private PaymentRepository paymentRepository;
    
    @Mock
    private PaymentProvider paymentProvider;
    
    @Mock
    private InvoiceService invoiceService;
    
    @InjectMocks
    private PaymentService paymentService;
    
    private PaymentLinkRequest paymentLinkRequest;
    private PaymentLinkResponse paymentLinkResponse;
    
    @BeforeEach
    void setUp() {
        paymentLinkRequest = PaymentLinkRequest.builder()
                .bookingId(1L)
                .patientId(1L)
                .amount(new BigDecimal("500.00"))
                .currency("INR")
                .description("Consultation fee")
                .customerName("John Doe")
                .customerEmail("john@example.com")
                .customerPhone("+1234567890")
                .build();
        
        paymentLinkResponse = PaymentLinkResponse.builder()
                .paymentLinkId("plink_123456")
                .paymentLink("https://razorpay.com/payment-link/123456")
                .shortUrl("https://rzp.io/abc123")
                .status("created")
                .providerOrderId("order_123456")
                .build();
    }
    
    @Test
    void testCreatePaymentLink_Success() {
        // Given
        when(paymentRepository.findByBookingIdAndStatus(any(), any())).thenReturn(java.util.Collections.emptyList());
        when(paymentProvider.createPaymentLink(any())).thenReturn(paymentLinkResponse);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        PaymentLinkResponse response = paymentService.createPaymentLink(paymentLinkRequest);
        
        // Then
        assertNotNull(response);
        assertEquals("plink_123456", response.getPaymentLinkId());
        verify(paymentProvider, times(1)).createPaymentLink(paymentLinkRequest);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }
    
    @Test
    void testCreatePaymentLink_AlreadyPaid() {
        // Given
        Payment existingPayment = Payment.builder()
                .id(1L)
                .bookingId(1L)
                .status(PaymentStatus.SUCCESS)
                .build();
        when(paymentRepository.findByBookingIdAndStatus(any(), eq(PaymentStatus.SUCCESS)))
                .thenReturn(java.util.Collections.singletonList(existingPayment));
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            paymentService.createPaymentLink(paymentLinkRequest);
        });
        
        verify(paymentProvider, never()).createPaymentLink(any());
    }
    
    @Test
    void testProcessWebhook_Success() {
        // Given
        WebhookPayload webhookPayload = WebhookPayload.builder()
                .event("payment_link.paid")
                .providerPaymentId("pay_123456")
                .paymentLinkId("plink_123456")
                .amount(new BigDecimal("500.00"))
                .status("SUCCESS")
                .paidAt(LocalDateTime.now())
                .build();
        
        Payment payment = Payment.builder()
                .id(1L)
                .bookingId(1L)
                .patientId(1L)
                .amount(new BigDecimal("500.00"))
                .status(PaymentStatus.PENDING)
                .paymentLinkId("plink_123456")
                .build();
        
        when(paymentRepository.findByPaymentLinkId("plink_123456")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        paymentService.processWebhook(webhookPayload);
        
        // Then
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(invoiceService, times(1)).generateInvoice(any(Payment.class));
    }
    
    @Test
    void testProcessWebhook_PaymentNotFound() {
        // Given
        WebhookPayload webhookPayload = WebhookPayload.builder()
                .paymentLinkId("plink_999999")
                .build();
        
        when(paymentRepository.findByPaymentLinkId("plink_999999")).thenReturn(Optional.empty());
        when(paymentRepository.findByProviderPaymentId(any())).thenReturn(Optional.empty());
        
        // When
        paymentService.processWebhook(webhookPayload);
        
        // Then
        verify(paymentRepository, never()).save(any());
        verify(invoiceService, never()).generateInvoice(any());
    }
}

