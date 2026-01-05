package com.appointment.booking.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;

@Entity
@Table(name = "pending_appointment_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingAppointmentRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctor_phone")
    private String doctorPhone;

    @Column(name = "patient_phone")
    private String patientPhone;

    @Column(name = "patient_name")
    private String patientName;

    @Column(name = "requested_date")
    private LocalDate requestedDate;

    @Column(name = "requested_start_time")
    private String requestedStartTime;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "address_id")
    private Long addressId;

    @org.hibernate.annotations.CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private java.time.LocalDateTime createdAt;

    @Column(name = "expires_at")
    private java.time.LocalDateTime expiresAt;
}
