package com.appointment.clinical.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "follow_ups")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowUp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_id", nullable = false)
    @Builder.Default
    private Long clinicId = 1L;

    @Column(name = "consultation_id", nullable = false)
    private Long consultationId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "interval_preset", length = 20)
    @Builder.Default
    private String intervalPreset = "7D"; // 3D, 7D, 15D, 30D, CUSTOM

    @Column(name = "reason")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private FollowUpStatus status = FollowUpStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_status", nullable = false)
    @Builder.Default
    private ContactStatus contactStatus = ContactStatus.NOT_CONTACTED;

    @Column(name = "assigned_staff_id")
    private Long assignedStaffId;

    @Column(name = "contact_attempt_count", nullable = false)
    @Builder.Default
    private Integer contactAttemptCount = 0;

    @Column(name = "last_contact_attempt_at")
    private LocalDateTime lastContactAttemptAt;

    @Column(name = "resulting_booking_id")
    private Long resultingBookingId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum FollowUpStatus {
        PENDING,
        BOOKED,
        COMPLETED,
        CANCELLED
    }

    public enum ContactStatus {
        NOT_CONTACTED,
        CONTACTED,
        UNREACHABLE,
        DECLINED,
        CALLBACK_REQUESTED
    }
}

