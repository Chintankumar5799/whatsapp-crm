package com.appointment.clinical.controller;

import com.appointment.booking.model.Booking;
import com.appointment.clinical.model.FollowUp;
import com.appointment.clinical.service.FollowUpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/clinical/followups")
@RequiredArgsConstructor
@Tag(name = "Follow-up Management", description = "Patient retention worklist, calling history, and revisit booking")
@CrossOrigin(origins = "http://localhost:3000")
public class FollowUpController {

    private final FollowUpService followUpService;

    @GetMapping("/worklist")
    @Operation(summary = "Get follow-up retention worklist (due_today, upcoming, overdue, all)")
    public ResponseEntity<List<FollowUpService.FollowUpWorklistItem>> getWorklist(
            @RequestParam(defaultValue = "1") Long clinicId,
            @RequestParam(defaultValue = "due_today") String filter
    ) {
        return ResponseEntity.ok(followUpService.getWorklist(clinicId, filter));
    }

    @PostMapping("/{id}/call-attempt")
    @Operation(summary = "Record receptionist call attempt and outcome status")
    public ResponseEntity<FollowUp> recordCallAttempt(
            @PathVariable Long id,
            @RequestParam FollowUp.ContactStatus contactStatus,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) Long staffId
    ) {
        return ResponseEntity.ok(followUpService.recordCallAttempt(id, contactStatus, notes, staffId));
    }

    @PostMapping("/{id}/convert")
    @Operation(summary = "Convert follow-up into a confirmed revisit appointment")
    public ResponseEntity<Booking> convertToAppointment(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time
    ) {
        return ResponseEntity.ok(followUpService.convertToAppointment(id, date, time));
    }
}

