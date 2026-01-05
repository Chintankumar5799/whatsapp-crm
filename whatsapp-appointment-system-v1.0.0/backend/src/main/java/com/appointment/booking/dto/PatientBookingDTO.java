package com.appointment.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientBookingDTO {
    private Long id;
    private Long doctorId;
    private String doctorName;
    private LocalDate bookingDate;
    private String startTime;
    private String status;
    private String paymentLink;
    private BigDecimal totalAmount;
}
