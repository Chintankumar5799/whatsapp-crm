package com.appointment.notification.repository;

import com.appointment.notification.model.PatientConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientConsentRepository extends JpaRepository<PatientConsent, Long> {
    Optional<PatientConsent> findByPatientIdAndConsentType(Long patientId, PatientConsent.ConsentType consentType);
    List<PatientConsent> findByPatientId(Long patientId);
}

