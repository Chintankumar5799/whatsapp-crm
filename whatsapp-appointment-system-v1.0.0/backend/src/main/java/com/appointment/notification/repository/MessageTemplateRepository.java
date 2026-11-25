package com.appointment.notification.repository;

import com.appointment.notification.model.MessageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, Long> {

    List<MessageTemplate> findByClinicIdOrderByIdAsc(Long clinicId);

    Optional<MessageTemplate> findByClinicIdAndTemplateType(Long clinicId, String templateType);
}

