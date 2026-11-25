package com.appointment.clinical.service;

import com.appointment.clinical.model.OpdQueue;
import com.appointment.clinical.repository.OpdQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpdQueueService {

    private final OpdQueueRepository opdQueueRepository;

    @Transactional
    public OpdQueue checkInPatient(Long clinicId, Long doctorId, Long patientId, Long bookingId, boolean priority) {
        LocalDate today = LocalDate.now();
        int nextQueueNumber = opdQueueRepository.findMaxQueueNumber(doctorId, today) + 1;

        OpdQueue queueEntry = OpdQueue.builder()
                .clinicId(clinicId != null ? clinicId : 1L)
                .doctorId(doctorId)
                .patientId(patientId)
                .bookingId(bookingId)
                .queueNumber(nextQueueNumber)
                .queueDate(today)
                .status(OpdQueue.QueueStatus.WAITING)
                .arrivalTime(LocalDateTime.now())
                .priorityFlag(priority)
                .build();

        OpdQueue saved = opdQueueRepository.save(queueEntry);
        log.info("Patient {} checked in to OPD Queue with token #{} for Doctor {}", patientId, nextQueueNumber, doctorId);
        return saved;
    }

    @Transactional
    public OpdQueue callPatientToConsultation(Long queueId) {
        OpdQueue queue = opdQueueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue entry not found: " + queueId));

        queue.setStatus(OpdQueue.QueueStatus.IN_CONSULTATION);
        queue.setConsultationStartTime(LocalDateTime.now());
        return opdQueueRepository.save(queue);
    }

    @Transactional
    public OpdQueue markConsultationCompleted(Long queueId) {
        OpdQueue queue = opdQueueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue entry not found: " + queueId));

        queue.setStatus(OpdQueue.QueueStatus.COMPLETED);
        queue.setConsultationEndTime(LocalDateTime.now());
        return opdQueueRepository.save(queue);
    }

    @Transactional
    public OpdQueue markNoShow(Long queueId) {
        OpdQueue queue = opdQueueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue entry not found: " + queueId));

        queue.setStatus(OpdQueue.QueueStatus.NO_SHOW);
        return opdQueueRepository.save(queue);
    }

    public List<OpdQueue> getTodayDoctorQueue(Long doctorId) {
        return opdQueueRepository.findByDoctorIdAndQueueDateOrderByQueueNumberAsc(doctorId, LocalDate.now());
    }

    public List<OpdQueue> getTodayClinicQueue(Long clinicId) {
        return opdQueueRepository.findByClinicIdAndQueueDateOrderByQueueNumberAsc(clinicId, LocalDate.now());
    }
}

