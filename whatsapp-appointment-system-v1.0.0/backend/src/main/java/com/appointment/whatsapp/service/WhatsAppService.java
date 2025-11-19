package com.appointment.whatsapp.service;

import com.appointment.whatsapp.dto.MessageRequest;
import com.appointment.whatsapp.dto.MessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppService {
    
    private final WhatsAppProvider whatsAppProvider;
    
    @Value("${whatsapp.twilio.phone-number}")
    private String fromNumber;
    
    public MessageResponse sendTextMessage(String to, String message) {
        MessageRequest request = MessageRequest.builder()
                .to(to)
                .from(fromNumber)
                .body(message)
                .build();
        
        return whatsAppProvider.sendTextMessage(request);
    }
    
    public MessageResponse sendBookingConfirmation(String to, String patientName, String date, 
                                                   String time, String doctorName) {
        String message = String.format(
                "Hello %s! Your appointment is confirmed for %s at %s with %s. " +
                "We look forward to seeing you!",
                patientName, date, time, doctorName
        );
        return sendTextMessage(to, message);
    }
    
    public MessageResponse sendBookingPending(String to, String patientName, String date, 
                                             String time) {
        String message = String.format(
                "Hello %s! Your appointment request for %s at %s is pending doctor approval. " +
                "We will notify you once it's confirmed.",
                patientName, date, time
        );
        return sendTextMessage(to, message);
    }
    
    public MessageResponse sendPaymentLink(String to, String patientName, String paymentLink) {
        String message = String.format(
                "Hello %s! Please complete your payment using this link: %s",
                patientName, paymentLink
        );
        return sendTextMessage(to, message);
    }
    
    public MessageResponse sendReminder(String to, String patientName, String time, 
                                       String doctorName, int hoursBefore) {
        String message = String.format(
                "Reminder: You have an appointment %s at %s with %s. " +
                "Please arrive 10 minutes early.",
                hoursBefore == 24 ? "tomorrow" : "in 1 hour",
                time, doctorName
        );
        return sendTextMessage(to, message);
    }
    
    public MessageResponse sendInvoice(String to, String patientName, String invoiceUrl) {
        MessageRequest request = MessageRequest.builder()
                .to(to)
                .from(fromNumber)
                .body("Hello " + patientName + "! Your invoice is ready.")
                .mediaUrl(invoiceUrl)
                .mediaType("document")
                .build();
        
        return whatsAppProvider.sendMedia(request, invoiceUrl, "document", 
                "Your invoice is attached.");
    }
    
    public MessageResponse sendSlotList(String to, String date, String[][] slots) {
        return whatsAppProvider.sendListMessage(
                MessageRequest.builder()
                        .to(to)
                        .from(fromNumber)
                        .body("Available slots for " + date)
                        .build(),
                "Available Slots",
                "Please select a slot:",
                slots
        );
    }
    
    public MessageResponse sendApprovalRequest(String to, String doctorName, String patientName, 
                                              String date, String time, String bookingId) {
        String[] buttons = {"Approve", "Reject"};
        String message = String.format(
                "New appointment request:\nPatient: %s\nDate: %s\nTime: %s\nBooking ID: %s",
                patientName, date, time, bookingId
        );
        
        return whatsAppProvider.sendInteractiveButtons(
                MessageRequest.builder()
                        .to(to)
                        .from(fromNumber)
                        .body(message)
                        .build(),
                "Please approve or reject:",
                buttons
        );
    }
}

