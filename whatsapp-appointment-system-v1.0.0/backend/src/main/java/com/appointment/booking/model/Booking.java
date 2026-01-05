package com.appointment.booking.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.math.BigDecimal;

@Entity
@Table(name = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_number", unique = true, length = 50)
    private String bookingNumber;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "patient_id") // Made nullable as patient might be unregistered? No, usually linked.
    private Long patientId;

    @Column(name = "slot_id")
    private Long slotId;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "requires_payment", nullable = false)
    @Builder.Default
    private Boolean requiresPayment = true;

    @Column(name = "payment_status", length = 20)
    private String paymentStatus; // PENDING, PAID, FAILED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    // New Fields
    @Column(name = "patient_name")
    private String patientName;
    
    @Column(name = "patient_phone")
    private String patientPhone;
    
    @Column(name = "address_id")
    private Long addressId;
    
    @Column(name = "doctor_notes", columnDefinition = "TEXT")
    private String doctorNotes;
    
    @Column(name = "disease_description", columnDefinition = "TEXT")
    private String diseaseDescription;
    
    @Column(name = "payment_link_url", columnDefinition = "TEXT")
    private String paymentLinkUrl;
    
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum BookingStatus {
        PENDING,      // Waiting for doctor approval
        CONFIRMED,    // Approved by doctor
        ACCEPTED,     // Synonym for CONFIRMED used in some flows
        CANCELLED,    // Cancelled
        COMPLETED,    // Appointment completed
        PAID          // Paid
    }
}
