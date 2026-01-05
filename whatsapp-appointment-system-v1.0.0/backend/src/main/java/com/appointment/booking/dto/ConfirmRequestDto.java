package com.appointment.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmRequestDto {
    @NotNull
    private Long doctorId;
    
    @NotNull
    @Min(1)
    private Integer durationMinutes;
    
    private String remarks;
}
