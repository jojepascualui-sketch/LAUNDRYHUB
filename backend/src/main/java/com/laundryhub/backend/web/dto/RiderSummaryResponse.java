package com.laundryhub.backend.web.dto;

/**
 * R1 header: "Hi, [RIDER NAME]", duty switch and the three counters.
 *
 * @param riderName      full name
 * @param initials       avatar initials
 * @param plateNumber    plate
 * @param onDuty         switch position
 * @param availableCount "Available" (also the "Available (n)" tab)
 * @param acceptedCount  "Accepted" (also the "My jobs (n)" tab)
 * @param doneTodayCount "Done today"
 */
public record RiderSummaryResponse(String riderName, String initials, String plateNumber, boolean onDuty,
                                   long availableCount, long acceptedCount, long doneTodayCount) {
}
