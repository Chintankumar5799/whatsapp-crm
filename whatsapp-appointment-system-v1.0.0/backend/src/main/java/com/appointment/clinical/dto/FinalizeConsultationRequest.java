package com.appointment.clinical.dto;

import com.appointment.clinical.model.Consultation;
import com.appointment.clinical.model.PrescriptionItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalizeConsultationRequest {
    private Long clinicId;
    private Long bookingId;
    private Long patientId;
    private Long doctorId;
    private Long queueId;
    private Consultation.CaseType caseType;
    private BigDecimal consultationFee;
    private Boolean feeOverride;
    private String feeOverrideReason;
    private String chiefComplaint;
    private Integer bpSystolic;
    private Integer bpDiastolic;
    private Integer pulse;
    private BigDecimal temperature;
    private BigDecimal weight;
    private BigDecimal height;
    private Integer spo2;
    private String clinicalExamination;
    private String diagnosis;
    private String doctorPrivateNotes;
    
    // Prescription items
    private List<PrescriptionItem> prescriptionItems;

    // Follow-up recommendation
    private LocalDate followUpDate;
    private String followUpInterval; // 3D, 7D, 15D, 30D, CUSTOM
    private String followUpReason;
}

