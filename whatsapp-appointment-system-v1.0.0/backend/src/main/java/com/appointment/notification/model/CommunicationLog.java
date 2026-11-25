package com.appointment.notification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "communication_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunicationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_id", nullable = false)
    @Builder.Default
    private Long clinicId = 1L;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "recipient_phone", nullable = false, length = 30)
    private String recipientPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    @Builder.Default
    private CommChannel channel = CommChannel.WHATSAPP;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;

    @Column(name = "message_body", nullable = false, columnDefinition = "TEXT")
    private String messageBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.SENT;

    @Column(name = "external_message_id", length = 100)
    private String externalMessageId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
    }

    public enum CommChannel {
        WHATSAPP,
        SMS,
        VOICE_CALL
    }

    public enum MessageType {
        APPOINTMENT_CONFIRMATION,
        APPOINTMENT_REMINDER_24H,
        APPOINTMENT_REMINDER_1H,
        FOLLOW_UP_RECALL,
        PAYMENT_LINK,
        INVOICE,
        POST_VISIT_CHECKIN,
        FEEDBACK_REQUEST
    }

    public enum DeliveryStatus {
        SENT,
        DELIVERED,
        FAILED
    }
}

