package com.appointment.booking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing slot holds in Redis or In-Memory fallback.
 * Prevents double-booking by temporarily reserving slots without requiring an external Redis daemon.
 */
@Slf4j
@Service
public class SlotHoldService {
    
    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;
    
    @Value("${slot.hold.duration-minutes:5}")
    private int holdDurationMinutes;
    
    private static final String HOLD_KEY_PREFIX = "slot:hold:";
    private static final String HOLD_PATTERN = HOLD_KEY_PREFIX + "*";

    // In-memory fallback when Redis is absent
    private final Map<String, InMemoryHold> inMemoryHolds = new ConcurrentHashMap<>();

    private static class InMemoryHold {
        final String patientId;
        final Instant expiresAt;

        InMemoryHold(String patientId, Instant expiresAt) {
            this.patientId = patientId;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
    
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
        
        if (redisTemplate != null) {
            try {
                Duration holdDuration = Duration.ofMinutes(holdDurationMinutes);
                redisTemplate.opsForValue().set(holdKey, holdValue, holdDuration);
                log.info("Created Redis hold for slot {} with token {} (expires in {} minutes)", 
                        slotId, holdToken, holdDurationMinutes);
                return holdToken;
            } catch (Exception e) {
                log.warn("Redis unavailable, falling back to in-memory hold: {}", e.getMessage());
            }
        }

        inMemoryHolds.put(holdKey, new InMemoryHold(holdValue, Instant.now().plus(Duration.ofMinutes(holdDurationMinutes))));
        log.info("Created in-memory hold for slot {} with token {} (expires in {} minutes)", 
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
        
        if (redisTemplate != null) {
            try {
                String storedPatientId = redisTemplate.opsForValue().get(holdKey);
                if (storedPatientId != null) {
                    if (!storedPatientId.equals(patientId.toString())) {
                        log.warn("Hold patient ID mismatch for slot {} with token {}", slotId, holdToken);
                        return false;
                    }
                    redisTemplate.delete(holdKey);
                    log.info("Redis hold validated and consumed for slot {} with token {}", slotId, holdToken);
                    return true;
                }
            } catch (Exception e) {
                log.warn("Redis error during hold validation, checking in-memory: {}", e.getMessage());
            }
        }

        InMemoryHold hold = inMemoryHolds.get(holdKey);
        if (hold == null || hold.isExpired()) {
            inMemoryHolds.remove(holdKey);
            log.warn("In-memory hold not found or expired for slot {} with token {}", slotId, holdToken);
            return false;
        }

        if (!hold.patientId.equals(patientId.toString())) {
            log.warn("In-memory hold patient ID mismatch for slot {} with token {}", slotId, holdToken);
            return false;
        }

        inMemoryHolds.remove(holdKey);
        log.info("In-memory hold validated and consumed for slot {} with token {}", slotId, holdToken);
        return true;
    }
    
    /**
     * Releases a hold (cancels it)
     * @param slotId The slot ID
     * @param holdToken The hold token
     */
    public void releaseHold(Long slotId, String holdToken) {
        String holdKey = HOLD_KEY_PREFIX + slotId + ":" + holdToken;
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(holdKey);
            } catch (Exception ignored) {}
        }
        inMemoryHolds.remove(holdKey);
        log.info("Hold released for slot {} with token {}", slotId, holdToken);
    }
    
    /**
     * Checks if a slot has an active hold
     * @param slotId The slot ID
     * @return true if slot has an active hold
     */
    public boolean hasActiveHold(Long slotId) {
        String prefix = HOLD_KEY_PREFIX + slotId + ":";
        if (redisTemplate != null) {
            try {
                Set<String> keys = redisTemplate.keys(prefix + "*");
                if (keys != null && !keys.isEmpty()) {
                    return true;
                }
            } catch (Exception e) {
                log.warn("Redis error in hasActiveHold: {}", e.getMessage());
            }
        }
        
        return inMemoryHolds.entrySet().stream()
                .anyMatch(entry -> entry.getKey().startsWith(prefix) && !entry.getValue().isExpired());
    }
    
    /**
     * Gets all active holds for a slot
     * @param slotId The slot ID
     * @return Set of hold tokens
     */
    public Set<String> getActiveHolds(Long slotId) {
        String prefix = HOLD_KEY_PREFIX + slotId + ":";
        if (redisTemplate != null) {
            try {
                Set<String> keys = redisTemplate.keys(prefix + "*");
                if (keys != null && !keys.isEmpty()) {
                    return keys.stream()
                            .map(key -> key.substring(key.lastIndexOf(':') + 1))
                            .collect(Collectors.toSet());
                }
            } catch (Exception e) {
                log.warn("Redis error in getActiveHolds: {}", e.getMessage());
            }
        }

        return inMemoryHolds.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix) && !entry.getValue().isExpired())
                .map(entry -> entry.getKey().substring(entry.getKey().lastIndexOf(':') + 1))
                .collect(Collectors.toSet());
    }
    
    /**
     * Cleanup expired holds (runs periodically)
     */
    @Scheduled(fixedRateString = "${slot.cleanup-interval-minutes:1}", initialDelay = 60000)
    public void cleanupExpiredHolds() {
        if (redisTemplate != null) {
            try {
                Set<String> allHolds = redisTemplate.keys(HOLD_PATTERN);
                if (allHolds != null) {
                    log.debug("Found {} active Redis slot holds", allHolds.size());
                }
            } catch (Exception e) {
                log.debug("Redis hold cleanup skipped: {}", e.getMessage());
            }
        }

        inMemoryHolds.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}

