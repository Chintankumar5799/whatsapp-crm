package com.appointment.booking.service;

import com.appointment.booking.model.Availability;
import com.appointment.booking.model.AvailabilityException;
import com.appointment.booking.model.Slot;
import com.appointment.booking.model.Slot.SlotStatus;
import com.appointment.booking.repository.AvailabilityExceptionRepository;
import com.appointment.booking.repository.AvailabilityRepository;
import com.appointment.booking.repository.SlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlotService {
    
    private final SlotRepository slotRepository;
    private final AvailabilityRepository availabilityRepository;
    private final AvailabilityExceptionRepository exceptionRepository;
    private final SlotHoldService slotHoldService;
    
    /**
     * Generates slots for a doctor for a given date range
     */
    @Transactional
    public void generateSlotsForDateRange(Long doctorId, LocalDate startDate, LocalDate endDate) {
        LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            generateSlotsForDate(doctorId, currentDate);
            currentDate = currentDate.plusDays(1);
        }
    }
    
    /**
     * Generates slots for a specific date
     */
    @Transactional
    public void generateSlotsForDate(Long doctorId, LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        
        // Check for exceptions first
        Optional<AvailabilityException> exception = exceptionRepository
                .findByDoctorIdAndExceptionDate(doctorId, date);
        
        if (exception.isPresent()) {
            AvailabilityException ex = exception.get();
            if (ex.getExceptionType() == AvailabilityException.ExceptionType.BLOCKED) {
                log.debug("Date {} is blocked for doctor {}", date, doctorId);
                return; // Don't generate slots for blocked dates
            } else if (ex.getExceptionType() == AvailabilityException.ExceptionType.MODIFIED_HOURS) {
                // Use modified hours
                if (ex.getStartTime() != null && ex.getEndTime() != null) {
                    generateSlotsForTimeRange(doctorId, date, ex.getStartTime(), ex.getEndTime(), 30);
                }
                return;
            }
        }
        
        // Get regular availability for this day
        List<Availability> availabilities = availabilityRepository
                .findByDoctorIdAndDayOfWeek(doctorId, dayOfWeek);
        
        if (availabilities.isEmpty()) {
            // Default availability: 09:00 - 17:00 if not configured
            generateSlotsForTimeRange(doctorId, date, LocalTime.of(9, 0), LocalTime.of(17, 0), 30);
        } else {
            for (Availability availability : availabilities) {
                if (availability.getIsActive()) {
                    generateSlotsForTimeRange(
                            doctorId,
                            date,
                            availability.getStartTime(),
                            availability.getEndTime(),
                            availability.getSlotDurationMinutes()
                    );
                }
            }
        }
    }
    
    private void generateSlotsForTimeRange(Long doctorId, LocalDate date, 
                                          LocalTime startTime, LocalTime endTime, 
                                          int slotDurationMinutes) {
        LocalTime currentTime = startTime;
        
        while (currentTime.plusMinutes(slotDurationMinutes).isBefore(endTime) ||
               currentTime.plusMinutes(slotDurationMinutes).equals(endTime)) {
            
            LocalTime slotEndTime = currentTime.plusMinutes(slotDurationMinutes);
            
            // Check if slot already exists
            Optional<Slot> existingSlot = slotRepository
                    .findByDoctorIdAndSlotDateAndStartTimeAndEndTime(
                            doctorId, date, currentTime, slotEndTime);
            
            if (existingSlot.isEmpty()) {
                // Check if slot is held in Redis
                Slot slot = Slot.builder()
                        .doctorId(doctorId)
                        .slotDate(date)
                        .startTime(currentTime)
                        .endTime(slotEndTime)
                        .status(SlotStatus.AVAILABLE)
                        .build();
                
                slotRepository.save(slot);
            }
            
            currentTime = currentTime.plusMinutes(slotDurationMinutes);
        }
    }
    
    /**
     * Gets available slots for a doctor on a specific date
     */
    public List<Slot> getAvailableSlots(Long doctorId, LocalDate date) {
        // First ensure slots are generated
        generateSlotsForDate(doctorId, date);
        
        List<Slot> slots = slotRepository.findByDoctorIdAndSlotDateAndStatus(
                doctorId, date, SlotStatus.AVAILABLE);
        
        // Filter out slots that have active holds
        return slots.stream()
                .filter(slot -> !slotHoldService.hasActiveHold(slot.getId()))
                .toList();
    }
    
    /**
     * Books a slot (marks as booked)
     */
    @Transactional
    public Slot bookSlot(Long slotId) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new IllegalStateException("Slot is not available");
        }
        
        slot.setStatus(SlotStatus.BOOKED);
        return slotRepository.save(slot);
    }
    
    /**
     * Releases a slot (marks as available)
     */
    @Transactional
    public Slot releaseSlot(Long slotId) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        
        slot.setStatus(SlotStatus.AVAILABLE);
        return slotRepository.save(slot);
    }
    
    /**
     * Scheduled job to generate slots for the next 30 days
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    public void generateSlotsForNextMonth() {
        log.info("Generating slots for next 30 days");
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(30);
        
        // Get all active doctors and generate slots
        // This would require a doctor service - for now, we'll generate for a specific doctor
        // In production, iterate through all active doctors
    }
}

