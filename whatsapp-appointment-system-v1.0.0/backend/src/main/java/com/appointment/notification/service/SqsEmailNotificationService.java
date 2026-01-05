// package com.appointment.notification.service;

// import com.appointment.notification.dto.EmailNotificationRequest;
// import com.appointment.booking.model.Booking;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
// import org.springframework.messaging.support.MessageBuilder;
// import org.springframework.stereotype.Service;

// @Slf4j
// @Service
// @RequiredArgsConstructor
// public class SqsEmailNotificationService {

//     private final QueueMessagingTemplate queueMessagingTemplate;

//     @Value("${cloud.aws.sqs.email-queue-url}")
//     private String emailQueueUrl;

//     public void sendBookingConfirmationEmail(Booking booking, String patientEmail) {
//         try {
//             log.info("Sending booking confirmation email to SQS for booking: {}", booking.getBookingNumber());

//             EmailNotificationRequest request = EmailNotificationRequest.builder()
//                     .to(patientEmail)
//                     .subject("Booking Confirmed: " + booking.getBookingNumber())
//                     .body("Dear " + booking.getPatientName() + ",\n\n" +
//                             "Your appointment with Dr. " + booking.getDoctorId() + " has been confirmed for " +
//                             booking.getBookingDate() + " at " + booking.getStartTime() + ".\n\n" +
//                             "Booking Reference: " + booking.getBookingNumber() + "\n\n" +
//                             "Regards,\nWhatsApp Appointment System")
//                     .bookingReference(booking.getBookingNumber())
//                     .build();

//             queueMessagingTemplate.send(emailQueueUrl, 
//                     MessageBuilder.withPayload(request).build());

//             log.info("Booking confirmation email sent to SQS successfully.");

//         } catch (Exception e) {
//             log.error("Failed to send booking confirmation email to SQS", e);
//         }
//     }
// }
