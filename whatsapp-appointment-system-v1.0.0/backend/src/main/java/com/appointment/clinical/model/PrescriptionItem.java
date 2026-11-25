package com.appointment.clinical.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "prescription_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consultation_id", nullable = false)
    private Long consultationId;

    @Column(name = "medicine_id")
    private Long medicineId;

    @Column(name = "custom_medicine_name", length = 150)
    private String customMedicineName;

    @Column(name = "dosage", nullable = false, length = 50)
    private String dosage; // e.g. 1-0-1 or 1-0-0

    @Column(name = "frequency", length = 50)
    private String frequency; // e.g. Daily, Twice daily

    @Column(name = "duration_days", nullable = false)
    @Builder.Default
    private Integer durationDays = 5;

    @Column(name = "instructions")
    private String instructions; // e.g. After food

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

