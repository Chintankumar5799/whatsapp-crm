package com.appointment.clinical.service;

import com.appointment.booking.model.Booking;
import com.appointment.booking.model.Patient;
import com.appointment.booking.repository.BookingRepository;
import com.appointment.booking.repository.PatientRepository;
import com.appointment.clinical.model.FollowUp;
import com.appointment.clinical.repository.FollowUpRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowUpService {

    private final FollowUpRepository followUpRepository;
    private final PatientRepository patientRepository;
    private final BookingRepository bookingRepository;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FollowUpWorklistItem {
        private FollowUp followUp;
        private Patient patient;
    }

    public List<FollowUpWorklistItem> getWorklist(Long clinicId, String filter) {
        final Long targetClinicId = clinicId != null ? clinicId : 1L;
        LocalDate today = LocalDate.now();
        List<FollowUp> list;

        if ("overdue".equalsIgnoreCase(filter)) {
            list = followUpRepository.findOverdueFollowUps(targetClinicId, today);
        } else if ("upcoming".equalsIgnoreCase(filter)) {
            list = followUpRepository.findUpcomingFollowUps(targetClinicId, today.plusDays(1), today.plusDays(14));
        } else if ("due_today".equalsIgnoreCase(filter)) {
            list = followUpRepository.findByClinicIdAndDueDateAndStatus(targetClinicId, today, FollowUp.FollowUpStatus.PENDING);
        } else {
            // Default: All pending
            list = followUpRepository.findAll().stream()
                    .filter(f -> targetClinicId.equals(f.getClinicId()))
                    .toList();
        }

        List<FollowUpWorklistItem> result = new ArrayList<>();
        for (FollowUp f : list) {
            Patient p = patientRepository.findById(f.getPatientId()).orElse(null);
            result.add(FollowUpWorklistItem.builder()
                    .followUp(f)
                    .patient(p)
                    .build());
        }
        return result;
    }

    @Transactional
    public FollowUp recordCallAttempt(Long followUpId, FollowUp.ContactStatus contactStatus, String notes, Long staffId) {
        FollowUp followUp = followUpRepository.findById(followUpId)
                .orElseThrow(() -> new IllegalArgumentException("Follow-up not found: " + followUpId));

        followUp.setContactStatus(contactStatus);
        followUp.setContactAttemptCount(followUp.getContactAttemptCount() + 1);
        followUp.setLastContactAttemptAt(LocalDateTime.now());
        if (staffId != null) {
            followUp.setAssignedStaffId(staffId);
        }

        if (contactStatus == FollowUp.ContactStatus.DECLINED) {
            followUp.setStatus(FollowUp.FollowUpStatus.CANCELLED);
        }

        log.info("Recorded call attempt on FollowUp #{}: status={}, attempts={}", 
                followUpId, contactStatus, followUp.getContactAttemptCount());
        return followUpRepository.save(followUp);
    }

    @Transactional
    public Booking convertToAppointment(Long followUpId, LocalDate date, LocalTime time) {
        FollowUp followUp = followUpRepository.findById(followUpId)
                .orElseThrow(() -> new IllegalArgumentException("Follow-up not found: " + followUpId));

        Patient patient = patientRepository.findById(followUp.getPatientId()).orElse(null);

        Booking booking = Booking.builder()
                .clinicId(followUp.getClinicId())
                .bookingNumber("BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .doctorId(followUp.getDoctorId())
                .patientId(followUp.getPatientId())
                .patientName(patient != null ? patient.getName() : "Patient")
                .patientPhone(patient != null ? patient.getPhone() : "")
                .bookingDate(date != null ? date : followUp.getDueDate())
                .startTime(time != null ? time : LocalTime.of(10, 0))
                .endTime(time != null ? time.plusMinutes(20) : LocalTime.of(10, 20))
                .status(Booking.BookingStatus.CONFIRMED)
                .caseType("OLD_CASE")
                .appointmentSource("FOLLOW_UP_RECALL")
                .notes("Revisit booked from follow-up reminder: " + (followUp.getReason() != null ? followUp.getReason() : ""))
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        followUp.setStatus(FollowUp.FollowUpStatus.BOOKED);
        followUp.setResultingBookingId(savedBooking.getId());
        followUpRepository.save(followUp);

        log.info("Converted FollowUp #{} to Appointment #{}", followUpId, savedBooking.getBookingNumber());
        return savedBooking;
    }
}
