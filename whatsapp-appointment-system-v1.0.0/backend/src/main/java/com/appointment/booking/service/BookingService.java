package com.appointment.booking.service;

import com.appointment.booking.model.Booking;
import com.appointment.booking.model.Booking.BookingStatus;
import com.appointment.booking.model.Slot;
import com.appointment.booking.repository.BookingRepository;
import com.appointment.payment.dto.PaymentLinkRequest;
import com.appointment.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {
    
    private final BookingRepository bookingRepository;
    private final SlotService slotService;
    private final SlotHoldService slotHoldService;
    private final PaymentService paymentService;
    
    /**
     * Creates a booking with a hold
     * @param doctorId Doctor ID
     * @param patientId Patient ID
     * @param slotId Slot ID
     * @param holdToken Hold token from Redis
     * @return Created booking
     */
    @Transactional
    public Booking createBookingWithHold(Long doctorId, Long patientId, Long slotId, String holdToken) {
        // Validate and consume the hold
        if (!slotHoldService.validateAndConsumeHold(slotId, holdToken, patientId)) {
            throw new IllegalStateException("Invalid or expired hold token");
        }
        
        // Get slot details
        Slot slot = slotService.bookSlot(slotId);
        
        // Create booking
        String bookingNumber = generateBookingNumber();
        Booking booking = Booking.builder()
                .bookingNumber(bookingNumber)
                .doctorId(doctorId)
                .patientId(patientId)
                .slotId(slotId)
                .bookingDate(slot.getSlotDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .status(BookingStatus.PENDING)
                .requiresPayment(true)
                .paymentStatus("PENDING")
                .build();
        
        booking = bookingRepository.save(booking);
        log.info("Booking created: {} for patient {} with doctor {}", 
                bookingNumber, patientId, doctorId);
        
        return booking;
    }
    
    /**
     * Confirms a booking (doctor approval)
     */
    @Transactional
    public Booking confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking cannot be confirmed in current status");
        }
        
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());
        booking = bookingRepository.save(booking);
        
        log.info("Booking {} confirmed by doctor", booking.getBookingNumber());
        
        // Generate payment link if payment is required
        if (booking.getRequiresPayment()) {
            generatePaymentLink(booking);
        }
        
        return booking;
    }
    
    /**
     * Rejects a booking
     */
    @Transactional
    public Booking rejectBooking(Long bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking cannot be rejected in current status");
        }
        
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setNotes(reason);
        
        // Release the slot
        if (booking.getSlotId() != null) {
            slotService.releaseSlot(booking.getSlotId());
        }
        
        booking = bookingRepository.save(booking);
        log.info("Booking {} rejected: {}", booking.getBookingNumber(), reason);
        
        return booking;
    }
    
    /**
     * Cancels a booking
     */
    @Transactional
    public Booking cancelBooking(Long bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setNotes(reason);
        
        // Release the slot
        if (booking.getSlotId() != null) {
            slotService.releaseSlot(booking.getSlotId());
        }
        
        booking = bookingRepository.save(booking);
        log.info("Booking {} cancelled: {}", booking.getBookingNumber(), reason);
        
        return booking;
    }
    
    /**
     * Gets bookings for a doctor on a specific date
     */
    public List<Booking> getDoctorBookingsForDate(Long doctorId, LocalDate date) {
        return bookingRepository.findByDoctorIdAndDateAndStatusIn(
                doctorId, date, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));
    }
    
    /**
     * Gets pending bookings for a doctor
     */
    public List<Booking> getPendingBookings(Long doctorId) {
        return bookingRepository.findByDoctorIdAndStatus(doctorId, BookingStatus.PENDING);
    }
    
    /**
     * Gets a booking by ID
     */
    public Optional<Booking> getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId);
    }
    
    /**
     * Gets a booking by booking number
     */
    public Optional<Booking> getBookingByNumber(String bookingNumber) {
        return bookingRepository.findByBookingNumber(bookingNumber);
    }
    
    private String generateBookingNumber() {
        String timestamp = LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "BK-" + timestamp + "-" + uniqueId;
    }
    
    private void generatePaymentLink(Booking booking) {
        try {
            // TODO: Get patient and doctor details
            PaymentLinkRequest request = PaymentLinkRequest.builder()
                    .bookingId(booking.getId())
                    .patientId(booking.getPatientId())
                    .amount(java.math.BigDecimal.valueOf(500.00)) // TODO: Get from doctor/service
                    .currency("INR")
                    .description("Consultation fee for booking " + booking.getBookingNumber())
                    .customerName("Patient") // TODO: Get from patient
                    .customerEmail("patient@example.com") // TODO: Get from patient
                    .customerPhone("+1234567890") // TODO: Get from patient
                    .build();
            
            paymentService.createPaymentLink(request);
        } catch (Exception e) {
            log.error("Error generating payment link for booking {}", booking.getId(), e);
        }
    }
}

