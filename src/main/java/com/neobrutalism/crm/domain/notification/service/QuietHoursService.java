package com.neobrutalism.crm.domain.notification.service;

import com.neobrutalism.crm.domain.notification.model.NotificationPreference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Service for handling quiet hours logic
 * Determines if notifications should be queued based on user preferences
 */
@Slf4j
@Service
public class QuietHoursService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Check if current time is within user's quiet hours
     *
     * @param preference User's notification preference
     * @return true if within quiet hours, false otherwise
     */
    public boolean isWithinQuietHours(NotificationPreference preference) {
        if (preference == null) {
            return false;
        }

        String quietHoursStart = preference.getQuietHoursStart();
        String quietHoursEnd = preference.getQuietHoursEnd();

        // If quiet hours not configured, not within quiet hours
        if (quietHoursStart == null || quietHoursEnd == null ||
            quietHoursStart.isEmpty() || quietHoursEnd.isEmpty()) {
            return false;
        }

        try {
            LocalTime now = LocalTime.now();
            LocalTime start = LocalTime.parse(quietHoursStart, TIME_FORMATTER);
            LocalTime end = LocalTime.parse(quietHoursEnd, TIME_FORMATTER);

            return isTimeBetween(now, start, end);
        } catch (DateTimeParseException e) {
            log.warn("Invalid quiet hours format for preference: start={}, end={}",
                    quietHoursStart, quietHoursEnd, e);
            return false;
        }
    }

    /**
     * Check if time is between start and end
     * Handles overnight ranges (e.g., 22:00 to 08:00)
     *
     * @param current Current time
     * @param start Start of quiet hours
     * @param end End of quiet hours
     * @return true if current is between start and end
     */
    private boolean isTimeBetween(LocalTime current, LocalTime start, LocalTime end) {
        if (start.isBefore(end)) {
            // Same day range (e.g., 08:00 to 17:00)
            return !current.isBefore(start) && current.isBefore(end);
        } else {
            // Overnight range (e.g., 22:00 to 08:00)
            return !current.isBefore(start) || current.isBefore(end);
        }
    }

    /**
     * Calculate when quiet hours will end
     * Returns the LocalTime when notifications should resume
     *
     * @param preference User's notification preference
     * @return End time of quiet hours, or null if not in quiet hours
     */
    public LocalTime calculateQuietHoursEnd(NotificationPreference preference) {
        if (!isWithinQuietHours(preference)) {
            return null;
        }

        try {
            return LocalTime.parse(preference.getQuietHoursEnd(), TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("Invalid quiet hours end time: {}", preference.getQuietHoursEnd(), e);
            return null;
        }
    }

    /**
     * Validate quiet hours format
     *
     * @param timeString Time string in HH:mm format
     * @return true if valid format
     */
    public boolean isValidTimeFormat(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return false;
        }

        try {
            LocalTime.parse(timeString, TIME_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
