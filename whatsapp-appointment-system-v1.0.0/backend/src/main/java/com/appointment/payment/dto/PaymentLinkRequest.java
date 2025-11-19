package com.appointment.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLinkRequest {
    private Long bookingId;
    private Long patientId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String callbackUrl;
    private String redirectUrl;
}

