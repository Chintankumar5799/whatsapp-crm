package com.appointment.payment.controller;

import com.appointment.payment.dto.PaymentLinkRequest;
import com.appointment.payment.dto.PaymentLinkResponse;
import com.appointment.payment.model.Invoice;
import com.appointment.payment.model.Payment;
import com.appointment.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Payment management APIs")
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @PostMapping("/links")
    @Operation(summary = "Create payment link", description = "Creates a payment link for a booking")
    public ResponseEntity<PaymentLinkResponse> createPaymentLink(@Valid @RequestBody PaymentLinkRequest request) {
        PaymentLinkResponse response = paymentService.createPaymentLink(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment", description = "Retrieves payment details by ID")
    public ResponseEntity<Payment> getPayment(@PathVariable Long paymentId) {
        return paymentService.getPayment(paymentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get payments by booking", description = "Retrieves all payments for a booking")
    public ResponseEntity<List<Payment>> getPaymentsByBooking(@PathVariable Long bookingId) {
        List<Payment> payments = paymentService.getPaymentsByBooking(bookingId);
        return ResponseEntity.ok(payments);
    }
    
    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get payments by patient", description = "Retrieves all payments for a patient")
    public ResponseEntity<List<Payment>> getPaymentsByPatient(@PathVariable Long patientId) {
        List<Payment> payments = paymentService.getPaymentsByPatient(patientId);
        return ResponseEntity.ok(payments);
    }
    
    @GetMapping("/invoices/booking/{bookingId}")
    @Operation(summary = "Get invoices by booking", description = "Retrieves all invoices for a booking")
    public ResponseEntity<List<Invoice>> getInvoicesByBooking(@PathVariable Long bookingId) {
        List<Invoice> invoices = paymentService.getInvoicesByBooking(bookingId);
        return ResponseEntity.ok(invoices);
    }
    
    @GetMapping("/invoices/{invoiceId}/pdf")
    @Operation(summary = "Download invoice PDF", description = "Downloads the PDF invoice")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long invoiceId) {
        try {
            byte[] pdfBytes = paymentService.getInvoicePdf(invoiceId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "invoice-" + invoiceId + ".pdf");
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error downloading invoice PDF", e);
            return ResponseEntity.notFound().build();
        }
    }
}

