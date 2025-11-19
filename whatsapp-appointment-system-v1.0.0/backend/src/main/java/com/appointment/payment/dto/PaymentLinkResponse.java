package com.appointment.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLinkResponse {
    private String paymentLinkId;
    private String paymentLink;
    private String shortUrl;
    private String status;
    private String providerOrderId;
}

