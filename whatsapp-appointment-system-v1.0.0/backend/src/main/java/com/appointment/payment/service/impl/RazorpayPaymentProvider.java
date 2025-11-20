package com.appointment.payment.service.impl;

import java.math.BigDecimal;  // This import is already there
import com.appointment.payment.dto.PaymentLinkRequest;
import com.appointment.payment.dto.PaymentLinkResponse;
import com.appointment.payment.dto.WebhookPayload;
import com.appointment.payment.service.PaymentProvider;
import com.appointment.payment.model.PaymentStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Order;
import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

@Slf4j
@Service
public class RazorpayPaymentProvider implements PaymentProvider {

    private final RazorpayClient razorpayClient;
    private final String webhookSecret;
    private final ObjectMapper objectMapper;
    
    public RazorpayPaymentProvider(
            @Value("${payment.razorpay.key-id}") String keyId,
            @Value("${payment.razorpay.key-secret}") String keySecret,
            @Value("${payment.razorpay.webhook-secret}") String webhookSecret) throws RazorpayException {
        this.razorpayClient = new RazorpayClient(keyId, keySecret);
        this.webhookSecret = webhookSecret;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public PaymentLinkResponse createPaymentLink(PaymentLinkRequest request) {
        try {
            JSONObject paymentLinkRequest = new JSONObject();
            paymentLinkRequest.put("amount", request.getAmount().multiply(new BigDecimal("100")).longValue()); // Convert to paise
            paymentLinkRequest.put("currency", request.getCurrency());
            paymentLinkRequest.put("description", request.getDescription());
            paymentLinkRequest.put("customer", new JSONObject()
                    .put("name", request.getCustomerName())
                    .put("email", request.getCustomerEmail())
                    .put("contact", request.getCustomerPhone()));
            
            JSONObject notify = new JSONObject();
            notify.put("sms", true);
            notify.put("email", true);
            paymentLinkRequest.put("notify", notify);
            
            paymentLinkRequest.put("reminder_enable", true);
            
            if (request.getCallbackUrl() != null) {
                paymentLinkRequest.put("callback_url", request.getCallbackUrl());
            }
            
            if (request.getRedirectUrl() != null) {
                paymentLinkRequest.put("callback_method", "get");
            }
            
            // Add metadata
            JSONObject notes = new JSONObject();
            notes.put("booking_id", request.getBookingId().toString());
            notes.put("patient_id", request.getPatientId().toString());
            paymentLinkRequest.put("notes", notes);
            
            PaymentLink paymentLink = razorpayClient.paymentLink.create(paymentLinkRequest);
            
            return PaymentLinkResponse.builder()
                    .paymentLinkId(paymentLink.get("id"))
                    .paymentLink(paymentLink.get("short_url"))
                    .shortUrl(paymentLink.get("short_url"))
                    .status(paymentLink.get("status"))
                    .providerOrderId(paymentLink.has("order_id") ? paymentLink.get("order_id") : null)
                    .build();
                    
        } catch (RazorpayException e) {
            log.error("Error creating Razorpay payment link", e);
            throw new RuntimeException("Failed to create payment link: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean verifyWebhookSignature(String payload, String signature, String secret) {
        try {
            String expectedSignature = calculateSignature(payload, secret);
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }
    
    private String calculateSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error calculating signature", e);
        }
    }
    
    @Override
    public WebhookPayload processWebhook(String payload) {
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            JsonNode event = jsonNode.get("event");
            JsonNode payloadNode = jsonNode.get("payload");
            
            if (payloadNode == null || !payloadNode.has("payment_link")) {
                log.warn("Invalid webhook payload structure");
                return null;
            }
            
            JsonNode paymentLinkEntity = payloadNode.get("payment_link").get("entity");
            JsonNode paymentEntity = payloadNode.has("payment") && payloadNode.get("payment").has("entity") 
                    ? payloadNode.get("payment").get("entity") : null;
            
            String paymentId = paymentEntity != null && paymentEntity.has("id") 
                    ? paymentEntity.get("id").asText() : null;
            String orderId = paymentLinkEntity.has("order_id") 
                    ? paymentLinkEntity.get("order_id").asText() : null;
            String paymentLinkId = paymentLinkEntity.has("id") 
                    ? paymentLinkEntity.get("id").asText() : null;
            
            long amount = paymentLinkEntity.has("amount") 
                    ? paymentLinkEntity.get("amount").asLong() : 0;
            BigDecimal amountDecimal = new BigDecimal(amount).divide(new BigDecimal("100")); // Convert from paise
            
            String status = paymentLinkEntity.has("status") 
                    ? paymentLinkEntity.get("status").asText() : "pending";
            
            String paidAtStr = paymentEntity != null && paymentEntity.has("created_at") 
                    ? paymentEntity.get("created_at").toString() : null;
            LocalDateTime paidAt = null;
            if (paidAtStr != null) {
                try {
                    long timestamp = Long.parseLong(paidAtStr);
                    paidAt = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
                } catch (Exception e) {
                    log.warn("Error parsing paid_at timestamp", e);
                }
            }
            
            return WebhookPayload.builder()
                    .event(event != null ? event.asText() : "payment_link.paid")
                    .providerPaymentId(paymentId)
                    .providerOrderId(orderId)
                    .paymentLinkId(paymentLinkId)
                    .amount(amountDecimal)
                    .currency(paymentLinkEntity.has("currency") ? paymentLinkEntity.get("currency").asText() : "INR")
                    .status(mapRazorpayStatus(status))
                    .paidAt(paidAt)
                    .paymentMethod(paymentEntity != null && paymentEntity.has("method") 
                            ? paymentEntity.get("method").asText() : null)
                    .metadata(payload)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error processing webhook payload", e);
            throw new RuntimeException("Failed to process webhook: " + e.getMessage(), e);
        }
    }
    
    private String mapRazorpayStatus(String razorpayStatus) {
        return switch (razorpayStatus.toLowerCase()) {
            case "paid" -> "SUCCESS";
            case "created", "issued" -> "PENDING";
            case "cancelled" -> "CANCELLED";
            default -> "PENDING";
        };
    }
    
    @Override
    public com.appointment.payment.model.PaymentStatus getPaymentStatus(String providerPaymentId) {
        try {
            com.razorpay.Payment payment = razorpayClient.payments.fetch(providerPaymentId);
            String status = payment.get("status");
            return mapToPaymentStatus(status);
        } catch (RazorpayException e) {
            log.error("Error fetching payment status from Razorpay", e);
            return com.appointment.payment.model.PaymentStatus.PENDING;
        }
    }
    
    private com.appointment.payment.model.PaymentStatus mapToPaymentStatus(String razorpayStatus) {
        return switch (razorpayStatus.toLowerCase()) {
            case "authorized", "captured" -> com.appointment.payment.model.PaymentStatus.SUCCESS;
            case "failed" -> com.appointment.payment.model.PaymentStatus.FAILED;
            case "refunded" -> com.appointment.payment.model.PaymentStatus.REFUNDED;
            default -> com.appointment.payment.model.PaymentStatus.PENDING;
        };
    }
    
    @Override
    public String getProviderName() {
        return "RAZORPAY";
    }
}