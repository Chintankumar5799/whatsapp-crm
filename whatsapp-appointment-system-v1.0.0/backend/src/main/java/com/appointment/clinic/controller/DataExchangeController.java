package com.appointment.clinic.controller;

import com.appointment.clinic.service.DataExchangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/data")
@RequiredArgsConstructor
@Tag(name = "Data Exchange & Migration", description = "Bulk CSV patient import and operational data exports")
@CrossOrigin(origins = "http://localhost:3000")
public class DataExchangeController {

    private final DataExchangeService dataExchangeService;

    @PostMapping(value = "/patients/import-csv", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @Operation(summary = "Bulk import patient records from CSV file")
    public ResponseEntity<DataExchangeService.ImportResult> importPatientsCsv(
            @RequestParam(defaultValue = "1") Long clinicId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok(dataExchangeService.importPatientsFromCsv(clinicId, content));
    }

    @PostMapping(value = "/patients/import-text")
    @Operation(summary = "Bulk import patient records from raw CSV text")
    public ResponseEntity<DataExchangeService.ImportResult> importPatientsRaw(
            @RequestParam(defaultValue = "1") Long clinicId,
            @RequestBody Map<String, String> body
    ) throws IOException {
        String content = body.getOrDefault("csvData", "");
        return ResponseEntity.ok(dataExchangeService.importPatientsFromCsv(clinicId, content));
    }

    @GetMapping(value = "/patients/export-csv", produces = "text/csv")
    @Operation(summary = "Export all clinic patients to CSV")
    public ResponseEntity<byte[]> exportPatientsCsv(@RequestParam(defaultValue = "1") Long clinicId) {
        String csv = dataExchangeService.exportPatientsCsv(clinicId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "patients-clinic-" + clinicId + "-" + LocalDate.now() + ".csv");
        return ResponseEntity.ok().headers(headers).body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping(value = "/appointments/export-csv", produces = "text/csv")
    @Operation(summary = "Export clinic appointments & attendance ledger to CSV")
    public ResponseEntity<byte[]> exportAppointmentsCsv(@RequestParam(defaultValue = "1") Long clinicId) {
        String csv = dataExchangeService.exportAppointmentsCsv(clinicId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "appointments-clinic-" + clinicId + "-" + LocalDate.now() + ".csv");
        return ResponseEntity.ok().headers(headers).body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping(value = "/consultations/export-csv", produces = "text/csv")
    @Operation(summary = "Export clinical consultations & diagnosis ledger to CSV")
    public ResponseEntity<byte[]> exportConsultationsCsv(@RequestParam(defaultValue = "1") Long clinicId) {
        String csv = dataExchangeService.exportConsultationsCsv(clinicId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "consultations-clinic-" + clinicId + "-" + LocalDate.now() + ".csv");
        return ResponseEntity.ok().headers(headers).body(csv.getBytes(StandardCharsets.UTF_8));
    }
}

