package com.appointment.payment.service;

import com.appointment.auth.model.User;
import com.appointment.auth.repository.UserRepository;
import com.appointment.booking.model.Booking;
import com.appointment.booking.model.Patient;
import com.appointment.booking.repository.BookingRepository;
import com.appointment.booking.repository.PatientRepository;
import com.appointment.clinical.repository.ConsultationRepository;
import com.appointment.payment.dto.PosBillRequest;
import com.appointment.payment.model.*;
import com.appointment.payment.repository.InvoiceItemRepository;
import com.appointment.payment.repository.InvoiceRepository;
import com.appointment.payment.repository.PaymentRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final ConsultationRepository consultationRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PdfInvoiceGenerator pdfInvoiceGenerator;

    @Value("${invoice.storage.path:./invoices}")
    private String invoiceStoragePath;

    @Transactional
    public Invoice generatePosBill(PosBillRequest req) {
        Long clinicId = req.getClinicId() != null ? req.getClinicId() : 1L;

        // Resolve patient & doctor
        Patient patient = patientRepository.findById(req.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + req.getPatientId()));

        User doctor = null;
        if (req.getDoctorId() != null && req.getDoctorId() > 0) {
            doctor = userRepository.findById(req.getDoctorId()).orElse(null);
        }
        String docName = doctor != null ? doctor.getFullName() : "Attending Physician";

        // Calculate subtotal from items
        BigDecimal subtotal = BigDecimal.ZERO;
        List<InvoiceItem> itemsToSave = new ArrayList<>();
        if (req.getItems() != null && !req.getItems().isEmpty()) {
            for (PosBillRequest.BillItemDto it : req.getItems()) {
                if (it.getUnitPrice() != null) {
                    int qty = it.getQuantity() != null && it.getQuantity() > 0 ? it.getQuantity() : 1;
                    BigDecimal itemTotal = it.getUnitPrice().multiply(BigDecimal.valueOf(qty));
                    subtotal = subtotal.add(itemTotal);
                    itemsToSave.add(InvoiceItem.builder()
                            .itemName(it.getItemName())
                            .quantity(qty)
                            .unitPrice(it.getUnitPrice())
                            .totalPrice(itemTotal)
                            .build());
                }
            }
        }
        if (subtotal.compareTo(BigDecimal.ZERO) == 0) {
            subtotal = BigDecimal.valueOf(500.00);
            itemsToSave.add(InvoiceItem.builder()
                    .itemName("Outpatient Consultation Fee")
                    .quantity(1)
                    .unitPrice(subtotal)
                    .totalPrice(subtotal)
                    .build());
        }

        BigDecimal discount = req.getDiscountAmount() != null ? req.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal netTotal = subtotal.subtract(discount).max(BigDecimal.ZERO);

        PaymentMethod pMethod = PaymentMethod.CASH;
        if (req.getPaymentMode() != null) {
            String mode = req.getPaymentMode().toUpperCase();
            if (mode.contains("UPI")) {
                pMethod = PaymentMethod.UPI;
            } else if (mode.contains("CARD")) {
                pMethod = PaymentMethod.CARD;
            } else if (mode.contains("NET") || mode.contains("BANK")) {
                pMethod = PaymentMethod.NETBANKING;
            }
        }

        // 1. Create Payment Record
        Payment payment = Payment.builder()
                .clinicId(clinicId)
                .bookingId(req.getBookingId())
                .consultationId(req.getConsultationId())
                .patientId(req.getPatientId())
                .amount(netTotal)
                .currency("INR")
                .status(PaymentStatus.SUCCESS)
                .paymentMethod(pMethod)
                .provider("POS_CASH_DESK")
                .paymentMode(req.getPaymentMode() != null ? req.getPaymentMode().toUpperCase() : "CASH")
                .discountAmount(discount)
                .description("In-clinic POS settlement via " + req.getPaymentMode())
                .paidAt(LocalDateTime.now())
                .build();
        payment = paymentRepository.save(payment);

        // 2. Create Invoice
        String invNumber = generateInvoiceNumber();
        Invoice invoice = Invoice.builder()
                .invoiceNumber(invNumber)
                .clinicId(clinicId)
                .bookingId(req.getBookingId())
                .consultationId(req.getConsultationId())
                .paymentId(payment.getId())
                .patientId(req.getPatientId())
                .doctorId(req.getDoctorId() != null ? req.getDoctorId() : 1L)
                .patientName(patient.getName())
                .doctorName(docName)
                .paymentMode(req.getPaymentMode() != null ? req.getPaymentMode().toUpperCase() : "CASH")
                .amount(subtotal)
                .discountAmount(discount)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(netTotal)
                .currency("INR")
                .issuedAt(LocalDateTime.now())
                .build();
        invoice = invoiceRepository.save(invoice);

        // 3. Save Invoice Items
        for (InvoiceItem it : itemsToSave) {
            it.setInvoiceId(invoice.getId());
            invoiceItemRepository.save(it);
        }
        invoice.setItems(itemsToSave);

        // 4. Update Booking if present
        if (req.getBookingId() != null) {
            Booking booking = bookingRepository.findById(req.getBookingId()).orElse(null);
            if (booking != null) {
                booking.setPaymentStatus("PAID");
                booking.setStatus(Booking.BookingStatus.COMPLETED);
                booking.setCompletedAt(LocalDateTime.now());
                bookingRepository.save(booking);
            }
        }

        // 5. Generate and Cache PDF
        try {
            String pdfPath = pdfInvoiceGenerator.generatePdf(invoice, payment);
            invoice.setPdfPath(pdfPath);
            invoiceRepository.save(invoice);
        } catch (Exception e) {
            log.warn("Could not save invoice PDF to disk: {}", e.getMessage());
        }

        log.info("Generated POS Bill Receipt: {} for patient: {}, Net Total: ₹{}", invNumber, patient.getName(), netTotal);
        return invoice;
    }

    @Transactional
    public Invoice generateInvoice(Payment payment) {
        log.info("Generating invoice for payment: {}", payment.getId());

        String invoiceNumber = generateInvoiceNumber();
        BigDecimal taxRate = new BigDecimal("0.00");
        BigDecimal taxAmount = payment.getAmount().multiply(taxRate);
        BigDecimal totalAmount = payment.getAmount().add(taxAmount);

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .clinicId(payment.getClinicId() != null ? payment.getClinicId() : 1L)
                .bookingId(payment.getBookingId())
                .paymentId(payment.getId())
                .patientId(payment.getPatientId())
                .doctorId(1L)
                .amount(payment.getAmount())
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .currency(payment.getCurrency())
                .issuedAt(LocalDateTime.now())
                .build();

        invoice = invoiceRepository.save(invoice);

        try {
            String pdfPath = pdfInvoiceGenerator.generatePdf(invoice, payment);
            invoice.setPdfPath(pdfPath);
            invoiceRepository.save(invoice);
        } catch (Exception e) {
            log.error("Error generating PDF invoice", e);
        }

        return invoice;
    }

    public String generateInvoiceNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "INV-" + timestamp + "-" + uniqueId;
    }

    public byte[] getInvoicePdf(Long invoiceId) throws IOException {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        if (invoice.getPdfPath() != null) {
            Path pdfPath = Paths.get(invoice.getPdfPath());
            if (Files.exists(pdfPath)) {
                return Files.readAllBytes(pdfPath);
            }
        }

        Payment payment = invoice.getPaymentId() != null
                ? paymentRepository.findById(invoice.getPaymentId()).orElse(null)
                : null;
        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdOrderByIdAsc(invoiceId);

        return pdfInvoiceGenerator.generateInvoicePdfBytes(invoice, payment, items);
    }
}
