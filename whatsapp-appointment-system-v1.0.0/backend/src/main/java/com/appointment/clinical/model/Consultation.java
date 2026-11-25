package com.appointment.clinical.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "consultations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Consultation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_id", nullable = false)
    @Builder.Default
    private Long clinicId = 1L;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_type", nullable = false)
    @Builder.Default
    private CaseType caseType = CaseType.NEW_CASE;

    @Column(name = "consultation_fee", nullable = false)
    private BigDecimal consultationFee;

    @Column(name = "fee_override", nullable = false)
    @Builder.Default
    private Boolean feeOverride = false;

    @Column(name = "fee_override_reason")
    private String feeOverrideReason;

    @Column(name = "chief_complaint", nullable = false, columnDefinition = "TEXT")
    private String chiefComplaint;

    @Column(name = "bp_systolic")
    private Integer bpSystolic;

    @Column(name = "bp_diastolic")
    private Integer bpDiastolic;

    @Column(name = "pulse")
    private Integer pulse;

    @Column(name = "temperature")
    private BigDecimal temperature;

    @Column(name = "weight")
    private BigDecimal weight;

    @Column(name = "height")
    private BigDecimal height;

    @Column(name = "spo2")
    private Integer spo2;

    @Column(name = "clinical_examination", columnDefinition = "TEXT")
    private String clinicalExamination;

    @Column(name = "diagnosis", nullable = false, columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "doctor_private_notes", columnDefinition = "TEXT")
    private String doctorPrivateNotes;

    @Column(name = "discharge_summary", columnDefinition = "TEXT")
    private String dischargeSummary;

    @Column(name = "discharge_condition", length = 100)
    private String dischargeCondition;

    @Column(name = "discharge_date")
    private LocalDateTime dischargeDate;

    @Column(name = "is_finalized", nullable = false)
    @Builder.Default
    private Boolean isFinalized = true;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (finalizedAt == null && Boolean.TRUE.equals(isFinalized)) {
            finalizedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum CaseType {
        NEW_CASE,
        OLD_CASE
    }
}

