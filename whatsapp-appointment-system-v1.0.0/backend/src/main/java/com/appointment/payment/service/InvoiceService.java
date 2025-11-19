package com.appointment.payment.service;

import com.appointment.payment.model.Invoice;
import com.appointment.payment.model.Payment;
import com.appointment.payment.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {
    
    private final InvoiceRepository invoiceRepository;
    private final PdfInvoiceGenerator pdfInvoiceGenerator;
    
    @Value("${invoice.storage.path:./invoices}")
    private String invoiceStoragePath;
    
    @Transactional
    public Invoice generateInvoice(Payment payment) {
        log.info("Generating invoice for payment: {}", payment.getId());
        
        // Generate invoice number
        String invoiceNumber = generateInvoiceNumber();
        
        // Calculate amounts (assuming 18% GST for now - should come from config)
        BigDecimal taxRate = new BigDecimal("0.18");
        BigDecimal taxAmount = payment.getAmount().multiply(taxRate);
        BigDecimal totalAmount = payment.getAmount().add(taxAmount);
        
        // Create invoice entity
        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .bookingId(payment.getBookingId())
                .paymentId(payment.getId())
                .patientId(payment.getPatientId())
                .doctorId(1L) // TODO: Get from booking
                .amount(payment.getAmount())
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .currency(payment.getCurrency())
                .issuedAt(LocalDateTime.now())
                .build();
        
        invoice = invoiceRepository.save(invoice);
        
        // Generate PDF
        try {
            String pdfPath = pdfInvoiceGenerator.generatePdf(invoice, payment);
            invoice.setPdfPath(pdfPath);
            invoiceRepository.save(invoice);
            log.info("PDF invoice generated: {}", pdfPath);
        } catch (Exception e) {
            log.error("Error generating PDF invoice", e);
            throw new RuntimeException("Failed to generate PDF invoice", e);
        }
        
        return invoice;
    }
    
    private String generateInvoiceNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "INV-" + timestamp + "-" + uniqueId;
    }
    
    public byte[] getInvoicePdf(Long invoiceId) throws IOException {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        
        if (invoice.getPdfPath() == null) {
            throw new IllegalStateException("PDF not generated for this invoice");
        }
        
        Path pdfPath = Paths.get(invoice.getPdfPath());
        if (!Files.exists(pdfPath)) {
            throw new IllegalStateException("PDF file not found");
        }
        
        return Files.readAllBytes(pdfPath);
    }
}
