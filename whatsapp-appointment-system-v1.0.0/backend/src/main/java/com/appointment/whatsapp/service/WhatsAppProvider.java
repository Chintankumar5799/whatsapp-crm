package com.appointment.whatsapp.service;

import com.appointment.whatsapp.dto.MessageRequest;
import com.appointment.whatsapp.dto.MessageResponse;

/**
 * Abstract interface for WhatsApp providers (Twilio, 360dialog, Gupshup, etc.)
 * Allows easy switching between WhatsApp providers
 */
public interface WhatsAppProvider {
    
    /**
     * Sends a text message
     */
    MessageResponse sendTextMessage(MessageRequest request);
    
    /**
     * Sends a message with interactive buttons
     */
    MessageResponse sendInteractiveButtons(MessageRequest request, String buttonText, String[] buttonOptions);
    
    /**
     * Sends a message with list
     */
    MessageResponse sendListMessage(MessageRequest request, String listTitle, String listDescription, 
                                   String[][] listItems);
    
    /**
     * Sends a template message
     */
    MessageResponse sendTemplateMessage(MessageRequest request, String templateName, String[] parameters);
    
    /**
     * Sends media (image, document, PDF)
     */
    MessageResponse sendMedia(MessageRequest request, String mediaUrl, String mediaType, String caption);
    
    /**
     * Verifies webhook signature
     */
    boolean verifyWebhookSignature(String payload, String signature);
    
    /**
     * Processes incoming webhook
     */
    IncomingMessage processWebhook(String payload);
    
    /**
     * Returns the provider name
     */
    String getProviderName();
    
    /**
     * Incoming message from webhook
     */
    record IncomingMessage(String from, String to, String body, String messageId, String messageType) {}
}

