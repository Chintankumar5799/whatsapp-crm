package com.appointment.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorLookupResponse {
    private boolean ambiguous;
    private DoctorInfo doctor;
    private List<DoctorInfo> doctors;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorInfo {
        private Long id;
        private String name;
        private String phone;
        private String specialization;
        private String qualification;
    }
}
