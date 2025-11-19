package com.appointment.whatsapp.controller;

import com.appointment.whatsapp.service.WhatsAppProvider;
import com.appointment.whatsapp.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/webhooks/whatsapp")
@RequiredArgsConstructor
public class WhatsAppWebhookController {
    
    private final WhatsAppProvider whatsAppProvider;
    private final WhatsAppService whatsAppService;
    
    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Twilio-Signature", required = false) String signature) {
        
        log.info("Received WhatsApp webhook");
        
        // Verify signature if provided
        if (signature != null && !whatsAppProvider.verifyWebhookSignature(payload, signature)) {
            log.warn("Invalid webhook signature");
            return ResponseEntity.status(401).body("Invalid signature");
        }
        
        // Process webhook
        WhatsAppProvider.IncomingMessage message = whatsAppProvider.processWebhook(payload);
        if (message != null) {
            log.info("Received message from {}: {}", message.from(), message.body());
            // Process the message (handle booking flow, etc.)
            processIncomingMessage(message);
        }
        
        return ResponseEntity.ok("OK");
    }
    
    @GetMapping
    public ResponseEntity<String> verifyWebhook(@RequestParam("hub.challenge") String challenge) {
        // For webhook verification (some providers)
        return ResponseEntity.ok(challenge);
    }
    
    private void processIncomingMessage(WhatsAppProvider.IncomingMessage message) {
        // Handle incoming WhatsApp messages
        // This would integrate with the booking flow
        // For example: "Book appointment", "Select date", etc.
        String body = message.body().toLowerCase();
        
        if (body.contains("book") || body.contains("appointment")) {
            // Initiate booking flow
            whatsAppService.sendTextMessage(message.from(), 
                    "Welcome! Please select a date for your appointment.");
        }
        // Add more message handling logic
    }
}

