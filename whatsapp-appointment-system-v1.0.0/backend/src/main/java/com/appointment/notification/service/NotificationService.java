package com.appointment.notification.service;

import com.appointment.auth.model.User;
import com.appointment.auth.repository.UserRepository;
import com.appointment.booking.model.Booking;
import com.appointment.booking.model.Patient;
import com.appointment.booking.repository.BookingRepository;
import com.appointment.booking.repository.PatientRepository;
import com.appointment.clinical.model.FollowUp;
import com.appointment.clinical.repository.FollowUpRepository;
import com.appointment.notification.model.CommunicationLog;
import com.appointment.notification.model.PatientConsent;
import com.appointment.notification.repository.CommunicationLogRepository;
import com.appointment.notification.repository.PatientConsentRepository;
import com.appointment.payment.model.Invoice;
import com.appointment.payment.model.Payment;
import com.appointment.payment.repository.InvoiceRepository;
import com.appointment.payment.repository.PaymentRepository;
import com.appointment.whatsapp.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final FollowUpRepository followUpRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final CommunicationLogRepository communicationLogRepository;
    private final PatientConsentRepository consentRepository;
    private final WhatsAppService whatsAppService;
    private final TemplateService templateService;

    /**
     * Sends 24-hour reminder for appointments across all doctors
     * Runs daily at 9:00 AM
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void send24HourReminders() {
        log.info("Sending dynamic 24-hour appointment reminders");
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Booking> bookings = bookingRepository.findAll().stream()
                .filter(b -> tomorrow.equals(b.getBookingDate()) && 
                             (b.getStatus() == Booking.BookingStatus.CONFIRMED || b.getStatus() == Booking.BookingStatus.ACCEPTED))
                .toList();

        for (Booking booking : bookings) {
            try {
                if (!hasConsent(booking.getPatientId(), PatientConsent.ConsentType.REMINDERS)) {
                    log.info("Skipping 24h reminder: Patient {} has revoked reminder consent", booking.getPatientId());
                    continue;
                }

                String patientPhone = getPatientPhone(booking);
                String patientName = getPatientName(booking);
                String doctorName = getDoctorName(booking.getDoctorId());

                if (patientPhone != null && !patientPhone.isEmpty()) {
                    String cleanPhone = formatWhatsAppPhone(patientPhone);

                    Map<String, String> vars = new HashMap<>();
                    vars.put("patient_name", patientName);
                    vars.put("doctor_name", doctorName);
                    vars.put("date", booking.getBookingDate().toString());
                    vars.put("time", booking.getStartTime().toString());
                    vars.put("booking_id", booking.getBookingNumber() != null ? booking.getBookingNumber() : booking.getId().toString());

                    String rendered = templateService.renderMessage(booking.getClinicId(), "REMINDER_24H", vars);
                    if (rendered != null) {
                        whatsAppService.sendTextMessage(cleanPhone, rendered);
                        logComm(booking.getClinicId(), booking.getPatientId(), booking.getId(), cleanPhone,
                                CommunicationLog.MessageType.APPOINTMENT_REMINDER_24H, rendered);
                    } else {
                        whatsAppService.sendReminder(cleanPhone, patientName, booking.getStartTime().toString(), doctorName, 24);
                        logComm(booking.getClinicId(), booking.getPatientId(), booking.getId(), cleanPhone,
                                CommunicationLog.MessageType.APPOINTMENT_REMINDER_24H,
                                "24h Reminder sent for appointment at " + booking.getStartTime() + " with " + doctorName);
                    }
                    log.info("Sent 24h reminder to {} for booking {}", patientPhone, booking.getBookingNumber());
                }
            } catch (Exception e) {
                log.error("Error sending 24h reminder for booking {}", booking.getId(), e);
            }
        }
    }

    /**
     * Sends 1-hour reminder for appointments
     * Runs every hour on the hour
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void send1HourReminders() {
        log.info("Sending dynamic 1-hour appointment reminders");
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalTime oneHourLater = now.plusHours(1);

        List<Booking> bookings = bookingRepository.findAll().stream()
                .filter(b -> today.equals(b.getBookingDate()) && 
                             (b.getStatus() == Booking.BookingStatus.CONFIRMED || b.getStatus() == Booking.BookingStatus.ACCEPTED) &&
                             b.getStartTime().isAfter(now) && b.getStartTime().isBefore(oneHourLater))
                .toList();

        for (Booking booking : bookings) {
            try {
                if (!hasConsent(booking.getPatientId(), PatientConsent.ConsentType.REMINDERS)) {
                    continue;
                }

                String patientPhone = getPatientPhone(booking);
                String patientName = getPatientName(booking);
                String doctorName = getDoctorName(booking.getDoctorId());

                if (patientPhone != null && !patientPhone.isEmpty()) {
                    String cleanPhone = formatWhatsAppPhone(patientPhone);
                    whatsAppService.sendReminder(cleanPhone, patientName, booking.getStartTime().toString(), doctorName, 1);
                    logComm(booking.getClinicId(), booking.getPatientId(), booking.getId(), cleanPhone,
                            CommunicationLog.MessageType.APPOINTMENT_REMINDER_1H,
                            "1h Reminder sent for appointment at " + booking.getStartTime() + " with " + doctorName);
                    log.info("Sent 1h reminder to {} for booking {}", patientPhone, booking.getBookingNumber());
                }
            } catch (Exception e) {
                log.error("Error sending 1h reminder for booking {}", booking.getId(), e);
            }
        }
    }

    /**
     * Sends automated doctor-recommended revisit reminder for follow-ups due in 2 days
     * Runs daily at 10:00 AM
     */
    @Scheduled(cron = "0 0 10 * * ?")
    public void sendRevisitReminders() {
        log.info("Sending dynamic follow-up revisit reminders");
        LocalDate targetDate = LocalDate.now().plusDays(2);

        List<FollowUp> dueFollowUps = followUpRepository.findAll().stream()
                .filter(f -> targetDate.equals(f.getDueDate()) && f.getStatus() == FollowUp.FollowUpStatus.PENDING)
                .toList();

        for (FollowUp followUp : dueFollowUps) {
            try {
                Patient patient = patientRepository.findById(followUp.getPatientId()).orElse(null);
                if (patient != null && patient.getPhone() != null) {
                    String cleanPhone = formatWhatsAppPhone(patient.getPhone());
                    String doctorName = getDoctorName(followUp.getDoctorId());

                    Map<String, String> vars = new HashMap<>();
                    vars.put("patient_name", patient.getName());
                    vars.put("doctor_name", doctorName);
                    vars.put("date", followUp.getDueDate().toString());
                    vars.put("reason", followUp.getReason() != null ? followUp.getReason() : "Clinical checkup");

                    String rendered = templateService.renderMessage(followUp.getClinicId(), "FOLLOW_UP", vars);
                    String message = (rendered != null) ? rendered : String.format(
                            "Hello %s! This is a reminder from your clinic that Dr. %s recommended a follow-up visit on %s (%s). Please let us know if you would like to book a slot.",
                            patient.getName(), doctorName, followUp.getDueDate(), followUp.getReason() != null ? followUp.getReason() : "Clinical checkup"
                    );
                    whatsAppService.sendTextMessage(cleanPhone, message);
                    logComm(followUp.getClinicId(), followUp.getPatientId(), null, cleanPhone,
                            CommunicationLog.MessageType.FOLLOW_UP_RECALL, message);
                    log.info("Sent revisit reminder to {} for FollowUp #{}", cleanPhone, followUp.getId());
                }
            } catch (Exception e) {
                log.error("Error sending revisit reminder for FollowUp #{}", followUp.getId(), e);
            }
        }
    }

    /**
     * Sends payment link after booking confirmation
     */
    public void sendPaymentLinkNotification(Long bookingId, String paymentLink) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

            String patientPhone = getPatientPhone(booking);
            String patientName = getPatientName(booking);

            if (patientPhone != null && !patientPhone.isEmpty()) {
                String cleanPhone = formatWhatsAppPhone(patientPhone);
                whatsAppService.sendPaymentLink(cleanPhone, patientName, paymentLink);
                logComm(booking.getClinicId(), booking.getPatientId(), bookingId, cleanPhone,
                        CommunicationLog.MessageType.PAYMENT_LINK, "Payment link sent: " + paymentLink);
                log.info("Sent payment link for booking {}", bookingId);
            }
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
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

            List<Invoice> invoices = invoiceRepository.findByPaymentId(paymentId);
            if (invoices.isEmpty()) {
                log.warn("No invoice found for payment {}", paymentId);
                return;
            }

            Invoice invoice = invoices.get(0);
            String invoiceUrl = "http://localhost:8080/api/payments/invoices/" + invoice.getInvoiceNumber() + "/pdf";

            Booking booking = payment.getBookingId() != null 
                    ? bookingRepository.findById(payment.getBookingId()).orElse(null) 
                    : null;

            String patientPhone = booking != null ? getPatientPhone(booking) : null;
            String patientName = booking != null ? getPatientName(booking) : "Patient";

            if (patientPhone != null && !patientPhone.isEmpty()) {
                String cleanPhone = formatWhatsAppPhone(patientPhone);
                whatsAppService.sendInvoice(cleanPhone, patientName, invoiceUrl);
                logComm(booking != null ? booking.getClinicId() : 1L, 
                        booking != null ? booking.getPatientId() : null, 
                        payment.getBookingId(), cleanPhone,
                        CommunicationLog.MessageType.INVOICE, "Invoice PDF delivered: " + invoiceUrl);
                log.info("Sent invoice for payment {}", paymentId);
            }
        } catch (Exception e) {
            log.error("Error sending invoice notification for payment {}", paymentId, e);
        }
    }

    /**
     * Sends booking confirmation notification
     */
    public void sendBookingConfirmation(Booking booking) {
        try {
            String patientPhone = getPatientPhone(booking);
            String patientName = getPatientName(booking);
            String doctorName = getDoctorName(booking.getDoctorId());

            if (patientPhone != null && !patientPhone.isEmpty()) {
                String cleanPhone = formatWhatsAppPhone(patientPhone);

                Map<String, String> vars = new HashMap<>();
                vars.put("patient_name", patientName);
                vars.put("doctor_name", doctorName);
                vars.put("date", booking.getBookingDate().toString());
                vars.put("time", booking.getStartTime().toString());
                vars.put("booking_id", booking.getBookingNumber() != null ? booking.getBookingNumber() : booking.getId().toString());

                String rendered = templateService.renderMessage(booking.getClinicId(), "CONFIRMATION", vars);
                if (rendered != null) {
                    whatsAppService.sendTextMessage(cleanPhone, rendered);
                    logComm(booking.getClinicId(), booking.getPatientId(), booking.getId(), cleanPhone,
                            CommunicationLog.MessageType.APPOINTMENT_CONFIRMATION, rendered);
                } else {
                    whatsAppService.sendBookingConfirmation(
                            cleanPhone,
                            patientName,
                            booking.getBookingDate().toString(),
                            booking.getStartTime().toString(),
                            doctorName
                    );
                    logComm(booking.getClinicId(), booking.getPatientId(), booking.getId(), cleanPhone,
                            CommunicationLog.MessageType.APPOINTMENT_CONFIRMATION,
                            "Booking confirmed for " + booking.getBookingDate() + " at " + booking.getStartTime() + " with " + doctorName);
                }
                log.info("Sent booking confirmation for booking {}", booking.getBookingNumber());
            }
        } catch (Exception e) {
            log.error("Error sending booking confirmation for booking {}", booking.getId(), e);
        }
    }

    /**
     * Sends Google Review / clinic feedback request to patient
     */
    public void sendReviewRequest(Long clinicId, Long patientId, String phone, String patientName, String doctorName) {
        try {
            if (!hasConsent(patientId, PatientConsent.ConsentType.PROMOTIONAL)) {
                log.info("Skipping review request: Patient {} has revoked feedback/promo consent", patientId);
                return;
            }
            String cleanPhone = formatWhatsAppPhone(phone);
            Map<String, String> vars = new HashMap<>();
            vars.put("patient_name", patientName);
            vars.put("doctor_name", doctorName);

            String rendered = templateService.renderMessage(clinicId, "REVIEW_REQUEST", vars);
            if (rendered != null) {
                whatsAppService.sendTextMessage(cleanPhone, rendered);
                logComm(clinicId, patientId, null, cleanPhone,
                        CommunicationLog.MessageType.FEEDBACK_REQUEST, rendered);
                log.info("Sent Google review request to {}", cleanPhone);
            }
        } catch (Exception e) {
            log.error("Error sending review request to patient {}", patientId, e);
        }
    }

    /**
     * Logs a communication event in the audit trail
     */
    public void logComm(Long clinicId, Long patientId, Long bookingId, String phone,
                        CommunicationLog.MessageType type, String body) {
        try {
            CommunicationLog entry = CommunicationLog.builder()
                    .clinicId(clinicId != null ? clinicId : 1L)
                    .patientId(patientId)
                    .bookingId(bookingId)
                    .recipientPhone(phone)
                    .channel(CommunicationLog.CommChannel.WHATSAPP)
                    .messageType(type)
                    .messageBody(body)
                    .status(CommunicationLog.DeliveryStatus.SENT)
                    .sentAt(LocalDateTime.now())
                    .build();
            communicationLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to persist communication log", e);
        }
    }

    private boolean hasConsent(Long patientId, PatientConsent.ConsentType type) {
        if (patientId == null) return true; // Default allow for unregistered/walk-ins
        return consentRepository.findByPatientIdAndConsentType(patientId, type)
                .map(PatientConsent::getIsGranted)
                .orElse(true); // Default true unless explicitly withdrawn
    }

    private String getPatientPhone(Booking booking) {
        if (booking.getPatientPhone() != null && !booking.getPatientPhone().isEmpty()) {
            return booking.getPatientPhone();
        }
        if (booking.getPatientId() != null) {
            return patientRepository.findById(booking.getPatientId())
                    .map(Patient::getPhone)
                    .orElse(null);
        }
        return null;
    }

    private String getPatientName(Booking booking) {
        if (booking.getPatientName() != null && !booking.getPatientName().isEmpty()) {
            return booking.getPatientName();
        }
        if (booking.getPatientId() != null) {
            return patientRepository.findById(booking.getPatientId())
                    .map(Patient::getName)
                    .orElse("Patient");
        }
        return "Patient";
    }

    private String getDoctorName(Long doctorId) {
        if (doctorId == null) return "Consulting Physician";
        return userRepository.findById(doctorId)
                .map(u -> (u.getName() != null ? u.getName() : "Dr. " + u.getUsername()))
                .orElse("Consulting Physician");
    }

    private String formatWhatsAppPhone(String phone) {
        String clean = phone.replace("+", "").replaceAll("\\s+", "");
        if (!clean.startsWith("whatsapp:")) {
            clean = "whatsapp:+" + (clean.startsWith("91") ? clean : "91" + clean);
        }
        return clean;
    }
}
