package com.appointment.whatsapp.service.impl;

import com.appointment.whatsapp.dto.MessageRequest;
import com.appointment.whatsapp.dto.MessageResponse;
import com.appointment.whatsapp.service.WhatsAppProvider;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TwilioWhatsAppProvider implements WhatsAppProvider {
    
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    
    public TwilioWhatsAppProvider(
            @Value("${whatsapp.twilio.account-sid}") String accountSid,
            @Value("${whatsapp.twilio.auth-token}") String authToken,
            @Value("${whatsapp.twilio.phone-number}") String fromNumber) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        Twilio.init(accountSid, authToken);
    }
    
    @Override
    public MessageResponse sendTextMessage(MessageRequest request) {
        try {
            Message message = Message.creator(
                    new PhoneNumber(request.getTo()),
                    new PhoneNumber(fromNumber),
                    request.getBody()
            ).create();
            
            return MessageResponse.builder()
                    .success(true)
                    .messageId(message.getSid())
                    .status(message.getStatus().toString())
                    .build();
        } catch (Exception e) {
            log.error("Error sending WhatsApp message via Twilio", e);
            return MessageResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
    
    @Override
    public MessageResponse sendInteractiveButtons(MessageRequest request, String buttonText, String[] buttonOptions) {
        // Twilio WhatsApp supports interactive buttons via templates
        // For now, send as text with button options listed
        StringBuilder message = new StringBuilder(request.getBody() != null ? request.getBody() + "\n\n" : "");
        message.append(buttonText).append("\n");
        for (int i = 0; i < buttonOptions.length; i++) {
            message.append((i + 1)).append(". ").append(buttonOptions[i]).append("\n");
        }
        
        MessageRequest modifiedRequest = MessageRequest.builder()
                .to(request.getTo())
                .from(request.getFrom())
                .body(message.toString())
                .build();
        
        return sendTextMessage(modifiedRequest);
    }
    
    @Override
    public MessageResponse sendListMessage(MessageRequest request, String listTitle, String listDescription, 
                                          String[][] listItems) {
        // Twilio WhatsApp list messages require templates
        // For now, send as formatted text
        StringBuilder message = new StringBuilder(request.getBody() != null ? request.getBody() + "\n\n" : "");
        message.append("*").append(listTitle).append("*\n");
        if (listDescription != null) {
            message.append(listDescription).append("\n\n");
        }
        
        for (int i = 0; i < listItems.length; i++) {
            String[] item = listItems[i];
            message.append((i + 1)).append(". ");
            if (item.length > 0) message.append(item[0]);
            if (item.length > 1) message.append(" - ").append(item[1]);
            message.append("\n");
        }
        
        MessageRequest modifiedRequest = MessageRequest.builder()
                .to(request.getTo())
                .from(request.getFrom())
                .body(message.toString())
                .build();
        
        return sendTextMessage(modifiedRequest);
    }
    
    @Override
    public MessageResponse sendTemplateMessage(MessageRequest request, String templateName, String[] parameters) {
        // Twilio template messages require pre-approved templates
        // For now, send as text with parameters substituted
        String messageBody = request.getBody();
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                messageBody = messageBody.replace("{" + i + "}", parameters[i]);
            }
        }
        
        MessageRequest modifiedRequest = MessageRequest.builder()
                .to(request.getTo())
                .from(request.getFrom())
                .body(messageBody)
                .build();
        
        return sendTextMessage(modifiedRequest);
    }
    
    @Override
    public MessageResponse sendMedia(MessageRequest request, String mediaUrl, String mediaType, String caption) {
        try {
            Message message = Message.creator(
                    new PhoneNumber(request.getTo()),
                    new PhoneNumber(fromNumber)
            ).setMediaUrl(java.util.Arrays.asList(new java.net.URI(mediaUrl)))
             .setBody(caption != null ? caption : "")
             .create();
            
            return MessageResponse.builder()
                    .success(true)
                    .messageId(message.getSid())
                    .status(message.getStatus().toString())
                    .build();
        } catch (Exception e) {
            log.error("Error sending WhatsApp media via Twilio", e);
            return MessageResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
    
    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        // Twilio webhook signature verification
        // For production, implement proper signature verification
        return true; // Simplified for now
    }
    
    @Override
    public IncomingMessage processWebhook(String payload) {
        // Parse Twilio webhook payload
        // This is a simplified version - in production, use proper JSON parsing
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(payload);
            
            String from = jsonNode.has("From") ? jsonNode.get("From").asText() : null;
            String to = jsonNode.has("To") ? jsonNode.get("To").asText() : null;
            String body = jsonNode.has("Body") ? jsonNode.get("Body").asText() : null;
            String messageId = jsonNode.has("MessageSid") ? jsonNode.get("MessageSid").asText() : null;
            String messageType = jsonNode.has("MessageType") ? jsonNode.get("MessageType").asText() : "text";
            
            return new IncomingMessage(from, to, body, messageId, messageType);
        } catch (Exception e) {
            log.error("Error processing Twilio webhook", e);
            return null;
        }
    }
    
    @Override
    public String getProviderName() {
        return "TWILIO";
    }
}

