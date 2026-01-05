package com.appointment.booking.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PatientBookingRequest {
    private String doctorPhone;
    private String patientPhone;
    private String patientName;
    private LocalDate requestedDate;
    private String requestedStartTime;
    private String description;
    private Long addressId;
}
