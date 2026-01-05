package com.appointment.payment.service.impl;

import com.appointment.payment.dto.PaymentLinkRequest;
import com.appointment.payment.dto.PaymentLinkResponse;
import com.appointment.payment.dto.WebhookPayload;
import com.appointment.payment.service.PaymentProvider;
import com.appointment.payment.model.PaymentStatus;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
public class StripePaymentProvider implements PaymentProvider {

    private final String webhookSecret;

    public StripePaymentProvider(
            @Value("${payment.stripe.api-key}") String apiKey,
            @Value("${payment.stripe.webhook-secret}") String webhookSecret) {
        Stripe.apiKey = apiKey;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public PaymentLinkResponse createPaymentLink(PaymentLinkRequest request) {
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(request.getRedirectUrl() != null ? request.getRedirectUrl() : "http://localhost:3000/payment/success")
                    .setCancelUrl(request.getCallbackUrl() != null ? request.getCallbackUrl() : "http://localhost:3000/payment/cancel")
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency(request.getCurrency().toLowerCase())
                                    .setUnitAmount(request.getAmount().multiply(new BigDecimal("100")).longValue())
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName(request.getDescription())
                                            .build())
                                    .build())
                            .build())
                    .setCustomerEmail(request.getCustomerEmail())
                    .putMetadata("booking_id", request.getBookingId().toString())
                    .putMetadata("patient_id", request.getPatientId() != null ? request.getPatientId().toString() : "")
                    .build();

            Session session = Session.create(params);

            return PaymentLinkResponse.builder()
                    .paymentLinkId(session.getId())
                    .paymentLink(session.getUrl())
                    .shortUrl(session.getUrl())
                    .status(session.getStatus())
                    .providerOrderId(session.getPaymentIntent())
                    .build();

        } catch (StripeException e) {
            log.error("Error creating Stripe checkout session", e);
            throw new RuntimeException("Failed to create payment link: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature, String secret) {
        try {
            Webhook.constructEvent(payload, signature, secret);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public WebhookPayload processWebhook(String payload) {
        // Simplified parsing for restoration
        return WebhookPayload.builder().metadata(payload).build();
    }

    @Override
    public PaymentStatus getPaymentStatus(String providerPaymentId) {
        return PaymentStatus.PENDING;
    }

    @Override
    public String getProviderName() {
        return "STRIPE";
    }
}
