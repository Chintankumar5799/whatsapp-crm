package com.appointment.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosBillRequest {
    @Builder.Default
    private Long clinicId = 1L;
    
    private Long patientId;
    private Long doctorId;
    private Long bookingId;
    private Long consultationId;

    @Builder.Default
    private String paymentMode = "CASH"; // CASH, UPI_QR, CARD, ONLINE

    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    private String notes;

    private List<BillItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillItemDto {
        private String itemName;
        @Builder.Default
        private Integer quantity = 1;
        private BigDecimal unitPrice;
    }
}

