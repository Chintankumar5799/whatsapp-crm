package com.appointment.clinical.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "medicines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Medicine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_id", nullable = false)
    @Builder.Default
    private Long clinicId = 1L;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "brand_name", length = 150)
    private String brandName;

    @Column(name = "dosage_form", nullable = false, length = 50)
    @Builder.Default
    private String dosageForm = "TABLET";

    @Column(name = "strength", length = 50)
    private String strength;

    @Column(name = "composition")
    private String composition;

    @Column(name = "is_custom", nullable = false)
    @Builder.Default
    private Boolean isCustom = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

