package com.appointment.booking.dto;

import com.appointment.booking.model.Patient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient360Dto {
    private Patient patient;
    private long totalLifetimeVisits;
    private BigDecimal totalLifetimeRevenue;
    private List<ConsultationDetailDto> consultations;
    private List<VitalsRecordDto> vitalsHistory;
    private List<AppointmentHistoryDto> appointments;
    private List<DocumentMetaDto> documents;
    private List<FollowUpHistoryDto> followUps;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsultationDetailDto {
        private Long id;
        private Long bookingId;
        private Long doctorId;
        private String doctorName;
        private String caseType;
        private BigDecimal consultationFee;
        private String chiefComplaint;
        private String clinicalExamination;
        private String diagnosis;
        private String doctorNotes;
        private LocalDateTime finalizedAt;
        private Integer bpSystolic;
        private Integer bpDiastolic;
        private Integer pulse;
        private BigDecimal temperature;
        private BigDecimal weight;
        private Integer spo2;
        private String pdfUrl;
        private String dischargePdfUrl;
        private int prescriptionItemsCount;
        private List<RxItemDto> prescriptionItems;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RxItemDto {
        private String medicineName;
        private String dosage;
        private String frequency;
        private Integer durationDays;
        private String instructions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VitalsRecordDto {
        private LocalDateTime recordedAt;
        private String bp;
        private Integer bpSystolic;
        private Integer bpDiastolic;
        private Integer pulse;
        private BigDecimal temperature;
        private BigDecimal weight;
        private Integer spo2;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppointmentHistoryDto {
        private Long id;
        private String bookingNumber;
        private LocalDate bookingDate;
        private String startTime;
        private String doctorName;
        private String status;
        private BigDecimal amount;
        private String paymentStatus;
        private String appointmentSource;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentMetaDto {
        private Long id;
        private String documentType;
        private String fileName;
        private String fileType;
        private Long fileSize;
        private String notes;
        private LocalDateTime uploadedAt;
        private String downloadUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FollowUpHistoryDto {
        private Long id;
        private LocalDate dueDate;
        private String doctorName;
        private String reason;
        private String status;
        private String contactStatus;
    }
}

