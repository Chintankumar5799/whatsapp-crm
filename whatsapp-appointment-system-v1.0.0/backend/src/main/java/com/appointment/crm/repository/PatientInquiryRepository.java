package com.appointment.crm.repository;

import com.appointment.crm.model.PatientInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientInquiryRepository extends JpaRepository<PatientInquiry, Long> {
    List<PatientInquiry> findByClinicIdOrderByCreatedAtDesc(Long clinicId);
    List<PatientInquiry> findByClinicIdAndStatusOrderByCreatedAtDesc(Long clinicId, PatientInquiry.InquiryStatus status);
    List<PatientInquiry> findByClinicIdAndPhone(Long clinicId, String phone);
}

