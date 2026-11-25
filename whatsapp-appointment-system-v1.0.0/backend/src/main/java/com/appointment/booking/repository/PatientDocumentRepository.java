package com.appointment.booking.repository;

import com.appointment.booking.model.PatientDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientDocumentRepository extends JpaRepository<PatientDocument, Long> {
    List<PatientDocument> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<PatientDocument> findByClinicIdOrderByCreatedAtDesc(Long clinicId);
}

