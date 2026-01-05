package com.appointment.auth.dto;

import com.appointment.auth.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RegisterRequest {
    @NotBlank
    private String username;
    
    @NotBlank
    private String password;
    
    @NotBlank
    @Email
    private String email;
    
    @NotBlank
    private String name;
    
    private String phone;
    
    private User.UserRole role; // OPTIONAL, default to PATIENT if null
    
    // Doctor specific fields
    private Long specializationId;
    private String qualification;
    private BigDecimal consultationFee;
    private Integer experienceYears;
    private String bio;
}
