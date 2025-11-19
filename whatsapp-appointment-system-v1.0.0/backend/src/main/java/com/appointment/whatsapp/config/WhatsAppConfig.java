package com.appointment.whatsapp.config;

import com.appointment.whatsapp.service.WhatsAppProvider;
import com.appointment.whatsapp.service.impl.TwilioWhatsAppProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration
public class WhatsAppConfig {
    
    @Bean
    @Primary
    public WhatsAppProvider whatsAppProvider(
            @Value("${whatsapp.provider:TWILIO}") String provider,
            @Value("${whatsapp.twilio.account-sid:}") String twilioAccountSid,
            @Value("${whatsapp.twilio.auth-token:}") String twilioAuthToken,
            @Value("${whatsapp.twilio.phone-number:}") String twilioPhoneNumber) {
        
        if ("TWILIO".equalsIgnoreCase(provider)) {
            return new TwilioWhatsAppProvider(twilioAccountSid, twilioAuthToken, twilioPhoneNumber);
        }
        
        // Add other providers here (360dialog, Gupshup, etc.)
        throw new IllegalArgumentException("Unsupported WhatsApp provider: " + provider);
    }
}

