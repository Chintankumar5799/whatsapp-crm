package com.appointment.booking.service;

import com.appointment.booking.dto.DashboardMetricsDto;
import com.appointment.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class MetricsService {
    
    private final BookingRepository bookingRepository;
    
    public DashboardMetricsDto getDashboardMetrics(Long doctorId, LocalDate date) {
        // Simplified metrics
        long booked = bookingRepository.countByDoctorIdAndDate(doctorId, date);
        return DashboardMetricsDto.builder()
                .slotsBooked(booked)
                .customersAttendedToday(booked) // Assuming attended = booked for now
                .build();
    }

    public DashboardMetricsDto.ChartData getChartData(Long doctorId, String period, LocalDate startDate) {
        // Placeholder for chart data
        return DashboardMetricsDto.ChartData.builder()
                .period(period)
                .dataPoints(Collections.emptyList())
                .build();
    }
}
