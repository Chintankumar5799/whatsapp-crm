package com.appointment.booking.controller;

import com.appointment.booking.dto.BookingRequest;
import com.appointment.booking.dto.BookingResponse;
import com.appointment.booking.model.Booking;
import com.appointment.booking.model.Slot;
import com.appointment.booking.service.BookingService;
import com.appointment.booking.service.SlotHoldService;
import com.appointment.booking.service.SlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking", description = "Booking management APIs")
public class BookingController {
    
    private final BookingService bookingService;
    private final SlotService slotService;
    private final SlotHoldService slotHoldService;
    
    @GetMapping("/slots/available")
    @Operation(summary = "Get available slots", description = "Gets available slots for a doctor on a specific date")
    public ResponseEntity<List<Slot>> getAvailableSlots(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Slot> slots = slotService.getAvailableSlots(doctorId, date);
        return ResponseEntity.ok(slots);
    }
    
    @PostMapping("/slots/{slotId}/hold")
    @Operation(summary = "Create slot hold", description = "Creates a temporary hold on a slot")
    public ResponseEntity<HoldResponse> createHold(
            @PathVariable Long slotId,
            @RequestParam Long patientId) {
        String holdToken = slotHoldService.createHold(slotId, patientId);
        return ResponseEntity.ok(new HoldResponse(holdToken, slotId));
    }
    
    @PostMapping("/create")
    @Operation(summary = "Create booking", description = "Creates a booking using a hold token")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        Booking booking = bookingService.createBookingWithHold(
                request.getDoctorId(),
                request.getPatientId(),
                request.getSlotId(),
                request.getHoldToken()
        );
        return ResponseEntity.ok(mapToResponse(booking));
    }
    
    @PostMapping("/{bookingId}/confirm")
    @Operation(summary = "Confirm booking", description = "Confirms a pending booking (doctor approval)")
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable Long bookingId) {
        Booking booking = bookingService.confirmBooking(bookingId);
        return ResponseEntity.ok(mapToResponse(booking));
    }
    
    @PostMapping("/{bookingId}/reject")
    @Operation(summary = "Reject booking", description = "Rejects a pending booking")
    public ResponseEntity<BookingResponse> rejectBooking(
            @PathVariable Long bookingId,
            @RequestParam(required = false) String reason) {
        Booking booking = bookingService.rejectBooking(bookingId, reason);
        return ResponseEntity.ok(mapToResponse(booking));
    }
    
    @GetMapping("/doctor/{doctorId}/date/{date}")
    @Operation(summary = "Get doctor bookings for date", description = "Gets all bookings for a doctor on a specific date")
    public ResponseEntity<List<BookingResponse>> getDoctorBookingsForDate(
            @PathVariable Long doctorId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Booking> bookings = bookingService.getDoctorBookingsForDate(doctorId, date);
        return ResponseEntity.ok(bookings.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }
    
    @GetMapping("/doctor/{doctorId}/pending")
    @Operation(summary = "Get pending bookings", description = "Gets all pending bookings for a doctor")
    public ResponseEntity<List<BookingResponse>> getPendingBookings(@PathVariable Long doctorId) {
        List<Booking> bookings = bookingService.getPendingBookings(doctorId);
        return ResponseEntity.ok(bookings.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }
    
    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .doctorId(booking.getDoctorId())
                .patientId(booking.getPatientId())
                .slotId(booking.getSlotId())
                .bookingDate(booking.getBookingDate())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .status(booking.getStatus().name())
                .notes(booking.getNotes())
                .createdAt(booking.getCreatedAt())
                .build();
    }
    
    public record HoldResponse(String holdToken, Long slotId) {}
}

