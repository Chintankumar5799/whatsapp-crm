package com.appointment.auth.controller;

import com.appointment.auth.dto.LoginRequest;
import com.appointment.auth.dto.LoginResponse;
import com.appointment.auth.model.User;
import com.appointment.auth.repository.UserRepository;
import com.appointment.auth.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    private final com.appointment.booking.repository.AvailabilityRepository availabilityRepository;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates user and returns JWT token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        
        if (!user.getIsActive()) {
            throw new RuntimeException("User account is inactive");
        }
        
        String token = jwtService.generateToken(user.getUsername(), user.getRole().name(), user.getId());
        
        return ResponseEntity.ok(LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .userId(user.getId())
                .build());
    }

    @PostMapping("/register")
    @Operation(summary = "Register", description = "Registers a new user (Patient or Doctor)")
    public ResponseEntity<?> register(@Valid @RequestBody com.appointment.auth.dto.RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        User.UserRole role = request.getRole() != null ? request.getRole() : User.UserRole.PATIENT;

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .role(role)
                .isActive(true)
                // Doctor specific fields
                .specializationId(request.getSpecializationId())
                .qualification(request.getQualification())
                .consultationFee(request.getConsultationFee())
                .experienceYears(request.getExperienceYears())
                .bio(request.getBio())
                .build();

        user = userRepository.save(user);

        if (user.getRole() == User.UserRole.DOCTOR) {
            java.util.List<com.appointment.booking.model.Availability> availabilities = new java.util.ArrayList<>();
            for (java.time.DayOfWeek day : java.time.DayOfWeek.values()) {
                if (day != java.time.DayOfWeek.SATURDAY && day != java.time.DayOfWeek.SUNDAY) { // Mon-Fri
                    availabilities.add(com.appointment.booking.model.Availability.builder()
                            .doctorId(user.getId())
                            .dayOfWeek(day)
                            .startTime(java.time.LocalTime.of(9, 0))
                            .endTime(java.time.LocalTime.of(17, 0))
                            .slotDurationMinutes(30)
                            .isActive(true)
                            .build());
                }
            }
            availabilityRepository.saveAll(availabilities);
        }

        return ResponseEntity.ok("User registered successfully");
    }
}

