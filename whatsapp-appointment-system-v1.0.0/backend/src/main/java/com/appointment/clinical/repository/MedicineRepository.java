package com.appointment.clinical.repository;

import com.appointment.clinical.model.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    List<Medicine> findByClinicIdAndIsActiveTrueOrderByNameAsc(Long clinicId);

    @Query("SELECT m FROM Medicine m WHERE m.clinicId = :clinicId AND m.isActive = true AND " +
           "(LOWER(m.name) LIKE LOWER(CONCAT('%', :term, '%')) OR LOWER(m.brandName) LIKE LOWER(CONCAT('%', :term, '%')))")
    List<Medicine> searchMedicines(@Param("clinicId") Long clinicId, @Param("term") String term);
}

