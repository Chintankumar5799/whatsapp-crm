package com.appointment.booking.dto;

import lombok.Data;

@Data
public class RejectRequestDto {
    private Long doctorId;
    private String message;
}
