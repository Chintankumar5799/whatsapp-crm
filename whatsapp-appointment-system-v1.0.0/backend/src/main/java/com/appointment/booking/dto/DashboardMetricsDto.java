package com.appointment.booking.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DashboardMetricsDto {
    private long slotsBooked;
    private long customersAttendedToday;
    
    @Data
    @Builder
    public static class ChartData {
        private String period;
        private List<DataPoint> dataPoints;
    }

    @Data
    @Builder
    public static class DataPoint {
        private String label;
        private long count;
        private double amount;
    }
}
