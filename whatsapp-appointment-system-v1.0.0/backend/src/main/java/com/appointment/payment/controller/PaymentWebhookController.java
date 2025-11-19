package com.appointment.payment.controller;

import com.appointment.payment.dto.WebhookPayload;
import com.appointment.payment.service.PaymentProvider;
import com.appointment.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/webhooks/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {
    
    private final PaymentService paymentService;
    private final PaymentProvider paymentProvider;
    
    @Value("${payment.razorpay.webhook-secret:}")
    private String webhookSecret;
    
    @PostMapping("/razorpay")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        
        log.info("Received Razorpay webhook");
        
        // Verify webhook signature
        if (signature != null && !paymentProvider.verifyWebhookSignature(payload, signature, webhookSecret)) {
            log.warn("Invalid webhook signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }
        
        try {
            // Process webhook
            WebhookPayload webhookPayload = paymentProvider.processWebhook(payload);
            if (webhookPayload != null) {
                paymentService.processWebhook(webhookPayload);
            }
            
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook: " + e.getMessage());
        }
    }
}

