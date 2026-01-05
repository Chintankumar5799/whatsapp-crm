package com.appointment.booking.service;

import com.appointment.booking.model.Booking;
import com.appointment.booking.model.Booking.BookingStatus;
import com.appointment.booking.model.Slot;
import com.appointment.booking.repository.BookingRepository;
import com.appointment.payment.dto.PaymentLinkRequest;
import com.appointment.payment.service.PaymentService;
import com.appointment.auth.repository.UserRepository;
import com.appointment.booking.dto.PatientBookingDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {
    
    private final BookingRepository bookingRepository;
    private final SlotService slotService;
    private final SlotHoldService slotHoldService;
    private final PaymentService paymentService;
    private final UserRepository userRepository;
    
    public BookingRepository getBookingRepository() {
        return bookingRepository;
    }

    @Transactional
    public Booking createBookingWithHold(Long doctorId, Long patientId, Long slotId, String holdToken) {
        if (!slotHoldService.validateAndConsumeHold(slotId, holdToken, patientId)) {
            throw new IllegalStateException("Invalid or expired hold token");
        }
        Slot slot = slotService.bookSlot(slotId);
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
        log.info("Booking created: {} for patient {} with doctor {}", bookingNumber, patientId, doctorId);
        return booking;
    }
    
    @Transactional
    public Booking confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        
        if (booking.getStatus() != BookingStatus.PENDING) {
             // Allow re-confirmation if needed or throw
             // throw new IllegalStateException("Booking cannot be confirmed in current status");
        }
        
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());
        booking = bookingRepository.save(booking);
        
        if (Boolean.TRUE.equals(booking.getRequiresPayment())) {
            generatePaymentLink(booking);
        }
        return booking;
    }
    
    @Transactional
    public Booking rejectBooking(Long bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setNotes(reason);
        if (booking.getSlotId() != null) {
            slotService.releaseSlot(booking.getSlotId());
        }
        return bookingRepository.save(booking);
    }
    
    @Transactional
    public Booking cancelBooking(Long bookingId, String reason) {
        return rejectBooking(bookingId, reason);
    }
    
    public List<Booking> getDoctorBookingsForDate(Long doctorId, LocalDate date) {
        // Return all bookings for dashboard visibility (including COMPLETED/PAID)
        return bookingRepository.findByDoctorIdAndDateAndStatusIn(
                doctorId, date, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.ACCEPTED, BookingStatus.COMPLETED, BookingStatus.PAID));
    }
    
    public List<Booking> getPendingBookings(Long doctorId) {
        return bookingRepository.findByDoctorIdAndStatus(doctorId, BookingStatus.PENDING);
    }
    
    public Optional<Booking> getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId);
    }
    
    public Optional<Booking> getBookingByNumber(String bookingNumber) {
        return bookingRepository.findByBookingNumber(bookingNumber);
    }
    
    private String generateBookingNumber() {
        String timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "BK-" + timestamp + "-" + uniqueId;
    }
    
    private void generatePaymentLink(Booking booking) {
        try {
            PaymentLinkRequest request = PaymentLinkRequest.builder()
                    .bookingId(booking.getId())
                    .patientId(booking.getPatientId())
                    .amount(java.math.BigDecimal.valueOf(500.00))
                    .currency("INR")
                    .description("Consultation fee for booking " + booking.getBookingNumber())
                    // TODO: fetch customer details
                    .build();
            paymentService.createPaymentLink(request);
        } catch (Exception e) {
            log.error("Error generating payment link", e);
        }
    }
    
    @Transactional
    public Booking completeAppointment(Long bookingId, String notes) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());
        booking.setDoctorNotes(notes);
        return bookingRepository.save(booking);
    }

    public List<PatientBookingDTO> getPatientBookingsDTO(Long patientId) {
        List<Booking> bookings = bookingRepository.findByPatientId(patientId);
        return bookings.stream().map(b -> {
             String doctorName = "Unknown";
             if(b.getDoctorId() != null) {
                 doctorName = userRepository.findById(b.getDoctorId())
                         .map(u -> u.getName() != null ? u.getName() : u.getUsername())
                         .orElse("Unknown");
             }
             return PatientBookingDTO.builder()
                 .id(b.getId())
                 .doctorId(b.getDoctorId())
                 .doctorName(doctorName)
                 .bookingDate(b.getBookingDate())
                 .startTime(b.getStartTime() != null ? b.getStartTime().toString() : null)
                 .status(b.getStatus().name())
                 .paymentLink(b.getPaymentLinkUrl())
                 .totalAmount(b.getTotalAmount())
                 .build();
        }).collect(Collectors.toList());
    }
}
