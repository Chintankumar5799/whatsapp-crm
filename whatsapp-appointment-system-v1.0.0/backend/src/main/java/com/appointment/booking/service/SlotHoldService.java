package com.appointment.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing slot holds in Redis
 * Prevents double-booking by temporarily reserving slots
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlotHoldService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${slot.hold.duration-minutes:5}")
    private int holdDurationMinutes;
    
    private static final String HOLD_KEY_PREFIX = "slot:hold:";
    private static final String HOLD_PATTERN = HOLD_KEY_PREFIX + "*";
    
    /**
     * Creates a hold on a slot
     * @param slotId The slot ID to hold
     * @param patientId The patient ID requesting the hold
     * @return Hold token (UUID) that must be used to confirm the booking
     */
    public String createHold(Long slotId, Long patientId) {
        String holdToken = UUID.randomUUID().toString();
        String holdKey = HOLD_KEY_PREFIX + slotId + ":" + holdToken;
        String holdValue = patientId.toString();
        
        Duration holdDuration = Duration.ofMinutes(holdDurationMinutes);
        redisTemplate.opsForValue().set(holdKey, holdValue, holdDuration);
        
        log.info("Created hold for slot {} with token {} (expires in {} minutes)", 
                slotId, holdToken, holdDurationMinutes);
        
        return holdToken;
    }
    
    /**
     * Validates and consumes a hold token
     * @param slotId The slot ID
     * @param holdToken The hold token
     * @param patientId The patient ID (must match the hold)
     * @return true if hold is valid and consumed, false otherwise
     */
    public boolean validateAndConsumeHold(Long slotId, String holdToken, Long patientId) {
        String holdKey = HOLD_KEY_PREFIX + slotId + ":" + holdToken;
        String storedPatientId = redisTemplate.opsForValue().get(holdKey);
        
        if (storedPatientId == null) {
            log.warn("Hold not found or expired for slot {} with token {}", slotId, holdToken);
            return false;
        }
        
        if (!storedPatientId.equals(patientId.toString())) {
            log.warn("Hold patient ID mismatch for slot {} with token {}", slotId, holdToken);
            return false;
        }
        
        // Consume the hold by deleting it
        redisTemplate.delete(holdKey);
        log.info("Hold validated and consumed for slot {} with token {}", slotId, holdToken);
        return true;
    }
    
    /**
     * Releases a hold (cancels it)
     * @param slotId The slot ID
     * @param holdToken The hold token
     */
    public void releaseHold(Long slotId, String holdToken) {
        String holdKey = HOLD_KEY_PREFIX + slotId + ":" + holdToken;
        redisTemplate.delete(holdKey);
        log.info("Hold released for slot {} with token {}", slotId, holdToken);
    }
    
    /**
     * Checks if a slot has an active hold
     * @param slotId The slot ID
     * @return true if slot has an active hold
     */
    public boolean hasActiveHold(Long slotId) {
        try {
            String pattern = HOLD_KEY_PREFIX + slotId + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            return keys != null && !keys.isEmpty();
        } catch (Exception e) {
            log.error("Redis error in hasActiveHold: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets all active holds for a slot
     * @param slotId The slot ID
     * @return Set of hold tokens
     */
    public Set<String> getActiveHolds(Long slotId) {
        try {
            String pattern = HOLD_KEY_PREFIX + slotId + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys == null) {
                return Set.of();
            }
            return keys.stream()
                    .map(key -> key.substring(key.lastIndexOf(':') + 1))
                    .collect(java.util.stream.Collectors.toSet());
        } catch (Exception e) {
            log.error("Redis error in getActiveHolds: {}", e.getMessage());
            return Set.of();
        }
    }
    
    /**
     * Cleanup expired holds (runs periodically)
     * Note: Redis TTL handles expiration automatically, but this can be used for logging
     */
    @Scheduled(fixedRateString = "${slot.cleanup-interval-minutes:1}", initialDelay = 60000)
    public void cleanupExpiredHolds() {
        try {
            Set<String> allHolds = redisTemplate.keys(HOLD_PATTERN);
            if (allHolds != null) {
                log.debug("Found {} active slot holds", allHolds.size());
            }
        } catch (Exception e) {
            log.error("Error during hold cleanup", e);
        }
    }
}

