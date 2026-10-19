package com.laundryhub.app.model.rider;

import androidx.annotation.NonNull;

/**
 * R1 header: name, duty switch and the three counters.
 *
 * @param riderName      "Hi, [RIDER NAME]"
 * @param onDuty         switch position
 * @param availableCount "Available"
 * @param acceptedCount  "Accepted"
 * @param doneTodayCount "Done today"
 */
public record RiderSummary(@NonNull String riderName, boolean onDuty, long availableCount, long acceptedCount,
                           long doneTodayCount) {

    /** Validates the counters. */
    public RiderSummary {
        if (riderName == null) {
            throw new IllegalArgumentException("Rider name is required");
        }
        if (availableCount < 0 || acceptedCount < 0 || doneTodayCount < 0) {
            throw new IllegalArgumentException("Counters cannot be negative");
        }
    }
}
