package com.appointment.clinical.controller;

import com.appointment.auth.model.User;
import com.appointment.auth.repository.UserRepository;
import com.appointment.booking.model.Patient;
import com.appointment.booking.repository.PatientRepository;
import com.appointment.clinic.model.Clinic;
import com.appointment.clinic.repository.ClinicRepository;
import com.appointment.clinical.dto.FinalizeConsultationRequest;
import com.appointment.clinical.model.Consultation;
import com.appointment.clinical.model.Medicine;
import com.appointment.clinical.model.OpdQueue;
import com.appointment.clinical.model.PrescriptionItem;
import com.appointment.clinical.repository.ConsultationRepository;
import com.appointment.clinical.repository.MedicineRepository;
import com.appointment.clinical.repository.PrescriptionItemRepository;
import com.appointment.clinical.service.ConsultationService;
import com.appointment.clinical.service.OpdQueueService;
import com.appointment.clinical.service.PdfPrescriptionGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/clinical")
@RequiredArgsConstructor
@Tag(name = "Clinical Management", description = "Consultations, Prescriptions, OPD Queue and Fee Assessment")
@CrossOrigin(origins = "http://localhost:3000")
public class ClinicalController {

    private final ConsultationService consultationService;
    private final OpdQueueService opdQueueService;
    private final MedicineRepository medicineRepository;
    private final ConsultationRepository consultationRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final ClinicRepository clinicRepository;
    private final PdfPrescriptionGenerator pdfPrescriptionGenerator;

    @GetMapping("/assess-fee")
    @Operation(summary = "Assess case type and consultation fee based on return validity")
    public ResponseEntity<ConsultationService.FeeAssessment> assessFee(
            @RequestParam(defaultValue = "1") Long clinicId,
            @RequestParam Long doctorId,
            @RequestParam Long patientId
    ) {
        ConsultationService.FeeAssessment assessment = consultationService.assessCaseAndFee(clinicId, doctorId, patientId);
        return ResponseEntity.ok(assessment);
    }

    @PostMapping("/consultations/finalize")
    @Operation(summary = "Finalize consultation, save vitals, diagnosis, prescriptions and follow-up")
    public ResponseEntity<Consultation> finalizeConsultation(@RequestBody FinalizeConsultationRequest req) {
        Consultation consultation = Consultation.builder()
                .clinicId(req.getClinicId() != null ? req.getClinicId() : 1L)
                .bookingId(req.getBookingId())
                .patientId(req.getPatientId())
                .doctorId(req.getDoctorId())
                .caseType(req.getCaseType() != null ? req.getCaseType() : Consultation.CaseType.NEW_CASE)
                .consultationFee(req.getConsultationFee())
                .feeOverride(Boolean.TRUE.equals(req.getFeeOverride()))
                .feeOverrideReason(req.getFeeOverrideReason())
                .chiefComplaint(req.getChiefComplaint())
                .bpSystolic(req.getBpSystolic())
                .bpDiastolic(req.getBpDiastolic())
                .pulse(req.getPulse())
                .temperature(req.getTemperature())
                .weight(req.getWeight())
                .height(req.getHeight())
                .spo2(req.getSpo2())
                .clinicalExamination(req.getClinicalExamination())
                .diagnosis(req.getDiagnosis())
                .doctorPrivateNotes(req.getDoctorPrivateNotes())
                .build();

        Consultation saved = consultationService.finalizeConsultation(
                consultation,
                req.getPrescriptionItems(),
                req.getFollowUpDate(),
                req.getFollowUpInterval(),
                req.getFollowUpReason()
        );

        // If queue entry exists, mark it completed
        if (req.getQueueId() != null) {
            try {
                opdQueueService.markConsultationCompleted(req.getQueueId());
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/consultations/patient/{patientId}")
    @Operation(summary = "Get previous clinical history for patient")
    public ResponseEntity<List<Consultation>> getPatientHistory(
            @PathVariable Long patientId,
            org.springframework.security.core.Authentication authentication
    ) {
        boolean isDoctor = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        List<Consultation> history = consultationService.getPatientHistory(patientId);
        if (!isDoctor) {
            history.forEach(c -> {
                c.setDoctorPrivateNotes(null);
                c.setClinicalExamination(null);
                c.setDiagnosis(null);
            });
        }
        return ResponseEntity.ok(history);
    }

    @GetMapping("/consultations/{id}/prescriptions")
    @Operation(summary = "Get prescription items for consultation")
    public ResponseEntity<List<PrescriptionItem>> getPrescriptionItems(@PathVariable Long id) {
        return ResponseEntity.ok(consultationService.getPrescriptionItems(id));
    }

    @GetMapping(value = "/consultations/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Generate and stream printable Prescription PDF")
    public ResponseEntity<byte[]> getPrescriptionPdf(@PathVariable Long id) {
        Consultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Consultation not found: " + id));

        List<PrescriptionItem> items = prescriptionItemRepository.findByConsultationIdOrderByIdAsc(id);
        Patient patient = patientRepository.findById(consultation.getPatientId()).orElse(null);
        User doctor = userRepository.findById(consultation.getDoctorId()).orElse(null);
        Clinic clinic = clinicRepository.findById(consultation.getClinicId()).orElse(null);

        try {
            byte[] pdfBytes = pdfPrescriptionGenerator.generatePrescriptionPdf(consultation, items, patient, doctor, clinic);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "prescription-" + id + ".pdf");
            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/consultations/{id}/discharge")
    @Operation(summary = "Record discharge summary notes and condition")
    public ResponseEntity<Consultation> updateDischargeSummary(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> payload
    ) {
        Consultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Consultation not found: " + id));

        consultation.setDischargeCondition(payload.getOrDefault("dischargeCondition", "Clinically Stable"));
        consultation.setDischargeSummary(payload.getOrDefault("dischargeSummary", "Outpatient care completed. Prescribed medications advised."));
        consultation.setDischargeDate(java.time.LocalDateTime.now());

        return ResponseEntity.ok(consultationRepository.save(consultation));
    }

    @GetMapping(value = "/consultations/{id}/discharge-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Generate and stream printable Discharge Summary PDF")
    public ResponseEntity<byte[]> getDischargeSummaryPdf(@PathVariable Long id) {
        Consultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Consultation not found: " + id));

        List<PrescriptionItem> items = prescriptionItemRepository.findByConsultationIdOrderByIdAsc(id);
        Patient patient = patientRepository.findById(consultation.getPatientId()).orElse(null);
        User doctor = userRepository.findById(consultation.getDoctorId()).orElse(null);
        Clinic clinic = clinicRepository.findById(consultation.getClinicId()).orElse(null);

        try {
            byte[] pdfBytes = pdfPrescriptionGenerator.generateDischargeSummaryPdf(consultation, items, patient, doctor, clinic);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "discharge-summary-" + id + ".pdf");
            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // --- OPD Queue APIs ---

    @PostMapping("/queue/checkin")
    @Operation(summary = "Check in patient into today's OPD queue")
    public ResponseEntity<OpdQueue> checkIn(
            @RequestParam(defaultValue = "1") Long clinicId,
            @RequestParam Long doctorId,
            @RequestParam Long patientId,
            @RequestParam(required = false) Long bookingId,
            @RequestParam(defaultValue = "false") boolean priority
    ) {
        OpdQueue queue = opdQueueService.checkInPatient(clinicId, doctorId, patientId, bookingId, priority);
        return ResponseEntity.ok(queue);
    }

    @GetMapping("/queue/doctor/{doctorId}")
    @Operation(summary = "Get today's queue for a doctor")
    public ResponseEntity<List<OpdQueue>> getDoctorQueue(@PathVariable Long doctorId) {
        return ResponseEntity.ok(opdQueueService.getTodayDoctorQueue(doctorId));
    }

    @PostMapping("/queue/{queueId}/call")
    @Operation(summary = "Doctor calls patient into consultation")
    public ResponseEntity<OpdQueue> callPatient(@PathVariable Long queueId) {
        return ResponseEntity.ok(opdQueueService.callPatientToConsultation(queueId));
    }

    @PostMapping("/queue/{queueId}/complete")
    @Operation(summary = "Mark queue patient consultation completed")
    public ResponseEntity<OpdQueue> completeQueue(@PathVariable Long queueId) {
        return ResponseEntity.ok(opdQueueService.markConsultationCompleted(queueId));
    }

    @PostMapping("/queue/{queueId}/no-show")
    @Operation(summary = "Mark patient as no-show")
    public ResponseEntity<OpdQueue> markNoShow(@PathVariable Long queueId) {
        return ResponseEntity.ok(opdQueueService.markNoShow(queueId));
    }

    // --- Medicine Catalogue APIs ---

    @GetMapping("/medicines/search")
    @Operation(summary = "Search clinic medicine catalogue")
    public ResponseEntity<List<Medicine>> searchMedicines(
            @RequestParam(defaultValue = "1") Long clinicId,
            @RequestParam String q
    ) {
        return ResponseEntity.ok(medicineRepository.searchMedicines(clinicId, q));
    }

    @PostMapping("/medicines")
    @Operation(summary = "Add custom medicine to clinic catalogue")
    public ResponseEntity<Medicine> addMedicine(@RequestBody Medicine medicine) {
        if (medicine.getClinicId() == null) {
            medicine.setClinicId(1L);
        }
        medicine.setIsCustom(true);
        return ResponseEntity.ok(medicineRepository.save(medicine));
    }
}

