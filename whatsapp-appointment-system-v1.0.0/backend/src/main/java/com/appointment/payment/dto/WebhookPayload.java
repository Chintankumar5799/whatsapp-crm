package com.appointment.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookPayload {
    private String event;
    private String providerPaymentId;
    private String providerOrderId;
    private String paymentLinkId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDateTime paidAt;
    private String paymentMethod;
    private String metadata;
}

