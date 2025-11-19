package com.appointment.payment.service;

import com.appointment.payment.model.Invoice;
import com.appointment.payment.model.Payment;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class PdfInvoiceGenerator {
    
    @Value("${invoice.storage.path:./invoices}")
    private String invoiceStoragePath;
    
    public String generatePdf(Invoice invoice, Payment payment) throws IOException {
        // Ensure directory exists
        Path dirPath = Paths.get(invoiceStoragePath);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        
        String fileName = invoice.getInvoiceNumber() + ".pdf";
        String filePath = Paths.get(invoiceStoragePath, fileName).toString();
        
        try (PdfWriter writer = new PdfWriter(filePath);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {
            
            // Header
            Paragraph header = new Paragraph("INVOICE")
                    .setFontSize(24)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(header);
            
            // Invoice details
            Paragraph invoiceNumber = new Paragraph("Invoice Number: " + invoice.getInvoiceNumber())
                    .setFontSize(12)
                    .setMarginBottom(5);
            document.add(invoiceNumber);
            
            Paragraph date = new Paragraph("Date: " + invoice.getIssuedAt()
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                    .setFontSize(12)
                    .setMarginBottom(20);
            document.add(date);
            
            // Patient and Doctor info (simplified - should fetch from DB)
            Paragraph patientInfo = new Paragraph("Bill To:\nPatient ID: " + invoice.getPatientId())
                    .setFontSize(10)
                    .setMarginBottom(10);
            document.add(patientInfo);
            
            Paragraph doctorInfo = new Paragraph("Service Provider:\nDoctor ID: " + invoice.getDoctorId())
                    .setFontSize(10)
                    .setMarginBottom(20);
            document.add(doctorInfo);
            
            // Items table
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1, 1, 1}))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);
            
            // Table header
            table.addHeaderCell(new Paragraph("Description").setBold());
            table.addHeaderCell(new Paragraph("Quantity").setBold().setTextAlignment(TextAlignment.CENTER));
            table.addHeaderCell(new Paragraph("Rate").setBold().setTextAlignment(TextAlignment.RIGHT));
            table.addHeaderCell(new Paragraph("Amount").setBold().setTextAlignment(TextAlignment.RIGHT));
            
            // Table data
            table.addCell(new Paragraph("Consultation Fee"));
            table.addCell(new Paragraph("1").setTextAlignment(TextAlignment.CENTER));
            table.addCell(new Paragraph(invoice.getAmount().toString()).setTextAlignment(TextAlignment.RIGHT));
            table.addCell(new Paragraph(invoice.getAmount().toString()).setTextAlignment(TextAlignment.RIGHT));
            
            document.add(table);
            
            // Totals
            Paragraph subtotal = new Paragraph("Subtotal: " + invoice.getAmount() + " " + invoice.getCurrency())
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginBottom(5);
            document.add(subtotal);
            
            Paragraph tax = new Paragraph("Tax (18%): " + invoice.getTaxAmount() + " " + invoice.getCurrency())
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginBottom(5);
            document.add(tax);
            
            Paragraph total = new Paragraph("Total: " + invoice.getTotalAmount() + " " + invoice.getCurrency())
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBold()
                    .setFontSize(14)
                    .setMarginBottom(20);
            document.add(total);
            
            // Payment info
            Paragraph paymentInfo = new Paragraph("Payment Status: " + payment.getStatus())
                    .setFontSize(10)
                    .setMarginTop(20);
            document.add(paymentInfo);
            
            if (payment.getPaidAt() != null) {
                Paragraph paidDate = new Paragraph("Paid On: " + payment.getPaidAt()
                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")))
                        .setFontSize(10);
                document.add(paidDate);
            }
            
            // Footer
            Paragraph footer = new Paragraph("Thank you for your business!")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10)
                    .setMarginTop(30);
            document.add(footer);
        }
        
        return filePath;
    }
}

