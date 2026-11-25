package com.appointment.clinical.service;

import com.appointment.auth.model.User;
import com.appointment.auth.repository.UserRepository;
import com.appointment.booking.model.Patient;
import com.appointment.booking.repository.PatientRepository;
import com.appointment.booking.service.PatientService;
import com.appointment.clinic.model.Clinic;
import com.appointment.clinic.repository.ClinicRepository;
import com.appointment.clinical.model.Consultation;
import com.appointment.clinical.model.FollowUp;
import com.appointment.clinical.model.PrescriptionItem;
import com.appointment.clinical.repository.ConsultationRepository;
import com.appointment.clinical.repository.FollowUpRepository;
import com.appointment.clinical.repository.PrescriptionItemRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final FollowUpRepository followUpRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final ClinicRepository clinicRepository;
    private final PatientService patientService;

    @Data
    @Builder
    public static class FeeAssessment {
        private Consultation.CaseType caseType;
        private BigDecimal consultationFee;
        private Integer daysSinceLastVisit;
        private Integer validityDays;
        private Boolean isWithinValidity;
    }

    /**
     * Determines whether a patient is a NEW_CASE or OLD_CASE and computes the appropriate fee.
     */
    public FeeAssessment assessCaseAndFee(Long clinicId, Long doctorId, Long patientId) {
        Clinic clinic = clinicRepository.findById(clinicId).orElse(null);
        int validityDays = (clinic != null && clinic.getFollowUpValidityDays() != null) 
                ? clinic.getFollowUpValidityDays() : 30;

        User doctor = userRepository.findById(doctorId).orElse(null);
        BigDecimal newFee = (doctor != null && doctor.getConsultationFee() != null) 
                ? doctor.getConsultationFee() 
                : (clinic != null && clinic.getNewCaseConsultationFee() != null ? clinic.getNewCaseConsultationFee() : BigDecimal.valueOf(500));
        
        BigDecimal oldFee = (doctor != null && doctor.getOldCaseFee() != null) 
                ? doctor.getOldCaseFee() 
                : (clinic != null && clinic.getOldCaseConsultationFee() != null ? clinic.getOldCaseConsultationFee() : BigDecimal.valueOf(300));

        Optional<Consultation> latestConsultation = consultationRepository.findLatestCompletedByPatientId(patientId);

        if (latestConsultation.isEmpty() || latestConsultation.get().getFinalizedAt() == null) {
            return FeeAssessment.builder()
                    .caseType(Consultation.CaseType.NEW_CASE)
                    .consultationFee(newFee)
                    .daysSinceLastVisit(null)
                    .validityDays(validityDays)
                    .isWithinValidity(false)
                    .build();
        }

        LocalDateTime lastVisit = latestConsultation.get().getFinalizedAt();
        long daysDiff = ChronoUnit.DAYS.between(lastVisit.toLocalDate(), LocalDate.now());

        if (daysDiff <= validityDays) {
            return FeeAssessment.builder()
                    .caseType(Consultation.CaseType.OLD_CASE)
                    .consultationFee(oldFee)
                    .daysSinceLastVisit((int) daysDiff)
                    .validityDays(validityDays)
                    .isWithinValidity(true)
                    .build();
        } else {
            return FeeAssessment.builder()
                    .caseType(Consultation.CaseType.NEW_CASE)
                    .consultationFee(newFee)
                    .daysSinceLastVisit((int) daysDiff)
                    .validityDays(validityDays)
                    .isWithinValidity(false)
                    .build();
        }
    }

    @Transactional
    public Consultation finalizeConsultation(
            Consultation consultation, 
            List<PrescriptionItem> prescriptionItems,
            LocalDate followUpDate,
            String followUpInterval,
            String followUpReason
    ) {
        consultation.setIsFinalized(true);
        consultation.setFinalizedAt(LocalDateTime.now());
        Consultation saved = consultationRepository.save(consultation);

        // Save prescription items
        if (prescriptionItems != null && !prescriptionItems.isEmpty()) {
            for (PrescriptionItem item : prescriptionItems) {
                item.setConsultationId(saved.getId());
                prescriptionItemRepository.save(item);
            }
        }

        // Create follow-up record if requested
        if (followUpDate != null) {
            FollowUp followUp = FollowUp.builder()
                    .clinicId(saved.getClinicId())
                    .consultationId(saved.getId())
                    .patientId(saved.getPatientId())
                    .doctorId(saved.getDoctorId())
                    .dueDate(followUpDate)
                    .intervalPreset(followUpInterval != null ? followUpInterval : "7D")
                    .reason(followUpReason)
                    .status(FollowUp.FollowUpStatus.PENDING)
                    .contactStatus(FollowUp.ContactStatus.NOT_CONTACTED)
                    .build();
            followUpRepository.save(followUp);
            log.info("Created follow-up for patient {} due on {}", saved.getPatientId(), followUpDate);
        }

        // Update patient visit record
        patientService.recordVisitCompletion(saved.getPatientId());

        return saved;
    }

    public List<Consultation> getPatientHistory(Long patientId) {
        return consultationRepository.findByPatientIdOrderByFinalizedAtDesc(patientId);
    }

    public List<PrescriptionItem> getPrescriptionItems(Long consultationId) {
        return prescriptionItemRepository.findByConsultationIdOrderByIdAsc(consultationId);
    }
}

