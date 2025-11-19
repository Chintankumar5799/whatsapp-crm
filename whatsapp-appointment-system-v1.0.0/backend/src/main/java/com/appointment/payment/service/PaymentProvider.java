package com.appointment.payment.service;

import com.appointment.payment.dto.PaymentLinkRequest;
import com.appointment.payment.dto.PaymentLinkResponse;
import com.appointment.payment.dto.WebhookPayload;

/**
 * Abstract interface for payment providers (Razorpay, Stripe, etc.)
 * Allows easy switching between payment providers
 */
public interface PaymentProvider {
    
    /**
     * Creates a payment link for the given request
     */
    PaymentLinkResponse createPaymentLink(PaymentLinkRequest request);
    
    /**
     * Verifies webhook signature from the payment provider
     */
    boolean verifyWebhookSignature(String payload, String signature, String secret);
    
    /**
     * Processes webhook payload and extracts payment information
     */
    WebhookPayload processWebhook(String payload);
    
    /**
     * Gets payment status from provider
     */
    com.appointment.payment.model.PaymentStatus getPaymentStatus(String providerPaymentId);
    
    /**
     * Returns the provider name (e.g., "RAZORPAY", "STRIPE")
     */
    String getProviderName();
}

