package com.appointment.crm.service;

import com.appointment.booking.model.Booking;
import com.appointment.booking.model.Patient;
import com.appointment.booking.service.PatientService;
import com.appointment.booking.repository.BookingRepository;
import com.appointment.crm.model.PatientInquiry;
import com.appointment.crm.repository.PatientInquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientInquiryService {

    private final PatientInquiryRepository inquiryRepository;
    private final PatientService patientService;
    private final BookingRepository bookingRepository;

    public List<PatientInquiry> getInquiries(Long clinicId, PatientInquiry.InquiryStatus status) {
        if (clinicId == null) {
            clinicId = 1L;
        }
        if (status != null) {
            return inquiryRepository.findByClinicIdAndStatusOrderByCreatedAtDesc(clinicId, status);
        }
        return inquiryRepository.findByClinicIdOrderByCreatedAtDesc(clinicId);
    }

    @Transactional
    public PatientInquiry createInquiry(PatientInquiry inquiry) {
        if (inquiry.getClinicId() == null) {
            inquiry.setClinicId(1L);
        }
        if (inquiry.getStatus() == null) {
            inquiry.setStatus(PatientInquiry.InquiryStatus.NEW);
        }
        return inquiryRepository.save(inquiry);
    }

    @Transactional
    public PatientInquiry updateStatus(Long inquiryId, PatientInquiry.InquiryStatus status, LocalDate nextFollowUp) {
        PatientInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("Inquiry not found: " + inquiryId));

        inquiry.setStatus(status);
        if (nextFollowUp != null) {
            inquiry.setNextFollowUpDate(nextFollowUp);
        }
        return inquiryRepository.save(inquiry);
    }

    @Transactional
    public PatientInquiry convertToPatientAndBooking(Long inquiryId, Long doctorId, LocalDate appointmentDate, LocalTime time) {
        PatientInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("Inquiry not found: " + inquiryId));

        // 1. Create or Find Patient
        List<Patient> existing = patientService.checkDuplicates(inquiry.getClinicId(), inquiry.getPhone(), inquiry.getName());
        Patient patient;
        if (!existing.isEmpty()) {
            patient = existing.get(0);
        } else {
            patient = Patient.builder()
                    .clinicId(inquiry.getClinicId())
                    .name(inquiry.getName())
                    .phone(inquiry.getPhone())
                    .build();
            patient = patientService.registerPatient(inquiry.getClinicId(), patient);
        }

        inquiry.setConvertedPatientId(patient.getId());

        // 2. Optional Booking
        if (doctorId != null && appointmentDate != null) {
            Booking booking = Booking.builder()
                    .clinicId(inquiry.getClinicId())
                    .bookingNumber("INQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .doctorId(doctorId)
                    .patientId(patient.getId())
                    .patientName(patient.getName())
                    .patientPhone(patient.getPhone())
                    .bookingDate(appointmentDate)
                    .startTime(time != null ? time : LocalTime.of(11, 0))
                    .endTime(time != null ? time.plusMinutes(20) : LocalTime.of(11, 20))
                    .status(Booking.BookingStatus.CONFIRMED)
                    .caseType("NEW_CASE")
                    .appointmentSource(inquiry.getSource().name())
                    .notes("Converted from Inquiry #" + inquiryId + ": " + inquiry.getRequestDescription())
                    .build();

            Booking savedBooking = bookingRepository.save(booking);
            inquiry.setConvertedBookingId(savedBooking.getId());
        }

        inquiry.setStatus(PatientInquiry.InquiryStatus.BOOKED);
        return inquiryRepository.save(inquiry);
    }
}

