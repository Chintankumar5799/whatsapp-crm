package com.appointment.payment.service;

import com.appointment.auth.model.User;
import com.appointment.auth.repository.UserRepository;
import com.appointment.booking.model.Patient;
import com.appointment.booking.repository.PatientRepository;
import com.appointment.clinic.model.Clinic;
import com.appointment.clinic.repository.ClinicRepository;
import com.appointment.payment.model.Invoice;
import com.appointment.payment.model.InvoiceItem;
import com.appointment.payment.model.Payment;
import com.appointment.payment.repository.InvoiceItemRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PdfInvoiceGenerator {

    private final ClinicRepository clinicRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final InvoiceItemRepository invoiceItemRepository;

    @Value("${invoice.storage.path:./invoices}")
    private String invoiceStoragePath;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public String generatePdf(Invoice invoice, Payment payment) throws IOException {
        Path dirPath = Paths.get(invoiceStoragePath);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        String fileName = invoice.getInvoiceNumber() + ".pdf";
        String filePath = Paths.get(invoiceStoragePath, fileName).toString();

        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdOrderByIdAsc(invoice.getId());
        byte[] pdfBytes = generateInvoicePdfBytes(invoice, payment, items);

        Files.write(Paths.get(filePath), pdfBytes);
        return filePath;
    }

    public byte[] generateInvoicePdfBytes(Invoice invoice, Payment payment, List<InvoiceItem> items) throws IOException {
        Clinic clinic = clinicRepository.findById(invoice.getClinicId() != null ? invoice.getClinicId() : 1L).orElse(null);
        Patient patient = patientRepository.findById(invoice.getPatientId()).orElse(null);
        User doctor = invoice.getDoctorId() != null ? userRepository.findById(invoice.getDoctorId()).orElse(null) : null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            document.setMargins(25, 30, 25, 30);

            // 1. Clinic Letterhead
            String clinicName = clinic != null && clinic.getName() != null ? clinic.getName() : "PMS Healthcare Clinic";
            String clinicAddress = clinic != null && clinic.getAddressLine1() != null
                    ? clinic.getAddressLine1() + ", " + (clinic.getCity() != null ? clinic.getCity() : "")
                    : "Outpatient Consulting Clinic";
            String clinicPhone = clinic != null && clinic.getPhone() != null ? clinic.getPhone() : "";
            String clinicUpi = clinic != null && clinic.getUpiId() != null ? clinic.getUpiId() : "";

            Paragraph header = new Paragraph(clinicName)
                    .setFontSize(18)
                    .setBold()
                    .setFontColor(new DeviceRgb(25, 118, 210))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(2);
            document.add(header);

            Paragraph addressLine = new Paragraph(clinicAddress + " | Helpline: " + clinicPhone)
                    .setFontSize(9)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(12);
            document.add(addressLine);

            // Title Banner
            Table titleTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .useAllAvailableWidth()
                    .setMarginBottom(15);

            Cell leftTitle = new Cell()
                    .add(new Paragraph("OFFICIAL CLINIC RECEIPT / TAX INVOICE").setBold().setFontSize(12).setFontColor(ColorConstants.BLACK))
                    .add(new Paragraph("Receipt #: " + invoice.getInvoiceNumber()).setFontSize(10))
                    .setBorder(null);

            String issuedAtStr = invoice.getIssuedAt() != null
                    ? invoice.getIssuedAt().format(DATE_TIME_FORMATTER)
                    : java.time.LocalDateTime.now().format(DATE_TIME_FORMATTER);

            String modeStr = invoice.getPaymentMode() != null ? invoice.getPaymentMode().toUpperCase() : "CASH";

            Cell rightTitle = new Cell()
                    .add(new Paragraph("Date: " + issuedAtStr).setFontSize(10))
                    .add(new Paragraph("Payment Mode: " + modeStr).setBold().setFontSize(10).setFontColor(new DeviceRgb(46, 125, 50)))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBorder(null);

            titleTable.addCell(leftTitle);
            titleTable.addCell(rightTitle);
            document.add(titleTable);

            // 2. Patient & Doctor Details Box
            Table metaTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .useAllAvailableWidth()
                    .setBorder(new SolidBorder(new DeviceRgb(220, 224, 230), 1))
                    .setMarginBottom(15);

            String patName = invoice.getPatientName() != null ? invoice.getPatientName() : (patient != null ? patient.getName() : "Patient #" + invoice.getPatientId());
            String patUid = patient != null && patient.getPatientUid() != null ? patient.getPatientUid() : "MRN: Pending";
            String patPhone = patient != null && patient.getPhone() != null ? patient.getPhone() : "";
            String patAgeGender = patient != null ? ((patient.getAge() != null ? patient.getAge() + " Yrs" : "") + " / " + (patient.getGender() != null ? patient.getGender() : "")) : "";

            Cell patCell = new Cell()
                    .add(new Paragraph("BILL TO PATIENT").setBold().setFontSize(9).setFontColor(ColorConstants.GRAY))
                    .add(new Paragraph(patName).setBold().setFontSize(11))
                    .add(new Paragraph(patUid + " | " + patAgeGender).setFontSize(9))
                    .add(new Paragraph("Contact: " + patPhone).setFontSize(9))
                    .setPadding(8)
                    .setBorder(null);

            String docName = invoice.getDoctorName() != null ? invoice.getDoctorName() : (doctor != null ? doctor.getFullName() : "Attending Physician");
            String docReg = doctor != null && doctor.getRegistrationNumber() != null ? "Reg #: " + doctor.getRegistrationNumber() : "Medical Council Registered";

            Cell docCell = new Cell()
                    .add(new Paragraph("CONSULTING DOCTOR").setBold().setFontSize(9).setFontColor(ColorConstants.GRAY))
                    .add(new Paragraph(docName).setBold().setFontSize(11))
                    .add(new Paragraph(docReg).setFontSize(9))
                    .setPadding(8)
                    .setBorder(null);

            metaTable.addCell(patCell);
            metaTable.addCell(docCell);
            document.add(metaTable);

            // 3. Itemized Procedures & Fees Table
            Table itemTable = new Table(UnitValue.createPercentArray(new float[]{4, 1, 2, 2}))
                    .useAllAvailableWidth()
                    .setMarginBottom(15);

            DeviceRgb headerBg = new DeviceRgb(240, 243, 246);
            itemTable.addHeaderCell(new Cell().add(new Paragraph("Item / Clinical Service").setBold().setFontSize(9)).setBackgroundColor(headerBg).setPadding(6));
            itemTable.addHeaderCell(new Cell().add(new Paragraph("Qty").setBold().setFontSize(9).setTextAlignment(TextAlignment.CENTER)).setBackgroundColor(headerBg).setPadding(6));
            itemTable.addHeaderCell(new Cell().add(new Paragraph("Rate (₹)").setBold().setFontSize(9).setTextAlignment(TextAlignment.RIGHT)).setBackgroundColor(headerBg).setPadding(6));
            itemTable.addHeaderCell(new Cell().add(new Paragraph("Amount (₹)").setBold().setFontSize(9).setTextAlignment(TextAlignment.RIGHT)).setBackgroundColor(headerBg).setPadding(6));

            if (items != null && !items.isEmpty()) {
                for (InvoiceItem it : items) {
                    itemTable.addCell(new Cell().add(new Paragraph(it.getItemName()).setFontSize(9)).setPadding(5));
                    itemTable.addCell(new Cell().add(new Paragraph(String.valueOf(it.getQuantity())).setFontSize(9).setTextAlignment(TextAlignment.CENTER)).setPadding(5));
                    itemTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", it.getUnitPrice())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)).setPadding(5));
                    itemTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", it.getTotalPrice())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)).setPadding(5));
                }
            } else {
                itemTable.addCell(new Cell().add(new Paragraph("Outpatient Consultation Fee").setFontSize(9)).setPadding(5));
                itemTable.addCell(new Cell().add(new Paragraph("1").setFontSize(9).setTextAlignment(TextAlignment.CENTER)).setPadding(5));
                itemTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", invoice.getAmount())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)).setPadding(5));
                itemTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", invoice.getAmount())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)).setPadding(5));
            }

            document.add(itemTable);

            // 4. Totals & Settlement Box
            Table totalsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .useAllAvailableWidth()
                    .setMarginBottom(25);

            Cell paymentStatusBadge = new Cell()
                    .add(new Paragraph("✓ PAYMENT RECEIVED").setBold().setFontSize(14).setFontColor(new DeviceRgb(46, 125, 50)))
                    .add(new Paragraph("Settled via " + modeStr).setFontSize(9).setFontColor(ColorConstants.GRAY))
                    .setBorder(null)
                    .setPadding(8);

            BigDecimal subtotal = invoice.getAmount();
            BigDecimal discount = invoice.getDiscountAmount() != null ? invoice.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal totalNet = invoice.getTotalAmount();

            Cell calcBox = new Cell()
                    .add(new Paragraph("Subtotal: ₹" + String.format("%.2f", subtotal)).setFontSize(9).setTextAlignment(TextAlignment.RIGHT))
                    .add(new Paragraph("Discount: ₹" + String.format("%.2f", discount)).setFontSize(9).setFontColor(ColorConstants.DARK_GRAY).setTextAlignment(TextAlignment.RIGHT))
                    .add(new Paragraph("Net Amount Paid: ₹" + String.format("%.2f", totalNet)).setBold().setFontSize(13).setFontColor(new DeviceRgb(25, 118, 210)).setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(null)
                    .setPadding(8);

            totalsTable.addCell(paymentStatusBadge);
            totalsTable.addCell(calcBox);
            document.add(totalsTable);

            // 5. Sign-off and Footer
            Table footerTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .useAllAvailableWidth()
                    .setMarginTop(20);

            Cell noteCell = new Cell()
                    .add(new Paragraph("Notes:").setBold().setFontSize(8).setFontColor(ColorConstants.GRAY))
                    .add(new Paragraph("Computer-generated clinic receipt. Thank you for visiting PMS Healthcare.").setFontSize(8).setFontColor(ColorConstants.GRAY))
                    .setBorder(null);

            Cell signCell = new Cell()
                    .add(new Paragraph("Authorized Cashier / Clinic Seal").setBold().setFontSize(9).setTextAlignment(TextAlignment.RIGHT))
                    .add(new Paragraph("(Electronically Verified)").setFontSize(8).setFontColor(ColorConstants.GRAY).setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(null);

            footerTable.addCell(noteCell);
            footerTable.addCell(signCell);
            document.add(footerTable);
        }

        return baos.toByteArray();
    }
}
