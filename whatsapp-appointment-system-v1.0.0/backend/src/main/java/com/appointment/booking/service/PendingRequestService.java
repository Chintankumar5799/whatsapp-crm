package com.appointment.booking.service;

import com.appointment.booking.model.Booking;
import com.appointment.booking.model.Booking.BookingStatus;
import com.appointment.booking.model.PendingAppointmentRequest;
import com.appointment.booking.repository.BookingRepository;
import com.appointment.booking.repository.PendingAppointmentRequestRepository;
import com.appointment.auth.repository.UserRepository;
import com.appointment.auth.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PendingRequestService {

    private final PendingAppointmentRequestRepository requestRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public PendingAppointmentRequest createPendingRequest(String doctorPhone, String patientPhone, String patientName, LocalDate date, String time, String description, Long addressId) {
        PendingAppointmentRequest request = PendingAppointmentRequest.builder()
                .doctorPhone(doctorPhone)
                .patientPhone(patientPhone)
                .patientName(patientName)
                .requestedDate(date)
                .requestedStartTime(time)
                .description(description)
                .addressId(addressId)
                .expiresAt(java.time.LocalDateTime.now().plusMinutes(30))
                .build();
        return requestRepository.save(request);
    }

    public List<PendingAppointmentRequest> getPendingRequests(Long doctorId) {
        return requestRepository.findByDoctorId(doctorId);
    }

    @Transactional
    public Booking confirmRequest(Long requestId, Long doctorId, Integer durationMinutes, String remarks) {
        try {
            System.out.println("Confirming request: " + requestId + " for doctor: " + doctorId);
            PendingAppointmentRequest request = requestRepository.findById(requestId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

            User doctor = userRepository.findById(doctorId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));

            // Check conflicts (Simplified, call slot service ideally)
            LocalTime start = LocalTime.parse(request.getRequestedStartTime());
            LocalTime end = start.plusMinutes(durationMinutes != null ? durationMinutes : 30);
            
            List<Booking> conflicts = bookingRepository.findConflictingBookings(
                    doctorId, 
                    request.getRequestedDate(), 
                    start, 
                    end,
                    List.of(BookingStatus.ACCEPTED, BookingStatus.CONFIRMED, BookingStatus.PENDING)
            );
            if (!conflicts.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot conflict");
            }

            Booking booking = new Booking();
            booking.setDoctorId(doctorId);
            // Generate a booking number
            booking.setBookingNumber("BK-" + System.currentTimeMillis());
            
            // Find patient user by phone if exists? 
            // For now, booking entity might expect patientId.
            userRepository.findByPhone(request.getPatientPhone()).ifPresent(p -> booking.setPatientId(p.getId()));

            booking.setPatientName(request.getPatientName());
            booking.setPatientPhone(request.getPatientPhone());
            booking.setBookingDate(request.getRequestedDate());
            booking.setStartTime(start);
            booking.setEndTime(end);
            booking.setStatus(BookingStatus.ACCEPTED);
            booking.setDoctorNotes(remarks);
            booking.setAddressId(request.getAddressId());
            booking.setDiseaseDescription(request.getDescription());
            
            // Save booking
            Booking saved = bookingRepository.save(booking);
            
            // Remove request
            requestRepository.delete(request);
            
            return saved;
        } catch (Exception e) {
            System.err.println("Error confirming request: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Transactional
    public void rejectRequest(Long requestId, Long doctorId, String message) {
        PendingAppointmentRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        requestRepository.delete(request);
        // Could send notification here
    }
}
