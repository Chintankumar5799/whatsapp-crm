package com.appointment.crm.controller;

import com.appointment.crm.model.PatientInquiry;
import com.appointment.crm.service.PatientInquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/crm/inquiries")
@RequiredArgsConstructor
@Tag(name = "Patient Inquiry CRM", description = "Lead pipeline, external inquiries, and conversion to patients")
@CrossOrigin(origins = "http://localhost:3000")
public class InquiryController {

    private final PatientInquiryService inquiryService;

    @GetMapping
    @Operation(summary = "Get all inquiries for clinic with optional status filter")
    public ResponseEntity<List<PatientInquiry>> getInquiries(
            @RequestParam(defaultValue = "1") Long clinicId,
            @RequestParam(required = false) PatientInquiry.InquiryStatus status
    ) {
        return ResponseEntity.ok(inquiryService.getInquiries(clinicId, status));
    }

    @PostMapping
    @Operation(summary = "Register new inquiry (Phone, Google, WhatsApp, Website, Walk-in)")
    public ResponseEntity<PatientInquiry> createInquiry(@RequestBody PatientInquiry inquiry) {
        return ResponseEntity.ok(inquiryService.createInquiry(inquiry));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update inquiry pipeline status and next follow-up date")
    public ResponseEntity<PatientInquiry> updateStatus(
            @PathVariable Long id,
            @RequestParam PatientInquiry.InquiryStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nextFollowUp
    ) {
        return ResponseEntity.ok(inquiryService.updateStatus(id, status, nextFollowUp));
    }

    @PostMapping("/{id}/convert")
    @Operation(summary = "Convert inquiry to registered patient and optional confirmed appointment")
    public ResponseEntity<PatientInquiry> convertToPatientAndBooking(
            @PathVariable Long id,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appointmentDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time
    ) {
        return ResponseEntity.ok(inquiryService.convertToPatientAndBooking(id, doctorId, appointmentDate, time));
    }
}

