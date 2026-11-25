package com.appointment.clinical.repository;

import com.appointment.clinical.model.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {
    List<PrescriptionItem> findByConsultationIdOrderByIdAsc(Long consultationId);
}

