package com.appointment.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsDto {
    private long slotsBooked;
    private long completedAppointments;
    private long walkInCount;
    private long cancelledAppointments;
    private long noShowAppointments;
    private long customersAttendedToday;
    private long newPatientsCount;
    private long oldPatientsCount;
    private long pendingInquiriesCount;
    private long dueFollowUpsToday;
    private BigDecimal totalRevenueToday;
    private double attendanceRatePercent;
    private long weeklyPatientsCount;
    private long monthlyPatientsCount;
    
    private ChartData chartData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartData {
        private String period;
        private List<TrendDataPoint> dailyTrends;
        private List<DataPoint> dataPoints;
        private Map<String, Long> inquirySourceBreakdown;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendDataPoint {
        private String date;
        private String dayOfWeek;
        private long newCases;
        private long oldCases;
        private long totalPatients;
        private double revenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPoint {
        private String label;
        private long count;
        private double amount;
    }
}
