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
}

