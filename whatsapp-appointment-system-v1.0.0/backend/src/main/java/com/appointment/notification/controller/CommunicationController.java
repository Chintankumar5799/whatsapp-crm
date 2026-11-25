package com.appointment.notification.controller;

import com.appointment.notification.model.CommunicationLog;
import com.appointment.notification.model.PatientConsent;
import com.appointment.notification.repository.CommunicationLogRepository;
import com.appointment.notification.repository.PatientConsentRepository;
import com.appointment.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Communication & Privacy", description = "Message delivery audit trail and patient privacy consent management")
@CrossOrigin(origins = "http://localhost:3000")
public class CommunicationController {

    private final CommunicationLogRepository communicationLogRepository;
    private final PatientConsentRepository consentRepository;
    private final NotificationService notificationService;

    @GetMapping("/logs")
    @Operation(summary = "Get communication delivery logs for clinic audit")
    public ResponseEntity<List<CommunicationLog>> getLogs(
            @RequestParam(defaultValue = "1") Long clinicId
    ) {
        return ResponseEntity.ok(communicationLogRepository.findByClinicIdOrderBySentAtDesc(clinicId));
    }

    @GetMapping("/consents/patient/{patientId}")
    @Operation(summary = "Get privacy consents granted/withdrawn by patient")
    public ResponseEntity<List<PatientConsent>> getPatientConsents(@PathVariable Long patientId) {
        return ResponseEntity.ok(consentRepository.findByPatientId(patientId));
    }

    @PostMapping("/consents/update")
    @Operation(summary = "Update patient consent for reminders, automated calls, or promotional outreach")
    public ResponseEntity<PatientConsent> updateConsent(
            @RequestParam(defaultValue = "1") Long clinicId,
            @RequestParam Long patientId,
            @RequestParam PatientConsent.ConsentType consentType,
            @RequestParam boolean isGranted,
            @RequestParam(defaultValue = "IN_PERSON_REGISTRATION") String consentMethod
    ) {
        PatientConsent consent = consentRepository.findByPatientIdAndConsentType(patientId, consentType)
                .orElse(PatientConsent.builder()
                        .clinicId(clinicId)
                        .patientId(patientId)
                        .consentType(consentType)
                        .consentMethod(consentMethod)
                        .grantedAt(LocalDateTime.now())
                        .build());

        consent.setIsGranted(isGranted);
        if (!isGranted) {
            consent.setWithdrawnAt(LocalDateTime.now());
        } else {
            consent.setWithdrawnAt(null);
        }

        return ResponseEntity.ok(consentRepository.save(consent));
    }

    @PostMapping("/trigger-reminders")
    @Operation(summary = "Manual trigger for appointment and revisit reminders")
    public ResponseEntity<String> triggerReminders(@RequestParam String type) {
        if ("24h".equalsIgnoreCase(type)) {
            notificationService.send24HourReminders();
            return ResponseEntity.ok("24h reminders dispatched successfully.");
        } else if ("1h".equalsIgnoreCase(type)) {
            notificationService.send1HourReminders();
            return ResponseEntity.ok("1h reminders dispatched successfully.");
        } else if ("revisit".equalsIgnoreCase(type)) {
            notificationService.sendRevisitReminders();
            return ResponseEntity.ok("Revisit reminders dispatched successfully.");
        }
        return ResponseEntity.badRequest().body("Invalid reminder type: use 24h, 1h, or revisit");
    }
}

