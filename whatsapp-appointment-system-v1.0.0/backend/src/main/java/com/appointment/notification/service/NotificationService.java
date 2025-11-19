package com.appointment.notification.service;

import com.appointment.booking.model.Booking;
import com.appointment.booking.repository.BookingRepository;
import com.appointment.payment.model.Invoice;
import com.appointment.payment.repository.InvoiceRepository;
import com.appointment.payment.model.Payment;
import com.appointment.payment.repository.PaymentRepository;
import com.appointment.whatsapp.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final WhatsAppService whatsAppService;
    
    /**
     * Sends 24-hour reminder for appointments
     * Runs daily at 9 AM
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void send24HourReminders() {
        log.info("Sending 24-hour appointment reminders");
        
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        List<Booking> bookings = bookingRepository.findByDoctorIdAndDateAndStatusIn(
                1L, // TODO: Get all doctors
                tomorrow.toLocalDate(),
                List.of(Booking.BookingStatus.CONFIRMED)
        );
        
        for (Booking booking : bookings) {
            try {
                // TODO: Get patient and doctor details
                whatsAppService.sendReminder(
                        "whatsapp:+1234567890", // TODO: Get patient WhatsApp
                        "Patient", // TODO: Get patient name
                        booking.getStartTime().toString(),
                        "Dr. Smith", // TODO: Get doctor name
                        24
                );
                log.info("Sent 24h reminder for booking {}", booking.getBookingNumber());
            } catch (Exception e) {
                log.error("Error sending 24h reminder for booking {}", booking.getId(), e);
            }
        }
    }
    
    /**
     * Sends 1-hour reminder for appointments
     * Runs every hour
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void send1HourReminders() {
        log.info("Sending 1-hour appointment reminders");
        
        LocalDateTime oneHourLater = LocalDateTime.now().plusHours(1);
        List<Booking> bookings = bookingRepository.findByDoctorIdAndDateAndStatusIn(
                1L, // TODO: Get all doctors
                oneHourLater.toLocalDate(),
                List.of(Booking.BookingStatus.CONFIRMED)
        );
        
        for (Booking booking : bookings) {
            // Check if appointment is within 1 hour
            if (booking.getStartTime().isBefore(oneHourLater.toLocalTime()) &&
                booking.getStartTime().isAfter(LocalDateTime.now().toLocalTime())) {
                try {
                    // TODO: Get patient and doctor details
                    whatsAppService.sendReminder(
                            "whatsapp:+1234567890", // TODO: Get patient WhatsApp
                            "Patient", // TODO: Get patient name
                            booking.getStartTime().toString(),
                            "Dr. Smith", // TODO: Get doctor name
                            1
                    );
                    log.info("Sent 1h reminder for booking {}", booking.getBookingNumber());
                } catch (Exception e) {
                    log.error("Error sending 1h reminder for booking {}", booking.getId(), e);
                }
            }
        }
    }
    
    /**
     * Sends payment link after booking confirmation
     */
    public void sendPaymentLinkNotification(Long bookingId, String paymentLink) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
            
            // TODO: Get patient details
            whatsAppService.sendPaymentLink(
                    "whatsapp:+1234567890", // TODO: Get patient WhatsApp
                    "Patient", // TODO: Get patient name
                    paymentLink
            );
            
            log.info("Sent payment link for booking {}", bookingId);
        } catch (Exception e) {
            log.error("Error sending payment link notification for booking {}", bookingId, e);
        }
    }
    
    /**
     * Sends invoice after successful payment
     */
    public void sendInvoiceNotification(Long paymentId) {
        try {
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
            
            List<Invoice> invoices = invoiceRepository.findByPaymentId(paymentId);
            if (invoices.isEmpty()) {
                log.warn("No invoice found for payment {}", paymentId);
                return;
            }
            
            Invoice invoice = invoices.get(0);
            // TODO: Get invoice PDF URL
            String invoiceUrl = "https://yourapp.com/invoices/" + invoice.getId() + "/pdf";
            
            // TODO: Get patient details
            whatsAppService.sendInvoice(
                    "whatsapp:+1234567890", // TODO: Get patient WhatsApp
                    "Patient", // TODO: Get patient name
                    invoiceUrl
            );
            
            log.info("Sent invoice for payment {}", paymentId);
        } catch (Exception e) {
            log.error("Error sending invoice notification for payment {}", paymentId, e);
        }
    }
    
    /**
     * Sends booking confirmation notification
     */
    public void sendBookingConfirmation(Booking booking) {
        try {
            // TODO: Get patient and doctor details
            whatsAppService.sendBookingConfirmation(
                    "whatsapp:+1234567890", // TODO: Get patient WhatsApp
                    "Patient", // TODO: Get patient name
                    booking.getBookingDate().toString(),
                    booking.getStartTime().toString(),
                    "Dr. Smith" // TODO: Get doctor name
            );
            
            log.info("Sent booking confirmation for booking {}", booking.getBookingNumber());
        } catch (Exception e) {
            log.error("Error sending booking confirmation for booking {}", booking.getId(), e);
        }
    }
    
    /**
     * Sends booking pending notification
     */
    public void sendBookingPending(Booking booking) {
        try {
            // TODO: Get patient details
            whatsAppService.sendBookingPending(
                    "whatsapp:+1234567890", // TODO: Get patient WhatsApp
                    "Patient", // TODO: Get patient name
                    booking.getBookingDate().toString(),
                    booking.getStartTime().toString()
            );
            
            log.info("Sent booking pending notification for booking {}", booking.getBookingNumber());
        } catch (Exception e) {
            log.error("Error sending booking pending notification for booking {}", booking.getId(), e);
        }
    }
}

