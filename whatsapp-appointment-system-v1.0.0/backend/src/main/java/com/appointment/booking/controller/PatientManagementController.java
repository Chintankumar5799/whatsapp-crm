package com.appointment.booking.controller;

import com.appointment.booking.dto.Patient360Dto;
import com.appointment.booking.model.Patient;
import com.appointment.booking.model.PatientDocument;
import com.appointment.booking.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
@Tag(name = "Patient Management", description = "Receptionist patient registration, lookup, duplicate check")
@CrossOrigin(origins = "http://localhost:3000")
public class PatientManagementController {

    private final PatientService patientService;

    @PostMapping("/register")
    @Operation(summary = "Register a new clinic patient")
    public ResponseEntity<Patient> registerPatient(
            @RequestParam(defaultValue = "1") Long clinicId,
            @RequestBody Patient patient
    ) {
        Patient registered = patientService.registerPatient(clinicId, patient);
        return ResponseEntity.ok(registered);
    }

    @GetMapping("/search")
    @Operation(summary = "Search patients by mobile number, name, or UID")
    public ResponseEntity<List<Patient>> searchPatients(
            @RequestParam(defaultValue = "1") Long clinicId,
            @RequestParam String query
    ) {
        return ResponseEntity.ok(patientService.searchPatients(clinicId, query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient by ID")
    public ResponseEntity<Patient> getPatientById(@PathVariable Long id) {
        return patientService.getPatientById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/check-duplicates")
    @Operation(summary = "Check potential duplicate patients by phone")
    public ResponseEntity<List<Patient>> checkDuplicates(
            @RequestParam(defaultValue = "1") Long clinicId,
            @RequestParam String phone,
            @RequestParam(required = false) String name
    ) {
        return ResponseEntity.ok(patientService.checkDuplicates(clinicId, phone, name));
    }

    // --- Document Attachment Endpoints ---

    @PostMapping(value = "/{id}/documents", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @Operation(summary = "Upload medical attachment for patient (Lab report, scan, ID)")
    public ResponseEntity<PatientDocument> uploadDocument(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Long clinicId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "LAB_REPORT") String documentType,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) Long uploadedByUserId
    ) throws IOException {
        PatientDocument doc = patientService.uploadDocument(
                clinicId,
                id,
                documentType,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes(),
                notes,
                uploadedByUserId
        );
        return ResponseEntity.ok(doc);
    }

    @GetMapping("/{id}/documents")
    @Operation(summary = "List all documents attached to patient profile")
    public ResponseEntity<List<PatientDocument>> getPatientDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.getPatientDocuments(id));
    }

    @GetMapping("/documents/{docId}/download")
    @Operation(summary = "Download / view attached patient document file")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long docId) {
        PatientDocument doc = patientService.getDocumentById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + docId));

        HttpHeaders headers = new HttpHeaders();
        if (doc.getFileType() != null) {
            headers.setContentType(MediaType.parseMediaType(doc.getFileType()));
        } else {
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }
        headers.setContentDispositionFormData("inline", doc.getFileName());

        return ResponseEntity.ok()
                .headers(headers)
                .body(doc.getFileData());
    }

    // --- Controlled Patient Merge Endpoint ---

    @PostMapping("/{primaryId}/merge/{duplicateId}")
    @Operation(summary = "Merge duplicate patient profile into primary master patient")
    public ResponseEntity<Patient> mergePatients(
            @PathVariable Long primaryId,
            @PathVariable Long duplicateId,
            @RequestParam(defaultValue = "1") Long clinicId
    ) {
        Patient merged = patientService.mergePatientProfiles(primaryId, duplicateId, clinicId);
        return ResponseEntity.ok(merged);
    }

    @GetMapping("/{id}/360-view")
    @Operation(summary = "Get full 360-degree patient profile (visits, vitals trends, documents, appointments)")
    public ResponseEntity<Patient360Dto> getPatient360View(
            @PathVariable Long id,
            org.springframework.security.core.Authentication authentication
    ) {
        boolean isClinicalAuthorized = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        return ResponseEntity.ok(patientService.getPatient360View(id, isClinicalAuthorized));
    }
}

