package com.appointment.clinical.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "opd_queue")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpdQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_id", nullable = false)
    @Builder.Default
    private Long clinicId = 1L;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "queue_number", nullable = false)
    private Integer queueNumber;

    @Column(name = "queue_date", nullable = false)
    private LocalDate queueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private QueueStatus status = QueueStatus.WAITING;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    @Column(name = "consultation_start_time")
    private LocalDateTime consultationStartTime;

    @Column(name = "consultation_end_time")
    private LocalDateTime consultationEndTime;

    @Column(name = "priority_flag", nullable = false)
    @Builder.Default
    private Boolean priorityFlag = false;

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

    public enum QueueStatus {
        WAITING,
        IN_CONSULTATION,
        COMPLETED,
        CANCELLED,
        NO_SHOW
    }
}

