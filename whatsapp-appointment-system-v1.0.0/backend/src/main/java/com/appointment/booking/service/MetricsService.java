package com.appointment.booking.service;

import com.appointment.booking.dto.DashboardMetricsDto;
import com.appointment.booking.model.Booking;
import com.appointment.booking.repository.BookingRepository;
import com.appointment.clinical.model.Consultation;
import com.appointment.clinical.model.FollowUp;
import com.appointment.clinical.model.OpdQueue;
import com.appointment.clinical.repository.ConsultationRepository;
import com.appointment.clinical.repository.FollowUpRepository;
import com.appointment.clinical.repository.OpdQueueRepository;
import com.appointment.crm.model.PatientInquiry;
import com.appointment.crm.repository.PatientInquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final BookingRepository bookingRepository;
    private final ConsultationRepository consultationRepository;
    private final OpdQueueRepository opdQueueRepository;
    private final FollowUpRepository followUpRepository;
    private final PatientInquiryRepository patientInquiryRepository;

    public DashboardMetricsDto getDashboardMetrics(Long doctorId, LocalDate date) {
        if (date == null) date = LocalDate.now();
        final LocalDate targetDate = date;

        // 1. Bookings for date
        List<Booking> allBookings = bookingRepository.findAll();
        List<Booking> dateBookings = allBookings.stream()
                .filter(b -> targetDate.equals(b.getBookingDate()))
                .filter(b -> doctorId == null || doctorId == 0 || doctorId.equals(b.getDoctorId()))
                .collect(Collectors.toList());

        long booked = dateBookings.size();
        long completed = dateBookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.COMPLETED).count();
        long cancelled = dateBookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.CANCELLED).count();
        long noShow = dateBookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.NO_SHOW).count();

        // 2. Queue & Walk-ins
        List<OpdQueue> queueItems = opdQueueRepository.findByClinicIdAndQueueDateOrderByQueueNumberAsc(1L, targetDate);
        if (doctorId != null && doctorId > 0) {
            queueItems = queueItems.stream().filter(q -> doctorId.equals(q.getDoctorId())).collect(Collectors.toList());
        }
        long walkIns = queueItems.stream().filter(q -> q.getBookingId() == null).count();
        long queueCompleted = queueItems.stream().filter(q -> q.getStatus() == OpdQueue.QueueStatus.COMPLETED).count();
        long attended = Math.max(completed, queueCompleted);

        // 3. Consultations (New vs Old Case & Revenue)
        List<Consultation> allConsults = consultationRepository.findAll();
        List<Consultation> dateConsults = allConsults.stream()
                .filter(c -> c.getFinalizedAt() != null && targetDate.equals(c.getFinalizedAt().toLocalDate()))
                .filter(c -> doctorId == null || doctorId == 0 || doctorId.equals(c.getDoctorId()))
                .collect(Collectors.toList());

        long newCases = dateConsults.stream().filter(c -> c.getCaseType() == Consultation.CaseType.NEW_CASE).count();
        long oldCases = dateConsults.stream().filter(c -> c.getCaseType() == Consultation.CaseType.OLD_CASE).count();

        BigDecimal revenue = BigDecimal.ZERO;
        for (Consultation c : dateConsults) {
            if (c.getConsultationFee() != null) {
                revenue = revenue.add(c.getConsultationFee());
            }
        }
        // Also add payments from completed bookings if consultation fee is zero
        if (revenue.compareTo(BigDecimal.ZERO) == 0) {
            double bookingTotal = dateBookings.stream()
                    .filter(b -> b.getStatus() == Booking.BookingStatus.COMPLETED && b.getTotalAmount() != null)
                    .mapToDouble(b -> b.getTotalAmount().doubleValue())
                    .sum();
            revenue = BigDecimal.valueOf(bookingTotal);
        }

        // 4. Inquiries & Follow-ups
        List<PatientInquiry> inquiries = patientInquiryRepository.findByClinicIdOrderByCreatedAtDesc(1L);
        long pendingInquiries = inquiries.stream()
                .filter(i -> i.getStatus() == PatientInquiry.InquiryStatus.NEW || i.getStatus() == PatientInquiry.InquiryStatus.CONTACTED)
                .count();

        List<FollowUp> dueFollowUps = followUpRepository.findByClinicIdAndDueDateAndStatus(1L, targetDate, FollowUp.FollowUpStatus.PENDING);
        long followUpsDueToday = dueFollowUps.size();

        // 5. Attendance percentage
        double attendanceRate = (booked + walkIns > 0) ? ((double) attended / (booked + walkIns)) * 100.0 : 0.0;

        // 6. 7-Day & 30-Day Totals
        LocalDate weekStart = targetDate.minusDays(6);
        LocalDate monthStart = targetDate.minusDays(29);
        long weeklyCount = allConsults.stream()
                .filter(c -> c.getFinalizedAt() != null)
                .map(c -> c.getFinalizedAt().toLocalDate())
                .filter(d -> !d.isBefore(weekStart) && !d.isAfter(targetDate))
                .count();

        long monthlyCount = allConsults.stream()
                .filter(c -> c.getFinalizedAt() != null)
                .map(c -> c.getFinalizedAt().toLocalDate())
                .filter(d -> !d.isBefore(monthStart) && !d.isAfter(targetDate))
                .count();

        // 7. Chart Data
        DashboardMetricsDto.ChartData chartData = getChartData(doctorId, "daily", targetDate);

        return DashboardMetricsDto.builder()
                .slotsBooked(booked)
                .completedAppointments(completed)
                .walkInCount(walkIns)
                .cancelledAppointments(cancelled)
                .noShowAppointments(noShow)
                .customersAttendedToday(attended)
                .newPatientsCount(newCases)
                .oldPatientsCount(oldCases)
                .pendingInquiriesCount(pendingInquiries)
                .dueFollowUpsToday(followUpsDueToday)
                .totalRevenueToday(revenue)
                .attendanceRatePercent(Math.round(attendanceRate * 10.0) / 10.0)
                .weeklyPatientsCount(weeklyCount)
                .monthlyPatientsCount(monthlyCount)
                .chartData(chartData)
                .build();
    }

    public DashboardMetricsDto.ChartData getChartData(Long doctorId, String period, LocalDate startDate) {
        return getChartData(doctorId, period, startDate, null);
    }

    public DashboardMetricsDto.ChartData getChartData(Long doctorId, String period, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        LocalDate fromDate;
        LocalDate toDate;

        if ("today".equalsIgnoreCase(period)) {
            fromDate = startDate != null ? startDate : today;
            toDate = fromDate;
        } else if ("month".equalsIgnoreCase(period) || "thisMonth".equalsIgnoreCase(period)) {
            fromDate = today.withDayOfMonth(1);
            toDate = today;
        } else if ("custom".equalsIgnoreCase(period)) {
            fromDate = startDate != null ? startDate : today.minusDays(6);
            toDate = endDate != null ? endDate : today;
            if (fromDate.isAfter(toDate)) {
                LocalDate temp = fromDate;
                fromDate = toDate;
                toDate = temp;
            }
        } else {
            // default: last 7 days
            toDate = startDate != null ? startDate : today;
            fromDate = toDate.minusDays(6);
        }

        List<Consultation> allConsults = consultationRepository.findAll();
        List<DashboardMetricsDto.TrendDataPoint> trends = new ArrayList<>();
        List<DashboardMetricsDto.DataPoint> dataPoints = new ArrayList<>();

        LocalDate curr = fromDate;
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd MMM");

        while (!curr.isAfter(toDate)) {
            final LocalDate d = curr;
            String label;
            long daysSpan = java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate);
            if ("today".equalsIgnoreCase(period)) {
                label = "Today (" + d.format(dtf) + ")";
            } else if (daysSpan <= 7) {
                label = d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + d.getDayOfMonth();
            } else {
                label = d.format(dtf);
            }

            List<Consultation> dayConsults = allConsults.stream()
                    .filter(c -> c.getFinalizedAt() != null && d.equals(c.getFinalizedAt().toLocalDate()))
                    .filter(c -> doctorId == null || doctorId == 0 || doctorId.equals(c.getDoctorId()))
                    .collect(Collectors.toList());

            long newC = dayConsults.stream().filter(c -> c.getCaseType() == Consultation.CaseType.NEW_CASE).count();
            long oldC = dayConsults.stream().filter(c -> c.getCaseType() == Consultation.CaseType.OLD_CASE).count();
            double rev = dayConsults.stream()
                    .map(c -> c.getConsultationFee() != null ? c.getConsultationFee().doubleValue() : 0.0)
                    .mapToDouble(Double::doubleValue).sum();

            trends.add(DashboardMetricsDto.TrendDataPoint.builder()
                    .date(d.toString())
                    .dayOfWeek(label)
                    .newCases(newC)
                    .oldCases(oldC)
                    .totalPatients(newC + oldC)
                    .revenue(rev)
                    .build());

            dataPoints.add(DashboardMetricsDto.DataPoint.builder()
                    .label(label)
                    .count(newC + oldC)
                    .amount(rev)
                    .build());

            curr = curr.plusDays(1);
        }

        // Inquiry source breakdown
        List<PatientInquiry> inquiries = patientInquiryRepository.findByClinicIdOrderByCreatedAtDesc(1L);
        Map<String, Long> sourceMap = inquiries.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getSource() != null ? i.getSource().name() : "OTHER",
                        Collectors.counting()
                ));

        return DashboardMetricsDto.ChartData.builder()
                .period(period != null ? period : "7days")
                .dailyTrends(trends)
                .dataPoints(dataPoints)
                .inquirySourceBreakdown(sourceMap)
                .build();
    }
}
