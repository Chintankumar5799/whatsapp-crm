package com.appointment.booking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "patient_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_id", nullable = false)
    @Builder.Default
    private Long clinicId = 1L;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "document_type", nullable = false, length = 50)
    @Builder.Default
    private String documentType = "LAB_REPORT";

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JsonIgnore
    @Column(name = "file_data")
    private byte[] fileData;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "uploaded_by_user_id")
    private Long uploadedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

