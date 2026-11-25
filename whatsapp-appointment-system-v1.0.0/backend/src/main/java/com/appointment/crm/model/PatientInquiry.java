package com.appointment.crm.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient_inquiries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientInquiry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_id", nullable = false)
    @Builder.Default
    private Long clinicId = 1L;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "request_description", columnDefinition = "TEXT")
    private String requestDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    @Builder.Default
    private InquirySource source = InquirySource.PHONE_CALL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private InquiryStatus status = InquiryStatus.NEW;

    @Column(name = "assigned_staff_id")
    private Long assignedStaffId;

    @Column(name = "next_follow_up_date")
    private LocalDate nextFollowUpDate;

    @Column(name = "converted_patient_id")
    private Long convertedPatientId;

    @Column(name = "converted_booking_id")
    private Long convertedBookingId;

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

    public enum InquirySource {
        GOOGLE_BUSINESS,
        WHATSAPP,
        WEBSITE,
        PHONE_CALL,
        REFERRAL,
        WALK_IN
    }

    public enum InquiryStatus {
        NEW,
        CONTACTED,
        FOLLOW_UP_REQUIRED,
        BOOKED,
        NOT_INTERESTED,
        CLOSED
    }
}

