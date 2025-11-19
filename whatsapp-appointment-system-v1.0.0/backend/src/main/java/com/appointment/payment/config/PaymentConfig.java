package com.appointment.payment.config;

import com.appointment.payment.service.PaymentProvider;
import com.appointment.payment.service.impl.RazorpayPaymentProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration
public class PaymentConfig {
    
    @Bean
    @Primary
    public PaymentProvider paymentProvider(
            @Value("${payment.razorpay.key-id:}") String razorpayKeyId,
            @Value("${payment.razorpay.key-secret:}") String razorpayKeySecret,
            @Value("${payment.razorpay.webhook-secret:}") String razorpayWebhookSecret) {
        try {
            return new RazorpayPaymentProvider(razorpayKeyId, razorpayKeySecret, razorpayWebhookSecret);
        } catch (Exception e) {
            log.error("Failed to initialize Razorpay payment provider", e);
            throw new RuntimeException("Payment provider initialization failed", e);
        }
    }
}

