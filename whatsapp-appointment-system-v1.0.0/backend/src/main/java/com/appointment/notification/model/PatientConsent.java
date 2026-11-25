package com.appointment.notification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "patient_consents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientConsent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_id", nullable = false)
    @Builder.Default
    private Long clinicId = 1L;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false)
    private ConsentType consentType;

    @Column(name = "is_granted", nullable = false)
    @Builder.Default
    private Boolean isGranted = true;

    @Column(name = "consent_method", length = 50)
    @Builder.Default
    private String consentMethod = "IN_PERSON_REGISTRATION";

    @Column(name = "granted_at", nullable = false, updatable = false)
    private LocalDateTime grantedAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @PrePersist
    protected void onCreate() {
        if (grantedAt == null) {
            grantedAt = LocalDateTime.now();
        }
    }

    public enum ConsentType {
        REMINDERS,
        AUTOMATED_CALLS,
        PROMOTIONAL
    }
}

