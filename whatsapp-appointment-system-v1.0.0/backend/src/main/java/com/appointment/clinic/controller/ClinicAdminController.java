package com.appointment.clinic.controller;

import com.appointment.clinic.model.Clinic;
import com.appointment.clinic.repository.ClinicRepository;
import com.appointment.clinic.service.ClinicAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/clinics")
@RequiredArgsConstructor
@Tag(name = "SaaS Clinic Administration", description = "Multi-clinic onboarding, subscription tiers, and plan enforcement")
@CrossOrigin(origins = "http://localhost:3000")
public class ClinicAdminController {

    private final ClinicAdminService clinicAdminService;
    private final ClinicRepository clinicRepository;

    @GetMapping
    @Operation(summary = "List all clinics with overview and subscription status")
    public ResponseEntity<List<ClinicAdminService.ClinicOverviewDto>> getAllClinics() {
        return ResponseEntity.ok(clinicAdminService.getAllClinicsOverview());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get clinic details by ID")
    public ResponseEntity<Clinic> getClinicById(@PathVariable Long id) {
        return ResponseEntity.ok(clinicRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Clinic not found: " + id)));
    }

    @PostMapping
    @Operation(summary = "Onboard a new clinic account")
    public ResponseEntity<Clinic> registerClinic(@RequestBody Clinic clinic) {
        return ResponseEntity.ok(clinicAdminService.registerNewClinic(clinic));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update clinic operational settings and letterhead")
    public ResponseEntity<Clinic> updateClinicSettings(@PathVariable Long id, @RequestBody Clinic clinic) {
        return ResponseEntity.ok(clinicAdminService.updateClinicSettings(id, clinic));
    }

    @PutMapping("/{id}/subscription")
    @Operation(summary = "Update clinic subscription tier and staff limits")
    public ResponseEntity<Clinic> updateSubscription(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        String plan = (String) body.get("subscriptionPlan");
        String status = (String) body.get("subscriptionStatus");
        Integer maxDocs = body.get("maxDoctors") != null ? Integer.valueOf(body.get("maxDoctors").toString()) : null;
        Integer maxStaff = body.get("maxStaff") != null ? Integer.valueOf(body.get("maxStaff").toString()) : null;
        Integer extendDays = body.get("extendDays") != null ? Integer.valueOf(body.get("extendDays").toString()) : null;

        return ResponseEntity.ok(clinicAdminService.updateSubscription(id, plan, status, maxDocs, maxStaff, extendDays));
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Get clinic operational statistics")
    public ResponseEntity<Map<String, Object>> getClinicStats(@PathVariable Long id) {
        return ResponseEntity.ok(clinicAdminService.getClinicStats(id));
    }

    @GetMapping("/{id}/staff")
    @Operation(summary = "List all staff (Doctors & Receptionists) for a clinic")
    public ResponseEntity<List<ClinicAdminService.StaffSummaryDto>> getClinicStaff(@PathVariable Long id) {
        return ResponseEntity.ok(clinicAdminService.getClinicStaff(id));
    }

    @PostMapping("/{id}/staff")
    @Operation(summary = "Create a new Doctor or Receptionist account for a clinic")
    public ResponseEntity<ClinicAdminService.StaffSummaryDto> createClinicStaff(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.ok(clinicAdminService.createClinicStaff(id, body));
    }

    @PutMapping("/staff/{userId}/status")
    @Operation(summary = "Toggle active/inactive status for a staff member")
    public ResponseEntity<ClinicAdminService.StaffSummaryDto> toggleStaffStatus(@PathVariable Long userId) {
        return ResponseEntity.ok(clinicAdminService.toggleStaffStatus(userId));
    }
}

