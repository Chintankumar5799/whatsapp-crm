package com.appointment.payment.controller;

import com.appointment.clinic.model.Clinic;
import com.appointment.clinic.repository.ClinicRepository;
import com.appointment.payment.dto.PaymentLinkRequest;
import com.appointment.payment.dto.PaymentLinkResponse;
import com.appointment.payment.dto.PosBillRequest;
import com.appointment.payment.model.Invoice;
import com.appointment.payment.model.Payment;
import com.appointment.payment.service.InvoiceService;
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
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Payment management APIs")
@CrossOrigin(origins = "*")
public class PaymentController {
    
    private final PaymentService paymentService;
    private final InvoiceService invoiceService;
    private final ClinicRepository clinicRepository;

    @PostMapping("/pos-bill")
    @Operation(summary = "Generate in-clinic POS bill and printable receipt")
    public ResponseEntity<Invoice> generatePosBill(@RequestBody PosBillRequest request) {
        Invoice invoice = invoiceService.generatePosBill(request);
        return ResponseEntity.ok(invoice);
    }

    @GetMapping("/clinic-upi")
    @Operation(summary = "Get clinic UPI payment details for QR display")
    public ResponseEntity<Map<String, String>> getClinicUpi(@RequestParam(defaultValue = "1") Long clinicId) {
        Clinic clinic = clinicRepository.findById(clinicId).orElse(null);
        String upiId = clinic != null && clinic.getUpiId() != null ? clinic.getUpiId() : "pms.clinic@upi";
        String name = clinic != null && clinic.getName() != null ? clinic.getName() : "PMS Healthcare Clinic";
        return ResponseEntity.ok(Map.of("upiId", upiId, "clinicName", name));
    }
    
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
            headers.setContentDispositionFormData("inline", "receipt-" + invoiceId + ".pdf");
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error downloading invoice PDF", e);
            return ResponseEntity.notFound().build();
        }
    }
}

