package com.appointment.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {
    private String to; // WhatsApp number (e.g., whatsapp:+1234567890)
    private String from; // Sender WhatsApp number
    private String body; // Message text
    private String mediaUrl; // URL for media
    private String mediaType; // image, document, video, audio
}

