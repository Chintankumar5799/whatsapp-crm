package com.appointment.booking.controller;

import com.appointment.booking.model.Availability;
import com.appointment.booking.model.AvailabilityException;
import com.appointment.booking.repository.AvailabilityExceptionRepository;
import com.appointment.booking.repository.AvailabilityRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/availability")
@RequiredArgsConstructor
@Tag(name = "Doctor Availability & Schedules", description = "Weekly shifts, slot durations, and leave exceptions")
@CrossOrigin(origins = "http://localhost:3000")
public class AvailabilityController {

    private final AvailabilityRepository availabilityRepository;
    private final AvailabilityExceptionRepository exceptionRepository;

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Get all weekly schedule slots for a doctor")
    public ResponseEntity<List<Availability>> getDoctorAvailability(@PathVariable Long doctorId) {
        return ResponseEntity.ok(availabilityRepository.findByDoctorId(doctorId));
    }

    @PostMapping
    @Operation(summary = "Create or update doctor weekly availability slot")
    public ResponseEntity<Availability> saveAvailability(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") != null ? Long.valueOf(body.get("id").toString()) : null;
        Long doctorId = Long.valueOf(body.get("doctorId").toString());
        DayOfWeek dayOfWeek = DayOfWeek.valueOf(body.get("dayOfWeek").toString().toUpperCase());
        LocalTime startTime = LocalTime.parse(body.get("startTime").toString());
        LocalTime endTime = LocalTime.parse(body.get("endTime").toString());
        Integer slotDuration = body.get("slotDurationMinutes") != null 
                ? Integer.valueOf(body.get("slotDurationMinutes").toString()) 
                : 30;
        Boolean isActive = body.get("isActive") != null 
                ? Boolean.valueOf(body.get("isActive").toString()) 
                : true;

        Availability slot;
        if (id != null) {
            slot = availabilityRepository.findById(id).orElse(new Availability());
        } else {
            slot = new Availability();
        }

        slot.setDoctorId(doctorId);
        slot.setDayOfWeek(dayOfWeek);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        slot.setSlotDurationMinutes(slotDuration);
        slot.setIsActive(isActive);

        log.info("Saved doctor {} availability for {}: {} - {} ({} min slots)", 
                doctorId, dayOfWeek, startTime, endTime, slotDuration);
        return ResponseEntity.ok(availabilityRepository.save(slot));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an availability slot")
    public ResponseEntity<Void> deleteAvailability(@PathVariable Long id) {
        availabilityRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // --- Exceptions & Leaves ---

    @GetMapping("/exceptions/{doctorId}")
    @Operation(summary = "Get holiday exceptions and leaves for doctor")
    public ResponseEntity<List<AvailabilityException>> getExceptions(@PathVariable Long doctorId) {
        return ResponseEntity.ok(exceptionRepository.findByDoctorId(doctorId));
    }

    @PostMapping("/exceptions")
    @Operation(summary = "Add holiday exception / leave block for doctor")
    public ResponseEntity<AvailabilityException> addException(@RequestBody Map<String, Object> body) {
        Long doctorId = Long.valueOf(body.get("doctorId").toString());
        LocalDate date = LocalDate.parse(body.get("exceptionDate").toString());
        String typeStr = body.getOrDefault("exceptionType", "BLOCKED").toString();
        AvailabilityException.ExceptionType type = AvailabilityException.ExceptionType.valueOf(typeStr);
        String reason = (String) body.getOrDefault("reason", "Doctor on leave / holiday");

        LocalTime startTime = body.get("startTime") != null ? LocalTime.parse(body.get("startTime").toString()) : null;
        LocalTime endTime = body.get("endTime") != null ? LocalTime.parse(body.get("endTime").toString()) : null;

        AvailabilityException exception = AvailabilityException.builder()
                .doctorId(doctorId)
                .exceptionDate(date)
                .exceptionType(type)
                .reason(reason)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        log.info("Added exception for doctor {} on {}: {}", doctorId, date, reason);
        return ResponseEntity.ok(exceptionRepository.save(exception));
    }

    @DeleteMapping("/exceptions/{id}")
    @Operation(summary = "Delete an exception / leave")
    public ResponseEntity<Void> deleteException(@PathVariable Long id) {
        exceptionRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}

