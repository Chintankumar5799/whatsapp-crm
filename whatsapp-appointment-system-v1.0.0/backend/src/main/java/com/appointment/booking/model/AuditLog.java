package com.appointment.booking.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // BOOKING, PAYMENT, etc.

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "action", nullable = false, length = 50)
    private String action; // CREATE, UPDATE, DELETE, APPROVE, etc.

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_type", length = 20)
    private String userType; // DOCTOR, PATIENT, ADMIN

    @Column(name = "details", columnDefinition = "TEXT")
    private String details; // JSON string with change details

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

