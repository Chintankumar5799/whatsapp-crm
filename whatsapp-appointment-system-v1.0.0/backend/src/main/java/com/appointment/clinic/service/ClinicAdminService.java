package com.appointment.clinic.service;

import com.appointment.auth.model.User;
import com.appointment.auth.repository.UserRepository;
import com.appointment.booking.model.Availability;
import com.appointment.booking.repository.AvailabilityRepository;
import com.appointment.booking.repository.BookingRepository;
import com.appointment.booking.repository.PatientRepository;
import com.appointment.clinic.model.Clinic;
import com.appointment.clinic.repository.ClinicRepository;
import com.appointment.clinical.repository.ConsultationRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClinicAdminService {

    private final ClinicRepository clinicRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final BookingRepository bookingRepository;
    private final ConsultationRepository consultationRepository;
    private final AvailabilityRepository availabilityRepository;
    private final PasswordEncoder passwordEncoder;

    @Data
    @Builder
    public static class ClinicOverviewDto {
        private Long id;
        private String code;
        private String name;
        private String phone;
        private String email;
        private String city;
        private String subscriptionPlan;
        private String subscriptionStatus;
        private LocalDateTime subscriptionEndsAt;
        private Integer maxDoctors;
        private Integer maxStaff;
        private Long doctorCount;
        private Long patientCount;
        private Boolean isActive;
    }

    public List<ClinicOverviewDto> getAllClinicsOverview() {
        return clinicRepository.findAll().stream().map(clinic -> {
            long docCount = userRepository.findByClinicId(clinic.getId()).stream()
                    .filter(u -> u.getRole() == User.UserRole.DOCTOR)
                    .count();
            long patCount = patientRepository.countByClinicId(clinic.getId());

            return ClinicOverviewDto.builder()
                    .id(clinic.getId())
                    .code(clinic.getCode())
                    .name(clinic.getName())
                    .phone(clinic.getPhone())
                    .email(clinic.getEmail())
                    .city(clinic.getCity())
                    .subscriptionPlan(clinic.getSubscriptionPlan())
                    .subscriptionStatus(clinic.getSubscriptionStatus())
                    .subscriptionEndsAt(clinic.getSubscriptionEndsAt())
                    .maxDoctors(clinic.getMaxDoctors())
                    .maxStaff(clinic.getMaxStaff())
                    .doctorCount(docCount)
                    .patientCount(patCount)
                    .isActive(clinic.getIsActive())
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public Clinic registerNewClinic(Clinic clinic) {
        if (clinic.getCode() == null || clinic.getCode().trim().isEmpty()) {
            clinic.setCode("CLINIC-" + System.currentTimeMillis() % 100000);
        } else {
            clinic.setCode(clinic.getCode().trim().toUpperCase());
        }

        if (clinicRepository.findByCode(clinic.getCode()).isPresent()) {
            throw new IllegalArgumentException("Clinic code already exists: " + clinic.getCode());
        }

        if (clinic.getSubscriptionPlan() == null) {
            clinic.setSubscriptionPlan("TRIAL");
        }
        if (clinic.getSubscriptionStatus() == null) {
            clinic.setSubscriptionStatus("ACTIVE");
        }
        if (clinic.getTrialEndsAt() == null && "TRIAL".equalsIgnoreCase(clinic.getSubscriptionPlan())) {
            clinic.setTrialEndsAt(LocalDateTime.now().plusDays(14));
        }
        if (clinic.getSubscriptionEndsAt() == null && !"TRIAL".equalsIgnoreCase(clinic.getSubscriptionPlan())) {
            clinic.setSubscriptionEndsAt(LocalDateTime.now().plusDays(365));
        }

        log.info("Onboarding new clinic: {} ({})", clinic.getName(), clinic.getCode());
        return clinicRepository.save(clinic);
    }

    @Transactional
    public Clinic updateClinicSettings(Long id, Clinic updatedData) {
        Clinic clinic = clinicRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Clinic not found: " + id));

        if (updatedData.getName() != null) clinic.setName(updatedData.getName());
        if (updatedData.getPhone() != null) clinic.setPhone(updatedData.getPhone());
        if (updatedData.getEmail() != null) clinic.setEmail(updatedData.getEmail());
        if (updatedData.getWhatsappNumber() != null) clinic.setWhatsappNumber(updatedData.getWhatsappNumber());
        if (updatedData.getAddressLine1() != null) clinic.setAddressLine1(updatedData.getAddressLine1());
        if (updatedData.getAddressLine2() != null) clinic.setAddressLine2(updatedData.getAddressLine2());
        if (updatedData.getCity() != null) clinic.setCity(updatedData.getCity());
        if (updatedData.getState() != null) clinic.setState(updatedData.getState());
        if (updatedData.getPostalCode() != null) clinic.setPostalCode(updatedData.getPostalCode());
        if (updatedData.getFollowUpValidityDays() != null) clinic.setFollowUpValidityDays(updatedData.getFollowUpValidityDays());
        if (updatedData.getNewCaseConsultationFee() != null) clinic.setNewCaseConsultationFee(updatedData.getNewCaseConsultationFee());
        if (updatedData.getOldCaseConsultationFee() != null) clinic.setOldCaseConsultationFee(updatedData.getOldCaseConsultationFee());
        if (updatedData.getPrescriptionHeaderHtml() != null) clinic.setPrescriptionHeaderHtml(updatedData.getPrescriptionHeaderHtml());
        if (updatedData.getPrescriptionFooterHtml() != null) clinic.setPrescriptionFooterHtml(updatedData.getPrescriptionFooterHtml());
        if (updatedData.getGoogleReviewUrl() != null) clinic.setGoogleReviewUrl(updatedData.getGoogleReviewUrl());
        if (updatedData.getUpiId() != null) clinic.setUpiId(updatedData.getUpiId());

        return clinicRepository.save(clinic);
    }

    @Transactional
    public Clinic updateSubscription(Long id, String plan, String status, Integer maxDocs, Integer maxStaff, Integer extendDays) {
        Clinic clinic = clinicRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Clinic not found: " + id));

        if (plan != null && !plan.trim().isEmpty()) {
            clinic.setSubscriptionPlan(plan.trim().toUpperCase());
        }
        if (status != null && !status.trim().isEmpty()) {
            clinic.setSubscriptionStatus(status.trim().toUpperCase());
        }
        if (maxDocs != null && maxDocs > 0) {
            clinic.setMaxDoctors(maxDocs);
        }
        if (maxStaff != null && maxStaff > 0) {
            clinic.setMaxStaff(maxStaff);
        }
        if (extendDays != null && extendDays > 0) {
            LocalDateTime currentEnd = clinic.getSubscriptionEndsAt() != null ? clinic.getSubscriptionEndsAt() : LocalDateTime.now();
            clinic.setSubscriptionEndsAt(currentEnd.plusDays(extendDays));
        }

        log.info("Updated subscription for clinic id {}: plan={}, status={}, maxDocs={}", id, clinic.getSubscriptionPlan(), clinic.getSubscriptionStatus(), clinic.getMaxDoctors());
        return clinicRepository.save(clinic);
    }

    public Map<String, Object> getClinicStats(Long clinicId) {
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Clinic not found: " + clinicId));

        long totalPatients = patientRepository.countByClinicId(clinicId);
        long activeDoctors = userRepository.findByClinicId(clinicId).stream()
                .filter(u -> u.getRole() == User.UserRole.DOCTOR && Boolean.TRUE.equals(u.getIsActive()))
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("clinicId", clinic.getId());
        stats.put("clinicName", clinic.getName());
        stats.put("code", clinic.getCode());
        stats.put("subscriptionPlan", clinic.getSubscriptionPlan());
        stats.put("subscriptionStatus", clinic.getSubscriptionStatus());
        stats.put("subscriptionEndsAt", clinic.getSubscriptionEndsAt());
        stats.put("maxDoctors", clinic.getMaxDoctors());
        stats.put("activeDoctors", activeDoctors);
        stats.put("totalPatients", totalPatients);
        stats.put("followUpValidityDays", clinic.getFollowUpValidityDays());
        stats.put("newCaseFee", clinic.getNewCaseConsultationFee());
        stats.put("oldCaseFee", clinic.getOldCaseConsultationFee());

        return stats;
    }

    @Data
    @Builder
    public static class StaffSummaryDto {
        private Long id;
        private Long clinicId;
        private String username;
        private String name;
        private String email;
        private String phone;
        private String role;
        private String qualification;
        private String registrationNumber;
        private BigDecimal consultationFee;
        private BigDecimal oldCaseFee;
        private Boolean isActive;
    }

    public List<StaffSummaryDto> getClinicStaff(Long clinicId) {
        return userRepository.findByClinicId(clinicId).stream()
                .filter(u -> u.getRole() != User.UserRole.PATIENT)
                .map(u -> StaffSummaryDto.builder()
                        .id(u.getId())
                        .clinicId(u.getClinicId())
                        .username(u.getUsername())
                        .name(u.getFullName())
                        .email(u.getEmail())
                        .phone(u.getPhone())
                        .role(u.getRole().name())
                        .qualification(u.getQualification())
                        .registrationNumber(u.getRegistrationNumber())
                        .consultationFee(u.getConsultationFee())
                        .oldCaseFee(u.getOldCaseFee())
                        .isActive(u.getIsActive())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public StaffSummaryDto createClinicStaff(Long clinicId, Map<String, Object> body) {
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Clinic not found: " + clinicId));

        String username = body.get("username") != null ? body.get("username").toString().trim() : "";
        String email = body.get("email") != null ? body.get("email").toString().trim() : "";
        String password = body.get("password") != null ? body.get("password").toString() : "admin123";
        String roleStr = body.get("role") != null ? body.get("role").toString().trim().toUpperCase() : "RECEPTIONIST";

        if (username.isEmpty() || email.isEmpty()) {
            throw new IllegalArgumentException("Username and email are required");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        User.UserRole role = User.UserRole.valueOf(roleStr);
        User user = User.builder()
                .clinicId(clinic.getId())
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(body.get("name") != null ? body.get("name").toString() : username)
                .phone(body.get("phone") != null ? body.get("phone").toString() : null)
                .role(role)
                .qualification(body.get("qualification") != null ? body.get("qualification").toString() : null)
                .registrationNumber(body.get("registrationNumber") != null ? body.get("registrationNumber").toString() : null)
                .consultationFee(body.get("consultationFee") != null && !body.get("consultationFee").toString().isEmpty()
                        ? new BigDecimal(body.get("consultationFee").toString()) : clinic.getNewCaseConsultationFee())
                .oldCaseFee(body.get("oldCaseFee") != null && !body.get("oldCaseFee").toString().isEmpty()
                        ? new BigDecimal(body.get("oldCaseFee").toString()) : clinic.getOldCaseConsultationFee())
                .isActive(true)
                .build();

        user = userRepository.save(user);

        if (role == User.UserRole.DOCTOR) {
            List<Availability> availabilities = new ArrayList<>();
            for (DayOfWeek day : DayOfWeek.values()) {
                if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                    availabilities.add(Availability.builder()
                            .doctorId(user.getId())
                            .dayOfWeek(day)
                            .startTime(LocalTime.of(9, 0))
                            .endTime(LocalTime.of(17, 0))
                            .slotDurationMinutes(30)
                            .isActive(true)
                            .build());
                }
            }
            availabilityRepository.saveAll(availabilities);
        }

        return StaffSummaryDto.builder()
                .id(user.getId())
                .clinicId(user.getClinicId())
                .username(user.getUsername())
                .name(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .qualification(user.getQualification())
                .registrationNumber(user.getRegistrationNumber())
                .consultationFee(user.getConsultationFee())
                .oldCaseFee(user.getOldCaseFee())
                .isActive(user.getIsActive())
                .build();
    }

    @Transactional
    public StaffSummaryDto toggleStaffStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Staff user not found: " + userId));
        user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
        user = userRepository.save(user);
        return StaffSummaryDto.builder()
                .id(user.getId())
                .clinicId(user.getClinicId())
                .username(user.getUsername())
                .name(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .qualification(user.getQualification())
                .registrationNumber(user.getRegistrationNumber())
                .consultationFee(user.getConsultationFee())
                .oldCaseFee(user.getOldCaseFee())
                .isActive(user.getIsActive())
                .build();
    }
}

