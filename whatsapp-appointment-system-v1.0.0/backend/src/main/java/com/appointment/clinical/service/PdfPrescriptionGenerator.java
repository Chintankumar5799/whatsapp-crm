package com.appointment.clinical.service;

import com.appointment.auth.model.User;
import com.appointment.booking.model.Patient;
import com.appointment.clinic.model.Clinic;
import com.appointment.clinical.model.Consultation;
import com.appointment.clinical.model.PrescriptionItem;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class PdfPrescriptionGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public byte[] generatePrescriptionPdf(
            Consultation consultation,
            List<PrescriptionItem> items,
            Patient patient,
            User doctor,
            Clinic clinic
    ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            document.setMargins(25, 30, 25, 30);

            // 1. Clinic Header
            String clinicName = clinic != null && clinic.getName() != null ? clinic.getName() : "PMS Healthcare Clinic";
            String clinicAddress = clinic != null && clinic.getAddressLine1() != null 
                    ? clinic.getAddressLine1() + ", " + (clinic.getCity() != null ? clinic.getCity() : "") 
                    : "Outpatient Clinic";
            String clinicContact = clinic != null && clinic.getPhone() != null ? "Phone: " + clinic.getPhone() : "";

            Paragraph clinicHeader = new Paragraph(clinicName)
                    .setFontSize(18)
                    .setBold()
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(2);
            document.add(clinicHeader);

            Paragraph clinicSubHeader = new Paragraph(clinicAddress + " | " + clinicContact)
                    .setFontSize(9)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(12);
            document.add(clinicSubHeader);

            // Divider line
            Table line = new Table(UnitValue.createPercentArray(new float[]{100}))
                    .useAllAvailableWidth()
                    .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 1));
            document.add(line);

            // 2. Doctor Info & Patient Info Table
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                    .useAllAvailableWidth()
                    .setMarginTop(8)
                    .setMarginBottom(12);

            // Doctor details (Left side)
            String docName = doctor != null && doctor.getName() != null ? doctor.getName() : "Consulting Physician";
            String qualification = doctor != null && doctor.getQualification() != null ? doctor.getQualification() : "MBBS, MD";
            String regNo = doctor != null && doctor.getRegistrationNumber() != null ? "Reg No: " + doctor.getRegistrationNumber() : "";

            Cell doctorCell = new Cell().setBorder(Border.NO_BORDER);
            doctorCell.add(new Paragraph("DOCTOR:").setFontSize(8).setBold().setFontColor(ColorConstants.GRAY));
            doctorCell.add(new Paragraph(docName).setFontSize(11).setBold());
            doctorCell.add(new Paragraph(qualification + (regNo.isEmpty() ? "" : " | " + regNo)).setFontSize(9));
            infoTable.addCell(doctorCell);

            // Patient details (Right side)
            String patientName = patient != null ? patient.getName() : "Walk-in Patient";
            String patientUid = patient != null ? patient.getPatientUid() : "N/A";
            String ageGender = (patient != null && patient.getAge() != null ? patient.getAge() + " yrs" : "") + 
                               (patient != null && patient.getGender() != null ? " / " + patient.getGender() : "");
            String dateStr = consultation.getFinalizedAt() != null 
                    ? consultation.getFinalizedAt().format(DATE_FORMATTER) 
                    : "";

            Cell patientCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
            patientCell.add(new Paragraph("PATIENT DETAILS:").setFontSize(8).setBold().setFontColor(ColorConstants.GRAY));
            patientCell.add(new Paragraph(patientName + " (" + patientUid + ")").setFontSize(11).setBold());
            patientCell.add(new Paragraph(ageGender + " | Date: " + dateStr).setFontSize(9));
            infoTable.addCell(patientCell);

            document.add(infoTable);

            // 3. Vitals Box (if available)
            StringBuilder vitalsStr = new StringBuilder();
            if (consultation.getBpSystolic() != null && consultation.getBpDiastolic() != null) {
                vitalsStr.append("BP: ").append(consultation.getBpSystolic()).append("/").append(consultation.getBpDiastolic()).append(" mmHg   ");
            }
            if (consultation.getPulse() != null) {
                vitalsStr.append("Pulse: ").append(consultation.getPulse()).append(" bpm   ");
            }
            if (consultation.getTemperature() != null) {
                vitalsStr.append("Temp: ").append(consultation.getTemperature()).append(" °F   ");
            }
            if (consultation.getWeight() != null) {
                vitalsStr.append("Weight: ").append(consultation.getWeight()).append(" kg   ");
            }
            if (consultation.getSpo2() != null) {
                vitalsStr.append("SpO2: ").append(consultation.getSpo2()).append("%   ");
            }

            if (vitalsStr.length() > 0) {
                Table vitalsTable = new Table(UnitValue.createPercentArray(new float[]{100}))
                        .useAllAvailableWidth()
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY, 0.2f)
                        .setMarginBottom(10);
                vitalsTable.addCell(new Cell()
                        .add(new Paragraph("Vitals: " + vitalsStr.toString()).setFontSize(9).setBold())
                        .setBorder(Border.NO_BORDER)
                        .setPadding(5));
                document.add(vitalsTable);
            }

            // 4. Clinical Complaints & Diagnosis
            Paragraph complaint = new Paragraph("Chief Complaint: " + (consultation.getChiefComplaint() != null ? consultation.getChiefComplaint() : "N/A"))
                    .setFontSize(10)
                    .setMarginBottom(4);
            document.add(complaint);

            Paragraph diagnosis = new Paragraph("Diagnosis: " + (consultation.getDiagnosis() != null ? consultation.getDiagnosis() : "N/A"))
                    .setFontSize(10)
                    .setBold()
                    .setMarginBottom(12);
            document.add(diagnosis);

            // 5. Prescription Symbol & Medications Table
            Paragraph rxSymbol = new Paragraph("Rx (Medications):")
                    .setFontSize(12)
                    .setBold()
                    .setFontColor(ColorConstants.BLUE)
                    .setMarginBottom(6);
            document.add(rxSymbol);

            Table rxTable = new Table(UnitValue.createPercentArray(new float[]{5, 40, 20, 15, 20}))
                    .useAllAvailableWidth()
                    .setMarginBottom(15);

            // Table Header
            String[] headers = {"#", "Medicine Name", "Dosage", "Duration", "Instructions"};
            for (String h : headers) {
                rxTable.addHeaderCell(new Cell()
                        .add(new Paragraph(h).setBold().setFontSize(9))
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY, 0.4f)
                        .setPadding(4));
            }

            // Table Rows
            if (items != null && !items.isEmpty()) {
                int idx = 1;
                for (PrescriptionItem item : items) {
                    String medName = item.getCustomMedicineName() != null ? item.getCustomMedicineName() : "Medicine #" + item.getMedicineId();
                    String dosage = item.getDosage() != null ? item.getDosage() : "1-0-1";
                    String duration = item.getDurationDays() != null ? item.getDurationDays() + " days" : "5 days";
                    String inst = item.getInstructions() != null ? item.getInstructions() : "After food";

                    rxTable.addCell(new Cell().add(new Paragraph(String.valueOf(idx++)).setFontSize(9)).setPadding(4));
                    rxTable.addCell(new Cell().add(new Paragraph(medName).setFontSize(9).setBold()).setPadding(4));
                    rxTable.addCell(new Cell().add(new Paragraph(dosage).setFontSize(9)).setPadding(4));
                    rxTable.addCell(new Cell().add(new Paragraph(duration).setFontSize(9)).setPadding(4));
                    rxTable.addCell(new Cell().add(new Paragraph(inst).setFontSize(9)).setPadding(4));
                }
            } else {
                Cell emptyCell = new Cell(1, 5)
                        .add(new Paragraph("No medications prescribed.").setFontSize(9).setItalic())
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(8);
                rxTable.addCell(emptyCell);
            }
            document.add(rxTable);

            // 6. Advice & Doctor Signature
            Table footerTable = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                    .useAllAvailableWidth()
                    .setMarginTop(30);

            Cell adviceCell = new Cell().setBorder(Border.NO_BORDER);
            adviceCell.add(new Paragraph("General Advice:").setFontSize(9).setBold());
            adviceCell.add(new Paragraph("• Take medications on time.\n• Drink plenty of water and maintain rest.\n• In case of emergency or adverse reaction, contact clinic immediately.").setFontSize(8).setFontColor(ColorConstants.DARK_GRAY));
            footerTable.addCell(adviceCell);

            Cell sigCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
            sigCell.add(new Paragraph("\n\n__________________________").setFontSize(9));
            sigCell.add(new Paragraph("Dr. " + docName).setFontSize(9).setBold());
            sigCell.add(new Paragraph("Authorized Signatory").setFontSize(8).setFontColor(ColorConstants.GRAY));
            footerTable.addCell(sigCell);

            document.add(footerTable);
        }

        return baos.toByteArray();
    }

    public byte[] generateDischargeSummaryPdf(
            Consultation consultation,
            List<PrescriptionItem> items,
            Patient patient,
            User doctor,
            Clinic clinic
    ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            document.setMargins(25, 30, 25, 30);

            // 1. Clinic Header
            String clinicName = clinic != null && clinic.getName() != null ? clinic.getName() : "PMS Healthcare Clinic";
            String clinicAddress = clinic != null && clinic.getAddressLine1() != null 
                    ? clinic.getAddressLine1() + ", " + (clinic.getCity() != null ? clinic.getCity() : "") 
                    : "Outpatient Clinic";
            String clinicContact = clinic != null && clinic.getPhone() != null ? "Phone: " + clinic.getPhone() : "";

            Paragraph clinicHeader = new Paragraph(clinicName)
                    .setFontSize(18)
                    .setBold()
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(2);

            Paragraph clinicSubHeader = new Paragraph(clinicAddress + (clinicContact.isEmpty() ? "" : " | " + clinicContact))
                    .setFontSize(8)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10);

            document.add(clinicHeader);
            document.add(clinicSubHeader);

            // 2. Document Title
            Paragraph docTitle = new Paragraph("OUTPATIENT CLINICAL SUMMARY & DISCHARGE")
                    .setFontSize(13)
                    .setBold()
                    .setFontColor(ColorConstants.BLUE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(12);
            document.add(docTitle);

            // 3. Patient & Doctor Info Table
            Table metaTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                    .useAllAvailableWidth()
                    .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                    .setMarginBottom(12);

            String pName = patient != null ? patient.getFullName() : "Unknown Patient";
            String pUid = patient != null ? patient.getPatientUid() : "N/A";
            String pAgeGender = (patient != null && patient.getAge() != null ? patient.getAge() + " yrs" : "") 
                    + (patient != null && patient.getGender() != null ? " / " + patient.getGender() : "");
            String pPhone = patient != null && patient.getPhoneNumber() != null ? patient.getPhoneNumber() : "N/A";
            String pBlood = patient != null && patient.getBloodGroup() != null ? patient.getBloodGroup() : "Unknown";

            String docName = doctor != null ? doctor.getFullName() : "Attending Physician";
            String docReg = doctor != null && doctor.getRegistrationNumber() != null ? "Reg No: " + doctor.getRegistrationNumber() : "";

            Cell patientCell = new Cell().setBorder(Border.NO_BORDER).setPadding(6);
            patientCell.add(new Paragraph("Patient: " + pName).setFontSize(10).setBold());
            patientCell.add(new Paragraph("Patient ID: " + pUid).setFontSize(9).setFontColor(ColorConstants.DARK_GRAY));
            patientCell.add(new Paragraph("Age/Gender: " + pAgeGender + " | Blood Group: " + pBlood).setFontSize(8).setFontColor(ColorConstants.GRAY));
            patientCell.add(new Paragraph("Contact: " + pPhone).setFontSize(8).setFontColor(ColorConstants.GRAY));
            metaTable.addCell(patientCell);

            Cell doctorCell = new Cell().setBorder(Border.NO_BORDER).setPadding(6).setTextAlignment(TextAlignment.RIGHT);
            doctorCell.add(new Paragraph("Dr. " + docName).setFontSize(10).setBold());
            if (!docReg.isEmpty()) {
                doctorCell.add(new Paragraph(docReg).setFontSize(8).setFontColor(ColorConstants.GRAY));
            }
            String dischargeDateStr = consultation.getDischargeDate() != null 
                    ? consultation.getDischargeDate().format(DATE_FORMATTER) 
                    : (consultation.getFinalizedAt() != null ? consultation.getFinalizedAt().format(DATE_FORMATTER) : "N/A");
            doctorCell.add(new Paragraph("Discharge Date: " + dischargeDateStr).setFontSize(9).setBold());
            metaTable.addCell(doctorCell);

            document.add(metaTable);

            // 4. Clinical Details
            Table clinicalTable = new Table(UnitValue.createPercentArray(new float[]{100}))
                    .useAllAvailableWidth()
                    .setMarginBottom(10);

            Cell clinicalCell = new Cell().setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)).setPadding(8);
            clinicalCell.add(new Paragraph("Chief Complaint:").setFontSize(9).setBold());
            clinicalCell.add(new Paragraph(consultation.getChiefComplaint() != null ? consultation.getChiefComplaint() : "None reported").setFontSize(9).setMarginBottom(6));

            clinicalCell.add(new Paragraph("Final / Provisional Diagnosis:").setFontSize(9).setBold());
            clinicalCell.add(new Paragraph(consultation.getDiagnosis() != null ? consultation.getDiagnosis() : "Clinically evaluated").setFontSize(9).setMarginBottom(6));

            String condition = consultation.getDischargeCondition() != null ? consultation.getDischargeCondition() : "Clinically Stable / Treatment Prescribed";
            clinicalCell.add(new Paragraph("Condition at Discharge:").setFontSize(9).setBold());
            clinicalCell.add(new Paragraph(condition).setFontSize(9).setFontColor(ColorConstants.DARK_GRAY).setMarginBottom(6));

            String summaryText = consultation.getDischargeSummary() != null && !consultation.getDischargeSummary().isEmpty()
                    ? consultation.getDischargeSummary()
                    : "Patient evaluated in outpatient clinic. Treatment prescribed with standard precautions and home care advice.";
            clinicalCell.add(new Paragraph("Discharge Summary & Care Instructions:").setFontSize(9).setBold());
            clinicalCell.add(new Paragraph(summaryText).setFontSize(9).setFontColor(ColorConstants.DARK_GRAY));

            clinicalTable.addCell(clinicalCell);
            document.add(clinicalTable);

            // 5. Prescribed Medications Table
            Paragraph rxHeading = new Paragraph("Discharge Medications & Regimen:")
                    .setFontSize(10)
                    .setBold()
                    .setMarginTop(8)
                    .setMarginBottom(4);
            document.add(rxHeading);

            Table rxTable = new Table(UnitValue.createPercentArray(new float[]{8, 37, 18, 15, 22}))
                    .useAllAvailableWidth()
                    .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f));

            String[] headers = {"#", "Medication", "Dosage", "Duration", "Instructions"};
            for (String h : headers) {
                rxTable.addHeaderCell(new Cell()
                        .add(new Paragraph(h).setFontSize(9).setBold())
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .setPadding(4));
            }

            if (items != null && !items.isEmpty()) {
                int idx = 1;
                for (PrescriptionItem item : items) {
                    String medName = item.getCustomMedicineName() != null ? item.getCustomMedicineName() : "Medicine #" + item.getMedicineId();
                    String dosage = item.getDosage() != null ? item.getDosage() : "1-0-1";
                    String duration = item.getDurationDays() != null ? item.getDurationDays() + " days" : "5 days";
                    String inst = item.getInstructions() != null ? item.getInstructions() : "After food";

                    rxTable.addCell(new Cell().add(new Paragraph(String.valueOf(idx++)).setFontSize(9)).setPadding(4));
                    rxTable.addCell(new Cell().add(new Paragraph(medName).setFontSize(9).setBold()).setPadding(4));
                    rxTable.addCell(new Cell().add(new Paragraph(dosage).setFontSize(9)).setPadding(4));
                    rxTable.addCell(new Cell().add(new Paragraph(duration).setFontSize(9)).setPadding(4));
                    rxTable.addCell(new Cell().add(new Paragraph(inst).setFontSize(9)).setPadding(4));
                }
            } else {
                Cell emptyCell = new Cell(1, 5)
                        .add(new Paragraph("No continuing medications required.").setFontSize(9).setItalic())
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(8);
                rxTable.addCell(emptyCell);
            }
            document.add(rxTable);

            // 6. Sign-off & Legal Disclaimer
            Table footerTable = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                    .useAllAvailableWidth()
                    .setMarginTop(25);

            Cell adviceCell = new Cell().setBorder(Border.NO_BORDER);
            adviceCell.add(new Paragraph("Emergency Red Flags:").setFontSize(9).setBold());
            adviceCell.add(new Paragraph("• Return immediately to the clinic or emergency room in case of high persistent fever, severe pain, breathlessness, or unusual reactions.\n• Maintain rest and hydration as advised.").setFontSize(8).setFontColor(ColorConstants.DARK_GRAY));
            footerTable.addCell(adviceCell);

            Cell sigCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
            sigCell.add(new Paragraph("\n\n__________________________").setFontSize(9));
            sigCell.add(new Paragraph("Dr. " + docName).setFontSize(9).setBold());
            sigCell.add(new Paragraph("Treating Physician").setFontSize(8).setFontColor(ColorConstants.GRAY));
            footerTable.addCell(sigCell);

            document.add(footerTable);
        }

        return baos.toByteArray();
    }
}

