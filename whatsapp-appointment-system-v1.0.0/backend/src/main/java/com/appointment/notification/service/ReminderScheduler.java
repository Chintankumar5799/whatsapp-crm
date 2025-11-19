package com.appointment.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for appointment reminders and other periodic tasks
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {
    
    private final NotificationService notificationService;
    
    /**
     * Daily report generation (runs at midnight)
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void generateDailyReport() {
        log.info("Generating daily report");
        // TODO: Implement daily report generation
    }
}

