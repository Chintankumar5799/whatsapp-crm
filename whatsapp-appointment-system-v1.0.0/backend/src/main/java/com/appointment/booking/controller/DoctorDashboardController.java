package com.appointment.booking.controller;

import com.appointment.booking.dto.ConfirmRequestDto;
import com.appointment.booking.dto.RejectRequestDto;
import com.appointment.booking.dto.DashboardMetricsDto;
import com.appointment.booking.model.Booking;
import com.appointment.booking.model.PendingAppointmentRequest;
import com.appointment.booking.service.BookingService;
import com.appointment.booking.service.PendingRequestService;
import com.appointment.booking.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/doctor/dashboard")
@RequiredArgsConstructor
@Tag(name = "Doctor Dashboard", description = "Doctor dashboard APIs")
@CrossOrigin(origins = "http://localhost:3000")
public class DoctorDashboardController {
    
    private final PendingRequestService pendingRequestService;
    private final BookingService bookingService;
    private final MetricsService metricsService;
    
    @GetMapping("/pending-requests")
    @Operation(summary = "Get pending appointment requests")
    public ResponseEntity<List<PendingAppointmentRequest>> getPendingRequests(
            @RequestParam Long doctorId) {
        List<PendingAppointmentRequest> requests = pendingRequestService.getPendingRequests(doctorId);
        return ResponseEntity.ok(requests);
    }
    
    @PostMapping("/requests/{requestId}/confirm")
    @Operation(summary = "Confirm a pending request")
    public ResponseEntity<Booking> confirmRequest(
            @PathVariable Long requestId,
            @RequestBody ConfirmRequestDto dto) {
        Booking booking = pendingRequestService.confirmRequest(
                requestId, dto.getDoctorId(), dto.getDurationMinutes(), dto.getRemarks());
        return ResponseEntity.ok(booking);
    }
    
    @PostMapping("/requests/{requestId}/reject")
    @Operation(summary = "Reject a pending request")
    public ResponseEntity<Void> rejectRequest(
            @PathVariable Long requestId,
            @RequestBody RejectRequestDto dto) {
        pendingRequestService.rejectRequest(requestId, dto.getDoctorId(), dto.getMessage());
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/bookings")
    @Operation(summary = "Get bookings for a date")
    public ResponseEntity<List<Booking>> getBookingsForDate(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Booking> bookings = bookingService.getDoctorBookingsForDate(doctorId, date);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/patients/search")
    @Operation(summary = "Search patient history by phone")
    public ResponseEntity<List<Booking>> searchPatientBookings(
            @RequestParam String phone) {
        // Using the new repository method.
        List<Booking> history = bookingService.getBookingRepository().findByPatientPhoneOrderByBookingDateDesc(phone);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/metrics")
    @Operation(summary = "Get dashboard metrics")
    public ResponseEntity<DashboardMetricsDto> getMetrics(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DashboardMetricsDto metrics = metricsService.getDashboardMetrics(doctorId, date);
        return ResponseEntity.ok(metrics);
    }
    
    @GetMapping("/metrics/charts")
    @Operation(summary = "Get chart data")
    public ResponseEntity<DashboardMetricsDto.ChartData> getChartData(
            @RequestParam Long doctorId,
            @RequestParam String period, // "daily" or "weekly"
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        DashboardMetricsDto.ChartData chartData = metricsService.getChartData(
                doctorId, period, startDate != null ? startDate : LocalDate.now());
        return ResponseEntity.ok(chartData);
    }

    @PostMapping("/bookings/{bookingId}/complete")
    @Operation(summary = "Mark appointment as completed")
    public ResponseEntity<Booking> completeAppointment(
            @PathVariable Long bookingId,
            @RequestBody String notes) {
        Booking booking = bookingService.completeAppointment(bookingId, notes);
        return ResponseEntity.ok(booking);
    }
}
