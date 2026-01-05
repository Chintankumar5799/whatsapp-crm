package com.appointment.booking.controller;

import com.appointment.booking.dto.PatientBookingRequest;
import com.appointment.booking.dto.DoctorLookupResponse;
import com.appointment.booking.model.PendingAppointmentRequest;
import com.appointment.booking.model.Slot;
import com.appointment.booking.service.PendingRequestService;
import com.appointment.booking.service.SlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/patient/bookings")
@RequiredArgsConstructor
@Tag(name = "Patient Booking", description = "Patient-facing booking APIs")
@CrossOrigin(origins = "http://localhost:3000")
public class PatientBookingController {
    
    private final com.appointment.auth.repository.UserRepository userRepository;
    private final com.appointment.booking.repository.SpecializationRepository specializationRepository;
    private final SlotService slotService;
    private final PendingRequestService pendingRequestService;
    private final com.appointment.booking.service.BookingService bookingService;
    
    @GetMapping("/specializations")
    @Operation(summary = "Get all filters", description = "Returns available specializations and unique qualifications")
    public ResponseEntity<Map<String, Object>> getFilters() {
        Map<String, Object> response = new HashMap<>();
        response.put("specializations", specializationRepository.findAll());
        
        List<String> qualifications = userRepository.findByRole(com.appointment.auth.model.User.UserRole.DOCTOR)
                .stream()
                .map(com.appointment.auth.model.User::getQualification)
                .filter(q -> q != null && !q.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        response.put("qualifications", qualifications);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/doctors")
    @Operation(summary = "List all doctors", description = "Returns a list of doctors, optionally filtered by specialization or qualification")
    public ResponseEntity<List<DoctorLookupResponse.DoctorInfo>> listDoctors(
            @RequestParam(required = false) Long specializationId,
            @RequestParam(required = false) String qualification) {
        
        List<com.appointment.auth.model.User> doctors = userRepository.findByRole(com.appointment.auth.model.User.UserRole.DOCTOR);
        
        List<DoctorLookupResponse.DoctorInfo> doctorInfos = doctors.stream()
                .filter(d -> specializationId == null || (d.getSpecializationId() != null && d.getSpecializationId().equals(specializationId)))
                .filter(d -> qualification == null || (d.getQualification() != null && d.getQualification().equalsIgnoreCase(qualification)))
                .map(doctor -> {
                    String specName = "General";
                    if (doctor.getSpecializationId() != null) {
                        specName = specializationRepository.findById(doctor.getSpecializationId())
                                .map(com.appointment.booking.model.Specialization::getName)
                                .orElse("General");
                    }
                    
                    return DoctorLookupResponse.DoctorInfo.builder()
                            .id(doctor.getId())
                            .name(doctor.getName() != null ? doctor.getName() : doctor.getUsername())
                            .phone(doctor.getPhone())
                            .specialization(specName)
                            .qualification(doctor.getQualification() != null ? doctor.getQualification() : "-")
                            .build();
                }).collect(Collectors.toList());

        return ResponseEntity.ok(doctorInfos);
    }

    @GetMapping("/doctor/lookup")
    @Operation(summary = "Lookup doctor by phone", description = "Returns doctor info for booking")
    public ResponseEntity<DoctorLookupResponse> lookupDoctor(@RequestParam String phone) {
        Optional<com.appointment.auth.model.User> doctorOpt = userRepository.findByPhone(phone);
        
        if (doctorOpt.isEmpty() || doctorOpt.get().getRole() != com.appointment.auth.model.User.UserRole.DOCTOR) {
            return ResponseEntity.notFound().build();
        }
        
        com.appointment.auth.model.User doctor = doctorOpt.get();
        String specName = "General";
        if (doctor.getSpecializationId() != null) {
            specName = specializationRepository.findById(doctor.getSpecializationId())
                    .map(com.appointment.booking.model.Specialization::getName)
                    .orElse("General");
        }

        return ResponseEntity.ok(DoctorLookupResponse.builder()
                .ambiguous(false)
                .doctor(DoctorLookupResponse.DoctorInfo.builder()
                        .id(doctor.getId())
                        .name(doctor.getName() != null ? doctor.getName() : doctor.getUsername())
                        .phone(doctor.getPhone())
                        .specialization(specName)
                        .qualification(doctor.getQualification() != null ? doctor.getQualification() : "-")
                        .build())
                .build());
    }
    
    @GetMapping("/slots/available")
    @Operation(summary = "Get available slots", description = "Gets available slots for a doctor on a date")
    public ResponseEntity<List<Slot>> getAvailableSlots(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Slot> slots = slotService.getAvailableSlots(doctorId, date);
        return ResponseEntity.ok(slots);
    }
    
    @PostMapping("/request")
    @Operation(summary = "Create pending appointment request")
    public ResponseEntity<PendingAppointmentRequest> createRequest(
            @RequestBody PatientBookingRequest request) {
        try {
            PendingAppointmentRequest pendingRequest = pendingRequestService.createPendingRequest(
                    request.getDoctorPhone(),
                    request.getPatientPhone(),
                    request.getPatientName(),
                    request.getRequestedDate(),
                    request.getRequestedStartTime(),
                    request.getDescription(),
                    request.getAddressId());
            return ResponseEntity.ok(pendingRequest);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Booking failed: " + e.getMessage(), e);
        }
    }

    @GetMapping("/list")
    @Operation(summary = "List bookings for a patient")
    public ResponseEntity<List<com.appointment.booking.dto.PatientBookingDTO>> listBookings(@RequestParam Long patientId) {
        return ResponseEntity.ok(bookingService.getPatientBookingsDTO(patientId));
    }
}
