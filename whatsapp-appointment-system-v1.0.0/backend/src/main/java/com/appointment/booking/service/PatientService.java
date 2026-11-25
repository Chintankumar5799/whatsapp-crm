package com.appointment.booking.service;

import com.appointment.auth.model.User;
import com.appointment.auth.repository.UserRepository;
import com.appointment.booking.dto.Patient360Dto;
import com.appointment.booking.model.Booking;
import com.appointment.booking.model.Patient;
import com.appointment.booking.model.PatientDocument;
import com.appointment.booking.repository.BookingRepository;
import com.appointment.booking.repository.PatientDocumentRepository;
import com.appointment.booking.repository.PatientRepository;
import com.appointment.clinic.model.Clinic;
import com.appointment.clinic.repository.ClinicRepository;
import com.appointment.clinical.model.Consultation;
import com.appointment.clinical.model.FollowUp;
import com.appointment.clinical.model.OpdQueue;
import com.appointment.clinical.model.PrescriptionItem;
import com.appointment.clinical.repository.ConsultationRepository;
import com.appointment.clinical.repository.FollowUpRepository;
import com.appointment.clinical.repository.OpdQueueRepository;
import com.appointment.clinical.repository.PrescriptionItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final ClinicRepository clinicRepository;
    private final PatientDocumentRepository patientDocumentRepository;
    private final BookingRepository bookingRepository;
    private final ConsultationRepository consultationRepository;
    private final FollowUpRepository followUpRepository;
    private final OpdQueueRepository opdQueueRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final UserRepository userRepository;

    @Transactional
    public Patient registerPatient(Long clinicId, Patient patient) {
        if (clinicId == null) {
            clinicId = 1L;
        }
        patient.setClinicId(clinicId);

        // Generate unique patient UID if not set
        if (patient.getPatientUid() == null || patient.getPatientUid().trim().isEmpty()) {
            patient.setPatientUid(generatePatientUid(clinicId));
        }

        // Calculate age if dateOfBirth is given
        if (patient.getDateOfBirth() != null && patient.getAge() == null) {
            patient.setAge(LocalDate.now().getYear() - patient.getDateOfBirth().getYear());
        }

        Patient saved = patientRepository.save(patient);
        log.info("Registered new patient: UID={}, Name={}, ClinicId={}", saved.getPatientUid(), saved.getName(), clinicId);
        return saved;
    }

    public List<Patient> checkDuplicates(Long clinicId, String phone, String name) {
        return patientRepository.findByClinicIdAndPhone(clinicId, phone);
    }

    public List<Patient> searchPatients(Long clinicId, String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return patientRepository.searchPatients(clinicId, query.trim());
    }

    public Optional<Patient> getPatientById(Long id) {
        return patientRepository.findById(id);
    }

    @Transactional
    public void recordVisitCompletion(Long patientId) {
        patientRepository.findById(patientId).ifPresent(p -> {
            p.setLastVisitDate(LocalDateTime.now());
            p.setTotalVisitsCount(p.getTotalVisitsCount() + 1);
            patientRepository.save(p);
        });
    }

    public String generatePatientUid(Long clinicId) {
        Clinic clinic = clinicRepository.findById(clinicId).orElse(null);
        String prefix = (clinic != null && clinic.getCode() != null) ? clinic.getCode() : "PMS";
        int year = LocalDate.now().getYear();
        long count = patientRepository.countByClinicId(clinicId) + 1;
        return String.format("%s-%d-%05d", prefix, year, count);
    }

    // --- Document Attachments ---

    @Transactional
    public PatientDocument uploadDocument(
            Long clinicId,
            Long patientId,
            String documentType,
            String fileName,
            String fileType,
            byte[] fileData,
            String notes,
            Long uploadedByUserId
    ) {
        PatientDocument doc = PatientDocument.builder()
                .clinicId(clinicId != null ? clinicId : 1L)
                .patientId(patientId)
                .documentType(documentType != null ? documentType : "LAB_REPORT")
                .fileName(fileName)
                .fileType(fileType)
                .fileSize(fileData != null ? (long) fileData.length : 0L)
                .fileData(fileData)
                .notes(notes)
                .uploadedByUserId(uploadedByUserId)
                .build();

        log.info("Uploaded document {} for patient id {}", fileName, patientId);
        return patientDocumentRepository.save(doc);
    }

    public List<PatientDocument> getPatientDocuments(Long patientId) {
        return patientDocumentRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    public Optional<PatientDocument> getDocumentById(Long docId) {
        return patientDocumentRepository.findById(docId);
    }

    // --- Controlled Patient Profile Merging ---

    @Transactional
    public Patient mergePatientProfiles(Long primaryPatientId, Long duplicatePatientId, Long clinicId) {
        if (primaryPatientId.equals(duplicatePatientId)) {
            throw new IllegalArgumentException("Cannot merge a patient into themselves.");
        }

        Patient primary = patientRepository.findById(primaryPatientId)
                .orElseThrow(() -> new IllegalArgumentException("Primary patient not found: " + primaryPatientId));
        Patient duplicate = patientRepository.findById(duplicatePatientId)
                .orElseThrow(() -> new IllegalArgumentException("Duplicate patient not found: " + duplicatePatientId));

        if (!primary.getClinicId().equals(duplicate.getClinicId())) {
            throw new IllegalArgumentException("Cannot merge patients across different clinics.");
        }

        log.info("Merging patient UID {} (ID {}) into primary UID {} (ID {})", 
                duplicate.getPatientUid(), duplicate.getId(), primary.getPatientUid(), primary.getId());

        // 1. Move all Bookings
        List<Booking> bookings = bookingRepository.findByPatientId(duplicatePatientId);
        for (Booking b : bookings) {
            b.setPatientId(primaryPatientId);
            bookingRepository.save(b);
        }

        // 2. Move all Consultations
        List<Consultation> consultations = consultationRepository.findByPatientIdOrderByFinalizedAtDesc(duplicatePatientId);
        for (Consultation c : consultations) {
            c.setPatientId(primaryPatientId);
            consultationRepository.save(c);
        }

        // 3. Move all Follow-ups
        List<FollowUp> followUps = followUpRepository.findByPatientId(duplicatePatientId);
        for (FollowUp f : followUps) {
            f.setPatientId(primaryPatientId);
            followUpRepository.save(f);
        }

        // 4. Move all OPD queue items
        List<OpdQueue> queueItems = opdQueueRepository.findByPatientId(duplicatePatientId);
        for (OpdQueue q : queueItems) {
            q.setPatientId(primaryPatientId);
            opdQueueRepository.save(q);
        }

        // 5. Move all Documents
        List<PatientDocument> documents = patientDocumentRepository.findByPatientIdOrderByCreatedAtDesc(duplicatePatientId);
        for (PatientDocument d : documents) {
            d.setPatientId(primaryPatientId);
            patientDocumentRepository.save(d);
        }

        // 6. Aggregate visit count
        int dupVisits = duplicate.getTotalVisitsCount() != null ? duplicate.getTotalVisitsCount() : 0;
        primary.setTotalVisitsCount((primary.getTotalVisitsCount() != null ? primary.getTotalVisitsCount() : 0) + dupVisits);

        // Update latest visit date if duplicate's visit was more recent
        if (duplicate.getLastVisitDate() != null) {
            if (primary.getLastVisitDate() == null || duplicate.getLastVisitDate().isAfter(primary.getLastVisitDate())) {
                primary.setLastVisitDate(duplicate.getLastVisitDate());
            }
        }

        // 7. Mark duplicate as merged
        duplicate.setIsMerged(true);
        duplicate.setMergedIntoPatientId(primaryPatientId);
        patientRepository.save(duplicate);

        Patient updatedPrimary = patientRepository.save(primary);
        log.info("Patient merge complete. Primary total visits: {}", updatedPrimary.getTotalVisitsCount());
        return updatedPrimary;
    }

    @Transactional(readOnly = true)
    public Patient360Dto getPatient360View(Long patientId) {
        return getPatient360View(patientId, true);
    }

    @Transactional(readOnly = true)
    public Patient360Dto getPatient360View(Long patientId, boolean includeSensitiveClinical) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));

        // 1. Fetch consultations
        List<Consultation> consultations = consultationRepository.findByPatientIdOrderByFinalizedAtDesc(patientId);
        List<Patient360Dto.ConsultationDetailDto> consultDtos = new ArrayList<>();
        List<Patient360Dto.VitalsRecordDto> vitalsList = new ArrayList<>();
        BigDecimal lifetimeRevenue = BigDecimal.ZERO;

        for (Consultation c : consultations) {
            String docName = userRepository.findById(c.getDoctorId())
                    .map(User::getFullName)
                    .orElse("Dr. " + c.getDoctorId());

            if (c.getConsultationFee() != null) {
                lifetimeRevenue = lifetimeRevenue.add(c.getConsultationFee());
            }

            // Prescription items
            List<PrescriptionItem> items = prescriptionItemRepository.findByConsultationIdOrderByIdAsc(c.getId());
            List<Patient360Dto.RxItemDto> rxDtos = items.stream().map(it -> Patient360Dto.RxItemDto.builder()
                    .medicineName(it.getCustomMedicineName() != null ? it.getCustomMedicineName() : "Medicine #" + it.getMedicineId())
                    .dosage(it.getDosage())
                    .frequency(it.getFrequency())
                    .durationDays(it.getDurationDays())
                    .instructions(it.getInstructions())
                    .build()).collect(Collectors.toList());

            consultDtos.add(Patient360Dto.ConsultationDetailDto.builder()
                    .id(c.getId())
                    .bookingId(c.getBookingId())
                    .doctorId(c.getDoctorId())
                    .doctorName(docName)
                    .caseType(c.getCaseType() != null ? c.getCaseType().name() : "NEW_CASE")
                    .consultationFee(c.getConsultationFee())
                    .chiefComplaint(c.getChiefComplaint())
                    .clinicalExamination(includeSensitiveClinical ? c.getClinicalExamination() : null)
                    .diagnosis(includeSensitiveClinical ? c.getDiagnosis() : null)
                    .doctorNotes(includeSensitiveClinical ? c.getDoctorPrivateNotes() : null)
                    .finalizedAt(c.getFinalizedAt() != null ? c.getFinalizedAt() : c.getCreatedAt())
                    .bpSystolic(c.getBpSystolic())
                    .bpDiastolic(c.getBpDiastolic())
                    .pulse(c.getPulse())
                    .temperature(c.getTemperature())
                    .weight(c.getWeight())
                    .spo2(c.getSpo2())
                    .pdfUrl("/api/clinical/consultations/" + c.getId() + "/pdf")
                    .dischargePdfUrl("/api/clinical/consultations/" + c.getId() + "/discharge-pdf")
                    .prescriptionItemsCount(items.size())
                    .prescriptionItems(rxDtos)
                    .build());

            // Vitals record
            if (c.getBpSystolic() != null || c.getPulse() != null || c.getWeight() != null) {
                String bpStr = (c.getBpSystolic() != null && c.getBpDiastolic() != null) 
                        ? (c.getBpSystolic() + "/" + c.getBpDiastolic()) : null;
                vitalsList.add(Patient360Dto.VitalsRecordDto.builder()
                        .recordedAt(c.getFinalizedAt() != null ? c.getFinalizedAt() : c.getCreatedAt())
                        .bp(bpStr)
                        .bpSystolic(c.getBpSystolic())
                        .bpDiastolic(c.getBpDiastolic())
                        .pulse(c.getPulse())
                        .temperature(c.getTemperature())
                        .weight(c.getWeight())
                        .spo2(c.getSpo2())
                        .build());
            }
        }

        // Sort vitals chronologically
        vitalsList.sort(Comparator.comparing(Patient360Dto.VitalsRecordDto::getRecordedAt));

        // 2. Fetch appointments
        List<Booking> bookings = bookingRepository.findByPatientId(patientId);
        List<Patient360Dto.AppointmentHistoryDto> apptDtos = bookings.stream().map(b -> {
            String docName = userRepository.findById(b.getDoctorId())
                    .map(User::getFullName)
                    .orElse("Doctor #" + b.getDoctorId());
            return Patient360Dto.AppointmentHistoryDto.builder()
                    .id(b.getId())
                    .bookingNumber(b.getBookingNumber())
                    .bookingDate(b.getBookingDate())
                    .startTime(b.getStartTime() != null ? b.getStartTime().toString() : "")
                    .doctorName(docName)
                    .status(b.getStatus() != null ? b.getStatus().name() : "")
                    .amount(b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO)
                    .paymentStatus(b.getPaymentStatus() != null ? b.getPaymentStatus() : "PENDING")
                    .appointmentSource(b.getAppointmentSource() != null ? b.getAppointmentSource() : "ONLINE")
                    .build();
        }).collect(Collectors.toList());

        // 3. Fetch documents
        List<PatientDocument> documents = patientDocumentRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        List<Patient360Dto.DocumentMetaDto> docDtos = documents.stream().map(d -> Patient360Dto.DocumentMetaDto.builder()
                .id(d.getId())
                .documentType(d.getDocumentType() != null ? d.getDocumentType() : "OTHER")
                .fileName(d.getFileName())
                .fileType(d.getFileType())
                .fileSize(d.getFileSize())
                .notes(d.getNotes())
                .uploadedAt(d.getCreatedAt())
                .downloadUrl("/api/patients/documents/" + d.getId() + "/download")
                .build()).collect(Collectors.toList());

        // 4. Fetch follow-ups
        List<FollowUp> followUps = followUpRepository.findByPatientId(patientId);
        List<Patient360Dto.FollowUpHistoryDto> followUpDtos = followUps.stream().map(f -> {
            String docName = userRepository.findById(f.getDoctorId())
                    .map(User::getFullName)
                    .orElse("Doctor #" + f.getDoctorId());
            return Patient360Dto.FollowUpHistoryDto.builder()
                    .id(f.getId())
                    .dueDate(f.getDueDate())
                    .doctorName(docName)
                    .reason(f.getReason())
                    .status(f.getStatus() != null ? f.getStatus().name() : "PENDING")
                    .contactStatus(f.getContactStatus() != null ? f.getContactStatus().name() : "PENDING")
                    .build();
        }).collect(Collectors.toList());

        return Patient360Dto.builder()
                .patient(patient)
                .totalLifetimeVisits(patient.getTotalVisitsCount() != null ? patient.getTotalVisitsCount() : consultDtos.size())
                .totalLifetimeRevenue(lifetimeRevenue)
                .consultations(consultDtos)
                .vitalsHistory(vitalsList)
                .appointments(apptDtos)
                .documents(docDtos)
                .followUps(followUpDtos)
                .build();
    }
}


